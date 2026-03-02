package Controllers;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.application.Platform;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.FlowPane;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import Models.CritereReference;
import Models.Evaluation;
import Models.EvaluationResult;
import Models.Projet;
import Services.CritereImpactService;
import Services.EvaluationService;
import Services.ProjetService;
import Services.ProjectEsgService;
import Services.DynamicPdfService;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.GreenLedger.MainFX;
import Utils.SessionManager;
import Models.TypeUtilisateur;
import Models.User;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import DataBase.MyConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import javafx.animation.PauseTransition;

public class CarbonAuditController extends BaseController {

    private static Projet selectedProjet;
    private static Integer lastSelectedEvaluationId;

    public static void setSelectedProjet(Projet projet) {
        selectedProjet = projet;
    }

    public static void setLastSelectedEvaluationId(Integer evaluationId) {
        lastSelectedEvaluationId = evaluationId;
    }

    @FXML private Button btnGestionProjets;

    @FXML private Button btnGestionEvaluations;

    @FXML private Button btnSettings;

    @FXML private TableView<Evaluation> tableAudits;
    @FXML private TableView<Projet> tableProjets;
    @FXML private TableView<CritereReference> tableCriteres;

    @FXML private TableColumn<Evaluation, Timestamp> colDate;
    @FXML private TableColumn<Evaluation, String> colDecision;
    @FXML private TableColumn<Evaluation, String> colProjetNom;
    @FXML private TableColumn<Evaluation, String> colObservations;
    @FXML private TableColumn<Evaluation, String> colScore;
    @FXML private TableColumn<Evaluation, Void> colAction;

    @FXML private TableColumn<Projet, String> colProjetTitre;
    @FXML private TableColumn<Projet, String> colProjetDescription;
    @FXML private TableColumn<Projet, Number> colProjetBudget;
    @FXML private TableColumn<Projet, Number> colProjetScore;
    @FXML private TableColumn<Projet, String> colProjetStatut;

    @FXML private TableColumn<CritereReference, String> colCritereNom;
    @FXML private TableColumn<CritereReference, Number> colCritereNote;
    @FXML private TableColumn<CritereReference, String> colCritereCommentaire;

    @FXML private TextArea txtObservations;
    @FXML private TextField txtIdProjet;
    @FXML private RadioButton chkDecisionApproved;
    @FXML private RadioButton chkDecisionRejected;
    @FXML private Label lblSelectedProjectName;

    @FXML private FlowPane flowCriteres;
    @FXML private TextField txtNomCritere;
    @FXML private TextArea txtCommentaireCritere;
    @FXML private TextField txtNote;
    @FXML private javafx.scene.layout.VBox criteriaFieldsBox;

    @FXML private Label lblProjetsAudit;
    @FXML private Label lblProjetsEvalues;
    @FXML private Label lblCriteresImpact;

    @FXML private TextField txtScoreFinal;

    @FXML private CheckBox chkAddCritere;
    @FXML private javafx.scene.layout.VBox boxAddCritere;

    @FXML private Label lblProfileName;
    @FXML private Label lblProfileType;

    @FXML private Label lblAISuggestion;
    @FXML private Button btnAISuggest;
    @FXML private Button btnScorePreview;
    @FXML private Button btnWhatIf;
    @FXML private TextArea txtAIInsights;
    @FXML private Button btnExportPdf;

    @FXML private javafx.scene.layout.HBox evaluationBanner;
    @FXML private Label lblEvaluationBanner;

    // Signature UI
    @FXML private javafx.scene.canvas.Canvas signatureCanvas;
    private javafx.scene.canvas.GraphicsContext sigGc;
    private double sigLastX, sigLastY;
    private boolean signatureDrawn = false;

    // ESG UI
    @FXML private Button btnCalcEsg;
    @FXML private Button btnSaveEsg;
    @FXML private Label lblEsgScore;
    @FXML private TextArea txtEsgDetails;

    // ML UI
    @FXML private Button btnMlPredict;
    @FXML private TextArea txtMlRecommendation;
    @FXML private Label lblMlStatus;
    @FXML private Label lblDecisionValue;
    @FXML private javafx.scene.control.ListView<String> listMlFactors;
    @FXML private javafx.scene.chart.LineChart<Number, Number> mlFactorsChart;

    private final EvaluationService evaluationService = new EvaluationService();
    private final ProjetService projetService = new ProjetService();
    private final CritereImpactService critereImpactService = new CritereImpactService();
    private final ProjectEsgService projectEsgService = new ProjectEsgService();
    private final Services.AdvancedEvaluationFacade advancedFacade = new Services.AdvancedEvaluationFacade();
    private final Services.CarbonReportService carbonReportService = new Services.CarbonReportService();

    private final ObservableList<CritereReference> referenceCriteres = FXCollections.observableArrayList();

    private Integer selectedEvaluationId;

    private final ToggleGroup decisionGroup = new ToggleGroup();

    private String lastMlDecision;
    private Double lastMlConfidence;

    private static final java.util.Map<Integer, String> mlDecisionByProject = new java.util.concurrent.ConcurrentHashMap<>();

    public static Projet getSelectedProjet() {
        return selectedProjet;
    }

    public static void storeMlDecision(int projectId, String decision) {
        if (decision == null) return;
        mlDecisionByProject.put(projectId, decision);
    }

    private static String getStoredMlDecision(int projectId) {
        return mlDecisionByProject.get(projectId);
    }

