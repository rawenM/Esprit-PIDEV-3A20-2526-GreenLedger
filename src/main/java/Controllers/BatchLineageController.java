package Controllers;

import Models.BatchEvent;
import Models.CarbonCreditBatch;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

/**
 * JavaFX Controller for Batch Lineage and Traceability UI.
 * Displays batch lineage, event timeline, and complete provenance.
 * 
 * @author GreenLedger Traceability Team
 */
public class BatchLineageController implements Initializable {

    @FXML private TextField batchIdField;
    @FXML private Label validationLabel;
    @FXML private VBox detailsCard;
    @FXML private Label serialNumberLabel;
    @FXML private Label batchTypeLabel;
    @FXML private Label totalAmountLabel;
    @FXML private Label remainingAmountLabel;
    @FXML private Label statusLabel;
    @FXML private Label verificationLabel;
    @FXML private Label eventCountLabel;
    @FXML private Label chainIntegrityLabel;
    @FXML private TreeView<String> lineageTreeView;
    @FXML private TableView<BatchEvent> eventsTable;
    @FXML private TableColumn<BatchEvent, Long> eventIdColumn;
    @FXML private TableColumn<BatchEvent, String> eventTypeColumn;
    @FXML private TableColumn<BatchEvent, String> eventActorColumn;
    @FXML private TableColumn<BatchEvent, String> eventHashColumn;
    @FXML private TableColumn<BatchEvent, String> eventTimestampColumn;
    @FXML private TextArea provenanceTextArea;
    @FXML private Button splitBatchButton;
    @FXML private Button viewParentButton;
    @FXML private Button viewChildrenButton;
    @FXML private Label statusMessageLabel;
    @FXML private Button refreshButton;
    @FXML private Button exportButton;

