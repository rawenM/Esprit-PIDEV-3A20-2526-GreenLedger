package Controllers;

import Models.Evaluation;
import Models.Projet;
import Services.EvaluationService;
import Services.ExpertWorkflowService;
import Services.ProjetService;
import Utils.NavigationContext;
import Utils.SessionManager;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Expert Shell Controller — master layout with sidebar + evaluation queue.
 * Login redirect for EXPERT_CARBONE.
 */
public class ExpertShellController extends BaseController {

    private static final int PAGE_SIZE = 10;

    // ── Sidebar ──────────────────────────────────────────────────────────────
    @FXML private Label  lblProfileName;
    @FXML private Label  lblProfileType;
    @FXML private Label  lblPageTitle;
    @FXML private Label  lblTopBadge;

    // ── KPI cards ────────────────────────────────────────────────────────────
    @FXML private Label statPending;
    @FXML private Label statOnPage;
    @FXML private Label statFraud;
    @FXML private Label statToVerify;
    @FXML private Label lblPageCount;
    @FXML private Label lblPageInfo;

    // ── Search + pagination ──────────────────────────────────────────────────
    @FXML private TextField txtSearch;
    @FXML private Button    btnPrev;
    @FXML private Button    btnNext;

    // ── Table ────────────────────────────────────────────────────────────────
    @FXML private TableView<Projet>          tableQueue;
    @FXML private TableColumn<Projet,String> colNom;
    @FXML private TableColumn<Projet,String> colDescription;
    @FXML private TableColumn<Projet,String> colStatut;
    @FXML private TableColumn<Projet,String> colFraud;
    @FXML private TableColumn<Projet,String> colMlDecision;
    @FXML private TableColumn<Projet,String> colAction;

    private final ProjetService         projetService    = new ProjetService();
    private final EvaluationService     evalService      = new EvaluationService();
    private final ExpertWorkflowService workflowService  = new ExpertWorkflowService();

    private List<Projet> allSubmitted = List.of();
    private int currentPage = 1;

    // ── Init ─────────────────────────────────────────────────────────────────

    @FXML
    public void initialize() {
        super.initialize();
        applyProfile(lblProfileName, lblProfileType);
        setupTable();
        loadQueue();

        if (txtSearch != null) {
            txtSearch.textProperty().addListener((o, a, b) -> { currentPage = 1; renderPage(); });
        }
    }

    // ── Sidebar navigation ───────────────────────────────────────────────────

    @FXML private void onQueue()       { /* already here */ }
    @FXML private void onDashboard()   { navigate("fxml/evaluation_dashboard"); }
    @FXML private void onFraud()       { navigate("fxml/expert_carbon_dashboard"); }
    @FXML private void onEvalNotifs()  { /* show notifications */ }
    @FXML private void onEditProfile() { navigate("editProfile"); }

    @FXML
    private void onBack() {
        String prev = NavigationContext.getInstance().getPreviousPage();
        if (prev != null && !prev.isEmpty() && !prev.equals(NavigationContext.getInstance().getCurrentPage())) {
            navigate(prev);
        } else {
            navigate("fxml/login");
        }
    }

    @FXML
    private void onLogout() {
        SessionManager.getInstance().invalidate();
        navigate("fxml/login");
    }

    @FXML private void onPrevPage() { if (currentPage > 1) { currentPage--; renderPage(); } }
    @FXML private void onNextPage() {
        int total = totalPages();
        if (currentPage < total) { currentPage++; renderPage(); }
    }

    // ── Data ─────────────────────────────────────────────────────────────────

    private void loadQueue() {
        List<Projet> all = projetService.afficher();

        // KPIs
        long pending  = all.stream().filter(p -> "SUBMITTED".equals(p.getStatutEvaluation())).count();
        long fraud    = all.stream().filter(p -> Boolean.TRUE.equals(p.getFraudFlag())).count();
        long toVerify = all.stream().filter(p -> {
            Double r = p.getFraudRiskScore();
            return r != null && r >= 0.35 && r < 0.65;
        }).count();

        if (statPending  != null) statPending.setText(String.valueOf(pending));
        if (statFraud    != null) statFraud.setText(String.valueOf(fraud));
        if (statToVerify != null) statToVerify.setText(String.valueOf(toVerify));
        if (lblTopBadge  != null) lblTopBadge.setText(String.valueOf(pending));

        allSubmitted = all.stream()
            .filter(p -> "SUBMITTED".equals(p.getStatutEvaluation()))
            .collect(Collectors.toList());

        currentPage = 1;
        renderPage();
    }

