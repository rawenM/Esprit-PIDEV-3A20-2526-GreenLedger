@echo off
chcp 65001 >nul
color 0A

echo ═══════════════════════════════════════════════════════════════
echo 📋 INSTALLATION DE LA TABLE AUDIT_LOG
echo ═══════════════════════════════════════════════════════════════
echo.

echo Entrez le mot de passe MySQL root:
set /p password=

echo.
echo Installation en cours...
echo.

mysql -u root -p%password% greenledger < database_audit_log.sql

if %ERRORLEVEL% EQU 0 (
    echo.
    echo ═══════════════════════════════════════════════════════════════
    echo ✅ TABLE CRÉÉE AVEC SUCCÈS!
    echo ═══════════════════════════════════════════════════════════════
    echo.
    echo Vérification:
    mysql -u root -p%password% -e "USE greenledger; SELECT COUNT(*) as nombre_logs FROM audit_log;"
    echo.
    echo Vous devriez voir 5 logs de test.
    echo.
) else (
    echo.
    echo ═══════════════════════════════════════════════════════════════
    echo ❌ ERREUR LORS DE LA CRÉATION
    echo ═══════════════════════════════════════════════════════════════
    echo.
    echo Vérifiez:
    echo   1. MySQL est démarré
    echo   2. Le mot de passe est correct
    echo   3. La base greenledger existe
    echo.
)

pause
