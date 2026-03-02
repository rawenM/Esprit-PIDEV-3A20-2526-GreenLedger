package Utils;

import Models.StatutUtilisateur;
import Models.TypeUtilisateur;
import Models.User;
import Services.FraudDetectionService;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service pour générer les données JSON pour Chart.js
 */
public class ChartDataService {
    
    private final Gson gson = new Gson();
    private final FraudDetectionService fraudService = new FraudDetectionService();
    
    /**
     * Génère les données JSON pour tous les graphiques
     */
    public String generateChartData(List<User> users) {
        JsonObject data = new JsonObject();
        
        // Données par statut
        data.add("status", generateStatusData(users));
        
        // Données par type
        data.add("type", generateTypeData(users));
        
        // Données d'inscriptions par mois
        data.add("registrations", generateRegistrationData(users));
        
        // Données de fraude
        data.add("fraud", generateFraudData(users));
        
        return gson.toJson(data);
    }
    
    /**
     * Génère les données de répartition par statut
     */
    private JsonObject generateStatusData(List<User> users) {
        JsonObject statusData = new JsonObject();
        
        long active = users.stream()
            .filter(u -> u.getStatut() == StatutUtilisateur.ACTIVE)
            .count();
        
        long pending = users.stream()
            .filter(u -> u.getStatut() == StatutUtilisateur.EN_ATTENTE)
            .count();
        
        long blocked = users.stream()
            .filter(u -> u.getStatut() == StatutUtilisateur.BLOQUE)
            .count();
        
        long suspended = users.stream()
            .filter(u -> u.getStatut() == StatutUtilisateur.SUSPENDU)
            .count();
        
        statusData.addProperty("active", active);
        statusData.addProperty("pending", pending);
        statusData.addProperty("blocked", blocked);
        statusData.addProperty("suspended", suspended);
        
        return statusData;
    }
    
    /**
     * Génère les données de répartition par type
     */
    private JsonObject generateTypeData(List<User> users) {
        JsonObject typeData = new JsonObject();
        
        long investor = users.stream()
            .filter(u -> u.getTypeUtilisateur() == TypeUtilisateur.INVESTISSEUR)
            .count();
        
        long projectOwner = users.stream()
            .filter(u -> u.getTypeUtilisateur() == TypeUtilisateur.PORTEUR_PROJET)
            .count();
        
        long admin = users.stream()
            .filter(u -> u.getTypeUtilisateur() == TypeUtilisateur.ADMIN)
            .count();
        
        long evaluator = users.stream()
            .filter(u -> u.getTypeUtilisateur() == TypeUtilisateur.EXPERT_CARBONE)
            .count();
        
        typeData.addProperty("investor", investor);
        typeData.addProperty("projectOwner", projectOwner);
        typeData.addProperty("admin", admin);
        typeData.addProperty("evaluator", evaluator);
        
        return typeData;
    }
    
    /**
     * Génère les données d'inscriptions par mois (6 derniers mois)
     */
    private JsonObject generateRegistrationData(List<User> users) {
        JsonObject registrationData = new JsonObject();
        
        // Obtenir les 6 derniers mois
        LocalDateTime now = LocalDateTime.now();
        List<String> labels = new ArrayList<>();
        List<Integer> values = new ArrayList<>();
        
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM yyyy", Locale.FRENCH);
        
        for (int i = 5; i >= 0; i--) {
            LocalDateTime month = now.minusMonths(i);
            String label = month.format(formatter);
            labels.add(label);
            
            // Compter les inscriptions pour ce mois
            int count = (int) users.stream()
                .filter(u -> u.getDateInscription() != null)
                .filter(u -> {
                    LocalDateTime userDate = u.getDateInscription();
                    return userDate.getYear() == month.getYear() &&
                           userDate.getMonthValue() == month.getMonthValue();
                })
                .count();
            
            values.add(count);
        }
        
        registrationData.add("labels", gson.toJsonTree(labels));
        registrationData.add("values", gson.toJsonTree(values));
        
        return registrationData;
    }
    
    /**
     * Génère les données de distribution des scores de fraude
     */
    private JsonObject generateFraudData(List<User> users) {
        JsonObject fraudData = new JsonObject();
        
        int safe = 0;      // 0-30
        int low = 0;       // 31-50
        int medium = 0;    // 51-70
        int high = 0;      // 71-85
        int critical = 0;  // 86-100
        
        for (User user : users) {
            try {
                // Utiliser le score déjà calculé dans la base de données
                double score = user.getFraudScore();
                
                if (score <= 30) safe++;
                else if (score <= 50) low++;
                else if (score <= 70) medium++;
                else if (score <= 85) high++;
                else critical++;
                
            } catch (Exception e) {
                // Si pas de score, considérer comme sûr
                safe++;
            }
        }
        
        fraudData.addProperty("safe", safe);
        fraudData.addProperty("low", low);
        fraudData.addProperty("medium", medium);
        fraudData.addProperty("high", high);
        fraudData.addProperty("critical", critical);
        
        return fraudData;
    }
    
    /**
     * Génère des données de test pour démonstration
     */
    public String generateSampleData() {
        JsonObject data = new JsonObject();
        
        // Statut
        JsonObject status = new JsonObject();
        status.addProperty("active", 45);
        status.addProperty("pending", 12);
        status.addProperty("blocked", 3);
        status.addProperty("suspended", 2);
        data.add("status", status);
        
        // Type
        JsonObject type = new JsonObject();
        type.addProperty("investor", 30);
        type.addProperty("projectOwner", 25);
        type.addProperty("admin", 5);
        type.addProperty("evaluator", 2);
        data.add("type", type);
        
        // Inscriptions
        JsonObject registrations = new JsonObject();
        registrations.add("labels", gson.toJsonTree(Arrays.asList(
            "Sep 2025", "Oct 2025", "Nov 2025", "Dec 2025", "Jan 2026", "Fev 2026"
        )));
        registrations.add("values", gson.toJsonTree(Arrays.asList(5, 8, 12, 15, 18, 4)));
        data.add("registrations", registrations);
        
        // Fraude
        JsonObject fraud = new JsonObject();
        fraud.addProperty("safe", 40);
        fraud.addProperty("low", 15);
        fraud.addProperty("medium", 5);
        fraud.addProperty("high", 2);
        fraud.addProperty("critical", 0);
        data.add("fraud", fraud);
        
        return gson.toJson(data);
    }
}
