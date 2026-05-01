@echo off
echo ========================================
echo RECOMPILATION DU SYSTEME DE LOGIN
echo ========================================
echo.

echo [1/3] Suppression des anciennes classes...
if exist "target\classes\Controllers\LoginController.class" (
    del /q "target\classes\Controllers\LoginController.class"
    echo Ô£à LoginController.class supprime
)
if exist "target\classes\Controllers\ForgotPasswordController.class" (
    del /q "target\classes\Controllers\ForgotPasswordController.class"
    echo Ô£à ForgotPasswordController.class supprime
)

echo.
echo [2/3] Compilation du nouveau systeme...
call compile-forgot-password.bat

echo.
echo [3/3] Verification...
if exist "target\classes\Controllers\ForgotPasswordController.class" (
    echo Ô£à ForgotPasswordController.class compile
) else (
    echo ÔØî Erreur: ForgotPasswordController.class non trouve
)

if exist "target\classes\Controllers\LoginController.class" (
    echo Ô£à LoginController.class compile
) else (
    echo ÔØî Erreur: LoginController.class non trouve
)

echo.
echo ========================================
echo Ô£à RECOMPILATION TERMINEE !
echo ========================================
echo.
echo MAINTENANT:
echo 1. Lancez l'application: run.bat
echo 2. Cliquez sur "Mot de passe oublie ?"
echo 3. Entrez votre email
echo 4. Verifiez l'email recu
echo.
echo L'email doit contenir un CODE a 6 chiffres, PAS de lien !
echo.
pause
