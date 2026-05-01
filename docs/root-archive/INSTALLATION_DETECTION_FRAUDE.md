# Installation de la Détection de Fraude

## Statut Actuel

✅ **Code compilé et prêt**
- FraudDetectionResult.java
- FraudDetectionService.java
- FraudDetectionDAOImpl.java
- UserServiceImpl.java (avec intégration)

✅ **Application testée**
- L'application démarre correctement
- Gmail API fonctionne
- 10 utilisateurs chargés

⏳ **Reste à faire**
- Créer la table `fraud_detection_results` dans MySQL
- Ajouter les colonnes `fraud_score` et `fraud_checked` à la table `utilisateurs`

## Étape 1: Ouvrir MySQL

### Option A: Via phpMyAdmin
1. Ouvrez votre navigateur
2. Allez sur: http://localhost/phpmyadmin
3. Connectez-vous avec vos identifiants
4. Sélectionnez la base de données `green_wallet`
5. Cliquez sur l'onglet "SQL"

### Option B: Via Ligne de Commande
1. Ouvrez l'invite de commande (CMD)
2. Naviguez vers le dossier MySQL bin:
   ```
   cd C:\xampp\mysql\bin
   ```
   ou
   ```
   cd C:\wamp64\bin\mysql\mysql8.0.x\bin
   ```
3. Connectez-vous à MySQL:
   ```
   mysql -u root -p
   ```
4. Sélectionnez la base de données:
   ```
   USE green_wallet;
   ```

## Étape 2: Créer la Table

Copiez et exécutez ce SQL:

```sql
-- Table pour stocker les résultats de détection de fraude
CREATE TABLE IF NOT EXISTS fraud_detection_results (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    risk_score DOUBLE NOT NULL,
    risk_level VARCHAR(20) NOT NULL,
    is_fraudulent BOOLEAN NOT NULL DEFAULT FALSE,
    recommendation VARCHAR(255),
    analysis_details TEXT,
    analyzed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    FOREIGN KEY (user_id) REFERENCES utilisateurs(id) ON DELETE CASCADE,
    INDEX idx_user_id (user_id),
    INDEX idx_risk_level (risk_level),
    INDEX idx_is_fraudulent (is_fraudulent),
    INDEX idx_analyzed_at (analyzed_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

## Étape 3: Ajouter les Colonnes à la Table utilisateurs

```sql
-- Ajouter les colonnes fraud_score et fraud_checked
ALTER TABLE utilisateurs 
ADD COLUMN fraud_score DOUBLE DEFAULT 0.0 AFTER email_verifie,
ADD COLUMN fraud_checked BOOLEAN DEFAULT FALSE AFTER fraud_score;

-- Créer les index pour les recherches rapides
CREATE INDEX idx_fraud_score ON utilisateurs(fraud_score);
CREATE INDEX idx_fraud_checked ON utilisateurs(fraud_checked);
```

## Étape 4: Vérifier l'Installation

```sql
-- Vérifier que la table a été créée
SHOW TABLES LIKE 'fraud_detection_results';

-- Vérifier la structure de la table
DESCRIBE fraud_detection_results;

-- Vérifier que les colonnes ont été ajoutées
DESCRIBE utilisateurs;
```

Vous devriez voir:
- Table `fraud_detection_results` avec 8 colonnes
- Colonnes `fraud_score` et `fraud_checked` dans la table `utilisateurs`

## Étape 5: Tester la Détection de Fraude

### Test 1: Script de Test
```bash
test-fraud-detection.bat
```

Résultat attendu:
```
=== Test Détection de Fraude avec IA ===

--- Test 1: Utilisateur Légitime ---
Score de risque: 0,0/100
Niveau: Faible
Recommandation: APPROUVER

--- Test 7: Multiples Indicateurs ---
Score de risque: 70,0/100
Niveau: Eleve
Recommandation: REJETER
```

### Test 2: Dans l'Application

1. Lancez l'application:
   ```bash
   run.bat
   ```

2. Créez un nouvel utilisateur avec des données suspectes:
   - Nom: Test
   - Prénom: Fake
   - Email: test@tempmail.com
   - Téléphone: 1111111111

3. Vérifiez les logs dans la console:
   ```
   [FraudDetection] Analyse de l'inscription...
   [FraudDetection] Score de risque: XX.X/100
   [FraudDetection] Niveau: XXX
   [FraudDetection] Recommandation: XXX
   ```

4. Si le score > 70, le compte sera automatiquement bloqué:
   ```
   [FraudDetection] ALERTE: Score de risque critique - Compte bloqué automatiquement
   ```

## Étape 6: Vérifier les Résultats dans la Base de Données

```sql
-- Voir tous les résultats de détection
SELECT * FROM fraud_detection_results ORDER BY analyzed_at DESC;

-- Voir les utilisateurs avec leur score de fraude
SELECT id, nom, prenom, email, fraud_score, fraud_checked, statut 
FROM utilisateurs 
ORDER BY fraud_score DESC;

-- Voir les détections frauduleuses
SELECT u.nom, u.prenom, u.email, f.risk_score, f.risk_level, f.recommendation
FROM utilisateurs u
JOIN fraud_detection_results f ON u.id = f.user_id
WHERE f.is_fraudulent = TRUE;
```

## Dépannage

### Erreur: Table 'fraud_detection_results' doesn't exist

**Solution:** Exécutez le SQL de l'Étape 2

### Erreur: Column 'fraud_score' doesn't exist

**Solution:** Exécutez le SQL de l'Étape 3

### Erreur: Cannot add foreign key constraint

**Cause:** La table `utilisateurs` n'existe pas ou n'a pas de colonne `id`

**Solution:** Vérifiez que votre base de données est correctement configurée

### L'analyse ne se déclenche pas

**Vérifications:**
1. Les classes sont compilées: `./compile-services.bat`
2. La table existe dans MySQL
3. Les logs montrent: `[FraudDetection] Analyse de l'inscription...`

## Fichiers Créés

- ✅ `src/main/java/Models/FraudDetectionResult.java`
- ✅ `src/main/java/Services/FraudDetectionService.java`
- ✅ `src/main/java/dao/IFraudDetectionDAO.java`
- ✅ `src/main/java/dao/FraudDetectionDAOImpl.java`
- ✅ `src/main/java/Services/UserServiceImpl.java` (modifié)
- ✅ `src/main/java/tools/TestFraudDetection.java`
- ✅ `database_fraud_detection.sql`
- ✅ `test-fraud-detection.bat`

## Documentation

- 📄 `FONCTIONNALITE_DETECTION_FRAUDE_IA.md` - Documentation complète
- 📄 `GUIDE_DEMARRAGE_DETECTION_FRAUDE.md` - Guide de démarrage
- 📄 `INSTALLATION_DETECTION_FRAUDE.md` - Ce fichier

## Prochaines Étapes

1. ✅ Compiler les classes (fait)
2. ⏳ Créer la table dans MySQL (à faire maintenant)
3. ⏳ Tester avec de vraies inscriptions
4. ⏳ Ajuster les seuils si nécessaire
5. ⏳ Créer l'interface d'administration pour visualiser les scores

## Support

Si vous rencontrez des problèmes:
1. Vérifiez les logs de l'application
2. Vérifiez que MySQL est démarré
3. Vérifiez que la base de données `green_wallet` existe
4. Testez avec `test-fraud-detection.bat`

---

**Une fois la table créée, la détection de fraude sera automatiquement active pour toutes les nouvelles inscriptions!** 🎉
