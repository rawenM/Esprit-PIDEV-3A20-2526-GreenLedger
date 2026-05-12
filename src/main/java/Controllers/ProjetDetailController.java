package Controllers;

import DataBase.MyConnection;
import Models.Projet;
import Services.EvaluationPdfService;
import Services.ProjetService;
import Utils.NavigationContext;
import Utils.SessionManager;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebView;
import javafx.stage.FileChooser;

import java.io.File;
import java.io.FileOutputStream;
import java.sql.*;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
/**
 * Project Detail page — matches the web app's /projets/{id} view.
 * Calculates: Score ESG, Avoided CO₂, ML Decision, Fraud Risk,
 *             Completeness, Recommendations, Financing section, Map.
 */
public class ProjetDetailController extends BaseController {

    // ── Sidebar ───────────────────────────────────────────────────────────
    @FXML private Label lblProfileName;
    @FXML private Label lblProfileType;

    // ── Hero ──────────────────────────────────────────────────────────────
    @FXML private Label lblProjetId;
    @FXML private Label lblTitre;
    @FXML private Label lblDescription;
    @FXML private Label lblStatutBadge;
    @FXML private Label lblEsgBadge;

    // ── Eval banner ───────────────────────────────────────────────────────
    @FXML private VBox evalBanner;

    // ── KPI cards ─────────────────────────────────────────────────────────
    @FXML private Label lblScoreEsg;
    @FXML private Label lblAvoidedCo2;
    @FXML private Label lblMlDecision;
    @FXML private Label lblFraudRisk;

    // ── Recommendations ───────────────────────────────────────────────────
    @FXML private VBox recoBox;
    @FXML private VBox recoList;

    // ── Completeness ──────────────────────────────────────────────────────
    @FXML private Label       lblCompleteness;
    @FXML private Label       lblCompletenessDetail;
    @FXML private ProgressBar progressCompleteness;

    // ── Financing ─────────────────────────────────────────────────────────
    @FXML private VBox  financingBox;
    @FXML private Label lblMontantDemande;
    @FXML private Label lblFundedAt;
    @FXML private Label lblEstimatedCredits;

    // ── Map ───────────────────────────────────────────────────────────────
    @FXML private WebView mapWebView;
    @FXML private Label   lblLocalisation;
    @FXML private Label   lblAqi;

    // ── Action buttons ────────────────────────────────────────────────────
    @FXML private Button btnModifier;
    @FXML private Button btnExportPdf;
    @FXML private Button btnSubmit;

    private final ProjetService        projetService = new ProjetService();
    private final EvaluationPdfService pdfService    = new EvaluationPdfService();

    private Projet projet;
<<<<<<< HEAD
    private Runnable onChanged = null;

    @FXML private Label lblId;
    @FXML private Label lblStatut;
    @FXML private TextField tfTitre;
    @FXML private TextField tfBudgetMontant;
    @FXML private ComboBox<String> cbBudgetDevise;
    @FXML private TextArea taBudgetRaison;
    @FXML private TextField tfScoreEsg;
    @FXML private TextField tfCompanyAddress;
    @FXML private TextField tfCompanyEmail;
    @FXML private TextField tfCompanyPhone;
    @FXML private TextArea taDescription;
    @FXML private Button btnSaveChanges;
    @FXML private Button btnCancelEdit;
    @FXML private Label lblDocsCount;
    @FXML private ListView<String> lvDocs;

    private List<ProjectDocument> docs = new ArrayList<>();
=======
>>>>>>> 697f7351277b2a6316572ab9077f2061a493ce44

    // ── Init ─────────────────────────────────────────────────────────────
    @FXML
    public void initialize() {
        super.initialize();
        applyProfile(lblProfileName, lblProfileType);

        int projectId = NavigationContext.getInstance().getCurrentProjectId();
        if (projectId > 0) {
            projet = projetService.getById(projectId);
            if (projet != null) render();
        }
    }

<<<<<<< HEAD
    public void setProjet(Projet p) {
        this.projet = p;
        render();
    }

