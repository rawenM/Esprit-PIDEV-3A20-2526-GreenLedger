package Utils;

import javafx.scene.Scene;
import javafx.scene.Parent;
import java.util.Arrays;
import java.util.List;
import java.util.prefs.Preferences;

/**
 * Centralized theme management system.
 * Applies themes to the Scene level so they persist across FXML view changes.
 */
public class ThemeManager {
    private static ThemeManager instance;
    private String currentTheme = "light";
    private Scene scene;
    private final Preferences prefs;
    
    // Available themes
    public static final String THEME_GREENLEDGER = "greenledger"; // GreenLedger Front Office (default)
    public static final String THEME_LIGHT = "light";
    public static final String THEME_DARK = "dark";
    public static final String THEME_FOREST = "forest";
    public static final String THEME_OCEAN = "ocean";
    public static final String THEME_CLASSIC = "classic";
    public static final String THEME_PURPLE = "purple";
    
    private ThemeManager() {
        prefs = Preferences.userNodeForPackage(ThemeManager.class);
        currentTheme = prefs.get("theme", THEME_GREENLEDGER);
    }
    
    public static ThemeManager getInstance() {
        if (instance == null) {
            instance = new ThemeManager();
        }
        return instance;
    }
    
    /**
     * Initialize ThemeManager with the application Scene.
     * Call this once when the application starts.
     */
    public void initialize(Scene scene) {
        this.scene = scene;
        applyCurrentTheme();
    }
    
    /**
     * Apply the currently selected theme to the scene.
     */
    public void applyCurrentTheme() {
        if (scene != null) {
            applyTheme(currentTheme);
        }
    }
    
    /**
     * Change to a different theme and apply it immediately.
     */
    public void setTheme(String theme) {
        if (!isValidTheme(theme)) {
            System.err.println("Invalid theme: " + theme);
            return;
        }
        
        currentTheme = theme;
        prefs.put("theme", theme);
        applyCurrentTheme();
    }
    
    /**
     * Apply theme stylesheets to the scene.
     * This persists across FXML view changes since Scene stays the same.
     */
    private void applyTheme(String theme) {
        if (scene == null) {
            System.err.println("Scene not initialized. Call initialize() first.");
            return;
        }
        
        scene.getStylesheets().clear();
        
        try {
            // Load app-base.css for structure
            java.net.URL baseUrl = getClass().getResource("/themes/app-base.css");
            if (baseUrl != null) {
                scene.getStylesheets().add(baseUrl.toExternalForm());
                System.out.println("✓ Loaded base styles");
            } else {
                // Fallback to app.css if app-base.css doesn't exist
                java.net.URL appUrl = getClass().getResource("/app.css");
                if (appUrl != null) {
                    scene.getStylesheets().add(appUrl.toExternalForm());
                    System.out.println("✓ Loaded app.css as base");
                }
            }
            
            // Load theme-specific colors (overrides base)
            java.net.URL themeUrl = getClass().getResource("/themes/theme-" + theme + ".css");
            if (themeUrl != null) {
                scene.getStylesheets().add(themeUrl.toExternalForm());
                System.out.println("✓ Applied theme: " + theme);
            } else {
                System.err.println("⚠ Theme file not found: /themes/theme-" + theme + ".css");
                System.out.println("  Available themes should be in src/main/resources/themes/");
            }
        } catch (Exception e) {
            System.err.println("✗ Error loading theme: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Get the currently active theme.
     */
    public String getCurrentTheme() {
        return currentTheme;
    }
    
    /**
     * Get list of all available themes.
     */
    public List<String> getAvailableThemes() {
        return Arrays.asList(THEME_GREENLEDGER, THEME_LIGHT, THEME_DARK, THEME_FOREST, THEME_OCEAN, THEME_CLASSIC, THEME_PURPLE);
    }
    
    /**
     * Get display-friendly theme names.
     */
    public List<String> getThemeDisplayNames() {
        return Arrays.asList(
            "GreenLedger Front Office",
            "Light Theme",
            "Dark Theme",
            "Earth Theme",
            "Cool Theme",
            "GreenLedger Classic",
            "Purple Theme"
        );
    }
    
    /**
     * Convert display name to theme ID.
     */
    public String themeFromDisplayName(String displayName) {
        if (displayName == null) {
            return THEME_GREENLEDGER;
        }
        switch (displayName.toLowerCase()) {
            case "greenledger front office": return THEME_GREENLEDGER;
            case "light theme": return THEME_LIGHT;
            case "dark theme": return THEME_DARK;
            case "earth theme": return THEME_FOREST;
            case "cool theme": return THEME_OCEAN;
            case "greenledger classic": return THEME_CLASSIC;
            case "purple theme": return THEME_PURPLE;
            default: return THEME_GREENLEDGER;
        }
    }
    
    /**
     * Convert theme ID to display name.
     */
    public String getDisplayName(String theme) {
        if (THEME_GREENLEDGER.equals(theme)) return "GreenLedger Front Office";
        if (THEME_LIGHT.equals(theme))       return "Light Theme";
        if (THEME_DARK.equals(theme))        return "Dark Theme";
        if (THEME_FOREST.equals(theme))      return "Earth Theme";
        if (THEME_OCEAN.equals(theme))       return "Cool Theme";
        if (THEME_CLASSIC.equals(theme))     return "GreenLedger Classic";
        if (THEME_PURPLE.equals(theme))      return "Purple Theme";
        return "GreenLedger Front Office";
    }
    
    private boolean isValidTheme(String theme) {
        return getAvailableThemes().contains(theme);
    }
    
    /**
     * Get the current Scene.
     */
    public Scene getScene() {
        return scene;
    }
}
