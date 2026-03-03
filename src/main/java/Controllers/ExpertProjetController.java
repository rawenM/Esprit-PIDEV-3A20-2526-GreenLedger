package Controllers;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.util.Callback;
import Models.Projet;
import org.GreenLedger.MainFX;
import Services.ProjetService;

<<<<<<< HEAD
=======
<<<<<<< HEAD
=======
>>>>>>> yassine_antar
import Services.EvaluationService;
import Models.Evaluation;
import Utils.SessionManager;
import Models.TypeUtilisateur;
import Models.User;

<<<<<<< HEAD
=======
>>>>>>> f3559248f463304c68513eb2c92f99791d2c4657
>>>>>>> yassine_antar

import java.io.IOException;

public class ExpertProjetController extends BaseController {

    @FXML
    private Button btnGestionProjets;

    @FXML
    private Button btnGestionEvaluations;

    @FXML
    private Button btnSettings;

    @FXML
    private TableView<Projet> tableProjets;

    @FXML
    private TableColumn<Projet, Integer> colId;

    @FXML
    private TableColumn<Projet, String> colTitre;

    @FXML
    private TableColumn<Projet, String> colDescription;

    @FXML
    private TableColumn<Projet, Double> colBudget;

    @FXML
    private TableColumn<Projet, String> colStatut;

    @FXML
    private TableColumn<Projet, String> colScore;

    @FXML
    private TableColumn<Projet, Void> colAction;

    @FXML
    private TableColumn<Projet, Void> colIcon;

    @FXML
<<<<<<< HEAD
    private TableColumn<Projet, Void> colDecision;
=======
<<<<<<< HEAD
=======
    private TableColumn<Projet, Void> colAi;
>>>>>>> yassine_antar

    @FXML
    private TableColumn<Projet, Void> colPdf;

    @FXML
    private TableColumn<Projet, Void> colCalcEsg;

    @FXML
<<<<<<< HEAD
=======
>>>>>>> f3559248f463304c68513eb2c92f99791d2c4657
>>>>>>> yassine_antar
    private Label lblTotal;

    @FXML
    private Label lblPending;

    @FXML
    private Label lblEvaluated;

<<<<<<< HEAD
    @FXML private Label lblProfileName;
    @FXML private Label lblProfileType;

    private final ObservableList<Projet> data = FXCollections.observableArrayList();
    private final ProjetService projetService = new ProjetService();
    private final EvaluationService evaluationService = new EvaluationService();
=======
<<<<<<< HEAD
    private final ObservableList<Projet> data = FXCollections.observableArrayList();
    private final ProjetService projetService = new ProjetService();
=======
    @FXML private Label lblProfileName;
    @FXML private Label lblProfileType;

    private final ObservableList<Projet> data = FXCollections.observableArrayList();
    private final ProjetService projetService = new ProjetService();
    private final EvaluationService evaluationService = new EvaluationService();
>>>>>>> f3559248f463304c68513eb2c92f99791d2c4657
>>>>>>> yassine_antar

    @FXML
    public void initialize() {
        super.initialize();

<<<<<<< HEAD
        applyProfile(lblProfileName, lblProfileType);

=======
<<<<<<< HEAD
=======
        applyProfile(lblProfileName, lblProfileType);

>>>>>>> f3559248f463304c68513eb2c92f99791d2c4657
>>>>>>> yassine_antar
        setActiveNav(btnGestionProjets);

        if (btnGestionProjets != null) {
            btnGestionProjets.setOnAction(event -> showGestionProjets());
        }
        if (btnGestionEvaluations != null) {
            btnGestionEvaluations.setOnAction(event -> showGestionEvaluations());
        }
        if (btnSettings != null) {
            btnSettings.setOnAction(event -> showSettings());
        }

        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colTitre.setCellValueFactory(new PropertyValueFactory<>("titre"));
        colDescription.setCellValueFactory(new PropertyValueFactory<>("description"));
        colBudget.setCellValueFactory(new PropertyValueFactory<>("budget"));
        colStatut.setCellValueFactory(cellData -> new SimpleStringProperty(
                cellData.getValue().getStatutEvaluation() == null || cellData.getValue().getStatutEvaluation().isEmpty()
                        ? "En attente"
                        : cellData.getValue().getStatutEvaluation()
        ));
<<<<<<< HEAD
        colScore.setCellValueFactory(cellData -> {
            Integer score = cellData.getValue().getScoreEsg();
            String label = (score == null || score <= 0) ? "Pending" : String.valueOf(score);
            return new SimpleStringProperty(label);
        });
        colAction.setCellFactory(createActionCell());
        colDecision.setCellFactory(createDecisionCell());
        colPdf.setCellFactory(createPdfCell());
=======
<<<<<<< HEAD
        colScore.setCellValueFactory(cellData -> new SimpleStringProperty(
                cellData.getValue().getScoreEsg() <= 0 ? "Pending" : String.valueOf(cellData.getValue().getScoreEsg())
        ));
        colAction.setCellFactory(createActionCell());
=======
        colScore.setCellValueFactory(cellData -> {
            Integer score = cellData.getValue().getScoreEsg();
            String label = (score == null || score <= 0) ? "Pending" : String.valueOf(score);
            return new SimpleStringProperty(label);
        });
        colAction.setCellFactory(createActionCell());
        colAi.setCellFactory(createAiCell());
        colPdf.setCellFactory(createPdfCell());
        colCalcEsg.setCellFactory(createCalcEsgCell());
>>>>>>> f3559248f463304c68513eb2c92f99791d2c4657
>>>>>>> yassine_antar
        colIcon.setCellFactory(column -> new TableCell<>() {
            private final Label icon = new Label(">>");
            {
                icon.getStyleClass().add("row-icon");
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : icon);
            }
        });

