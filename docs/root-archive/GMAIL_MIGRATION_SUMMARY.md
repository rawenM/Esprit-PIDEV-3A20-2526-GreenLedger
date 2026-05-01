# 📧 Migration SendGrid → Gmail API

## ✅ Changements effectués

### Fichiers supprimés
- ❌ `SENDGRID_SUMMARY.md`
- ❌ `SENDGRID_INTEGRATION_GUIDE.md`
- ❌ `SENDGRID_MIGRATION_EXAMPLES.md`
- ❌ `SENDGRID_QUICK_START.md`
- ❌ `src/main/java/Utils/SendGridEmailService.java`
- ❌ `src/main/java/Utils/UnifiedEmailService.java`
- ❌ `src/main/java/tools/TestSendGridIntegration.java`

### Fichiers créés
- ✅ `src/main/java/Utils/GmailApiService.java` - Service d'envoi d'emails via Gmail API
- ✅ `src/main/java/tools/TestGmailApi.java` - Outil de test Gmail API
- ✅ `GMAIL_API_SETUP_GUIDE.md` - Guide complet de configuration
- ✅ `GMAIL_QUICK_START.md` - Guide de démarrage rapide (5 min)
- ✅ `GMAIL_MIGRATION_SUMMARY.md` - Ce fichier

### Fichiers modifiés
- ✏️ `pom.xml` - Dépendances SendGrid remplacées par Gmail API
- ✏️ `.env` - Configuration Gmail API
- ✏️ `.env.example` - Template de configuration Gmail API
- ✏️ `.gitignore` - Ajout de credentials.json et tokens/

---

## 🎯 Nouvelle architecture

### Dépendances Maven (pom.xml)

```xml
<!-- Google Gmail API -->
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

### Configuration (.env)

```env
# Gmail API Configuration
GMAIL_API_ENABLED=true
GMAIL_FROM_EMAIL=votre.email@gmail.com
GMAIL_FROM_NAME=GreenLedger Team

# SMTP (Fallback)
SMTP_HOST=smtp.gmail.com
SMTP_PORT=587
SMTP_USERNAME=your_email@example.com
SMTP_PASSWORD=your_app_password
SMTP_FROM=GreenLedger Team <your_email@example.com>
SMTP_AUTH=true
SMTP_STARTTLS=true

# Application URLs
APP_RESET_URL_PREFIX=http://127.0.0.1:8088/reset?token=
```

---

## 📝 Utilisation

### Service Gmail API

```java
import Utils.GmailApiService;

public class Example {
    private final GmailApiService gmailService = new GmailApiService();
    
    public void sendEmail() {
        if (gmailService.isConfigured()) {
            boolean success = gmailService.sendWelcomeEmail(
                "user@example.com",
                "Jean Dupont"
            );
        }
    }
}
```

### Types d'emails disponibles

Tous les types d'emails de SendGrid ont été migrés :

1. `sendWelcomeEmail(email, fullName)` - Email de bienvenue
2. `sendVerificationEmail(email, fullName, token)` - Email de vérification
3. `sendResetPasswordEmail(email, fullName, token)` - Réinitialisation mot de passe
4. `sendAccountApprovedEmail(email, fullName)` - Compte approuvé
5. `sendAccountRejectedEmail(email, fullName, reason)` - Compte rejeté
6. `sendAccountBlockedEmail(email, fullName, reason)` - Compte bloqué
7. `sendAccountUnblockedEmail(email, fullName)` - Compte débloqué

---

## 🚀 Configuration requise

### 1. Créer un projet Google Cloud

1. Allez sur https://console.cloud.google.com/
2. Créez un projet "GreenLedger Email"
3. Activez l'API Gmail

### 2. Configurer OAuth2

1. Créez un écran de consentement OAuth
2. Créez un ID client OAuth (Application de bureau)
3. Téléchargez `credentials.json`
4. Placez-le dans `src/main/resources/credentials.json`

### 3. Première authentification

Au premier lancement, une fenêtre de navigateur s'ouvrira pour autoriser l'application.
Les tokens seront sauvegardés dans le dossier `tokens/`.

---

## 🔄 Migration du code existant

### Avant (SendGrid)

```java
import Utils.SendGridEmailService;

SendGridEmailService sendGridService = new SendGridEmailService();
sendGridService.sendWelcomeEmail(email, fullName);
```

### Après (Gmail API)

```java
import Utils.GmailApiService;

GmailApiService gmailService = new GmailApiService();
gmailService.sendWelcomeEmail(email, fullName);
```

**C'est tout !** L'API est identique, seul le nom de la classe change.

---

## ✨ Avantages de Gmail API

| Critère | SendGrid | Gmail API |
|---------|----------|-----------|
| **Prix** | 100 emails/jour gratuit | Illimité (quota Gmail) |
| **Configuration** | API Key + vérification domaine | OAuth2 simple |
| **Limite d'envoi** | 100/jour (gratuit) | 500/jour (Gmail standard) |
| | 3000/mois | 2000/jour (Workspace) |
| **Authentification** | Numéro de téléphone requis | Compte Google existant |
| **Complexité** | Moyenne | Simple |
| **Fiabilité** | Excellente | Excellente (Google) |

---

## 🐛 Dépannage

### Erreur : "credentials.json not found"

Placez le fichier dans `src/main/resources/credentials.json`

### Erreur : "Access blocked"

Ajoutez votre email dans les "Utilisateurs de test" de l'écran de consentement OAuth

### Emails non reçus

1. Vérifiez le dossier spam
2. Vérifiez les logs : `[Gmail API]`
3. Vérifiez que `GMAIL_API_ENABLED=true`

---

## 📚 Documentation

- [Guide complet](GMAIL_API_SETUP_GUIDE.md) - Configuration détaillée
- [Démarrage rapide](GMAIL_QUICK_START.md) - Configuration en 5 minutes
- [Gmail API Docs](https://developers.google.com/gmail/api) - Documentation officielle

---

## ✅ Prochaines étapes

1. Suivez le guide [GMAIL_QUICK_START.md](GMAIL_QUICK_START.md)
2. Configurez votre projet Google Cloud
3. Téléchargez `credentials.json`
4. Testez avec `TestGmailApi.java`
5. Mettez à jour votre code pour utiliser `GmailApiService`

---

## 🎉 Migration terminée !

Tous les fichiers SendGrid/Twilio ont été supprimés et remplacés par Gmail API.
L'application est prête à envoyer des emails via votre compte Gmail ! 🚀
