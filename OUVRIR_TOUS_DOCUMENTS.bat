@echo off
chcp 65001 >nul
color 0A

echo ═══════════════════════════════════════════════════════════════
echo 📚 OUVERTURE DE TOUS LES DOCUMENTS IMPORTANTS
echo ═══════════════════════════════════════════════════════════════
echo.
echo Ouverture en cours...
echo.

timeout /t 1 >nul

echo ✓ Ouverture de COMMENCEZ_PAR_ICI.txt...
start COMMENCEZ_PAR_ICI.txt
timeout /t 1 >nul

echo ✓ Ouverture de LANCER_PRESENTATION.txt...
start LANCER_PRESENTATION.txt
timeout /t 1 >nul

echo ✓ Ouverture de PHRASES_JURY_EXACTES.txt...
start PHRASES_JURY_EXACTES.txt
timeout /t 1 >nul

echo ✓ Ouverture de RESUME_VISUEL_PROJET.txt...
start RESUME_VISUEL_PROJET.txt
timeout /t 1 >nul

echo ✓ Ouverture de GUIDE_PRESENTATION_JURY.md...
start GUIDE_PRESENTATION_JURY.md
timeout /t 1 >nul

echo.
echo ═══════════════════════════════════════════════════════════════
echo ✅ TOUS LES DOCUMENTS SONT OUVERTS!
echo ═══════════════════════════════════════════════════════════════
echo.
echo Lisez-les dans cet ordre:
echo   1. COMMENCEZ_PAR_ICI.txt
echo   2. LANCER_PRESENTATION.txt
echo   3. PHRASES_JURY_EXACTES.txt
echo   4. RESUME_VISUEL_PROJET.txt
echo   5. GUIDE_PRESENTATION_JURY.md
echo.
pause
