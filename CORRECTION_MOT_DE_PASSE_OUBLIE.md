# Correction: Fonctionnalité "Mot de Passe Oublié"

## Problème Identifié

L'utilisateur ne recevait pas d'email lors de l'utilisation de la fonctionnalité "Mot de passe oublié" dans l'application. Seul le token était affiché dans une boîte de dialogue.

## Cause du Problème

Le service `GmailApiService` utilisait `System.getenv()` pour lire les variables d'environnement, mais le fichier `.env` n'était pas chargé automatiquement par Java. Les variables d'environnement étaient définies dans `run.bat`, mais elles n'étaient pas toujours disponibles lors de l'exécution de l'application.

## Solutions Appliquées

### 1. Création de `EnvLoader.java`

Création d'une classe utilitaire pour charger automatiquement le fichier `.env` :

**Fichier:** `src/main/java/Utils/EnvLoader.java`

Cette classe :
- Charge le fichier `.env` depuis la racine du projet
- Rend les variables disponibles via `System.getProperty()`
- Fournit une méthode `get()` pour accéder aux variables
- Supporte plusieurs emplacements de fichier `.env`

### 2. Mise à Jour de `GmailApiService.java`

Modification du constructeur pour utiliser `EnvLoader` :

```java
public GmailApiService() {
    // Load .env file first
    EnvLoader.load();
    
    this.enabled = Boolean.parseBoolean(EnvLoader.get("GMAIL_API_ENABLED", "false"));
    this.fromEmail = EnvLoader.get("GMAIL_FROM_EMAIL", "");
    this.fromName = EnvLoader.get("GMAIL_FROM_NAME", "GreenLedger Team");
    // ...
}
```

### 3. Correction du Fichier `.env`

Mise à jour de l'email dans le fichier `.env` :

```env
GMAIL_API_ENABLED=true
GMAIL_FROM_EMAIL=ibrahimimajid058@gmail.com
GMAIL_FROM_NAME=GreenLedger Team
```

### 4. Ajout de Logs Détaillés

Ajout de logs dans `UserServiceImpl.initiatePasswordReset()` pour faciliter le débogage :

```java
System.out.println("[INFO] Tentative d'envoi d'email de reinitialisation...");
System.out.println("[INFO] Email service configured: " + emailService.isConfigured());
System.out.println("[INFO] Resultat envoi email: " + sent);
```

## Test de la Correction

### Test 1: Script de Test (Confirmé ✅)

```bash
./test-reset-password.bat
```

**Résultat:**
```
[EnvLoader] Loaded 22 variables from .env
[Gmail API] Service initialisé avec succès
[UnifiedEmail] Utilisation de Gmail API pour les emails
OK Service email configure
[Gmail API] Email envoyé avec succès à : ibrahimimajid058@gmail.com
OK Email envoye avec succes !
```

### Test 2: Dans l'Application

1. Lancer l'application avec `run.bat`
2. Cliquer sur "Mot de passe oublié"
3. Entrer l'email: `ibrahimimajid058@gmail.com`
4. Vérifier les logs dans la console
5. Vérifier la réception de l'email

**Logs Attendus:**
```
[EnvLoader] Loaded 22 variables from .env
[Gmail API] Service initialisé avec succès
[UnifiedEmail] Utilisation de Gmail API pour les emails
[INFO] Tentative d'envoi d'email de reinitialisation...
[INFO] Email service configured: true
[INFO] Resultat envoi email: true
[OK] Email de reinitialisation envoye a: ibrahimimajid058@gmail.com
```

## Flux Complet de la Fonctionnalité

### 1. Utilisateur Clique sur "Mot de passe oublié"

**Fichier:** `src/main/java/Controllers/LoginController.java`
- Méthode: `handleForgotPassword()`
- Affiche une boîte de dialogue pour entrer l'email

### 2. Génération du Token

**Fichier:** `src/main/java/Services/UserServiceImpl.java`
- Méthode: `initiatePasswordReset(String emailOrPhone)`
- Génère un token UUID unique
- Hash le token avec BCrypt pour sécurité
- Définit l'expiration à 1 heure
- Sauvegarde dans la base de données

### 3. Envoi de l'Email

**Fichier:** `src/main/java/Utils/UnifiedEmailService.java`
- Utilise Gmail API en priorité
- Fallback vers SMTP si Gmail API non disponible
- Appelle `GmailApiService.sendResetPasswordEmail()`

### 4. Gmail API Envoie l'Email

**Fichier:** `src/main/java/Utils/GmailApiService.java`
- Charge les credentials OAuth2
- Crée un email HTML avec le lien de réinitialisation
- Envoie via l'API Gmail
- Retourne `true` si succès

### 5. Affichage du Résultat

**Fichier:** `src/main/java/Controllers/LoginController.java`
- Affiche une alerte de confirmation
- Affiche le token (pour test local)
- Propose d'ouvrir le formulaire de réinitialisation

## Configuration Requise

### Variables d'Environnement (dans `.env`)

```env
GMAIL_API_ENABLED=true
GMAIL_FROM_EMAIL=ibrahimimajid058@gmail.com
GMAIL_FROM_NAME=GreenLedger Team
```

### Fichiers Requis

1. `src/main/resources/credentials.json` - Credentials OAuth2 Gmail API
2. `tokens/` - Dossier contenant les tokens OAuth2 (créé automatiquement)
3. `.env` - Variables d'environnement

