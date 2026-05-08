package Services;

import Models.User;
import Models.AuditLog;
import Models.Wallet;

import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * CSV Export Service for Admin Dashboard
 * Exports data to CSV format for analysis and reporting
 * 
 * Supported exports:
 * - Users list
 * - Audit logs
 * - Wallets
 * - Custom data
 */
public class CsvExportService {

    private static final DateTimeFormatter DATE_FORMATTER = 
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final String CSV_SEPARATOR = ",";
    private static final String CSV_QUOTE = "\"";

    /**
     * Export users to CSV file
     */
    public boolean exportUsers(List<User> users, String filePath) {
        try (FileWriter writer = new FileWriter(filePath)) {
            // Write header
            writer.append("ID,Nom,Prénom,Email,Téléphone,Type,Statut,Date Inscription,Dernière Connexion,Score Fraude,Email Vérifié\n");
            
            // Write data
            for (User user : users) {
                writer.append(String.valueOf(user.getId())).append(CSV_SEPARATOR);
                writer.append(escapeCsv(user.getNom())).append(CSV_SEPARATOR);
                writer.append(escapeCsv(user.getPrenom())).append(CSV_SEPARATOR);
                writer.append(escapeCsv(user.getEmail())).append(CSV_SEPARATOR);
                writer.append(escapeCsv(user.getTelephone())).append(CSV_SEPARATOR);
                writer.append(user.getTypeUtilisateur() != null ? 
                    user.getTypeUtilisateur().getLibelle() : "").append(CSV_SEPARATOR);
                writer.append(user.getStatut() != null ? 
                    user.getStatut().getLibelle() : "").append(CSV_SEPARATOR);
                writer.append(formatDate(user.getDateInscription())).append(CSV_SEPARATOR);
                writer.append(formatDate(user.getDerniereConnexion())).append(CSV_SEPARATOR);
                writer.append(String.format("%.2f", user.getFraudScore())).append(CSV_SEPARATOR);
                writer.append(user.isEmailVerifie() ? "Oui" : "Non");
                writer.append("\n");
            }
            
            System.out.println("[CSV Export] " + users.size() + " users exported to " + filePath);
            return true;
            
        } catch (IOException e) {
            System.err.println("[CSV Export] Error exporting users: " + e.getMessage());
            return false;
        }
    }

    /**
     * Export audit logs to CSV file
     */
    public boolean exportAuditLogs(List<AuditLog> logs, String filePath) {
        try (FileWriter writer = new FileWriter(filePath)) {
            // Write header
            writer.append("ID,Date,Type Action,Utilisateur,Email,Description,Statut,IP,Navigateur,OS\n");
            
            // Write data
            for (AuditLog log : logs) {
                writer.append(String.valueOf(log.getId())).append(CSV_SEPARATOR);
                writer.append(formatDate(log.getCreatedAt())).append(CSV_SEPARATOR);
                writer.append(log.getActionType() != null ? 
                    log.getActionType().getLabel() : "").append(CSV_SEPARATOR);
                writer.append(escapeCsv(log.getUserName())).append(CSV_SEPARATOR);
                writer.append(escapeCsv(log.getUserEmail())).append(CSV_SEPARATOR);
                writer.append(escapeCsv(log.getActionDescription())).append(CSV_SEPARATOR);
                writer.append(log.getStatus() != null ? 
                    log.getStatus().getLabel() : "").append(CSV_SEPARATOR);
                writer.append(escapeCsv(log.getIpAddress())).append(CSV_SEPARATOR);
                writer.append(escapeCsv(log.getBrowser())).append(CSV_SEPARATOR);
                writer.append(escapeCsv(log.getOperatingSystem()));
                writer.append("\n");
            }
            
            System.out.println("[CSV Export] " + logs.size() + " audit logs exported to " + filePath);
            return true;
            
        } catch (IOException e) {
            System.err.println("[CSV Export] Error exporting audit logs: " + e.getMessage());
            return false;
        }
    }

    /**
     * Export wallets to CSV file
     */
    public boolean exportWallets(List<Wallet> wallets, String filePath) {
        try (FileWriter writer = new FileWriter(filePath)) {
            // Write header
            writer.append("ID,Numéro Wallet,Nom,Type Propriétaire,ID Propriétaire,Crédits Disponibles,Crédits Retirés,Total,Date Création\n");
            
            // Write data
            for (Wallet wallet : wallets) {
                writer.append(String.valueOf(wallet.getId())).append(CSV_SEPARATOR);
                writer.append(String.valueOf(wallet.getWalletNumber())).append(CSV_SEPARATOR);
                writer.append(escapeCsv(wallet.getName())).append(CSV_SEPARATOR);
                writer.append(escapeCsv(wallet.getOwnerType())).append(CSV_SEPARATOR);
                writer.append(String.valueOf(wallet.getOwnerId())).append(CSV_SEPARATOR);
                writer.append(String.format("%.2f", wallet.getAvailableCredits())).append(CSV_SEPARATOR);
                writer.append(String.format("%.2f", wallet.getRetiredCredits())).append(CSV_SEPARATOR);
                writer.append(String.format("%.2f", wallet.getTotalCredits())).append(CSV_SEPARATOR);
                writer.append(wallet.getCreatedAt() != null ? 
                    wallet.getCreatedAt().toString() : "");
                writer.append("\n");
            }
            
            System.out.println("[CSV Export] " + wallets.size() + " wallets exported to " + filePath);
            return true;
            
        } catch (IOException e) {
            System.err.println("[CSV Export] Error exporting wallets: " + e.getMessage());
            return false;
        }
    }

