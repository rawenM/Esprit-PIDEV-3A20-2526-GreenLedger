package Services;

import Models.FraudDetectionResult;
import Models.FraudDetectionResult.FraudIndicator;
import Models.FraudDetectionResult.RiskLevel;
import Models.User;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Service de détection de fraude avec IA pour les inscriptions utilisateurs
 * Analyse plusieurs indicateurs pour calculer un score de risque
 */
public class FraudDetectionService {

    // Patterns de détection
    private static final Pattern DISPOSABLE_EMAIL_PATTERN = Pattern.compile(
        ".*(tempmail|guerrillamail|10minutemail|throwaway|mailinator|trashmail|fakeinbox).*",
        Pattern.CASE_INSENSITIVE
    );
    
    private static final Pattern SUSPICIOUS_NAME_PATTERN = Pattern.compile(
        ".*(test|fake|admin|root|demo|sample|example|null|undefined).*",
        Pattern.CASE_INSENSITIVE
    );
    
    private static final Pattern PHONE_PATTERN = Pattern.compile("^\\+?[0-9]{10,15}$");
    
    // Seuils de décision
    private static final double FRAUD_THRESHOLD = 70.0; // Score > 70 = frauduleux
    private static final double REVIEW_THRESHOLD = 40.0; // Score > 40 = à examiner
    
    /**
     * Analyse une inscription utilisateur et retourne un résultat de détection de fraude
     */
    public FraudDetectionResult analyzeRegistration(User user) {
        FraudDetectionResult result = new FraudDetectionResult();
        result.setUserId(user.getId());
        
        List<FraudIndicator> indicators = new ArrayList<>();
        double totalScore = 0.0;
        
        // 1. Vérification de l'email
        FraudIndicator emailIndicator = checkEmail(user.getEmail());
        indicators.add(emailIndicator);
        if (emailIndicator.isDetected()) {
            totalScore += emailIndicator.getWeight() * 100;
        }
        
        // 2. Vérification du nom et prénom
        FraudIndicator nameIndicator = checkName(user.getNom(), user.getPrenom());
        indicators.add(nameIndicator);
        if (nameIndicator.isDetected()) {
            totalScore += nameIndicator.getWeight() * 100;
        }
        
        // 3. Vérification du téléphone
        FraudIndicator phoneIndicator = checkPhone(user.getTelephone());
        indicators.add(phoneIndicator);
        if (phoneIndicator.isDetected()) {
            totalScore += phoneIndicator.getWeight() * 100;
        }
        
        // 4. Vérification de la cohérence des données
        FraudIndicator consistencyIndicator = checkDataConsistency(user);
        indicators.add(consistencyIndicator);
        if (consistencyIndicator.isDetected()) {
            totalScore += consistencyIndicator.getWeight() * 100;
        }
        
        // 5. Analyse comportementale (vitesse d'inscription)
        FraudIndicator behaviorIndicator = checkBehavior(user);
        indicators.add(behaviorIndicator);
        if (behaviorIndicator.isDetected()) {
            totalScore += behaviorIndicator.getWeight() * 100;
        }
        
        // 6. Vérification de l'adresse (si disponible)
        if (user.getAdresse() != null && !user.getAdresse().isEmpty()) {
            FraudIndicator addressIndicator = checkAddress(user.getAdresse());
            indicators.add(addressIndicator);
            if (addressIndicator.isDetected()) {
                totalScore += addressIndicator.getWeight() * 100;
            }
        }
        
        // 7. Analyse du type d'utilisateur
        FraudIndicator roleIndicator = checkUserRole(user);
        indicators.add(roleIndicator);
        if (roleIndicator.isDetected()) {
            totalScore += roleIndicator.getWeight() * 100;
        }
        
        // Calculer le score final
        result.setIndicators(indicators);
        result.setRiskScore(Math.min(100, totalScore));
        result.setFraudulent(totalScore >= FRAUD_THRESHOLD);
        
        // Générer la recommandation
        String recommendation = generateRecommendation(totalScore);
        result.setRecommendation(recommendation);
        
        // Générer les détails de l'analyse
        String details = generateAnalysisDetails(indicators, totalScore);
        result.setAnalysisDetails(details);
        
        System.out.println("[FraudDetection] Analyse terminée pour: " + user.getEmail());
        System.out.println("  Score de risque: " + String.format("%.1f", totalScore) + "/100");
        System.out.println("  Niveau: " + result.getRiskLevel().getLabel());
        System.out.println("  Recommandation: " + recommendation);
        
        return result;
    }
    
