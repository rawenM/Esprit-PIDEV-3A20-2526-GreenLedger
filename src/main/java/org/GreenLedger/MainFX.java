package org.GreenLedger;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class MainFX extends Application {

    private static Scene scene;

    @Override
    public void start(Stage stage) throws IOException {

        // Charger l'interface FXML disponible
        Parent root = loadFXML("expertProjet");

        scene = new Scene(root, 1200, 800);
        stage.setTitle("Carbon Expert Audit - Green Financing Platform");
        stage.setScene(scene);
        stage.show();
    }

    public static void setRoot(String fxml) throws IOException {
        scene.setRoot(loadFXML(fxml));
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
