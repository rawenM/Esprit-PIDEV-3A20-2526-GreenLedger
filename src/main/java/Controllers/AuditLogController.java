package Controllers;

import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.web.WebView;
import javafx.scene.web.WebEngine;
import Models.AuditLog;
import dao.AuditLogDAO;
import dao.AuditLogDAOImpl;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
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
    @FXML private Label successRateLabel;

    // Chart WebViews
    @FXML private WebView chartActionsType;
    @FXML private WebView chartSuccessRate;
    @FXML private WebView chartActivity;
    @FXML private WebView chartBrowsers;

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

    /** Cached Chart.js source — loaded once from classpath */
    private static String CHARTJS_SRC = null;

    private static String getChartJs() {
        if (CHARTJS_SRC != null) return CHARTJS_SRC;
        try (InputStream is = AuditLogController.class.getResourceAsStream("/js/chart.umd.min.js")) {
            if (is != null) {
                CHARTJS_SRC = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            }
        } catch (Exception e) {
            System.err.println("[AUDIT CHARTS] Could not load chart.js: " + e.getMessage());
            CHARTJS_SRC = "";
        }
        return CHARTJS_SRC;
    }

    @FXML
    public void initialize() {
        setupTableColumns();
        setupFilters();
        loadAuditLogs();
        updateStatistics();
        // Defer chart rendering until WebViews are fully laid out
        javafx.application.Platform.runLater(this::renderCharts);
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
            long total   = auditLogDAO.count();
            long today   = auditLogDAO.countToday();
            long failed  = auditLogDAO.countByStatus(AuditLog.ActionStatus.FAILED);
            long warning = auditLogDAO.countByStatus(AuditLog.ActionStatus.WARNING);
            long success = auditLogDAO.countByStatus(AuditLog.ActionStatus.SUCCESS);
            totalLogsLabel.setText(String.valueOf(total));
            todayLogsLabel.setText(String.valueOf(today));
            failedLogsLabel.setText(String.valueOf(failed));
            if (warningLogsLabel != null) warningLogsLabel.setText(String.valueOf(warning));

            // Success rate
            if (successRateLabel != null && total > 0) {
                long pct = Math.round(success * 100.0 / total);
                successRateLabel.setText(pct + "%");
            }
        } catch (Exception e) {
            System.err.println("[AUDIT UI] Erreur stats: " + e.getMessage());
        }
    }

    private void renderCharts() {
        try {
            // Build data from allLogs — skip records with null fields
            Map<String, Long> byType = allLogs.stream()
                .filter(l -> l.getActionType() != null)
                .collect(Collectors.groupingBy(
                    l -> l.getActionType().name(), Collectors.counting()));
            if (byType.isEmpty()) {
                byType = new java.util.HashMap<>();
                byType.put("NO_DATA", 1L);
            }

            long successCount = allLogs.stream()
                .filter(l -> l.getStatus() != null && l.getStatus() == AuditLog.ActionStatus.SUCCESS).count();
            long failedCount  = allLogs.stream()
                .filter(l -> l.getStatus() != null && l.getStatus() == AuditLog.ActionStatus.FAILED).count();

            // 7-day activity
            java.time.LocalDate today = java.time.LocalDate.now();
            long[] daily = new long[7];
            String[] dayLabels = new String[7];
            java.time.format.DateTimeFormatter dayFmt =
                java.time.format.DateTimeFormatter.ofPattern("dd/MM");
            for (int i = 6; i >= 0; i--) {
                java.time.LocalDate d = today.minusDays(i);
                dayLabels[6 - i] = d.format(dayFmt);
                final java.time.LocalDate fd = d;
                daily[6 - i] = allLogs.stream()
                    .filter(l -> l.getCreatedAt() != null
                              && l.getCreatedAt().toLocalDate().equals(fd))
                    .count();
            }

            // Browser distribution
            Map<String, Long> byBrowser = allLogs.stream()
                .filter(l -> l.getBrowser() != null && !l.getBrowser().isEmpty())
                .collect(Collectors.groupingBy(l -> l.getBrowser(), Collectors.counting()));
            if (byBrowser.isEmpty()) {
                byBrowser = new java.util.HashMap<>();
                byBrowser.put("Chrome", (long) Math.max(allLogs.size(), 1));
            }

            // Render — Chart.js v2 API
            if (chartActionsType != null)
                chartActionsType.getEngine().loadContent(
                    buildHBarV2(byType));
            if (chartSuccessRate != null)
                chartSuccessRate.getEngine().loadContent(
                    buildDoughnutV2(successCount, failedCount));
            if (chartActivity != null)
                chartActivity.getEngine().loadContent(
                    buildLineV2(dayLabels, daily));
            if (chartBrowsers != null)
                chartBrowsers.getEngine().loadContent(
                    buildHBarV2(byBrowser));

        } catch (Exception e) {
            System.err.println("[AUDIT CHARTS] renderCharts error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ── Chart.js v2 builders ─────────────────────────────────────────────────

    private String buildHBarV2(Map<String, Long> data) {
        StringBuilder labels = new StringBuilder();
        StringBuilder values = new StringBuilder();
        String[] palette = {"#10B981","#3B82F6","#F59E0B","#8B5CF6",
                            "#EF4444","#06B6D4","#F97316","#EC4899"};
        StringBuilder bgColors = new StringBuilder();
        int i = 0;
        for (Map.Entry<String, Long> e : data.entrySet()) {
            if (i > 0) { labels.append(","); values.append(","); bgColors.append(","); }
            labels.append("\"").append(e.getKey()).append("\"");
            values.append(e.getValue());
            bgColors.append("\"").append(palette[i % palette.length]).append("\"");
            i++;
        }
        String dataset = "{label:'',data:[" + values + "]," +
            "backgroundColor:[" + bgColors + "]," +
            "borderWidth:0}";
        String opts = "{responsive:true,maintainAspectRatio:false," +
            "legend:{display:false}," +
            "scales:{xAxes:[{gridLines:{color:'rgba(255,255,255,0.05)'}," +
            "ticks:{fontColor:'#9ca3af'}}]," +
            "yAxes:[{gridLines:{display:false}," +
            "ticks:{fontColor:'#9ca3af',fontSize:10}}]}}";
        return wrapChart("horizontalBar",
            "{labels:[" + labels + "],datasets:[" + dataset + "]}", opts);
    }

    private String buildDoughnutV2(long success, long failed) {
        String dataset = "{data:[" + success + "," + failed + "]," +
            "backgroundColor:[\"#10B981\",\"#EF4444\"]," +
            "borderWidth:0}";
        String opts = "{responsive:true,maintainAspectRatio:false," +
            "cutoutPercentage:72," +
            "legend:{position:'bottom',labels:{fontColor:'#9ca3af',fontSize:11}}}";
        return wrapChart("doughnut",
            "{labels:[\"SUCCESS\",\"FAILED\"],datasets:[" + dataset + "]}", opts);
    }

    private String buildLineV2(String[] labels, long[] values) {
        StringBuilder lbls = new StringBuilder();
        StringBuilder vals = new StringBuilder();
        for (int i = 0; i < labels.length; i++) {
            if (i > 0) { lbls.append(","); vals.append(","); }
            lbls.append("\"").append(labels[i]).append("\"");
            vals.append(values[i]);
        }
        String dataset = "{label:'Actions',data:[" + vals + "]," +
            "borderColor:\"#3B82F6\"," +
            "backgroundColor:\"rgba(59,130,246,0.08)\"," +
            "pointBackgroundColor:\"#3B82F6\"," +
            "pointRadius:4,borderWidth:2,fill:true}";
        String opts = "{responsive:true,maintainAspectRatio:false," +
            "legend:{display:false}," +
            "scales:{xAxes:[{gridLines:{color:'rgba(255,255,255,0.05)'}," +
            "ticks:{fontColor:'#9ca3af',fontSize:10}}]," +
            "yAxes:[{gridLines:{color:'rgba(255,255,255,0.05)'}," +
            "ticks:{fontColor:'#9ca3af',beginAtZero:true}}]}}";
        return wrapChart("line",
            "{labels:[" + lbls + "],datasets:[" + dataset + "]}", opts);
    }

    private String wrapChart(String type, String data, String options) {
        String js = getChartJs();
        return "<!DOCTYPE html><html><head><meta charset='utf-8'>" +
            "<style>" +
            "*{margin:0;padding:0;box-sizing:border-box}" +
            "html,body{width:100%;height:100%;background:#1e293b;overflow:hidden}" +
            "body{display:flex;align-items:center;justify-content:center;padding:10px}" +
            ".chart-wrap{position:relative;width:100%;height:100%}" +
            "</style>" +
            "<script>" + js + "</script>" +
            "</head><body>" +
            "<div class='chart-wrap'><canvas id='c'></canvas></div>" +
            "<script>" +
            "try{" +
            "  var ctx=document.getElementById('c').getContext('2d');" +
            "  new Chart(ctx,{type:'" + type + "',data:" + data +
            ",options:" + options + "});" +
            "}catch(e){document.body.innerHTML='<pre style=\"color:red;font-size:10px\">'+e+'</pre>';}" +
            "</script></body></html>";
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
        javafx.application.Platform.runLater(this::renderCharts);
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