### Dépendances Maven

```xml
<dependency>
    <groupId>com.google.api-client</groupId>
    <artifactId>google-api-client</artifactId>
    <version>2.2.0</version>
</dependency>
<dependency>
    <groupId>com.google.oauth-client</groupId>
    <artifactId>google-oauth-client-jetty</artifactId>
    <version>1.34.1</version>
</dependency>
<dependency>
    <groupId>com.google.apis</groupId>
    <artifactId>google-api-services-gmail</artifactId>
    <version>v1-rev20220404-2.0.0</version>
</dependency>
```

## Sécurité

### Token de Réinitialisation

- **Format:** UUID v4 (128 bits d'entropie)
- **Stockage:** Hash BCrypt dans la base de données
- **Expiration:** 1 heure
- **Usage unique:** Le token est supprimé après utilisation

### Email

- **Authentification:** OAuth2 (plus sécurisé que mot de passe)
- **Protocole:** HTTPS via Gmail API
- **Contenu:** Lien avec token, pas de mot de passe

## Débogage

### Si l'Email n'est Pas Envoyé

1. Vérifier les logs dans la console
2. Vérifier que `[EnvLoader] Loaded X variables from .env` apparaît
3. Vérifier que `[Gmail API] Service initialisé avec succès` apparaît
4. Vérifier que `[UnifiedEmail] Utilisation de Gmail API pour les emails` apparaît
5. Vérifier que `[INFO] Email service configured: true` apparaît

### Si Gmail API n'est Pas Configuré

```
[Gmail API] Service désactivé
[UnifiedEmail] Utilisation de SMTP pour les emails (fallback)
```

**Solution:** Vérifier le fichier `.env` et `credentials.json`

### Si Aucun Service n'est Configuré

```
[UnifiedEmail] Aucun service d'email configuré
[WARN] Service email non configure - email non envoye
```

**Solution:** Configurer Gmail API ou SMTP

## Fichiers Modifiés

1. ✅ `src/main/java/Utils/EnvLoader.java` (NOUVEAU)
2. ✅ `src/main/java/Utils/GmailApiService.java` (MODIFIÉ)
3. ✅ `src/main/java/Services/UserServiceImpl.java` (MODIFIÉ - logs)
4. ✅ `.env` (MODIFIÉ - email corrigé)

## Fichiers Compilés

```bash
./compile-services.bat  # UserServiceImpl
./compile-gmail.bat     # GmailApiService, UnifiedEmailService
javac -d target/classes -cp "target/classes" src/main/java/Utils/EnvLoader.java
```

## Prochaines Étapes

1. ✅ Corriger le fichier `.env` avec le bon email
2. ✅ Tester avec `test-reset-password.bat` (CONFIRMÉ - Email envoyé avec succès)
3. ⏳ Lancer l'application avec `run.bat`
4. ⏳ Tester la fonctionnalité "Mot de passe oublié" dans l'interface
5. ⏳ Vérifier la réception de l'email dans la boîte `ibrahimimajid058@gmail.com`
6. ⏳ Tester la réinitialisation complète du mot de passe

## Instructions pour l'Utilisateur

### Pour Tester Maintenant:

1. **Lancer l'application:**
   ```bash
   run.bat
   ```

2. **Dans l'interface de connexion:**
   - Cliquer sur "Mot de passe oublié"
   - Entrer votre email: `ibrahimimajid058@gmail.com`
   - Cliquer sur OK

3. **Vérifier les logs dans la console:**
   Vous devriez voir:
   ```
   [EnvLoader] Loaded 22 variables from .env
   [Gmail API] Service initialisé avec succès
   [UnifiedEmail] Utilisation de Gmail API pour les emails
   [INFO] Tentative d'envoi d'email de reinitialisation...
   [INFO] Email service configured: true
   [INFO] Resultat envoi email: true
   [OK] Email de reinitialisation envoye a: ibrahimimajid058@gmail.com
   ```

4. **Vérifier votre boîte email:**
   - Ouvrir Gmail: https://mail.google.com
   - Chercher un email de "GreenLedger Team"
   - Sujet: "Reinitialisation de votre mot de passe"
   - Cliquer sur le lien dans l'email

5. **Réinitialiser le mot de passe:**
   - Entrer le nouveau mot de passe (minimum 8 caractères)
   - Confirmer le nouveau mot de passe
   - Cliquer sur "Réinitialiser"

### Si l'Email n'Arrive Pas:

1. **Vérifier le dossier Spam/Courrier indésirable**
2. **Vérifier les logs de la console** pour voir si l'email a été envoyé
3. **Vérifier que Gmail API est bien configuré:**
   ```bash
   test-env-loader.bat
   ```
   Devrait afficher:
   ```
   GMAIL_API_ENABLED = true
   GMAIL_FROM_EMAIL = ibrahimimajid058@gmail.com
   ```

4. **Tester l'envoi d'email directement:**
   ```bash
   test-reset-password.bat
   ```

## Notes

- Le test script fonctionne parfaitement ✅
- L'email est envoyé avec succès à `ibrahimimajid058@gmail.com` ✅
- Le token est généré et stocké correctement ✅
- Le lien de réinitialisation est inclus dans l'email ✅
- L'expiration est fixée à 1 heure ✅

## Documentation Complète

Voir `FONCTIONNALITE_MOT_DE_PASSE_OUBLIE.md` pour la documentation complète de la fonctionnalité.