    private void renderPage() {
        String kw = txtSearch != null ? txtSearch.getText() : "";
        List<Projet> filtered = allSubmitted;
        if (kw != null && !kw.isBlank()) {
            String k = kw.toLowerCase();
            filtered = allSubmitted.stream()
                .filter(p -> (p.getTitre() != null && p.getTitre().toLowerCase().contains(k))
                          || (p.getDescription() != null && p.getDescription().toLowerCase().contains(k)))
                .collect(Collectors.toList());
        }

        int total = Math.max(1, (int) Math.ceil((double) filtered.size() / PAGE_SIZE));
        currentPage = Math.max(1, Math.min(currentPage, total));

        int from = (currentPage - 1) * PAGE_SIZE;
        int to   = Math.min(from + PAGE_SIZE, filtered.size());
        List<Projet> page = filtered.subList(from, to);

        tableQueue.setItems(FXCollections.observableArrayList(page));

        if (statOnPage  != null) statOnPage.setText(String.valueOf(page.size()));
        if (lblPageCount != null) lblPageCount.setText(filtered.size() + " présentés");
        if (lblPageInfo  != null) lblPageInfo.setText("Page " + currentPage + " / " + total + " (" + filtered.size() + " projets)");
        if (btnPrev != null) btnPrev.setDisable(currentPage <= 1);
        if (btnNext != null) btnNext.setDisable(currentPage >= total);
    }

    private int totalPages() {
        return Math.max(1, (int) Math.ceil((double) allSubmitted.size() / PAGE_SIZE));
    }

    // ── Table setup ──────────────────────────────────────────────────────────

    private void setupTable() {
        // PROJET — name + id sub
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
                Label id = new Label("ID Évaluation " + p.getId());
                id.setStyle("-fx-font-size:9px;-fx-text-fill:#9CA3AF;");
                VBox box = new VBox(2, name, id);
                setGraphic(box); setText(null);
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
                String t = d.length() > 55 ? d.substring(0, 55) + "…" : d;
                setText(t);
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
                Label badge = new Label("SUBMITTED");
                badge.setStyle("-fx-background-color:#DBEAFE;-fx-text-fill:#1D4ED8;-fx-font-size:9px;-fx-font-weight:700;-fx-background-radius:4;-fx-padding:3 8;");
                setGraphic(badge); setText(null);
            }
        });

        // FRAUDE badge
        colFraud.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setGraphic(null); return;
                }
                Projet p = (Projet) getTableRow().getItem();
                Boolean flag = p.getFraudFlag();
                Double  risk = p.getFraudRiskScore();

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
                setGraphic(badge); setText(null);
            }
        });

        // SUGGEST ML
        colMlDecision.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setText("—"); setStyle("-fx-text-fill:#9CA3AF;"); setGraphic(null); return;
                }
                Projet p = (Projet) getTableRow().getItem();
                Integer esg = p.getScoreEsg();
                if (esg == null) { setText("—"); setStyle("-fx-text-fill:#9CA3AF;"); setGraphic(null); return; }
                boolean approved = esg >= 7;
                Label badge = new Label(approved ? "APPROVED" : "REJECTED");
                badge.setStyle("-fx-background-color:" + (approved ? "#D1FAE5" : "#FEE2E2")
                    + ";-fx-text-fill:" + (approved ? "#065F46" : "#991B1B")
                    + ";-fx-font-size:9px;-fx-font-weight:700;-fx-background-radius:4;-fx-padding:2 6;");
                Label cred = new Label("0,000%");
                cred.setStyle("-fx-font-size:9px;-fx-text-fill:#9CA3AF;");
                VBox box = new VBox(2, badge, cred);
                setGraphic(box); setText(null);
            }
        });

        // ACTION — "Évaluer" button
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

    // ── Start evaluation ─────────────────────────────────────────────────────

    private void startEvaluation(Projet projet) {
        if (!"SUBMITTED".equals(projet.getStatutEvaluation())) {
            new Alert(Alert.AlertType.WARNING, "Le projet doit être SUBMITTED.", ButtonType.OK).showAndWait();
            return;
        }
        try {
            List<Evaluation> existing = evalService.afficherParProjet(projet.getId());
            int evalId;
            if (!existing.isEmpty()) {
                evalId = existing.get(0).getIdEvaluation();
            } else {
                projetService.updateStatut(projet.getId(), "IN_PROGRESS");
                Evaluation eval = new Evaluation();
                eval.setIdProjet(projet.getId());
                evalService.ajouter(eval);
                List<Evaluation> created = evalService.afficherParProjet(projet.getId());
                if (created.isEmpty()) {
                    new Alert(Alert.AlertType.ERROR, "Impossible de créer l'évaluation.", ButtonType.OK).showAndWait();
                    return;
                }
                evalId = created.get(0).getIdEvaluation();
            }
            NavigationContext.getInstance().setCurrentProjectId(projet.getId());
            NavigationContext.getInstance().setCurrentEvaluationId(evalId);
            navigate("fxml/evaluation_form");
        } catch (Exception e) {
            new Alert(Alert.AlertType.ERROR, "Erreur: " + e.getMessage(), ButtonType.OK).showAndWait();
        }
    }

    private void navigate(String fxml) {
        try { org.GreenLedger.MainFX.setRoot(fxml); }
        catch (Exception e) { System.err.println("[ExpertShell] Nav: " + e.getMessage()); }
    }
}
