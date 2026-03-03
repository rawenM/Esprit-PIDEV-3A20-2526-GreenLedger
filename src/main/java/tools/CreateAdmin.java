package tools;

import Models.TypeUtilisateur;
import DataBase.MyConnection;
import Utils.PasswordUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Utilitaire simple pour creer ou mettre a jour un utilisateur admin/test depuis la ligne de commande
 * Usage (IDE): executer la classe avec les arguments: email password firstname lastname
 */
public class CreateAdmin {
    public static void main(String[] args) {
        if (args.length < 4) {
            System.out.println("Usage: CreateAdmin <email> <password> <prenom> <nom>");
            System.out.println("Exemple: CreateAdmin admin@plateforme.com Admin@123 Admin Admin");
            return;
        }

        String email = args[0].trim().toLowerCase();
        String password = args[1];
        String prenom = args[2];
        String nom = args[3];

        Connection conn = null;
        try {
            conn = MyConnection.getInstance().getConnection();

            // Verifier si l'utilisateur existe
            String selectSql = "SELECT id FROM `user` WHERE LOWER(email) = LOWER(?)";
            try (PreparedStatement ps = conn.prepareStatement(selectSql)) {
                ps.setString(1, email);
                ResultSet rs = ps.executeQuery();
                String hashed = PasswordUtil.hashPassword(password);

                if (rs.next()) {
                    long id = rs.getLong(1);
                    System.out.println("Utilisateur existant trouve (id=" + id + "), mise a jour du mot de passe et activation.");
                    String updateSql = "UPDATE `user` SET mot_de_passe = ?, email_verifie = ?, statut = ? WHERE id = ?";
                    try (PreparedStatement ups = conn.prepareStatement(updateSql)) {
                        ups.setString(1, hashed);
                        ups.setBoolean(2, true);
                        ups.setString(3, "ACTIVE");
                        ups.setLong(4, id);
                        ups.executeUpdate();
                        System.out.println("[CLEAN] Mise a jour terminee. Email verifie et mot de passe mis a jour.");
                    }
                } else {
                    System.out.println("Aucun utilisateur trouve, creation d'un nouvel utilisateur.");
                    String insertSql = "INSERT INTO `user` (nom, prenom, email, mot_de_passe, type_utilisateur, statut, email_verifie) VALUES (?, ?, ?, ?, ?, ?, ?)";
                    try (PreparedStatement ins = conn.prepareStatement(insertSql)) {
                        ins.setString(1, nom);
                        ins.setString(2, prenom);
                        ins.setString(3, email);
                        ins.setString(4, hashed);
                        ins.setString(5, TypeUtilisateur.ADMIN.name());
                        ins.setString(6, "ACTIVE");
                        ins.setBoolean(7, true);
                        ins.executeUpdate();
                        System.out.println("[CLEAN] Utilisateur cree et active.");
                    }
                }
            }

        } catch (SQLException ex) {
            System.err.println("Erreur SQL: " + ex.getMessage());
            ex.printStackTrace();
        } catch (Exception ex) {
            System.err.println("Erreur: " + ex.getMessage());
            ex.printStackTrace();
        } finally {
            if (conn != null) {
                try {
                    conn.close();
                } catch (Exception ignored) {
                }
            }
        }
    }
}
