@echo off
echo Test Email Reinitialisation...
echo.

REM Définir les variables d'environnement
set GMAIL_API_ENABLED=true
set GMAIL_FROM_EMAIL=ibrahimimajid058@gmail.com
set GMAIL_FROM_NAME=GreenLedger Team

REM Définir le répertoire Maven local
set M2=%USERPROFILE%\.m2\repository

REM Construire le classpath
set CP=target\classes
set CP=%CP%;%M2%\com\google\api-client\google-api-client\2.2.0\google-api-client-2.2.0.jar
set CP=%CP%;%M2%\com\google\oauth-client\google-oauth-client-jetty\1.34.1\google-oauth-client-jetty-1.34.1.jar
set CP=%CP%;%M2%\com\google\apis\google-api-services-gmail\v1-rev20220404-2.0.0\google-api-services-gmail-v1-rev20220404-2.0.0.jar
set CP=%CP%;%M2%\com\google\oauth-client\google-oauth-client\1.34.1\google-oauth-client-1.34.1.jar
set CP=%CP%;%M2%\com\google\oauth-client\google-oauth-client-java6\1.34.1\google-oauth-client-java6-1.34.1.jar
set CP=%CP%;%M2%\com\google\http-client\google-http-client\1.42.3\google-http-client-1.42.3.jar
set CP=%CP%;%M2%\com\google\http-client\google-http-client-gson\1.42.3\google-http-client-gson-1.42.3.jar
set CP=%CP%;%M2%\com\google\code\gson\gson\2.10.1\gson-2.10.1.jar
set CP=%CP%;%M2%\com\google\guava\guava\31.1-jre\guava-31.1-jre.jar
set CP=%CP%;%M2%\com\google\code\findbugs\jsr305\3.0.2\jsr305-3.0.2.jar
set CP=%CP%;%M2%\org\apache\httpcomponents\httpclient\4.5.13\httpclient-4.5.13.jar
set CP=%CP%;%M2%\org\apache\httpcomponents\httpcore\4.4.15\httpcore-4.4.15.jar
set CP=%CP%;%M2%\commons-logging\commons-logging\1.2\commons-logging-1.2.jar
set CP=%CP%;%M2%\commons-codec\commons-codec\1.15\commons-codec-1.15.jar
set CP=%CP%;%M2%\com\sun\mail\jakarta.mail\2.0.1\jakarta.mail-2.0.1.jar
set CP=%CP%;%M2%\com\sun\activation\jakarta.activation\2.0.1\jakarta.activation-2.0.1.jar
set CP=%CP%;%M2%\io\opencensus\opencensus-api\0.31.1\opencensus-api-0.31.1.jar
set CP=%CP%;%M2%\io\opencensus\opencensus-contrib-http-util\0.31.1\opencensus-contrib-http-util-0.31.1.jar
set CP=%CP%;%M2%\io\grpc\grpc-context\1.27.2\grpc-context-1.27.2.jar

echo Compilation de TestResetPassword.java...
javac -encoding UTF-8 -cp "%CP%" -d target/classes src/main/java/tools/TestResetPassword.java

if %ERRORLEVEL% EQU 0 (
    echo.
    echo Lancement du test...
    java -cp "%CP%" tools.TestResetPassword
) else (
    echo.
    echo Erreur de compilation
)

pause