    public void setOnChanged(Runnable r) {
        this.onChanged = r;
    }
    @FXML
    private void onAnnulerProjet() {
        if (projet == null) return;

        boolean isDraft = "DRAFT".equalsIgnoreCase(projet.getStatut());
        String msg = isDraft
                ? "Supprimer définitivement le DRAFT ?"
                : "Annuler le projet (statut CANCELLED) ?";

        if (!confirm(msg)) return;

        if (isDraft) service.delete(projet.getId());
        else service.cancel(projet.getId());

        if (onChanged != null) onChanged.run();
        closeWindow();
    }

    @FXML
    private void onModifier() {
        if (projet == null) return;

        btnSaveChanges.setVisible(true);
        btnCancelEdit.setVisible(true);

        tfScoreEsg.setDisable(true);

        boolean lockedTitreBudget = !"DRAFT".equalsIgnoreCase(projet.getStatut());
        tfTitre.setDisable(lockedTitreBudget);
        tfBudgetMontant.setDisable(lockedTitreBudget);
        if (cbBudgetDevise != null) cbBudgetDevise.setDisable(lockedTitreBudget);
        if (taBudgetRaison != null) taBudgetRaison.setDisable(lockedTitreBudget);

        taDescription.setDisable(false);
        tfCompanyAddress.setDisable(false);
        tfCompanyEmail.setDisable(false);
        tfCompanyPhone.setDisable(false);

        // ✅ docs restent read-only (on ne les active pas)
        if (lvDocs != null) lvDocs.setDisable(false);
    }

    @FXML
    private void onCancelEdit() {
        btnSaveChanges.setVisible(false);
        btnCancelEdit.setVisible(false);
        render();
    }

    @FXML
    private void onSaveChanges() {
        if (projet == null) return;

        boolean isDraft = "DRAFT".equalsIgnoreCase(projet.getStatut());

        projet.setDescription(taDescription.getText());
        projet.setCompanyAddress(emptyToNull(tfCompanyAddress.getText()));
        projet.setCompanyEmail(emptyToNull(tfCompanyEmail.getText()));
        projet.setCompanyPhone(emptyToNull(tfCompanyPhone.getText()));

        if (!isDraft) {
            service.updateDescriptionOnly(
                    projet.getId(),
                    projet.getDescription(),
                    projet.getCompanyAddress(),
                    projet.getCompanyEmail(),
                    projet.getCompanyPhone()
            );
            if (onChanged != null) onChanged.run();
            closeWindow();
            return;
        }

        String titre = safe(tfTitre.getText());
        if (titre.length() < 3) { error("Titre: min 3 caractères."); return; }

        double montant;
        try {
            montant = Double.parseDouble(safe(tfBudgetMontant.getText()));
            if (montant <= 0) throw new Exception();
        } catch (Exception e) { error("Budget invalide (>0)."); return; }

        String raison = safe(taBudgetRaison != null ? taBudgetRaison.getText() : null);
        if (raison.length() < 3) { error("Raison budget: min 3 caractères."); return; }

        String devise = (cbBudgetDevise != null && cbBudgetDevise.getValue() != null)
                ? cbBudgetDevise.getValue()
                : "TND";

        Budget b = projet.getBudgetObj();
        if (b == null) b = new Budget();
        b.setMontant(montant);
        b.setRaison(raison);
        b.setDevise(devise);
        b.setIdProjet(projet.getId());

        projet.setTitre(titre);
        projet.setBudget(b);

        service.update(projet);
        if (onChanged != null) onChanged.run();
        closeWindow();
    }

    // =========================
    // DOCS / IMAGES (READ-ONLY)
    // =========================
    @FXML
    private void onOpenSelectedDoc() {
        if (lvDocs == null || docs == null || docs.isEmpty()) return;

        int idx = lvDocs.getSelectionModel().getSelectedIndex();
        if (idx < 0 || idx >= docs.size()) {
            error("Veuillez sélectionner un fichier.");
            return;
        }

        ProjectDocument d = docs.get(idx);
        String pth = d.getFilePath();
        if (pth == null || pth.trim().isEmpty()) {
            error("Chemin du fichier introuvable.");
            return;
        }

        try {
            File f = resolveFile(pth);
            if (!f.exists()) {
                error("Fichier introuvable sur disque:\n" + f.getAbsolutePath());
                return;
            }

            // ✅ ouvre dans l'app par défaut (Edge/Chrome/Adobe/Photos)
            Desktop.getDesktop().open(f);

        } catch (Exception ex) {
            error("Impossible d'ouvrir le fichier : " + ex.getMessage());
        }
    }

