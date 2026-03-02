package Services;

import dao.IUserDAO;
import dao.UserDAOImpl;
import Models.StatutUtilisateur;
import Models.TypeUtilisateur;
import Models.User;
import Utils.EmailService;
import Utils.PasswordUtil;
import Utils.SessionManager;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

public class UserServiceImpl implements IUserService {

    private final IUserDAO userDAO;
    private final ValidationService validationService;
    private final EmailService emailService;

    public UserServiceImpl() {
        this.userDAO = new UserDAOImpl();
        this.validationService = new ValidationService();
        this.emailService = new EmailService();
    }

    @Override
    public User register(User user, String password) throws Exception {
        // 1. Validation des donnees
        List<String> errors = validationService.validateUser(user);
        if (!errors.isEmpty()) {
            throw new Exception("Erreurs de validation:\n- " + String.join("\n- ", errors));
        }

        // Normaliser l'email (trim + lowercase)
        if (user.getEmail() != null) {
            user.setEmail(user.getEmail().trim().toLowerCase());
        }

        // 2. Verification si l'email existe deja
        if (userDAO.emailExists(user.getEmail())) {
            throw new Exception("Cet email est deja utilise");
        }

        // 2b. Si le role est Expert Carbone, s'assurer qu'il n'existe pas deja
        if (user.getTypeUtilisateur() == TypeUtilisateur.EXPERT_CARBONE) {
            long count = userDAO.countByType(TypeUtilisateur.EXPERT_CARBONE);
            if (count > 0) {
                throw new Exception("Un expert carbone existe deja dans le systeme. Impossible d'en creer un deuxieme.");
            }
        }

        // 3. Validation du mot de passe
        String passwordError = PasswordUtil.getPasswordErrorMessage(password);
        if (passwordError != null) {
            throw new Exception(passwordError);
        }

        // 4. Hashage du mot de passe
        user.setMotDePasse(PasswordUtil.hashPassword(password));

        // 4b. Statut et emailVerifie par defaut
        if (user.getStatut() == null) {
            user.setStatut(StatutUtilisateur.EN_ATTENTE);
        }
        user.setEmailVerifie(user.isEmailVerifie());

        // 5. Generation du token de verification
        user.setTokenVerification(UUID.randomUUID().toString());

        // 6. Sauvegarde dans la BD
        User savedUser = userDAO.save(user);

        if (savedUser != null) {
            System.out.println("[OK] Utilisateur inscrit: " + savedUser.getEmail());
            trySendWelcomeEmail(savedUser);
            return savedUser;
        }

        throw new Exception("Erreur lors de la creation de l'utilisateur en base de donnees. Veuillez verifier vos informations et reessayer.");
    }

