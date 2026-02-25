package Controllers;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.application.Platform;
import javafx.scene.layout.FlowPane;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.cell.PropertyValueFactory;
import Models.CritereReference;
import Models.Evaluation;
import Models.EvaluationResult;
import Models.Projet;
import Services.CritereImpactService;
import Services.EvaluationService;
import org.GreenLedger.MainFX;
import Services.ProjetService;
import Services.ProjectEsgService;
import Utils.SessionManager;
import Models.TypeUtilisateur;
import Models.User;


import java.io.IOException;
import java.sql.Timestamp;
import java.util.List;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

public class CarbonAuditController extends BaseController {

    private static Projet selectedProjet;
    private static Integer lastSelectedEvaluationId;

    public static void setSelectedProjet(Projet projet) {
        selectedProjet = projet;
    }

    @FXML private Button btnGestionProjets;

    @FXML private Button btnGestionEvaluations;

    @FXML private Button btnSettings;

    @FXML private ComboBox<String> comboProjet;
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

    private final EvaluationService evaluationService = new EvaluationService();
    private final ProjetService projetService = new ProjetService();
    private final CritereImpactService critereImpactService = new CritereImpactService();
    private final ProjectEsgService projectEsgService = new ProjectEsgService();
    private final Services.AdvancedEvaluationFacade advancedFacade = new Services.AdvancedEvaluationFacade();
    private final Services.CarbonReportService carbonReportService = new Services.CarbonReportService();

    private final ObservableList<CritereReference> referenceCriteres = FXCollections.observableArrayList();

    private Integer selectedEvaluationId;

