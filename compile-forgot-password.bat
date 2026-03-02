@echo off
echo ========================================
echo Compilation du systeme Mot de passe oublie avec code
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
set CP=%CP%;%JAVAFXDIR%\javafx-controls\20.0.2\javafx-controls-20.0.2-win.jar
set CP=%CP%;%JAVAFXDIR%\javafx-fxml\20.0.2\javafx-fxml-20.0.2.jar
set CP=%CP%;%JAVAFXDIR%\javafx-fxml\20.0.2\javafx-fxml-20.0.2-win.jar
set CP=%CP%;%JAVAFXDIR%\javafx-graphics\20.0.2\javafx-graphics-20.0.2.jar
set CP=%CP%;%JAVAFXDIR%\javafx-graphics\20.0.2\javafx-graphics-20.0.2-win.jar
set CP=%CP%;%JAVAFXDIR%\javafx-base\20.0.2\javafx-base-20.0.2.jar
set CP=%CP%;%JAVAFXDIR%\javafx-base\20.0.2\javafx-base-20.0.2-win.jar
set CP=%CP%;%JAVAFXDIR%\javafx-swing\20.0.2\javafx-swing-20.0.2.jar
set CP=%CP%;%JAVAFXDIR%\javafx-swing\20.0.2\javafx-swing-20.0.2-win.jar
set CP=%CP%;%JAVAFXDIR%\javafx-web\20.0.2\javafx-web-20.0.2.jar
set CP=%CP%;%JAVAFXDIR%\javafx-web\20.0.2\javafx-web-20.0.2-win.jar

echo [1/6] Compilation de User.java...
javac -encoding UTF-8 -cp "%CP%" -d target/classes src/main/java/Models/User.java
if %ERRORLEVEL% NEQ 0 (
    echo ❌ Erreur lors de la compilation de User.java
    pause
    exit /b 1
)
echo ✅ User.java compile

echo.
echo [2/6] Compilation de EnvLoader.java...
javac -encoding UTF-8 -cp "%CP%" -d target/classes src/main/java/Utils/EnvLoader.java
if %ERRORLEVEL% NEQ 0 (
    echo ❌ Erreur lors de la compilation de EnvLoader.java
    pause
    exit /b 1
)
echo ✅ EnvLoader.java compile

echo.
echo [3/6] Compilation de GmailApiService.java et EmailService.java...
javac -encoding UTF-8 -cp "%CP%" -d target/classes src/main/java/Utils/GmailApiService.java src/main/java/Utils/EmailService.java
if %ERRORLEVEL% NEQ 0 (
    echo ❌ Erreur lors de la compilation des services email
    pause
    exit /b 1
)
echo ✅ Services email compiles

echo.
echo [4/6] Compilation de UnifiedEmailService.java...
javac -encoding UTF-8 -cp "%CP%" -d target/classes src/main/java/Utils/UnifiedEmailService.java
if %ERRORLEVEL% NEQ 0 (
    echo ❌ Erreur lors de la compilation de UnifiedEmailService.java
    pause
    exit /b 1
)
echo ✅ UnifiedEmailService.java compile

echo.
echo [5/6] Compilation de UserServiceImpl.java...
javac -encoding UTF-8 -cp "%CP%" -d target/classes src/main/java/Services/IUserService.java src/main/java/Services/UserServiceImpl.java
if %ERRORLEVEL% NEQ 0 (
    echo ❌ Erreur lors de la compilation de UserServiceImpl.java
    pause
    exit /b 1
)
echo ✅ UserServiceImpl.java compile

echo.
echo [6/6] Compilation de ForgotPasswordController.java...
javac -encoding UTF-8 -cp "%CP%" -d target/classes src/main/java/Controllers/ForgotPasswordController.java
if %ERRORLEVEL% NEQ 0 (
    echo ❌ Erreur lors de la compilation de ForgotPasswordController.java
    pause
    exit /b 1
)
echo ✅ ForgotPasswordController.java compile

echo.
echo [7/7] Copie du fichier FXML...
if not exist "target\classes\fxml" mkdir target\classes\fxml
copy src\main\resources\fxml\forgot_password.fxml target\classes\fxml\ >nul 2>&1
if %ERRORLEVEL% EQU 0 (
    echo ✅ forgot_password.fxml copie
) else (
    echo ⚠️  Avertissement: fichier FXML non copie
)

echo.
echo ========================================
echo ✅ COMPILATION TERMINEE AVEC SUCCES !
echo ========================================
echo.
echo FONCTIONNALITE: Mot de passe oublie avec code de verification
echo.
echo FLUX:
echo 1. Utilisateur entre son email
echo 2. Code a 6 chiffres genere et envoye par email
echo 3. Utilisateur entre le code + nouveau mot de passe
echo 4. Verification et reinitialisation
echo.
echo CARACTERISTIQUES:
echo ✅ Code a 6 chiffres aleatoire
echo ✅ Expiration apres 10 minutes
echo ✅ Compte a rebours visuel
echo ✅ Possibilite de renvoyer le code
echo ✅ Email HTML professionnel
echo ✅ Validation complete du mot de passe
echo.
echo PROCHAINES ETAPES:
echo 1. Lancer l'application: run.bat
echo 2. Cliquer sur "Mot de passe oublie ?"
echo 3. Tester le flux complet
echo.
pause
