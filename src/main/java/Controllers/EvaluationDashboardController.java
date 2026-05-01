package Controllers;

import Models.Evaluation;
import Models.Projet;
import Services.EvaluationService;
import Services.ProjetService;
import Utils.NavigationContext;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Dashboard Évaluations — matches screenshot 1
 * Columns: PROJET | SCORE FINAL | SCORE ML | DÉCISION | STATUT PROJET | ACTIONS
 * Actions: Modifier | Résumé | Réévaluer
 */
public class EvaluationDashboardController extends BaseController {

    private static final int PAGE_SIZE = 10;

    @FXML private Label  lblProfileName;
    @FXML private Label  lblProfileType;
    @FXML private TextField  txtSearch;
    @FXML private ComboBox<String> cmbDecision;
    @FXML private CheckBox   chkReeval;
    @FXML private Label      lblTotal;
    @FXML private Label      lblCount;
    @FXML private Label      lblPage;
    @FXML private Button     btnPrev;
    @FXML private Button     btnNext;

    @FXML private TableView<Evaluation>          tableEvals;
    @FXML private TableColumn<Evaluation,String> colProjet;
    @FXML private TableColumn<Evaluation,String> colScoreFinal;
    @FXML private TableColumn<Evaluation,String> colScoreMl;
    @FXML private TableColumn<Evaluation,String> colDecision;
    @FXML private TableColumn<Evaluation,String> colStatut;
    @FXML private TableColumn<Evaluation,String> colActions;

    private final EvaluationService evalService  = new EvaluationService();
    private final ProjetService     projetService = new ProjetService();

    private List<Evaluation> allEvals = List.of();
    private int currentPage = 1;

    @FXML
    public void initialize() {
        super.initialize();
        applyProfile(lblProfileName, lblProfileType);
        setupFilters();
        setupTable();
        loadAll();
    }

    @FXML private void onBack()        { navigateBack(); }
    @FXML private void onQueue()       { navigate("fxml/expert_shell"); }
    @FXML private void onDashboard()   { /* already here */ }
    @FXML private void onFraud()       { navigate("fxml/expert_carbon_dashboard"); }
    @FXML private void onEditProfile() { navigate("editProfile"); }
    @FXML private void onFilter()      { currentPage = 1; applyFilters(); }

    @FXML
    private void onLogout() {
        Utils.SessionManager.getInstance().invalidate();
        navigate("fxml/login");
    }

    @FXML private void onPrevPage() { if (currentPage > 1) { currentPage--; renderPage(filtered()); } }
    @FXML private void onNextPage() {
        int total = totalPages(filtered());
        if (currentPage < total) { currentPage++; renderPage(filtered()); }
    }

    private void setupFilters() {
        cmbDecision.setItems(FXCollections.observableArrayList(
            "Toutes décisions", "APPROVED", "REJECTED", "REVISION_REQUIRED"
        ));
        cmbDecision.setValue("Toutes décisions");
        txtSearch.textProperty().addListener((o, a, b) -> { currentPage = 1; applyFilters(); });
        chkReeval.selectedProperty().addListener((o, a, b) -> { currentPage = 1; applyFilters(); });
    }

    private void loadAll() {
        allEvals = evalService.getAllEvaluations();
        lblTotal.setText(allEvals.size() + " évals");
        applyFilters();
    }

    private void applyFilters() {
        renderPage(filtered());
    }

    private List<Evaluation> filtered() {
        String kw  = txtSearch.getText() != null ? txtSearch.getText().toLowerCase() : "";
        String dec = cmbDecision.getValue();

        return allEvals.stream()
            .filter(e -> kw.isBlank()
                || (e.getTitreProjet() != null && e.getTitreProjet().toLowerCase().contains(kw)))
            .filter(e -> dec == null || "Toutes décisions".equals(dec)
                || dec.equalsIgnoreCase(e.getDecision()))
            .collect(Collectors.toList());
    }

