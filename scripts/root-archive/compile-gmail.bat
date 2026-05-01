@echo off
echo Compilation des services Gmail...
echo.

REM Définir le répertoire Maven local
set M2=%USERPROFILE%\.m2\repository

REM Construire le classpath avec toutes les dépendances
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
set CP=%CP%;%M2%\io\opencensus\opencensus-api\0.31.1\opencensus-api-0.31.1.jar
set CP=%CP%;%M2%\io\grpc\grpc-context\1.27.2\grpc-context-1.27.2.jar

echo Compilation de GmailApiService.java...
javac -cp "%CP%" -d target/classes src/main/java/Utils/GmailApiService.java

if %ERRORLEVEL% EQU 0 (
    echo.
    echo Compilation de UnifiedEmailService.java...
    javac -cp "%CP%" -d target/classes src/main/java/Utils/UnifiedEmailService.java
)

if %ERRORLEVEL% EQU 0 (
    echo.
    echo Compilation de TestGmailApi.java...
    javac -cp "%CP%" -d target/classes src/main/java/tools/TestGmailApi.java
)

if %ERRORLEVEL% EQU 0 (
    echo.
    echo ✅ Compilation réussie !
    echo.
    echo Vous pouvez maintenant lancer : test-gmail.bat
) else (
    echo.
    echo ❌ Erreur de compilation
)

pause
