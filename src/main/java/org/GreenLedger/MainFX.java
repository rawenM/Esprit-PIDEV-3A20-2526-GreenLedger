package org.GreenLedger;

import DataBase.MyConnection;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.scene.image.Image;
import Utils.ThemeManager;
import Utils.NavigationContext;
import Utils.ResetHttpServer;

import java.io.IOException;
import java.net.URL;

public class MainFX extends Application {

    private static Scene scene;
    private ResetHttpServer resetServer;

    @Override
    public void start(Stage primaryStage) throws IOException {

        // Test de la connexion a la base de donnees
        MyConnection db = MyConnection.getInstance();
        if (!db.testConnection()) {
            System.err.println("[CLEAN] ERREUR: Impossible de se connecter a la base de donnees !");
            System.err.println("Verifiez que le serveur de base de donnees est demarr et que l'URL/les identifiants sont corrects.");
            return;
        }

        // Charger la page de connexion
        URL fxmlUrl = getClass().getResource("/fxml/login.fxml");
        if (fxmlUrl == null) {
            System.err.println("[CLEAN] Fichier FXML '/fxml/login.fxml' introuvable sur le classpath");
            return;
        }
        Parent root = FXMLLoader.load(fxmlUrl);

        scene = new Scene(root);
        // Charger la feuille de style de maniere robuste
        URL cssUrl = getClass().getResource("/css/style.css");
        if (cssUrl != null) {
            scene.getStylesheets().add(cssUrl.toExternalForm());
        } else {
            System.err.println("[CLEAN] Fichier CSS '/css/style.css' introuvable sur le classpath");
        }
        URL appCssUrl = getClass().getResource("/app.css");
        if (appCssUrl != null) {
            scene.getStylesheets().add(appCssUrl.toExternalForm());
        } else {
            System.err.println("[CLEAN] Fichier CSS '/app.css' introuvable sur le classpath");
        }

        // Initialize ThemeManager with the scene (applies saved theme)
        ThemeManager.getInstance().initialize(scene);

        java.net.URL iconUrl = MainFX.class.getResource("/images/bg.png");
        if (iconUrl != null) {
            primaryStage.getIcons().add(new Image(iconUrl.toExternalForm()));
        }

        primaryStage.setTitle("Green Ledger - Connexion");
        primaryStage.setScene(scene);
        primaryStage.setMinWidth(850);
        primaryStage.setMinHeight(750);
        primaryStage.setWidth(850);
        primaryStage.setHeight(750);
        primaryStage.show();

        try {
            // Demarrer le serveur HTTP local pour la reinitialisation si necessaire
            try {
                int port = Integer.parseInt(System.getenv().getOrDefault("RESET_HTTP_PORT", "8080"));
                resetServer = new ResetHttpServer(port);
                resetServer.start();
            } catch (Exception e) {
                System.err.println("[CLEAN] Impossible de demarrer ResetHttpServer: " + e.getMessage());
            }

            System.out.println("[CLEAN] Application demarree avec succes");

        } catch (Exception e) {
            System.err.println("[CLEAN] Erreur lors du demarrage de l'application");
            e.printStackTrace();
        }
    }

    public static void setRoot(String fxml) throws IOException {
        NavigationContext.getInstance().navigateTo(fxml);
        scene.setRoot(loadFXML(fxml));
        ensureBaseStyles(scene);
    }

    private static void ensureBaseStyles(Scene scene) {
        if (scene == null) {
            return;
        }
        if (scene.getStylesheets().stream().noneMatch(s -> s.endsWith("/css/style.css"))) {
            URL cssUrl = MainFX.class.getResource("/css/style.css");
            if (cssUrl != null) {
                scene.getStylesheets().add(cssUrl.toExternalForm());
            }
        }
        if (scene.getStylesheets().stream().noneMatch(s -> s.endsWith("/app.css"))) {
            URL appCssUrl = MainFX.class.getResource("/app.css");
            if (appCssUrl != null) {
                scene.getStylesheets().add(appCssUrl.toExternalForm());
            }
        }
        ThemeManager.getInstance().initialize(scene);
    }

    public void stop() {
        // Fermer la connexion a la base de donnees lors de la fermeture
        MyConnection.getInstance().closeConnection();
        // Arreter le serveur reset si demarr
        try {
            if (resetServer != null) resetServer.stop();
        } catch (Exception ignored) {}
        System.out.println("[CLEAN] Application fermee");
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