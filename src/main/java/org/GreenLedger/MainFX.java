package org.GreenLedger;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.scene.image.Image;
import Utils.ThemeManager;
import Utils.NavigationContext;

import java.io.IOException;

public class MainFX extends Application {

    private static Scene scene;

    @Override
    public void start(Stage stage) throws IOException {

        // Charger l'interface FXML disponible
        Parent root = loadFXML("expertProjet");

        scene = new Scene(root, 1100, 720);
        
        // Initialize ThemeManager with the scene (applies saved theme)
        ThemeManager.getInstance().initialize(scene);

        java.net.URL iconUrl = MainFX.class.getResource("/images/bg.png");
        if (iconUrl != null) {
            stage.getIcons().add(new Image(iconUrl.toExternalForm()));
        }

        stage.setTitle("Green Ledger");
        stage.setScene(scene);
        stage.setMinWidth(1000);
        stage.setMinHeight(600);
        stage.show();
    }

    public static void setRoot(String fxml) throws IOException {
        NavigationContext.getInstance().navigateTo(fxml);
        scene.setRoot(loadFXML(fxml));
        // Theme persists because it's applied to Scene, not root!
    }
    
    public static Scene getScene() {
        return scene;
    }

    private static Parent loadFXML(String fxml) throws IOException {
        java.net.URL resource = MainFX.class.getResource("/" + fxml + ".fxml");
        if (resource == null) {
            throw new IOException("FXML introuvable: /" + fxml + ".fxml");
        }
        FXMLLoader fxmlLoader = new FXMLLoader(resource);
        return fxmlLoader.load();
    }

    public static void main(String[] args) {
        launch();
    }
}
