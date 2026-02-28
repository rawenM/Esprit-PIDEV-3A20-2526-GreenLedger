# 📧 Services d'envoi d'emails - Documentation

## Vue d'ensemble

GreenLedger dispose de 3 services d'envoi d'emails :

1. **GmailApiService** - Service principal utilisant l'API Gmail (recommandé)
2. **EmailService** - Service SMTP de fallback
3. **UnifiedEmailService** - Service unifié avec fallback automatique

---

## 🎯 Quel service utiliser ?

### UnifiedEmailService (Recommandé)

Utilisez ce service dans votre code. Il choisit automatiquement le meilleur service disponible :

```java
import Utils.UnifiedEmailService;

public class MyController {
    private final UnifiedEmailService emailService = new UnifiedEmailService();
    
    public void sendEmail() {
        emailService.sendWelcomeEmail("user@example.com", "Jean Dupont");
    }
}
```

**Avantages :**
- Utilise Gmail API si configuré (gratuit, fiable)
- Fallback automatique sur SMTP si Gmail API n'est pas disponible
- API simple et unifiée
- Pas besoin de gérer la logique de fallback

---

## 📝 Services disponibles

### 1. GmailApiService

Service d'envoi via l'API Gmail (OAuth2).

**Configuration requise :**
```env
GMAIL_API_ENABLED=true
GMAIL_FROM_EMAIL=votre.email@gmail.com
GMAIL_FROM_NAME=GreenLedger Team
```

**Fichiers requis :**
- `src/main/resources/credentials.json` (téléchargé depuis Google Cloud Console)

**Utilisation :**
```java
import Utils.GmailApiService;

GmailApiService gmailService = new GmailApiService();
if (gmailService.isConfigured()) {
    gmailService.sendWelcomeEmail("user@example.com", "Jean Dupont");
}
```

**Méthodes disponibles :**
- `sendWelcomeEmail(email, fullName)`
- `sendVerificationEmail(email, fullName, token)`
- `sendResetPasswordEmail(email, fullName, token)`
- `sendAccountApprovedEmail(email, fullName)`
- `sendAccountRejectedEmail(email, fullName, reason)`
- `sendAccountBlockedEmail(email, fullName, reason)`
- `sendAccountUnblockedEmail(email, fullName)`

---

### 2. EmailService (SMTP)

Service d'envoi via SMTP (fallback).

**Configuration requise :**
```env
SMTP_HOST=smtp.gmail.com
SMTP_PORT=587
SMTP_USERNAME=your_email@example.com
SMTP_PASSWORD=your_app_password
SMTP_FROM=GreenLedger Team <your_email@example.com>
SMTP_AUTH=true
SMTP_STARTTLS=true
```

**Utilisation :**
```java
import Utils.EmailService;

EmailService emailService = new EmailService();
if (emailService.isConfigured()) {
    emailService.sendWelcomeEmail("user@example.com", "Jean Dupont");
}
```

**Méthodes disponibles :**
- `sendWelcomeEmail(email, fullName)`
- `sendResetEmail(email, token)`
- `sendAccountStatusEmail(email, fullName, status)`

---

### 3. UnifiedEmailService

Service unifié avec fallback automatique.

**Configuration :**
Configurez Gmail API OU SMTP (ou les deux pour redondance).

**Utilisation :**
```java
import Utils.UnifiedEmailService;

UnifiedEmailService emailService = new UnifiedEmailService();
emailService.sendWelcomeEmail("user@example.com", "Jean Dupont");
```

**Logique de fallback :**
1. Essaie Gmail API si configuré
2. Sinon, utilise SMTP
3. Sinon, simule l'envoi (logs uniquement)

**Méthodes disponibles :**
- `sendWelcomeEmail(email, fullName)`
- `sendVerificationEmail(email, fullName, token)`
- `sendResetPasswordEmail(email, fullName, token)`
- `sendAccountApprovedEmail(email, fullName)`
- `sendAccountRejectedEmail(email, fullName, reason)`
- `sendAccountBlockedEmail(email, fullName, reason)`
- `sendAccountUnblockedEmail(email, fullName)`
- `sendAccountStatusEmail(email, fullName, status)` - Méthode générique

---

## 🚀 Configuration rapide

