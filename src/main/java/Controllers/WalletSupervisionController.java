package Controllers;

import Services.WalletSupervisionService;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.sql.Timestamp;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * Wallet Supervision Controller for Admin Dashboard
 * Monitors wallet health, deficits, and financial risks
 * 
 * Features:
 * - Overview metrics (total wallets, negative wallets, deficits)
 * - Top 25 negative wallets table
 * - Priority owners with multiple negative wallets
 * - At-risk wallets monitoring
 * - Health score calculation
 */
public class WalletSupervisionController {

    // Overview metrics
    @FXML private Label totalWalletsLabel;
    @FXML private Label negativeWalletsLabel;
    @FXML private Label atRiskWalletsLabel;
    @FXML private Label cumulativeDeficitLabel;
    @FXML private Label avgBalanceLabel;
    @FXML private Label totalAvailableLabel;

    // Negative wallets table
    @FXML private TableView<NegativeWalletData> negativeWalletsTable;
    @FXML private TableColumn<NegativeWalletData, Integer> walletIdColumn;
    @FXML private TableColumn<NegativeWalletData, String> walletNumberColumn;
    @FXML private TableColumn<NegativeWalletData, String> walletNameColumn;
    @FXML private TableColumn<NegativeWalletData, String> ownerTypeColumn;
    @FXML private TableColumn<NegativeWalletData, Integer> ownerIdColumn;
    @FXML private TableColumn<NegativeWalletData, Double> deficitColumn;
    @FXML private TableColumn<NegativeWalletData, Double> retiredColumn;
    @FXML private TableColumn<NegativeWalletData, String> priorityColumn;
    @FXML private TableColumn<NegativeWalletData, String> createdAtColumn;

    // Priority owners table
    @FXML private TableView<PriorityOwnerData> priorityOwnersTable;
    @FXML private TableColumn<PriorityOwnerData, String> ownerTypeCol;
    @FXML private TableColumn<PriorityOwnerData, Integer> ownerIdCol;
    @FXML private TableColumn<PriorityOwnerData, Integer> negativeCountCol;
    @FXML private TableColumn<PriorityOwnerData, Double> totalDeficitCol;
    @FXML private TableColumn<PriorityOwnerData, String> riskLevelCol;

    // At-risk wallets table
    @FXML private TableView<AtRiskWalletData> atRiskWalletsTable;
    @FXML private TableColumn<AtRiskWalletData, Integer> riskWalletIdColumn;
    @FXML private TableColumn<AtRiskWalletData, String> riskWalletNumberColumn;
    @FXML private TableColumn<AtRiskWalletData, String> riskWalletNameColumn;
    @FXML private TableColumn<AtRiskWalletData, Double> riskAvailableColumn;
    @FXML private TableColumn<AtRiskWalletData, String> riskWarningColumn;

    private WalletSupervisionService supervisionService;
    private ObservableList<NegativeWalletData> negativeWalletsList = FXCollections.observableArrayList();
    private ObservableList<PriorityOwnerData> priorityOwnersList = FXCollections.observableArrayList();
    private ObservableList<AtRiskWalletData> atRiskWalletsList = FXCollections.observableArrayList();

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @FXML
    public void initialize() {
        supervisionService = new WalletSupervisionService();
        
        setupNegativeWalletsTable();
        setupPriorityOwnersTable();
        setupAtRiskWalletsTable();
        
        loadData();
    }

