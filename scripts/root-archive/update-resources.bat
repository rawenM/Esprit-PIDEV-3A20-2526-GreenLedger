@echo off
echo ========================================
echo MISE A JOUR DES RESSOURCES
echo ========================================

echo Copie de user_statistics.fxml...
copy /Y "src\main\resources\fxml\user_statistics.fxml" "target\classes\fxml\user_statistics.fxml"
if %ERRORLEVEL% NEQ 0 (
    echo Erreur lors de la copie de user_statistics.fxml
    pause
    exit /b 1
)
echo user_statistics.fxml copie avec succes!

echo.
echo Copie de user-statistics.html...
copy /Y "src\main\resources\charts\user-statistics.html" "target\classes\charts\user-statistics.html"
if %ERRORLEVEL% NEQ 0 (
    echo Erreur lors de la copie de user-statistics.html
    pause
    exit /b 1
)
echo user-statistics.html copie avec succes!

echo.
echo ========================================
echo RESSOURCES MISES A JOUR!
echo ========================================
echo.
echo Les fichiers suivants ont ete mis a jour:
echo - target/classes/fxml/user_statistics.fxml
echo - target/classes/charts/user-statistics.html
echo.
echo Vous pouvez maintenant relancer l'application avec run.bat
echo.
pause
