@echo off
echo ========================================
echo DIAGNOSTIC DES PROBLEMES
echo ========================================
echo.

echo [1/3] Verification de la base de donnees...
echo.
echo IMPORTANT: Avez-vous execute database_fraud_detection.sql dans phpMyAdmin?
echo.
echo Pour verifier:
echo 1. Ouvrez phpMyAdmin: http://localhost/phpmyadmin
echo 2. Selectionnez la base 'greenledger'
echo 3. Cliquez sur 'SQL'
echo 4. Executez cette requete:
echo.
echo    SHOW COLUMNS FROM user LIKE 'fraud%%';
echo.
echo Si vous voyez 'fraud_score' et 'fraud_checked', c'est bon!
echo Sinon, executez database_fraud_detection.sql
echo.
pause

echo.
echo [2/3] Verification du fichier .env...
echo.
type .env
echo.
echo Verifiez que GMAIL_API_ENABLED=true
echo.
pause

echo.
echo [3/3] Test d'envoi d'email...
echo.
echo Voulez-vous tester l'envoi d'email? (O/N)
set /p test_email=
if /i "%test_email%"=="O" (
    echo.
    echo Execution du test...
    test-gmail.bat
)

echo.
echo ========================================
echo DIAGNOSTIC TERMINE
echo ========================================
echo.
echo SOLUTIONS:
echo.
echo PROBLEME 1: Emails non envoyes
echo   → Verifiez que GMAIL_API_ENABLED=true dans .env
echo   → Verifiez que les tokens OAuth2 sont dans le dossier tokens/
echo   → Testez avec: test-gmail.bat
echo.
echo PROBLEME 2: Colonne Actions manquante
echo   → Le fichier FXML a ete corrige
echo   → Recompilez avec: mvn clean compile
echo   → Relancez: run.bat
echo.
echo PROBLEME 3: Informations de fraude non affichees
echo   → Executez database_fraud_detection.sql dans phpMyAdmin
echo   → Recompilez avec: mvn clean compile
echo   → Relancez: run.bat
echo.
pause
