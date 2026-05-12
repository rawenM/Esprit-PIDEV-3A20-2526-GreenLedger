package Controllers;

import Models.Projet;
import Models.Evaluation;
import Services.EvaluationService;
import Services.ProjetService;
import Utils.NavigationContext;
import Utils.SessionManager;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Evaluation Queue Controller
 * Route: GET /expert/evaluations/queue
 *
 * Shows all SUBMITTED projects paginated (10/page).
 * Each row: id, name, description, date, fraud_flag, fraud_risk, ml_decision, credibility.
 * "▶ Évaluer" button starts evaluation.
 */
public class EvaluationQueueController extends BaseController {

    private static final int PAGE_SIZE = 10;

    // ── KPI labels ──────────────────────────────────────────────────────────
    @FXML private Label lblPending;
    @FXML private Label lblOnPage;
    @FXML private Label lblFraudCount;
    @FXML private Label lblToVerify;
    @FXML private Label lblPageInfo;
    @FXML private Label lblPageCount;

    // ── Search ───────────────────────────────────────────────────────────────
    @FXML private TextField txtSearch;

    // ── Table ────────────────────────────────────────────────────────────────
    @FXML private TableView<Projet>          tableQueue;
    @FXML private TableColumn<Projet,String> colNom;
    @FXML private TableColumn<Projet,String> colDescription;
    @FXML private TableColumn<Projet,String> colStatut;
    @FXML private TableColumn<Projet,String> colFraud;
    @FXML private TableColumn<Projet,String> colMlDecision;
    @FXML private TableColumn<Projet,String> colAction;

    // ── Pagination ───────────────────────────────────────────────────────────
    @FXML private Button btnPrev;
    @FXML private Button btnNext;

    private final ProjetService     projetService     = new ProjetService();
    private final EvaluationService evaluationService = new EvaluationService();

    private List<Projet> allProjects = List.of();
    private int currentPage = 1;

    // ── Init ─────────────────────────────────────────────────────────────────

    @FXML
    public void initialize() {
        super.initialize();
        setupTable();
        loadQueue();

        // Live search
        txtSearch.textProperty().addListener((obs, o, n) -> {
            currentPage = 1;
            applyFilter(n);
        });
    }

    // ── Navigation ───────────────────────────────────────────────────────────

    @FXML private void onBack()    { navigateBack(); }
    @FXML private void onRefresh() { loadQueue(); }

    @FXML
    private void onPrevPage() {
        if (currentPage > 1) { currentPage--; renderPage(); }
    }

    @FXML
    private void onNextPage() {
        int total = totalPages();
        if (currentPage < total) { currentPage++; renderPage(); }
    }

    // ── Data loading ─────────────────────────────────────────────────────────

    private void loadQueue() {
        List<Projet> all = projetService.afficher();

        long pending  = all.stream().filter(p -> "SUBMITTED".equals(p.getStatutEvaluation())).count();
        long fraud    = all.stream().filter(p -> Boolean.TRUE.equals(p.getFraudFlag())).count();
        long toVerify = all.stream().filter(p -> {
            Double r = p.getFraudRiskScore();
            return r != null && r >= 0.35 && r < 0.65;
        }).count();

        lblPending.setText(String.valueOf(pending));
        lblFraudCount.setText(String.valueOf(fraud));
        lblToVerify.setText(String.valueOf(toVerify));

        allProjects = all.stream()
            .filter(p -> "SUBMITTED".equals(p.getStatutEvaluation()))
            .collect(Collectors.toList());

        currentPage = 1;
        renderPage();
    }

    private void applyFilter(String keyword) {
        List<Projet> filtered = allProjects;
        if (keyword != null && !keyword.isBlank()) {
            String kw = keyword.toLowerCase();
            filtered = allProjects.stream()
                .filter(p -> (p.getTitre() != null && p.getTitre().toLowerCase().contains(kw))
                          || (p.getDescription() != null && p.getDescription().toLowerCase().contains(kw)))
                .collect(Collectors.toList());
        }
        renderPage(filtered);
    }

    private void renderPage() {
        applyFilter(txtSearch.getText());
    }

