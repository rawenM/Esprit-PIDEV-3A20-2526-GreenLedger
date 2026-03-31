# Guide de Test: Fonctionnalité "Mot de Passe Oublié"

## Problème Résolu

Le lien dans l'email pointait vers le port 8088, mais le serveur HTTP local écoute sur le port 8080. Cette incohérence a été corrigée.

## Configuration Corrigée

**Fichier `.env`:**
```env
APP_RESET_URL_PREFIX=http://127.0.0.1:8080/reset?token=
RESET_HTTP_PORT=8080
```

## Méthode 1: Test Complet avec Serveur HTTP (Recommandé)

Cette méthode teste le flux complet comme un utilisateur réel.

### Étape 1: Lancer l'Application

```bash
run.bat
```

**Logs attendus:**
```
[EnvLoader] Loaded 22 variables from .env
[Gmail API] Service initialisé avec succès
[UnifiedEmail] Utilisation de Gmail API pour les emails
[CLEAN] ResetHttpServer démarré sur http://127.0.0.1:8080
[CLEAN] Application demarree avec succes
```

### Étape 2: Demander la Réinitialisation

1. Sur l'écran de connexion, cliquez sur **"Mot de passe oublié"**
2. Entrez votre email: `ibrahimimajid058@gmail.com`
3. Cliquez sur **OK**

**Logs attendus:**
```
[INFO] Tentative d'envoi d'email de reinitialisation...
[INFO] Email service configured: true
[INFO] Resultat envoi email: true
[OK] Email de reinitialisation envoye a: ibrahimimajid058@gmail.com
```

### Étape 3: Vérifier l'Email

1. Ouvrez Gmail: https://mail.google.com
2. Cherchez un email de **"GreenLedger Team"**
3. Sujet: **"Reinitialisation de votre mot de passe"**
4. Ouvrez l'email

**Contenu de l'email:**
```
Bonjour [Votre Nom],

Vous avez demandé à réinitialiser votre mot de passe. 
Cliquez sur le lien ci-dessous :

[Réinitialiser mon mot de passe]

Ce lien expire dans 1 heure.

Si vous n'avez pas demandé cette réinitialisation, ignorez cet email.

L'équipe GreenLedger
```

### Étape 4: Cliquer sur le Lien

1. Cliquez sur le bouton **"Réinitialiser mon mot de passe"** dans l'email
2. Votre navigateur s'ouvre sur: `http://127.0.0.1:8080/reset?token=...`

**Page affichée:**
```
Réinitialisation du mot de passe

Token (depuis email): [token pré-rempli]
Nouveau mot de passe: [_____________]
Confirmer le mot de passe: [_____________]

[Réinitialiser]
```

### Étape 5: Réinitialiser le Mot de Passe

1. Le token est déjà pré-rempli
2. Entrez votre nouveau mot de passe (minimum 8 caractères)
3. Confirmez le mot de passe
4. Cliquez sur **"Réinitialiser"**

**Résultat attendu:**
```
Succès

Mot de passe réinitialisé avec succès.
```

### Étape 6: Se Connecter avec le Nouveau Mot de Passe

1. Retournez à l'application
2. Entrez votre email: `ibrahimimajid058@gmail.com`
3. Entrez votre nouveau mot de passe
4. Cliquez sur **"Se connecter"**

✅ Vous devriez être connecté avec succès!

---

## Méthode 2: Test Rapide sans Serveur HTTP

Si vous voulez tester rapidement sans utiliser le lien dans l'email.

### Étape 1: Lancer l'Application

```bash
run.bat
```

### Étape 2: Demander la Réinitialisation

1. Cliquez sur **"Mot de passe oublié"**
2. Entrez votre email: `ibrahimimajid058@gmail.com`
3. Cliquez sur **OK**

### Étape 3: Utiliser le Formulaire dans l'Application

1. Une boîte de dialogue s'affiche avec le message:
   ```
   Si cet email/numéro existe, vous recevrez les instructions 
   pour réinitialiser votre mot de passe.
   
   (En test local, le token est affiché ci-dessous)
   
   Token: [token-uuid]
   ```

2. Cliquez sur **"Ouvrir formulaire de reset"**

3. Un formulaire s'ouvre dans l'application:
   - Le token est déjà pré-rempli
   - Entrez votre nouveau mot de passe
   - Confirmez le mot de passe
   - Cliquez sur **"Réinitialiser"**

4. Connectez-vous avec le nouveau mot de passe

---

## Méthode 3: Test Script (Vérification Technique)

Pour vérifier que l'envoi d'email fonctionne correctement.

```bash
test-reset-password.bat
```

**Résultat attendu:**
```
=== Test Email Reinitialisation ===

[EnvLoader] Loaded 22 variables from .env
[Gmail API] Service initialisé avec succès
[UnifiedEmail] Utilisation de Gmail API pour les emails
OK Service email configure

Envoi d'email de reinitialisation a: ibrahimimajid058@gmail.com
[Gmail API] Email envoyé avec succès à : ibrahimimajid058@gmail.com

OK Email envoye avec succes !
Verifiez votre boite email: ibrahimimajid058@gmail.com
```

