@echo off
chcp 65001 > nul
echo ========================================
echo CORRECTION ET COMPILATION AUDIT LOG
echo ========================================
echo.

REM Définir le répertoire Maven local
set M2=%USERPROFILE%\.m2\repository
set JAVAFXDIR=%M2%\org\openjfx

REM Construire le classpath complet
set CP=target\classes
set CP=%CP%;%M2%\mysql\mysql-connector-java\8.0.26\mysql-connector-java-8.0.26.jar
set CP=%CP%;%M2%\org\mindrot\jbcrypt\0.4\jbcrypt-0.4.jar
set CP=%CP%;%M2%\com\google\api-client\google-api-client\2.2.0\google-api-client-2.2.0.jar
set CP=%CP%;%M2%\com\google\oauth-client\google-oauth-client-jetty\1.34.1\google-oauth-client-jetty-1.34.1.jar
set CP=%CP%;%M2%\com\google\apis\google-api-services-gmail\v1-rev20220404-2.0.0\google-api-services-gmail-v1-rev20220404-2.0.0.jar
set CP=%CP%;%M2%\com\google\oauth-client\google-oauth-client\1.34.1\google-oauth-client-1.34.1.jar
set CP=%CP%;%M2%\com\google\http-client\google-http-client\1.42.3\google-http-client-1.42.3.jar
set CP=%CP%;%M2%\com\google\http-client\google-http-client-gson\1.42.3\google-http-client-gson-1.42.3.jar
set CP=%CP%;%M2%\com\google\code\gson\gson\2.10.1\gson-2.10.1.jar
set CP=%CP%;%M2%\com\sun\mail\jakarta.mail\2.0.1\jakarta.mail-2.0.1.jar
set CP=%CP%;%JAVAFXDIR%\javafx-controls\20.0.2\javafx-controls-20.0.2.jar
set CP=%CP%;%JAVAFXDIR%\javafx-fxml\20.0.2\javafx-fxml-20.0.2.jar
set CP=%CP%;%JAVAFXDIR%\javafx-graphics\20.0.2\javafx-graphics-20.0.2.jar
set CP=%CP%;%JAVAFXDIR%\javafx-base\20.0.2\javafx-base-20.0.2.jar
set CP=%CP%;%JAVAFXDIR%\javafx-web\20.0.2\javafx-web-20.0.2.jar

echo [1/4] Compilation AuditLogDAOImpl.java...
javac -encoding UTF-8 -cp "%CP%" -d target/classes src/main/java/dao/AuditLogDAOImpl.java 2>&1
if %ERRORLEVEL% NEQ 0 (
    echo Erreur lors de la compilation de AuditLogDAOImpl
    pause
    exit /b 1
)
echo OK - AuditLogDAOImpl compile

echo.
echo [2/4] Compilation LoginController.java...
javac -encoding UTF-8 -cp "%CP%" -d target/classes src/main/java/Controllers/LoginController.java 2>&1
if %ERRORLEVEL% NEQ 0 (
    echo Erreur lors de la compilation de LoginController
    pause
    exit /b 1
)
echo OK - LoginController compile

echo.
echo [3/4] Compilation AdminUsersController.java...
javac -encoding UTF-8 -cp "%CP%" -d target/classes src/main/java/Controllers/AdminUsersController.java 2>&1
if %ERRORLEVEL% NEQ 0 (
    echo Erreur lors de la compilation de AdminUsersController
    pause
    exit /b 1
)
echo OK - AdminUsersController compile

echo.
echo [4/4] Compilation ForgotPasswordController.java...
javac -encoding UTF-8 -cp "%CP%" -d target/classes src/main/java/Controllers/ForgotPasswordController.java 2>&1
if %ERRORLEVEL% NEQ 0 (
    echo Erreur lors de la compilation de ForgotPasswordController
    pause
    exit /b 1
)
echo OK - ForgotPasswordController compile

echo.
echo ========================================
echo COMPILATION TERMINEE AVEC SUCCES
echo ========================================
echo.
echo Le journal d'activite est maintenant operationnel!
echo.
echo PROCHAINE ETAPE:
echo Lancez l'application avec: run.bat
echo.
pause
