# 📧 Système d'envoi d'emails - GreenLedger

## 🎯 Résumé rapide

GreenLedger utilise maintenant **Gmail API** pour envoyer des emails (au lieu de SendGrid/Twilio).

**Avantages :**
- ✅ Gratuit et sans limite stricte
- ✅ Pas de numéro de téléphone requis
- ✅ Utilise votre compte Gmail existant
- ✅ Configuration en 5 minutes

---

## 🚀 Démarrage rapide

### 1. Configuration (5 minutes)

Suivez le guide : **[GMAIL_QUICK_START.md](GMAIL_QUICK_START.md)**

**Résumé :**
1. Créez un projet sur [Google Cloud Console](https://console.cloud.google.com/)
2. Activez l'API Gmail
3. Créez un ID client OAuth2
4. Téléchargez `credentials.json` → placez dans `src/main/resources/`
5. Configurez `.env` :

```env
GMAIL_API_ENABLED=true
GMAIL_FROM_EMAIL=votre.email@gmail.com
GMAIL_FROM_NAME=GreenLedger Team
```

### 2. Compilation

```bash
mvn clean compile
```

### 3. Test

```bash
java -cp target/classes tools.TestGmailApi
```

---

## 💻 Utilisation dans le code

```java
import Utils.UnifiedEmailService;

public class MyController {
    private final UnifiedEmailService emailService = new UnifiedEmailService();
    
    public void sendEmail() {
        emailService.sendWelcomeEmail("user@example.com", "Jean Dupont");
    }
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

## 📚 Documentation complète

| Fichier | Description |
|---------|-------------|
| **[LISEZ_MOI_EMAILS.txt](LISEZ_MOI_EMAILS.txt)** | Guide simple de démarrage |
| **[GMAIL_QUICK_START.md](GMAIL_QUICK_START.md)** | Configuration en 5 minutes |
| **[GMAIL_API_SETUP_GUIDE.md](GMAIL_API_SETUP_GUIDE.md)** | Guide complet et détaillé |
| **[EMAIL_SERVICES_README.md](EMAIL_SERVICES_README.md)** | Documentation des services |
| **[CHANGEMENTS_EMAILS.md](CHANGEMENTS_EMAILS.md)** | Liste des changements |

---

## 🔧 Services disponibles

### UnifiedEmailService (Recommandé)
Service unifié qui choisit automatiquement le meilleur service :
1. Gmail API (si configuré)
2. SMTP (fallback)
3. Simulation (logs)

### GmailApiService
Service principal utilisant l'API Gmail avec OAuth2.

### EmailService
Service SMTP de fallback (déjà existant).

---

## 🐛 Dépannage

### Gmail API ne fonctionne pas
1. Vérifiez `GMAIL_API_ENABLED=true` dans `.env`
2. Vérifiez que `credentials.json` existe dans `src/main/resources/`
3. Supprimez le dossier `tokens/` et réautorisez

### Emails non reçus
1. Vérifiez le dossier spam
2. Vérifiez les logs : `[Gmail API]` ou `[UnifiedEmail]`
3. Testez avec `TestGmailApi.java`

---

## ✅ Checklist

- [ ] Gmail API configuré (voir [GMAIL_QUICK_START.md](GMAIL_QUICK_START.md))
- [ ] `credentials.json` dans `src/main/resources/`
- [ ] Variables d'environnement dans `.env`
- [ ] Projet compilé (`mvn clean compile`)
- [ ] Test d'envoi réussi
- [ ] Code mis à jour pour utiliser `UnifiedEmailService`

---

## 🎉 Prêt !

Votre application peut maintenant envoyer des emails via Gmail API ! 🚀

**Commencez ici :** [GMAIL_QUICK_START.md](GMAIL_QUICK_START.md)
