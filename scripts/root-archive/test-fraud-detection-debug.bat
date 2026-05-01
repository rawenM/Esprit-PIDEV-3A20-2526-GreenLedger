@echo off
echo ============================================================
echo TEST DE DETECTION DE FRAUDE - DEBUG
echo ============================================================
echo.

echo Compilation...
call mvn clean compile -q

echo.
echo Execution du test...
echo.

java -cp "target/classes;%USERPROFILE%\.m2\repository\org\mindrot\jbcrypt\0.4\jbcrypt-0.4.jar;%USERPROFILE%\.m2\repository\mysql\mysql-connector-java\8.0.33\mysql-connector-java-8.0.33.jar" tools.TestFraudDetectionDebug

echo.
echo ============================================================
echo Test termine
echo ============================================================
pause