    private void renderPage(List<Evaluation> source) {
        int total = Math.max(1, (int) Math.ceil((double) source.size() / PAGE_SIZE));
        currentPage = Math.max(1, Math.min(currentPage, total));

        int from = (currentPage - 1) * PAGE_SIZE;
        int to   = Math.min(from + PAGE_SIZE, source.size());

        tableEvals.setItems(FXCollections.observableArrayList(source.subList(from, to)));
        lblCount.setText(source.size() + " évaluations");
        lblPage.setText("Page " + currentPage + " / " + total);
        btnPrev.setDisable(currentPage <= 1);
        btnNext.setDisable(currentPage >= total);
    }

    private int totalPages(List<Evaluation> source) {
        return Math.max(1, (int) Math.ceil((double) source.size() / PAGE_SIZE));
    }

    private void setupTable() {
        // PROJET column — avatar circle + name + id
        colProjet.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setGraphic(null); return;
                }
                Evaluation e = (Evaluation) getTableRow().getItem();
                String titre = e.getTitreProjet() != null ? e.getTitreProjet() : "—";
                String initials = titre.length() >= 2 ? titre.substring(0, 2).toUpperCase() : "??";

                Label avatar = new Label(initials);
                avatar.setStyle("-fx-background-color:#2D5F3F;-fx-text-fill:white;-fx-font-size:10px;-fx-font-weight:700;-fx-background-radius:16;-fx-min-width:32;-fx-min-height:32;-fx-alignment:CENTER;");

                javafx.scene.layout.VBox info = new javafx.scene.layout.VBox(2);
                Label name = new Label(titre.length() > 22 ? titre.substring(0, 22) + "…" : titre);
                name.setStyle("-fx-font-size:12px;-fx-font-weight:700;-fx-text-fill:#1A2E26;");
                Label id = new Label("#" + e.getIdProjet());
                id.setStyle("-fx-font-size:10px;-fx-text-fill:#9CA3AF;");
                info.getChildren().addAll(name, id);

