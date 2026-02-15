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


import java.io.IOException;
import java.sql.Timestamp;
import java.util.List;

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

    private final EvaluationService evaluationService = new EvaluationService();
    private final ProjetService projetService = new ProjetService();
    private final CritereImpactService critereImpactService = new CritereImpactService();

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
    }

    private void updateProjetStats(ObservableList<Projet> projets) {
        if (lblProjetsAudit == null || lblProjetsEvalues == null) {
            return;
        }
        long pending = projets.stream().filter(p -> {
            String s = p.getStatutEvaluation();
            return s == null || s.isEmpty() || s.equalsIgnoreCase("En attente");
        }).count();
        long evaluated = projets.size() - pending;
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
        int createdId = evaluationService.ajouterAvecCriteres(evaluation, resultats);
        if (createdId <= 0) {
            showError("Creation evaluation echouee.");
            return;
        }
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
        evaluationService.modifier(evaluation);
        critereImpactService.modifierResultats(evaluation.getIdEvaluation(), resultats);
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
        evaluationService.supprimer(id);
        selectedEvaluationId = null;
        refreshEvaluations();
        refreshProjets();
        refreshCriteres();
        rebuildCriteriaFields(null);
        clearEvaluationForm();
    }

    @FXML
    void ajouterCritere() {
        String nom = requireLength(txtNomCritere, "Nom du critere", 3, 100);
        String description = requireText(txtCommentaireCritere, "Description");
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
        String nom = requireLength(txtNomCritere, "Nom du critere", 3, 100);
        String description = requireText(txtCommentaireCritere, "Description");
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
        txtNomCritere.clear();
        txtNote.clear();
        txtCommentaireCritere.clear();
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
        int totalWeight = 0;
        int weightedSum = 0;
        for (EvaluationResult critere : criteres) {
            int poids = getPoidsForCritere(critere.getIdCritere());
            weightedSum += critere.getNote() * poids;
            totalWeight += poids;
        }
        return totalWeight == 0 ? 0.0 : weightedSum / (double) totalWeight;
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
        int totalWeight = 0;
        int weightedSum = 0;
        for (javafx.scene.Node node : criteriaFieldsBox.getChildren()) {
            if (!(node instanceof javafx.scene.layout.HBox)) {
                continue;
            }
            javafx.scene.layout.HBox row = (javafx.scene.layout.HBox) node;
            Integer idCritere = (Integer) row.getProperties().get("critereId");
            javafx.scene.control.TextField noteField = (javafx.scene.control.TextField) row.getProperties().get("noteField");
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
                weightedSum += note * poids;
                totalWeight += poids;
            } catch (NumberFormatException ignore) {
                // ignore invalid values in preview
            }
        }
        return totalWeight == 0 ? 0.0 : weightedSum / (double) totalWeight;
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
            String commentaire = requireText(commentField, "Commentaire technique");
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

            javafx.scene.control.TextArea commentField = new javafx.scene.control.TextArea();
            commentField.setPromptText("Commentaire technique");
            commentField.getStyleClass().addAll("field", "textarea");
            commentField.setPrefRowCount(2);

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

    private void updateScorePreview() {
        if (txtScoreFinal == null) {
            return;
        }
        double score = calculateScoreFromFieldsLenient();
        txtScoreFinal.setText(score > 0 ? formatScore(score) : "");
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

}
