# 🚀 Gmail API - Démarrage Rapide (5 minutes)

## Étapes rapides

### 1. Créer un projet Google Cloud (2 min)

1. Allez sur https://console.cloud.google.com/
2. Créez un nouveau projet "GreenLedger Email"
3. Activez l'API Gmail dans "Bibliothèque"

### 2. Configurer OAuth2 (2 min)

1. Allez dans "API et services" → "Identifiants"
2. Configurez l'écran de consentement :
   - Type : Externe
   - Nom : GreenLedger
   - Portée : `https://www.googleapis.com/auth/gmail.send`
   - Utilisateurs de test : Ajoutez votre email Gmail
3. Créez un "ID client OAuth" :
   - Type : Application de bureau
   - Téléchargez le JSON

### 3. Installer le fichier credentials (30 sec)

1. Renommez le fichier téléchargé en `credentials.json`
2. Placez-le dans : `src/main/resources/credentials.json`

### 4. Configurer .env (30 sec)

```env
GMAIL_API_ENABLED=true
GMAIL_FROM_EMAIL=votre.email@gmail.com
GMAIL_FROM_NAME=GreenLedger Team
```

### 5. Tester (1 min)

```bash
mvn clean compile
java -cp target/classes tools.TestGmailApi
```

Au premier lancement, une fenêtre de navigateur s'ouvrira pour autoriser l'application.

## ✅ C'est tout !

Votre application peut maintenant envoyer des emails via Gmail !

Pour plus de détails, consultez [GMAIL_API_SETUP_GUIDE.md](GMAIL_API_SETUP_GUIDE.md)
