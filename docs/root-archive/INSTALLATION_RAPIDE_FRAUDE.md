# Installation Rapide - Détection de Fraude

## ✅ Adapté pour votre base de données `greenledger`

---

## Étape 1: Ouvrir phpMyAdmin

1. Ouvrez votre navigateur
2. Allez sur: `http://localhost/phpmyadmin`
3. Connectez-vous (généralement: user=root, password=vide)

---

## Étape 2: Sélectionner la Base de Données

1. Dans le menu de gauche, cliquez sur **`greenledger`**
2. Cliquez sur l'onglet **"SQL"** en haut

---

## Étape 3: Exécuter le Script SQL

1. Ouvrez le fichier **`database_fraud_detection.sql`** avec un éditeur de texte
2. Copiez TOUT le contenu (Ctrl+A puis Ctrl+C)
3. Collez dans la zone SQL de phpMyAdmin (Ctrl+V)
4. Cliquez sur **"Exécuter"** en bas à droite

---

## Étape 4: Vérifier l'Installation

### Option A: Via phpMyAdmin

1. Dans le menu de gauche, actualisez la liste des tables (F5)
2. Vous devriez voir une nouvelle table: **`fraud_detection_results`**
3. Cliquez sur la table **`user`**
4. Cliquez sur l'onglet **"Structure"**
5. Vérifiez que vous avez 2 nouvelles colonnes:
   - `fraud_score` (DOUBLE)
   - `fraud_checked` (BOOLEAN)

### Option B: Via Script SQL

1. Dans l'onglet SQL, collez ce code:
```sql
USE greenledger;

-- Vérifier la table
SHOW TABLES LIKE 'fraud_detection_results';

-- Vérifier les colonnes
DESCRIBE fraud_detection_results;

-- Vérifier les nouvelles colonnes de user
SELECT COLUMN_NAME, COLUMN_TYPE 
FROM INFORMATION_SCHEMA.COLUMNS 
WHERE TABLE_SCHEMA = 'greenledger' 
AND TABLE_NAME = 'user' 
AND COLUMN_NAME IN ('fraud_score', 'fraud_checked');
```

2. Cliquez sur "Exécuter"

**Résultat attendu:**
```
✓ Table fraud_detection_results existe
✓ 8 colonnes dans fraud_detection_results
✓ Colonnes fraud_score et fraud_checked dans user
```

---

## Étape 5: Tester la Fonctionnalité

### Test 1: Script de Test

```bash
test-fraud-detection.bat
```

**Résultat attendu:**
```
=== Test Détection de Fraude avec IA ===

--- Test 1: Utilisateur Légitime ---
Score de risque: 0,0/100
Niveau: Faible
✓ Aucun indicateur de fraude détecté

--- Test 7: Multiples Indicateurs ---
Score de risque: 70,0/100
Niveau: Eleve
⚠️  EMAIL: Email jetable détecté
⚠️  NAME: Nom suspect détecté
⚠️  PHONE: Numéro répétitif
⚠️  ADDRESS: Adresse suspecte
```

### Test 2: Dans l'Application

1. Lancez l'application:
   ```bash
   run.bat
   ```

2. Créez un nouvel utilisateur avec des données suspectes:
   - Nom: **Test**
   - Prénom: **Fake**
   - Email: **test@tempmail.com**
   - Téléphone: **1111111111**
   - Mot de passe: **Test1234!**

3. Vérifiez les logs dans la console:
   ```
   [FraudDetection] Analyse de l'inscription...
   [FraudDetection] Score de risque: XX.X/100
   [FraudDetection] Niveau: XXX
   [FraudDetection] Recommandation: XXX
   ```

4. Si le score > 70:
   ```
   [FraudDetection] ALERTE: Score de risque critique
   Compte bloqué automatiquement
   ```

### Test 3: Vérifier dans la Base de Données

```sql
USE greenledger;

-- Voir les dernières détections
SELECT 
    u.nom,
    u.prenom,
    u.email,
    f.risk_score,
    f.risk_level,
    f.recommendation,
    f.analyzed_at
FROM user u
LEFT JOIN fraud_detection_results f ON u.id = f.user_id
ORDER BY u.id DESC
LIMIT 10;
```

