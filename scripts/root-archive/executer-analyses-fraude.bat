@echo off
echo ========================================
echo GENERER ANALYSES DE FRAUDE
echo ========================================
echo.

echo [INFO] Generation des analyses de fraude pour les utilisateurs existants...
echo.

mysql -u root -p greenledger < generer_analyses_fraude_existantes.sql

if %ERRORLEVEL% NEQ 0 (
    echo.
    echo [ERREUR] Echec de l'execution du script
    echo.
    pause
    exit /b 1
)

echo.
echo ========================================
echo ANALYSES GENEREES AVEC SUCCES
echo ========================================
echo.
echo Tous les utilisateurs ont maintenant une analyse de fraude.
echo Vous pouvez cliquer sur "Detail Fraude" pour voir les details.
echo.
pause
