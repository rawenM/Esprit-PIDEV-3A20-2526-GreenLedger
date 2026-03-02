@echo off
echo ========================================
echo Activation reCAPTCHA par defaut
echo ========================================
echo.

echo [1/2] Copie du fichier FXML...
copy /Y "src\main\resources\fxml\login_with_captcha_choice.fxml" "target\classes\fxml\login_with_captcha_choice.fxml" >nul 2>&1
if %ERRORLEVEL% EQU 0 (
    echo Ô£à FXML copie avec succes
) else (
    echo ÔØî Erreur lors de la copie du FXML
    pause
    exit /b 1
)

echo.
echo [2/2] Le controleur Java a deja ete modifie
echo Ô£à Modifications appliquees

echo.
echo ========================================
echo Ô£à MISE A JOUR TERMINEE !
echo ========================================
echo.
echo CHANGEMENTS:
echo Ô£à reCAPTCHA selectionne par defaut (au lieu d'Equation)
echo Ô£à Interface "Je ne suis pas un robot" visible
echo Ô£à Les 3 methodes restent disponibles
echo.
echo PROCHAINES ETAPES:
echo 1. Lancer l'application: run.bat
echo 2. Verifier que reCAPTCHA est selectionne
echo 3. Tester la connexion avec reCAPTCHA
echo.
echo NOTE: Si vous voyez encore l'equation, fermez completement
echo l'application et relancez-la.
echo.
pause