    private void setupNegativeWalletsTable() {
        walletIdColumn.setCellValueFactory(cellData -> 
            new SimpleIntegerProperty(cellData.getValue().getId()).asObject());
        
        walletNumberColumn.setCellValueFactory(cellData -> 
            new SimpleStringProperty(cellData.getValue().getWalletNumber()));
        
        walletNameColumn.setCellValueFactory(cellData -> 
            new SimpleStringProperty(cellData.getValue().getName()));
        
        ownerTypeColumn.setCellValueFactory(cellData -> 
            new SimpleStringProperty(cellData.getValue().getOwnerType()));
        
        ownerIdColumn.setCellValueFactory(cellData -> 
            new SimpleIntegerProperty(cellData.getValue().getOwnerId()).asObject());
        
        deficitColumn.setCellValueFactory(cellData -> 
            new SimpleDoubleProperty(cellData.getValue().getDeficit()).asObject());
        deficitColumn.setCellFactory(column -> new TableCell<NegativeWalletData, Double>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(String.format("%.2f", item));
                    setStyle("-fx-text-fill: #EF4444; -fx-font-weight: bold;");
                }
            }
        });
        
        retiredColumn.setCellValueFactory(cellData -> 
            new SimpleDoubleProperty(cellData.getValue().getRetiredCredits()).asObject());
        retiredColumn.setCellFactory(column -> new TableCell<NegativeWalletData, Double>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : String.format("%.2f", item));
            }
        });
        
        priorityColumn.setCellValueFactory(cellData -> 
            new SimpleStringProperty(cellData.getValue().getPriority()));
        priorityColumn.setCellFactory(column -> new TableCell<NegativeWalletData, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    String color = switch (item) {
                        case "CRITICAL" -> "#DC2626";
                        case "HIGH" -> "#EA580C";
                        case "MEDIUM" -> "#F59E0B";
                        default -> "#6B7280";
                    };
                    setStyle("-fx-background-color: " + color + "; -fx-text-fill: white; " +
                            "-fx-font-weight: bold; -fx-padding: 5; -fx-background-radius: 5;");
                }
            }
        });
        
        createdAtColumn.setCellValueFactory(cellData -> 
            new SimpleStringProperty(cellData.getValue().getFormattedCreatedAt()));
    }

    private void setupPriorityOwnersTable() {
        ownerTypeCol.setCellValueFactory(cellData -> 
            new SimpleStringProperty(cellData.getValue().getOwnerType()));
        
        ownerIdCol.setCellValueFactory(cellData -> 
            new SimpleIntegerProperty(cellData.getValue().getOwnerId()).asObject());
        
        negativeCountCol.setCellValueFactory(cellData -> 
            new SimpleIntegerProperty(cellData.getValue().getNegativeWalletCount()).asObject());
        
        totalDeficitCol.setCellValueFactory(cellData -> 
            new SimpleDoubleProperty(cellData.getValue().getTotalDeficit()).asObject());
        totalDeficitCol.setCellFactory(column -> new TableCell<PriorityOwnerData, Double>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(String.format("%.2f", item));
                    setStyle("-fx-text-fill: #EF4444; -fx-font-weight: bold;");
                }
            }
        });
        
        riskLevelCol.setCellValueFactory(cellData -> 
            new SimpleStringProperty(cellData.getValue().getRiskLevel()));
        riskLevelCol.setCellFactory(column -> new TableCell<PriorityOwnerData, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    String color = switch (item) {
                        case "CRITICAL" -> "#DC2626";
                        case "HIGH" -> "#EA580C";
                        case "MEDIUM" -> "#F59E0B";
                        default -> "#6B7280";
                    };
                    setStyle("-fx-background-color: " + color + "; -fx-text-fill: white; " +
                            "-fx-font-weight: bold; -fx-padding: 5; -fx-background-radius: 5;");
                }
            }
        });
    }

    private void setupAtRiskWalletsTable() {
        riskWalletIdColumn.setCellValueFactory(cellData -> 
            new SimpleIntegerProperty(cellData.getValue().getId()).asObject());
        
        riskWalletNumberColumn.setCellValueFactory(cellData -> 
            new SimpleStringProperty(cellData.getValue().getWalletNumber()));
        
        riskWalletNameColumn.setCellValueFactory(cellData -> 
            new SimpleStringProperty(cellData.getValue().getName()));
        
        riskAvailableColumn.setCellValueFactory(cellData -> 
            new SimpleDoubleProperty(cellData.getValue().getAvailableCredits()).asObject());
        riskAvailableColumn.setCellFactory(column -> new TableCell<AtRiskWalletData, Double>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(String.format("%.2f", item));
                    String color = item < 10 ? "#EF4444" : item < 25 ? "#F59E0B" : "#F97316";
                    setStyle("-fx-text-fill: " + color + "; -fx-font-weight: bold;");
                }
            }
        });
        
        riskWarningColumn.setCellValueFactory(cellData -> 
            new SimpleStringProperty(cellData.getValue().getWarningLevel()));
        riskWarningColumn.setCellFactory(column -> new TableCell<AtRiskWalletData, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    String color = switch (item) {
                        case "URGENT" -> "#DC2626";
                        case "HIGH" -> "#EA580C";
                        case "MEDIUM" -> "#F59E0B";
                        default -> "#6B7280";
                    };
                    setStyle("-fx-background-color: " + color + "; -fx-text-fill: white; " +
                            "-fx-font-weight: bold; -fx-padding: 5; -fx-background-radius: 5;");
                }
            }
        });
    }

    private void loadData() {
        loadOverview();
        loadNegativeWallets();
        loadPriorityOwners();
        loadAtRiskWallets();
    }

    private void loadOverview() {
        Map<String, Object> overview = supervisionService.getWalletOverview();
        
        totalWalletsLabel.setText(String.valueOf(overview.get("totalWallets")));
        negativeWalletsLabel.setText(String.valueOf(overview.get("negativeWallets")));
        atRiskWalletsLabel.setText(String.valueOf(overview.get("atRiskWallets")));
        cumulativeDeficitLabel.setText(String.format("%.2f", (double) overview.get("cumulativeDeficit")));
        avgBalanceLabel.setText(String.format("%.2f", (double) overview.get("averageBalance")));
        totalAvailableLabel.setText(String.format("%.2f", (double) overview.get("totalAvailableCredits")));
    }

    private void loadNegativeWallets() {
        negativeWalletsList.clear();
        
        List<Map<String, Object>> wallets = supervisionService.getNegativeWallets(25);
        for (Map<String, Object> wallet : wallets) {
            negativeWalletsList.add(new NegativeWalletData(wallet));
        }
        
        negativeWalletsTable.setItems(negativeWalletsList);
    }

    private void loadPriorityOwners() {
        priorityOwnersList.clear();
        
        List<Map<String, Object>> owners = supervisionService.getPriorityOwners(10);
        for (Map<String, Object> owner : owners) {
            priorityOwnersList.add(new PriorityOwnerData(owner));
        }
        
        priorityOwnersTable.setItems(priorityOwnersList);
    }

    private void loadAtRiskWallets() {
        atRiskWalletsList.clear();
        
        List<Map<String, Object>> wallets = supervisionService.getAtRiskWallets(20);
        for (Map<String, Object> wallet : wallets) {
            atRiskWalletsList.add(new AtRiskWalletData(wallet));
        }
        
        atRiskWalletsTable.setItems(atRiskWalletsList);
    }

    @FXML
    private void handleRefresh() {
        loadData();
        showSuccess("Données actualisées");
    }

    private void showSuccess(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Succès");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    // Data classes
    public static class NegativeWalletData {
        private int id;
        private String walletNumber;
        private String name;
        private String ownerType;
        private int ownerId;
        private double deficit;
        private double retiredCredits;
        private String priority;
        private Timestamp createdAt;

        public NegativeWalletData(Map<String, Object> data) {
            this.id = (int) data.get("id");
            this.walletNumber = (String) data.get("walletNumber");
            this.name = (String) data.get("name");
            this.ownerType = (String) data.get("ownerType");
            this.ownerId = (int) data.get("ownerId");
            this.deficit = (double) data.get("deficit");
            this.retiredCredits = (double) data.get("retiredCredits");
            this.priority = (String) data.get("priority");
            this.createdAt = (Timestamp) data.get("createdAt");
        }

        public int getId() { return id; }
        public String getWalletNumber() { return walletNumber; }
        public String getName() { return name; }
        public String getOwnerType() { return ownerType; }
        public int getOwnerId() { return ownerId; }
        public double getDeficit() { return deficit; }
        public double getRetiredCredits() { return retiredCredits; }
        public String getPriority() { return priority; }
        public String getFormattedCreatedAt() {
            return createdAt != null ? createdAt.toLocalDateTime().format(DATE_FORMATTER) : "N/A";
        }
    }

    public static class PriorityOwnerData {
        private String ownerType;
        private int ownerId;
        private int negativeWalletCount;
        private double totalDeficit;
        private String riskLevel;

        public PriorityOwnerData(Map<String, Object> data) {
            this.ownerType = (String) data.get("ownerType");
            this.ownerId = (int) data.get("ownerId");
            this.negativeWalletCount = (int) data.get("negativeWalletCount");
            this.totalDeficit = (double) data.get("totalDeficit");
            this.riskLevel = (String) data.get("riskLevel");
        }

        public String getOwnerType() { return ownerType; }
        public int getOwnerId() { return ownerId; }
        public int getNegativeWalletCount() { return negativeWalletCount; }
        public double getTotalDeficit() { return totalDeficit; }
        public String getRiskLevel() { return riskLevel; }
    }

    public static class AtRiskWalletData {
        private int id;
        private String walletNumber;
        private String name;
        private double availableCredits;
        private String warningLevel;

        public AtRiskWalletData(Map<String, Object> data) {
            this.id = (int) data.get("id");
            this.walletNumber = (String) data.get("walletNumber");
            this.name = (String) data.get("name");
            this.availableCredits = (double) data.get("availableCredits");
            this.warningLevel = (String) data.get("warningLevel");
        }

        public int getId() { return id; }
        public String getWalletNumber() { return walletNumber; }
        public String getName() { return name; }
        public double getAvailableCredits() { return availableCredits; }
        public String getWarningLevel() { return warningLevel; }
    }
}
