# 📋 GUIDE - JOURNAL D'ACTIVITÉ (AUDIT LOG)

## 🎯 DESCRIPTION

Le Journal d'Activité est un système de traçabilité complet qui enregistre **AUTOMATIQUEMENT** toutes les actions des utilisateurs et administrateurs.

### ⚠️ IMPORTANT: ENREGISTREMENT AUTOMATIQUE

L'enregistrement est **AUTOMATIQUE** et **NE PEUT PAS être désactivé** par l'administrateur. C'est essentiel pour:
- ✅ Conformité RGPD
- ✅ Sécurité et traçabilité
- ✅ Détection d'activités suspectes
- ✅ Audit et conformité légale

L'administrateur peut **CONSULTER** les logs, mais **NE PEUT PAS**:
- ❌ Désactiver l'enregistrement
- ❌ Supprimer des logs
- ❌ Modifier des logs

---

## 📦 INSTALLATION

### Étape 1: Créer la table MySQL

```bash
mysql -u root -p greenledger < database_audit_log.sql
```

Ou exécutez manuellement dans MySQL Workbench:
```sql
USE greenledger;
SOURCE database_audit_log.sql;
```

### Étape 2: Vérifier la création

```sql
USE greenledger;
SHOW TABLES LIKE 'audit_log';
SELECT COUNT(*) FROM audit_log;
```

Vous devriez voir 5 logs de test.

---

## 🔧 UTILISATION DANS LE CODE

### 1. Importer le service

```java
import Services.AuditLogService;
import Models.AuditLog;
```

### 2. Enregistrer une action

Le service est un **Singleton**, utilisez `getInstance()`:

```java
AuditLogService auditService = AuditLogService.getInstance();
```

### 3. Exemples d'enregistrement

#### Connexion réussie
```java
auditService.logLogin(user, "192.168.1.100");
```

#### Tentative de connexion échouée
```java
auditService.logLoginFailed("user@example.com", "Mot de passe incorrect", "192.168.1.100");
```

#### Inscription
```java
auditService.logRegister(newUser, "192.168.1.100");
```

#### Modification de profil
```java
auditService.logProfileUpdate(user, "email", "old@example.com", "new@example.com");
```

#### Changement de mot de passe
```java
auditService.logPasswordChange(user);
```

#### Admin valide un compte
```java
auditService.logAdminValidateUser(adminUser, targetUser);
```

#### Admin bloque un utilisateur
```java
auditService.logAdminBlockUser(adminUser, targetUser);
```

#### Fraude détectée
```java
auditService.logFraudDetected(user, 85, "10.0.0.1");
```

---

## 📊 ACTIONS ENREGISTRÉES AUTOMATIQUEMENT

| Action | Type | Quand? |
|--------|------|--------|
| Connexion | `USER_LOGIN` | Après authentification réussie |
| Connexion échouée | `USER_LOGIN_FAILED` | Après échec d'authentification |
| Déconnexion | `USER_LOGOUT` | Quand l'utilisateur se déconnecte |
| Inscription | `USER_REGISTER` | Après création de compte |
| Modification profil | `USER_PROFILE_UPDATE` | Après modification de données |
| Changement mot de passe | `USER_PASSWORD_CHANGE` | Après changement réussi |
| Réinitialisation | `USER_PASSWORD_RESET` | Après réinitialisation |
| Validation compte | `ADMIN_USER_VALIDATE` | Admin valide un compte |
| Blocage utilisateur | `ADMIN_USER_BLOCK` | Admin bloque un utilisateur |
| Déblocage utilisateur | `ADMIN_USER_UNBLOCK` | Admin débloque un utilisateur |
| Suppression utilisateur | `ADMIN_USER_DELETE` | Admin supprime un utilisateur |
| Consultation fraude | `ADMIN_VIEW_FRAUD` | Admin consulte détails fraude |
| Fraude détectée | `FRAUD_DETECTED` | Système détecte une fraude |
| CAPTCHA échoué | `CAPTCHA_FAILED` | Échec de vérification CAPTCHA |
| CAPTCHA réussi | `CAPTCHA_SUCCESS` | Succès de vérification CAPTCHA |

---

## 🔍 INTÉGRATION DANS LES CONTRÔLEURS EXISTANTS

### LoginController.java

Ajoutez après une connexion réussie:
```java
// Dans handleLogin() après authentification réussie
AuditLogService.getInstance().logLogin(user, "127.0.0.1");
```

Ajoutez après une connexion échouée:
```java
// Dans handleLogin() après échec
AuditLogService.getInstance().logLoginFailed(
    emailField.getText(), 
    "Mot de passe incorrect", 
    "127.0.0.1"
);
```

### RegisterController.java

Ajoutez après inscription réussie:
```java
// Dans handleRegister() après création du compte
AuditLogService.getInstance().logRegister(newUser, "127.0.0.1");
```

### AdminUsersController.java

Ajoutez dans `handleValidateUser()`:
```java
// Après validation réussie
AuditLogService.getInstance().logAdminValidateUser(currentUser, user);
```

