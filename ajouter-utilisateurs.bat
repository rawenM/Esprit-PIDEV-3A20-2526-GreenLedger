@echo off
chcp 65001 >nul
echo ═══════════════════════════════════════════════════════════════
echo    AJOUTER DES UTILISATEURS DE TEST
echo ═══════════════════════════════════════════════════════════════
echo.
echo Ce script va ajouter 6 utilisateurs de test dans la base greenledger
echo.
echo Utilisateurs qui seront créés :
echo   1. admin@plateforme.com      - ADMINISTRATEUR
echo   2. investisseur@test.com     - INVESTISSEUR
echo   3. porteur@test.com          - PORTEUR_PROJET
echo   4. expert@test.com           - EXPERT_CARBONE
echo   5. bloque@test.com           - INVESTISSEUR (bloqué)
echo   6. fraude@test.com           - INVESTISSEUR (score fraude élevé)
echo.
echo Mot de passe pour tous : admin123
echo.
echo ═══════════════════════════════════════════════════════════════
echo.
pause

echo.
echo Exécution du script SQL...
echo.

mysql -u root -p greenledger < ajouter_utilisateurs_test.sql

if %ERRORLEVEL% EQU 0 (
    echo.
    echo ═══════════════════════════════════════════════════════════════
    echo    ✅ UTILISATEURS AJOUTÉS AVEC SUCCÈS !
    echo ═══════════════════════════════════════════════════════════════
    echo.
    echo Prochaines étapes :
    echo   1. Recompilez le projet dans votre IDE
    echo   2. Lancez run.bat
    echo   3. Connectez-vous avec admin@plateforme.com / admin123
    echo.
) else (
    echo.
    echo ═══════════════════════════════════════════════════════════════
    echo    ❌ ERREUR LORS DE L'AJOUT DES UTILISATEURS
    echo ═══════════════════════════════════════════════════════════════
    echo.
    echo Vérifiez que :
    echo   - MySQL est démarré
    echo   - La base greenledger existe
    echo   - Le mot de passe root est correct
    echo.
)

pause