    @Override
    public Optional<User> login(String email, String password) {
        // 1. Rechercher l'utilisateur par email
        Optional<User> userOpt = userDAO.findByEmail(email);

        if (userOpt.isEmpty()) {
            System.err.println("[ERR] Login echoue : email introuvable => " + email);
            return Optional.empty();
        }

        User user = userOpt.get();

        // Debug
        try {
            String stored = user.getMotDePasse();
            System.out.println("[DBG] LOGIN DEBUG: Email=" + user.getEmail());
            System.out.println("   - Password en base (BCrypt): " + (stored != null ? stored.substring(0, Math.min(20, stored.length())) + "..." : "NULL"));
            System.out.println("   - Password length: " + (stored != null ? stored.length() : 0));
            System.out.println("[DBG] Utilisateur trouve: " + user.getEmail() + " | storedPasswordPresent=" + (stored != null && !stored.isEmpty()) + " | emailVerifie=" + user.isEmailVerifie() + " | statut=" + user.getStatut());
        } catch (Exception ex) {
            System.err.println("[WARN] Erreur lors de l'affichage des infos utilisateur: " + ex.getMessage());
        }

        // 2. Verifier que le mot de passe stocke existe
        if (user.getMotDePasse() == null || user.getMotDePasse().isEmpty()) {
            System.err.println("[ERR] Login echoue : mot de passe non present en base pour l'email => " + email);
            return Optional.empty();
        }

        // Detection du format du mot de passe stocke
        String stored = user.getMotDePasse();
        boolean looksHashed = stored.startsWith("$2a$") || stored.startsWith("$2b$") || stored.startsWith("$2y$") || stored.startsWith("$2$");
        if (!looksHashed) {
            System.out.println("[WARN] Mot de passe en base ne semble pas hache pour " + email);
            if (stored.equals(password)) {
                System.out.println("[INFO] Migration: mot de passe en clair valide detecte, re-hash et mise a jour de la base pour " + email);
                user.setMotDePasse(PasswordUtil.hashPassword(password));
                userDAO.update(user);
                user.setDerniereConnexion(LocalDateTime.now());
                userDAO.update(user);
                System.out.println("[OK] Connexion reussie apres migration: " + email);
                SessionManager.getInstance().createSession(user);
                return Optional.of(user);
            }
            System.err.println("[ERR] Login echoue : mot de passe incorrect (et non hache) pour => " + email);
            return Optional.empty();
        }

        // 3. Verifier le mot de passe
        System.out.println("[INFO] Verification du mot de passe (BCrypt check)...");
        boolean passwordOk = PasswordUtil.checkPassword(password, user.getMotDePasse());
        System.out.println("   - Password check result: " + passwordOk);
        if (!passwordOk) {
            System.err.println("[ERR] Login echoue : mot de passe incorrect pour => " + email);
            return Optional.empty();
        }

        // 4. Verifier le statut du compte
        if (user.getStatut() == StatutUtilisateur.BLOQUE) {
            System.err.println("[ERR] Compte bloque: " + email);
            throw new RuntimeException("Votre compte a ete bloque. Contactez l'administrateur.");
        }

        if (user.getStatut() == StatutUtilisateur.SUSPENDU) {
            System.err.println("[ERR] Compte suspendu: " + email);
            throw new RuntimeException("Votre compte est temporairement suspendu.");
        }

        // 5. Verifier si l'email est verifie
        if (!user.isEmailVerifie()) {
            System.err.println("[WARN] Email non verifie: " + email);
            throw new RuntimeException("Veuillez verifier votre email avant de vous connecter.");
        }

        // 6. Mettre a jour la derniere connexion
        user.setDerniereConnexion(LocalDateTime.now());
        userDAO.update(user);

        System.out.println("[OK] Connexion reussie: " + email);
        SessionManager.getInstance().createSession(user);
        return Optional.of(user);
    }

    @Override
    public void logout(User user) {
        System.out.println("[INFO] Deconnexion de: " + user.getEmail());
        SessionManager.getInstance().invalidate();
    }

    @Override
    public User updateProfile(User user) throws Exception {
        // 1. Validation
        List<String> errors = validationService.validateUser(user);
        if (!errors.isEmpty()) {
            throw new Exception("Erreurs de validation:\n- " + String.join("\n- ", errors));
        }

        // 2. Verifier que l'utilisateur existe
        Optional<User> existingUser = userDAO.findById(user.getId());
        if (existingUser.isEmpty()) {
            throw new Exception("Utilisateur non trouve");
        }

        // 3. Si l'email a change, verifier qu'il n'est pas deja utilise
        if (!user.getEmail().equals(existingUser.get().getEmail())) {
            if (userDAO.emailExists(user.getEmail())) {
                throw new Exception("Cet email est deja utilise");
            }
        }

        // 4. Mise a jour
        User updatedUser = userDAO.update(user);
        if (updatedUser == null) {
            throw new Exception("Erreur lors de la mise a jour");
        }

        System.out.println("[OK] Profil mis a jour: " + user.getEmail());
        return updatedUser;
    }

    @Override
    public boolean changePassword(Long userId, String oldPassword, String newPassword) {
        Optional<User> userOpt = userDAO.findById(userId);

        if (userOpt.isEmpty()) {
            System.err.println("[ERR] Utilisateur non trouve");
            return false;
        }

        User user = userOpt.get();

        // Verifier l'ancien mot de passe
        if (!PasswordUtil.checkPassword(oldPassword, user.getMotDePasse())) {
            System.err.println("[ERR] Ancien mot de passe incorrect");
            return false;
        }

        // Valider le nouveau mot de passe
        String passwordError = PasswordUtil.getPasswordErrorMessage(newPassword);
        if (passwordError != null) {
            System.err.println("[ERR] " + passwordError);
            return false;
        }

        // Mettre a jour
        user.setMotDePasse(PasswordUtil.hashPassword(newPassword));
        User updatedUser = userDAO.update(user);

        if (updatedUser != null) {
            System.out.println("[OK] Mot de passe change pour: " + user.getEmail());
            return true;
        }

        return false;
    }

    @Override
    public boolean resetPassword(String emailOrPhone) {
        String token = initiatePasswordReset(emailOrPhone);
        return token != null;
    }