    private void renderPage(List<Projet> source) {
        int total = (int) Math.ceil((double) source.size() / PAGE_SIZE);
        if (total == 0) total = 1;
        currentPage = Math.max(1, Math.min(currentPage, total));

        int from = (currentPage - 1) * PAGE_SIZE;
        int to   = Math.min(from + PAGE_SIZE, source.size());
        List<Projet> page = source.subList(from, to);

        tableQueue.setItems(FXCollections.observableArrayList(page));

        if (lblOnPage != null) lblOnPage.setText(String.valueOf(page.size()));
        if (lblPageCount != null) lblPageCount.setText(source.size() + " présentés");
        lblPageInfo.setText("Page " + currentPage + " / " + total + "  (" + source.size() + " projets)");
        btnPrev.setDisable(currentPage <= 1);
        btnNext.setDisable(currentPage >= total);
    }

    private int totalPages() {
        return Math.max(1, (int) Math.ceil((double) allProjects.size() / PAGE_SIZE));
    }

    // ── Table setup ──────────────────────────────────────────────────────────

    private void setupTable() {
        // PROJET — name + id sub-label
        colNom.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setGraphic(null); return;
                }
                Projet p = (Projet) getTableRow().getItem();
                String titre = p.getTitre() != null ? p.getTitre() : "—";
                Label name = new Label(titre);
                name.setStyle("-fx-font-size:12px;-fx-font-weight:700;-fx-text-fill:#1A2E26;");
                name.setWrapText(false);
                Label id = new Label("ID Évaluation " + p.getId());
                id.setStyle("-fx-font-size:9px;-fx-text-fill:#9CA3AF;");
                javafx.scene.layout.VBox box = new javafx.scene.layout.VBox(2, name, id);
                setGraphic(box);
                setText(null);
            }
        });

        // DESCRIPTION — truncated
        colDescription.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setText(null); return;
                }
                Projet p = (Projet) getTableRow().getItem();
                String d = p.getDescription();
                if (d == null || d.isBlank()) { setText("—"); setStyle("-fx-text-fill:#9CA3AF;-fx-font-size:11px;"); return; }
                String truncated = d.length() > 70 ? d.substring(0, 70) + "…" : d;
                setText(truncated);
                setStyle("-fx-font-size:11px;-fx-text-fill:#374151;");
                setTooltip(new Tooltip(d));
            }
        });

        // STATUT badge
        colStatut.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setGraphic(null); return;
                }
                Projet p = (Projet) getTableRow().getItem();
                Label badge = new Label("SUBMITTED");
                badge.setStyle("-fx-background-color:#DBEAFE;-fx-text-fill:#1D4ED8;-fx-font-size:9px;-fx-font-weight:700;-fx-background-radius:4;-fx-padding:3 8;");
                setGraphic(badge);
                setText(null);
            }
        });

        // FRAUDE badge — OK / Suspect / À vérifier
        colFraud.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setGraphic(null); return;
                }
                Projet p = (Projet) getTableRow().getItem();
                Boolean flag = p.getFraudFlag();
                Double risk  = p.getFraudRiskScore();

                String label, bg, color;
                if (Boolean.TRUE.equals(flag) || (risk != null && risk >= 0.65)) {
                    label = "Suspect\nRisk:" + (risk != null ? String.format("%.4f", risk) : "—");
                    bg = "#FEE2E2"; color = "#991B1B";
                } else if (risk != null && risk >= 0.35) {
                    label = "À vérifier\nRisk:" + String.format("%.4f", risk);
                    bg = "#FEF3C7"; color = "#92400E";
                } else {
                    label = "OK";
                    bg = "#D1FAE5"; color = "#065F46";
                }
                Label badge = new Label(label);
                badge.setStyle("-fx-background-color:" + bg + ";-fx-text-fill:" + color
                    + ";-fx-font-size:9px;-fx-font-weight:700;-fx-background-radius:4;-fx-padding:3 8;");
                badge.setWrapText(true);
                setGraphic(badge);
                setText(null);
            }
        });