    /**
     * Export users with fraud details to CSV
     */
    public boolean exportUsersWithFraud(List<User> users, String filePath) {
        try (FileWriter writer = new FileWriter(filePath)) {
            // Write header
            writer.append("ID,Nom Complet,Email,Type,Statut,Score Fraude,Fraude Vérifiée,Date Inscription,Pays,Ville\n");
            
            // Write data
            for (User user : users) {
                writer.append(String.valueOf(user.getId())).append(CSV_SEPARATOR);
                writer.append(escapeCsv(user.getNomComplet())).append(CSV_SEPARATOR);
                writer.append(escapeCsv(user.getEmail())).append(CSV_SEPARATOR);
                writer.append(user.getTypeUtilisateur() != null ? 
                    user.getTypeUtilisateur().getLibelle() : "").append(CSV_SEPARATOR);
                writer.append(user.getStatut() != null ? 
                    user.getStatut().getLibelle() : "").append(CSV_SEPARATOR);
                writer.append(String.format("%.2f", user.getFraudScore())).append(CSV_SEPARATOR);
                writer.append(user.isFraudChecked() ? "Oui" : "Non").append(CSV_SEPARATOR);
                writer.append(formatDate(user.getDateInscription())).append(CSV_SEPARATOR);
                writer.append(escapeCsv(user.getLastLoginCountry())).append(CSV_SEPARATOR);
                writer.append(escapeCsv(user.getLastLoginCity()));
                writer.append("\n");
            }
            
            System.out.println("[CSV Export] " + users.size() + " users with fraud data exported to " + filePath);
            return true;
            
        } catch (IOException e) {
            System.err.println("[CSV Export] Error exporting users with fraud: " + e.getMessage());
            return false;
        }
    }

    /**
     * Export pending activations with AI recommendations
     */
    public boolean exportPendingActivations(List<User> users, 
                                           UserActivationRecommendationService aiService,
                                           String filePath) {
        try (FileWriter writer = new FileWriter(filePath)) {
            // Write header
            writer.append("ID,Nom Complet,Email,Type,Date Inscription,Recommandation IA,Confiance,Score,Raisons\n");
            
            // Write data
            for (User user : users) {
                var prediction = aiService.predict(user);
                
                writer.append(String.valueOf(user.getId())).append(CSV_SEPARATOR);
                writer.append(escapeCsv(user.getNomComplet())).append(CSV_SEPARATOR);
                writer.append(escapeCsv(user.getEmail())).append(CSV_SEPARATOR);
                writer.append(user.getTypeUtilisateur() != null ? 
                    user.getTypeUtilisateur().getLibelle() : "").append(CSV_SEPARATOR);
                writer.append(formatDate(user.getDateInscription())).append(CSV_SEPARATOR);
                writer.append(escapeCsv((String) prediction.get("label"))).append(CSV_SEPARATOR);
                writer.append(String.valueOf(prediction.get("confidence"))).append(CSV_SEPARATOR);
                writer.append(String.format("%.2f", (double) prediction.get("score"))).append(CSV_SEPARATOR);
                
                // Format reasons
                @SuppressWarnings("unchecked")
                List<java.util.Map<String, String>> reasons = 
                    (List<java.util.Map<String, String>>) prediction.get("reasons");
                StringBuilder reasonsStr = new StringBuilder();
                for (var reason : reasons) {
                    reasonsStr.append(reason.get("text")).append("; ");
                }
                writer.append(escapeCsv(reasonsStr.toString()));
                writer.append("\n");
            }
            
            System.out.println("[CSV Export] " + users.size() + " pending activations exported to " + filePath);
            return true;
            
        } catch (IOException e) {
            System.err.println("[CSV Export] Error exporting pending activations: " + e.getMessage());
            return false;
        }
    }

    /**
     * Escape CSV special characters
     */
    private String escapeCsv(String value) {
        if (value == null) return "";
        
        // If value contains comma, quote, or newline, wrap in quotes and escape quotes
        if (value.contains(CSV_SEPARATOR) || value.contains(CSV_QUOTE) || value.contains("\n")) {
            return CSV_QUOTE + value.replace(CSV_QUOTE, CSV_QUOTE + CSV_QUOTE) + CSV_QUOTE;
        }
        
        return value;
    }

    /**
     * Format date for CSV
     */
    private String formatDate(LocalDateTime date) {
        return date != null ? date.format(DATE_FORMATTER) : "";
    }

    /**
     * Generate filename with timestamp
     */
    public static String generateFilename(String prefix) {
        String timestamp = LocalDateTime.now().format(
            DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")
        );
        return prefix + "_" + timestamp + ".csv";
    }
}
