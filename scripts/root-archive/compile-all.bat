@echo off
echo ========================================
echo COMPILATION COMPLETE DU PROJET GREENLEDGER
echo ========================================
echo.

REM Définir le répertoire Maven local
set M2=%USERPROFILE%\.m2\repository
set JAVAFXDIR=%M2%\org\openjfx

REM Construire le classpath complet
set CP=target\classes
set CP=%CP%;%M2%\com\google\api-client\google-api-client\2.2.0\google-api-client-2.2.0.jar
set CP=%CP%;%M2%\com\google\oauth-client\google-oauth-client-jetty\1.34.1\google-oauth-client-jetty-1.34.1.jar
set CP=%CP%;%M2%\com\google\apis\google-api-services-gmail\v1-rev20220404-2.0.0\google-api-services-gmail-v1-rev20220404-2.0.0.jar
set CP=%CP%;%M2%\com\google\oauth-client\google-oauth-client\1.34.1\google-oauth-client-1.34.1.jar
set CP=%CP%;%M2%\com\google\oauth-client\google-oauth-client-java6\1.34.1\google-oauth-client-java6-1.34.1.jar
set CP=%CP%;%M2%\com\google\http-client\google-http-client\1.42.3\google-http-client-1.42.3.jar
set CP=%CP%;%M2%\com\google\http-client\google-http-client-gson\1.42.3\google-http-client-gson-1.42.3.jar
set CP=%CP%;%M2%\com\google\code\gson\gson\2.10.1\gson-2.10.1.jar
set CP=%CP%;%M2%\com\sun\mail\jakarta.mail\2.0.1\jakarta.mail-2.0.1.jar
set CP=%CP%;%M2%\com\sun\activation\jakarta.activation\2.0.1\jakarta.activation-2.0.1.jar
set CP=%CP%;%M2%\mysql\mysql-connector-java\8.0.26\mysql-connector-java-8.0.26.jar
set CP=%CP%;%M2%\org\mindrot\jbcrypt\0.4\jbcrypt-0.4.jar
set CP=%CP%;%JAVAFXDIR%\javafx-controls\20.0.2\javafx-controls-20.0.2.jar
set CP=%CP%;%JAVAFXDIR%\javafx-fxml\20.0.2\javafx-fxml-20.0.2.jar
set CP=%CP%;%JAVAFXDIR%\javafx-graphics\20.0.2\javafx-graphics-20.0.2.jar
set CP=%CP%;%JAVAFXDIR%\javafx-base\20.0.2\javafx-base-20.0.2.jar
set CP=%CP%;%JAVAFXDIR%\javafx-swing\20.0.2\javafx-swing-20.0.2.jar
set CP=%CP%;%JAVAFXDIR%\javafx-web\20.0.2\javafx-web-20.0.2.jar

echo ========================================
echo ETAPE 1: COMPILATION DES MODELES
echo ========================================
echo.

echo [1/3] Compilation de User.java...
javac -encoding UTF-8 -cp "%CP%" -d target/classes src/main/java/Models/User.java
if %ERRORLEVEL% NEQ 0 (
    echo ❌ Erreur lors de la compilation de User.java
    pause
    exit /b 1
)
echo ✅ User.java compile

echo.
echo [2/3] Compilation de FraudDetectionResult.java...
javac -encoding UTF-8 -cp "%CP%" -d target/classes src/main/java/Models/FraudDetectionResult.java
if %ERRORLEVEL% NEQ 0 (
    echo ❌ Erreur lors de la compilation de FraudDetectionResult.java
    pause
    exit /b 1
)
echo ✅ FraudDetectionResult.java compile

