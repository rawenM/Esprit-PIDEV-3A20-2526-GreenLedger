package dao;

import Models.StatutUtilisateur;
import Models.TypeUtilisateur;
import Models.User;
import DataBase.MyConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class UserDAOImpl implements IUserDAO {

    private final Connection connection;
    // Indique si la colonne token_expiry est disponible dans la table
    private final boolean hasTokenExpiryColumn;
    // Indique si la colonne token_hash est disponible dans la table
    private final boolean hasTokenHashColumn;
    // Indique si les colonnes de fraude sont disponibles
    private final boolean hasFraudScoreColumn;
    private final boolean hasFraudCheckedColumn;

    public UserDAOImpl() {
        this.connection = MyConnection.getInstance().getConnection();
        boolean hasTokenExpiry = false;
        boolean hasTokenHash = false;
        boolean hasFraudScore = false;
        boolean hasFraudChecked = false;
        
        // Tentative d'ajout de la colonne token_expiry si elle n'existe pas (migration légère)
        try {
            DatabaseMetaData md = connection.getMetaData();
            try (ResultSet rs = md.getColumns(null, null, "user", "token_expiry")) {
                if (!rs.next()) {
                    try (Statement st = connection.createStatement()) {
                        st.executeUpdate("ALTER TABLE `user` ADD COLUMN token_expiry DATETIME NULL");
                        System.out.println("[CLEAN] Colonne token_expiry ajoutée à la table user (migration automatique)");
                        hasTokenExpiry = true;
                    } catch (SQLException ex) {
                        System.err.println("[CLEAN] Impossible d'ajouter la colonne token_expiry automatiquement: " + ex.getMessage());
                        try (ResultSet rs2 = md.getColumns(null, null, "user", "token_expiry")) {
                            if (rs2.next()) hasTokenExpiry = true;
                        } catch (Exception ignored) {}
                    }
                } else {
                    hasTokenExpiry = true;
                }
            }
            // Idem pour token_hash
            try (ResultSet rs = md.getColumns(null, null, "user", "token_hash")) {
                if (!rs.next()) {
                    try (Statement st = connection.createStatement()) {
                        st.executeUpdate("ALTER TABLE `user` ADD COLUMN token_hash VARCHAR(255) NULL");
                        System.out.println("[CLEAN] Colonne token_hash ajoutée à la table user (migration automatique)");
                        hasTokenHash = true;
                    } catch (SQLException ex) {
                        System.err.println("[CLEAN] Impossible d'ajouter la colonne token_hash automatiquement: " + ex.getMessage());
                        try (ResultSet rs2 = md.getColumns(null, null, "user", "token_hash")) {
                            if (rs2.next()) hasTokenHash = true;
                        } catch (Exception ignored) {}
                    }
                } else {
                    hasTokenHash = true;
                }
            }
            
            // Vérifier les colonnes de fraude
            try (ResultSet rs = md.getColumns(null, null, "user", "fraud_score")) {
                if (rs.next()) {
                    hasFraudScore = true;
                    System.out.println("[FraudDetection] Colonne fraud_score détectée");
                }
            }
            try (ResultSet rs = md.getColumns(null, null, "user", "fraud_checked")) {
                if (rs.next()) {
                    hasFraudChecked = true;
                    System.out.println("[FraudDetection] Colonne fraud_checked détectée");
                }
            }
        } catch (Exception ignored) {}
        
        this.hasTokenExpiryColumn = hasTokenExpiry;
        this.hasTokenHashColumn = hasTokenHash;
        this.hasFraudScoreColumn = hasFraudScore;
        this.hasFraudCheckedColumn = hasFraudChecked;
    }

    @Override
    public User save(User user) {
        // Construire la requête SQL sans les colonnes photo et token si NULL
        String sql = "INSERT INTO `user` (nom, prenom, email, mot_de_passe, telephone, " +
                "adresse, date_naissance, type_utilisateur, statut, " +
                "email_verifie, token_verification";

        if (hasTokenExpiryColumn) {
            sql += ", token_expiry";
        }
        if (hasTokenHashColumn) {
            sql += ", token_hash";
        }
        sql += ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?";
        if (hasTokenExpiryColumn) {
            sql += ", ?";
        }
        if (hasTokenHashColumn) {
            sql += ", ?";
        }
        sql += ")";

        try (PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, user.getNom());
            ps.setString(2, user.getPrenom());
            ps.setString(3, user.getEmail());
            ps.setString(4, user.getMotDePasse());
            ps.setString(5, user.getTelephone() != null ? user.getTelephone() : "");
            ps.setString(6, user.getAdresse() != null ? user.getAdresse() : "");
            if (user.getDateNaissance() != null) {
                ps.setDate(7, Date.valueOf(user.getDateNaissance()));
            } else {
                ps.setNull(7, Types.DATE);
            }
            ps.setString(8, user.getTypeUtilisateur() != null ? user.getTypeUtilisateur().name() : TypeUtilisateur.INVESTISSEUR.name());
            ps.setString(9, user.getStatut() != null ? user.getStatut().name() : "EN_ATTENTE");
            ps.setBoolean(10, user.isEmailVerifie());
            ps.setString(11, user.getTokenVerification() != null ? user.getTokenVerification() : "");

            int idx = 12;
            if (hasTokenExpiryColumn) {
                if (user.getTokenExpiry() != null) {
                    ps.setTimestamp(idx, Timestamp.valueOf(user.getTokenExpiry()));
                } else {
                    ps.setNull(idx, Types.TIMESTAMP);
                }
                idx++;
            }
            if (hasTokenHashColumn) {
                ps.setString(idx, user.getTokenHash() != null ? user.getTokenHash() : "");
                idx++;
            }

            int affectedRows = ps.executeUpdate();

            if (affectedRows == 0) {
                throw new SQLException("La création de l'utilisateur a échoué");
            }

            // Récupérer l'ID généré
            try (ResultSet generatedKeys = ps.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    user.setId(generatedKeys.getLong(1));
                }
            }

            System.out.println("[CLEAN] Utilisateur créé: " + user.getEmail());
            return user;

        } catch (SQLException e) {
            System.err.println("[CLEAN] Erreur lors de la création d'utilisateur");
            System.err.println("   Email: " + user.getEmail());
            System.err.println("   Type: " + (user.getTypeUtilisateur() != null ? user.getTypeUtilisateur().name() : "NULL"));
            System.err.println("   Statut: " + (user.getStatut() != null ? user.getStatut().name() : "NULL"));
            System.err.println("   Message: " + e.getMessage());
            System.err.println("   SQL State: " + e.getSQLState());
            System.err.println("   Error Code: " + e.getErrorCode());
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public Optional<User> findById(Long id) {
        String sql = "SELECT * FROM `user` WHERE id = ?";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, id);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return Optional.of(mapResultSetToUser(rs));
            }
        } catch (SQLException e) {
            System.err.println("[CLEAN] Erreur lors de la recherche par ID: " + e.getMessage());
            e.printStackTrace();
        }

        return Optional.empty();
    }

    @Override
    public List<User> findAll() {
        List<User> users = new ArrayList<>();
        String sql = "SELECT * FROM `user` ORDER BY date_inscription DESC";

        try (PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                users.add(mapResultSetToUser(rs));
            }

            System.out.println("[CLEAN] " + users.size() + " utilisateurs trouvés");

        } catch (SQLException e) {
            System.err.println("[CLEAN] Erreur lors de la récupération: " + e.getMessage());
            e.printStackTrace();
        }

        return users;
    }

    @Override
    public User update(User user) {
        String sql = "UPDATE `user` SET nom = ?, prenom = ?, email = ?, telephone = ?, " +
                "adresse = ?, date_naissance = ?, type_utilisateur = ?, statut = ?, " +
                "email_verifie = ?, mot_de_passe = ?, token_verification = ?, derniere_connexion = ?";

        if (hasTokenExpiryColumn) {
            sql += ", token_expiry = ?";
        }
        if (hasTokenHashColumn) {
            sql += ", token_hash = ?";
        }
        if (hasFraudScoreColumn) {
            sql += ", fraud_score = ?";
        }
        if (hasFraudCheckedColumn) {
            sql += ", fraud_checked = ?";
        }
        sql += " WHERE id = ?";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {

            ps.setString(1, user.getNom());
            ps.setString(2, user.getPrenom());
            ps.setString(3, user.getEmail());
            ps.setString(4, user.getTelephone() != null ? user.getTelephone() : "");
            ps.setString(5, user.getAdresse() != null ? user.getAdresse() : "");
            if (user.getDateNaissance() != null) {
                ps.setDate(6, Date.valueOf(user.getDateNaissance()));
            } else {
                ps.setNull(6, Types.DATE);
            }
            ps.setString(7, user.getTypeUtilisateur() != null ? user.getTypeUtilisateur().name() : TypeUtilisateur.INVESTISSEUR.name());
            ps.setString(8, user.getStatut() != null ? user.getStatut().name() : "EN_ATTENTE");
            ps.setBoolean(9, user.isEmailVerifie());
            ps.setString(10, user.getMotDePasse() != null ? user.getMotDePasse() : "");
            ps.setString(11, user.getTokenVerification() != null ? user.getTokenVerification() : "");

            int paramIndex = 12;
            // premièrement : derniere_connexion
            if (user.getDerniereConnexion() != null) {
                ps.setTimestamp(paramIndex, Timestamp.valueOf(user.getDerniereConnexion()));
            } else {
                ps.setNull(paramIndex, Types.TIMESTAMP);
            }
            paramIndex++;

            // ensuite token_expiry si présent
            if (hasTokenExpiryColumn) {
                if (user.getTokenExpiry() != null) {
                    ps.setTimestamp(paramIndex, Timestamp.valueOf(user.getTokenExpiry()));
                } else {
                    ps.setNull(paramIndex, Types.TIMESTAMP);
                }
                paramIndex++;
            }

            // token_hash si présent
            if (hasTokenHashColumn) {
                ps.setString(paramIndex, user.getTokenHash() != null ? user.getTokenHash() : "");
                paramIndex++;
            }

            // fraud_score si présent
            if (hasFraudScoreColumn) {
                ps.setDouble(paramIndex, user.getFraudScore());
                paramIndex++;
            }

            // fraud_checked si présent
            if (hasFraudCheckedColumn) {
                ps.setBoolean(paramIndex, user.isFraudChecked());
                paramIndex++;
            }

            // id
            ps.setLong(paramIndex, user.getId());

            int affectedRows = ps.executeUpdate();

            if (affectedRows > 0) {
                System.out.println("[CLEAN] Utilisateur mis à jour: " + user.getEmail());
                return user;
            }

        } catch (SQLException e) {
            System.err.println("[CLEAN] Erreur lors de la mise à jour: " + e.getMessage());
            System.err.println("   Email: " + user.getEmail());
            System.err.println("   SQL State: " + e.getSQLState());
            e.printStackTrace();
        }

        return null;
    }

    @Override
    public boolean delete(Long id) {
        String sql = "DELETE FROM `user` WHERE id = ?";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, id);
            int affectedRows = ps.executeUpdate();

            if (affectedRows > 0) {
                System.out.println("[CLEAN] Utilisateur supprimé (ID: " + id + ")");
                return true;
            }

        } catch (SQLException e) {
            System.err.println("[CLEAN] Erreur lors de la suppression: " + e.getMessage());
            e.printStackTrace();
        }

        return false;
    }

    @Override
    public Optional<User> findByEmail(String email) {
        String sql = "SELECT * FROM `user` WHERE LOWER(TRIM(email)) = LOWER(TRIM(?))";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, email);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                User u = mapResultSetToUser(rs);
                System.out.println("🔎 findByEmail: trouvé pour email='" + email + "' -> id=" + u.getId());
                return Optional.of(u);
            } else {
                System.out.println("🔎 findByEmail: aucun utilisateur pour email='" + email + "'");
            }
        } catch (SQLException e) {
            System.err.println("[CLEAN] Erreur lors de la recherche par email: " + e.getMessage());
            e.printStackTrace();
        }

        return Optional.empty();
    }

    @Override
    public Optional<User> findByTelephone(String telephone) {
        if (telephone == null) return Optional.empty();
        String sql = "SELECT * FROM `user` WHERE REPLACE(REPLACE(telephone, ' ', ''), '-', '') = REPLACE(REPLACE(?, ' ', ''), '-', '') LIMIT 1";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, telephone);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                User u = mapResultSetToUser(rs);
                System.out.println("🔎 findByTelephone: trouvé pour telephone='" + telephone + "' -> id=" + u.getId());
                return Optional.of(u);
            } else {
                System.out.println("🔎 findByTelephone: aucun utilisateur pour telephone='" + telephone + "'");
            }
        } catch (SQLException e) {
            System.err.println("[CLEAN] Erreur lors de la recherche par telephone: " + e.getMessage());
            e.printStackTrace();
        }

        return Optional.empty();
    }

    @Override
    public Optional<User> findByToken(String token) {
        if (token == null) return Optional.empty();
        String sql = "SELECT * FROM `user` WHERE token_verification = ? LIMIT 1";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, token);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return Optional.of(mapResultSetToUser(rs));
            }
        } catch (SQLException e) {
            System.err.println("[CLEAN] Erreur lors de la recherche par token: " + e.getMessage());
            e.printStackTrace();
        }

        return Optional.empty();
    }

    @Override
    public List<User> findByType(TypeUtilisateur type) {
        List<User> users = new ArrayList<>();
        String sql = "SELECT * FROM `user` WHERE type_utilisateur = ?";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, type.name());
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                users.add(mapResultSetToUser(rs));
            }

        } catch (SQLException e) {
            System.err.println("[CLEAN] Erreur lors de la recherche par type: " + e.getMessage());
            e.printStackTrace();
        }

        return users;
    }

    @Override
    public List<User> findByStatut(StatutUtilisateur statut) {
        List<User> users = new ArrayList<>();
        String sql = "SELECT * FROM `user` WHERE statut = ?";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, statut.name());
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                users.add(mapResultSetToUser(rs));
            }

        } catch (SQLException e) {
            System.err.println("[CLEAN] Erreur lors de la recherche par statut: " + e.getMessage());
            e.printStackTrace();
        }

        return users;
    }

    @Override
    public boolean emailExists(String email) {
        String sql = "SELECT COUNT(*) FROM `user` WHERE LOWER(TRIM(email)) = LOWER(TRIM(?))";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, email);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            System.err.println("[CLEAN] Erreur lors de la vérification de l'email: " + e.getMessage());
            e.printStackTrace();
        }

        return false;
    }

    @Override
    public long count() {
        String sql = "SELECT COUNT(*) FROM `user`";

        try (PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            if (rs.next()) {
                return rs.getLong(1);
            }
        } catch (SQLException e) {
            System.err.println("[CLEAN] Erreur lors du comptage: " + e.getMessage());
            e.printStackTrace();
        }

        return 0;
    }

    @Override
    public long countByStatut(StatutUtilisateur statut) {
        String sql = "SELECT COUNT(*) FROM `user` WHERE statut = ?";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, statut.name());
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return rs.getLong(1);
            }
        } catch (SQLException e) {
            System.err.println("[CLEAN] Erreur lors du comptage par statut: " + e.getMessage());
            e.printStackTrace();
        }

        return 0;
    }

    @Override
    public long countByType(TypeUtilisateur type) {
        String sql = "SELECT COUNT(*) FROM `user` WHERE type_utilisateur = ?";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, type.name());
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return rs.getLong(1);
            }
        } catch (SQLException e) {
            System.err.println("[CLEAN] Erreur lors du comptage par type: " + e.getMessage());
            e.printStackTrace();
        }

        return 0;
    }

    /**
     * Méthode utilitaire pour mapper un ResultSet vers un objet User
     */
    private User mapResultSetToUser(ResultSet rs) throws SQLException {
        User user = new User();

        user.setId(rs.getLong("id"));
        user.setNom(rs.getString("nom"));
        user.setPrenom(rs.getString("prenom"));
        user.setEmail(rs.getString("email"));
        user.setMotDePasse(rs.getString("mot_de_passe"));
        user.setTelephone(rs.getString("telephone"));
        user.setAdresse(rs.getString("adresse"));

        Date dateNaissance = rs.getDate("date_naissance");
        if (dateNaissance != null) {
            user.setDateNaissance(dateNaissance.toLocalDate());
        }

        // Mapping tolérant aux valeurs stockées (nom ou libellé)
        String typeStr = rs.getString("type_utilisateur");
        user.setTypeUtilisateur(parseTypeUtilisateur(typeStr));

        String statutStr = rs.getString("statut");
        user.setStatut(parseStatutUtilisateur(statutStr));

        user.setPhoto(rs.getString("photo"));

        Timestamp dateInscription = null;
        try {
            dateInscription = rs.getTimestamp("date_inscription");
        } catch (SQLException ignored) {}
        if (dateInscription != null) {
            user.setDateInscription(dateInscription.toLocalDateTime());
        }

        Timestamp derniereConnexion = null;
        try {
            derniereConnexion = rs.getTimestamp("derniere_connexion");
        } catch (SQLException ignored) {}
        if (derniereConnexion != null) {
            user.setDerniereConnexion(derniereConnexion.toLocalDateTime());
        }

        try {
            user.setEmailVerifie(rs.getBoolean("email_verifie"));
        } catch (SQLException ignored) {}

        try {
            user.setTokenVerification(rs.getString("token_verification"));
        } catch (SQLException ignored) {}

        try {
            Timestamp tokenTs = rs.getTimestamp("token_expiry");
            if (tokenTs != null) user.setTokenExpiry(tokenTs.toLocalDateTime());
        } catch (SQLException ignored) {}

        try {
            String tokenHash = rs.getString("token_hash");
            if (tokenHash != null) user.setTokenHash(tokenHash);
        } catch (SQLException ignored) {}

        // Champs de détection de fraude
        try {
            user.setFraudScore(rs.getDouble("fraud_score"));
        } catch (SQLException ignored) {
            user.setFraudScore(0.0);
        }

        try {
            user.setFraudChecked(rs.getBoolean("fraud_checked"));
        } catch (SQLException ignored) {
            user.setFraudChecked(false);
        }

        return user;
    }

    private TypeUtilisateur parseTypeUtilisateur(String dbValue) {
        if (dbValue == null) return null;
        String raw = dbValue.trim();
        // Essayer valueOf direct
        try {
            return TypeUtilisateur.valueOf(raw);
        } catch (Exception ignored) {
        }
        // Normalisation: supprimer espaces/underscores et comparer insensible à la casse
        String norm = raw.replaceAll("[_\\s]+", "").toLowerCase();
        for (TypeUtilisateur t : TypeUtilisateur.values()) {
            if (t.name().replaceAll("[_\\s]+", "").toLowerCase().equals(norm)
                    || t.getLibelle().replaceAll("[_\\s]+", "").toLowerCase().equals(norm)) {
                return t;
            }
        }
        // Enfin, essayer de récupérer via nom en uppercase
        try {
            return TypeUtilisateur.valueOf(raw.toUpperCase());
        } catch (Exception e) {
            return null;
        }
    }

    private StatutUtilisateur parseStatutUtilisateur(String dbValue) {
        if (dbValue == null) return null;
        String raw = dbValue.trim();
        try {
            return StatutUtilisateur.valueOf(raw);
        } catch (Exception ignored) {
        }
        String norm = raw.replaceAll("[_\\s]+", "").toLowerCase();
        for (StatutUtilisateur s : StatutUtilisateur.values()) {
            if (s.name().replaceAll("[_\\s]+", "").toLowerCase().equals(norm)
                    || s.getLibelle().replaceAll("[_\\s]+", "").toLowerCase().equals(norm)) {
                return s;
            }
        }
        try {
            return StatutUtilisateur.valueOf(raw.toUpperCase());
        } catch (Exception e) {
            return null;
        }
    }
}
