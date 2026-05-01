package Controllers;

import DataBase.MyConnection;
import Models.Projet;
import Services.EvaluationPdfService;
import Services.ProjetService;
import Utils.NavigationContext;
import Utils.SessionManager;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.stage.FileChooser;

import java.io.File;
import java.io.FileOutputStream;
import java.sql.*;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * Page 1 — Project list for Porteur de Projet.
 * Shows only projects WHERE entreprise_id = currentUser.id
 */
public class PorteurProjetsController extends BaseController {

    @FXML private TextField        txtSearch;
    @FXML private ComboBox<String> cmbStatut;
    @FXML private Label            lblCount;

    @FXML private TableView<ProjetRow>          tableProjects;
    @FXML private TableColumn<ProjetRow,String> colId;
    @FXML private TableColumn<ProjetRow,String> colTitre;
    @FXML private TableColumn<ProjetRow,String> colStatut;
    @FXML private TableColumn<ProjetRow,String> colScore;
    @FXML private TableColumn<ProjetRow,String> colDecision;
    @FXML private TableColumn<ProjetRow,String> colDate;
    @FXML private TableColumn<ProjetRow,String> colActions;

    private final ProjetService        projetService = new ProjetService();
    private final EvaluationPdfService pdfService    = new EvaluationPdfService();

    private List<ProjetRow> allRows;

    @FXML
    public void initialize() {
        super.initialize();
        cmbStatut.setItems(FXCollections.observableArrayList(
            "Tous", "DRAFT", "SUBMITTED", "IN_PROGRESS", "APPROVED", "REJECTED"));
        cmbStatut.setValue("Tous");
        setupTable();
        loadProjects();
        applyProfile(lblProfileName, lblProfileType);
    }

    @FXML private void onBack()     { navigate("fxml/porteur_shell"); }
    @FXML private void onNewProjet(){ navigate("fxml/porteur_projet_form"); }
    @FXML private void onFilter()   { applyFilter(); }

    private void loadProjects() {
        var user = SessionManager.getInstance().getCurrentUser();
        if (user == null) return;

        List<Projet> projets = projetService.getByEntreprise(user.getId().intValue());
        allRows = projets.stream().map(p -> {
            ProjetRow row = new ProjetRow();
            row.projet    = p;
            row.score     = loadLatestScore(p.getId());
            row.mlDecision= loadMlDecision(p.getId());
            return row;
        }).collect(Collectors.toList());

        applyFilter();
    }

    private void applyFilter() {
        String kw  = txtSearch.getText() != null ? txtSearch.getText().toLowerCase() : "";
        String st  = cmbStatut.getValue();

        List<ProjetRow> filtered = allRows.stream()
            .filter(r -> kw.isBlank()
                || r.projet.getTitre().toLowerCase().contains(kw)
                || (r.projet.getDescription() != null
                    && r.projet.getDescription().toLowerCase().contains(kw)))
            .filter(r -> "Tous".equals(st) || st == null
                || st.equalsIgnoreCase(r.projet.getStatutEvaluation()))
            .collect(Collectors.toList());

        tableProjects.setItems(FXCollections.observableArrayList(filtered));
        lblCount.setText(filtered.size() + " projet(s)");
    }

    private void setupTable() {
        colId.setCellValueFactory(c ->
            new SimpleStringProperty(String.valueOf(c.getValue().projet.getId())));
        colTitre.setCellValueFactory(c ->
            new SimpleStringProperty(c.getValue().projet.getTitre()));
        colDate.setCellValueFactory(c -> {
            var d = c.getValue().projet.getDateCreation();
            return new SimpleStringProperty(d != null
                ? d.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) : "-");
        });