    private void loadDocuments() {
        docs = new ArrayList<>();
        if (projet == null) return;

        try {
            docs = documentService.getByProject(projet.getId());
        } catch (Exception e) {
            System.out.println("loadDocuments error: " + e.getMessage());
            docs = new ArrayList<>();
        }

        if (lblDocsCount != null) {
            lblDocsCount.setText(docs.size() + " fichier(s)");
        }

        if (lvDocs != null) {
            List<String> items = new ArrayList<>();
            for (ProjectDocument d : docs) {
                String tag = d.isImage() ? "🖼" : "📄";
                items.add(tag + " " + safe(d.getFileName()));
            }
            lvDocs.setItems(FXCollections.observableArrayList(items));
        }
    }

    private File resolveFile(String filePathFromDb) {
        File f = new File(filePathFromDb);

        // Si le chemin en DB est relatif ("uploads/.."), on le résout depuis le dossier du projet
        if (!f.isAbsolute()) {
            Path abs = Paths.get(System.getProperty("user.dir")).resolve(filePathFromDb).normalize();
            f = abs.toFile();
        }
        return f;
    }


=======
    // ── Render ────────────────────────────────────────────────────────────
>>>>>>> 697f7351277b2a6316572ab9077f2061a493ce44
    private void render() {
        String statut = projet.getStatut() != null ? projet.getStatut() : "DRAFT";

        // Hero
        set(lblProjetId,    "PROJET #" + projet.getId());
        set(lblTitre,       projet.getTitre());
        set(lblDescription, projet.getDescription() != null ? projet.getDescription() : "");
        set(lblStatutBadge, statut);

        // Score ESG — stored 0-100, display /10
        double esgDisplay = computeEsgDisplay(projet.getScoreEsg());
        set(lblScoreEsg, String.format(Locale.ROOT, "%.1f", esgDisplay));
        set(lblEsgBadge, String.format(Locale.ROOT, "Score ESG: %.1f/10", esgDisplay));

        // Avoided CO₂
        double avoided = computeAvoidedCo2();
        set(lblAvoidedCo2, String.format(Locale.ROOT, "%.3f", avoided));

        // ML Decision
        String mlDecision = loadMlDecision();
        set(lblMlDecision, mlDecision != null ? mlDecision : "—");
        if (lblMlDecision != null) {
            String color = "APPROVED".equalsIgnoreCase(mlDecision) ? "#059669"
                : "REJECTED".equalsIgnoreCase(mlDecision) ? "#dc2626" : "#f59e0b";
            lblMlDecision.setStyle("-fx-font-size:20px;-fx-font-weight:800;-fx-text-fill:" + color + ";");
        }

        // Fraud risk
        double fraudPct = projet.getFraudRiskScore() != null ? projet.getFraudRiskScore() * 100 : 0;
        set(lblFraudRisk, String.format(Locale.ROOT, "%.0f%%", fraudPct));
        boolean isSuspect = Boolean.TRUE.equals(projet.getFraudFlag()) || fraudPct >= 55;
        if (lblFraudRisk != null) {
            String fc = isSuspect ? "#dc2626" : fraudPct >= 40 ? "#f59e0b" : "#059669";
            lblFraudRisk.setStyle("-fx-font-size:28px;-fx-font-weight:800;-fx-text-fill:" + fc + ";");
        }

        // Eval banner
        boolean approved = "APPROVED".equalsIgnoreCase(statut);
        if (evalBanner != null) { evalBanner.setVisible(approved); evalBanner.setManaged(approved); }

        // Recommendations
        String recos = loadRecommendations();
        if (recos != null && !recos.isBlank() && recoBox != null && recoList != null) {
            recoList.getChildren().clear();
            for (String r : recos.split("\\|")) {
                String t = r.trim();
                if (!t.isEmpty()) {
                    Label lbl = new Label("• " + t);
                    lbl.setWrapText(true);
                    lbl.setStyle("-fx-font-size:12px;-fx-text-fill:#92400e;");
                    recoList.getChildren().add(lbl);
                }
            }
            recoBox.setVisible(true); recoBox.setManaged(true);
        }

        // Completeness
        int filled = computeFilledFields();
        int pct    = Math.round(filled / 19.0f * 100);
        set(lblCompleteness,       pct + "%");
        set(lblCompletenessDetail, filled + " champs remplis sur 19");
        if (progressCompleteness != null) progressCompleteness.setProgress(pct / 100.0);

        // Financing — show only when statut_financement = 'FUNDED'
        String sf = projet.getStatutFinancement();
        boolean showFin = "FUNDED".equalsIgnoreCase(sf);
        if (financingBox != null) { financingBox.setVisible(showFin); financingBox.setManaged(showFin); }
        if (showFin) {
            Double md = projet.getMontantDemande();
            set(lblMontantDemande, md != null ? String.format(Locale.ROOT, "$%,.0f", md) : "—");

            // Read funded_at from projet table directly, fallback to financements.completed_at
            String fundedDate = loadFundedAt(projet.getId());
            set(lblFundedAt, fundedDate != null ? fundedDate : "—");

            // Estimated credits = round(esg² × 5)
            long estCredits = Math.round(esgDisplay * esgDisplay * 5);
            set(lblEstimatedCredits, estCredits + " crédits");
        }

        // Map — use entity lat/lng (now loaded from DB)
        set(lblLocalisation, projet.getLocalisation() != null ? projet.getLocalisation() : "—");
        Platform.runLater(this::loadMap);

        // Action buttons
        boolean isDraft = "DRAFT".equalsIgnoreCase(statut);
        if (btnSubmit != null) { btnSubmit.setVisible(isDraft); btnSubmit.setManaged(isDraft); }
    }