                HBox box = new HBox(8, avatar, info);
                box.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
                setGraphic(box);
            }
        });

        // SCORE FINAL
        colScoreFinal.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setText(null); setGraphic(null); return;
                }
                Evaluation e = (Evaluation) getTableRow().getItem();
                double score = e.getScoreGlobal();
                if (score == 0) { setText("—"); setStyle("-fx-text-fill:#9CA3AF;"); return; }
                Label lbl = new Label(String.format("%.2f", score));
                lbl.setStyle("-fx-font-size:14px;-fx-font-weight:800;-fx-text-fill:" + (score >= 7 ? "#2D5F3F" : score >= 5 ? "#F59E0B" : "#EF4444") + ";");
                Label sub = new Label("/10");
                sub.setStyle("-fx-font-size:9px;-fx-text-fill:#9CA3AF;");
                HBox box = new HBox(2, lbl, sub);
                box.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
                setGraphic(box);
                setText(null);
            }
        });

        // SCORE ML — show credibility %
        colScoreMl.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setText("—"); setStyle("-fx-text-fill:#9CA3AF;"); return;
                }
                Evaluation e = (Evaluation) getTableRow().getItem();
                Projet p = projetService.getById(e.getIdProjet());
                if (p == null || p.getScoreEsg() == null) { setText("—"); setStyle("-fx-text-fill:#9CA3AF;"); return; }
                Label score = new Label(String.valueOf(p.getScoreEsg()));
                score.setStyle("-fx-font-size:14px;-fx-font-weight:800;-fx-text-fill:#1A2E26;");
                Label cred = new Label("0,000%");
                cred.setStyle("-fx-font-size:9px;-fx-text-fill:#9CA3AF;-fx-padding:0 0 0 4;");
                HBox box = new HBox(2, score, cred);
                box.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
                setGraphic(box);
                setText(null);
            }
        });

        // DÉCISION badge
        colDecision.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setGraphic(null); return;
                }
                Evaluation e = (Evaluation) getTableRow().getItem();
                String dec = e.getDecision();
                if (dec == null) { setText("—"); setGraphic(null); return; }
                boolean approved = dec.toLowerCase().contains("approv");
                Label badge = new Label(approved ? "● APPROUVÉ" : "● REJETÉ");
                badge.setStyle("-fx-background-color:" + (approved ? "#D1FAE5" : "#FEE2E2")
                    + ";-fx-text-fill:" + (approved ? "#065F46" : "#991B1B")
                    + ";-fx-font-size:10px;-fx-font-weight:700;-fx-background-radius:4;-fx-padding:3 8;");
                setGraphic(badge);
                setText(null);
            }
        });

        // STATUT PROJET badge
        colStatut.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setGraphic(null); return;
                }
                Evaluation e = (Evaluation) getTableRow().getItem();
                Projet p = projetService.getById(e.getIdProjet());
                String statut = p != null ? p.getStatutEvaluation() : "—";
                boolean approved = "APPROVED".equals(statut);
                boolean draft    = "DRAFT".equals(statut);
                String bg    = approved ? "#D1FAE5" : draft ? "#F3F4F6" : "#FEF3C7";
                String color = approved ? "#065F46" : draft ? "#374151" : "#92400E";
                String icon  = approved ? "✓ " : draft ? "⬜ " : "⏳ ";
                Label badge = new Label(icon + statut);
                badge.setStyle("-fx-background-color:" + bg + ";-fx-text-fill:" + color
                    + ";-fx-font-size:10px;-fx-font-weight:700;-fx-background-radius:4;-fx-padding:3 8;");
                setGraphic(badge);
                setText(null);
            }
        });

        // ACTIONS: Modifier | Résumé | Réévaluer
        colActions.setCellFactory(col -> new TableCell<>() {
            private final Button btnModifier  = new Button("✏️ Modifier");
            private final Button btnResume    = new Button("👁 Résumé");
            private final Button btnReeval    = new Button("🔄 Réévaluer");

            {
                String base = "-fx-font-size:10px;-fx-font-weight:600;-fx-background-radius:5;-fx-padding:5 10;-fx-cursor:hand;";
                btnModifier.setStyle(base + "-fx-background-color:#F3F4F6;-fx-text-fill:#374151;-fx-border-color:#E5E7EB;-fx-border-width:1;");
                btnResume.setStyle(base + "-fx-background-color:#EFF6FF;-fx-text-fill:#1D4ED8;-fx-border-color:#BFDBFE;-fx-border-width:1;");
                btnReeval.setStyle(base + "-fx-background-color:#F0FDF4;-fx-text-fill:#15803D;-fx-border-color:#BBF7D0;-fx-border-width:1;");

                btnModifier.setOnAction(ev -> {
                    Evaluation e = getTableView().getItems().get(getIndex());
                    NavigationContext.getInstance().setCurrentProjectId(e.getIdProjet());
                    NavigationContext.getInstance().setCurrentEvaluationId(e.getIdEvaluation());
                    navigate("fxml/evaluation_form");
                });
                btnResume.setOnAction(ev -> {
                    Evaluation e = getTableView().getItems().get(getIndex());
                    NavigationContext.getInstance().setCurrentProjectId(e.getIdProjet());
                    NavigationContext.getInstance().setCurrentEvaluationId(e.getIdEvaluation());
                    navigate("fxml/evaluation_resume");
                });
                btnReeval.setOnAction(ev -> {
                    Evaluation e = getTableView().getItems().get(getIndex());
                    NavigationContext.getInstance().setCurrentProjectId(e.getIdProjet());
                    navigate("fxml/evaluation_queue");
                });
            }

            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) { setGraphic(null); return; }
                Evaluation e = getTableView().getItems().get(getIndex());
                boolean canReeval = e.getDecision() != null;
                HBox box = new HBox(4, btnModifier, btnResume);
                if (canReeval) box.getChildren().add(btnReeval);
                setGraphic(box);
            }
        });
    }

    private void navigate(String fxml) {
        try { org.GreenLedger.MainFX.setRoot(fxml); }
        catch (Exception e) { System.err.println("[EvalDashboard] Nav: " + e.getMessage()); }
    }
}