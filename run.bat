@echo off
REM Script pour lancer l'application Green Ledger

setlocal enabledelayedexpansion

REM Définir les variables d'environnement pour Gmail API
set GMAIL_API_ENABLED=true
set GMAIL_FROM_EMAIL=ibrahimimajid058@gmail.com
set GMAIL_FROM_NAME=GreenLedger Team

set JAVAFXDIR=%USERPROFILE%\.m2\repository\org\openjfx

echo [INFO] Application launched...

java ^
  --module-path "%JAVAFXDIR%\javafx-controls\20.0.2\javafx-controls-20.0.2-win.jar;%JAVAFXDIR%\javafx-graphics\20.0.2\javafx-graphics-20.0.2-win.jar;%JAVAFXDIR%\javafx-base\20.0.2\javafx-base-20.0.2-win.jar;%JAVAFXDIR%\javafx-fxml\20.0.2\javafx-fxml-20.0.2-win.jar;%JAVAFXDIR%\javafx-web\20.0.2\javafx-web-20.0.2-win.jar;%JAVAFXDIR%\javafx-media\20.0.2\javafx-media-20.0.2-win.jar" ^
  --add-modules javafx.controls,javafx.fxml,javafx.web,javafx.media ^
  -cp "target\classes;%USERPROFILE%\.m2\repository\mysql\mysql-connector-java\8.0.26\mysql-connector-java-8.0.26.jar;%USERPROFILE%\.m2\repository\org\mindrot\jbcrypt\0.4\jbcrypt-0.4.jar;%USERPROFILE%\.m2\repository\com\sun\mail\jakarta.mail\2.0.1\jakarta.mail-2.0.1.jar;%USERPROFILE%\.m2\repository\com\sun\activation\jakarta.activation\2.0.1\jakarta.activation-2.0.1.jar;%USERPROFILE%\.m2\repository\org\apache\pdfbox\pdfbox\2.0.29\pdfbox-2.0.29.jar;%USERPROFILE%\.m2\repository\org\apache\pdfbox\fontbox\2.0.29\fontbox-2.0.29.jar;%USERPROFILE%\.m2\repository\commons-logging\commons-logging\1.2\commons-logging-1.2.jar;%USERPROFILE%\.m2\repository\org\apache\opennlp\opennlp-tools\2.3.3\opennlp-tools-2.3.3.jar;%USERPROFILE%\.m2\repository\org\slf4j\slf4j-api\1.7.36\slf4j-api-1.7.36.jar;%USERPROFILE%\.m2\repository\org\slf4j\slf4j-simple\1.7.36\slf4j-simple-1.7.36.jar;%USERPROFILE%\.m2\repository\org\apache\httpcomponents\client5\httpclient5\5.2.1\httpclient5-5.2.1.jar;%USERPROFILE%\.m2\repository\org\apache\httpcomponents\core5\httpcore5\5.2\httpcore5-5.2.jar;%USERPROFILE%\.m2\repository\org\apache\httpcomponents\core5\httpcore5-h2\5.2\httpcore5-h2-5.2.jar;%USERPROFILE%\.m2\repository\com\google\code\gson\gson\2.10.1\gson-2.10.1.jar;%USERPROFILE%\.m2\repository\com\google\api-client\google-api-client\2.2.0\google-api-client-2.2.0.jar;%USERPROFILE%\.m2\repository\com\google\oauth-client\google-oauth-client-jetty\1.34.1\google-oauth-client-jetty-1.34.1.jar;%USERPROFILE%\.m2\repository\com\google\apis\google-api-services-gmail\v1-rev20220404-2.0.0\google-api-services-gmail-v1-rev20220404-2.0.0.jar;%USERPROFILE%\.m2\repository\com\google\oauth-client\google-oauth-client\1.34.1\google-oauth-client-1.34.1.jar;%USERPROFILE%\.m2\repository\com\google\oauth-client\google-oauth-client-java6\1.34.1\google-oauth-client-java6-1.34.1.jar;%USERPROFILE%\.m2\repository\com\google\http-client\google-http-client\1.42.3\google-http-client-1.42.3.jar;%USERPROFILE%\.m2\repository\com\google\http-client\google-http-client-gson\1.42.3\google-http-client-gson-1.42.3.jar;%USERPROFILE%\.m2\repository\io\opencensus\opencensus-api\0.31.1\opencensus-api-0.31.1.jar;%USERPROFILE%\.m2\repository\io\opencensus\opencensus-contrib-http-util\0.31.1\opencensus-contrib-http-util-0.31.1.jar;%USERPROFILE%\.m2\repository\io\grpc\grpc-context\1.27.2\grpc-context-1.27.2.jar;%USERPROFILE%\.m2\repository\com\google\guava\guava\31.1-jre\guava-31.1-jre.jar;%USERPROFILE%\.m2\repository\com\google\code\findbugs\jsr305\3.0.2\jsr305-3.0.2.jar" ^
  org.GreenLedger.MainFX

pause