    // ── Calculations ──────────────────────────────────────────────────────

    private double computeEsgDisplay(Integer scoreEsg) {
        if (scoreEsg == null) return 0.0;
        return scoreEsg > 10 ? scoreEsg / 10.0 : scoreEsg;
    }

    private double computeAvoidedCo2() {
        // 1. Use stored value if available
        if (projet.getAvoidedTco2() != null && projet.getAvoidedTco2() > 0)
            return projet.getAvoidedTco2();

        // 2. Try carbon_metric table (Climatiq API result)
        double actualFromApi = loadCarbonMetricActual();

        double energie  = d(projet.getConsommationEnergie());
        double distance = d(projet.getDistanceTransport());
        double materiau = d(projet.getQuantiteMateriau());
        double dechets  = d(projet.getDechetsGeneres());
        double eau      = d(projet.getConsommationEau());

        // Baseline
        double baseline = energie * 0.0006 + distance * 0.00012 + materiau * 0.0015 + dechets * 0.0009;

        // Actual
        double actual;
        if (actualFromApi > 0) {
            actual = actualFromApi;
        } else {
            double scope1 = distance * 0.00008 + dechets * 0.00070;
            double scope2 = energie * 0.00045;
            double scope3 = materiau * 0.00120 + eau * 0.00010;
            actual = scope1 + scope2 + scope3;
            // Fallback to declared emissions
            if (actual == 0 && projet.getEmissionsEstimees() != null)
                actual = projet.getEmissionsEstimees();
        }

        return Math.max(0, baseline - actual);
    }

