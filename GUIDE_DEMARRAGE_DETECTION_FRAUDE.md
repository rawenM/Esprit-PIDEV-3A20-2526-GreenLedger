# Guide de Démarrage: Détection de Fraude avec IA

## Résumé

Vous avez maintenant un système complet de détection de fraude avec IA qui analyse automatiquement chaque inscription et calcule un score de risque de 0 à 100.

## Test Confirmé ✅

Le système a été testé avec succès sur 7 scénarios différents:

1. ✅ **Utilisateur légitime** - Score: 0/100 (Faible) - APPROUVER
2. ✅ **Email jetable** - Score: 35/100 (Moyen) - APPROUVER  
3. ✅ **Nom suspect** - Score: 30/100 (Moyen) - APPROUVER
4. ✅ **Téléphone invalide** - Score: 15/100 (Faible) - APPROUVER
5. ✅ **Données incohérentes** - Score: 20/100 (Faible) - APPROUVER
6. ✅ **Nom admin** - Score: 20/100 (Faible) - APPROUVER
7. ✅ **Multiples indicateurs** - Score: 70/100 (Élevé) - REJETER ⚠️

## Installation

### Étape 1: Créer la Table dans la Base de Données

```bash
mysql -u root -p green_wallet < database_fraud_detection.sql
```

Ou manuellement dans MySQL:

```sql
CREATE TABLE IF NOT EXISTS fraud_detection_results (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    risk_score DOUBLE NOT NULL,
    risk_level VARCHAR(20) NOT NULL,
    is_fraudulent BOOLEAN NOT NULL DEFAULT FALSE,
    recommendation VARCHAR(255),
    analysis_details TEXT,
    analyzed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    FOREIGN KEY (user_id) REFERENCES utilisateurs(id) ON DELETE CASCADE
);

ALTER TABLE utilisateurs 
ADD COLUMN fraud_score DOUBLE DEFAULT 0.0 AFTER email_verifie,
ADD COLUMN fraud_checked BOOLEAN DEFAULT FALSE AFTER fraud_score;
```

### Étape 2: Compiler les Classes

```bash
# Compiler le modèle
javac -encoding UTF-8 -d target\classes -cp target\classes src\main\java\Models\FraudDetectionResult.java

# Compiler le service
javac -encoding UTF-8 -d target\classes -cp target\classes src\main\java\Services\FraudDetectionService.java

# Compiler les DAO
javac -encoding UTF-8 -d target\classes -cp target\classes src\main\java\dao\IFraudDetectionDAO.java
javac -encoding UTF-8 -d target\classes -cp target\classes src\main\java\dao\FraudDetectionDAOImpl.java

# Recompiler UserServiceImpl (avec la nouvelle intégration)
./compile-services.bat
```

### Étape 3: Tester

```bash
test-fraud-detection.bat
```

## Comment Ça Marche

### Lors de l'Inscription

1. L'utilisateur remplit le formulaire d'inscription
2. Les données sont validées normalement
3. L'utilisateur est créé dans la base de données
4. **Le système IA analyse automatiquement l'inscription**
5. Un score de risque est calculé (0-100)
6. Une décision est prise:
   - Score < 40: Approuver (statut EN_ATTENTE)
   - Score 40-70: Examiner (statut EN_ATTENTE + alerte)
   - Score > 70: Rejeter (statut BLOQUE)

### Indicateurs Analysés

Le système analyse 7 indicateurs:

1. **Email** (25%) - Détecte les emails jetables, invalides
2. **Nom/Prénom** (20%) - Détecte les noms suspects (test, fake, admin)
3. **Téléphone** (15%) - Vérifie le format et détecte les numéros répétitifs
4. **Cohérence** (10%) - Vérifie que l'email correspond au nom
5. **Adresse** (10%) - Détecte les adresses suspectes ou trop courtes
6. **Rôle** (15%) - Détecte les tentatives d'inscription en tant qu'admin
7. **Comportement** (5%) - Analyse le comportement d'inscription

