package Controllers;

import Utils.ThemeManager;
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
     * Helper method to populate profile labels from the current session user.
     * Child controllers can use this to show profile info at the top.
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
