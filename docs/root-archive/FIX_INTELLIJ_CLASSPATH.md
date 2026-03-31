# Fix IntelliJ ClassNotFoundException for MainFX

## Problem
IntelliJ is not including `target/classes` in the classpath, causing:
```
Error: Could not find or load main class org.GreenLedger.MainFX
Caused by: java.lang.ClassNotFoundException: org.GreenLedger.MainFX
```

## Solutions

### Solution 1: Rebuild Project in IntelliJ (Quickest)
1. In IntelliJ, click **Build** menu
2. Select **Rebuild Project**
3. Wait for build to complete
4. Run MainFX again

### Solution 2: Reimport Maven Project
1. Open **Maven** tool window (View → Tool Windows → Maven)
2. Click the **Reload All Maven Projects** button (circular arrows icon)
3. Wait for import to complete
4. Click **Build** → **Build Project**
5. Run MainFX again

### Solution 3: Invalidate Caches
1. Click **File** → **Invalidate Caches...**
2. Check all options
3. Click **Invalidate and Restart**
4. After restart, rebuild project

### Solution 4: Fix Run Configuration Manually
1. Click **Run** → **Edit Configurations...**
2. Find your MainFX configuration
3. In **Use classpath of module**, select **Pi_Dev** (or your module name)
4. Ensure **Include dependencies with "Provided" scope** is checked
5. Click **OK** and run again

### Solution 5: Run from Terminal (Alternative)
Use the fixed run.bat script:
```batch
cd D:\PiDev\Pi_Dev
.\run.bat
```

## Verification
After applying any solution, verify:
```powershell
# Check if class exists
Test-Path "target\classes\org\GreenLedger\MainFX.class"
# Should return: True
```

## Root Cause
The pom.xml had merge conflicts that were just fixed:
- ✅ JavaFX mainClass now correctly set to `org.GreenLedger.MainFX`
- ✅ JavaFX modules include: controls, fxml, web, media, swing

IntelliJ needs to rebuild to recognize these changes.