    private BatchTraceabilityController traceabilityController;
    private CarbonCreditBatch currentBatch;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        traceabilityController = new BatchTraceabilityController();
        setupEventTable();
        detailsCard.setVisible(false);
    }

    /**
     * Setup the events table columns.
     */
    private void setupEventTable() {
        eventIdColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        eventTypeColumn.setCellValueFactory(new PropertyValueFactory<>("eventType"));
        eventActorColumn.setCellValueFactory(new PropertyValueFactory<>("actor"));
        
        // Truncated hash display
        eventHashColumn.setCellValueFactory(cellData -> {
            String hash = cellData.getValue().getEventHash();
            return new javafx.beans.property.SimpleStringProperty(
                hash != null && hash.length() > 16 ? hash.substring(0, 16) + "..." : hash
            );
        });
        
        // Formatted timestamp
        eventTimestampColumn.setCellValueFactory(cellData -> {
            var created = cellData.getValue().getCreatedAt();
            return new javafx.beans.property.SimpleStringProperty(
                created != null ? created.format(DATE_FORMATTER) : "N/A"
            );
        });
    }

    /**
     * Set batch ID and load details (called from external controllers).
     */
    public void setBatchId(int batchId) {
        batchIdField.setText(String.valueOf(batchId));
    }

    /**
     * Load batch details (called from external controllers).
     */
    public void loadBatchDetails() {
        String batchIdText = batchIdField.getText().trim();
        if (!batchIdText.isEmpty()) {
            try {
                int batchId = Integer.parseInt(batchIdText);
                loadBatchDetails(batchId);
            } catch (NumberFormatException e) {
                showError("Invalid batch ID format");
            }
        }
    }

    /**
     * Handle search button click.
     */
    @FXML
    private void handleSearch() {
        String batchIdText = batchIdField.getText().trim();
        if (batchIdText.isEmpty()) {
            showError("Please enter a batch ID");
            return;
        }

        try {
            int batchId = Integer.parseInt(batchIdText);
            loadBatchDetails(batchId);
        } catch (NumberFormatException e) {
            showError("Invalid batch ID format");
        }
    }

    /**
     * Load and display batch details.
     */
    private void loadBatchDetails(int batchId) {
        try {
            // Get batch summary
            BatchTraceabilityController.BatchTraceabilitySummary summary = 
                traceabilityController.getBatchSummary(batchId);

            if (summary == null || summary.getBatch() == null) {
                showError("Batch not found");
                detailsCard.setVisible(false);
                return;
            }

            currentBatch = summary.getBatch();
            
            // Update details
            serialNumberLabel.setText(currentBatch.getSerialNumber() != null ? 
                currentBatch.getSerialNumber() : "N/A");
            batchTypeLabel.setText(currentBatch.getBatchTypeBadge());
            totalAmountLabel.setText(String.format("%.2f tCO2", 
                currentBatch.getTotalAmount() != null ? currentBatch.getTotalAmount().doubleValue() : 0));
            remainingAmountLabel.setText(String.format("%.2f tCO2", 
                currentBatch.getRemainingAmount() != null ? currentBatch.getRemainingAmount().doubleValue() : 0));
            statusLabel.setText(currentBatch.getStatus());
            verificationLabel.setText(currentBatch.getVerificationBadge());
            eventCountLabel.setText(String.valueOf(summary.getEventCount()));
            
            // Chain integrity
            boolean chainValid = summary.isChainValid();
            chainIntegrityLabel.setText(chainValid ? "✓ Valid" : "✗ Tampered");
            chainIntegrityLabel.setStyle(chainValid ? 
                "-fx-text-fill: green; -fx-font-weight: bold;" : 
                "-fx-text-fill: red; -fx-font-weight: bold;");
            
            validationLabel.setText(chainValid ? "✓ Chain Valid" : "✗ Chain Invalid");
            validationLabel.setStyle(chainValid ? 
                "-fx-text-fill: green; -fx-font-weight: bold;" : 
                "-fx-text-fill: red; -fx-font-weight: bold;");

            // Load tabs
            loadLineageTree(batchId);
            loadEvents(batchId);
            loadProvenance(batchId);

            // Enable/disable buttons
            viewParentButton.setDisable(currentBatch.getParentBatchId() == null);
            viewChildrenButton.setDisable(summary.getChildrenCount() == 0);
            splitBatchButton.setDisable(currentBatch.getRemainingAmount().doubleValue() <= 0);

            detailsCard.setVisible(true);
            statusMessageLabel.setText("Loaded batch " + batchId);

        } catch (Exception e) {
            showError("Error loading batch: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Load lineage tree view.
     */
    private void loadLineageTree(int batchId) {
        List<CarbonCreditBatch> lineage = traceabilityController.getBatchLineage(batchId);
        
        TreeItem<String> root = new TreeItem<>("Batch Lineage");
        root.setExpanded(true);

        for (CarbonCreditBatch batch : lineage) {
            String label = String.format("Batch #%d - %s - %.2f tCO2 (%s)",
                batch.getId(),
                batch.getSerialNumber(),
                batch.getTotalAmount() != null ? batch.getTotalAmount().doubleValue() : 0,
                batch.getBatchTypeBadge());
            
            TreeItem<String> item = new TreeItem<>(label);
            item.setExpanded(true);
            
            if (batch.getId() == batchId) {
                root.getChildren().add(item);
            } else if (batch.getParentBatchId() != null) {
                // Find parent and add as child
                findAndAddChild(root, batch.getParentBatchId(), item);
            } else {
                root.getChildren().add(item);
            }
        }

        lineageTreeView.setRoot(root);
    }

    /**
     * Helper to build tree structure.
     */
    private boolean findAndAddChild(TreeItem<String> node, int parentId, TreeItem<String> child) {
        String nodeText = node.getValue();
        if (nodeText.contains("Batch #" + parentId)) {
            node.getChildren().add(child);
            return true;
        }

        for (TreeItem<String> childNode : node.getChildren()) {
            if (findAndAddChild(childNode, parentId, child)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Load events table.
     */
    private void loadEvents(int batchId) {
        List<BatchEvent> events = traceabilityController.getBatchEvents(batchId);
        eventsTable.getItems().clear();
        eventsTable.getItems().addAll(events);
    }

    /**
     * Load provenance text.
     */
    private void loadProvenance(int batchId) {
        String provenance = traceabilityController.getBatchProvenance(batchId);
        // Format JSON for better readability
        try {
            com.google.gson.Gson gson = new com.google.gson.GsonBuilder().setPrettyPrinting().create();
            Object json = gson.fromJson(provenance, Object.class);
            provenanceTextArea.setText(gson.toJson(json));
        } catch (Exception e) {
            provenanceTextArea.setText(provenance);
        }
    }

    /**
     * Handle refresh button.
     */
    @FXML
    private void handleRefresh() {
        if (currentBatch != null) {
            loadBatchDetails(currentBatch.getId());
        }
    }

    /**
     * Handle export to blockchain format.
     */
    @FXML
    private void handleExport() {
        if (currentBatch == null) {
            showError("No batch loaded");
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Export Blockchain Data");
        fileChooser.setInitialFileName("batch_" + currentBatch.getId() + "_blockchain.json");
        fileChooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("JSON Files", "*.json")
        );

        Stage stage = (Stage) exportButton.getScene().getWindow();
        File file = fileChooser.showSaveDialog(stage);

        if (file != null) {
            try {
                String blockchainData = traceabilityController.exportToBlockchainFormat(currentBatch.getId());
                
                // Pretty print JSON
                com.google.gson.Gson gson = new com.google.gson.GsonBuilder().setPrettyPrinting().create();
                Object json = gson.fromJson(blockchainData, Object.class);
                String formatted = gson.toJson(json);

                try (FileWriter writer = new FileWriter(file)) {
                    writer.write(formatted);
                }

                statusMessageLabel.setText("Exported to " + file.getName());
                showInfo("Export successful!");

            } catch (IOException e) {
                showError("Export failed: " + e.getMessage());
            }
        }
    }

    /**
     * Handle split batch button.
     */
    @FXML
    private void handleSplitBatch() {
        if (currentBatch == null) {
            showError("No batch loaded");
            return;
        }

        // Show dialog to get split amount and target wallet
        TextInputDialog amountDialog = new TextInputDialog();
        amountDialog.setTitle("Split Batch");
        amountDialog.setHeaderText("Split Batch #" + currentBatch.getId());
        amountDialog.setContentText("Amount to split (tCO2):");

        Optional<String> amountResult = amountDialog.showAndWait();
        if (amountResult.isEmpty()) return;

        try {
            double amountToSplit = Double.parseDouble(amountResult.get());
            
            if (amountToSplit <= 0 || amountToSplit > currentBatch.getRemainingAmount().doubleValue()) {
                showError("Invalid amount. Must be between 0 and " + 
                    currentBatch.getRemainingAmount().doubleValue());
                return;
            }

            TextInputDialog walletDialog = new TextInputDialog();
            walletDialog.setTitle("Split Batch");
            walletDialog.setHeaderText("Target Wallet");
            walletDialog.setContentText("Destination Wallet ID:");

            Optional<String> walletResult = walletDialog.showAndWait();
            if (walletResult.isEmpty()) return;

            int targetWalletId = Integer.parseInt(walletResult.get());

            // Perform split
            int childBatchId = traceabilityController.splitBatch(
                currentBatch.getId(), 
                amountToSplit, 
                targetWalletId, 
                "USER_UI"
            );

            if (childBatchId > 0) {
                showInfo("Batch split successful! New batch ID: " + childBatchId);
                loadBatchDetails(currentBatch.getId()); // Refresh
            } else {
                showError("Batch split failed");
            }

        } catch (NumberFormatException e) {
            showError("Invalid number format");
        }
    }

    /**
     * Handle view parent button.
     */
    @FXML
    private void handleViewParent() {
        if (currentBatch != null && currentBatch.getParentBatchId() != null) {
            batchIdField.setText(String.valueOf(currentBatch.getParentBatchId()));
            handleSearch();
        }
    }

    /**
     * Handle view children button.
     */
    @FXML
    private void handleViewChildren() {
        if (currentBatch != null) {
            List<CarbonCreditBatch> children = traceabilityController.getChildBatches(currentBatch.getId());
            if (!children.isEmpty()) {
                // Show first child
                batchIdField.setText(String.valueOf(children.get(0).getId()));
                handleSearch();
            }
        }
    }

    /**
     * Show error message.
     */
    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
        statusMessageLabel.setText("Error: " + message);
    }

    /**
     * Show info message.
     */
    private void showInfo(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Success");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
