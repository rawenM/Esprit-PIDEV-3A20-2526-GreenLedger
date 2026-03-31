@echo off
echo ========================================
echo COMPILATION: LoginController (2 choix)
echo ========================================
echo.

cd /d "%~dp0"

echo [1/2] Compilation de LoginController.java...
javac -encoding UTF-8 -d target/classes -cp "target/classes;lib/*" src/main/java/Controllers/LoginController.java

if %ERRORLEVEL% NEQ 0 (
    echo.
    echo [ERREUR] Echec de la compilation
    echo.
    pause
    exit /b 1
)

echo [OK] LoginController compile
echo.

echo [2/2] Copie de login.fxml...
xcopy /Y /Q "src\main\resources\fxml\login.fxml" "target\classes\fxml\" >nul 2>&1

if %ERRORLEVEL% NEQ 0 (
    echo [ERREUR] Echec de la copie du FXML
    pause
    exit /b 1
)

echo [OK] FXML copie
echo.
echo ========================================
echo COMPILATION TERMINEE AVEC SUCCES
echo ========================================
echo.
echo Vous pouvez maintenant lancer l'application avec:
echo   ./run.bat
echo.
pause