### Option 1 : Gmail API (Recommandé)

1. Suivez le guide [GMAIL_QUICK_START.md](GMAIL_QUICK_START.md)
2. Configurez `.env` :
```env
GMAIL_API_ENABLED=true
GMAIL_FROM_EMAIL=votre.email@gmail.com
GMAIL_FROM_NAME=GreenLedger Team
```

### Option 2 : SMTP (Fallback)

1. Générez un mot de passe d'application Gmail
2. Configurez `.env` :
```env
SMTP_HOST=smtp.gmail.com
SMTP_PORT=587
SMTP_USERNAME=votre.email@gmail.com
SMTP_PASSWORD=votre_mot_de_passe_application
SMTP_FROM=GreenLedger Team <votre.email@gmail.com>
SMTP_AUTH=true
SMTP_STARTTLS=true
```

---

## 📊 Comparaison des services

| Critère | Gmail API | SMTP |
|---------|-----------|------|
| **Configuration** | OAuth2 + credentials.json | Username + Password |
| **Sécurité** | Très élevée (OAuth2) | Moyenne (mot de passe) |
| **Limite d'envoi** | 500/jour (Gmail standard) | 500/jour |
| | 2000/jour (Workspace) | 2000/jour (Workspace) |
| **Fiabilité** | Excellente | Bonne |
| **Complexité** | Moyenne | Simple |
| **Coût** | Gratuit | Gratuit |

---

## 🔧 Migration du code existant

### Si vous utilisez EmailService directement

**Avant :**
```java
import Utils.EmailService;

EmailService emailService = new EmailService();
emailService.sendWelcomeEmail(email, fullName);
```

**Après :**
```java
import Utils.UnifiedEmailService;

UnifiedEmailService emailService = new UnifiedEmailService();
emailService.sendWelcomeEmail(email, fullName);
```

### Si vous utilisez SendGridEmailService (ancien)

**Avant :**
```java
import Utils.SendGridEmailService;

SendGridEmailService sendGridService = new SendGridEmailService();
sendGridService.sendWelcomeEmail(email, fullName);
```

**Après :**
```java
import Utils.UnifiedEmailService;

UnifiedEmailService emailService = new UnifiedEmailService();
emailService.sendWelcomeEmail(email, fullName);
```

---

## 🐛 Dépannage

### Aucun email n'est envoyé

1. Vérifiez les logs :
```
[UnifiedEmail] Utilisation de Gmail API pour les emails
[Gmail API] Email envoyé avec succès à: user@example.com
```

2. Vérifiez la configuration :
```java
UnifiedEmailService emailService = new UnifiedEmailService();
if (!emailService.isConfigured()) {
    System.out.println("Aucun service d'email configuré !");
}
```

### Gmail API ne fonctionne pas

1. Vérifiez `GMAIL_API_ENABLED=true` dans `.env`
2. Vérifiez que `credentials.json` existe dans `src/main/resources/`
3. Vérifiez que l'authentification OAuth2 a été effectuée (dossier `tokens/`)

### SMTP ne fonctionne pas

1. Vérifiez les credentials SMTP dans `.env`
2. Vérifiez que vous utilisez un mot de passe d'application (pas votre mot de passe Gmail)
3. Vérifiez les logs pour les erreurs de connexion

---

## 📚 Documentation complète

- [GMAIL_QUICK_START.md](GMAIL_QUICK_START.md) - Configuration Gmail API en 5 minutes
- [GMAIL_API_SETUP_GUIDE.md](GMAIL_API_SETUP_GUIDE.md) - Guide complet Gmail API
- [GMAIL_MIGRATION_SUMMARY.md](GMAIL_MIGRATION_SUMMARY.md) - Résumé de la migration SendGrid → Gmail

---

## ✅ Checklist

- [ ] Service d'email configuré (Gmail API ou SMTP)
- [ ] Variables d'environnement dans `.env`
- [ ] Code mis à jour pour utiliser `UnifiedEmailService`
- [ ] Tests d'envoi effectués
- [ ] Emails reçus avec succès

---

## 🎉 Prêt à envoyer des emails !

Utilisez `UnifiedEmailService` dans votre code et laissez le système choisir automatiquement le meilleur service disponible ! 🚀
