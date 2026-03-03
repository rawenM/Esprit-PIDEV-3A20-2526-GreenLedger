package Controllers;

import javafx.application.Platform;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import Models.MarketplaceEscrow;
import Services.MarketplaceOrderService;
import Utils.SessionManager;
import org.GreenLedger.MainFX;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Admin controller for managing marketplace escrows
 * Displays held escrows and allows verification/release/refund
 */
public class EscrowManagementController {

    @FXML private TableView<MarketplaceEscrow> escrowsTable;
    @FXML private TableColumn<MarketplaceEscrow, Integer> escrowIdColumn;
    @FXML private TableColumn<MarketplaceEscrow, Integer> orderIdColumn;
    @FXML private TableColumn<MarketplaceEscrow, Long> buyerIdColumn;
    @FXML private TableColumn<MarketplaceEscrow, Long> sellerIdColumn;
    @FXML private TableColumn<MarketplaceEscrow, Double> amountColumn;
    @FXML private TableColumn<MarketplaceEscrow, LocalDateTime> createdAtColumn;
    @FXML private TableColumn<MarketplaceEscrow, Long> hoursHeldColumn;
    @FXML private TableColumn<MarketplaceEscrow, String> statusColumn;
    @FXML private TableColumn<MarketplaceEscrow, Void> actionsColumn;

    @FXML private Label lblHeldCount;
    @FXML private Label lblTotalAmount;
    @FXML private Label lblReadyToRelease;
    @FXML private Label lblEmptyMessage;
    @FXML private Label lblProfileName;
    @FXML private Label lblProfileType;

