@echo off
echo ========================================
echo Test de configuration Google reCAPTCHA
echo ========================================
echo.

REM Définir le répertoire Maven local
set M2=%USERPROFILE%\.m2\repository
set JAVAFXDIR=%M2%\org\openjfx

REM Construire le classpath
set CP=target\classes
set CP=%CP%;%M2%\com\google\code\gson\gson\2.10.1\gson-2.10.1.jar
set CP=%CP%;%M2%\com\google\http-client\google-http-client\1.42.3\google-http-client-1.42.3.jar

echo Compilation de CaptchaService.java...
javac -encoding UTF-8 -cp "%CP%" -d target/classes src/main/java/Utils/CaptchaService.java
if %ERRORLEVEL% NEQ 0 (
    echo ❌ Erreur de compilation
    pause
    exit /b 1
)
echo ✅ CaptchaService.java compile

echo.
echo Compilation de TestRecaptchaConfig.java...
javac -encoding UTF-8 -cp "%CP%" -d target/classes src/main/java/tools/TestRecaptchaConfig.java
if %ERRORLEVEL% NEQ 0 (
    echo ❌ Erreur de compilation
    pause
    exit /b 1
)
echo ✅ TestRecaptchaConfig.java compile

echo.
echo ========================================
echo Execution du test...
echo ========================================
echo.

java -cp "%CP%" tools.TestRecaptchaConfig

echo.
pause