    @Override
    public String initiatePasswordReset(String emailOrPhone) {
        if (emailOrPhone == null || emailOrPhone.trim().isEmpty()) return null;

        if (emailOrPhone.contains("@")) {
            Optional<User> userOpt = userDAO.findByEmail(emailOrPhone.trim());
            if (userOpt.isEmpty()) {
                System.err.println("[ERR] Email non trouve");
                return null;
            }

            User user = userOpt.get();
            String resetToken = UUID.randomUUID().toString();
            String tokenHash = PasswordUtil.hashPassword(resetToken);
            user.setTokenVerification(resetToken);
            user.setTokenHash(tokenHash);
            user.setTokenExpiry(LocalDateTime.now().plusMinutes(30));
            userDAO.update(user);

            Utils.EmailService emailService = new Utils.EmailService();
            boolean sent = false;
            try {
                if (emailService.isConfigured()) sent = emailService.sendResetEmail(user.getEmail(), resetToken);
            } catch (Exception ignored) {}

            if (sent) return resetToken;
            System.out.println("[INFO] Email de reinitialisation (fallback) a: " + user.getEmail());
            System.out.println("[INFO] Token: " + resetToken);
            return resetToken;
        } else {
            Optional<User> userOpt = userDAO.findByTelephone(emailOrPhone.trim());
            if (userOpt.isEmpty()) {
                System.err.println("[ERR] Telephone non trouve");
                return null;
            }
            User user = userOpt.get();
            String resetToken = UUID.randomUUID().toString();
            String tokenHash = PasswordUtil.hashPassword(resetToken);
            user.setTokenVerification(resetToken);
            user.setTokenHash(tokenHash);
            user.setTokenExpiry(LocalDateTime.now().plusMinutes(30));
            userDAO.update(user);
            System.out.println("[INFO] SMS de reinitialisation (fallback) a: " + user.getTelephone());
            System.out.println("[INFO] Token/OTP: " + resetToken);
            return resetToken;
        }
    }

    @Override
    public boolean resetPasswordByPhone(String phone) {
        if (phone == null || phone.trim().isEmpty()) return false;

        Optional<User> userOpt = userDAO.findByTelephone(phone.trim());
        if (userOpt.isEmpty()) {
            System.err.println("[ERR] Telephone non trouve");
            return false;
        }

        User user = userOpt.get();
        String resetToken = UUID.randomUUID().toString();
        user.setTokenVerification(resetToken);
        user.setTokenExpiry(LocalDateTime.now().plusMinutes(30));
        userDAO.update(user);

        System.out.println("[INFO] SMS de reinitialisation a envoyer a: " + user.getTelephone());
        System.out.println("[INFO] Token/OTP: " + resetToken);

        return true;
    }

    @Override
    public boolean resetPasswordWithToken(String token, String newPassword) {
        if (token == null || token.trim().isEmpty() || newPassword == null || newPassword.isEmpty()) return false;

        Optional<User> userOpt = userDAO.findByToken(token);
        if (userOpt.isEmpty()) {
            System.err.println("[ERR] Token invalide ou utilisateur introuvable");
            return false;
        }

        User user = userOpt.get();

        if (user.getTokenHash() != null && !user.getTokenHash().isEmpty()) {
            if (!PasswordUtil.checkPassword(token, user.getTokenHash())) {
                System.err.println("[ERR] Token invalide (hash mismatch) pour: " + user.getEmail());
                return false;
            }
        } else if (!token.equals(user.getTokenVerification())) {
            System.err.println("[ERR] Token invalide pour: " + user.getEmail());
            return false;
        }

        if (user.getTokenExpiry() != null && user.getTokenExpiry().isBefore(LocalDateTime.now())) {
            System.err.println("[ERR] Token expire pour: " + user.getEmail());
            return false;
        }

        String err = PasswordUtil.getPasswordErrorMessage(newPassword);
        if (err != null) {
            System.err.println("[ERR] Mot de passe non conforme: " + err);
            return false;
        }

        user.setMotDePasse(PasswordUtil.hashPassword(newPassword));
        user.setTokenVerification(null);
        user.setTokenHash(null);
        user.setTokenExpiry(null);
        user.setEmailVerifie(true);
        userDAO.update(user);
        System.out.println("[OK] Mot de passe reinitialise pour: " + user.getEmail());
        return true;
    }

    @Override
    public User validateAccount(Long userId) {
        Optional<User> userOpt = userDAO.findById(userId);

        if (userOpt.isEmpty()) {
            System.err.println("[ERR] Utilisateur non trouve");
            return null;
        }

        User user = userOpt.get();
        user.setStatut(StatutUtilisateur.ACTIVE);
        user.setEmailVerifie(true);
        user.setTokenVerification(null);

        User updatedUser = userDAO.update(user);
        System.out.println("[OK] Compte valide: " + user.getEmail());
        trySendStatusEmail(user, "valide");
        return updatedUser;
    }

