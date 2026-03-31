@echo off
echo ========================================
echo Compilation du systeme de detection de fraude
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

echo [1/5] Compilation du modele User...
javac -encoding UTF-8 -cp "%CP%" -d target/classes src/main/java/Models/User.java
if %ERRORLEVEL% NEQ 0 (
    echo ❌ Erreur lors de la compilation de User.java
    pause
    exit /b 1
)
echo ✅ User.java compile

echo.
echo [2/5] Compilation du modele FraudDetectionResult...
javac -encoding UTF-8 -cp "%CP%" -d target/classes src/main/java/Models/FraudDetectionResult.java
if %ERRORLEVEL% NEQ 0 (
    echo ❌ Erreur lors de la compilation de FraudDetectionResult.java
    pause
    exit /b 1
)
echo ✅ FraudDetectionResult.java compile

echo.
echo [3/5] Compilation du service FraudDetectionService...
javac -encoding UTF-8 -cp "%CP%" -d target/classes src/main/java/Services/FraudDetectionService.java
if %ERRORLEVEL% NEQ 0 (
    echo ❌ Erreur lors de la compilation de FraudDetectionService.java
    pause
    exit /b 1
)
echo ✅ FraudDetectionService.java compile

echo.
echo [4/5] Compilation du DAO FraudDetectionDAOImpl...
javac -encoding UTF-8 -cp "%CP%" -d target/classes src/main/java/dao/IFraudDetectionDAO.java src/main/java/dao/FraudDetectionDAOImpl.java
if %ERRORLEVEL% NEQ 0 (
    echo ❌ Erreur lors de la compilation des DAO
    pause
    exit /b 1
)
echo ✅ DAO compile

echo.
echo [5/5] Compilation du controleur AdminUsersController...
javac -encoding UTF-8 -cp "%CP%" -d target/classes src/main/java/Controllers/AdminUsersController.java
if %ERRORLEVEL% NEQ 0 (
    echo ❌ Erreur lors de la compilation de AdminUsersController.java
    pause
    exit /b 1
)
echo ✅ AdminUsersController.java compile

echo.
echo ========================================
echo ✅ COMPILATION TERMINEE AVEC SUCCES !
echo ========================================
echo.
echo Prochaines etapes:
echo 1. Executer database_fraud_detection.sql dans phpMyAdmin
echo 2. Lancer l'application avec run.bat
echo 3. Tester la detection de fraude
echo.
pause
