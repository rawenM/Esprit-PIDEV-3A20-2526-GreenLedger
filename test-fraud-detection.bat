@echo off
echo Test Detection de Fraude avec IA...
echo.

echo Compilation des classes...
javac -encoding UTF-8 -d target/classes -cp "target/classes" "src/main/java/Models/FraudDetectionResult.java"
javac -encoding UTF-8 -d target/classes -cp "target/classes" "src/main/java/Services/FraudDetectionService.java"
javac -encoding UTF-8 -d target/classes -cp "target/classes" "src/main/java/tools/TestFraudDetection.java"

echo.
echo Lancement du test...
java -cp "target/classes" tools.TestFraudDetection

pause