    private double loadCarbonMetricActual() {
        String sql = "SELECT total_tco2 FROM carbon_metric WHERE project_id=? ORDER BY id DESC LIMIT 1";
        try (Connection conn = MyConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, projet.getId());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) { double v = rs.getDouble(1); return rs.wasNull() ? 0 : v; }
            }
        } catch (SQLException e) { /* ignore */ }
        return 0;
    }

    private String loadMlDecision() {
        // Try ml_decision_snapshots first, then ml_predictions
        String[] sqls = {
            "SELECT decision FROM ml_decision_snapshots WHERE project_id=? ORDER BY id DESC LIMIT 1",
            "SELECT decision FROM ml_predictions WHERE project_id=? ORDER BY id DESC LIMIT 1"
        };
        for (String sql : sqls) {
            try (Connection conn = MyConnection.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, projet.getId());
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) { String v = rs.getString(1); if (v != null) return v; }
                }
            } catch (SQLException e) { /* try next */ }
        }
        return null;
    }

    private String loadRecommendations() {
        String[] sqls = {
            "SELECT recommendations FROM ml_decision_snapshots WHERE project_id=? ORDER BY id DESC LIMIT 1",
            "SELECT recommendations FROM ml_predictions WHERE project_id=? ORDER BY id DESC LIMIT 1"
        };
        for (String sql : sqls) {
            try (Connection conn = MyConnection.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, projet.getId());
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) { String v = rs.getString(1); if (v != null && !v.isBlank()) return v; }
                }
            } catch (SQLException e) { /* try next */ }
        }
        return null;
    }

    private int computeFilledFields() {
        int count = 0;
        // Général (2)
        if (notEmpty(projet.getTitre()))       count++;
        if (notEmpty(projet.getDescription())) count++;
        // Contact (5)
        if (notEmpty(projet.getCompanyAddress())) count++;
        if (notEmpty(projet.getCompanyEmail()))   count++;
        if (notEmpty(projet.getCompanyPhone()))   count++;
        if (notEmpty(projet.getLocalisation()))   count++;
        // Coordonnées GPS — latitude + longitude together = 1 slot
        if (projet.getLatitude() != null && projet.getLongitude() != null) count++;
        // Environnement (12)
        if (projet.getConsommationEnergie() != null) count++;
        if (notEmpty(projet.getUniteEnergie()))      count++;
        if (projet.getDistanceTransport() != null)   count++;
        if (notEmpty(projet.getTypeTransport()))     count++;
        if (notEmpty(projet.getTypeMateriau()))      count++;
        if (projet.getQuantiteMateriau() != null)    count++;
        if (projet.getConsommationEau() != null)     count++;
        if (projet.getDechetsGeneres() != null)      count++;
        if (projet.getEmissionsEstimees() != null)   count++;
        if (notEmpty(projet.getSourceEmissions()))   count++;
        if (notEmpty(projet.getSecteur()))           count++;
        if (notEmpty(projet.getTypeProjet()))        count++;
        return Math.min(count, 19);
    }

    private void loadMap() {
        if (mapWebView == null) return;
        Double lat = projet.getLatitude();
        Double lng = projet.getLongitude();
        String titre = projet.getTitre() != null ? projet.getTitre().replace("'", "\\'") : "Projet";

        // Show coordinates label
        if (lblLocalisation != null && lat != null && lng != null) {
            set(lblLocalisation, String.format(Locale.ROOT, "%.4f , %.4f", lat, lng));
        } else if (lblLocalisation != null) {
            set(lblLocalisation, projet.getLocalisation() != null ? projet.getLocalisation() : "—");
        }

        // Read Leaflet JS/CSS from classpath
        String leafletJs  = readResource("/js/leaflet.js");
        String leafletCss = readResource("/js/leaflet.css");

        double mapLat = lat != null ? lat : 36.7372;
        double mapLng = lng != null ? lng : 3.0865;
        int zoom = (lat != null && lng != null) ? 13 : 5;

        String markerJs = (lat != null && lng != null)
            ? "L.marker([" + lat + "," + lng + "]).addTo(map)" +
              ".bindPopup('<b>" + titre + "</b><br>" +
              (projet.getLocalisation() != null ? projet.getLocalisation() : "") +
              "').openPopup();"
            : "";

        String html = "<!DOCTYPE html><html><head><meta charset='utf-8'>" +
            "<style>" + leafletCss + "</style>" +
            "<style>*{margin:0;padding:0}html,body,#map{width:100%;height:100%;}</style>" +
            "<script>" + leafletJs + "</script>" +
            "</head><body><div id='map'></div><script>" +
            "var map=L.map('map').setView([" + mapLat + "," + mapLng + "]," + zoom + ");" +
            "L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png'," +
            "{attribution:'© OpenStreetMap'}).addTo(map);" +
            markerJs +
            "</script></body></html>";

        mapWebView.getEngine().loadContent(html);

        // Show AQI if available
        Integer aqi = projet.getAirQualityIndex();
        if (lblAqi != null) {
            if (aqi != null) {
                lblAqi.setText("Qualité de l'air (AQI): " + aqi);
                lblAqi.setVisible(true);
                lblAqi.setManaged(true);
            } else {
                lblAqi.setVisible(false);
                lblAqi.setManaged(false);
            }
        }
    }

    private String readResource(String path) {
        try (java.io.InputStream is = getClass().getResourceAsStream(path)) {
            if (is == null) return "";
            return new String(is.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
        } catch (Exception e) { return ""; }
    }

    // ── Actions ───────────────────────────────────────────────────────────

    @FXML private void onModifier() {
        if (projet == null) return;
        NavigationContext.getInstance().setCurrentProjectId(projet.getId());
        navigate("fxml/porteur_projet_form");
    }

    @FXML private void onSubmit() {
        if (projet == null) return;
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
            "Soumettre \"" + projet.getTitre() + "\" pour évaluation ?", ButtonType.YES, ButtonType.NO);
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.YES) {
                projetService.updateStatut(projet.getId(), "SUBMITTED");
                new Alert(Alert.AlertType.INFORMATION, "Projet soumis !", ButtonType.OK).showAndWait();
                navigate("fxml/porteur_projets");
            }
        });
    }

    @FXML private void onExportPdf() {
        if (projet == null) return;
        long evalId = loadLatestEvalId(projet.getId());
        if (evalId <= 0) {
            new Alert(Alert.AlertType.WARNING, "Aucune évaluation disponible.", ButtonType.OK).showAndWait();
            return;
        }
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Enregistrer le PDF");
        chooser.setInitialFileName("projet_" + projet.getId() + ".pdf");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF", "*.pdf"));
        File file = chooser.showSaveDialog(lblTitre.getScene().getWindow());
        if (file == null) return;
        long fEvalId = evalId;
        new Thread(() -> {
            try {
                byte[] pdf = pdfService.generatePdf(fEvalId);
                try (FileOutputStream fos = new FileOutputStream(file)) { fos.write(pdf); }
                Platform.runLater(() ->
                    new Alert(Alert.AlertType.INFORMATION, "PDF généré: " + file.getName(), ButtonType.OK).showAndWait());
            } catch (Exception ex) {
                Platform.runLater(() ->
                    new Alert(Alert.AlertType.ERROR, "Erreur PDF: " + ex.getMessage(), ButtonType.OK).showAndWait());
            }
        }).start();
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

    /** Read funded_at from projet table; fallback to financements.completed_at */
    private String loadFundedAt(int projectId) {
        // 1. Try entity field first (now loaded from DB via ProjetService)
        if (projet.getFundedAt() != null)
            return projet.getFundedAt().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));

        // 2. Try projet.funded_at directly from DB
        String sql1 = "SELECT funded_at FROM projet WHERE id=?";
        try (Connection conn = MyConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql1)) {
            ps.setInt(1, projectId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Timestamp ts = rs.getTimestamp("funded_at");
                    if (ts != null)
                        return ts.toLocalDateTime().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
                }
            }
        } catch (SQLException e) { /* try fallback */ }

        // 3. Fallback: financements.completed_at
        String sql2 = "SELECT completed_at FROM financements " +
                      "WHERE project_id=? AND statut='COMPLETED' ORDER BY id DESC LIMIT 1";
        try (Connection conn = MyConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql2)) {
            ps.setInt(1, projectId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Timestamp ts = rs.getTimestamp("completed_at");
                    if (ts != null)
                        return ts.toLocalDateTime().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
                }
            }
        } catch (SQLException e) { /* ignore */ }

        return null;
    }

    // ── Sidebar navigation ────────────────────────────────────────────────
    @FXML private void onDashboard()   { navigate("fxml/porteur_shell"); }
    @FXML private void onProjets()     { navigate("fxml/porteur_projets"); }
    @FXML private void onWallet()      { navigate("greenwallet"); }
    @FXML private void onFinancement() { navigate("financement"); }
    @FXML private void onMarketplace() { navigate("fxml/marketplace"); }
    @FXML private void onBlockchain()  { navigate("fxml/porteur_projets"); }
    @FXML private void onOutils()      { navigate("fxml/porteur_projets"); }
    @FXML private void onAssistant()   { navigate("fxml/porteur_assistant"); }
    @FXML private void onProfile()     { navigate("editProfile"); }
    @FXML private void onLogout() {
        SessionManager.getInstance().invalidate();
        navigate("fxml/login");
    }

    // ── Helpers ───────────────────────────────────────────────────────────
    private void set(Label lbl, String text) { if (lbl != null) lbl.setText(text); }
    private boolean notEmpty(String s) { return s != null && !s.trim().isEmpty(); }
    private double d(Double v) { return v != null ? v : 0.0; }

    private void navigate(String fxml) {
        try { org.GreenLedger.MainFX.setRoot(fxml); }
        catch (Exception e) { System.err.println("[ProjetDetail] Nav: " + e.getMessage()); }
    }
}