    /**
     * Vérifie si l'email est suspect
     */
    private FraudIndicator checkEmail(String email) {
        boolean isSuspicious = false;
        String description = "Email valide";
        
        if (email == null || email.isEmpty()) {
            isSuspicious = true;
            description = "Email manquant";
        } else if (DISPOSABLE_EMAIL_PATTERN.matcher(email).matches()) {
            isSuspicious = true;
            description = "Email jetable détecté (tempmail, guerrillamail, etc.)";
        } else if (!email.contains("@") || !email.contains(".")) {
            isSuspicious = true;
            description = "Format d'email invalide";
        } else if (email.split("@")[0].length() < 3) {
            isSuspicious = true;
            description = "Email trop court (moins de 3 caractères avant @)";
        }
        
        return new FraudIndicator("EMAIL", description, 0.25, isSuspicious);
    }
    
    /**
     * Vérifie si le nom/prénom est suspect
     */
    private FraudIndicator checkName(String nom, String prenom) {
        boolean isSuspicious = false;
        String description = "Nom et prénom valides";
        
        if (nom == null || nom.isEmpty() || prenom == null || prenom.isEmpty()) {
            isSuspicious = true;
            description = "Nom ou prénom manquant";
        } else if (SUSPICIOUS_NAME_PATTERN.matcher(nom).matches() || 
                   SUSPICIOUS_NAME_PATTERN.matcher(prenom).matches()) {
            isSuspicious = true;
            description = "Nom suspect détecté (test, fake, admin, etc.)";
        } else if (nom.length() < 2 || prenom.length() < 2) {
            isSuspicious = true;
            description = "Nom ou prénom trop court";
        } else if (nom.equals(prenom)) {
            isSuspicious = true;
            description = "Nom et prénom identiques";
        } else if (nom.matches(".*\\d.*") || prenom.matches(".*\\d.*")) {
            isSuspicious = true;
            description = "Nom ou prénom contient des chiffres";
        }
        
        return new FraudIndicator("NAME", description, 0.20, isSuspicious);
    }
    
    /**
     * Vérifie si le téléphone est valide
     */
    private FraudIndicator checkPhone(String telephone) {
        boolean isSuspicious = false;
        String description = "Téléphone valide";
        
        if (telephone == null || telephone.isEmpty()) {
            isSuspicious = true;
            description = "Numéro de téléphone manquant";
        } else if (!PHONE_PATTERN.matcher(telephone).matches()) {
            isSuspicious = true;
            description = "Format de téléphone invalide";
        } else if (telephone.matches("(\\d)\\1{9,}")) {
            isSuspicious = true;
            description = "Numéro de téléphone répétitif (ex: 1111111111)";
        } else if (telephone.matches("^0+$") || telephone.matches("^1+$")) {
            isSuspicious = true;
            description = "Numéro de téléphone invalide (tous 0 ou tous 1)";
        }
        
        return new FraudIndicator("PHONE", description, 0.15, isSuspicious);
    }
    
    /**
     * Vérifie la cohérence des données
     */
    private FraudIndicator checkDataConsistency(User user) {
        boolean isSuspicious = false;
        String description = "Données cohérentes";
        
        // Vérifier si l'email correspond au nom
        if (user.getEmail() != null && user.getNom() != null && user.getPrenom() != null) {
            String emailLocal = user.getEmail().split("@")[0].toLowerCase();
            String nomLower = user.getNom().toLowerCase();
            String prenomLower = user.getPrenom().toLowerCase();
            
            // Si l'email ne contient ni le nom ni le prénom, c'est suspect
            if (!emailLocal.contains(nomLower.substring(0, Math.min(3, nomLower.length()))) &&
                !emailLocal.contains(prenomLower.substring(0, Math.min(3, prenomLower.length())))) {
                isSuspicious = true;
                description = "Email ne correspond pas au nom/prénom";
            }
        }
        
        return new FraudIndicator("CONSISTENCY", description, 0.10, isSuspicious);
    }
    
