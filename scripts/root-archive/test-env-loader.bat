@echo off
echo Test EnvLoader...
echo.

echo Compilation de EnvLoader.java...
javac -d target/classes -cp "target/classes" src/main/java/Utils/EnvLoader.java

echo.
echo Creation du test...
echo package tools; > src\main\java\tools\TestEnvLoader.java
echo import Utils.EnvLoader; >> src\main\java\tools\TestEnvLoader.java
echo. >> src\main\java\tools\TestEnvLoader.java
echo public class TestEnvLoader { >> src\main\java\tools\TestEnvLoader.java
echo     public static void main(String[] args) { >> src\main\java\tools\TestEnvLoader.java
echo         System.out.println("=== Test EnvLoader ==="); >> src\main\java\tools\TestEnvLoader.java
echo         System.out.println(); >> src\main\java\tools\TestEnvLoader.java
echo         EnvLoader.load(); >> src\main\java\tools\TestEnvLoader.java
echo         System.out.println(); >> src\main\java\tools\TestEnvLoader.java
echo         System.out.println("Variables chargees:"); >> src\main\java\tools\TestEnvLoader.java
echo         System.out.println("  GMAIL_API_ENABLED = " + EnvLoader.get("GMAIL_API_ENABLED")); >> src\main\java\tools\TestEnvLoader.java
echo         System.out.println("  GMAIL_FROM_EMAIL = " + EnvLoader.get("GMAIL_FROM_EMAIL")); >> src\main\java\tools\TestEnvLoader.java
echo         System.out.println("  GMAIL_FROM_NAME = " + EnvLoader.get("GMAIL_FROM_NAME")); >> src\main\java\tools\TestEnvLoader.java
echo         System.out.println(); >> src\main\java\tools\TestEnvLoader.java
echo         System.out.println("OK Test termine !"); >> src\main\java\tools\TestEnvLoader.java
echo     } >> src\main\java\tools\TestEnvLoader.java
echo } >> src\main\java\tools\TestEnvLoader.java

echo Compilation du test...
javac -d target/classes -cp "target/classes" src/main/java/tools/TestEnvLoader.java

echo.
echo Lancement du test...
java -cp "target/classes" tools.TestEnvLoader

pause
