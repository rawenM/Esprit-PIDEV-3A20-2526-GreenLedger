@echo off
chcp 65001 >nul
color 0A

echo ═══════════════════════════════════════════════════════════════
echo 📋 INSTALLATION DU JOURNAL D'ACTIVITÉ (AUDIT LOG)
echo ═══════════════════════════════════════════════════════════════
echo.
echo Ce script va installer le système de journal d'activité.
echo.
echo ⚠️  IMPORTANT:
echo    L'enregistrement est AUTOMATIQUE et ne peut pas être désactivé.
echo    C'est essentiel pour la conformité RGPD et la sécurité.
echo.
pause

echo.
echo ═══════════════════════════════════════════════════════════════
echo ÉTAPE 1: CRÉATION DE LA TABLE MYSQL
echo ═══════════════════════════════════════════════════════════════
echo.
echo Veuillez exécuter le fichier SQL dans MySQL:
echo    database_audit_log.sql
echo.
echo Méthode 1: MySQL Workbench
echo    1. Ouvrir MySQL Workbench
echo    2. Ouvrir database_audit_log.sql
echo    3. Exécuter le script
echo.
echo Méthode 2: Ligne de commande
echo    mysql -u root -p greenledger ^< database_audit_log.sql
echo.
set /p done="Avez-vous exécuté le script SQL? (O/N): "
if /i "%done%"=="N" (
    echo.
    echo ❌ Veuillez d'abord exécuter le script SQL!
    pause
    exit /b 1
)

echo.
echo ✅ Table créée!
echo.

echo ═══════════════════════════════════════════════════════════════
echo ÉTAPE 2: VÉRIFICATION DES FICHIERS JAVA
echo ═══════════════════════════════════════════════════════════════
echo.

if exist "src\main\java\Models\AuditLog.java" (
    echo ✅ AuditLog.java trouvé
) else (
    echo ❌ AuditLog.java manquant!
)

if exist "src\main\java\dao\AuditLogDAO.java" (
    echo ✅ AuditLogDAO.java trouvé
) else (
    echo ❌ AuditLogDAO.java manquant!
)

if exist "src\main\java\dao\AuditLogDAOImpl.java" (
    echo ✅ AuditLogDAOImpl.java trouvé
) else (
    echo ❌ AuditLogDAOImpl.java manquant!
)

if exist "src\main\java\Services\AuditLogService.java" (
    echo ✅ AuditLogService.java trouvé
) else (
    echo ❌ AuditLogService.java manquant!
)

echo.
echo ═══════════════════════════════════════════════════════════════
echo ÉTAPE 3: INTÉGRATION DANS LES CONTRÔLEURS
echo ═══════════════════════════════════════════════════════════════
echo.
echo Vous devez maintenant intégrer le service dans vos contrôleurs:
echo.
echo 📝 LoginController.java
echo    • Après connexion réussie: AuditLogService.getInstance().logLogin(user, "127.0.0.1");
echo    • Après connexion échouée: AuditLogService.getInstance().logLoginFailed(...);
echo.
echo 📝 RegisterController.java
echo    • Après inscription: AuditLogService.getInstance().logRegister(newUser, "127.0.0.1");
echo.
echo 📝 AdminUsersController.java
echo    • Après validation: AuditLogService.getInstance().logAdminValidateUser(...);
echo    • Après blocage: AuditLogService.getInstance().logAdminBlockUser(...);
echo    • Après suppression: AuditLogService.getInstance().logAdminDeleteUser(...);
echo    • Dans showFraudDetails: AuditLogService.getInstance().logAdminViewFraud(...);
echo.
echo 📝 ForgotPasswordController.java
echo    • Après réinitialisation: AuditLogService.getInstance().logPasswordReset(...);
echo.
echo Consultez GUIDE_JOURNAL_ACTIVITE.md pour les détails complets.
echo.

echo ═══════════════════════════════════════════════════════════════
echo ÉTAPE 4: TEST
echo ═══════════════════════════════════════════════════════════════
echo.
echo Pour tester:
echo    1. Lancez l'application: run.bat
echo    2. Connectez-vous
echo    3. Vérifiez dans MySQL:
echo       SELECT * FROM audit_log ORDER BY created_at DESC LIMIT 10;
echo.

echo ═══════════════════════════════════════════════════════════════
echo ✅ INSTALLATION TERMINÉE!
echo ═══════════════════════════════════════════════════════════════
echo.
echo Prochaines étapes:
echo    1. Intégrer dans les contrôleurs (30 min)
echo    2. Tester l'enregistrement (10 min)
echo    3. Créer l'interface de consultation (optionnel, 2-3h)
echo.
echo Documentation complète: GUIDE_JOURNAL_ACTIVITE.md
echo.
pause