Ajoutez dans `handleBlockUser()`:
```java
// Après blocage réussi
AuditLogService.getInstance().logAdminBlockUser(currentUser, user);
```

Ajoutez dans `handleDeleteUser()`:
```java
// Après suppression réussie
AuditLogService.getInstance().logAdminDeleteUser(currentUser, user);
```

Ajoutez dans `showFraudDetails()`:
```java
// Au début de la méthode
AuditLogService.getInstance().logAdminViewFraud(currentUser, user);
```

### ForgotPasswordController.java

Ajoutez après réinitialisation réussie:
```java
// Dans handleResetPassword() après succès
AuditLogService.getInstance().logPasswordReset(emailField.getText());
```

---

## 📈 CONSULTATION DES LOGS (POUR L'ADMIN)

### Récupérer tous les logs
```java
AuditLogDAO dao = AuditLogService.getInstance().getDAO();
List<AuditLog> logs = dao.findAll(100, 0); // 100 premiers logs
```

### Récupérer les logs d'un utilisateur
```java
List<AuditLog> userLogs = dao.findByUserId(userId);
```

### Récupérer les logs par type
```java
List<AuditLog> loginLogs = dao.findByActionType(AuditLog.ActionType.USER_LOGIN);
```

### Récupérer les logs d'aujourd'hui
```java
List<AuditLog> todayLogs = dao.findToday();
```

### Rechercher dans les logs
```java
List<AuditLog> searchResults = dao.search("admin@example.com");
```

### Récupérer les tentatives de connexion échouées
```java
List<AuditLog> failedLogins = dao.findRecentFailedLogins(10);
```

---

## 🎨 INTERFACE UTILISATEUR (À CRÉER)

### Option 1: Page dédiée "Journal d'Activité"

Créez une nouvelle page FXML avec:
- TableView pour afficher les logs
- Filtres: Date, Type d'action, Utilisateur, Statut
- Recherche par mot-clé
- Pagination
- Export PDF/CSV

### Option 2: Intégration dans le dashboard admin

Ajoutez une section "Activité Récente" avec:
- Les 10 derniers logs
- Bouton "Voir tout" qui ouvre la page dédiée

---

## 💬 PHRASES POUR LE JURY

### Présentation
> "J'ai implémenté un système de journal d'activité qui enregistre automatiquement toutes les actions des utilisateurs et administrateurs. C'est essentiel pour la conformité RGPD et la sécurité."

### Fonctionnement
> "L'enregistrement est automatique et ne peut pas être désactivé par l'administrateur. C'est une exigence légale pour les applications qui traitent des données personnelles. L'admin peut consulter les logs, les filtrer, et les exporter, mais ne peut pas les modifier ou les supprimer."

### Avantages
> "Ce système permet de détecter les activités suspectes, comme plusieurs tentatives de connexion échouées, des modifications de profil inhabituelles, ou des actions admin critiques. On peut voir qui a fait quoi, quand, et depuis quelle adresse IP."

---

## 🔒 SÉCURITÉ ET CONFORMITÉ

### RGPD
✅ Traçabilité des accès aux données personnelles
✅ Historique des modifications
✅ Détection des accès non autorisés
✅ Audit trail complet

### Sécurité
✅ Détection des tentatives de connexion échouées
✅ Alerte sur activités suspectes
✅ Traçabilité des actions admin
✅ Logs immuables (non modifiables)

---

## 📊 STATISTIQUES DISPONIBLES

Vous pouvez afficher dans le dashboard:
- Nombre total de logs
- Logs d'aujourd'hui
- Tentatives de connexion échouées (dernières 24h)
- Actions admin (dernières 24h)
- Fraudes détectées (dernières 24h)

```java
long totalLogs = dao.count();
List<AuditLog> todayLogs = dao.findToday();
List<AuditLog> failedLogins = dao.findRecentFailedLogins(10);
long fraudCount = dao.countByActionType(AuditLog.ActionType.FRAUD_DETECTED);
```

---

## ✅ CHECKLIST D'INTÉGRATION

- [ ] Table `audit_log` créée dans MySQL
- [ ] Fichiers Java créés (AuditLog.java, AuditLogDAO.java, etc.)
- [ ] Service intégré dans LoginController
- [ ] Service intégré dans RegisterController
- [ ] Service intégré dans AdminUsersController
- [ ] Service intégré dans ForgotPasswordController
- [ ] Tests effectués
- [ ] Interface de consultation créée (optionnel)

---

## 🚀 PROCHAINES ÉTAPES

1. **Créer la table MySQL** (5 min)
2. **Intégrer dans les contrôleurs** (30 min)
3. **Tester l'enregistrement** (10 min)
4. **Créer l'interface de consultation** (optionnel, 2-3h)

---

## 📞 SUPPORT

Si vous avez des questions ou des problèmes:
1. Vérifiez que la table `audit_log` existe
2. Vérifiez les logs dans la console
3. Testez avec une action simple (connexion)

---

**Vous avez maintenant un système de journal d'activité professionnel et conforme RGPD! 🎉**
