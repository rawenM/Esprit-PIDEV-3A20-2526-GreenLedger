package Controllers;

import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import Models.AuditLog;
import dao.AuditLogDAO;
import dao.AuditLogDAOImpl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

public class AuditLogController {

    @FXML private TableView<AuditLog> auditLogTable;
    @FXML private TableColumn<AuditLog, Long> idColumn;
    @FXML private TableColumn<AuditLog, String> dateColumn;
    @FXML private TableColumn<AuditLog, String> actionTypeColumn;
    @FXML private TableColumn<AuditLog, String> userEmailColumn;
    @FXML private TableColumn<AuditLog, String> descriptionColumn;
    @FXML private TableColumn<AuditLog, String> statusColumn;
    @FXML private TableColumn<AuditLog, String> ipAddressColumn;
    @FXML private TableColumn<AuditLog, Void> detailsColumn;

    @FXML private Label totalLogsLabel;
    @FXML private Label todayLogsLabel;
    @FXML private Label failedLogsLabel;
    @FXML private Label warningLogsLabel;

    @FXML private ComboBox<String> filterTypeCombo;
    @FXML private ComboBox<String> filterStatusCombo;
    @FXML private TextField filterEmailField;
    @FXML private DatePicker filterDatePicker;

    @FXML private Button prevButton;
    @FXML private Button nextButton;
    @FXML private Label pageLabel;

    private final AuditLogDAO auditLogDAO = new AuditLogDAOImpl();
    private ObservableList<AuditLog> allLogs = FXCollections.observableArrayList();
    private ObservableList<AuditLog> filteredLogs = FXCollections.observableArrayList();
    
    private int currentPage = 1;
    private final int itemsPerPage = 50;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    @FXML
    public void initialize() {
        setupTableColumns();
        setupFilters();
        loadAuditLogs();
        updateStatistics();
    }

