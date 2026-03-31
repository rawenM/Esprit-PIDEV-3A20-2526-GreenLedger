# 📧 Guide de configuration Gmail API

## Vue d'ensemble

Ce guide vous explique comment configurer l'API Gmail pour envoyer des emails depuis GreenLedger.

## Avantages de Gmail API

- ✅ **Gratuit** : Pas de limite d'envoi pour usage personnel
- ✅ **Fiable** : Infrastructure Google
- ✅ **Simple** : Utilise votre compte Gmail existant
- ✅ **Sécurisé** : Authentification OAuth2

---

## 🚀 Configuration étape par étape

### Étape 1 : Créer un projet Google Cloud

1. Allez sur [Google Cloud Console](https://console.cloud.google.com/)
2. Cliquez sur **"Créer un projet"** ou sélectionnez un projet existant
3. Donnez un nom à votre projet (ex: "GreenLedger Email")
4. Cliquez sur **"Créer"**

### Étape 2 : Activer l'API Gmail

1. Dans le menu de gauche, allez dans **"API et services"** → **"Bibliothèque"**
2. Recherchez **"Gmail API"**
3. Cliquez sur **"Gmail API"**
4. Cliquez sur **"Activer"**

### Étape 3 : Créer des identifiants OAuth2

1. Allez dans **"API et services"** → **"Identifiants"**
2. Cliquez sur **"Créer des identifiants"** → **"ID client OAuth"**
3. Si demandé, configurez l'écran de consentement OAuth :
   - Type d'application : **Externe**
   - Nom de l'application : **GreenLedger**
   - Email d'assistance utilisateur : votre email
   - Domaine autorisé : laissez vide pour le développement
   - Cliquez sur **"Enregistrer et continuer"**
   - Portées : Cliquez sur **"Ajouter ou supprimer des portées"**
     - Recherchez et ajoutez : `https://www.googleapis.com/auth/gmail.send`
   - Cliquez sur **"Enregistrer et continuer"**
   - Utilisateurs de test : Ajoutez votre adresse Gmail
   - Cliquez sur **"Enregistrer et continuer"**

4. Revenez à **"Identifiants"** et cliquez sur **"Créer des identifiants"** → **"ID client OAuth"**
5. Type d'application : **Application de bureau**
6. Nom : **GreenLedger Desktop Client**
7. Cliquez sur **"Créer"**

### Étape 4 : Télécharger le fichier credentials.json

1. Une fenêtre s'ouvre avec vos identifiants
2. Cliquez sur **"Télécharger JSON"**
3. Renommez le fichier en **`credentials.json`**
4. Placez-le dans : `src/main/resources/credentials.json`

### Étape 5 : Configurer les variables d'environnement

Modifiez votre fichier `.env` :

```env
# Gmail API Configuration
GMAIL_API_ENABLED=true
GMAIL_FROM_EMAIL=votre.email@gmail.com
GMAIL_FROM_NAME=GreenLedger Team

# URL de l'application (pour les liens dans les emails)
APP_RESET_URL_PREFIX=http://127.0.0.1:8088/reset?token=
```

### Étape 6 : Première authentification

1. Compilez le projet :
```bash
mvn clean compile
```

2. Lancez l'application
3. Au premier envoi d'email, une fenêtre de navigateur s'ouvrira
4. Connectez-vous avec votre compte Gmail
5. Autorisez l'application à envoyer des emails
6. Les tokens seront sauvegardés dans le dossier `tokens/`

---

## 📝 Utilisation dans le code

### Exemple simple

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
            
            if (success) {
                System.out.println("Email envoyé !");
            }
        }
    }
}
```

### Types d'emails disponibles

```java
// Email de bienvenue
gmailService.sendWelcomeEmail(email, fullName);

// Email de vérification
gmailService.sendVerificationEmail(email, fullName, verificationToken);

// Email de réinitialisation de mot de passe
gmailService.sendResetPasswordEmail(email, fullName, resetToken);

// Email de compte approuvé
gmailService.sendAccountApprovedEmail(email, fullName);

// Email de compte rejeté
gmailService.sendAccountRejectedEmail(email, fullName, reason);

// Email de compte bloqué
gmailService.sendAccountBlockedEmail(email, fullName, reason);

// Email de compte débloqué
gmailService.sendAccountUnblockedEmail(email, fullName);
```

---

## 🔧 Intégration avec EmailService existant

Modifiez votre `EmailService.java` pour utiliser Gmail API :

```java
public class EmailService {
    private final GmailApiService gmailService = new GmailApiService();
    
    public boolean sendWelcomeEmail(String toEmail, String fullName) {
        if (gmailService.isConfigured()) {
            return gmailService.sendWelcomeEmail(toEmail, fullName);
        }
        // Fallback sur SMTP si Gmail API n'est pas configuré
        return sendViaSMTP(toEmail, fullName);
    }
}
```

---

## 🐛 Dépannage

### Erreur : "credentials.json not found"

**Solution** : Vérifiez que le fichier `credentials.json` est bien dans `src/main/resources/`

### Erreur : "Access blocked: This app's request is invalid"

**Solution** : 
1. Vérifiez que vous avez ajouté votre email dans les "Utilisateurs de test"
2. Vérifiez que l'API Gmail est bien activée

### Erreur : "The user has not granted the app"

**Solution** : 
1. Supprimez le dossier `tokens/`
2. Relancez l'application
3. Réautorisez l'application

### Les emails ne sont pas reçus

**Solution** :
1. Vérifiez le dossier spam
2. Vérifiez que l'adresse email est correcte
3. Consultez les logs : `[Gmail API]`

---

## 🔒 Sécurité

### Fichiers à ne PAS commiter sur Git

Ajoutez dans votre `.gitignore` :

```
# Gmail API credentials
credentials.json
tokens/
```

### Bonnes pratiques

- Ne partagez jamais votre `credentials.json`
- Ne commitez jamais le dossier `tokens/`
- Utilisez des variables d'environnement pour les configurations sensibles
- En production, utilisez un compte de service Google

---

## 📊 Limites et quotas

### Gmail API (gratuit)

- **Quota quotidien** : 1 milliard de requêtes/jour
- **Limite d'envoi** : 500 emails/jour (compte Gmail standard)
- **Limite d'envoi** : 2000 emails/jour (Google Workspace)

Pour augmenter les limites, utilisez un compte Google Workspace.

---

## 🎯 Prochaines étapes

1. ✅ Configurer Gmail API
2. ✅ Tester l'envoi d'emails
3. 📝 Personnaliser les templates HTML
4. 🚀 Déployer en production

---

## 📚 Ressources

- [Gmail API Documentation](https://developers.google.com/gmail/api)
- [OAuth2 Guide](https://developers.google.com/identity/protocols/oauth2)
- [Google Cloud Console](https://console.cloud.google.com/)

---

## ✅ Checklist de configuration

- [ ] Projet Google Cloud créé
- [ ] Gmail API activée
- [ ] Écran de consentement OAuth configuré
- [ ] Identifiants OAuth2 créés
- [ ] Fichier `credentials.json` téléchargé et placé dans `src/main/resources/`
- [ ] Variables d'environnement configurées dans `.env`
- [ ] Première authentification effectuée
- [ ] Test d'envoi d'email réussi

---

## 🎉 Félicitations !

Votre application peut maintenant envoyer des emails via Gmail API ! 🚀
