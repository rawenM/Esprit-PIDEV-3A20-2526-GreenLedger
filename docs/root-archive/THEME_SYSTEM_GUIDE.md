# 🎨 GreenLedger Theme System Implementation Guide

## Overview
Your app now has a **centralized theme system** that persists across all FXML views. Users can switch themes from any view, and the selection is saved in preferences.

## 🏗️ Architecture

### How It Works
- **Scene-Level Styling**: Themes are applied to the `Scene`, not individual FXMLs
- **Persistent Navigation**: Theme persists when switching between FXMLs (greenwallet, expertProjet, etc.)  
- **User Preferences**: Theme choice is saved and restored on app restart
- **Base + Overlay**: Base structure + theme color overlays

### File Structure
```
src/main/resources/
├── themes/
│   ├── app-base.css       ← Structural styles (complete fallback)
│   ├── theme-light.css    ← Light theme colors (current design)
│   ├── theme-dark.css     ← Dark mode
│   ├── theme-forest.css   ← Green variant
│   └── theme-ocean.css    ← Blue variant
└── app.css               ← Legacy (can keep for compatibility)
```

### Java Classes
```
src/main/java/
├── Utils/
│   └── ThemeManager.java        ← Singleton managing theme state
├── Controllers/
│   └── BaseController.java      ← Base class with theme selector
└── org/GreenLedger/
    └── MainFX.java             ← Updated with theme initialization
```

---

## ✅ What's Already Done

### 1. ThemeManager (Utils/ThemeManager.java)
- ✅ Singleton pattern
- ✅ Saves theme preference to system
- ✅ Applies themes at Scene level
- ✅ 4 Built-in themes: Light, Dark, Forest, Ocean

### 2. MainFX.java
- ✅ Initializes ThemeManager with Scene on startup
- ✅ Loads last-used theme automatically
- ✅ Theme persists through `setRoot()` navigation

### 3. BaseController.java
- ✅ Reusable controller with theme selector logic
- ✅ Auto-populates ComboBox with themes
- ✅ Handles theme switching

### 4. Example Implementation
- ✅ GreenWalletController extends BaseController
- ✅ greenwallet.fxml has theme selector in sidebar
- ✅ Theme selector at bottom of sidebar with spacer

---

## 🔧 How to Add Theme Selector to Any View

### Step 1: Add Theme Selector to FXML

Add this to your sidebar or top bar in any FXML:

```xml
<!-- Theme Selector - Can go anywhere in your UI -->
<VBox spacing="6">
    <Label text="THÈME" styleClass="section-title-sidebar"/>
    <ComboBox fx:id="themeSelector"
              styleClass="field"
              prefHeight="32"
              style="-fx-font-size: 11px;"/>
</VBox>
```

**Pro Tip**: Add a spacer before it to push to bottom:
```xml
<!-- Spacer pushes content to bottom -->
<Region VBox.vgrow="ALWAYS"/>
```

### Step 2: Update Controller

**Option A - Extend BaseController** (Recommended):
```java
package Controllers;

import javafx.fxml.FXML;

public class YourController extends BaseController {
    
    @FXML
    public void initialize() {
        super.initialize(); // Enables theme switching!
        
        // Your initialization code...
    }
}
```

**Option B - Manual Integration**:
```java
package Controllers;

import Utils.ThemeManager;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;

public class YourController {
    
    @FXML
    private ComboBox<String> themeSelector;
    
    @FXML
    public void initialize() {
        // Populate theme selector
        themeSelector.setItems(FXCollections.observableArrayList(
            ThemeManager.getInstance().getThemeDisplayNames()
        ));
        
        // Set current theme
        String currentTheme = ThemeManager.getInstance().getCurrentTheme();
        themeSelector.setValue(
            ThemeManager.getInstance().getDisplayName(currentTheme)
        );
        
        // Handle changes
        themeSelector.setOnAction(e -> {
            String selected = ThemeManager.getInstance()
                .themeFromDisplayName(themeSelector.getValue());
            ThemeManager.getInstance().setTheme(selected);
        });
    }
}
```

### Step 3: Remove Old Stylesheet References (Optional)

The theme system handles stylesheets at Scene level, so you can remove:
```xml
<!-- OLD - Can remove this -->
<HBox ... stylesheets="@app.css">

<!-- NEW - No stylesheet needed! -->
<HBox ...>
```

But keeping it won't cause conflicts - Scene-level styles take precedence.

---

## 🎨 Creating Custom Themes

### Quick Method - Copy & Modify Existing Theme

1. Copy an existing theme:
```powershell
Copy-Item "src\main\resources\themes\theme-light.css" `
          "src\main\resources\themes\theme-purple.css"
```

2. Edit colors in the new file:
```css
/* theme-purple.css */
.btn-primary {
    -fx-background-color: #8B5CF6;  /* Purple */
    -fx-text-fill: #FFFFFF;
}