---

## Structure de la Base de Données

### Table: fraud_detection_results

```
+-------------------+--------------+------+-----+-------------------+
| Field             | Type         | Null | Key | Default           |
+-------------------+--------------+------+-----+-------------------+
| id                | bigint       | NO   | PRI | NULL              |
| user_id           | bigint       | NO   | MUL | NULL              |
| risk_score        | double       | NO   |     | NULL              |
| risk_level        | varchar(20)  | NO   | MUL | NULL              |
| is_fraudulent     | tinyint(1)   | NO   | MUL | 0                 |
| recommendation    | varchar(255) | YES  |     | NULL              |
| analysis_details  | text         | YES  |     | NULL              |
| analyzed_at       | timestamp    | YES  | MUL | CURRENT_TIMESTAMP |
+-------------------+--------------+------+-----+-------------------+
```

### Colonnes ajoutées à user

```
+--------------+------------+------+-----+---------+
| Field        | Type       | Null | Key | Default |
+--------------+------------+------+-----+---------+
| fraud_score  | double     | YES  | MUL | 0.0     |
| fraud_checked| tinyint(1) | YES  | MUL | 0       |
+--------------+------------+------+-----+---------+
```

---

## Requêtes Utiles

### Voir tous les résultats de détection

```sql
SELECT * FROM fraud_detection_results ORDER BY analyzed_at DESC;
```

### Voir les utilisateurs avec leur score de fraude

```sql
SELECT 
    id,
    nom,
    prenom,
    email,
    fraud_score,
    fraud_checked,
    statut
FROM user
ORDER BY fraud_score DESC;
```

### Voir les détections frauduleuses

```sql
SELECT 
    u.nom,
    u.prenom,
    u.email,
    f.risk_score,
    f.risk_level,
    f.recommendation,
    f.is_fraudulent
FROM user u
JOIN fraud_detection_results f ON u.id = f.user_id
WHERE f.is_fraudulent = TRUE
ORDER BY f.analyzed_at DESC;
```

### Statistiques de fraude

```sql
SELECT 
    COUNT(*) AS total_analyses,
    SUM(CASE WHEN is_fraudulent = TRUE THEN 1 ELSE 0 END) AS fraudes_detectees,
    AVG(risk_score) AS score_moyen,
    MAX(risk_score) AS score_max,
    MIN(risk_score) AS score_min
FROM fraud_detection_results;
```

---

## Dépannage

### Erreur: Table 'fraud_detection_results' already exists

**Solution:** La table existe déjà, c'est bon! Passez à l'étape de vérification.

### Erreur: Column 'fraud_score' already exists

**Solution:** Les colonnes existent déjà, c'est bon! Passez à l'étape de vérification.

### Erreur: Cannot add foreign key constraint

**Cause:** La table `user` n'existe pas ou n'a pas de colonne `id`

**Solution:** Vérifiez que votre base de données `greenledger` contient bien la table `user`

### L'analyse ne se déclenche pas

**Vérifications:**
1. ✓ Les classes sont compilées: `./compile-services.bat`
2. ✓ La table existe dans MySQL
3. ✓ Les logs montrent: `[FraudDetection] Analyse de l'inscription...`
4. ✓ La base de données s'appelle bien `greenledger`

---

## Temps d'Installation

⏱️ **Temps total: 5 minutes**

- Copier/coller le SQL: 1 minute
- Exécution du script: 10 secondes
- Vérification: 2 minutes
- Test: 2 minutes

---

## Support

Si vous rencontrez des problèmes:

1. Vérifiez que MySQL est démarré
2. Vérifiez que la base `greenledger` existe
3. Vérifiez que la table `user` existe
4. Exécutez `verifier_installation_fraude.sql` pour un diagnostic complet

---

## Fichiers Importants

- ✅ `database_fraud_detection.sql` - Script d'installation (ADAPTÉ)
- ✅ `verifier_installation_fraude.sql` - Script de vérification
- ✅ `test-fraud-detection.bat` - Test de la fonctionnalité
- 📄 `PRESENTATION_DETECTION_FRAUDE_JURY.md` - Pour la présentation

---

**Une fois installé, la détection de fraude sera automatiquement active!** 🎉