    /**
     * Analyse comportementale
     */
    private FraudIndicator checkBehavior(User user) {
        boolean isSuspicious = false;
        String description = "Comportement normal";
        
        // Vérifier si l'inscription est trop rapide (simulation)
        // Dans une vraie implémentation, on mesurerait le temps entre l'ouverture du formulaire et la soumission
        
        // Pour l'instant, on vérifie juste si toutes les données sont remplies de manière suspecte
        if (user.getEmail() != null && user.getNom() != null && user.getPrenom() != null &&
            user.getTelephone() != null && user.getAdresse() != null) {
            // Tous les champs remplis instantanément pourrait être un bot
            // Mais c'est aussi normal, donc poids faible
        }
        
        return new FraudIndicator("BEHAVIOR", description, 0.05, isSuspicious);
    }
    
    /**
     * Vérifie l'adresse
     */
    private FraudIndicator checkAddress(String adresse) {
        boolean isSuspicious = false;
        String description = "Adresse valide";
        
        if (adresse == null || adresse.isEmpty()) {
            isSuspicious = true;
            description = "Adresse manquante";
        } else if (adresse.length() < 10) {
            isSuspicious = true;
            description = "Adresse trop courte";
        } else if (adresse.matches(".*\\b(test|fake|none|n/a|na)\\b.*")) {
            isSuspicious = true;
            description = "Adresse suspecte (test, fake, etc.)";
        }
        
        return new FraudIndicator("ADDRESS", description, 0.10, isSuspicious);
    }
    
    /**
     * Vérifie le rôle de l'utilisateur
     */
    private FraudIndicator checkUserRole(User user) {
        boolean isSuspicious = false;
        String description = "Rôle approprié";
        
        // Les tentatives d'inscription en tant qu'admin ou expert sont suspectes
        if (user.isAdmin()) {
            isSuspicious = true;
            description = "Tentative d'inscription en tant qu'administrateur";
        }
        
        return new FraudIndicator("ROLE", description, 0.15, isSuspicious);
    }
    
    /**
     * Génère une recommandation basée sur le score
     */
    private String generateRecommendation(double score) {
        if (score >= FRAUD_THRESHOLD) {
            return "REJETER - Score de risque trop élevé";
        } else if (score >= REVIEW_THRESHOLD) {
            return "EXAMINER - Vérification manuelle recommandée";
        } else {
            return "APPROUVER - Risque faible";
        }
    }
    
    /**
     * Génère les détails de l'analyse en format texte
     */
    private String generateAnalysisDetails(List<FraudIndicator> indicators, double score) {
        StringBuilder sb = new StringBuilder();
        sb.append("Analyse de détection de fraude\n");
        sb.append("Score total: ").append(String.format("%.1f", score)).append("/100\n\n");
        sb.append("Indicateurs analysés:\n");
        
        for (FraudIndicator indicator : indicators) {
            sb.append("- ").append(indicator.getType()).append(": ");
            sb.append(indicator.getDescription());
            if (indicator.isDetected()) {
                sb.append(" [DÉTECTÉ - Poids: ").append(indicator.getWeight()).append("]");
            }
            sb.append("\n");
        }
        
        return sb.toString();
    }
    
    /**
     * Analyse rapide pour obtenir juste le score
     */
    public double getQuickRiskScore(User user) {
        FraudDetectionResult result = analyzeRegistration(user);
        return result.getRiskScore();
    }
    
    /**
     * Vérifie si un utilisateur est considéré comme frauduleux
     */
    public boolean isFraudulent(User user) {
        FraudDetectionResult result = analyzeRegistration(user);
        return result.isFraudulent();
    }
}
