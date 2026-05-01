@echo off
echo ========================================
echo COMPILATION CHART.JS INTEGRATION
echo ========================================

REM Définir le répertoire Maven local
set M2=%USERPROFILE%\.m2\repository
set JAVAFXDIR=%M2%\org\openjfx

REM Construire le module path pour JavaFX
set MODULEPATH=%JAVAFXDIR%\javafx-controls\20.0.2\javafx-controls-20.0.2-win.jar
set MODULEPATH=%MODULEPATH%;%JAVAFXDIR%\javafx-graphics\20.0.2\javafx-graphics-20.0.2-win.jar
set MODULEPATH=%MODULEPATH%;%JAVAFXDIR%\javafx-base\20.0.2\javafx-base-20.0.2-win.jar
set MODULEPATH=%MODULEPATH%;%JAVAFXDIR%\javafx-fxml\20.0.2\javafx-fxml-20.0.2-win.jar
set MODULEPATH=%MODULEPATH%;%JAVAFXDIR%\javafx-web\20.0.2\javafx-web-20.0.2-win.jar

REM Construire le classpath
set CP=target\classes
set CP=%CP%;%M2%\com\google\code\gson\gson\2.10.1\gson-2.10.1.jar
set CP=%CP%;%M2%\mysql\mysql-connector-java\8.0.26\mysql-connector-java-8.0.26.jar

echo.
echo [1/2] Compilation de ChartDataService.java...
javac -encoding UTF-8 --module-path "%MODULEPATH%" --add-modules javafx.controls,javafx.fxml,javafx.web -cp "%CP%" -d target/classes src/main/java/Utils/ChartDataService.java
if %ERRORLEVEL% NEQ 0 (
    echo Erreur lors de la compilation de ChartDataService.java
    pause
    exit /b 1
)
echo ChartDataService.java compile avec succes!

echo.
echo [2/2] Compilation de UserStatisticsController.java...
javac -encoding UTF-8 --module-path "%MODULEPATH%" --add-modules javafx.controls,javafx.fxml,javafx.web -cp "%CP%" -d target/classes src/main/java/Controllers/UserStatisticsController.java
if %ERRORLEVEL% NEQ 0 (
    echo Erreur lors de la compilation de UserStatisticsController.java
    pause
    exit /b 1
)
echo UserStatisticsController.java compile avec succes!

echo.
echo [3/3] Recompilation de AdminUsersController.java...
javac -encoding UTF-8 --module-path "%MODULEPATH%" --add-modules javafx.controls,javafx.fxml,javafx.web -cp "%CP%" -d target/classes src/main/java/Controllers/AdminUsersController.java
if %ERRORLEVEL% NEQ 0 (
    echo Erreur lors de la compilation de AdminUsersController.java
    pause
    exit /b 1
)
echo AdminUsersController.java compile avec succes!

echo.
echo ========================================
echo COMPILATION TERMINEE AVEC SUCCES!
echo ========================================
echo.
echo FICHIERS CREES:
echo - src/main/resources/charts/user-statistics.html (Chart.js HTML)
echo - src/main/java/Utils/ChartDataService.java (Generation donnees)
echo - src/main/java/Controllers/UserStatisticsController.java (Controleur)
echo - src/main/resources/fxml/user_statistics.fxml (Interface)
echo.
echo PROCHAINES ETAPES:
echo 1. Lancez l'application avec run.bat
echo 2. Connectez-vous en tant qu'admin
echo 3. Cliquez sur "Statistiques" dans le menu
echo 4. Admirez vos graphiques Chart.js!
echo.
pause
