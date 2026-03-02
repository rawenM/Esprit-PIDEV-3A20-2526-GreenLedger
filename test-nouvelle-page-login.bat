@echo off
echo ========================================
echo TEST: Nouvelle Page de Login
echo ========================================
echo.

echo CHANGEMENT APPLIQUE:
echo --------------------
echo Ô£à MainFX.java modifie
echo Ô£à login.fxml -^> login_with_captcha_choice.fxml
echo Ô£à reCAPTCHA selectionne par defaut
echo.

echo VERIFICATION:
echo -------------
if exist "target\classes\org\GreenLedger\MainFX.class" (
    echo Ô£à MainFX.class compile
) else (
    echo ÔØî MainFX.class non trouve
)

if exist "target\classes\fxml\login_with_captcha_choice.fxml" (
    echo Ô£à login_with_captcha_choice.fxml present
) else (
    echo ÔØî login_with_captcha_choice.fxml non trouve
)

echo.
echo ========================================
echo LANCER L'APPLICATION
echo ========================================
echo.
echo Commande: run.bat
echo.
echo RESULTAT ATTENDU:
echo -----------------
echo Ô£à Page de login avec 3 options CAPTCHA
echo Ô£à reCAPTCHA selectionne par defaut
echo Ô£à Case "Je ne suis pas un robot" visible
echo Ô£à PAS d'equation mathematique
echo.
echo Appuyez sur une touche pour lancer l'application...
pause >nul

echo.
echo Lancement de l'application...
call run.bat
