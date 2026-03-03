@echo off
echo ========================================
echo Compilation de la page de connexion avec 3 CAPTCHA
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
set CP=%CP%;%M2%\com\google\http-client\google-http-client\1.42.3\google-http-client-1.42.3.jar
set CP=%CP%;%M2%\com\google\http-client\google-http-client-gson\1.42.3\google-http-client-gson-1.42.3.jar
set CP=%CP%;%M2%\com\google\code\gson\gson\2.10.1\gson-2.10.1.jar
set CP=%CP%;%M2%\mysql\mysql-connector-java\8.0.26\mysql-connector-java-8.0.26.jar
set CP=%CP%;%M2%\org\mindrot\jbcrypt\0.4\jbcrypt-0.4.jar
set CP=%CP%;%JAVAFXDIR%\javafx-controls\20.0.2\javafx-controls-20.0.2.jar
set CP=%CP%;%JAVAFXDIR%\javafx-fxml\20.0.2\javafx-fxml-20.0.2.jar
set CP=%CP%;%JAVAFXDIR%\javafx-graphics\20.0.2\javafx-graphics-20.0.2.jar
set CP=%CP%;%JAVAFXDIR%\javafx-base\20.0.2\javafx-base-20.0.2.jar
set CP=%CP%;%JAVAFXDIR%\javafx-swing\20.0.2\javafx-swing-20.0.2.jar
set CP=%CP%;%JAVAFXDIR%\javafx-web\20.0.2\javafx-web-20.0.2.jar

echo [1/3] Compilation de PuzzleCaptchaService.java...
javac -encoding UTF-8 -cp "%CP%" -d target/classes src/main/java/Utils/PuzzleCaptchaService.java
if %ERRORLEVEL% NEQ 0 (
    echo ❌ Erreur lors de la compilation de PuzzleCaptchaService.java
    pause
    exit /b 1
)
echo ✅ PuzzleCaptchaService.java compile

echo.
echo [2/3] Compilation de CaptchaService.java...
javac -encoding UTF-8 -cp "%CP%" -d target/classes src/main/java/Utils/CaptchaService.java
if %ERRORLEVEL% NEQ 0 (
    echo ❌ Erreur lors de la compilation de CaptchaService.java
    pause
    exit /b 1
)
echo ✅ CaptchaService.java compile

echo.
echo [3/3] Compilation de LoginWithCaptchaChoiceController.java...
javac -encoding UTF-8 -cp "%CP%" -d target/classes src/main/java/Controllers/LoginWithCaptchaChoiceController.java
if %ERRORLEVEL% NEQ 0 (
    echo ❌ Erreur lors de la compilation de LoginWithCaptchaChoiceController.java
    pause
    exit /b 1
)
echo ✅ LoginWithCaptchaChoiceController.java compile

echo.
echo ========================================
echo ✅ COMPILATION TERMINEE AVEC SUCCES !
echo ========================================
echo.
echo FICHIERS COMPILES:
echo ✅ PuzzleCaptchaService.java
echo ✅ CaptchaService.java
echo ✅ LoginWithCaptchaChoiceController.java
echo.
echo FICHIERS FXML:
echo ✅ login_with_captcha_choice.fxml
echo.
echo 3 METHODES CAPTCHA DISPONIBLES:
echo 1. CAPTCHA Mathematique (equation simple)
echo 2. Google reCAPTCHA (API externe, tres securise)
echo 3. Puzzle Slider (developpement interne, ludique)
echo.
echo PROCHAINES ETAPES:
echo 1. Copier login_with_captcha_choice.fxml vers login.fxml (optionnel)
echo 2. Lancer l'application avec run.bat
echo 3. Tester les 3 methodes de CAPTCHA
echo.
echo POUR UTILISER LA NOUVELLE PAGE:
echo Option 1: Remplacer login.fxml
echo   copy src\main\resources\fxml\login.fxml src\main\resources\fxml\login_old.fxml
echo   copy src\main\resources\fxml\login_with_captcha_choice.fxml src\main\resources\fxml\login.fxml
echo.
echo Option 2: Modifier le point d'entree pour charger login_with_captcha_choice.fxml
echo.
pause
