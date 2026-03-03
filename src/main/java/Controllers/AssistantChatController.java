package Controllers;

import Services.OpenAiAssistantService;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

public class AssistantChatController {

    @FXML private TextArea taChat;
    @FXML private TextField tfInput;
    @FXML private Button btnSend;
    @FXML private Label lblStatus;

    private final OpenAiAssistantService ai = new OpenAiAssistantService();

    @FXML
    public void initialize() {
        // Entrée = envoyer
        if (tfInput != null) {
            tfInput.setOnAction(e -> onSend());
        }

        if (!Utils.ConversationCache.isEmpty()) {
            taChat.setText(Utils.ConversationCache.get());
        } else {
            append("Assistant", "Salut 👋 Pose ta question sur GreenLedger.");
        }
        setStatus("");
    }

    @FXML
    private void onClear() {
        taChat.clear();
        Utils.ConversationCache.clear();
        setStatus("");
    }

    @FXML
    private void onSend() {
        if (tfInput == null || taChat == null) return;

        String raw = tfInput.getText();
        if (raw == null) raw = "";
        raw = raw.trim();

        if (raw.isEmpty()) {
            setStatus("Écris une question d’abord.");
            return;
        }


        final String question = raw;

        tfInput.clear();
        append("Vous", question);

        setBusy(true);
        setStatus("Réflexion…");

        Task<String> task = new Task<>() {
            @Override
            protected String call() {
                // Ton service : ask(String)
                return ai.ask(question);
            }
        };

        task.setOnSucceeded(e -> {
            String answer = task.getValue();
            if (answer == null || answer.trim().isEmpty()) {
                answer = "Je n’ai pas pu répondre. Vérifie la clé OpenAI et la connexion Internet.";
            }
            append("Assistant", answer);
            setBusy(false);
            setStatus("");
        });

        task.setOnFailed(e -> {
            Throwable ex = task.getException();
            append("Erreur", ex == null ? "Erreur inconnue" : ex.getMessage());
            setBusy(false);
            setStatus("Erreur pendant l’appel API.");
        });

        Thread t = new Thread(task);
        t.setDaemon(true);
        t.start();
    }

    private void setBusy(boolean busy) {
        Platform.runLater(() -> {
            if (btnSend != null) btnSend.setDisable(busy);
            if (tfInput != null) tfInput.setDisable(busy);
        });
    }

    private void setStatus(String s) {
        Platform.runLater(() -> {
            if (lblStatus != null) lblStatus.setText(s == null ? "" : s);
        });
    }

    private void append(String who, String msg) {
        String text = who + " :\n" + msg + "\n\n";

        Platform.runLater(() -> {
            taChat.appendText(text);
        });

        Utils.ConversationCache.append(text);
    }
}