    @Override
    public User blockUser(Long userId) {
        Optional<User> userOpt = userDAO.findById(userId);

        if (userOpt.isEmpty()) {
            return null;
        }

        User user = userOpt.get();
        user.setStatut(StatutUtilisateur.BLOQUE);

        User updatedUser = userDAO.update(user);
        System.out.println("[INFO] Utilisateur bloque: " + user.getEmail());
        trySendStatusEmail(user, "bloque");
        return updatedUser;
    }

    @Override
    public User unblockUser(Long userId) {
        Optional<User> userOpt = userDAO.findById(userId);

        if (userOpt.isEmpty()) {
            return null;
        }

        User user = userOpt.get();
        user.setStatut(StatutUtilisateur.ACTIVE);

        User updatedUser = userDAO.update(user);
        System.out.println("[OK] Utilisateur debloque: " + user.getEmail());
        trySendStatusEmail(user, "debloque");
        return updatedUser;
    }

    @Override
    public User suspendUser(Long userId) {
        Optional<User> userOpt = userDAO.findById(userId);

        if (userOpt.isEmpty()) {
            return null;
        }

        User user = userOpt.get();
        user.setStatut(StatutUtilisateur.SUSPENDU);

        User updatedUser = userDAO.update(user);
        System.out.println("[PAUSE] Utilisateur suspendu: " + user.getEmail());
        return updatedUser;
    }

    @Override
    public boolean deleteUser(Long userId) {
        if (userId == null) return false;
        Optional<User> userOpt = userDAO.findById(userId);
        boolean ok = userDAO.delete(userId);
        if (ok) System.out.println("[DEL] Utilisateur supprime (ID=" + userId + ")");
        if (ok && userOpt.isPresent()) {
            trySendStatusEmail(userOpt.get(), "supprime");
        }
        return ok;
    }

    private void trySendWelcomeEmail(User user) {
        if (user == null || user.getEmail() == null) return;
        try {
            emailService.sendWelcomeEmail(user.getEmail(), user.getNomComplet());
        } catch (Exception e) {
            System.err.println("[CLEAN] Email bienvenue non envoye: " + e.getMessage());
        }
    }

    private void trySendStatusEmail(User user, String status) {
        if (user == null || user.getEmail() == null) return;
        try {
            emailService.sendAccountStatusEmail(user.getEmail(), user.getNomComplet(), status);
        } catch (Exception e) {
            System.err.println("[CLEAN] Email statut non envoye: " + e.getMessage());
        }
    }

    @Override
    public User updateUserRole(Long userId, TypeUtilisateur newRole) {
        Optional<User> userOpt = userDAO.findById(userId);
        if (userOpt.isEmpty()) return null;
        User user = userOpt.get();
        user.setTypeUtilisateur(newRole);
        User updated = userDAO.update(user);
        if (updated != null) System.out.println("[INFO] Role mis a jour pour " + updated.getEmail() + " -> " + newRole);
        return updated;
    }

    @Override
    public List<User> getAllUsers() {
        return userDAO.findAll();
    }

    @Override
    public Optional<User> getUserById(Long id) {
        return userDAO.findById(id);
    }

    @Override
    public Optional<User> getUserByEmail(String email) {
        return userDAO.findByEmail(email);
    }

    @Override
    public List<User> getUsersByStatut(StatutUtilisateur statut) {
        return userDAO.findByStatut(statut);
    }

    @Override
    public List<User> getUsersByType(TypeUtilisateur type) {
        return userDAO.findByType(type);
    }

    @Override
    public long getUserCount() {
        return userDAO.count();
    }

    @Override
    public long getActiveUserCount() {
        return userDAO.countByStatut(StatutUtilisateur.ACTIVE);
    }

    @Override
    public long getPendingUserCount() {
        return userDAO.countByStatut(StatutUtilisateur.EN_ATTENTE);
    }

    @Override
    public long getBlockedUserCount() {
        return userDAO.countByStatut(StatutUtilisateur.BLOQUE);
    }

    @Override
    public List<User> searchUsers(String searchTerm) {
        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            return getAllUsers();
        }

        String term = searchTerm.toLowerCase();

        return getAllUsers().stream()
                .filter(user ->
                        user.getNom().toLowerCase().contains(term) ||
                                user.getPrenom().toLowerCase().contains(term) ||
                                user.getEmail().toLowerCase().contains(term) ||
                                (user.getTelephone() != null && user.getTelephone().contains(term))
                )
                .collect(Collectors.toList());
    }
}