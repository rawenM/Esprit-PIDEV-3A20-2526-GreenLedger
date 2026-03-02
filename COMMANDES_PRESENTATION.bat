@echo off
chcp 65001 >nul
color 0A

echo ═══════════════════════════════════════════════════════════════
echo 🚀 GREENLEDGER - COMMANDES RAPIDES PRÉSENTATION
echo ═══════════════════════════════════════════════════════════════
echo.

:MENU
echo.
echo Choisissez une action:
echo.
echo [1] 🚀 Lancer l'application (PRÉSENTATION)
echo [2] 📚 Ouvrir le guide de présentation
echo [3] 📄 Ouvrir les phrases pour le jury
echo [4] 📊 Ouvrir le résumé visuel
echo [5] ✅ Ouvrir la checklist
echo [6] 🔄 Mettre à jour les ressources
echo [7] ❌ Quitter
echo.
set /p choice="Votre choix (1-7): "

if "%choice%"=="1" goto LAUNCH
if "%choice%"=="2" goto GUIDE
if "%choice%"=="3" goto PHRASES
if "%choice%"=="4" goto RESUME
if "%choice%"=="5" goto CHECKLIST
if "%choice%"=="6" goto UPDATE
if "%choice%"=="7" goto END

echo Choix invalide!
goto MENU

:LAUNCH
echo.
echo ═══════════════════════════════════════════════════════════════
echo 🚀 LANCEMENT DE L'APPLICATION
echo ═══════════════════════════════════════════════════════════════
echo.
echo ⚠️  AVANT DE LANCER:
echo    1. MySQL est-il démarré? (O/N)
set /p mysql="   Réponse: "
if /i "%mysql%"=="N" (
    echo.
    echo ❌ Veuillez démarrer MySQL d'abord!
    pause
    goto MENU
)

echo    2. Avez-vous fermé tous les navigateurs? (O/N)
set /p browsers="   Réponse: "
if /i "%browsers%"=="N" (
    echo.
    echo ⚠️  Fermez les navigateurs pour éviter les conflits WebView
    pause
)

echo.
echo ✅ Lancement de l'application...
echo.
call run.bat
goto MENU

:GUIDE
echo.
echo 📚 Ouverture du guide de présentation...
start GUIDE_PRESENTATION_JURY.md
goto MENU

:PHRASES
echo.
echo 📄 Ouverture des phrases pour le jury...
start PHRASES_JURY_EXACTES.txt
goto MENU

:RESUME
echo.
echo 📊 Ouverture du résumé visuel...
start RESUME_VISUEL_PROJET.txt
goto MENU

:CHECKLIST
echo.
echo ✅ CHECKLIST AVANT PRÉSENTATION
echo ═══════════════════════════════════════════════════════════════
echo.
echo □ MySQL démarré
echo □ Navigateurs fermés
echo □ Application testée
echo □ Email de test ouvert
echo □ Au moins 5 utilisateurs en base
echo □ Au moins 2 avec fraude détectée
echo □ Documents lus (GUIDE_PRESENTATION_JURY.md)
echo.
pause
goto MENU

:UPDATE
echo.
echo 🔄 Mise à jour des ressources...
call update-resources.bat
goto MENU

:END
echo.
echo ═══════════════════════════════════════════════════════════════
echo 🍀 BONNE CHANCE POUR VOTRE PRÉSENTATION!
echo ═══════════════════════════════════════════════════════════════
echo.
pause
exit
