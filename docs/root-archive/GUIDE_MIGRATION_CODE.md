# 🔄 Guide de migration du code

## Vue d'ensemble

Ce guide vous aide à migrer votre code existant pour utiliser le nouveau système d'emails Gmail API.

---

## 🎯 Stratégie de migration

### Option 1 : Migration progressive (Recommandée)

Utilisez `UnifiedEmailService` qui fonctionne avec Gmail API ET SMTP.

**Avantages :**
- Pas besoin de tout migrer d'un coup
- Fallback automatique sur SMTP
- Aucun changement d'API

### Option 2 : Migration directe

Remplacez directement par `GmailApiService`.

**Avantages :**
- Code plus simple
- Utilise uniquement Gmail API

---

## 📝 Exemples de migration

### Si vous utilisez EmailService

**Avant :**
```java
import Utils.EmailService;

public class RegisterController {
    private final EmailService emailService = new EmailService();
    
    public void register(String email, String fullName) {
        emailService.sendWelcomeEmail(email, fullName);
    }
}
```

**Après (Option 1 - Recommandée) :**
```java
import Utils.UnifiedEmailService;

public class RegisterController {
    private final UnifiedEmailService emailService = new UnifiedEmailService();
    
    public void register(String email, String fullName) {
        emailService.sendWelcomeEmail(email, fullName);
    }
}
```

**Après (Option 2) :**
```java
import Utils.GmailApiService;

public class RegisterController {
    private final GmailApiService emailService = new GmailApiService();
    
    public void register(String email, String fullName) {
        if (emailService.isConfigured()) {
            emailService.sendWelcomeEmail(email, fullName);
        }
    }
}
```

---

### Si vous utilisez SendGridEmailService (ancien)

**Avant :**
```java
import Utils.SendGridEmailService;

public class AdminController {
    private final SendGridEmailService emailService = new SendGridEmailService();
    
    public void approveAccount(String email, String fullName) {
        emailService.sendAccountApprovedEmail(email, fullName);
    }
}
```

**Après :**
```java
import Utils.UnifiedEmailService;

public class AdminController {
    private final UnifiedEmailService emailService = new UnifiedEmailService();
    
    public void approveAccount(String email, String fullName) {
        emailService.sendAccountApprovedEmail(email, fullName);
    }
}
```

---

### Réinitialisation de mot de passe

**Avant :**
```java
import Utils.EmailService;

public class ResetPasswordController {
    private final EmailService emailService = new EmailService();
    
    public void sendResetLink(String email, String token) {
        emailService.sendResetEmail(email, token);
    }
}
```

**Après :**
```java
import Utils.UnifiedEmailService;

public class ResetPasswordController {
    private final UnifiedEmailService emailService = new UnifiedEmailService();
    
    public void sendResetLink(String email, String fullName, String token) {
        emailService.sendResetPasswordEmail(email, fullName, token);
    }
}
```

**Note :** La nouvelle méthode prend `fullName` en paramètre pour personnaliser l'email.

---

### Changement de statut de compte

**Avant :**
```java
import Utils.EmailService;

public class AdminUsersController {
    private final EmailService emailService = new EmailService();
    
    public void changeStatus(String email, String fullName, String status) {
        emailService.sendAccountStatusEmail(email, fullName, status);
    }
}
```

**Après :**
```java
import Utils.UnifiedEmailService;

public class AdminUsersController {
    private final UnifiedEmailService emailService = new UnifiedEmailService();
    
    public void changeStatus(String email, String fullName, String status) {
        // Méthode générique (fonctionne comme avant)
        emailService.sendAccountStatusEmail(email, fullName, status);
        
        // OU utilisez les méthodes spécifiques :
        switch (status.toLowerCase()) {
            case "valide":
                emailService.sendAccountApprovedEmail(email, fullName);
                break;
            case "refuse":
                emailService.sendAccountRejectedEmail(email, fullName, "Raison du rejet");
                break;
            case "bloque":
                emailService.sendAccountBlockedEmail(email, fullName, "Raison du blocage");
                break;
            case "debloque":
                emailService.sendAccountUnblockedEmail(email, fullName);
                break;
        }
    }
}
```

---

## 🔍 Rechercher et remplacer

### Recherche globale dans le projet

