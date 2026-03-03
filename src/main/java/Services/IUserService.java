package Services;


import Models.StatutUtilisateur;
import Models.TypeUtilisateur;
import Models.User;

import java.util.List;
import java.util.Optional;

/**
 * Interface du service utilisateur
 */
public interface IUserService {

    // Authentification
    User register(User user, String password) throws Exception;
    Optional<User> login(String email, String password);
    void logout(User user);

    // Gestion du profil
    User updateProfile(User user) throws Exception;
    boolean changePassword(Long userId, String oldPassword, String newPassword);
    boolean resetPassword(String emailOrPhone);
    boolean resetPasswordByPhone(String phone);

    // Gestion administrative
    User validateAccount(Long userId);
    User blockUser(Long userId);
    User unblockUser(Long userId);
    User suspendUser(Long userId);

    // Recherche et récupération
    List<User> getAllUsers();
    Optional<User> getUserById(Long id);
    Optional<User> getUserByEmail(String email);
    List<User> getUsersByStatut(StatutUtilisateur statut);
    List<User> getUsersByType(TypeUtilisateur type);

    // Statistiques
    long getUserCount();
    long getActiveUserCount();
    long getPendingUserCount();
    long getBlockedUserCount();

    // Recherche avancée
    List<User> searchUsers(String searchTerm);

    // Administration supplémentaire
    boolean deleteUser(Long userId);
    User updateUserRole(Long userId, TypeUtilisateur newRole);

    // Réinitialisation via token
    boolean resetPasswordWithToken(String token, String newPassword);

    /**
     * Démarrer la procédure de réinitialisation : génère un token et essaie d'envoyer
     * l'email/SMS. Retourne le token généré (utile pour test/fallback), ou null en cas d'erreur.
     */
    String initiatePasswordReset(String emailOrPhone);
}