        tableProjets.setItems(data);
        refreshTable();
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

    private void showSettings() {
        try {
            MainFX.setRoot("settings");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private Callback<TableColumn<Projet, Void>, TableCell<Projet, Void>> createActionCell() {
        return column -> new TableCell<>() {
            private final Button button = new Button();
            {
                button.getStyleClass().addAll("btn", "btn-primary");
                button.setOnAction(event -> {
                    Projet projet = getTableView().getItems().get(getIndex());
                    try {
                        java.util.List<Evaluation> evals = evaluationService.afficherParProjet(projet.getId());
                        if (evals != null && !evals.isEmpty()) {
                            Evaluation last = evals.get(evals.size() - 1);
                            CarbonAuditController.setLastSelectedEvaluationId(last.getIdEvaluation());
                        } else {
                            CarbonAuditController.setLastSelectedEvaluationId(null);
                        }
                        CarbonAuditController.setSelectedProjet(projet);
                        MainFX.setRoot("gestionCarbone");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                    return;
                }
                Projet projet = getTableView().getItems().get(getIndex());
                boolean hasEvaluation = false;
                try {
                    java.util.List<Evaluation> evals = evaluationService.afficherParProjet(projet.getId());
                    hasEvaluation = evals != null && !evals.isEmpty();
                } catch (Exception ignore) { }
                button.setText(hasEvaluation ? "Modifier" : "Evaluer");
                setGraphic(button);
            }
        };
    }

    private Callback<TableColumn<Projet, Void>, TableCell<Projet, Void>> createPdfCell() {
        return column -> new TableCell<>() {
            private final Button btn = new Button("PDF");
            {
                btn.getStyleClass().addAll("btn", "btn");
                btn.setOnAction(event -> {
                    Projet p = getTableView().getItems().get(getIndex());
                    try {
                        java.util.List<Models.Evaluation> evals = evaluationService.afficherParProjet(p.getId());
                        if (evals == null || evals.isEmpty()) {
                            javafx.scene.control.Alert a = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.INFORMATION);
                            a.setHeaderText("Export PDF");
                            a.setContentText("Aucune évaluation trouvée pour ce projet.");
                            a.showAndWait();
                            return;
                        }
                        Models.Evaluation last = evals.get(evals.size() - 1);
                        java.util.List<Models.EvaluationResult> res = new Services.CritereImpactService().afficherParEvaluation(last.getIdEvaluation());
                        if (res == null) res = java.util.Collections.emptyList();

                        Models.AiSuggestion s = new Services.AdvancedEvaluationFacade().suggest(p, res);

                        javafx.stage.FileChooser chooser = new javafx.stage.FileChooser();
                        chooser.setTitle("Exporter l'évaluation en PDF");
                        chooser.getExtensionFilters().add(new javafx.stage.FileChooser.ExtensionFilter("PDF", "*.pdf"));
                        chooser.setInitialFileName("evaluation-" + last.getIdEvaluation() + ".pdf");
                        javafx.stage.Window owner = (btn.getScene() != null) ? btn.getScene().getWindow() : null;
                        java.io.File file = chooser.showSaveDialog(owner);
                        if (file == null) return;

                        new Services.PdfService().generateEvaluationPdf(last, res, s, file);

                        javafx.scene.control.Alert ok = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.INFORMATION);
                        ok.setHeaderText("Export PDF réussi");
                        ok.setContentText("Fichier sauvegardé: " + file.getAbsolutePath());
                        ok.showAndWait();

                    } catch (Exception ex) {
                        javafx.scene.control.Alert err = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR);
                        err.setHeaderText("Export PDF échoué");
                        err.setContentText(ex.getMessage());
                        err.showAndWait();
                    }
                });
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : btn);
            }
        };
    }