        // Statut badge
        colStatut.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setGraphic(null); return;
                }
                String s = getTableRow().getItem().projet.getStatutEvaluation();
                if (s == null) s = "DRAFT";
                Label badge = new Label(s);
                String bg = switch (s) {
                    case "APPROVED"    -> "#D1FAE5"; case "REJECTED"    -> "#FEE2E2";
                    case "SUBMITTED"   -> "#DBEAFE"; case "IN_PROGRESS" -> "#FEF3C7";
                    default            -> "#F3F4F6";
                };
                String fg = switch (s) {
                    case "APPROVED"    -> "#065F46"; case "REJECTED"    -> "#991B1B";
                    case "SUBMITTED"   -> "#1D4ED8"; case "IN_PROGRESS" -> "#92400E";
                    default            -> "#374151";
                };
                badge.setStyle("-fx-background-color:" + bg + ";-fx-text-fill:" + fg
                    + ";-fx-font-size:10px;-fx-font-weight:700;-fx-background-radius:4;-fx-padding:3 8;");
                setGraphic(badge); setText(null);
            }
        });

        // Score
        colScore.setCellValueFactory(c -> {
            Double sc = c.getValue().score;
            return new SimpleStringProperty(sc != null
                ? String.format(Locale.ROOT, "%.2f", sc) : "-");
        });

        // ML Decision
        colDecision.setCellValueFactory(c -> {
            String d = c.getValue().mlDecision;
            return new SimpleStringProperty(d != null ? d : "-");
        });

        // Actions
        colActions.setCellFactory(col -> new TableCell<>() {
            private final Button btnView   = new Button("Voir");
            private final Button btnEdit   = new Button("Modifier");
            private final Button btnSubmit = new Button("Soumettre");
            private final Button btnPdf    = new Button("PDF");
            private final Button btnDel    = new Button("Suppr.");

            {
                String base = "-fx-font-size:10px;-fx-font-weight:600;-fx-background-radius:5;-fx-padding:5 8;-fx-cursor:hand;";
                btnView.setStyle(base + "-fx-background-color:#EFF6FF;-fx-text-fill:#1D4ED8;");
                btnEdit.setStyle(base + "-fx-background-color:#F3F4F6;-fx-text-fill:#374151;");
                btnSubmit.setStyle(base + "-fx-background-color:#FEF3C7;-fx-text-fill:#92400E;");
                btnPdf.setStyle(base + "-fx-background-color:#F0FDF4;-fx-text-fill:#15803D;");
                btnDel.setStyle(base + "-fx-background-color:#FEE2E2;-fx-text-fill:#991B1B;");

                btnView.setOnAction(e -> {
                    ProjetRow r = getTableView().getItems().get(getIndex());
                    NavigationContext.getInstance().setCurrentProjectId(r.projet.getId());
                    navigate("ProjetDetail");
                });
                btnEdit.setOnAction(e -> {
                    ProjetRow r = getTableView().getItems().get(getIndex());
                    NavigationContext.getInstance().setCurrentProjectId(r.projet.getId());
                    navigate("fxml/porteur_projet_form");
                });
                btnSubmit.setOnAction(e -> {
                    ProjetRow r = getTableView().getItems().get(getIndex());
                    handleSubmit(r.projet);
                });
                btnPdf.setOnAction(e -> {
                    ProjetRow r = getTableView().getItems().get(getIndex());
                    handlePdf(r.projet);
                });
                btnDel.setOnAction(e -> {
                    ProjetRow r = getTableView().getItems().get(getIndex());
                    handleDelete(r.projet);
                });
            }

            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setGraphic(null); return;
                }
                ProjetRow r = getTableRow().getItem();
                String statut = r.projet.getStatutEvaluation();
                HBox box = new HBox(4, btnView);
                boolean canEdit = "DRAFT".equals(statut) || "SUBMITTED".equals(statut);
                boolean canSubmit = "DRAFT".equals(statut);
                boolean canDelete = "DRAFT".equals(statut);
                if (canEdit)   box.getChildren().add(btnEdit);
                if (canSubmit) box.getChildren().add(btnSubmit);
                box.getChildren().add(btnPdf);
                if (canDelete) box.getChildren().add(btnDel);
                setGraphic(box);
            }
        });
    }

    private void handleSubmit(Projet p) {
        var user = SessionManager.getInstance().getCurrentUser();
        if (user == null) return;
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
            "Soumettre \"" + p.getTitre() + "\" pour evaluation ?", ButtonType.YES, ButtonType.NO);
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.YES) {
                projetService.updateStatut(p.getId(), "SUBMITTED");
                loadProjects();
                new Alert(Alert.AlertType.INFORMATION,
                    "Projet soumis pour evaluation !", ButtonType.OK).showAndWait();
            }
        });
    }

    private void handlePdf(Projet p) {
        // Load latest evaluation id for this project
        long evalId = loadLatestEvalId(p.getId());
        if (evalId <= 0) {
            new Alert(Alert.AlertType.WARNING,
                "Aucune evaluation disponible pour ce projet.", ButtonType.OK).showAndWait();
            return;
        }
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Enregistrer le PDF");
        chooser.setInitialFileName("projet_" + p.getId() + ".pdf");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF", "*.pdf"));
        File file = chooser.showSaveDialog(tableProjects.getScene().getWindow());
        if (file == null) return;
        long fEvalId = evalId;
        new Thread(() -> {
            try {
                byte[] pdf = pdfService.generatePdf(fEvalId);
                try (FileOutputStream fos = new FileOutputStream(file)) { fos.write(pdf); }
                Platform.runLater(() ->
                    new Alert(Alert.AlertType.INFORMATION, "PDF genere: " + file.getName(), ButtonType.OK).showAndWait());
            } catch (Exception ex) {
                Platform.runLater(() ->
                    new Alert(Alert.AlertType.ERROR, "Erreur PDF: " + ex.getMessage(), ButtonType.OK).showAndWait());
            }
        }).start();
    }

    private void handleDelete(Projet p) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
            "Supprimer \"" + p.getTitre() + "\" ?", ButtonType.YES, ButtonType.NO);
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.YES) {
                projetService.delete(p.getId());
                loadProjects();
            }
        });
    }

    private Double loadLatestScore(int projectId) {
        String sql = "SELECT score_final FROM evaluation WHERE id_projet=? ORDER BY id_evaluation DESC LIMIT 1";
        try (Connection conn = MyConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, projectId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) { double v = rs.getDouble(1); return rs.wasNull() ? null : v; }
            }
        } catch (SQLException e) { /* ignore */ }
        return null;
    }

    private String loadMlDecision(int projectId) {
        String sql = "SELECT decision FROM ml_predictions WHERE project_id=? ORDER BY id DESC LIMIT 1";
        try (Connection conn = MyConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, projectId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getString(1);
            }
        } catch (SQLException e) { /* ignore */ }
        return null;
    }

    private long loadLatestEvalId(int projectId) {
        String sql = "SELECT id_evaluation FROM evaluation WHERE id_projet=? ORDER BY id_evaluation DESC LIMIT 1";
        try (Connection conn = MyConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, projectId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getLong(1);
            }
        } catch (SQLException e) { /* ignore */ }
        return -1;
    }

    // ── Sidebar fields ────────────────────────────────────────────────────
    @FXML private Label lblProfileName;
    @FXML private Label lblProfileType;

    // ── Sidebar navigation ────────────────────────────────────────────────
    @FXML private void onDashboard()   { navigate("fxml/porteur_shell"); }
    @FXML private void onProjets()     { loadProjects(); }
    @FXML private void onEvaluations() { navigate("ownerEvaluations"); }
    @FXML private void onFinancing()   { navigate("financement"); }
    @FXML private void onMessages()    { navigate("fxml/porteur_messages"); }
    @FXML private void onAssistant()   { navigate("fxml/porteur_assistant"); }
    @FXML private void onEditProfile() { navigate("editProfile"); }
    @FXML private void onLogout() {
        Utils.SessionManager.getInstance().invalidate();
        navigate("fxml/login");
    }

    private void navigate(String fxml) {
        try { org.GreenLedger.MainFX.setRoot(fxml); }
        catch (Exception e) { System.err.println("[PorteurProjets] Nav: " + e.getMessage()); }
    }

    // ── Inner DTO ─────────────────────────────────────────────────────────
    public static class ProjetRow {
        Projet projet;
        Double score;
        String mlDecision;
    }
}