    @FXML
    public void initialize() {
        super.initialize(); // Enable theme switching

        applyProfile(lblProfileName, lblProfileType);

        setActiveNav(btnGestionEvaluations);
        if (btnGestionEvaluations != null) {
            Platform.runLater(() -> btnGestionEvaluations.requestFocus());
        }

        critereImpactService.ensureDefaultReferences();
        enforceSingleDecision();
        setupStaticInputConstraints();

        // Initialiser les gestionnaires des boutons de navigation
        if (btnGestionProjets != null) {
            btnGestionProjets.setOnAction(event -> showGestionProjets());
        }
        if (btnGestionEvaluations != null) {
            btnGestionEvaluations.setOnAction(event -> showGestionEvaluations());
        }
        if (btnSettings != null) {
            btnSettings.setOnAction(event -> showSettings());
        }

        // Appliquer formatters / contrôles de saisie pour les champs statiques du formulaire de critères
        setupStaticInputConstraints();

        if (tableAudits != null) {
            tableAudits.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_SUBSEQUENT_COLUMNS);
            tableAudits.setFixedCellSize(36);
            tableAudits.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
            tableAudits.setFocusTraversable(true);
            tableAudits.addEventFilter(javafx.scene.input.MouseEvent.MOUSE_PRESSED, event -> {
                javafx.scene.Node node = event.getPickResult().getIntersectedNode();
                while (node != null && !(node instanceof javafx.scene.control.TableRow)) {
                    node = node.getParent();
                }
                if (node instanceof javafx.scene.control.TableRow) {
                    javafx.scene.control.TableRow<?> row = (javafx.scene.control.TableRow<?>) node;
                    if (!row.isEmpty()) {
                        tableAudits.getSelectionModel().select(row.getIndex());
                        tableAudits.requestFocus();
                        applyEvaluationToForm((Evaluation) row.getItem());
                    }
                }
            });
            tableAudits.setRowFactory(tv -> {
                TableRow<Evaluation> row = new TableRow<>();
                row.setOnMouseClicked(event -> {
                    if (!row.isEmpty() && event.getButton() == javafx.scene.input.MouseButton.PRIMARY) {
                        tableAudits.getSelectionModel().select(row.getIndex());
                        applyEvaluationToForm(row.getItem());
                    }
                });
                return row;
            });
        }
        if (tableProjets != null) {
            tableProjets.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_SUBSEQUENT_COLUMNS);
            tableProjets.setFixedCellSize(36);
        }
        if (tableCriteres != null) {
            tableCriteres.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_SUBSEQUENT_COLUMNS);
            tableCriteres.setFixedCellSize(34);
            tableCriteres.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        }

        if (colDate != null) {
            colDate.setCellValueFactory(new PropertyValueFactory<>("dateEvaluation"));
            colDecision.setCellValueFactory(new PropertyValueFactory<>("decision"));
            colProjetNom.setCellValueFactory(new PropertyValueFactory<>("titreProjet"));
            colObservations.setCellValueFactory(new PropertyValueFactory<>("observations"));
            if (colScore != null) {
                colScore.setCellValueFactory(cellData ->
                        new javafx.beans.property.SimpleStringProperty(formatScore(cellData.getValue().getScoreGlobal())));
            }
        }
        if (colAction != null) {
            colAction.setSortable(false);
            colAction.setCellFactory(col -> new TableCell<Evaluation, Void>() {
                private final Button actionButton = new Button("Update Status");
                {
                    actionButton.getStyleClass().add("btn-secondary");
                    actionButton.setOnAction(event -> {
                        Evaluation evaluation = getTableView().getItems().get(getIndex());
                        applyDecisionToStatus(evaluation);
                    });
                }

                @Override
                protected void updateItem(Void item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty) {
                        setGraphic(null);
                    } else {
                        setGraphic(actionButton);
                    }
                }
            });
        }
        if (colProjetTitre != null) {
            colProjetTitre.setCellValueFactory(new PropertyValueFactory<>("titre"));
            colProjetDescription.setCellValueFactory(new PropertyValueFactory<>("description"));
            colProjetBudget.setCellValueFactory(new PropertyValueFactory<>("budget"));
            colProjetScore.setCellValueFactory(new PropertyValueFactory<>("scoreEsg"));
            colProjetStatut.setCellValueFactory(new PropertyValueFactory<>("statutEvaluation"));
        }
        if (colCritereNom != null) {
            colCritereNom.setCellValueFactory(new PropertyValueFactory<>("nomCritere"));
            colCritereNote.setCellValueFactory(new PropertyValueFactory<>("poids"));
            colCritereCommentaire.setCellValueFactory(new PropertyValueFactory<>("description"));
        }

        // Wrap long text so it remains readable within the available width.
        applyWrapping(colObservations);
        applyWrapping(colProjetNom);
        applyWrapping(colProjetDescription);
        applyWrapping(colCritereCommentaire);

        refreshProjets();
        refreshEvaluations();
        refreshCriteres();
        rebuildCriteriaFields(null);
        if (tableAudits != null) {
            tableAudits.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, selected) -> {
                if (selected != null) {
                    applyEvaluationToForm(selected);
                }
            });
        }
        if (tableCriteres != null) {
            tableCriteres.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, selected) -> {
                if (selected != null) {
                    txtNomCritere.setText(selected.getNomCritere());
                    txtNote.setText(String.valueOf(selected.getPoids()));
                    txtCommentaireCritere.setText(selected.getDescription());
                }
            });
        }
        if (tableProjets != null) {
            tableProjets.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, selected) -> {
                if (selected != null) {
                    selectedProjet = selected;
                    if (txtIdProjet != null) {
                        txtIdProjet.setText(String.valueOf(selected.getId()));
                    }
                    if (lblSelectedProjectName != null) {
                        lblSelectedProjectName.setText(selected.getTitre());
                    }
                }
            });
        }

        selectProjetIfSet();

        if (lastSelectedEvaluationId != null) {
            loadEvaluationById(lastSelectedEvaluationId);
        }

        if (boxAddCritere != null) {
            boxAddCritere.setVisible(true);
            boxAddCritere.setManaged(true);
        }

        // Init signature pad
        if (signatureCanvas != null) {
            sigGc = signatureCanvas.getGraphicsContext2D();
            sigGc.setLineWidth(2.0);
            sigGc.setStroke(javafx.scene.paint.Color.BLACK);

            signatureCanvas.addEventHandler(javafx.scene.input.MouseEvent.MOUSE_PRESSED, e -> {
                sigLastX = e.getX();
                sigLastY = e.getY();
            });
            signatureCanvas.addEventHandler(javafx.scene.input.MouseEvent.MOUSE_DRAGGED, e -> {
                sigGc.strokeLine(sigLastX, sigLastY, e.getX(), e.getY());
                sigLastX = e.getX();
                sigLastY = e.getY();
                signatureDrawn = true;
            });
        }

        if (lblDecisionValue != null) {
            lblDecisionValue.setText("Décision ML: —");
        }
    }

    private void refreshCriteres() {
        if (tableCriteres == null) {
            return;
        }
        referenceCriteres.setAll(critereImpactService.afficherReferences());
        tableCriteres.setItems(referenceCriteres);
        updateCritereStats(referenceCriteres.size());
    }

    private void refreshProjets() {
        ObservableList<Projet> projets = FXCollections.observableArrayList(projetService.afficher());
        if (tableProjets != null) {
            tableProjets.setItems(projets);
        }
        updateProjetStats(projets);
        selectProjetIfSet();
    }

    private void refreshEvaluations() {
        ObservableList<Evaluation> evaluations = FXCollections.observableArrayList(evaluationService.afficher());
        if (evaluations == null) return;
        // Recompute score for each evaluation from stored resultats to ensure correctness
        Services.CritereImpactService critService = new Services.CritereImpactService();
        for (Evaluation ev : evaluations) {
            List<EvaluationResult> res = critService.afficherParEvaluation(ev.getIdEvaluation());
            if (res != null && !res.isEmpty()) {
                double computed = calculateScore(res);
                ev.setScoreGlobal(computed);
                // Optionally update DB score if different
                // evaluationService.modifier(ev); // commented to avoid unintended writes
            }
        }
        if (tableAudits == null) {
            return;
        }
        tableAudits.setItems(evaluations);
        tableAudits.refresh();

        if (selectedProjet != null) {
            Evaluation match = null;
            for (Evaluation evaluation : evaluations) {
                if (evaluation != null && evaluation.getIdProjet() == selectedProjet.getId()) {
                    match = evaluation;
                    break;
                }
            }
            if (match != null) {
                tableAudits.getSelectionModel().select(match);
                return;
            }
        }

        if (lastSelectedEvaluationId != null) {
            for (Evaluation evaluation : evaluations) {
                if (evaluation != null && evaluation.getIdEvaluation() == lastSelectedEvaluationId) {
                    tableAudits.getSelectionModel().select(evaluation);
                    return;
                }
            }
        }

        if (!evaluations.isEmpty()) {
            tableAudits.getSelectionModel().selectFirst();
        }
        // Force refresh to show updated scores
        if (tableAudits != null) {
            tableAudits.refresh();
        }
    }

    private void updateProjetStats(ObservableList<Projet> projets) {
        if (lblProjetsAudit == null || lblProjetsEvalues == null) {
            return;
        }
        java.util.Set<Integer> evaluatedIds = new java.util.HashSet<>();
        java.util.List<Evaluation> evaluations = evaluationService.afficher();
        if (evaluations != null) {
            for (Evaluation evaluation : evaluations) {
                evaluatedIds.add(evaluation.getIdProjet());
            }
        }
        long submitted = projets.stream().filter(p -> "SUBMITTED".equalsIgnoreCase(p.getStatut())).count();
        long evaluated = projets.stream().filter(p -> evaluatedIds.contains(p.getId())).count();
        long pending = Math.max(0, submitted - evaluated);
        lblProjetsAudit.setText(String.valueOf(pending));
        lblProjetsEvalues.setText(String.valueOf(evaluated));
    }

    private void updateCritereStats(int total) {
        if (lblCriteresImpact == null) {
            return;
        }
        lblCriteresImpact.setText(String.valueOf(total));
    }

    private void selectProjetIfSet() {
        if (selectedProjet == null) {
            return;
        }
        if (txtIdProjet != null) {
            txtIdProjet.setText(String.valueOf(selectedProjet.getId()));
        }
        if (lblSelectedProjectName != null) {
            lblSelectedProjectName.setText(selectedProjet.getTitre());
        }
        selectedEvaluationId = null;
        rebuildCriteriaFields(null);
    }

    private void selectEvaluationForProjet(int projetId) {
        if (tableAudits == null) {
            selectedEvaluationId = null;
            rebuildCriteriaFields(null);
            return;
        }
        Evaluation match = null;
        for (Evaluation evaluation : tableAudits.getItems()) {
            if (evaluation != null && evaluation.getIdProjet() == projetId) {
                match = evaluation;
                break;
            }
        }
        if (match != null) {
            tableAudits.getSelectionModel().select(match);
            selectedEvaluationId = match.getIdEvaluation();
            lastSelectedEvaluationId = selectedEvaluationId;
        } else {
            selectedEvaluationId = null;
        }
        rebuildCriteriaFields(selectedEvaluationId);
    }

    private void updateAISuggestion(List<EvaluationResult> criteres) {
        if (criteres == null || criteres.isEmpty()) {
            if (lblAISuggestion != null) {
                lblAISuggestion.setText("Ajoutez des critères pour obtenir une suggestion IA.");
            }
            if (txtAIInsights != null) {
                txtAIInsights.clear();
            }
            return;
        }
        Models.AiSuggestion s = advancedFacade.suggest(null, criteres);
        if (lblAISuggestion != null) {
            lblAISuggestion.setText(
                    "Suggestion: " + s.getSuggestionDecision() +
                            " • Confiance: " + String.format(java.util.Locale.ROOT, "%.2f", s.getConfiance()) +
                            " • Score: " + String.format(java.util.Locale.ROOT, "%.2f", s.getScore())
            );
        }
        if (txtAIInsights != null) {
            StringBuilder sb = new StringBuilder();
            if (!s.getTopFactors().isEmpty()) {
                sb.append("Facteurs clés:\n");
                for (String f : s.getTopFactors()) {
                    sb.append(" • ").append(f).append("\n");
                }
            }
            if (!s.getWarnings().isEmpty()) {
                sb.append("\nAvertissements:\n");
                for (String w : s.getWarnings()) {
                    sb.append(" • ").append(w).append("\n");
                }
            }
            if (s.getConclusion() != null && !s.getConclusion().isEmpty()) {
                sb.append("\nConclusion:\n • ").append(s.getConclusion()).append("\n");
            }
            if (s.getRecommendations() != null && !s.getRecommendations().isEmpty()) {
                sb.append("\nRecommandations:\n");
                for (String r : s.getRecommendations()) {
                    sb.append(" • ").append(r).append("\n");
                }
            }
            txtAIInsights.setText(sb.toString().trim());
        }
    }

    @FXML
    void handleAISuggest() {
        List<EvaluationResult> resultats = collectResultatsFromFields();
        if (resultats == null || resultats.isEmpty()) {
            showError("Complétez au moins un critère (note et commentaire) pour lancer la suggestion IA.");
            return;
        }
        updateAISuggestion(resultats);
    }

    @FXML
    void handleScorePreview() {
        double score = calculateScoreFromFieldsLenient();
        if (lblAISuggestion != null) {
            lblAISuggestion.setText("Score prévisionnel: " + formatScore(score));
        }
    }

    @FXML
    void handleWhatIf() {
        // Scénario simple: +1 point sur chaque note (max 10)
        if (criteriaFieldsBox == null || criteriaFieldsBox.getChildren().isEmpty()) {
            showError("Aucun critère à simuler.");
            return;
        }
        java.util.List<EvaluationResult> list = new java.util.ArrayList<>();
        for (javafx.scene.Node node : criteriaFieldsBox.getChildren()) {
            if (!(node instanceof javafx.scene.layout.HBox)) continue;
            javafx.scene.layout.HBox row = (javafx.scene.layout.HBox) node;
            Integer idCritere = (Integer) row.getProperties().get("critereId");
            javafx.scene.control.TextField noteField = (javafx.scene.control.TextField) row.getProperties().get("noteField");
            javafx.scene.control.CheckBox respectBox = (javafx.scene.control.CheckBox) row.getProperties().get("respectBox");
            if (idCritere == null || noteField == null) continue;
            String text = noteField.getText() == null ? "" : noteField.getText().trim();
            if (text.isEmpty()) continue;
            try {
                int note = Math.min(10, Integer.parseInt(text) + 1);
                Models.EvaluationResult r = new Models.EvaluationResult();
                r.setIdCritere(idCritere);
                r.setNote(note);
                r.setEstRespecte(respectBox != null && respectBox.isSelected());
                list.add(r);
            } catch (NumberFormatException ignore) { }
        }
        if (list.isEmpty()) {
            showError("Entrez des notes numériques pour simuler un scénario.");
            return;
        }
        double newScore = calculateScore(list);
        if (txtAIInsights != null) {
            String previous = txtAIInsights.getText() == null ? "" : txtAIInsights.getText();
            String add = (previous.isEmpty() ? "" : (previous + "\n\n")) +
                    "What-if (+1 sur chaque note) → Score: " + formatScore(newScore);
            txtAIInsights.setText(add);
        } else if (lblAISuggestion != null) {
            lblAISuggestion.setText("What-if (+1) → Score: " + formatScore(newScore));
        }
    }

    @FXML
    void ajouterEvaluation() {
        try {
            if (!validateEvaluationSchema()) {
                return;
            }

            Integer ensuredProjectId = resolveCurrentProjectId();
            if (ensuredProjectId != null && txtIdProjet != null) {
                txtIdProjet.setText(String.valueOf(ensuredProjectId));
            }

            // Lancer l'IA d'abord pour obtenir la decision avant la creation.
            handleMlPredict();

            Evaluation evaluation = readEvaluationFromForm(false);
            if (evaluation == null) {
                return;
            }
            List<EvaluationResult> resultats = collectResultatsFromFields();
            if (resultats == null || resultats.isEmpty()) {
                showError("Ajoutez au moins un critere avant de creer l'evaluation.");
                return;
            }
            if (resolveDecision(false, evaluation.getIdProjet()) == null) {
                String localDecision = computeLocalDecision(resultats);
                if (localDecision != null) {
                    lastMlDecision = localDecision;
                    storeMlDecision(evaluation.getIdProjet(), localDecision);
                }
            }
            if (resolveDecision(false, evaluation.getIdProjet()) == null) {
                showError("Lancez l'evaluation ML pour obtenir la decision.");
                return;
            }

            evaluation.setScoreGlobal(calculateScore(resultats));
            if (txtScoreFinal != null) {
                txtScoreFinal.setText(formatScore(evaluation.getScoreGlobal()));
            }

            // Affichage minimaliste IA: "NomCritere: Recommandation" (top 3)
            try {
                java.util.List<String> recs = advancedFacade.criterionRecommendations(resultats);
                if (lblAISuggestion != null) {
                    String compact = recs.stream().limit(3).collect(Collectors.joining(" • "));
                    lblAISuggestion.setText(compact);
                }
            } catch (Exception ignore) { }

            int createdId = evaluationService.ajouterAvecCriteres(evaluation, resultats);
            if (createdId <= 0) {
                String details = evaluationService.getLastErrorMessage();
                showError("Creation evaluation echouee." + (details != null ? ("\n" + details) : ""));
                return;
            }

            // Calculer et persister le score ESG du projet suite à la nouvelle évaluation
            try {
                Integer esg = projectEsgService.calculateEsgForProject(evaluation.getIdProjet());
                if (esg != null) {
                    // Récupérer le projet et mettre à jour scoreEsg
                    java.util.List<Projet> all = projetService.afficher();
                    if (all != null) {
                        for (Projet p : all) {
                            if (p != null && p.getId() == evaluation.getIdProjet()) {
                                p.setScoreEsg(esg);
                                try { projetService.update(p); } catch (Exception ignore) {}
                                break;
                            }
                        }
                    }
                }
            } catch (Exception ex) {
                // On ne bloque pas l'UX si l'ESG ne peut pas être calculé
                System.err.println("ESG update failed: " + ex.getMessage());
            }

            // ============ EXTERNAL API INTEGRATION ============
            // Enrich evaluation with external carbon and air quality data
            try {
                // Get the project details
                java.util.List<Projet> allProjects = projetService.afficher();
                if (allProjects != null) {
                    for (Projet proj : allProjects) {
                        if (proj != null && proj.getId() == evaluation.getIdProjet()) {
                            // Run external API enrichment off the UI thread to avoid blocking
                            final Projet projFinal = proj;
                            final Integer evalIdFinal = createdId;
                            java.util.concurrent.CompletableFuture
                                    .runAsync(() -> {
                                        try {
                                            Models.CarbonReport carbonReport = carbonReportService.createReport(
                                                    (long) projFinal.getId(),
                                                    projFinal.getTitre(),
                                                    (long) projFinal.getEntrepriseId(),
                                                    projFinal.getCompanyEmail() != null ? projFinal.getCompanyEmail() : "Unknown"
                                            );
                                            if (carbonReport != null) {
                                                carbonReportService.enrichWithExternalData(carbonReport, projFinal);
                                                javafx.application.Platform.runLater(() -> {
                                                    if (txtAIInsights != null && carbonReport.getEvaluationDetails() != null) {
                                                        String currentText = txtAIInsights.getText();
                                                        txtAIInsights.setText(currentText + "\n\n" + carbonReport.getEvaluationDetails());
                                                    }
                                                    System.out.println("[AUDIT CONTROLLER] ✓ External API data integrated for evaluation " + evalIdFinal);
                                                });
                                            }
                                        } catch (Exception apiEx) {
                                            javafx.application.Platform.runLater(() -> {
                                                System.err.println("[AUDIT CONTROLLER] External API enrichment failed: " + apiEx.getMessage());
                                                if (txtAIInsights != null) {
                                                    String currentText = txtAIInsights.getText();
                                                    txtAIInsights.setText(currentText + "\n\n⚠️ External API data unavailable");
                                                }
                                            });
                                        }
                                    })
                                    .orTimeout(5, java.util.concurrent.TimeUnit.SECONDS)
                                    .exceptionally(ex -> {
                                        javafx.application.Platform.runLater(() -> {
                                            System.err.println("[AUDIT CONTROLLER] External API timeout: " + ex.getMessage());
                                            if (txtAIInsights != null) {
                                                String currentText = txtAIInsights.getText();
                                                txtAIInsights.setText(currentText + "\n\n⚠️ External API timeout");
                                            }
                                        });
                                        return null;
                                    });
                            break;
                        }
                    }
                }
            } catch (Exception apiEx) {
                // Graceful degradation - continue without external data
                System.err.println("[AUDIT CONTROLLER] External API enrichment failed: " + apiEx.getMessage());
                if (txtAIInsights != null) {
                    String currentText = txtAIInsights.getText();
                    txtAIInsights.setText(currentText + "\n\n⚠️ External API data unavailable");
                }
            }
            // ==================================================

            refreshEvaluations();
            refreshProjets();
            refreshCriteres();
            rebuildCriteriaFields(createdId);
            clearEvaluationForm();

            // Auto run ML and redirect to dashboard
            selectedProjet = findProjectById(evaluation.getIdProjet());
            showCongratsPopupAndRedirect();
        } catch (Exception ex) {
            System.err.println("Creation evaluation crashed: " + ex.getMessage());
            showError("Creation evaluation echouee: " + ex.getMessage());
        }
    }

    private void showCongratsPopupAndRedirect() {
        if (evaluationBanner != null && lblEvaluationBanner != null) {
            lblEvaluationBanner.setText("Evaluation creee. Score ESG en calcul, decision ML en cours...");
            evaluationBanner.setVisible(true);
            evaluationBanner.setManaged(true);
        }

        PauseTransition delay = new PauseTransition(javafx.util.Duration.seconds(1.8));
        delay.setOnFinished(event -> {
            if (evaluationBanner != null) {
                evaluationBanner.setVisible(false);
                evaluationBanner.setManaged(false);
            }
            handleOpenMlDashboard();
        });
        delay.play();
    }

    @FXML
    void modifierEvaluation() {
        if (!validateEvaluationSchema()) {
            return;
        }
        Evaluation evaluation = readEvaluationFromForm(true);
        if (evaluation == null) {
            return;
        }
        List<EvaluationResult> resultats = collectResultatsFromFields();
        if (resultats == null || resultats.isEmpty()) {
            showError("Ajoutez au moins un critere avant de modifier l'evaluation.");
            return;
        }
        evaluation.setScoreGlobal(calculateScore(resultats));
        if (txtScoreFinal != null) {
            txtScoreFinal.setText(formatScore(evaluation.getScoreGlobal()));
        }
        updateAISuggestion(resultats);
        evaluationService.modifier(evaluation);
        critereImpactService.modifierResultats(evaluation.getIdEvaluation(), resultats);

        // Recalcul ESG projet après modification
        try {
            Integer esg = projectEsgService.calculateEsgForProject(evaluation.getIdProjet());
            java.util.List<Projet> all = projetService.afficher();
            if (all != null) {
                for (Projet p : all) {
                    if (p != null && p.getId() == evaluation.getIdProjet()) {
                        p.setScoreEsg(esg);
                        try { projetService.update(p); } catch (Exception ignore) {}
                        break;
                    }
                }
            }
        } catch (Exception ex) {
            System.err.println("ESG update failed: " + ex.getMessage());
        }

        refreshEvaluations();
        refreshProjets();
    }

    @FXML
    void supprimerEvaluation() {
        Integer id = selectedEvaluationId;
        if (id == null) {
            showError("Selectionnez une evaluation.");
            return;
        }

        // Retenir l'id projet avant suppression
        Integer projetIdForEsg = null;
        java.util.List<Evaluation> allEvalsBefore = evaluationService.afficher();
        if (allEvalsBefore != null) {
            for (Evaluation ev : allEvalsBefore) {
                if (ev != null && ev.getIdEvaluation() == id) {
                    projetIdForEsg = ev.getIdProjet();
                    break;
                }
            }
        }

        evaluationService.supprimer(id);
        selectedEvaluationId = null;

        // Recalculer l'ESG après suppression
        if (projetIdForEsg != null) {
            try {
                Integer esg = projectEsgService.calculateEsgForProject(projetIdForEsg);
                java.util.List<Projet> all = projetService.afficher();
                if (all != null) {
                    for (Projet p : all) {
                        if (p != null && p.getId() == projetIdForEsg) {
                            p.setScoreEsg(esg); // peut être null si plus d'évaluations
                            try { projetService.update(p); } catch (Exception ignore) {}
                            break;
                        }
                    }
                }
            } catch (Exception ex) {
                System.err.println("ESG update failed: " + ex.getMessage());
            }
        }

        refreshEvaluations();
        refreshProjets();
        refreshCriteres();
        rebuildCriteriaFields(null);
        clearEvaluationForm();
    }

    @FXML
    void ajouterCritere() {
        // Nom du critere : min 8 max 30
        String nom = requireLength(txtNomCritere, "Nom du critere", 8, 30);
        // Description : min 8 max 250
        String description = requireLength(txtCommentaireCritere, "Description", 8, 250);
        // Poids : entier entre 1 et 10
        Integer poids = requireNote(txtNote.getText());
        if (nom == null || description == null || poids == null) {
            return;
        }
        CritereReference critere = new CritereReference(nom, description, poids);
        critereImpactService.ajouterReference(critere);
        refreshCriteres();
        rebuildCriteriaFields(selectedEvaluationId);
        clearCritereForm();
    }

    @FXML
    void modifierCritere() {
        CritereReference selected = tableCriteres != null ? tableCriteres.getSelectionModel().getSelectedItem() : null;
        if (selected == null) {
            showError("Selectionnez un critere.");
            return;
        }
        String nom = requireLength(txtNomCritere, "Nom du critere", 8, 30);
        String description = requireLength(txtCommentaireCritere, "Description", 8, 250);
        Integer poids = requireNote(txtNote.getText());
        if (nom == null || description == null || poids == null) {
            return;
        }
        selected.setNomCritere(nom);
        selected.setPoids(poids);
        selected.setDescription(description);
        critereImpactService.modifierReference(selected);
        refreshCriteres();
        rebuildCriteriaFields(selectedEvaluationId);
    }

    @FXML
    void supprimerCritere() {
        CritereReference selected = tableCriteres != null ? tableCriteres.getSelectionModel().getSelectedItem() : null;
        if (selected == null) {
            showError("Selectionnez un critere.");
            return;
        }
        boolean deleted = critereImpactService.supprimerReference(selected.getIdCritere());
        if (!deleted) {
            showError("Suppression echouee.");
            return;
        }
        refreshCriteres();
        rebuildCriteriaFields(selectedEvaluationId);
        clearCritereForm();
    }

    private void clearCritereForm() {
        if (txtNomCritere != null) {
            txtNomCritere.clear();
        }
        if (txtNote != null) {
            txtNote.clear();
        }
        if (txtCommentaireCritere != null) {
            txtCommentaireCritere.clear();
        }
    }

    private Evaluation readEvaluationFromForm(boolean requireId) {
        if (txtObservations == null || txtIdProjet == null) {
            showError("Formulaire evaluation incomplet.");
            return null;
        }
        String observations = requireLength(txtObservations, "Observations", 10, 250);
        Integer idProjet = parseInt(txtIdProjet.getText(), "ID Projet");

        if (observations == null || idProjet == null) {
            return null;
        }

        String statut = projetService.getStatutById(idProjet);
        if (statut != null && statut.trim().equalsIgnoreCase("CANCELLED")) {
            showError("Impossible d'evaluer un projet cancelled.");
            return null;
        }

        String decision = resolveDecision(requireId, idProjet);
        if (decision == null) {
            showError("Lancez l'évaluation ML pour obtenir la décision.");
            return null;
        }

        Evaluation evaluation = new Evaluation(observations, 0, decision, idProjet);
        if (requireId) {
            if (selectedEvaluationId == null) {
                showError("Selectionnez une evaluation.");
                return null;
            }
            evaluation.setIdEvaluation(selectedEvaluationId);
        }
        return evaluation;
    }

    private void loadEvaluationById(int evaluationId) {
        Evaluation target = getEvaluationById(evaluationId);
        if (target == null) {
            return;
        }
        applyEvaluationToForm(target);
        if (target.getDecision() != null) {
            lastMlDecision = target.getDecision();
        }
        selectedEvaluationId = target.getIdEvaluation();
    }

    private Evaluation getEvaluationById(int evaluationId) {
        List<Evaluation> all = evaluationService.afficher();
        if (all == null) return null;
        for (Evaluation ev : all) {
            if (ev != null && ev.getIdEvaluation() == evaluationId) {
                return ev;
            }
        }
        return null;
    }

    private String resolveDecision(boolean requireId, int projectId) {
        if (lastMlDecision != null) {
            return mapMlDecision(lastMlDecision);
        }
        String stored = getStoredMlDecision(projectId);
        if (stored != null) {
            return mapMlDecision(stored);
        }
        if (requireId) {
            if (tableAudits != null) {
                Evaluation selected = tableAudits.getSelectionModel().getSelectedItem();
                if (selected != null && selected.getDecision() != null) {
                    return selected.getDecision();
                }
            }
            Integer evalId = selectedEvaluationId != null ? selectedEvaluationId : lastSelectedEvaluationId;
            if (evalId != null) {
                Evaluation ev = getEvaluationById(evalId);
                if (ev != null && ev.getDecision() != null) {
                    return ev.getDecision();
                }
            }
        }
        return null;
    }

    private String mapMlDecision(String mlDecision) {
        if (mlDecision == null) return null;
        String v = mlDecision.trim().toLowerCase();
        if (v.contains("approve") || v.contains("accept") || v.contains("ok") || v.contains("accepted")) {
            return "Approuve";
        }
        if (v.contains("reject") || v.contains("refuse") || v.contains("rejected")) {
            return "Rejete";
        }
        return "Rejete";
    }

    private void clearEvaluationForm() {
        if (txtObservations != null) {
            txtObservations.clear();
        }
        if (txtIdProjet != null) {
            txtIdProjet.clear();
        }
        if (txtScoreFinal != null) {
            txtScoreFinal.clear();
        }
        lastMlDecision = null;
        lastMlConfidence = null;
        lastSelectedEvaluationId = null;
        if (lblDecisionValue != null) {
            lblDecisionValue.setText("Décision ML: —");
        }
        if (lblMlStatus != null) {
            lblMlStatus.setText("ML: prêt");
        }
        if (listMlFactors != null) {
            listMlFactors.getItems().clear();
        }
        if (mlFactorsChart != null) {
            mlFactorsChart.getData().clear();
        }
    }

    private void enforceSingleDecision() {
        if (chkDecisionApproved == null || chkDecisionRejected == null) {
            return;
        }
        chkDecisionApproved.setToggleGroup(decisionGroup);
        chkDecisionRejected.setToggleGroup(decisionGroup);

        chkDecisionApproved.setDisable(false);
        chkDecisionRejected.setDisable(false);
    }

    private void setDecisionCheckboxes(String decision) {
        if (chkDecisionApproved == null || chkDecisionRejected == null) {
            return;
        }
        clearDecisionSelection();
        if (decision == null) {
            return;
        }
        String value = decision.trim().toLowerCase();
        if (value.contains("approuve") || value.contains("accept") || value.contains("accepte")) {
            chkDecisionApproved.setSelected(true);
        } else if (value.contains("rejete") || value.contains("refuse") || value.contains("refus")) {
            chkDecisionRejected.setSelected(true);
        }
    }

    private void clearDecisionSelection() {
        if (decisionGroup != null) {
            decisionGroup.selectToggle(null);
        }
    }

    private void applyEvaluationToForm(Evaluation selected) {
        if (selected == null) {
            return;
        }
        if (txtObservations != null) {
            txtObservations.setText(selected.getObservations());
        }
        if (txtIdProjet != null) {
            txtIdProjet.setText(String.valueOf(selected.getIdProjet()));
        }
        if (lblSelectedProjectName != null && selected.getTitreProjet() != null) {
            lblSelectedProjectName.setText(selected.getTitreProjet());
        }
        if (txtScoreFinal != null) {
            txtScoreFinal.setText(formatScore(selected.getScoreGlobal()));
        }
        if (lblDecisionValue != null) {
            lblDecisionValue.setText("Décision ML: " + selected.getDecision());
        }
        selectedEvaluationId = selected.getIdEvaluation();
        lastSelectedEvaluationId = selectedEvaluationId;
        rebuildCriteriaFields(selectedEvaluationId);
    }

    @FXML
    private void handleOpenMlDashboard() {
        if (txtIdProjet != null && !txtIdProjet.getText().isBlank()) {
            Integer id = parseInt(txtIdProjet.getText(), "ID Projet");
            if (id != null) {
                Projet p = findProjectById(id);
                if (p != null) {
                    selectedProjet = p;
                }
            }
        }
        try {
            MainFX.setRoot("mlDecision");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private Integer resolveCurrentProjectId() {
        if (txtIdProjet != null) {
            String raw = txtIdProjet.getText();
            if (raw != null && !raw.trim().isEmpty()) {
                try {
                    return Integer.parseInt(raw.trim());
                } catch (NumberFormatException ignore) {
                    // fall through
                }
            }
        }
        if (selectedProjet != null) {
            return selectedProjet.getId();
        }
        return null;
    }

    @FXML
    void handleMlPredict() {
        try {
            if (lblMlStatus != null) {
                lblMlStatus.setText("");
            }

            Integer projetId = resolveCurrentProjectId();
            if (projetId == null) {
                return;
            }

            List<EvaluationResult> results = buildResultsLenientForEsg();
            if (results.isEmpty()) {
                return;
            }

            Projet project = findProjectById(projetId);
            String description = project != null && project.getDescription() != null && !project.getDescription().isBlank()
                    ? project.getDescription()
                    : (project != null ? project.getTitre() : "");
            String sector = (project != null && project.getActivityType() != null && !project.getActivityType().isBlank())
                    ? project.getActivityType() : "unknown";
            double budget = project != null ? project.getBudget() : 0.0;

            Map<String, Object> payloadMap = new java.util.LinkedHashMap<>();
            payloadMap.put("description", description);
            payloadMap.put("budget", budget);
            payloadMap.put("sector", sector);
            payloadMap.put("criteres", buildCriteriaPayload(results));

            String payload = new Gson().toJson(payloadMap);
            if (payload == null || payload.isBlank()) {
                return;
            }

            String baseUrl = System.getenv().getOrDefault("ML_API_BASE_URL", "http://localhost:8082");
            String endpoint = baseUrl.endsWith("/") ? baseUrl + "analyze-project" : baseUrl + "/analyze-project";

            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(endpoint))
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .timeout(Duration.ofSeconds(8))
                    .POST(HttpRequest.BodyPublishers.ofString(payload, StandardCharsets.UTF_8))
                    .build();

            HttpClient client = HttpClient.newHttpClient();
            HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            String body = resp.body() == null ? "" : resp.body().trim();
            if (resp.statusCode() != 200 || body.isEmpty()) {
                String localDecision = computeLocalDecision(results);
                if (localDecision != null) {
                    lastMlDecision = localDecision;
                    storeMlDecision(projetId, localDecision);
                    if (lblDecisionValue != null) {
                        lblDecisionValue.setText("Décision ML: " + mapMlDecision(localDecision));
                    }
                }
                return;
            }

            String jsonBody = body;
            if (!jsonBody.startsWith("{")) {
                int start = jsonBody.indexOf('{');
                int end = jsonBody.lastIndexOf('}');
                if (start >= 0 && end > start) {
                    jsonBody = jsonBody.substring(start, end + 1).trim();
                }
            }
            if (!jsonBody.startsWith("{")) {
                return;
            }

            Map<String, Object> parsed = new Gson().fromJson(jsonBody, new TypeToken<Map<String, Object>>(){}.getType());
            int esgScore = getInt(parsed.get("predicted_esg_score"), 0);
            int credibility = getInt(parsed.get("credibility_score"), 0);
            String carbonRisk = String.valueOf(parsed.getOrDefault("carbon_risk", "N/A"));
            String recommendations = String.valueOf(parsed.getOrDefault("recommendations", ""));

            String decision = deriveDecision(esgScore, carbonRisk);
            lastMlDecision = decision;
            storeMlDecision(projetId, decision);
            lastMlConfidence = credibility / 100.0;

            if (txtMlRecommendation != null) {
                StringBuilder sb = new StringBuilder();
                sb.append("ESG: ").append(esgScore).append(" | Risk: ").append(carbonRisk).append("\n\n");
                if (recommendations != null && !recommendations.isBlank()) {
                    sb.append(recommendations);
                }
                txtMlRecommendation.setText(sb.toString().trim());
            }
            if (lblDecisionValue != null) {
                lblDecisionValue.setText("Décision ML: " + mapMlDecision(decision));
            }
        } catch (Exception ignored) {
            // Silent fallback: keep local recommendations/decision.
        }
    }

    private java.util.List<java.util.Map<String, Object>> buildCriteriaPayload(List<EvaluationResult> results) {
        java.util.Map<Integer, String> names = new java.util.HashMap<>();
        for (CritereReference ref : referenceCriteres) {
            names.put(ref.getIdCritere(), ref.getNomCritere());
        }
        java.util.List<java.util.Map<String, Object>> out = new java.util.ArrayList<>();
        for (EvaluationResult r : results) {
            java.util.Map<String, Object> row = new java.util.LinkedHashMap<>();
            row.put("name", names.getOrDefault(r.getIdCritere(), "Critere #" + r.getIdCritere()));
            row.put("note", r.getNote());
            row.put("respect", r.isEstRespecte());
            out.add(row);
        }
        return out;
    }

    private String deriveDecision(int esgScore, String carbonRisk) {
        String risk = carbonRisk == null ? "" : carbonRisk.toLowerCase(java.util.Locale.ROOT);
        if (esgScore >= 65 && !risk.contains("high")) {
            return "APPROVE";
        }
        if (esgScore >= 55 && risk.contains("low")) {
            return "APPROVE";
        }
        return "REJECT";
    }

    private String computeLocalDecision(List<EvaluationResult> results) {
        if (results == null || results.isEmpty()) {
            return null;
        }
        double score = calculateScore(results);
        double complianceRate = results.stream().mapToDouble(r -> r.isEstRespecte() ? 1.0 : 0.0).average().orElse(0.0);
        if (score >= 6.5 && complianceRate >= 0.6) {
            return "APPROVE";
        }
        return "REJECT";
    }

    private int getInt(Object value, int fallback) {
        if (value == null) return fallback;
        try {
            if (value instanceof Number) {
                return ((Number) value).intValue();
            }
            return Integer.parseInt(String.valueOf(value));
        } catch (Exception ex) {
            return fallback;
        }
    }

    private void updateMlVisualization(List<EvaluationResult> results, String decision, String confStr) {
        if (listMlFactors != null) {
            listMlFactors.getItems().clear();
        }
        if (mlFactorsChart != null) {
            mlFactorsChart.getData().clear();
        }

        List<Models.ScoreExplanation> explanations = advancedFacade.explainScore(results);
        explanations.sort(java.util.Comparator.comparingDouble(Models.ScoreExplanation::getContribution).reversed());

        if (listMlFactors != null) {
            for (int i = 0; i < Math.min(5, explanations.size()); i++) {
                Models.ScoreExplanation e = explanations.get(i);
                listMlFactors.getItems().add(e.getNomCritere() + " • impact " + String.format(java.util.Locale.ROOT, "%.2f", e.getContribution()));
            }
        }

        if (mlFactorsChart != null) {
            javafx.scene.chart.XYChart.Series<Number, Number> series = new javafx.scene.chart.XYChart.Series<>();
            series.setName("Impact par critère");
            int idx = 1;
            for (Models.ScoreExplanation e : explanations) {
                if (idx > 8) break;
                series.getData().add(new javafx.scene.chart.XYChart.Data<>(idx, e.getContribution()));
                idx++;
            }
            mlFactorsChart.getData().add(series);
        }
    }

    private Projet findProjectById(int projetId) {
        List<Projet> all = projetService.afficher();
        if (all == null) return null;
        for (Projet p : all) {
            if (p != null && p.getId() == projetId) return p;
        }
        return null;
    }

    private boolean validateEvaluationSchema() {
        String[][] required = new String[][]{
                {"evaluation", "id_evaluation"},
                {"evaluation", "date_evaluation"},
                {"evaluation", "observations_globales"},
                {"evaluation", "score_final"},
                {"evaluation", "est_valide"},
                {"evaluation", "id_projet"},
                {"evaluation_resultat", "id_evaluation"},
                {"evaluation_resultat", "id_critere"},
                {"evaluation_resultat", "note"},
                {"evaluation_resultat", "commentaire_expert"},
                {"evaluation_resultat", "est_respecte"},
                {"critere_reference", "id_critere"},
                {"critere_reference", "nom_critere"},
                {"critere_reference", "poids"},
                {"projet", "id"}
        };

        try (Connection conn = MyConnection.getConnection()) {
            for (String[] req : required) {
                if (!columnExists(conn, req[0], req[1])) {
                    showError("Schéma incomplet: colonne manquante " + req[0] + "." + req[1]);
                    return false;
                }
            }
        } catch (SQLException ex) {
            showError("Échec vérification base: " + ex.getMessage());
            return false;
        }
        return true;
    }

    private boolean columnExists(Connection conn, String table, String column) throws SQLException {
        String sql = "SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND COLUMN_NAME = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, table);
            ps.setString(2, column);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    private <S> void applyWrapping(TableColumn<S, String> column) {
        if (column == null) {
            return;
        }
        column.setCellFactory(col -> new TableCell<S, String>() {
            private final javafx.scene.text.Text text = new javafx.scene.text.Text();
            {
                text.wrappingWidthProperty().bind(col.widthProperty().subtract(16));
                setGraphic(text);
                setPrefHeight(Control.USE_COMPUTED_SIZE);
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    text.setText("");
                    setGraphic(null);
                } else {
                    text.setText(item);
                    setGraphic(text);
                }
            }
        });
    }

    private void setupStaticInputConstraints() {
        // Currently handled by dynamic fields; kept for future static constraints.
    }

    private void setActiveNav(Button active) {
        if (btnGestionProjets != null) {
            btnGestionProjets.getStyleClass().remove("nav-btn-active");
        }
        if (btnGestionEvaluations != null) {
            btnGestionEvaluations.getStyleClass().remove("nav-btn-active");
        }
        if (active != null) {
            active.getStyleClass().add("nav-btn-active");
        }
    }

    @FXML
    private void showGestionProjets() {
        System.out.println("Affichage de la gestion des projets");
        try {
            MainFX.setRoot("expertProjet");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void showGestionEvaluations() {
        System.out.println("Affichage de la gestion des évaluations");
        try {
            MainFX.setRoot("gestionCarbone");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void showSettings() {
        System.out.println("Affichage des parametres");
        try {
            MainFX.setRoot("settings");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleEditProfile() {
        try {
            MainFX.setRoot("editProfile");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleBack() {
        try {
            MainFX.setRoot(resolveBackTarget());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String resolveBackTarget() {
        return "fxml/dashboard";
    }

    private Integer parseInt(String text, String fieldName) {
        try {
            return Integer.parseInt(text.trim());
        } catch (NumberFormatException ex) {
            showError(fieldName + " invalide.");
            return null;
        }
    }

    private Integer requireNote(String text) {
        Integer note = parseInt(text, "Note");
        if (note == null) {
            return null;
        }
        if (note < 1 || note > 10) {
            showError("Note doit etre entre 1 et 10.");
            return null;
        }
        return note;
    }

    private String requireText(TextInputControl control, String fieldName) {
        if (control == null) {
            return null;
        }
        String value = control.getText() != null ? control.getText().trim() : "";
        if (value.isEmpty()) {
            showError(fieldName + " est obligatoire.");
            return null;
        }
        return value;
    }

    private String requireLength(TextInputControl control, String fieldName, int min, int max) {
        String value = requireText(control, fieldName);
        if (value == null) {
            return null;
        }
        int len = value.length();
        if (len < min || len > max) {
            showError(fieldName + " doit etre entre " + min + " et " + max + " caracteres.");
            return null;
        }
        return value;
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Validation");
        alert.setHeaderText("Erreur de saisie");
        alert.setContentText(message);
        alert.showAndWait();
    }

    private Integer extractLeadingNumber(String value) {
        int i = 0;
        while (i < value.length() && Character.isWhitespace(value.charAt(i))) {
            i++;
        }
        int start = i;
        while (i < value.length() && Character.isDigit(value.charAt(i))) {
            i++;
        }
        if (i == start) {
            return null;
        }
        return Integer.parseInt(value.substring(start, i));
    }

    private double calculateScore(List<EvaluationResult> criteres) {
        if (criteres.isEmpty()) {
            return 0.0;
        }
        final double NON_COMPLIANCE_FACTOR = 0.6;
        double weightedSum = 0.0;
        int totalWeight = 0;
        for (EvaluationResult critere : criteres) {
            int poids = getPoidsForCritere(critere.getIdCritere());
            double noteEff = critere.getNote() * (critere.isEstRespecte() ? 1.0 : NON_COMPLIANCE_FACTOR);
            if (noteEff < 0) noteEff = 0;
            if (noteEff > 10) noteEff = 10;
            weightedSum += noteEff * poids;
            totalWeight += poids;
        }
        return totalWeight == 0 ? 0.0 : weightedSum / totalWeight;
    }

    private int getPoidsForCritere(int idCritere) {
        for (CritereReference ref : referenceCriteres) {
            if (ref.getIdCritere() == idCritere) {
                return Math.max(ref.getPoids(), 1);
            }
        }
        return 1;
    }

    private double calculateScoreFromFields() {
        List<EvaluationResult> results = collectResultatsFromFields();
        return results == null ? 0.0 : calculateScore(results);
    }

    private double calculateScoreFromFieldsLenient() {
        if (criteriaFieldsBox == null || criteriaFieldsBox.getChildren().isEmpty()) {
            return 0.0;
        }
        final double NON_COMPLIANCE_FACTOR = 0.6;
        double weightedSum = 0.0;
        int totalWeight = 0;
        for (javafx.scene.Node node : criteriaFieldsBox.getChildren()) {
            if (!(node instanceof javafx.scene.layout.HBox)) {
                continue;
            }
            javafx.scene.layout.HBox row = (javafx.scene.layout.HBox) node;
            Integer idCritere = (Integer) row.getProperties().get("critereId");
            javafx.scene.control.TextField noteField = (javafx.scene.control.TextField) row.getProperties().get("noteField");
            javafx.scene.control.CheckBox respectBox = (javafx.scene.control.CheckBox) row.getProperties().get("respectBox");
            if (idCritere == null || noteField == null) continue;
            String text = noteField.getText() == null ? "" : noteField.getText().trim();
            if (text.isEmpty()) continue;
            try {
                int note = Integer.parseInt(text);
                if (note < 1 || note > 10) continue;
                int poids = idCritere == null ? 1 : getPoidsForCritere(idCritere);
                double noteEff = note * ((respectBox != null && respectBox.isSelected()) ? 1.0 : NON_COMPLIANCE_FACTOR);
                if (noteEff < 0) noteEff = 0;
                if (noteEff > 10) noteEff = 10;
                weightedSum += noteEff * poids;
                totalWeight += poids;
            } catch (NumberFormatException ignore) {
                // ignore invalid values in preview
            }
        }
        return totalWeight == 0 ? 0.0 : weightedSum / totalWeight;
    }

    private String formatScore(double score) {
        return String.format(java.util.Locale.US, "%.2f", score);
    }

    private List<EvaluationResult> collectResultatsFromFields() {
        if (criteriaFieldsBox == null) {
            return java.util.Collections.emptyList();
        }
        List<EvaluationResult> results = new java.util.ArrayList<>();
        for (javafx.scene.Node node : criteriaFieldsBox.getChildren()) {
            if (!(node instanceof javafx.scene.layout.HBox)) {
                continue;
            }
            javafx.scene.layout.HBox row = (javafx.scene.layout.HBox) node;
            Integer idCritere = (Integer) row.getProperties().get("critereId");
            javafx.scene.control.TextField noteField = (javafx.scene.control.TextField) row.getProperties().get("noteField");
            javafx.scene.control.TextArea commentField = (javafx.scene.control.TextArea) row.getProperties().get("commentField");
            javafx.scene.control.CheckBox respectBox = (javafx.scene.control.CheckBox) row.getProperties().get("respectBox");

            if (noteField == null || commentField == null) {
                continue;
            }

            Integer note = requireNote(noteField.getText());
            String commentaire = requireLength(commentField, "Commentaire technique", 8, 250);
            if (idCritere == null || note == null || commentaire == null) {
                return null;
            }

            EvaluationResult result = new EvaluationResult();
            result.setIdCritere(idCritere);
            result.setNote(note);
            result.setCommentaireExpert(commentaire);
            result.setEstRespecte(respectBox != null && respectBox.isSelected());
            results.add(result);
        }
        return results;
    }

    // Construit des résultats tolérants (sans exiger les commentaires) et renseigne le nom du critère
    private java.util.List<Models.EvaluationResult> buildResultsLenientForEsg() {
        java.util.List<Models.EvaluationResult> list = new java.util.ArrayList<>();
        if (criteriaFieldsBox == null) return list;
        java.util.Map<Integer, String> nameIndex = new java.util.HashMap<>();
        for (Models.CritereReference ref : referenceCriteres) {
            nameIndex.put(ref.getIdCritere(), ref.getNomCritere());
        }
        for (javafx.scene.Node node : criteriaFieldsBox.getChildren()) {
            if (!(node instanceof javafx.scene.layout.HBox)) continue;
            javafx.scene.layout.HBox row = (javafx.scene.layout.HBox) node;
            Integer idCritere = (Integer) row.getProperties().get("critereId");
            javafx.scene.control.TextField noteField = (javafx.scene.control.TextField) row.getProperties().get("noteField");
            javafx.scene.control.CheckBox respectBox = (javafx.scene.control.CheckBox) row.getProperties().get("respectBox");
            if (idCritere == null || noteField == null) continue;
            String text = noteField.getText() == null ? "" : noteField.getText().trim();
            if (text.isEmpty()) continue;
            try {
                int note = Integer.parseInt(text);
                if (note < 1 || note > 10) continue;
                String nom = nameIndex.getOrDefault(idCritere, "Critère #" + idCritere);
                Models.EvaluationResult r = new Models.EvaluationResult();
                r.setIdCritere(idCritere);
                r.setNomCritere(nom);
                r.setNote(note);
                r.setEstRespecte(respectBox != null && respectBox.isSelected());
                list.add(r);
            } catch (NumberFormatException ignore) { }
        }
        return list;
    }

    private void rebuildCriteriaFields(Integer evaluationId) {
        if (criteriaFieldsBox == null) {
            return;
        }
        java.util.Map<Integer, EvaluationResult> existing = new java.util.HashMap<>();
        if (evaluationId != null) {
            for (EvaluationResult critere : critereImpactService.afficherParEvaluation(evaluationId)) {
                existing.put(critere.getIdCritere(), critere);
            }
        }

        criteriaFieldsBox.getChildren().clear();
        for (CritereReference reference : referenceCriteres) {
            javafx.scene.layout.HBox row = new javafx.scene.layout.HBox(12);
            javafx.scene.control.Label name = new javafx.scene.control.Label(reference.getNomCritere());
            name.getStyleClass().add("form-label");
            name.setPrefWidth(180);

            javafx.scene.control.CheckBox respectBox = new javafx.scene.control.CheckBox("Respecte");

            javafx.scene.control.TextField noteField = new javafx.scene.control.TextField();
            noteField.setPromptText("Note 1-10");
            noteField.getStyleClass().add("field");
            noteField.setPrefWidth(120);

            UnaryOperator<TextFormatter.Change> integerFilter = change -> {
                String newText = change.getControlNewText();
                if (newText.matches("\\d{0,2}")) {
                    return change;
                }
                return null;
            };
            noteField.setTextFormatter(new TextFormatter<>(integerFilter));

            noteField.textProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal == null || newVal.trim().isEmpty()) {
                    markInvalid(noteField, false);
                    updateScorePreview();
                    return;
                }
                try {
                    int v = Integer.parseInt(newVal.trim());
                    markInvalid(noteField, v < 1 || v > 10);
                } catch (NumberFormatException e) {
                    markInvalid(noteField, true);
                }
                updateScorePreview();
            });

            javafx.scene.control.TextArea commentField = new javafx.scene.control.TextArea();
            commentField.setPromptText("Commentaire technique");
            commentField.getStyleClass().addAll("field", "textarea");
            commentField.setPrefRowCount(2);

            limitTextLength(commentField, 250);
            commentField.textProperty().addListener((obs, oldV, newV) -> {
                boolean invalid = !newV.trim().isEmpty() && (newV.trim().length() < 8 || newV.trim().length() > 250);
                markInvalid(commentField, invalid);
            });

            EvaluationResult existingResult = existing.get(reference.getIdCritere());
            if (existingResult != null) {
                noteField.setText(String.valueOf(existingResult.getNote()));
                commentField.setText(existingResult.getCommentaireExpert());
                respectBox.setSelected(existingResult.isEstRespecte());
            }

            noteField.textProperty().addListener((obs, oldVal, newVal) -> updateScorePreview());

            row.getProperties().put("critereId", reference.getIdCritere());
            row.getProperties().put("noteField", noteField);
            row.getProperties().put("commentField", commentField);
            row.getProperties().put("respectBox", respectBox);
            row.getChildren().addAll(name, respectBox, noteField, commentField);
            criteriaFieldsBox.getChildren().add(row);
        }
        updateScorePreview();
    }

    private void markInvalid(javafx.scene.control.TextField field, boolean invalid) {
        if (field == null) return;
        if (invalid) {
            if (!field.getStyleClass().contains("field-error")) {
                field.getStyleClass().add("field-error");
            }
        } else {
            field.getStyleClass().remove("field-error");
        }
    }

    private void markInvalid(javafx.scene.control.TextArea field, boolean invalid) {
        if (field == null) return;
        if (invalid) {
            if (!field.getStyleClass().contains("field-error")) {
                field.getStyleClass().add("field-error");
            }
        } else {
            field.getStyleClass().remove("field-error");
        }
    }

    private void limitTextLength(javafx.scene.control.TextArea field, int maxLength) {
        if (field == null) return;
        field.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && newVal.length() > maxLength) {
                field.setText(oldVal);
            }
        });
    }

    private void updateScorePreview() {
        double score = calculateScoreFromFieldsLenient();
        if (txtScoreFinal != null) {
            txtScoreFinal.setText(score > 0 ? formatScore(score) : "");
        }
    }

    private void applyDecisionToStatus(Evaluation evaluation) {
        if (evaluation == null) {
            showError("Selectionnez une evaluation.");
            return;
        }
        String status = mapDecisionToStatus(evaluation.getDecision());
        if (status == null) {
            showError("Decision invalide. Utilisez accepte/approuve ou refuse/rejete.");
            return;
        }
        boolean updated = projetService.updateStatut(evaluation.getIdProjet(), status);
        if (!updated) {
            showError("Mise a jour statut echouee.");
            return;
        }
        refreshProjets();
        refreshEvaluations();
    }

    private String mapDecisionToStatus(String decision) {
        if (decision == null) {
            return null;
        }
        String value = decision.trim().toLowerCase();
        if (value.isEmpty()) {
            return null;
        }
        if (value.contains("accepte") || value.contains("accept") || value.contains("approuve") || value.contains("approve")) {
            return "IN_PROGRESS";
        }
        if (value.contains("refuse") || value.contains("refus") || value.contains("rejete") || value.contains("reject")) {
            return "CANCELLED";
        }
        return null;
    }

    @FXML
    void handleCalculateESG() {
        java.util.List<Models.EvaluationResult> results = buildResultsLenientForEsg();
        if (results.isEmpty()) {
            showError("Saisissez des notes pour au moins un critère afin de calculer le score ESG.");
            return;
        }
        Services.ProjectEsgService.EsgBreakdown b = new Services.ProjectEsgService().breakdown(results);
        int esg100 = (int) Math.round(b.esg10 * 10.0);
        if (lblEsgScore != null) {
            lblEsgScore.setText(String.valueOf(esg100));
        }
        if (txtEsgDetails != null) {
            String details = String.format(java.util.Locale.ROOT,
                    "Formule: ESG = 50%%*E + 30%%*S + 20%%*G (sur 0–10), puis ×10 -> 0–100\n" +
                            "Pénalisation: note_effective = note × 0.6 si Non Respecté\n" +
                            "E = %.2f, S = %.2f, G = %.2f, ESG(0–10) = %.2f, ESG(0–100) = %d",
                    b.e, b.s, b.g, b.esg10, esg100);
            txtEsgDetails.setText(details);
        }
    }

    @FXML
    void handleSaveESG() {
        Integer projetId = parseInt(txtIdProjet != null ? txtIdProjet.getText() : null, "ID Projet");
        if (projetId == null) return;

        java.util.List<Models.EvaluationResult> results = buildResultsLenientForEsg();
        if (results.isEmpty()) {
            showError("Saisissez des notes pour au moins un critère afin d'enregistrer le score ESG.");
            return;
        }
        Services.ProjectEsgService.EsgBreakdown b = new Services.ProjectEsgService().breakdown(results);
        int esg100 = (int) Math.round(b.esg10 * 10.0);

        try {
            java.util.List<Models.Projet> all = projetService.afficher();
            if (all != null) {
                for (Models.Projet p : all) {
                    if (p != null && p.getId() == projetId) {
                        p.setScoreEsg(esg100);
                        try { projetService.update(p); } catch (Exception ignore) {}
                        break;
                    }
                }
            }
            if (lblEsgScore != null) lblEsgScore.setText(String.valueOf(esg100));
            if (txtEsgDetails != null && (txtEsgDetails.getText() == null || txtEsgDetails.getText().isEmpty())) {
                txtEsgDetails.setText("Score ESG enregistré pour le projet #" + projetId + ": " + esg100);
            }
            refreshProjets();
        } catch (Exception ex) {
            showError("Échec lors de l'enregistrement du score ESG: " + ex.getMessage());
        }
    }

    @FXML
    void handleClearSignature() {
        if (sigGc != null && signatureCanvas != null) {
            sigGc.clearRect(0, 0, signatureCanvas.getWidth(), signatureCanvas.getHeight());
            signatureDrawn = false;
        }
    }

    @FXML
    void handleExportPdf() {
        try {
            Evaluation evaluation;
            java.util.List<EvaluationResult> resultats;
            if (selectedEvaluationId != null) {
                evaluation = tableAudits != null ? tableAudits.getSelectionModel().getSelectedItem() : null;
                if (evaluation == null) {
                    for (Evaluation ev : evaluationService.afficher()) {
                        if (ev != null && ev.getIdEvaluation() == selectedEvaluationId) {
                            evaluation = ev;
                            break;
                        }
                    }
                }
                if (evaluation == null) {
                    showError("Aucune évaluation sélectionnée.");
                    return;
                }
                resultats = critereImpactService.afficherParEvaluation(selectedEvaluationId);
                if (resultats == null) resultats = java.util.Collections.emptyList();
            } else {
                evaluation = readEvaluationFromForm(false);
                if (evaluation == null) {
                    showError("Remplissez le formulaire ou sélectionnez une évaluation pour exporter.");
                    return;
                }
                resultats = collectResultatsFromFields();
                if (resultats == null || resultats.isEmpty()) {
                    showError("Ajoutez au moins un critère pour exporter.");
                    return;
                }
                evaluation.setScoreGlobal(calculateScore(resultats));
            }

            Models.AiSuggestion suggestion = advancedFacade.suggest(null, resultats);

            javafx.stage.FileChooser chooser = new javafx.stage.FileChooser();
            chooser.setTitle("Exporter l'évaluation en PDF");
            chooser.getExtensionFilters().add(new javafx.stage.FileChooser.ExtensionFilter("PDF", "*.pdf"));
            String baseName = "evaluation-" + (selectedEvaluationId != null ? selectedEvaluationId : evaluation.getIdProjet());
            chooser.setInitialFileName(baseName + ".pdf");
            javafx.stage.Window owner = (btnExportPdf != null && btnExportPdf.getScene() != null) ? btnExportPdf.getScene().getWindow() : null;
            java.io.File file = chooser.showSaveDialog(owner);
            if (file == null) return;

            byte[] signaturePng = getSignaturePng();
            String evaluatorName = (lblProfileName != null && lblProfileName.getText() != null && !lblProfileName.getText().isEmpty())
                    ? lblProfileName.getText() : "Utilisateur";
            String evaluatorRole = (lblProfileType != null && lblProfileType.getText() != null && !lblProfileType.getText().isEmpty())
                    ? lblProfileType.getText() : "Expert Carbone";

            boolean usedDynamic = false;
            try {
                Services.DynamicPdfService dynamic = new Services.DynamicPdfService();
                if (dynamic.isConfigured()) {
                    String html = dynamic.buildEvaluationHtml(evaluation, resultats, suggestion, signaturePng, evaluatorName, evaluatorRole);
                    dynamic.writePdfFromHtml(html, file);
                    usedDynamic = true;
                }
            } catch (Exception ex) {
                // Fall back silently
            }

            if (!usedDynamic) {
                new Services.PdfService().generateEvaluationPdfWithSignature(
                        evaluation, resultats, suggestion, file, signaturePng, evaluatorName, evaluatorRole
                );
            }

            Alert ok = new Alert(Alert.AlertType.INFORMATION);
            ok.setHeaderText("Export PDF reussi");
            ok.setContentText("[API CONFIG] DynamicPDF export completed\nFichier sauvegarde: " + file.getAbsolutePath());
            ok.showAndWait();

            System.out.println("[API CONFIG] DynamicPDF export completed -> " + file.getAbsolutePath());

        } catch (Exception ex) {
            showError("Echec export PDF: " + ex.getMessage());
        }
    }

    private byte[] getSignaturePng() {
        if (signatureCanvas == null || !signatureDrawn) return null;
        int w = (int) signatureCanvas.getWidth();
        int h = (int) signatureCanvas.getHeight();

        javafx.scene.image.WritableImage wi = new javafx.scene.image.WritableImage(w, h);
        signatureCanvas.snapshot(null, wi);

        javafx.scene.image.PixelReader pr = wi.getPixelReader();
        if (pr == null) return null;
        java.awt.image.BufferedImage bi = new java.awt.image.BufferedImage(w, h, java.awt.image.BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int argb = pr.getArgb(x, y);
                bi.setRGB(x, y, argb);
            }
        }

        try (java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream()) {
            javax.imageio.ImageIO.write(bi, "png", baos);
            return baos.toByteArray();
        } catch (java.io.IOException e) {
            return null;
        }
    }
}