Recherchez ces imports dans votre projet :

```java
import Utils.EmailService;
import Utils.SendGridEmailService;
```

Remplacez par :

```java
import Utils.UnifiedEmailService;
```

### Recherche des instanciations

Recherchez :
```java
new EmailService()
new SendGridEmailService()
```

Remplacez par :
```java
new UnifiedEmailService()
```

---

## 📋 Checklist de migration

### Pour chaque fichier utilisant EmailService

- [ ] Remplacer l'import
- [ ] Remplacer l'instanciation
- [ ] Vérifier les appels de méthodes
- [ ] Ajouter `fullName` si nécessaire (pour `sendResetPasswordEmail`)
- [ ] Tester l'envoi d'email

### Fichiers à vérifier

Recherchez dans ces dossiers :
- `src/main/java/Controllers/`
- `src/main/java/Services/`
- Tout fichier utilisant l'envoi d'emails

---

## 🧪 Tests

### Test unitaire

```java
import Utils.UnifiedEmailService;

public class EmailTest {
    public static void main(String[] args) {
        UnifiedEmailService emailService = new UnifiedEmailService();
        
        if (!emailService.isConfigured()) {
            System.out.println("❌ Service d'email non configuré");
            return;
        }
        
        boolean success = emailService.sendWelcomeEmail(
            "test@example.com",
            "Test User"
        );
        
        if (success) {
            System.out.println("✅ Email envoyé avec succès");
        } else {
            System.out.println("❌ Échec de l'envoi");
        }
    }
}
```

### Test avec l'outil fourni

```bash
java -cp target/classes tools.TestGmailApi
```

---

## 🎯 Correspondance des méthodes

| EmailService (ancien) | UnifiedEmailService (nouveau) |
|----------------------|-------------------------------|
| `sendWelcomeEmail(email, fullName)` | `sendWelcomeEmail(email, fullName)` ✅ |
| `sendResetEmail(email, token)` | `sendResetPasswordEmail(email, fullName, token)` ⚠️ |
| `sendAccountStatusEmail(email, fullName, status)` | `sendAccountStatusEmail(email, fullName, status)` ✅ |
| N/A | `sendVerificationEmail(email, fullName, token)` ✨ |
| N/A | `sendAccountApprovedEmail(email, fullName)` ✨ |
| N/A | `sendAccountRejectedEmail(email, fullName, reason)` ✨ |
| N/A | `sendAccountBlockedEmail(email, fullName, reason)` ✨ |
| N/A | `sendAccountUnblockedEmail(email, fullName)` ✨ |

**Légende :**
- ✅ Identique
- ⚠️ Paramètre supplémentaire requis
- ✨ Nouvelle méthode

---

## 🐛 Problèmes courants

### Erreur : "fullName parameter required"

**Problème :** La nouvelle méthode `sendResetPasswordEmail` nécessite `fullName`.

**Solution :**
```java
// Avant
emailService.sendResetEmail(email, token);

// Après
emailService.sendResetPasswordEmail(email, fullName, token);
```

Si vous n'avez pas `fullName`, utilisez l'email :
```java
String fullName = email.split("@")[0]; // Utilise la partie avant @
emailService.sendResetPasswordEmail(email, fullName, token);
```

### Erreur : "Cannot resolve symbol UnifiedEmailService"

**Problème :** Le projet n'est pas compilé.

**Solution :**
```bash
mvn clean compile
```

---

## ✅ Validation

Après la migration, vérifiez :

1. **Compilation réussie**
   ```bash
   mvn clean compile
   ```

2. **Aucune erreur de compilation**
   ```bash
   mvn clean verify
   ```

3. **Test d'envoi d'email**
   ```bash
   java -cp target/classes tools.TestGmailApi
   ```

4. **Test fonctionnel**
   - Inscrivez un nouvel utilisateur
   - Vérifiez que l'email de bienvenue est reçu
   - Testez la réinitialisation de mot de passe
   - Testez les changements de statut

---

## 🎉 Migration terminée !

Une fois tous les fichiers migrés et testés, votre application utilise le nouveau système d'emails ! 🚀

**Besoin d'aide ?** Consultez [EMAIL_SERVICES_README.md](EMAIL_SERVICES_README.md)