    private Callback<TableColumn<Projet, Void>, TableCell<Projet, Void>> createDecisionCell() {
        return column -> new TableCell<>() {
            private final Button btn = new Button("Check Decision");
            {
                btn.getStyleClass().addAll("btn", "btn-secondary");
                btn.setOnAction(event -> {
                    Projet p = getTableView().getItems().get(getIndex());
                    try {
                        CarbonAuditController.setSelectedProjet(p);
                        MainFX.setRoot("mlDecision");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : btn);
            }
        };
    }

<<<<<<< HEAD
    private void refreshTable() {
<<<<<<< HEAD
=======
        data.setAll(projetService.afficher());
=======
    private Callback<TableColumn<Projet, Void>, TableCell<Projet, Void>> createPdfCell() {
        return column -> new TableCell<>() {
            private final Button btn = new Button("PDF");
            {
                btn.getStyleClass().addAll("btn", "btn");
                btn.setOnAction(event -> {
                    Projet p = getTableView().getItems().get(getIndex());
                    try {
                        java.util.List<Models.Evaluation> evals = evaluationService.afficherParProjet(p.getId());
                        if (evals == null || evals.isEmpty()) {
                            javafx.scene.control.Alert a = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.INFORMATION);
                            a.setHeaderText("Export PDF");
                            a.setContentText("Aucune évaluation trouvée pour ce projet.");
                            a.showAndWait();
                            return;
                        }
                        Models.Evaluation last = evals.get(evals.size() - 1);
                        java.util.List<Models.EvaluationResult> res = new Services.CritereImpactService().afficherParEvaluation(last.getIdEvaluation());
                        if (res == null) res = java.util.Collections.emptyList();

                        Models.AiSuggestion s = new Services.AdvancedEvaluationFacade().suggest(p, res);

                        javafx.stage.FileChooser chooser = new javafx.stage.FileChooser();
                        chooser.setTitle("Exporter l'évaluation en PDF");
                        chooser.getExtensionFilters().add(new javafx.stage.FileChooser.ExtensionFilter("PDF", "*.pdf"));
                        chooser.setInitialFileName("evaluation-" + last.getIdEvaluation() + ".pdf");
                        javafx.stage.Window owner = (btn.getScene() != null) ? btn.getScene().getWindow() : null;
                        java.io.File file = chooser.showSaveDialog(owner);
                        if (file == null) return;

                        new Services.PdfService().generateEvaluationPdf(last, res, s, file);

                        javafx.scene.control.Alert ok = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.INFORMATION);
                        ok.setHeaderText("Export PDF réussi");
                        ok.setContentText("Fichier sauvegardé: " + file.getAbsolutePath());
                        ok.showAndWait();

                    } catch (Exception ex) {
                        javafx.scene.control.Alert err = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR);
                        err.setHeaderText("Export PDF échoué");
                        err.setContentText(ex.getMessage());
                        err.showAndWait();
                    }
                });
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : btn);
            }
        };
    }

    private Callback<TableColumn<Projet, Void>, TableCell<Projet, Void>> createAiCell() {
        return column -> new TableCell<>() {
            private final Button btn = new Button("IA");
            {
                btn.getStyleClass().addAll("btn", "btn-secondary");
                btn.setOnAction(event -> {
                    Projet p = getTableView().getItems().get(getIndex());
                    // Récupérer la dernière évaluation du projet
                    java.util.List<Models.Evaluation> evals = evaluationService.afficherParProjet(p.getId());
                    if (evals == null || evals.isEmpty()) {
                        javafx.scene.control.Alert a = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.INFORMATION);
                        a.setHeaderText("Suggestion IA");
                        a.setContentText("Aucune évaluation trouvée pour ce projet.");
                        a.showAndWait();
                        return;
                    }
                    Models.Evaluation last = evals.get(evals.size() - 1);
                    java.util.List<Models.EvaluationResult> res = new Services.CritereImpactService().afficherParEvaluation(last.getIdEvaluation());
                    if (res == null || res.isEmpty()) {
                        javafx.scene.control.Alert a = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.INFORMATION);
                        a.setHeaderText("Suggestion IA");
                        a.setContentText("Aucun résultat de critères pour l'évaluation #" + last.getIdEvaluation());
                        a.showAndWait();
                        return;
                    }
                    Models.AiSuggestion s = new Services.AdvancedEvaluationFacade().suggest(p, res);
                    StringBuilder msg = new StringBuilder();
                    msg.append("Suggestion: ").append(s.getSuggestionDecision())
                            .append(" • Confiance: ").append(String.format(java.util.Locale.ROOT, "%.2f", s.getConfiance()))
                            .append(" • Score: ").append(String.format(java.util.Locale.ROOT, "%.2f", s.getScore()));
                    if (!s.getTopFactors().isEmpty()) {
                        msg.append("\n\nFacteurs clés:\n");
                        for (String f : s.getTopFactors()) {
                            msg.append(" • ").append(f).append("\n");
                        }
                    }
                    if (!s.getWarnings().isEmpty()) {
                        msg.append("\nAvertissements:\n");
                        for (String w : s.getWarnings()) {
                            msg.append(" • ").append(w).append("\n");
                        }
                    }
                    javafx.scene.control.Alert a = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.INFORMATION);
                    a.setHeaderText("Suggestion IA – Projet #" + p.getId());
                    a.setContentText(msg.toString().trim());
                    a.showAndWait();
                });
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : btn);
            }
        };
    }

    private Callback<TableColumn<Projet, Void>, TableCell<Projet, Void>> createCalcEsgCell() {
        return column -> new TableCell<>() {
            private final Button btn = new Button("Calc ESG");
            {
                btn.getStyleClass().addAll("btn", "btn");
                btn.setOnAction(event -> {
                    Projet p = getTableView().getItems().get(getIndex());
                    try {
                        Integer esg = new Services.ProjectEsgService().calculateEsgForProject(p.getId());
                        if (esg == null) {
                            javafx.scene.control.Alert a = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.INFORMATION);
                            a.setHeaderText("Calcul ESG");
                            a.setContentText("Impossible de calculer le score ESG (aucune évaluation/critères).");
                            a.showAndWait();
                            return;
                        }
                        p.setScoreEsg(esg);
                        // Persister si possible
                        try {
                            new Services.ProjetService().update(p);
                        } catch (Exception ignored) { }
                        // Rafraîchir l'affichage de la colonne Score ESG
                        getTableView().refresh();

                        javafx.scene.control.Alert ok = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.INFORMATION);
                        ok.setHeaderText("Score ESG mis à jour");
                        ok.setContentText("Projet #" + p.getId() + " • ESG = " + esg);
                        ok.showAndWait();
                    } catch (Exception ex) {
                        javafx.scene.control.Alert err = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR);
                        err.setHeaderText("Échec calcul ESG");
                        err.setContentText(ex.getMessage());
                        err.showAndWait();
                    }
                });
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : btn);
            }
        };
    }

    private void refreshTable() {
>>>>>>> yassine_antar
        java.util.List<Projet> projets = projetService.afficher();
        java.util.List<Projet> submitted = new java.util.ArrayList<>();
        for (Projet projet : projets) {
            String statut = projet.getStatut();
            if (statut != null && statut.equalsIgnoreCase("SUBMITTED")) {
                submitted.add(projet);
            }
        }
        data.setAll(submitted);
<<<<<<< HEAD
=======
>>>>>>> f3559248f463304c68513eb2c92f99791d2c4657
>>>>>>> yassine_antar
        updateStats();
    }

    private void updateStats() {
        long total = data.size();
<<<<<<< HEAD
=======
<<<<<<< HEAD
        long pending = data.stream().filter(p -> {
            String s = p.getStatutEvaluation();
            return s == null || s.isEmpty() || s.equalsIgnoreCase("En attente");
        }).count();
        long evaluated = total - pending;
=======
>>>>>>> yassine_antar
        java.util.Set<Integer> evaluatedIds = new java.util.HashSet<>();
        java.util.List<Evaluation> evaluations = evaluationService.afficher();
        if (evaluations != null) {
            for (Evaluation evaluation : evaluations) {
                evaluatedIds.add(evaluation.getIdProjet());
            }
        }
        long evaluated = data.stream().filter(p -> evaluatedIds.contains(p.getId())).count();
        long pending = Math.max(0, total - evaluated);
<<<<<<< HEAD
=======
>>>>>>> f3559248f463304c68513eb2c92f99791d2c4657
>>>>>>> yassine_antar

        lblTotal.setText(String.valueOf(total));
        lblPending.setText(String.valueOf(pending));
        lblEvaluated.setText(String.valueOf(evaluated));
    }
<<<<<<< HEAD

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
=======
<<<<<<< HEAD
>>>>>>> yassine_antar
}
=======

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
}
>>>>>>> f3559248f463304c68513eb2c92f99791d2c4657
