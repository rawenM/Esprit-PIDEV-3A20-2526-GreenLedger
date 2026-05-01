package Controllers;

import DataBase.MyConnection;
import Models.SignatureData;
import Services.EvaluationPdfService;
import Services.EvaluationService;
import Services.SignatureService;
import Utils.NavigationContext;
import javafx.application.Platform;
import javafx.concurrent.Worker;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.FileChooser;

import java.io.File;
import java.io.FileOutputStream;
import java.sql.*;
import java.util.Locale;

/**
 * Résumé screen — shows evaluation results, signature status,
 * allows electronic signing and PDF download.
 *
 * Opened from EvaluationDashboardController "Résumé" button.
 */
public class EvaluationResumeController extends BaseController {

    // ── Sidebar ───────────────────────────────────────────────────────────
    @FXML private Label lblProfileName;
    @FXML private Label lblProfileType;

    // ── Top bar ───────────────────────────────────────────────────────────
    @FXML private Label  lblTitle;
    @FXML private Label  lblSubtitle;
    @FXML private Button btnSign;
    @FXML private Button btnDownloadPdf;
    @FXML private Button btnRelaunch;

    // ── Banners ───────────────────────────────────────────────────────────
    @FXML private HBox  boxUnsignedBanner;
    @FXML private HBox  boxSignedBanner;
    @FXML private Label lblSignedBy;
    @FXML private Label lblSignedHash;

    // ── KPI cards ─────────────────────────────────────────────────────────
    @FXML private Label lblScore;
    @FXML private Label lblDecision;
    @FXML private Label lblEmissions;
    @FXML private Label lblEmissionsSub;
    @FXML private Label lblFraud;
    @FXML private Label lblFraudSub;

    // ── Comment ───────────────────────────────────────────────────────────
    // (removed — criteria table and comment textarea removed from UI)

    // ── Signature display ─────────────────────────────────────────────────
    @FXML private VBox    boxSignatureDisplay;
    @FXML private WebView signatureWebView;
    @FXML private Label   lblSigHash;

    // ── Signature dialog ──────────────────────────────────────────────────
    @FXML private StackPane signatureOverlay;
    @FXML private TextField txtSignerName;
    @FXML private WebView   signatureCanvas;

    // ── Services ──────────────────────────────────────────────────────────
    private final SignatureService     sigService = new SignatureService();
    private final EvaluationPdfService pdfService = new EvaluationPdfService();

    // ── State ─────────────────────────────────────────────────────────────
    private int  evaluationId;
    private int  projectId;
    private SignatureData currentSignature;

    // ── Init ──────────────────────────────────────────────────────────────

    @FXML
    public void initialize() {
        super.initialize();

        evaluationId = NavigationContext.getInstance().getCurrentEvaluationId() != null
            ? NavigationContext.getInstance().getCurrentEvaluationId() : 0;
        projectId    = NavigationContext.getInstance().getCurrentProjectId() != null
            ? NavigationContext.getInstance().getCurrentProjectId() : 0;

        if (evaluationId == 0) {
            showError("Aucune évaluation sélectionnée.");
            return;
        }

        setupSignatureBannerOnly();
        loadData();
        initSignatureCanvas();
        applyProfile(lblProfileName, lblProfileType);
    }

    // ── Data loading ──────────────────────────────────────────────────────