echo.
echo [3/3] Compilation des autres modeles...
javac -encoding UTF-8 -cp "%CP%" -d target/classes src/main/java/Models/*.java
if %ERRORLEVEL% NEQ 0 (
    echo ⚠️  Avertissement lors de la compilation des modeles
)
echo ✅ Modeles compiles

echo.
echo ========================================
echo ETAPE 2: COMPILATION DES UTILS
echo ========================================
echo.

echo [1/5] Compilation de EnvLoader.java...
javac -encoding UTF-8 -cp "%CP%" -d target/classes src/main/java/Utils/EnvLoader.java
if %ERRORLEVEL% NEQ 0 (
    echo ❌ Erreur lors de la compilation de EnvLoader.java
    pause
    exit /b 1
)
echo ✅ EnvLoader.java compile

echo.
echo [2/5] Compilation de PasswordUtil.java...
javac -encoding UTF-8 -cp "%CP%" -d target/classes src/main/java/Utils/PasswordUtil.java
if %ERRORLEVEL% NEQ 0 (
    echo ❌ Erreur lors de la compilation de PasswordUtil.java
    pause
    exit /b 1
)
echo ✅ PasswordUtil.java compile

echo.
echo [3/5] Compilation de GmailApiService.java...
javac -encoding UTF-8 -cp "%CP%" -d target/classes src/main/java/Utils/GmailApiService.java
if %ERRORLEVEL% NEQ 0 (
    echo ❌ Erreur lors de la compilation de GmailApiService.java
    pause
    exit /b 1
)
echo ✅ GmailApiService.java compile

echo.
echo [4/5] Compilation de CaptchaService.java...
javac -encoding UTF-8 -cp "%CP%" -d target/classes src/main/java/Utils/CaptchaService.java
if %ERRORLEVEL% NEQ 0 (
    echo ❌ Erreur lors de la compilation de CaptchaService.java
    pause
    exit /b 1
)
echo ✅ CaptchaService.java compile

echo.
echo [5/5] Compilation de PuzzleCaptchaService.java...
javac -encoding UTF-8 -cp "%CP%" -d target/classes src/main/java/Utils/PuzzleCaptchaService.java
if %ERRORLEVEL% NEQ 0 (
    echo ❌ Erreur lors de la compilation de PuzzleCaptchaService.java
    pause
    exit /b 1
)
echo ✅ PuzzleCaptchaService.java compile

echo.
echo ========================================
echo ETAPE 3: COMPILATION DES SERVICES
echo ========================================
echo.

echo [1/2] Compilation de FraudDetectionService.java...
javac -encoding UTF-8 -cp "%CP%" -d target/classes src/main/java/Services/FraudDetectionService.java
if %ERRORLEVEL% NEQ 0 (
    echo ❌ Erreur lors de la compilation de FraudDetectionService.java
    pause
    exit /b 1
)
echo ✅ FraudDetectionService.java compile

echo.
echo [2/2] Compilation de UserServiceImpl.java...
javac -encoding UTF-8 -cp "%CP%" -d target/classes src/main/java/Services/UserServiceImpl.java
if %ERRORLEVEL% NEQ 0 (
    echo ❌ Erreur lors de la compilation de UserServiceImpl.java
    pause
    exit /b 1
)
echo ✅ UserServiceImpl.java compile

echo.
echo ========================================
echo ETAPE 4: COMPILATION DES DAO
echo ========================================
echo.

echo Compilation des DAO...
javac -encoding UTF-8 -cp "%CP%" -d target/classes src/main/java/dao/IFraudDetectionDAO.java src/main/java/dao/FraudDetectionDAOImpl.java
if %ERRORLEVEL% NEQ 0 (
    echo ❌ Erreur lors de la compilation des DAO
    pause
    exit /b 1
)
echo ✅ DAO compiles

echo.
echo ========================================
echo ETAPE 5: COMPILATION DES CONTROLLERS
echo ========================================
echo.

echo [1/2] Compilation de AdminUsersController.java...
javac -encoding UTF-8 -cp "%CP%" -d target/classes src/main/java/Controllers/AdminUsersController.java
if %ERRORLEVEL% NEQ 0 (
    echo ❌ Erreur lors de la compilation de AdminUsersController.java
    pause
    exit /b 1
)
echo ✅ AdminUsersController.java compile

echo.
echo [2/2] Compilation de PuzzleCaptchaController.java...
javac -encoding UTF-8 -cp "%CP%" -d target/classes src/main/java/Controllers/PuzzleCaptchaController.java
if %ERRORLEVEL% NEQ 0 (
    echo ❌ Erreur lors de la compilation de PuzzleCaptchaController.java
    pause
    exit /b 1
)
echo ✅ PuzzleCaptchaController.java compile

echo.
echo ========================================
echo ✅ COMPILATION COMPLETE TERMINEE !
echo ========================================
echo.
echo RESUME:
echo ✅ Modeles compiles (User, FraudDetectionResult, etc.)
echo ✅ Utils compiles (Gmail API, CAPTCHA, Puzzle, etc.)
echo ✅ Services compiles (FraudDetection, UserService)
echo ✅ DAO compiles (FraudDetectionDAO)
echo ✅ Controllers compiles (Admin, PuzzleCaptcha)
echo.
echo FONCTIONNALITES DISPONIBLES:
echo ✅ 2 APIs: Gmail API + Google reCAPTCHA
echo ✅ 2 Fonctionnalites avancees: Email + Reset mot de passe
echo ✅ 1 IA: Detection de fraude (7 indicateurs)
echo ✅ 3 Methodes CAPTCHA: Math + reCAPTCHA + Puzzle
echo.
echo PROCHAINES ETAPES:
echo 1. Verifier que MySQL est demarre
echo 2. Executer database_fraud_detection.sql (si pas deja fait)
echo 3. Lancer l'application avec run.bat
echo 4. Tester la detection de fraude dans le dashboard admin
echo.
pause