<<<<<<< HEAD
        // SUGGEST ML — APPROVED/REJECTED badge + credibility %
=======
        // SUGGEST ML — read from ml_predictions/ml_decision_snapshots, not ESG score
>>>>>>> 697f7351277b2a6316572ab9077f2061a493ce44
        colMlDecision.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setText("—"); setStyle("-fx-text-fill:#9CA3AF;"); setGraphic(null); return;
                }
                Projet p = (Projet) getTableRow().getItem();
<<<<<<< HEAD
                Integer esg = p.getScoreEsg();
                if (esg == null) { setText("—"); setStyle("-fx-text-fill:#9CA3AF;"); setGraphic(null); return; }

                boolean approved = esg >= 7;
                Label badge = new Label(approved ? "APPROVED" : "REJECTED");
                badge.setStyle("-fx-background-color:" + (approved ? "#D1FAE5" : "#FEE2E2")
                    + ";-fx-text-fill:" + (approved ? "#065F46" : "#991B1B")
                    + ";-fx-font-size:9px;-fx-font-weight:700;-fx-background-radius:4;-fx-padding:2 6;");
                Label cred = new Label("0,000%");
=======
                // Load ML decision from DB
                String decision = loadMlDecision(p.getId());
                if (decision == null || decision.isBlank()) {
                    setText("—"); setStyle("-fx-text-fill:#9CA3AF;"); setGraphic(null); return;
                }
                boolean approved = "APPROVED".equalsIgnoreCase(decision);
                boolean rejected = "REJECTED".equalsIgnoreCase(decision);
                String bg    = approved ? "#D1FAE5" : rejected ? "#FEE2E2" : "#FEF3C7";
                String color = approved ? "#065F46" : rejected ? "#991B1B" : "#92400E";
                Label badge = new Label(decision.toUpperCase());
                badge.setStyle("-fx-background-color:" + bg + ";-fx-text-fill:" + color
                    + ";-fx-font-size:9px;-fx-font-weight:700;-fx-background-radius:4;-fx-padding:2 6;");
                // Confidence from ml_decision_snapshots
                double conf = loadMlConfidence(p.getId());
                Label cred = new Label(conf > 0
                    ? String.format("%.3f%%", conf * 100) : "—");
>>>>>>> 697f7351277b2a6316572ab9077f2061a493ce44
                cred.setStyle("-fx-font-size:9px;-fx-text-fill:#9CA3AF;");
                javafx.scene.layout.VBox box = new javafx.scene.layout.VBox(2, badge, cred);
                setGraphic(box);
                setText(null);
            }
        });

        // ACTION — single "Évaluer" button
        colAction.setCellFactory(col -> new TableCell<>() {
            private final Button btn = new Button("Évaluer");
            {
                btn.setStyle("-fx-background-color:white;-fx-text-fill:#1A2E26;-fx-font-size:11px;-fx-font-weight:600;-fx-background-radius:6;-fx-padding:6 14;-fx-cursor:hand;-fx-border-color:#E0E5E3;-fx-border-width:1;");
                btn.setOnAction(e -> {
                    Projet p = getTableView().getItems().get(getIndex());
                    startEvaluation(p);
                });
            }
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : btn);
            }
        });
    }