    private MarketplaceOrderService orderService;
    private DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @FXML
    public void initialize() {
        System.out.println("[ESCROW DEBUG] Controller initialize() called at " + System.currentTimeMillis());
        System.out.println("[ESCROW DEBUG] escrowsTable = " + escrowsTable);
        System.out.println("[ESCROW DEBUG] lblHeldCount = " + lblHeldCount);
        System.out.println("[ESCROW DEBUG] lblTotalAmount = " + lblTotalAmount);
        System.out.println("[ESCROW DEBUG] lblReadyToRelease = " + lblReadyToRelease);
        System.out.println("[ESCROW DEBUG] All labels initialized: " + (escrowsTable != null && lblHeldCount != null && lblTotalAmount != null && lblReadyToRelease != null));
        
        orderService = MarketplaceOrderService.getInstance();
        System.out.println("[ESCROW DEBUG] orderService initialized");
        
        // DEBUG: Print all escrows in database
        System.out.println("[ESCROW DEBUG] ===== DUMPING ALL ESCROWS FROM DATABASE =====");
        orderService.debugPrintAllEscrows();
        System.out.println("[ESCROW DEBUG] ===== END DATABASE DUMP =====");
        
        // DEBUG: Create a test escrow if none exist
        System.out.println("[ESCROW DEBUG] Creating test escrow for demo purposes...");
        orderService.debugCreateTestEscrow();
        
        setupTableColumns();
        loadHeldEscrows();
        
        // Refresh every 30 seconds
        Thread refreshThread = new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(30000); // 30 seconds
                    Platform.runLater(this::loadHeldEscrows);
                } catch (InterruptedException e) {
                    break;
                }
            }
        });
        refreshThread.setDaemon(true);
        refreshThread.start();

        // Set profile info if labels exist
        try {
            var user = SessionManager.getInstance().getCurrentUser();
            if (user != null && lblProfileName != null && lblProfileType != null) {
                lblProfileName.setText(user.getNom() + " " + user.getPrenom());
                lblProfileType.setText(user.getTypeUtilisateur().toString());
            }
        } catch (Exception e) {
            System.err.println("[ESCROW] Could not set profile info: " + e.getMessage());
        }
    }

    /**
     * Setup table column bindings
     */
    private void setupTableColumns() {
        escrowIdColumn.setCellValueFactory(cellData -> 
            new SimpleIntegerProperty(cellData.getValue().getId()).asObject());
        orderIdColumn.setCellValueFactory(cellData -> 
            new SimpleObjectProperty<>(cellData.getValue().getOrderId()));
        buyerIdColumn.setCellValueFactory(cellData -> 
            new SimpleLongProperty(cellData.getValue().getBuyerId()).asObject());
        sellerIdColumn.setCellValueFactory(cellData -> 
            new SimpleLongProperty(cellData.getValue().getSellerId()).asObject());
        amountColumn.setCellValueFactory(cellData -> 
            new SimpleDoubleProperty(cellData.getValue().getAmountUsd()).asObject());
        amountColumn.setCellFactory(column -> new TableCell<MarketplaceEscrow, Double>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? "" : String.format("$%.2f", item));
            }
        });

        createdAtColumn.setCellValueFactory(cellData -> {
            java.sql.Timestamp ts = cellData.getValue().getCreatedAt();
            return new SimpleObjectProperty<>(ts != null ? ts.toLocalDateTime() : null);
        });
        createdAtColumn.setCellFactory(column -> new TableCell<MarketplaceEscrow, LocalDateTime>() {
            @Override
            protected void updateItem(LocalDateTime item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? "" : item.format(dateFormatter));
            }
        });

        hoursHeldColumn.setCellValueFactory(cellData -> 
            new SimpleObjectProperty<>(cellData.getValue().getHoursHeld()));
        hoursHeldColumn.setCellFactory(column -> new TableCell<MarketplaceEscrow, Long>() {
            @Override
            protected void updateItem(Long item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setText("");
                } else {
                    String color = item >= 24 ? "#2ecc71" : (item >= 20 ? "#f39c12" : "#e74c3c");
                    setText(item + "h");
                    setStyle("-fx-text-fill: " + color + ";");
                }
            }
        });

        statusColumn.setCellValueFactory(cellData -> 
            new SimpleObjectProperty<>(cellData.getValue().getStatus()));
        statusColumn.setCellFactory(column -> new TableCell<MarketplaceEscrow, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setText("");
                    setStyle("");
                } else {
                    setText(item);
                    if ("HELD".equalsIgnoreCase(item)) {
                        setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
                    }
                }
            }
        });

        // Actions column
        actionsColumn.setCellFactory(column -> new TableCell<MarketplaceEscrow, Void>() {
            private final Button releaseBtn = new Button("Libérer");
            private final Button refundBtn = new Button("Rembourser");

            {
                releaseBtn.setStyle("-fx-padding: 4 8; -fx-font-size: 10;");
                refundBtn.setStyle("-fx-padding: 4 8; -fx-font-size: 10; -fx-text-fill: #fff; -fx-background-color: #e74c3c;");

                releaseBtn.setOnAction(event -> handleReleaseEscrow(getTableView().getItems().get(getIndex())));
                refundBtn.setOnAction(event -> handleRefundEscrow(getTableView().getItems().get(getIndex())));
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    HBox box = new HBox(6, releaseBtn, refundBtn);
                    setGraphic(box);
                }
            }
        });
    }

    /**
     * Load all held escrows from database
     */
    private void loadHeldEscrows() {
        try {
            System.out.println("[ESCROW DATA DEBUG] Starting loadHeldEscrows()");
            System.out.println("[ESCROW DATA DEBUG] orderService = " + orderService);
            
            List<MarketplaceEscrow> escrows = orderService.getHeldEscrows();
            System.out.println("[ESCROW DATA DEBUG] Retrieved " + (escrows == null ? "NULL" : escrows.size()) + " escrows from service");
            
            if (escrows == null) {
                escrows = new ArrayList<>();
                System.out.println("[ESCROW DATA DEBUG] Escrows was null, using empty list");
            }
            
            for (MarketplaceEscrow e : escrows) {
                System.out.println("[ESCROW DATA DEBUG] Escrow: ID=" + e.getId() + ", OrderID=" + e.getOrderId() + ", Amount=" + e.getAmountUsd() + ", Status=" + e.getStatus());
            }
            
            ObservableList<MarketplaceEscrow> data = FXCollections.observableArrayList(escrows);
            System.out.println("[ESCROW DATA DEBUG] Created ObservableList with " + data.size() + " items");
            
            escrowsTable.setItems(data);
            System.out.println("[ESCROW DATA DEBUG] Set items to table");

            // Update stats
            int count = escrows.size();
            double total = escrows.stream().mapToDouble(MarketplaceEscrow::getAmountUsd).sum();
            int readyToRelease = (int) escrows.stream()
                .filter(e -> e.getHoursHeld() >= 24)
                .count();

            System.out.println("[ESCROW DATA DEBUG] Stats: count=" + count + ", total=" + total + ", readyToRelease=" + readyToRelease);

            lblHeldCount.setText(String.valueOf(count));
            lblTotalAmount.setText(String.format("%.2f USD", total));
            lblReadyToRelease.setText(String.valueOf(readyToRelease));

            lblEmptyMessage.setVisible(count == 0);
            escrowsTable.setVisible(count > 0);

            System.out.println("[ESCROW DATA DEBUG] UI updated successfully. Table visible=" + escrowsTable.isVisible());
        } catch (Exception e) {
            System.err.println("[ESCROW DATA DEBUG] Exception in loadHeldEscrows: " + e.getMessage());
            e.printStackTrace();
            showError("Erreur", "Impossible de charger les escrows: " + e.getMessage());
        }
    }

    /**
     * Handle release escrow action
     */
    @FXML
    private void handleReleaseEscrow(MarketplaceEscrow escrow) {
        if (escrow == null) return;

        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Libérer l'Escrow");
        confirmAlert.setHeaderText("Confirmer la Libération");
        confirmAlert.setContentText(String.format(
            "Êtes-vous sûr de libérer cet escrow?\n\n" +
            "Escrow ID: %d\nCommande: %d\nMontant: $%.2f\n\n" +
            "Cela transférera $%.2f au vendeur (3%% de frais = $%.2f)",
            escrow.getId(),
            escrow.getOrderId(),
            escrow.getAmountUsd(),
            escrow.getAmountUsd() * 0.97,
            escrow.getAmountUsd() * 0.03
        ));

        Optional<ButtonType> result = confirmAlert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                boolean success = orderService.verifyAndReleaseEscrow(escrow.getId());
                if (success) {
                    showInfo("Succès", "Escrow libéré avec succès à l'ID " + escrow.getId());
                    loadHeldEscrows();
                } else {
                    showError("Erreur", "Impossible de libérer l'escrow");
                }
            } catch (Exception e) {
                showError("Erreur", "Erreur lors de la libération: " + e.getMessage());
            }
        }
    }

    /**
     * Handle refund escrow action
     */
    @FXML
    private void handleRefundEscrow(MarketplaceEscrow escrow) {
        if (escrow == null) return;

        Alert confirmAlert = new Alert(Alert.AlertType.WARNING);
        confirmAlert.setTitle("Rembourser l'Escrow");
        confirmAlert.setHeaderText("Confirmer le Remboursement");
        confirmAlert.setContentText(String.format(
            "Êtes-vous sûr de rembourser cet escrow à l'acheteur?\n\n" +
            "Escrow ID: %d\nCommande: %d\nMontant: $%.2f\n\n" +
            "Cela remboursera le montant complet à l'acheteur.",
            escrow.getId(),
            escrow.getOrderId(),
            escrow.getAmountUsd()
        ));

        Optional<ButtonType> result = confirmAlert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                boolean success = orderService.refundEscrowToBuyer(escrow.getId());
                if (success) {
                    showInfo("Succès", "Escrow remboursé avec succès à l'acheteur");
                    loadHeldEscrows();
                } else {
                    showError("Erreur", "Impossible de rembourser l'escrow");
                }
            } catch (Exception e) {
                showError("Erreur", "Erreur lors du remboursement: " + e.getMessage());
            }
        }
    }

    /**
     * Navigation methods
     */
    @FXML
    private void handleRefresh() {
        loadHeldEscrows();
        showInfo("Actualisation", "Table des escrows actualisée");
    }

    @FXML
    private void handleBack() {
        try {
            MainFX.setRoot("fxml/admin_users");
        } catch (Exception e) {
            showError("Erreur", "Impossible de naviguer: " + e.getMessage());
        }
    }

    @FXML
    private void handleNavUsers() {
        try {
            MainFX.setRoot("fxml/admin_users");
        } catch (Exception e) {
            showError("Erreur", "Impossible de naviguer: " + e.getMessage());
        }
    }

    @FXML
    private void handleNavProjets() {
        showError("Non implémenté", "Page des projets en développement");
    }

    @FXML
    private void handleNavEvaluations() {
        showError("Non implémenté", "Page des évaluations en développement");
    }

    @FXML
    private void handleNavSettings() {
        showError("Non implémenté", "Page des paramètres en développement");
    }

    @FXML
    private void handleEditProfile() {
        showError("Non implémenté", "Modification de profil en développement");
    }

    /**
     * Helper methods
     */
    private void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