    private final ToggleGroup decisionGroup = new ToggleGroup();

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
                    String label = selected.getId() + " - " + selected.getTitre();
                    if (txtIdProjet != null) {
                        txtIdProjet.setText(String.valueOf(selected.getId()));
                    }
                    if (comboProjet != null) {
                        comboProjet.getSelectionModel().select(label);
                    }
                }
            });
        }
        if (comboProjet != null) {
            comboProjet.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, selected) -> {
                if (selected != null) {
                    Integer extracted = extractLeadingNumber(selected);
                    if (extracted != null && txtIdProjet != null) {
                        txtIdProjet.setText(String.valueOf(extracted));
                    }
                    if (selectedEvaluationId == null) {
                        rebuildCriteriaFields(null);
                    }
                }
            });
        }

        selectProjetIfSet();

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
        if (comboProjet != null) {
            ObservableList<String> labels = FXCollections.observableArrayList();
            for (Projet projet : projets) {
                String statut = projet.getStatut();
                if (statut == null || !statut.equalsIgnoreCase("SUBMITTED")) {
                    continue;
                }
                labels.add(projet.getId() + " - " + projet.getTitre());
            }
            comboProjet.setItems(labels);
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
        Evaluation evaluation = readEvaluationFromForm(false);
        if (evaluation == null) {
            return;
        }
        List<EvaluationResult> resultats = collectResultatsFromFields();
        if (resultats == null || resultats.isEmpty()) {
            showError("Ajoutez au moins un critere avant de creer l'evaluation.");
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
            showError("Creation evaluation echouee.");
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
                        // Create a carbon report for this evaluation
                        Models.CarbonReport carbonReport = carbonReportService.createReport(
                            (long) proj.getId(),
                            proj.getTitre(),
                            (long) proj.getEntrepriseId(),
                            proj.getCompanyEmail() != null ? proj.getCompanyEmail() : "Unknown"
                        );
                        
                        // Enrich with external API data (graceful degradation)
                        if (carbonReport != null) {
                            carbonReportService.enrichWithExternalData(carbonReport, proj);
                            
                            // Display enrichment info in AI insights area
                            if (txtAIInsights != null && carbonReport.getEvaluationDetails() != null) {
                                String currentText = txtAIInsights.getText();
                                txtAIInsights.setText(currentText + "\n\n" + carbonReport.getEvaluationDetails());
                            }
                            
                            System.out.println("[AUDIT CONTROLLER] ✓ External API data integrated");
                        }
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
    }

    @FXML
    void modifierEvaluation() {
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
        String decision = decisionFromSelection();
        Integer idProjet = parseInt(txtIdProjet.getText(), "ID Projet");

        if (observations == null || decision == null || idProjet == null) {
            return null;
        }

        String statut = projetService.getStatutById(idProjet);
        if (statut != null && statut.trim().equalsIgnoreCase("CANCELLED")) {
            showError("Impossible d'evaluer un projet cancelled.");
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
        clearDecisionSelection();
    }

    private String decisionFromSelection() {
        if (chkDecisionApproved == null || chkDecisionRejected == null) {
            showError("Decision manquante.");
            return null;
        }
        if (chkDecisionApproved.isSelected() == chkDecisionRejected.isSelected()) {
            showError("Selectionnez une seule decision.");
            return null;
        }
        return chkDecisionApproved.isSelected() ? "Approuve" : "Rejete";
    }

    private void enforceSingleDecision() {
        // ToggleGroup already enforces single selection.
        chkDecisionApproved.setToggleGroup(decisionGroup);
        chkDecisionRejected.setToggleGroup(decisionGroup);

        if (chkDecisionApproved != null) {
            chkDecisionApproved.setDisable(false);
        }
        if (chkDecisionRejected != null) {
            chkDecisionRejected.setDisable(false);
        }
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

    private Integer parseInt(String text, String fieldName) {
        try {
            return Integer.parseInt(text.trim());
        } catch (NumberFormatException ex) {
            showError(fieldName + " invalide.");
            return null;
        }
    }

    /**
     * Poids: entier entre 1 et 10.
     */
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

    private double calculateScore(java.util.List<EvaluationResult> criteres) {
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
            if (noteField == null) {
                continue;
            }
            String text = noteField.getText() == null ? "" : noteField.getText().trim();
            if (text.isEmpty()) {
                continue;
            }
            try {
                int note = Integer.parseInt(text);
                if (note < 1 || note > 10) {
                    continue;
                }
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

    /**
     * Collecte des résultats depuis les champs dynamiques.
     * On applique ici la validation requise :
     * - note obligatoire et entre 1 et 10
     * - commentaire technique entre 8 et 250
     */
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
            // Commentaire technique : min 8 max 250
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

            // TextFormatter pour n'accepter que des chiffres (0-99)
            UnaryOperator<TextFormatter.Change> integerFilter = change -> {
                String newText = change.getControlNewText();
                if (newText.matches("\\d{0,2}")) {
                    return change;
                }
                return null;
            };
            noteField.setTextFormatter(new TextFormatter<>(integerFilter));

            // Validation visuelle à la saisie (on marque si hors bornes)
            noteField.textProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal == null || newVal.trim().isEmpty()) {
                    markInvalid(noteField, false); // allow empty until submission
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

            // Limiter longueur et validation visuelle
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

    /**
     * Setup input constraints for static form fields
     */
    private void setupStaticInputConstraints() {
        // This method can be expanded to add constraints to static fields
        // Currently, dynamic fields have their own constraints in rebuildCriteriaFields
    }

    /**
     * Mark a TextField as invalid by applying CSS style
     */
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

    /**
     * Mark a TextArea as invalid by applying CSS style
     */
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

    /**
     * Limit text length in TextArea
     */
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

    private void updateDecisionAvailability() {
        // Decision is always enabled; no gating.
    }

    private boolean areCriteriaComplete() {
        return true;
    }

    private void applyEvaluationToForm(Evaluation selected) {
        if (selected == null) {
            return;
        }
        if (txtObservations != null) {
            txtObservations.setText(selected.getObservations());
        }
        setDecisionCheckboxes(selected.getDecision());
        if (txtIdProjet != null) {
            txtIdProjet.setText(String.valueOf(selected.getIdProjet()));
        }
        if (txtScoreFinal != null) {
            txtScoreFinal.setText(formatScore(selected.getScoreGlobal()));
        }
        selectedEvaluationId = selected.getIdEvaluation();
        lastSelectedEvaluationId = selectedEvaluationId;
        rebuildCriteriaFields(selectedEvaluationId);
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

    @FXML
    void handleClearSignature() {
        if (sigGc != null && signatureCanvas != null) {
            sigGc.clearRect(0, 0, signatureCanvas.getWidth(), signatureCanvas.getHeight());
            signatureDrawn = false;
        }
    }

    private byte[] getSignaturePng() {
        if (signatureCanvas == null || !signatureDrawn) return null;
        int w = (int) signatureCanvas.getWidth();
        int h = (int) signatureCanvas.getHeight();

        javafx.scene.image.WritableImage wi = new javafx.scene.image.WritableImage(w, h);
        signatureCanvas.snapshot(null, wi);

        // Manually convert JavaFX image to AWT BufferedImage (no javafx.embed.swing dependency)
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

    @FXML
    void handleExportPdf() {
        try {
            Evaluation evaluation;
            java.util.List<EvaluationResult> resultats;
            if (selectedEvaluationId != null) {
                // Utiliser l'évaluation sélectionnée dans le tableau
                evaluation = tableAudits != null ? tableAudits.getSelectionModel().getSelectedItem() : null;
                if (evaluation == null) {
                    // fallback: rechercher par id
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
                // Exporter depuis le formulaire courant si complet
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

            // Suggestion IA (facultatif mais utile dans le PDF)
            Models.AiSuggestion suggestion = advancedFacade.suggest(null, resultats);

            // Choisir l'emplacement du fichier
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

            new Services.PdfService().generateEvaluationPdfWithSignature(
                    evaluation, resultats, suggestion, file, signaturePng, evaluatorName, evaluatorRole
            );

            // After local PDF generation: call remote extraction (pdfrest) using PdfRestService.
            // Run extraction in background thread to avoid blocking UI.
            final java.io.File exported = file;
            final String apiMsgPrefix = "Extraction distante: ";
            new Thread(() -> {
                try {
                    // Prefer Adobe service if configured
                    Services.AdobePdfService adobe = new Services.AdobePdfService();
                    String extracted = null;
                    if (adobe.isConfigured()) {
                        try {
                            extracted = adobe.extractTextFromFilePath(exported.getAbsolutePath());
                        } catch (Exception adEx) {
                            System.err.println("Adobe extraction failed: " + adEx.getMessage());
                        }
                    }
                    if (extracted == null || extracted.isEmpty()) {
                        // fallback to PdfRestService
                        try {
                            Services.PdfRestService rest = new Services.PdfRestService();
                            extracted = rest.extractTextFromFilePath(exported.getAbsolutePath());
                        } catch (Exception prEx) {
                            System.err.println("PdfRest extraction failed: " + prEx.getMessage());
                        }
                    }
                    final String display = (extracted == null || extracted.isEmpty()) ? "(Aucun texte extrait)" : extracted;
                    Platform.runLater(() -> {
                        // Populate AI insights area with extracted text summary
                        if (txtAIInsights != null) {
                            String previous = txtAIInsights.getText() == null ? "" : txtAIInsights.getText();
                            String added = (previous.isEmpty() ? "" : (previous + "\n\n")) + "[Extraction PDF] \n" + display;
                            txtAIInsights.setText(added);
                        }
                        Alert info = new Alert(Alert.AlertType.INFORMATION);
                        info.setTitle("Extraction PDF");
                        info.setHeaderText("Extraction terminée");
                        javafx.scene.control.TextArea ta = new javafx.scene.control.TextArea(display);
                        ta.setWrapText(true);
                        ta.setEditable(false);
                        ta.setPrefWidth(600);
                        ta.setPrefHeight(400);
                        info.getDialogPane().setContent(ta);
                        info.showAndWait();
                    });
                } catch (Exception ex) {
                    final String msg = ex.getMessage();
                    Platform.runLater(() -> {
                        showError(apiMsgPrefix + (msg == null ? "Erreur inconnue" : msg));
                    });
                }
            }).start();

            Alert ok = new Alert(Alert.AlertType.INFORMATION);
            ok.setHeaderText("Export PDF réussi");
            ok.setContentText("Fichier sauvegardé: " + file.getAbsolutePath() + "\nL'extraction distante est en cours...");
            ok.showAndWait();

        } catch (Exception ex) {
            showError("Échec export PDF: " + ex.getMessage());
        }
    }

}