    private void loadData() {
        // Load evaluation + project info
        String sql = "SELECT e.id_evaluation, e.id_projet, e.score_final, e.est_valide, " +
                     "e.observations_globales, e.date_evaluation, p.titre " +
                     "FROM evaluation e LEFT JOIN projet p ON p.id = e.id_projet " +
                     "WHERE e.id_evaluation = ?";
        try (Connection conn = MyConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, evaluationId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String titre = rs.getString("titre");
                    lblTitle.setText("Résumé — " + (titre != null ? titre : "Évaluation #" + evaluationId));

                    double sf = rs.getDouble("score_final");
                    boolean sfNull = rs.wasNull();
                    double score = sfNull ? 0 : sf;

                    // Fallback to ml_predictions
                    if (sfNull || score == 0) score = loadMlScore();

                    if (score > 0) {
                        lblScore.setText(String.format(Locale.ROOT, "%.2f", score));
                        lblScore.setStyle("-fx-font-size:28px;-fx-font-weight:800;-fx-text-fill:"
                            + (score >= 7 ? "#059669" : score >= 5 ? "#D97706" : "#DC2626") + ";");
                    }

                    boolean approved = rs.getBoolean("est_valide");
                    String dec = approved ? "APPROVED" : "REJECTED";
                    lblDecision.setText(dec);
                    lblDecision.setStyle("-fx-font-size:16px;-fx-font-weight:800;-fx-text-fill:"
                        + (approved ? "#059669" : "#DC2626") + ";");

                    String obs = rs.getString("observations_globales");
                    currentSignature = sigService.extractSignature(obs);

                    updateSignatureBanners();
                }
            }
        } catch (SQLException ex) {
            System.err.println("[Resume] loadData: " + ex.getMessage());
        }

        // Carbon metrics
        loadCarbonMetrics();

        // Fraud
        loadFraud();
    }

    private double loadMlScore() {
        String sql = "SELECT predicted_esg_score FROM ml_predictions " +
                     "WHERE project_id=? AND evaluation_id=? ORDER BY id DESC LIMIT 1";
        try (Connection conn = MyConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, projectId);
            ps.setInt(2, evaluationId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) { int v = rs.getInt(1); return rs.wasNull() ? 0 : v; }
            }
        } catch (SQLException ex) { /* ignore */ }
        return 0;
    }

    private void loadCarbonMetrics() {
        String sql = "SELECT scope1_tco2, scope2_tco2, scope3_tco2, total_tco2 " +
                     "FROM carbon_metric WHERE project_id=? AND evaluation_id=? ORDER BY id DESC LIMIT 1";
        try (Connection conn = MyConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, projectId);
            ps.setInt(2, evaluationId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    double total = rs.getDouble("total_tco2");
                    double s1 = rs.getDouble("scope1_tco2");
                    double s2 = rs.getDouble("scope2_tco2");
                    double s3 = rs.getDouble("scope3_tco2");
                    lblEmissions.setText(String.format(Locale.ROOT, "%.3f", total));
                    lblEmissionsSub.setText(String.format(Locale.ROOT,
                        "S1=%.3f / S2=%.3f / S3=%.3f tCO2e", s1, s2, s3));
                    return;
                }
            }
        } catch (SQLException ex) { /* ignore */ }
        // Fallback
        String fb = "SELECT emissions_estimees FROM projet WHERE id=?";
        try (Connection conn = MyConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(fb)) {
            ps.setInt(1, projectId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    double v = rs.getDouble(1);
                    if (!rs.wasNull()) lblEmissions.setText(String.format(Locale.ROOT, "%.3f", v));
                }
            }
        } catch (SQLException ex) { /* ignore */ }
    }

    private void loadFraud() {
        String sql = "SELECT fraud_risk_score, fraud_flag FROM projet WHERE id=?";
        try (Connection conn = MyConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, projectId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    double risk = rs.getDouble("fraud_risk_score");
                    boolean flag = rs.getBoolean("fraud_flag");
                    String level = risk >= 0.65 ? "ÉLEVÉ" : risk >= 0.35 ? "MOYEN" : "FAIBLE";
                    String color = risk >= 0.65 ? "#DC2626" : risk >= 0.35 ? "#D97706" : "#059669";
                    lblFraud.setText(String.format(Locale.ROOT, "%.4f", risk));
                    lblFraud.setStyle("-fx-font-size:16px;-fx-font-weight:800;-fx-text-fill:" + color + ";");
                    lblFraudSub.setText(level + (flag ? " — Suspect" : " — OK"));
                }
            }
        } catch (SQLException ex) { /* ignore */ }
    }

    // ── Table setup (removed) ─────────────────────────────────────────────
    private void setupSignatureBannerOnly() {
        // placeholder — table removed from UI
    }

    // ── Signature banners ─────────────────────────────────────────────────

    private void updateSignatureBanners() {
        if (currentSignature != null && currentSignature.isSigned()) {
            boxUnsignedBanner.setVisible(false);
            boxUnsignedBanner.setManaged(false);
            boxSignedBanner.setVisible(true);
            boxSignedBanner.setManaged(true);
            lblSignedBy.setText("Signé par " + currentSignature.getSignedByName()
                + "  le  " + currentSignature.getSignedAt());
            String hash = currentSignature.getSignatureHash();
            lblSignedHash.setText("Hash: " + (hash != null ? hash.substring(0, Math.min(32, hash.length())) + "..." : "—"));
            btnSign.setText("✍ Re-signer");

            // Show signature image
            showSignatureImage(currentSignature.getSignatureImage());
        } else {
            boxUnsignedBanner.setVisible(true);
            boxUnsignedBanner.setManaged(true);
            boxSignedBanner.setVisible(false);
            boxSignedBanner.setManaged(false);
            boxSignatureDisplay.setVisible(false);
            boxSignatureDisplay.setManaged(false);
        }
    }

    private void showSignatureImage(String dataUrl) {
        if (dataUrl == null || dataUrl.isBlank()) return;
        boxSignatureDisplay.setVisible(true);
        boxSignatureDisplay.setManaged(true);
        if (currentSignature.getSignatureHash() != null)
            lblSigHash.setText("Hash SHA-256: " + currentSignature.getSignatureHash());

        String html = "<!DOCTYPE html><html><body style='margin:0;background:#F9FAFB;'>"
            + "<img src='" + dataUrl + "' style='max-width:100%;max-height:110px;display:block;margin:4px auto;'/>"
            + "</body></html>";
        signatureWebView.getEngine().loadContent(html);
    }

    // ── Signature canvas ──────────────────────────────────────────────────

    private void initSignatureCanvas() {
        WebEngine engine = signatureCanvas.getEngine();
        engine.setJavaScriptEnabled(true);
        engine.loadContent(buildSignatureCanvasHtml());
    }

    private String buildSignatureCanvasHtml() {
        return "<!DOCTYPE html><html><head><style>"
            + "body{margin:0;background:#fff;overflow:hidden;}"
            + "canvas{display:block;cursor:crosshair;border:none;}"
            + "</style></head><body>"
            + "<canvas id='c' width='480' height='150'></canvas>"
            + "<script>"
            + "var c=document.getElementById('c'),ctx=c.getContext('2d');"
            + "ctx.strokeStyle='#1A2E26';ctx.lineWidth=2.5;ctx.lineCap='round';ctx.lineJoin='round';"
            + "var drawing=false,lx=0,ly=0;"
            + "c.addEventListener('mousedown',function(e){drawing=true;lx=e.offsetX;ly=e.offsetY;});"
            + "c.addEventListener('mousemove',function(e){if(!drawing)return;"
            + "ctx.beginPath();ctx.moveTo(lx,ly);ctx.lineTo(e.offsetX,e.offsetY);ctx.stroke();"
            + "lx=e.offsetX;ly=e.offsetY;});"
            + "c.addEventListener('mouseup',function(){drawing=false;});"
            + "c.addEventListener('mouseleave',function(){drawing=false;});"
            + "function clearCanvas(){ctx.clearRect(0,0,c.width,c.height);}"
            + "function getDataUrl(){return c.toDataURL('image/png');}"
            + "function isEmpty(){"
            + "var d=ctx.getImageData(0,0,c.width,c.height).data;"
            + "for(var i=3;i<d.length;i+=4){if(d[i]>0)return false;}return true;}"
            + "</script></body></html>";
    }

    // ── Actions ───────────────────────────────────────────────────────────

    @FXML private void onBack()     { navigateBack(); }
    @FXML private void onDashboard()   { navigate("fxml/evaluation_dashboard"); }
    @FXML private void onEditProfile() { navigate("editProfile"); }
    @FXML private void onLogout() {
        Utils.SessionManager.getInstance().invalidate();
        navigate("fxml/login");
    }
    @FXML private void onRelaunch() {
        NavigationContext.getInstance().setCurrentProjectId(projectId);
        NavigationContext.getInstance().setCurrentEvaluationId(evaluationId);
        navigate("fxml/evaluation_form");
    }

    @FXML
    private void onSign() {
        txtSignerName.clear();
        signatureCanvas.getEngine().executeScript("clearCanvas()");
        signatureOverlay.setVisible(true);
        signatureOverlay.setManaged(true);
    }

    @FXML
    private void onCancelSign() {
        signatureOverlay.setVisible(false);
        signatureOverlay.setManaged(false);
    }

    @FXML
    private void onClearSignature() {
        signatureCanvas.getEngine().executeScript("clearCanvas()");
    }

    @FXML
    private void onConfirmSign() {
        String signerName = txtSignerName.getText().trim();
        if (signerName.isBlank()) {
            new Alert(Alert.AlertType.WARNING, "Veuillez saisir le nom du signataire.", ButtonType.OK).showAndWait();
            return;
        }

        // Check canvas is not empty
        Object empty = signatureCanvas.getEngine().executeScript("isEmpty()");
        if (Boolean.TRUE.equals(empty)) {
            new Alert(Alert.AlertType.WARNING, "Veuillez dessiner votre signature.", ButtonType.OK).showAndWait();
            return;
        }

        // Get base64 PNG from canvas
        String dataUrl = (String) signatureCanvas.getEngine().executeScript("getDataUrl()");
        if (dataUrl == null || !dataUrl.startsWith("data:image/")) {
            new Alert(Alert.AlertType.ERROR, "Impossible de récupérer la signature.", ButtonType.OK).showAndWait();
            return;
        }

        // Close dialog
        signatureOverlay.setVisible(false);
        signatureOverlay.setManaged(false);

        // Sign on background thread
        new Thread(() -> {
            try {
                sigService.signEvaluation(evaluationId, signerName, dataUrl);
                // Reload signature data
                String obs = loadObservations();
                currentSignature = sigService.extractSignature(obs);
                Platform.runLater(() -> {
                    updateSignatureBanners();
                    new Alert(Alert.AlertType.INFORMATION,
                        "Signature enregistrée avec succès !", ButtonType.OK).showAndWait();
                });
            } catch (Exception ex) {
                Platform.runLater(() ->
                    new Alert(Alert.AlertType.ERROR,
                        "Erreur lors de la signature: " + ex.getMessage(), ButtonType.OK).showAndWait());
            }
        }, "sign-thread").start();
    }

    @FXML
    private void onDownloadPdf() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Enregistrer le rapport PDF");
        chooser.setInitialFileName("evaluation_" + evaluationId + ".pdf");
        chooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));
        File file = chooser.showSaveDialog(btnDownloadPdf.getScene().getWindow());
        if (file == null) return;

        btnDownloadPdf.setDisable(true);
        btnDownloadPdf.setText("⏳ Génération...");

        new Thread(() -> {
            try {
                byte[] pdf = pdfService.generatePdf(evaluationId);
                try (FileOutputStream fos = new FileOutputStream(file)) {
                    fos.write(pdf);
                }
                Platform.runLater(() -> {
                    btnDownloadPdf.setDisable(false);
                    btnDownloadPdf.setText("⬇ Télécharger PDF");
                    new Alert(Alert.AlertType.INFORMATION,
                        "PDF généré avec succès !\n" + file.getAbsolutePath(),
                        ButtonType.OK).showAndWait();
                });
            } catch (Exception ex) {
                Platform.runLater(() -> {
                    btnDownloadPdf.setDisable(false);
                    btnDownloadPdf.setText("⬇ Télécharger PDF");
                    new Alert(Alert.AlertType.ERROR,
                        "Erreur PDF: " + ex.getMessage(), ButtonType.OK).showAndWait();
                });
            }
        }, "pdf-thread").start();
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private String loadObservations() {
        String sql = "SELECT observations_globales FROM evaluation WHERE id_evaluation=?";
        try (Connection conn = MyConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, evaluationId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getString(1);
            }
        } catch (SQLException ex) { /* ignore */ }
        return "";
    }

    private void navigate(String fxml) {
        try { org.GreenLedger.MainFX.setRoot(fxml); }
        catch (Exception e) { System.err.println("[Resume] Nav: " + e.getMessage()); }
    }

    private void showError(String msg) {
        new Alert(Alert.AlertType.ERROR, msg, ButtonType.OK).showAndWait();
    }
}