    private void setupTableColumns() {
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        
        dateColumn.setCellValueFactory(cellData -> {
            LocalDateTime date = cellData.getValue().getCreatedAt();
            return new SimpleStringProperty(date != null ? date.format(DATE_FORMATTER) : "");
        });
        
        actionTypeColumn.setCellValueFactory(cellData -> 
            new SimpleStringProperty(cellData.getValue().getActionType().name()));
        
        userEmailColumn.setCellValueFactory(new PropertyValueFactory<>("userEmail"));
        descriptionColumn.setCellValueFactory(new PropertyValueFactory<>("actionDescription"));
        
        statusColumn.setCellValueFactory(cellData -> 
            new SimpleStringProperty(cellData.getValue().getStatus().name()));
        statusColumn.setCellFactory(column -> new TableCell<AuditLog, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    String color = switch (item) {
                        case "SUCCESS" -> "#27ae60";
                        case "FAILED" -> "#e74c3c";
                        case "WARNING" -> "#f39c12";
                        default -> "#95a5a6";
                    };
                    setStyle("-fx-background-color: " + color + "; -fx-text-fill: white; " +
                            "-fx-font-weight: bold; -fx-padding: 5; -fx-background-radius: 5;");
                }
            }
        });
        
        ipAddressColumn.setCellValueFactory(new PropertyValueFactory<>("ipAddress"));
        
        detailsColumn.setCellFactory(column -> new TableCell<AuditLog, Void>() {
            private final Button detailsBtn = new Button("📄");
            {
                detailsBtn.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-font-size: 14px;");
                detailsBtn.setOnAction(e -> {
                    AuditLog log = getTableView().getItems().get(getIndex());
                    showLogDetails(log);
                });
            }
            
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : detailsBtn);
            }
        });
    }

    private void setupFilters() {
        // Types d'actions
        filterTypeCombo.getItems().add("Tous");
        for (AuditLog.ActionType type : AuditLog.ActionType.values()) {
            filterTypeCombo.getItems().add(type.name());
        }
        filterTypeCombo.setValue("Tous");
        
        // Statuts
        filterStatusCombo.getItems().addAll("Tous", "SUCCESS", "FAILED", "WARNING");
        filterStatusCombo.setValue("Tous");
    }

    private void loadAuditLogs() {
        try {
            List<AuditLog> logs = auditLogDAO.findAll();
            allLogs.clear();
            allLogs.addAll(logs);
            
            filteredLogs.clear();
            filteredLogs.addAll(allLogs);
            
            updatePagination();
            
            System.out.println("[AUDIT UI] " + logs.size() + " logs chargés");
        } catch (Exception e) {
            System.err.println("[AUDIT UI] Erreur lors du chargement: " + e.getMessage());
            e.printStackTrace();
            showError("Erreur", "Impossible de charger les logs: " + e.getMessage());
        }
    }

    private void updateStatistics() {
        try {
            long total = auditLogDAO.count();
            long today = auditLogDAO.countToday();
            long failed = auditLogDAO.countByStatus(AuditLog.ActionStatus.FAILED);
            long warning = auditLogDAO.countByStatus(AuditLog.ActionStatus.WARNING);
            
            totalLogsLabel.setText(String.valueOf(total));
            todayLogsLabel.setText(String.valueOf(today));
            failedLogsLabel.setText(String.valueOf(failed));
            warningLogsLabel.setText(String.valueOf(warning));
        } catch (Exception e) {
            System.err.println("[AUDIT UI] Erreur stats: " + e.getMessage());
        }
    }

    private void updatePagination() {
        int totalPages = (int) Math.ceil((double) filteredLogs.size() / itemsPerPage);
        if (totalPages == 0) totalPages = 1;
        
        pageLabel.setText("Page " + currentPage + " / " + totalPages);
        
        prevButton.setDisable(currentPage <= 1);
        nextButton.setDisable(currentPage >= totalPages);
        
        int fromIndex = (currentPage - 1) * itemsPerPage;
        int toIndex = Math.min(fromIndex + itemsPerPage, filteredLogs.size());
        
        if (fromIndex < filteredLogs.size()) {
            auditLogTable.setItems(FXCollections.observableArrayList(
                filteredLogs.subList(fromIndex, toIndex)
            ));
        } else {
            auditLogTable.setItems(FXCollections.observableArrayList());
        }
    }

    @FXML
    private void handleRefresh() {
        loadAuditLogs();
        updateStatistics();
        showSuccess("Logs actualisés");
    }

    @FXML
    private void handleFilter() {
        String selectedType = filterTypeCombo.getValue();
        String selectedStatus = filterStatusCombo.getValue();
        String emailFilter = filterEmailField.getText().trim().toLowerCase();
        LocalDate selectedDate = filterDatePicker.getValue();
        
        filteredLogs.clear();
        
        filteredLogs.addAll(allLogs.stream()
            .filter(log -> {
                // Filtre type
                if (!"Tous".equals(selectedType) && !log.getActionType().name().equals(selectedType)) {
                    return false;
                }
                
                // Filtre statut
                if (!"Tous".equals(selectedStatus) && !log.getStatus().name().equals(selectedStatus)) {
                    return false;
                }
                
                // Filtre email
                if (!emailFilter.isEmpty() && 
                    (log.getUserEmail() == null || !log.getUserEmail().toLowerCase().contains(emailFilter))) {
                    return false;
                }
                
                // Filtre date
                if (selectedDate != null && log.getCreatedAt() != null) {
                    LocalDate logDate = log.getCreatedAt().toLocalDate();
                    if (!logDate.equals(selectedDate)) {
                        return false;
                    }
                }
                
                return true;
            })
            .collect(Collectors.toList())
        );
        
        currentPage = 1;
        updatePagination();
        
        showSuccess(filteredLogs.size() + " logs trouvés");
    }

    @FXML
    private void handleResetFilters() {
        filterTypeCombo.setValue("Tous");
        filterStatusCombo.setValue("Tous");
        filterEmailField.clear();
        filterDatePicker.setValue(null);
        
        filteredLogs.clear();
        filteredLogs.addAll(allLogs);
        
        currentPage = 1;
        updatePagination();
    }

    @FXML
    private void handlePrevious() {
        if (currentPage > 1) {
            currentPage--;
            updatePagination();
        }
    }

    @FXML
    private void handleNext() {
        int totalPages = (int) Math.ceil((double) filteredLogs.size() / itemsPerPage);
        if (currentPage < totalPages) {
            currentPage++;
            updatePagination();
        }
    }

    @FXML
    private void handleCleanOld() {
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Nettoyage");
        confirmation.setHeaderText("Supprimer les logs de plus de 30 jours ?");
        confirmation.setContentText("Cette action est irréversible !");
        
        confirmation.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    int deleted = auditLogDAO.deleteOlderThan(30);
                    showSuccess(deleted + " logs supprimés");
                    handleRefresh();
                } catch (Exception e) {
                    showError("Erreur", "Impossible de nettoyer: " + e.getMessage());
                }
            }
        });
    }

    private void showLogDetails(AuditLog log) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Détails du Log #" + log.getId());
        alert.setHeaderText(null);
        
        StringBuilder content = new StringBuilder();
        content.append("=== INFORMATIONS GÉNÉRALES ===\n\n");
        content.append("ID: ").append(log.getId()).append("\n");
        content.append("Date: ").append(log.getCreatedAt().format(DATE_FORMATTER)).append("\n");
        content.append("Type: ").append(log.getActionType()).append("\n");
        content.append("Statut: ").append(log.getStatus()).append("\n\n");
        
        content.append("=== UTILISATEUR ===\n\n");
        content.append("ID: ").append(log.getUserId() != null ? log.getUserId() : "N/A").append("\n");
        content.append("Email: ").append(log.getUserEmail() != null ? log.getUserEmail() : "N/A").append("\n");
        content.append("Nom: ").append(log.getUserName() != null ? log.getUserName() : "N/A").append("\n\n");
        
        content.append("=== ACTION ===\n\n");
        content.append("Description: ").append(log.getActionDescription()).append("\n");
        
        if (log.getTargetUserId() != null) {
            content.append("\n=== UTILISATEUR CIBLE ===\n\n");
            content.append("ID: ").append(log.getTargetUserId()).append("\n");
            content.append("Email: ").append(log.getTargetUserEmail()).append("\n");
        }
        
        content.append("\n=== TECHNIQUE ===\n\n");
        content.append("IP: ").append(log.getIpAddress() != null ? log.getIpAddress() : "N/A").append("\n");
        content.append("User Agent: ").append(log.getUserAgent() != null ? log.getUserAgent() : "N/A").append("\n");
        content.append("Navigateur: ").append(log.getBrowser() != null ? log.getBrowser() : "N/A").append("\n");
        content.append("OS: ").append(log.getOperatingSystem() != null ? log.getOperatingSystem() : "N/A").append("\n");
        
        if (log.getErrorMessage() != null) {
            content.append("\n=== ERREUR ===\n\n");
            content.append(log.getErrorMessage()).append("\n");
        }
        
        if (log.getOldValue() != null || log.getNewValue() != null) {
            content.append("\n=== MODIFICATIONS ===\n\n");
            content.append("Ancienne valeur: ").append(log.getOldValue() != null ? log.getOldValue() : "N/A").append("\n");
            content.append("Nouvelle valeur: ").append(log.getNewValue() != null ? log.getNewValue() : "N/A").append("\n");
        }
        
        alert.setContentText(content.toString());
        alert.getDialogPane().setPrefWidth(600);
        alert.getDialogPane().setPrefHeight(500);
        alert.showAndWait();
    }

    private void showSuccess(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Succès");
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
