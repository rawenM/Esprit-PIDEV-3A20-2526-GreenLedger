package Controllers;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * Standalone launcher for Comprehensive Test Panel
 * Run this class to open the test interface independently
 */
public class TestLauncher extends Application {

    @Override
    public void start(Stage primaryStage) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/ComprehensiveTest.fxml"));
        Scene scene = new Scene(loader.load());
        
        primaryStage.setTitle("🧪 Comprehensive System Test Panel");
        primaryStage.setScene(scene);
        primaryStage.setWidth(1400);
        primaryStage.setHeight(900);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
    
    /**
     * Static method to launch test panel from other parts of application
     */
    public static void launchTestPanel() {
        new Thread(() -> {
            Application.launch(TestLauncher.class);
        }).start();
    }
}