<<<<<<< HEAD
=======
    // ── ML helpers ───────────────────────────────────────────────────────────

    /** Query ml_decision_snapshots first, then ml_predictions.
     *  Only returns a value if confidence > 0 (ML was actually run). */
    private String loadMlDecision(int projectId) {
        // Only show if ML was actually run with a real confidence score
        String sql1 = "SELECT decision, confidence FROM ml_decision_snapshots " +
                      "WHERE project_id=? AND decision IS NOT NULL AND decision != '' " +
                      "ORDER BY id DESC LIMIT 1";
        try (java.sql.Connection conn = DataBase.MyConnection.getConnection();
             java.sql.PreparedStatement ps = conn.prepareStatement(sql1)) {
            ps.setInt(1, projectId);
            try (java.sql.ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String v = rs.getString("decision");
                    double conf = rs.getDouble("confidence");
                    // Only show if confidence > 0 (ML was actually run)
                    if (v != null && !v.isBlank() && conf > 0) return v;
                }
            }
        } catch (java.sql.SQLException e) { /* try next */ }

        String sql2 = "SELECT decision, confidence FROM ml_predictions " +
                      "WHERE project_id=? AND decision IS NOT NULL AND decision != '' " +
                      "ORDER BY id DESC LIMIT 1";
        try (java.sql.Connection conn = DataBase.MyConnection.getConnection();
             java.sql.PreparedStatement ps = conn.prepareStatement(sql2)) {
            ps.setInt(1, projectId);
            try (java.sql.ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String v = rs.getString("decision");
                    double conf = rs.getDouble("confidence");
                    if (v != null && !v.isBlank() && conf > 0) return v;
                }
            }
        } catch (java.sql.SQLException e) { /* ignore */ }
        return null;
    }

    /** Returns true if an evaluation row exists for this project. */
    private boolean hasEvaluation(int projectId) {
        String sql = "SELECT COUNT(*) FROM evaluation WHERE id_projet=?";
        try (java.sql.Connection conn = DataBase.MyConnection.getConnection();
             java.sql.PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, projectId);
            try (java.sql.ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1) > 0;
            }
        } catch (java.sql.SQLException e) { /* ignore */ }
        return false;
    }

    /** Returns confidence (0-1) from ml_decision_snapshots, or 0 if not found or zero. */
    private double loadMlConfidence(int projectId) {
        String sql = "SELECT confidence FROM ml_decision_snapshots " +
                     "WHERE project_id=? AND confidence > 0 ORDER BY id DESC LIMIT 1";
        try (java.sql.Connection conn = DataBase.MyConnection.getConnection();
             java.sql.PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, projectId);
            try (java.sql.ResultSet rs = ps.executeQuery()) {
                if (rs.next()) { double v = rs.getDouble(1); return rs.wasNull() ? 0 : v; }
            }
        } catch (java.sql.SQLException e) { /* ignore */ }
        return 0;
    }

>>>>>>> 697f7351277b2a6316572ab9077f2061a493ce44
    // ── Start evaluation ─────────────────────────────────────────────────────

    private void startEvaluation(Projet projet) {
        // Validate status
        if (!"SUBMITTED".equals(projet.getStatutEvaluation())) {
            new Alert(Alert.AlertType.WARNING,
                "Le projet doit être en statut SUBMITTED.", ButtonType.OK).showAndWait();
            return;
        }
        if (projet.getTitre() == null || projet.getTitre().isBlank()
         || projet.getDescription() == null || projet.getDescription().isBlank()) {
            new Alert(Alert.AlertType.WARNING,
                "Le projet doit avoir un nom et une description.", ButtonType.OK).showAndWait();
            return;
        }

        try {
            // Check if evaluation already exists
            List<Evaluation> existing = evaluationService.afficherParProjet(projet.getId());
            int evaluationId;

            if (!existing.isEmpty()) {
                evaluationId = existing.get(0).getIdEvaluation();
            } else {
                // Transition to IN_PROGRESS
                projetService.updateStatut(projet.getId(), "IN_PROGRESS");

                // Create evaluation
                Evaluation eval = new Evaluation();
                eval.setIdProjet(projet.getId());
                evaluationService.ajouter(eval);

                List<Evaluation> created = evaluationService.afficherParProjet(projet.getId());
                if (created.isEmpty()) {
                    new Alert(Alert.AlertType.ERROR, "Impossible de créer l'évaluation.", ButtonType.OK).showAndWait();
                    return;
                }
                evaluationId = created.get(0).getIdEvaluation();
            }

            // Navigate to evaluation form
            NavigationContext.getInstance().setCurrentProjectId(projet.getId());
            NavigationContext.getInstance().setCurrentEvaluationId(evaluationId);
            org.GreenLedger.MainFX.setRoot("fxml/evaluation_form");

        } catch (Exception e) {
            new Alert(Alert.AlertType.ERROR, "Erreur: " + e.getMessage(), ButtonType.OK).showAndWait();
        }
    }
}