.stat-value {
    -fx-text-fill: #8B5CF6;  /* Purple */
}
/* ... etc */
```

3. Register in ThemeManager.java:
```java
public static final String THEME_PURPLE = "purple";

public List<String> getAvailableThemes() {
    return Arrays.asList(
        THEME_LIGHT, THEME_DARK, THEME_FOREST, THEME_OCEAN, THEME_PURPLE
    );
}

public List<String> getThemeDisplayNames() {
    return Arrays.asList(
        "Light", "Dark", "Forest", "Ocean", "Purple"
    );
}
```

4. Update conversion method:
```java
public String themeFromDisplayName(String displayName) {
    switch (displayName.toLowerCase()) {
        case "light": return THEME_LIGHT;
        case "dark": return THEME_DARK;
        case "forest": return THEME_FOREST;
        case "ocean": return THEME_OCEAN;
        case "purple": return THEME_PURPLE;
        default: return THEME_LIGHT;
    }
}
```

---

## 🌈 Built-in Themes

### Light (Default)
- **Background**: Soft sage white (#F7F9F8)
- **Primary**: Forest green (#2D5F3F)
- **Use Case**: Professional, daytime use
- **Best For**: Normal office environments

### Dark
- **Background**: Deep dark (#0F1419)
- **Primary**: Bright green (#3D7A4F)
- **Use Case**: Low-light environments
- **Best For**: Night work, reducing eye strain

### Forest
- **Background**: Light sage (#E8F2EC)
- **Primary**: Rich green (#237A47)
- **Use Case**: Nature-focused, immersive
- **Best For**: Environmental emphasis

### Ocean
- **Background**: Clean blue-gray (#F5F8FA)
- **Primary**: Professional blue (#2D65A0)
- **Use Case**: Corporate finance aesthetic
- **Best For**: Traditional banking feel

---

## 🔥 Advanced Customization

### Dynamic Theme Loading
```java
// Load themes from configuration file
ThemeManager manager = ThemeManager.getInstance();
String userTheme = loadFromConfig();  // e.g., "dark"
manager.setTheme(userTheme);
```

### Per-User Theme Preferences
```java
// Already implemented! Uses java.util.prefs.Preferences
// Automatically saves to:
// Windows: HKEY_CURRENT_USER\Software\JavaSoft\Prefs\Utils
// Mac: ~/Library/Preferences/com.apple.java.util.prefs.plist
// Linux: ~/.java/.userPrefs/Utils/prefs.xml
```

### Theme Change Listeners
```java
// Add to ThemeManager.java if needed
private List<Consumer<String>> listeners = new ArrayList<>();

public void addThemeChangeListener(Consumer<String> listener) {
    listeners.add(listener);
}

private void notifyListeners() {
    listeners.forEach(l -> l.accept(currentTheme));
}
```

---

## 🐞 Troubleshooting

### Theme not changing?
```java
// Verify Scene is initialized
ThemeManager.getInstance().getScene(); // Should not be null

// Force re-apply
ThemeManager.getInstance().applyCurrentTheme();
```

### Colors not showing?
- Theme CSS must override specific selectors
- Check browser dev tools CSS specificity rules apply to JavaFX too
- More specific selectors win: `.btn-primary` beats `.button`

### Theme resets on navigation?
- Make sure you're using `MainFX.setRoot()` for navigation
- Don't create new Scenes, reuse the existing one
- Theme is on Scene, not Root

---

## 📊 UI Placement Recommendations

### Sidebar (Recommended) ✅
- Always visible
- Non-intrusive
- Natural location for settings

### Top Bar
```xml
<HBox styleClass="topbar" alignment="CENTER_RIGHT" spacing="12">
    <Label text="Theme:"/>
    <ComboBox fx:id="themeSelector" prefWidth="120"/>
</HBox>
```

### Settings Menu/Dialog
- Less accessible but cleaner
- Good for minimalist designs

### User Profile Dropdown
- Modern app pattern
- Requires more implementation

---

## ✨ Next Steps

1. **Add theme selector to all your views**:
   - expertProjet.fxml
   - gestionCarbone.fxml
   - main.fxml
   - test.fxml

2. **Test theme persistence**:
   - Switch theme
   - Navigate between views
   - Restart app
   - Theme should be remembered!

3. **Customize themes**:
   - Adjust colors to match your brand
   - Create company-specific themes
   - Add seasonal themes (winter, summer, etc.)

4.**Consider adding**:
   - Icon next to theme name (🌞🌙🌲🌊)
   - Animated theme transitions
   - High contrast theme for accessibility

---

## 📝 Quick Reference

### Change Theme Programmatically
```java
ThemeManager.getInstance().setTheme("dark");
```

### Get Current Theme
```java
String theme = ThemeManager.getInstance().getCurrentTheme();
```

### Check Available Themes
```java
List<String> themes = ThemeManager.getInstance().getAvailableThemes();
```

---

**Happy Theming! 🎨**

Built with ❤️ for GreenLedger Finance Platform
