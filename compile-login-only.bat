@echo off
echo ========================================
echo COMPILATION DE LoginController UNIQUEMENT
echo ========================================

REM Définir le répertoire Maven local
set M2=%USERPROFILE%\.m2\repository
set JAVAFXDIR=%M2%\org\openjfx

REM Construire le module path pour JavaFX
set MODULEPATH=%JAVAFXDIR%\javafx-controls\20.0.2\javafx-controls-20.0.2-win.jar
set MODULEPATH=%MODULEPATH%;%JAVAFXDIR%\javafx-graphics\20.0.2\javafx-graphics-20.0.2-win.jar
set MODULEPATH=%MODULEPATH%;%JAVAFXDIR%\javafx-base\20.0.2\javafx-base-20.0.2-win.jar
set MODULEPATH=%MODULEPATH%;%JAVAFXDIR%\javafx-fxml\20.0.2\javafx-fxml-20.0.2-win.jar
set MODULEPATH=%MODULEPATH%;%JAVAFXDIR%\javafx-web\20.0.2\javafx-web-20.0.2-win.jar
set MODULEPATH=%MODULEPATH%;%JAVAFXDIR%\javafx-swing\20.0.2\javafx-swing-20.0.2-win.jar

REM Construire le classpath pour les autres dépendances
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

echo Compilation de LoginController.java...
javac -encoding UTF-8 --module-path "%MODULEPATH%" --add-modules javafx.controls,javafx.fxml,javafx.web -cp "%CP%" -d target/classes src/main/java/Controllers/LoginController.java
if %ERRORLEVEL% NEQ 0 (
    echo Erreur lors de la compilation de LoginController.java
    pause
    exit /b 1
)
echo LoginController.java compile avec succes!
echo.
echo Vous pouvez maintenant lancer l'application avec run.bat
pause
