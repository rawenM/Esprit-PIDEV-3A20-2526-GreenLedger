package Controllers;

import Utils.ThemeManager;
import Utils.NavigationContext;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import Models.User;
import Utils.SessionManager;

/**
 * Base controller with common functionality like theme switching.
 * All your controllers can extend this class to inherit theme switching capability.
 */
public abstract class BaseController {
    
    @FXML
    protected ComboBox<String> themeSelector;
    
    /**
     * Call this in your controller's initialize() method after super.initialize()
     */
    protected void initializeThemeSelector() {
        if (themeSelector != null) {
            // Populate with theme options
            themeSelector.setItems(FXCollections.observableArrayList(
                ThemeManager.getInstance().getThemeDisplayNames()
            ));
            
            // Set current theme as selected
            String currentTheme = ThemeManager.getInstance().getCurrentTheme();
            themeSelector.setValue(ThemeManager.getInstance().getDisplayName(currentTheme));
            
            // Listen for changes
            themeSelector.setOnAction(event -> onThemeChange());
        }
    }
    
    /**
     * Handle theme change from ComboBox
     */
    @FXML
    protected void onThemeChange() {
        if (themeSelector == null || themeSelector.getValue() == null) {
            return;
        }
        
        String selectedTheme = ThemeManager.getInstance()
            .themeFromDisplayName(themeSelector.getValue());
        
        ThemeManager.getInstance().setTheme(selectedTheme);
    }
    
    /**
     * Override in child controllers for initialization logic
     */
    @FXML
    public void initialize() {
        // Base initialization
        initializeThemeSelector();
    }

    /**
     * Navigate to the previous page using NavigationContext history.
     * Falls back to the role-appropriate shell if no history exists.
     */
    protected void navigateBack() {
        String prev = NavigationContext.getInstance().getPreviousPage();
        String curr = NavigationContext.getInstance().getCurrentPage();

        if (prev != null && !prev.isEmpty() && !prev.equals(curr)) {
            try {
                org.GreenLedger.MainFX.setRoot(prev);
                return;
            } catch (Exception ignored) {}
        }

        // Fallback: go to role shell
        User user = SessionManager.getInstance().getCurrentUser();
        String fallback = "fxml/login";
        if (user != null && user.getTypeUtilisateur() != null) {
            switch (user.getTypeUtilisateur()) {
                case ADMIN          -> fallback = "fxml/admin_shell";
                case EXPERT_CARBONE -> fallback = "fxml/expert_shell";
                case PORTEUR_PROJET -> fallback = "fxml/porteur_shell";
                case INVESTISSEUR   -> fallback = "fxml/investisseur_shell";
            }
        }
        try { org.GreenLedger.MainFX.setRoot(fallback); }
        catch (Exception e) { System.err.println("[Back] Navigation failed: " + e.getMessage()); }
    }

    /**
     * Helper method to populate profile labels from the current session user.
     */
    protected void applyProfile(Label nameLabel, Label typeLabel) {
        if (nameLabel == null || typeLabel == null) {
            return;
        }
        User user = SessionManager.getInstance().getCurrentUser();
        if (user == null) {
            return;
        }
        nameLabel.setText(user.getNomComplet());
        typeLabel.setText(user.getTypeUtilisateur().getLibelle());
    }
}
