package Controllers;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.application.Platform;
import javafx.scene.layout.FlowPane;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.cell.PropertyValueFactory;
import Models.CritereImpact;
import Models.Evaluation;
import Models.Projet;
import Services.CritereImpactService;
import Services.EvaluationService;
import org.GreenLedger.MainFX;
import Services.ProjetService;


import java.io.IOException;
import java.sql.Timestamp;

public class CarbonAuditController extends BaseController {

    private static Projet selectedProjet;

    public static void setSelectedProjet(Projet projet) {
        selectedProjet = projet;
    }

    @FXML private Button btnGestionProjets;

    @FXML private Button btnGestionEvaluations;

    @FXML private Button btnSettings;

    @FXML private ComboBox<String> comboProjet;
    @FXML private TableView<Evaluation> tableAudits;
    @FXML private TableView<Projet> tableProjets;
    @FXML private TableView<CritereImpact> tableCriteres;

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

    @FXML private TableColumn<CritereImpact, String> colCritereNom;
    @FXML private TableColumn<CritereImpact, Number> colCritereNote;
    @FXML private TableColumn<CritereImpact, String> colCritereCommentaire;

    @FXML private TextArea txtObservations;
    @FXML private TextField txtIdProjet;
    @FXML private CheckBox chkDecisionApproved;
    @FXML private CheckBox chkDecisionRejected;

    @FXML private FlowPane flowCriteres;
    @FXML private TextField txtNomCritere;
    @FXML private TextArea txtCommentaireCritere;
    @FXML private TextField txtNote;

    @FXML private Label lblProjetsAudit;
    @FXML private Label lblProjetsEvalues;
    @FXML private Label lblCriteresImpact;

    private final EvaluationService evaluationService = new EvaluationService();
    private final ProjetService projetService = new ProjetService();
    private final CritereImpactService critereImpactService = new CritereImpactService();

    private Integer selectedEvaluationId;

    @FXML
    public void initialize() {
        super.initialize(); // Enable theme switching

        setActiveNav(btnGestionEvaluations);
        if (btnGestionEvaluations != null) {
            Platform.runLater(() -> btnGestionEvaluations.requestFocus());
        }

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
            colCritereNom.setCellValueFactory(new PropertyValueFactory<>("nom"));
            colCritereNote.setCellValueFactory(new PropertyValueFactory<>("note"));
            colCritereCommentaire.setCellValueFactory(new PropertyValueFactory<>("commentaireTechnique"));
        }

        // Wrap long text so it remains readable within the available width.
        applyWrapping(colObservations);
        applyWrapping(colProjetNom);
        applyWrapping(colProjetDescription);
        applyWrapping(colCritereCommentaire);

        refreshProjets();
        refreshEvaluations();
        if (tableAudits != null) {
            tableAudits.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, selected) -> {
                if (selected != null) {
                    if (txtObservations != null) {
                        txtObservations.setText(selected.getObservations());
                    }
                    setDecisionCheckboxes(selected.getDecision());
                    if (txtIdProjet != null) {
                        txtIdProjet.setText(String.valueOf(selected.getIdProjet()));
                    }
                    selectedEvaluationId = selected.getIdEvaluation();
                    refreshCriteres();
                }
            });
        }
        if (tableCriteres != null) {
            tableCriteres.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, selected) -> {
                if (selected != null) {
                    txtNomCritere.setText(selected.getNom());
                    txtNote.setText(String.valueOf(selected.getNote()));
                    txtCommentaireCritere.setText(selected.getCommentaireTechnique());
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
                }
            });
        }

        selectProjetIfSet();
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
        if (tableAudits != null) {
            tableAudits.setItems(evaluations);
            tableAudits.refresh();
        }
    }

    private void refreshCriteres() {
        if (tableCriteres == null) {
            return;
        }
        if (selectedEvaluationId == null) {
            tableCriteres.setItems(FXCollections.observableArrayList());
            updateCritereStats(0);
            return;
        }
        ObservableList<CritereImpact> criteres = FXCollections.observableArrayList(
                critereImpactService.afficherParEvaluation(selectedEvaluationId)
        );
        tableCriteres.setItems(criteres);
        updateCritereStats(criteres.size());
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
        if (selectedProjet == null || comboProjet == null) {
            return;
        }
        String label = selectedProjet.getId() + " - " + selectedProjet.getTitre();
        comboProjet.getSelectionModel().select(label);
        if (txtIdProjet != null) {
            txtIdProjet.setText(String.valueOf(selectedProjet.getId()));
        }
    }

    @FXML
    void ajouterEvaluation() {
        Evaluation evaluation = readEvaluationFromForm(false);
        if (evaluation == null) {
            return;
        }
        evaluationService.ajouter(evaluation);
        refreshEvaluations();
        refreshProjets();
        clearEvaluationForm();
    }

    @FXML
    void modifierEvaluation() {
        Evaluation evaluation = readEvaluationFromForm(true);
        if (evaluation == null) {
            return;
        }
        evaluationService.modifier(evaluation);
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
        clearEvaluationForm();
    }

    @FXML
    void ajouterCritere() {
        if (selectedEvaluationId == null) {
            showError("Selectionnez une evaluation.");
            return;
        }
        String nom = requireLength(txtNomCritere, "Nom du critere", 10, 50);
        String commentaire = requireText(txtCommentaireCritere, "Commentaire technique");
        Integer note = requireNote(txtNote.getText());
        if (nom == null || commentaire == null || note == null) {
            return;
        }
        CritereImpact critere = new CritereImpact(
                nom,
                note,
                commentaire,
                selectedEvaluationId
        );
        critereImpactService.ajouter(critere);
        refreshCriteres();
        clearCritereForm();
    }

    @FXML
    void modifierCritere() {
        CritereImpact selected = tableCriteres != null ? tableCriteres.getSelectionModel().getSelectedItem() : null;
        if (selected == null) {
            showError("Selectionnez un critere.");
            return;
        }
        String nom = requireLength(txtNomCritere, "Nom du critere", 10, 50);
        String commentaire = requireText(txtCommentaireCritere, "Commentaire technique");
        Integer note = requireNote(txtNote.getText());
        if (nom == null || commentaire == null || note == null) {
            return;
        }
        selected.setNom(nom);
        selected.setNote(note);
        selected.setCommentaireTechnique(commentaire);
        critereImpactService.modifier(selected);
        refreshCriteres();
    }

    @FXML
    void supprimerCritere() {
        CritereImpact selected = tableCriteres != null ? tableCriteres.getSelectionModel().getSelectedItem() : null;
        if (selected == null) {
            showError("Selectionnez un critere.");
            return;
        }
        critereImpactService.supprimer(selected.getIdCritere());
        refreshCriteres();
        clearCritereForm();
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
        clearDecisionSelection();
    }

    private void clearCritereForm() {
        txtNomCritere.clear();
        txtNote.clear();
        txtCommentaireCritere.clear();
    }

    private String decisionFromSelection() {
        if (chkDecisionApproved == null || chkDecisionRejected == null) {
            showError("Decision manquante.");
            return null;
        }
        boolean approved = chkDecisionApproved.isSelected();
        boolean rejected = chkDecisionRejected.isSelected();
        if (approved == rejected) {
            showError("Selectionnez une seule decision.");
            return null;
        }
        return approved ? "Approuve" : "Rejete";
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
        if (chkDecisionApproved != null) {
            chkDecisionApproved.setSelected(false);
        }
        if (chkDecisionRejected != null) {
            chkDecisionRejected.setSelected(false);
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
        if (note != 0 && note != 1) {
            showError("Note doit etre 0 ou 1.");
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
}