---

## Dépannage

### Problème: "Ce site est inaccessible" (ERR_CONNECTION_REFUSED)

**Cause:** Le serveur HTTP local n'est pas démarré.

**Solution:**
1. Vérifiez que l'application est en cours d'exécution (`run.bat`)
2. Vérifiez les logs pour voir: `[CLEAN] ResetHttpServer démarré sur http://127.0.0.1:8080`
3. Si le serveur n'a pas démarré, vérifiez qu'aucune autre application n'utilise le port 8080

### Problème: Email non reçu

**Solutions:**
1. Vérifiez le dossier **Spam/Courrier indésirable**
2. Vérifiez les logs de la console pour voir si l'email a été envoyé
3. Testez avec `test-reset-password.bat`
4. Vérifiez la configuration Gmail API avec `test-env-loader.bat`

### Problème: Token invalide ou expiré

**Causes possibles:**
- Le token a expiré (1 heure après génération)
- Le token a déjà été utilisé
- Le token ne correspond pas à celui dans la base de données

**Solution:**
1. Demandez un nouveau token via "Mot de passe oublié"
2. Utilisez le token dans l'heure qui suit

### Problème: Mot de passe non conforme

**Exigences du mot de passe:**
- Minimum 8 caractères
- Au moins une lettre majuscule
- Au moins une lettre minuscule
- Au moins un chiffre
- Au moins un caractère spécial (@, #, $, %, etc.)

---

## Sécurité

### Token de Réinitialisation

- **Format:** UUID v4 (128 bits d'entropie)
- **Stockage:** Hash BCrypt dans la base de données
- **Expiration:** 1 heure
- **Usage unique:** Le token est supprimé après utilisation
- **Validation:** Le token est vérifié contre le hash BCrypt

### Serveur HTTP Local

- **Écoute:** Uniquement sur 127.0.0.1 (localhost)
- **Accès:** Uniquement depuis la machine locale
- **Sécurité:** Pas d'accès externe possible
- **Arrêt:** Automatique quand l'application se ferme

### Email

- **Authentification:** OAuth2 (plus sécurisé que mot de passe)
- **Protocole:** HTTPS via Gmail API
- **Contenu:** Lien avec token, pas de mot de passe
- **Expiration:** Le lien expire après 1 heure

---

## Flux Complet

```
┌─────────────────────────────────────────────────────────────┐
│ 1. Utilisateur clique "Mot de passe oublié"                │
└─────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│ 2. Utilisateur entre son email                              │
└─────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│ 3. Application génère un token UUID                         │
│    - Hash le token avec BCrypt                              │
│    - Sauvegarde dans la base de données                     │
│    - Définit l'expiration à 1 heure                         │
└─────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│ 4. Application envoie l'email via Gmail API                 │
│    - Email contient le lien avec le token                   │
│    - Lien: http://127.0.0.1:8080/reset?token=...           │
└─────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│ 5. Utilisateur reçoit l'email                               │
│    - Ouvre l'email dans Gmail                               │
│    - Clique sur le lien                                     │
└─────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│ 6. Navigateur ouvre le lien                                 │
│    - Serveur HTTP local affiche le formulaire               │
│    - Token pré-rempli dans le formulaire                    │
└─────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│ 7. Utilisateur entre le nouveau mot de passe                │
│    - Confirme le mot de passe                               │
│    - Clique sur "Réinitialiser"                             │
└─────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│ 8. Application valide et met à jour                         │
│    - Vérifie le token (hash BCrypt)                         │
│    - Vérifie l'expiration                                   │
│    - Hash le nouveau mot de passe                           │
│    - Met à jour dans la base de données                     │
│    - Supprime le token                                      │
└─────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│ 9. Utilisateur peut se connecter                            │
│    - Avec le nouveau mot de passe                           │
└─────────────────────────────────────────────────────────────┘
```

---

## Fichiers Modifiés

1. ✅ `.env` - Port corrigé de 8088 à 8080
2. ✅ `src/main/java/Utils/EnvLoader.java` - Nouveau
3. ✅ `src/main/java/Utils/GmailApiService.java` - Utilise EnvLoader
4. ✅ `src/main/java/Services/UserServiceImpl.java` - Logs détaillés

---

## Prochaines Étapes

1. ✅ Configuration corrigée (port 8080)
2. ⏳ Tester avec la Méthode 1 (flux complet)
3. ⏳ Vérifier la réception de l'email
4. ⏳ Cliquer sur le lien dans l'email
5. ⏳ Réinitialiser le mot de passe
6. ⏳ Se connecter avec le nouveau mot de passe

---

## Support

Si vous rencontrez des problèmes:

1. Vérifiez les logs de la console
2. Testez avec `test-reset-password.bat`
3. Vérifiez la configuration avec `test-env-loader.bat`
4. Consultez `CORRECTION_MOT_DE_PASSE_OUBLIE.md` pour plus de détails
5. Consultez `FONCTIONNALITE_MOT_DE_PASSE_OUBLIE.md` pour la documentation complète
