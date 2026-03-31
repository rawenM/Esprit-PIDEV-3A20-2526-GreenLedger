@echo off
chcp 65001 > nul
echo ========================================
echo COMPILATION - Integration Audit Log
echo ========================================
echo.

set JAVAFX_PATH=C:\Program Files\Java\javafx-sdk-23.0.1\lib

echo [1/3] Compilation LoginController...
javac -encoding UTF-8 -d target/classes -cp "target/classes;lib/*" --module-path "%JAVAFX_PATH%" --add-modules javafx.controls,javafx.fxml,javafx.web src/main/java/Controllers/LoginController.java 2>&1
if %ERRORLEVEL% NEQ 0 (
    echo ERREUR lors de la compilation de LoginController
    pause
    exit /b 1
)
echo OK - LoginController compile

echo.
echo [2/3] Compilation AdminUsersController...
javac -encoding UTF-8 -d target/classes -cp "target/classes;lib/*" --module-path "%JAVAFX_PATH%" --add-modules javafx.controls,javafx.fxml,javafx.web src/main/java/Controllers/AdminUsersController.java 2>&1
if %ERRORLEVEL% NEQ 0 (
    echo ERREUR lors de la compilation de AdminUsersController
    pause
    exit /b 1
)
echo OK - AdminUsersController compile

echo.
echo [3/3] Compilation ForgotPasswordController...
javac -encoding UTF-8 -d target/classes -cp "target/classes;lib/*" --module-path "%JAVAFX_PATH%" --add-modules javafx.controls,javafx.fxml,javafx.web src/main/java/Controllers/ForgotPasswordController.java 2>&1
if %ERRORLEVEL% NEQ 0 (
    echo ERREUR lors de la compilation de ForgotPasswordController
    pause
    exit /b 1
)
echo OK - ForgotPasswordController compile

echo.
echo ========================================
echo COMPILATION TERMINEE AVEC SUCCES
echo ========================================
echo.
echo Les controleurs ont ete integres avec le journal d'activite.
echo Toutes les actions seront maintenant enregistrees automatiquement.
echo.
pause