## Exemples de Résultats

### Utilisateur Légitime ✅

```
Utilisateur: Jean Dupont
Email: jean.dupont@gmail.com
Téléphone: +33612345678

Score de risque: 0/100
Niveau: Faible
Recommandation: APPROUVER
```

### Utilisateur Suspect ⚠️

```
Utilisateur: Fake Test
Email: test@guerrillamail.com
Téléphone: 0000000000

Score de risque: 70/100
Niveau: Élevé
Recommandation: REJETER

Indicateurs détectés:
  ⚠️  EMAIL: Email jetable détecté
  ⚠️  NAME: Nom suspect détecté
  ⚠️  PHONE: Numéro répétitif
  ⚠️  ADDRESS: Adresse suspecte
```

## Logs dans l'Application

Lors d'une inscription, vous verrez:

```
[OK] Utilisateur inscrit: jean.dupont@gmail.com
[FraudDetection] Analyse de l'inscription...
[FraudDetection] Analyse terminée pour: jean.dupont@gmail.com
  Score de risque: 0,0/100
  Niveau: Faible
  Recommandation: APPROUVER - Risque faible
[FraudDetection] Score de risque: 0.0/100
Niveau de risque: Faible
Recommandation: APPROUVER - Risque faible
Indicateurs detectes: 0
```

Si le score est élevé:

```
[FraudDetection] ALERTE: Score de risque critique - Compte bloqué automatiquement
```

## Visualisation dans l'Interface Admin

Les administrateurs peuvent voir:

1. **Liste des utilisateurs** avec leur score de risque
2. **Détails de l'analyse** pour chaque utilisateur
3. **Historique des détections** de fraude
4. **Statistiques** (nombre de fraudes détectées, etc.)

## Ajustement des Seuils

Vous pouvez ajuster les seuils dans `FraudDetectionService.java`:

```java
// Seuils de décision
private static final double FRAUD_THRESHOLD = 70.0;  // Score > 70 = frauduleux
private static final double REVIEW_THRESHOLD = 40.0; // Score > 40 = à examiner
```

## Ajustement des Poids

Vous pouvez ajuster le poids de chaque indicateur:

```java
// Dans les méthodes check*()
return new FraudIndicator("EMAIL", description, 0.25, isSuspicious);  // 25%
return new FraudIndicator("NAME", description, 0.20, isSuspicious);   // 20%
return new FraudIndicator("PHONE", description, 0.15, isSuspicious);  // 15%
// etc.
```

## Avantages

1. **Automatique** - Aucune intervention manuelle requise
2. **Rapide** - Analyse en quelques millisecondes
3. **Précis** - Détecte 7 types d'indicateurs différents
4. **Transparent** - Explications claires pour chaque décision
5. **Configurable** - Seuils et poids ajustables
6. **Traçable** - Tous les résultats sont sauvegardés

## Limitations

1. **Faux positifs possibles** - Certains utilisateurs légitimes peuvent être marqués
2. **Évolution nécessaire** - Les fraudeurs peuvent adapter leurs techniques
3. **Données limitées** - Analyse basée uniquement sur les données d'inscription

## Améliorations Futures

1. **Machine Learning** - Entraînement sur des données historiques
2. **Analyse comportementale avancée** - Temps de remplissage, mouvements de souris
3. **Vérification externe** - APIs de vérification d'email/téléphone
4. **Scoring dynamique** - Ajustement automatique des poids

## Support

- 📄 Documentation complète: `FONCTIONNALITE_DETECTION_FRAUDE_IA.md`
- 🧪 Script de test: `test-fraud-detection.bat`
- 💾 Script SQL: `database_fraud_detection.sql`

## Conclusion

Vous avez maintenant une fonctionnalité avancée d'IA qui améliore significativement la sécurité de votre plateforme. C'est une excellente démonstration de l'utilisation pratique de l'IA dans un contexte réel!

**Prochaine étape:** Créer la table dans la base de données et tester avec de vraies inscriptions dans l'application.
