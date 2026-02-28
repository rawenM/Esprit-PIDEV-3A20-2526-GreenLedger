package Controllers;


import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import Models.FraudDetectionResult;
import Models.StatutUtilisateur;
import Models.TypeUtilisateur;
import Models.User;
import Services.IUserService;
import Services.UserServiceImpl;
import Utils.SessionManager;
import dao.FraudDetectionDAOImpl;
import dao.IFraudDetectionDAO;
import org.GreenLedger.MainFX;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

public class AdminUsersController {

    @FXML private TableView<User> usersTable;
    @FXML private TableColumn<User, Long> idColumn;
    @FXML private TableColumn<User, String> nomColumn;
    @FXML private TableColumn<User, String> prenomColumn;
    @FXML private TableColumn<User, String> emailColumn;
    @FXML private TableColumn<User, String> telephoneColumn;
    @FXML private TableColumn<User, TypeUtilisateur> typeColumn;
    @FXML private TableColumn<User, StatutUtilisateur> statutColumn;
    @FXML private TableColumn<User, LocalDateTime> dateInscriptionColumn;
    @FXML private TableColumn<User, Void> actionsColumn;
    @FXML private TableColumn<User, Void> fraudScoreColumn; // NOUVELLE COLONNE

    @FXML private TextField searchField;
    @FXML private ComboBox<String> filterStatutCombo;
    @FXML private ComboBox<String> filterTypeCombo;

    @FXML private Label totalUsersLabel;
    @FXML private Label activeUsersLabel;
    @FXML private Label pendingUsersLabel;
    @FXML private Label blockedUsersLabel;
    @FXML private Label lblProfileName;
    @FXML private Label lblProfileType;
    
    // NOUVEAUX LABELS POUR STATISTIQUES DE FRAUDE
    @FXML private Label fraudDetectedLabel;
    @FXML private Label fraudSafeLabel;
    @FXML private Label fraudWarningLabel;

    @FXML private StackPane contentPane;
    @FXML private VBox usersContent;

    private final IUserService userService = new UserServiceImpl();
    private final IFraudDetectionDAO fraudDetectionDAO = new FraudDetectionDAOImpl(); // NOUVEAU
    private ObservableList<User> usersList = FXCollections.observableArrayList();
    private User currentUser;

    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @FXML
    public void initialize() {
        try {
            System.out.println("[DEBUG] Initialisation du AdminUsersController...");
            setupTableColumns();
            System.out.println("[DEBUG] Colonnes du tableau configurees");
        } catch (Exception ex) {
            System.err.println("[CLEAN] Erreur lors de la configuration des colonnes: " + ex.getMessage());
            ex.printStackTrace();
        }

        try {
            setupFilters();
            System.out.println("[DEBUG] Filtres configures");
        } catch (Exception ex) {
            System.err.println("[CLEAN] Erreur lors de la configuration des filtres: " + ex.getMessage());
            ex.printStackTrace();
        }

        try {
            loadUsers();
            System.out.println("[DEBUG] Utilisateurs charges");
        } catch (Exception ex) {
            System.err.println("[CLEAN] Erreur lors du chargement des utilisateurs: " + ex.getMessage());
            ex.printStackTrace();
            showError("Erreur", "Impossible de charger la liste des utilisateurs: " + ex.getMessage());
        }

        try {
            updateStatistics();
            System.out.println("[DEBUG] Statistiques mises a jour");
        } catch (Exception ex) {
            System.err.println("[CLEAN] Erreur lors de la mise a jour des statistiques: " + ex.getMessage());
            ex.printStackTrace();
        }

        // Si la vue est chargee directement, recuperer l'utilisateur depuis la session
        try {
            User sessionUser = SessionManager.getInstance().getCurrentUser();
            if (sessionUser != null) {
                setCurrentUser(sessionUser);
                System.out.println("[DEBUG] Utilisateur courant defini depuis la session");
            }
        } catch (Exception ignored) {
            System.err.println("[CLEAN] Erreur lors de la recuperation de l'utilisateur de session");
        }

        System.out.println("[DEBUG] Initialisation du AdminUsersController terminee");
    }

    public void setCurrentUser(User user) {
        this.currentUser = user;
        if (user == null) {
            // Tenter de récupérer depuis la session si disponible
            try {
                this.currentUser = SessionManager.getInstance().getCurrentUser();
            } catch (Exception ignored) {}
        }
        if (this.currentUser == null) return;

        if (lblProfileName != null) {
            lblProfileName.setText(this.currentUser.getNomComplet());
        }
        if (lblProfileType != null) {
            lblProfileType.setText(this.currentUser.getTypeUtilisateur().getLibelle());
        }

        // Si l'utilisateur connecté n'est pas admin, désactiver les actions d'administration
        if (!this.currentUser.isAdmin()) {
            // Désactiver les contrôles sensibles
            try {
                if (usersTable != null) usersTable.setDisable(true);
                if (searchField != null) searchField.setDisable(true);
                if (filterStatutCombo != null) filterStatutCombo.setDisable(true);
                if (filterTypeCombo != null) filterTypeCombo.setDisable(true);
            } catch (Exception ignored) {}

            showWarning("Accès réservé aux administrateurs. Les actions sont désactivées.");
            return;
        }
    }

    private void setupTableColumns() {
        // Colonnes simples
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        nomColumn.setCellValueFactory(new PropertyValueFactory<>("nom"));
        prenomColumn.setCellValueFactory(new PropertyValueFactory<>("prenom"));
        emailColumn.setCellValueFactory(new PropertyValueFactory<>("email"));
        telephoneColumn.setCellValueFactory(new PropertyValueFactory<>("telephone"));

        // Colonne Type avec libellé
        typeColumn.setCellValueFactory(cellData ->
                new SimpleObjectProperty<>(cellData.getValue().getTypeUtilisateur()));
        typeColumn.setCellFactory(column -> new TableCell<User, TypeUtilisateur>() {
            @Override
            protected void updateItem(TypeUtilisateur item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item.getLibelle());
                }
            }
        });

        // Colonne Statut avec badge coloré
        statutColumn.setCellValueFactory(cellData ->
                new SimpleObjectProperty<>(cellData.getValue().getStatut()));
        statutColumn.setCellFactory(column -> new TableCell<User, StatutUtilisateur>() {
            @Override
            protected void updateItem(StatutUtilisateur item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item.getLibelle());
                    setStyle("-fx-background-color: " + item.getCouleur() + "; " +
                            "-fx-background-radius: 12px; " +
                            "-fx-padding: 5px 10px; " +
                            "-fx-text-fill: white; " +
                            "-fx-font-weight: bold;");
                }
            }
        });

        // Colonne Date avec format
        dateInscriptionColumn.setCellValueFactory(cellData ->
                new SimpleObjectProperty<>(cellData.getValue().getDateInscription()));
        dateInscriptionColumn.setCellFactory(column -> new TableCell<User, LocalDateTime>() {
            @Override
            protected void updateItem(LocalDateTime item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.format(DATE_FORMATTER));
                }
            }
        });

        // NOUVELLE COLONNE: Score de Fraude avec badge coloré et bouton détails
        fraudScoreColumn.setCellFactory(column -> new TableCell<User, Void>() {
            private final Label scoreLabel = new Label();
            private final Button detailsBtn = new Button("Détails");
            private final HBox container = new HBox(10, scoreLabel, detailsBtn);

            {
                container.setAlignment(Pos.CENTER_LEFT);
                detailsBtn.setStyle("-fx-background-color: #3B82F6; -fx-text-fill: white; -fx-font-size: 10px;");
                detailsBtn.setTooltip(new Tooltip("Voir l'analyse complète"));
                
                detailsBtn.setOnAction(e -> {
                    User user = getTableView().getItems().get(getIndex());
                    showFraudDetails(user);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    User user = getTableView().getItems().get(getIndex());
                    if (user != null && user.isFraudChecked()) {
                        double score = user.getFraudScore();
                        String level;
                        String color;
                        String emoji;
                        
                        if (score < 25) {
                            level = "Faible";
                            color = "#10B981";
                            emoji = "🟢";
                        } else if (score < 50) {
                            level = "Moyen";
                            color = "#F59E0B";
                            emoji = "🟡";
                        } else if (score < 75) {
                            level = "Élevé";
                            color = "#FB923C";
                            emoji = "🟠";
                        } else {
                            level = "Critique";
                            color = "#EF4444";
                            emoji = "🔴";
                        }
                        
                        scoreLabel.setText(String.format("%.0f/100 - %s %s", score, level, emoji));
                        scoreLabel.setStyle("-fx-background-color: " + color + "; " +
                                "-fx-background-radius: 12px; " +
                                "-fx-padding: 5px 10px; " +
                                "-fx-text-fill: white; " +
                                "-fx-font-weight: bold; " +
                                "-fx-font-size: 11px;");
                        setGraphic(container);
                    } else {
                        scoreLabel.setText("Non analysé");
                        scoreLabel.setStyle("-fx-text-fill: #6B7280; -fx-font-style: italic;");
                        setGraphic(scoreLabel);
                    }
                }
            }
        });

        // Colonne Actions avec boutons
        actionsColumn.setCellFactory(column -> new TableCell<User, Void>() {
            private final Button validateBtn = new Button("✓");
            private final Button blockBtn = new Button("⛔");
            private final Button deleteBtn = new Button("🗑");
            private final Button editBtn = new Button("✏️");
            private final HBox container = new HBox(5, validateBtn, blockBtn, deleteBtn, editBtn);

            {
                validateBtn.setStyle("-fx-background-color: #10B981; -fx-text-fill: white; -fx-font-size: 14px; -fx-padding: 5px 10px;");
                blockBtn.setStyle("-fx-background-color: #EF4444; -fx-text-fill: white; -fx-font-size: 14px; -fx-padding: 5px 10px;");
                deleteBtn.setStyle("-fx-background-color: #6B7280; -fx-text-fill: white; -fx-font-size: 14px; -fx-padding: 5px 10px;");
                editBtn.setStyle("-fx-background-color: #F59E0B; -fx-text-fill: white; -fx-font-size: 14px; -fx-padding: 5px 10px;");

                validateBtn.setTooltip(new Tooltip("Valider"));
                blockBtn.setTooltip(new Tooltip("Bloquer"));
                deleteBtn.setTooltip(new Tooltip("Supprimer"));
                editBtn.setTooltip(new Tooltip("Éditer"));

                validateBtn.setOnAction(e -> {
                    User user = getTableView().getItems().get(getIndex());
                    handleValidateUser(user);
                });

                blockBtn.setOnAction(e -> {
                    User user = getTableView().getItems().get(getIndex());
                    handleBlockUser(user);
                });

                deleteBtn.setOnAction(e -> {
                    User user = getTableView().getItems().get(getIndex());
                    handleDeleteUser(user);
                });

                editBtn.setOnAction(e -> {
                    User user = getTableView().getItems().get(getIndex());
                    handleEditUser(user);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    setGraphic(container);
                }
            }
        });
    }

    private void setupFilters() {
        // Filtre Statut
        filterStatutCombo.getItems().add("Tous");
        filterStatutCombo.getItems().addAll(
                "En attente",
                "Active",
                "Bloqué",
                "Suspendu"
        );
        filterStatutCombo.setValue("Tous");

        // Filtre Type
        filterTypeCombo.getItems().add("Tous");
        filterTypeCombo.getItems().addAll(
                "Investisseur",
                "Porteur de Projet",
                "Expert Carbone",
                "Administrateur"
        );
        filterTypeCombo.setValue("Tous");
    }

    private void loadUsers() {
        try {
            System.out.println("[DEBUG] Chargement de tous les utilisateurs...");
            List<User> users = userService.getAllUsers();
            System.out.println("[DEBUG] " + (users != null ? users.size() : 0) + " utilisateurs recuperes");

            usersList.clear();
            if (users != null && !users.isEmpty()) {
                usersList.addAll(users);
            }
            usersTable.setItems(usersList);
            System.out.println("[DEBUG] Tableau des utilisateurs mis a jour avec " + usersList.size() + " lignes");
        } catch (Exception ex) {
            System.err.println("[CLEAN] Erreur lors du chargement des utilisateurs: " + ex.getMessage());
            ex.printStackTrace();
            usersList.clear();
            usersTable.setItems(usersList);
            showError("Erreur", "Impossible de charger les utilisateurs: " + ex.getMessage());
        }
    }

    @FXML
    private void handleSearch() {
        String searchTerm = searchField.getText().toLowerCase().trim();
        String selectedStatut = filterStatutCombo.getValue();
        String selectedType = filterTypeCombo.getValue();

        List<User> allUsers = userService.getAllUsers();
        ObservableList<User> filteredUsers = FXCollections.observableArrayList();

        for (User user : allUsers) {
            // Filtre de recherche
            boolean matchesSearch = searchTerm.isEmpty() ||
                    user.getNom().toLowerCase().contains(searchTerm) ||
                    user.getPrenom().toLowerCase().contains(searchTerm) ||
                    user.getEmail().toLowerCase().contains(searchTerm) ||
                    (user.getTelephone() != null && user.getTelephone().contains(searchTerm));

            // Filtre statut
            boolean matchesStatut = selectedStatut.equals("Tous") ||
                    user.getStatut().getLibelle().equals(selectedStatut);

            // Filtre type
            boolean matchesType = selectedType.equals("Tous") ||
                    user.getTypeUtilisateur().getLibelle().equals(selectedType);

            if (matchesSearch && matchesStatut && matchesType) {
                filteredUsers.add(user);
            }
        }

        usersTable.setItems(filteredUsers);
    }

    @FXML
    private void handleRefresh() {
        loadUsers();
        updateStatistics();
        searchField.clear();
        filterStatutCombo.setValue("Tous");
        filterTypeCombo.setValue("Tous");
    }

    @FXML
    private void handleAddUser(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/register.fxml"));
            Parent root = loader.load();

            RegisterController controller = loader.getController();
            controller.setAdminMode(true);
            controller.setOnSuccess(this::handleRefresh);

            Stage modal = new Stage();
            modal.setTitle("Créer un nouvel utilisateur");
            modal.initOwner(((Node) event.getSource()).getScene().getWindow());
            modal.setScene(new Scene(root));
            modal.setResizable(false);
            modal.showAndWait();

        } catch (IOException e) {
            e.printStackTrace();
            showWarning("Impossible d'ouvrir le formulaire d'ajout");
        }
    }

    private void handleValidateUser(User user) {
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Validation");
        confirmation.setHeaderText("Valider le compte de " + user.getNomComplet() + " ?");
        confirmation.setContentText("Cette action activera le compte utilisateur.");

        Optional<ButtonType> result = confirmation.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            User validatedUser = userService.validateAccount(user.getId());
            if (validatedUser != null) {
                showSuccess("Compte validé avec succès");
                handleRefresh();
            }
        }
    }

    private void handleBlockUser(User user) {
        if (user.getStatut() == StatutUtilisateur.BLOQUE) {
            // Débloquer
            User unblockedUser = userService.unblockUser(user.getId());
            if (unblockedUser != null) {
                showSuccess("Utilisateur débloqué");
                handleRefresh();
            }
        } else {
            // Bloquer
            Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
            confirmation.setTitle("Blocage");
            confirmation.setHeaderText("Bloquer " + user.getNomComplet() + " ?");
            confirmation.setContentText("L'utilisateur ne pourra plus se connecter.");

            Optional<ButtonType> result = confirmation.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.OK) {
                User blockedUser = userService.blockUser(user.getId());
                if (blockedUser != null) {
                    showSuccess("Utilisateur bloqué");
                    handleRefresh();
                }
            }
        }
    }

    private void handleDeleteUser(User user) {
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Suppression");
        confirmation.setHeaderText("Supprimer " + user.getNomComplet() + " ?");
        confirmation.setContentText("Cette action est irréversible !");
        confirmation.getButtonTypes().setAll(ButtonType.YES, ButtonType.NO);

        Optional<ButtonType> result = confirmation.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.YES) {
            boolean ok = userService.deleteUser(user.getId());
            if (ok) {
                showSuccess("Utilisateur supprimé");
                handleRefresh();
            } else {
                showWarning("Impossible de supprimer l'utilisateur");
            }
        }
    }

    private void handleEditUser(User user) {
        if (user == null) return;
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/edit_user.fxml"));
            Parent root = loader.load();

            EditUserController controller = loader.getController();
            controller.setUser(user);
            controller.setOnSaved(this::handleRefresh);

            Stage modal = new Stage();
            modal.setTitle("Éditer utilisateur");
            modal.initOwner(usersTable.getScene().getWindow());
            modal.setScene(new Scene(root));
            modal.setResizable(false);
            modal.showAndWait();

        } catch (IOException e) {
            e.printStackTrace();
            showWarning("Impossible d'ouvrir le formulaire d'édition");
        }
    }

    @FXML
    private void handleLogout(ActionEvent event) {
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Déconnexion");
        confirmation.setHeaderText("Voulez-vous vraiment vous déconnecter ?");

        Optional<ButtonType> result = confirmation.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                userService.logout(currentUser);

                MainFX.setRoot("fxml/login");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void updateStatistics() {
        long total = userService.getUserCount();
        long active = userService.getActiveUserCount();
        long pending = userService.getPendingUserCount();
        long blocked = userService.getBlockedUserCount();

        totalUsersLabel.setText(String.valueOf(total));
        activeUsersLabel.setText(String.valueOf(active));
        pendingUsersLabel.setText(String.valueOf(pending));
        blockedUsersLabel.setText(String.valueOf(blocked));
        
        // NOUVELLES STATISTIQUES DE FRAUDE
        if (fraudDetectedLabel != null && fraudSafeLabel != null && fraudWarningLabel != null) {
            List<User> allUsers = userService.getAllUsers();
            long fraudDetected = allUsers.stream()
                    .filter(u -> u.isFraudChecked() && u.getFraudScore() >= 75)
                    .count();
            long fraudSafe = allUsers.stream()
                    .filter(u -> u.isFraudChecked() && u.getFraudScore() < 25)
                    .count();
            long fraudWarning = allUsers.stream()
                    .filter(u -> u.isFraudChecked() && u.getFraudScore() >= 25 && u.getFraudScore() < 75)
                    .count();
            
            fraudDetectedLabel.setText(String.valueOf(fraudDetected));
            fraudSafeLabel.setText(String.valueOf(fraudSafe));
            fraudWarningLabel.setText(String.valueOf(fraudWarning));
        }
    }

    private void showSuccess(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Succes");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showWarning(String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Attention");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    @FXML
    private void handleNavUsers() {
        showUsersContent();
    }

    @FXML
    private void handleNavProjets() {
        loadContent("GestionProjet");
    }

    @FXML
    private void handleNavEvaluations() {
        loadContent("gestionCarbone");
    }

    @FXML
    private void handleNavSettings() {
        loadContent("settings");
    }

    private void showUsersContent() {
        if (contentPane != null && usersContent != null) {
            contentPane.getChildren().setAll(usersContent);
        }
        handleRefresh();
    }

    private void loadContent(String fxml) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/" + fxml + ".fxml"));
            Parent root = loader.load();
            Parent content = stripSidebar(root);
            if (contentPane != null) {
                contentPane.getChildren().setAll(content);
            }
        } catch (IOException e) {
            showWarning("Navigation impossible");
        }
    }

    private Parent stripSidebar(Parent root) {
        if (root instanceof javafx.scene.layout.HBox) {
            javafx.scene.layout.HBox hbox = (javafx.scene.layout.HBox) root;
            java.util.List<javafx.scene.Node> toRemove = new java.util.ArrayList<>();
            for (javafx.scene.Node child : hbox.getChildren()) {
                if (child.getStyleClass().contains("sidebar")) {
                    toRemove.add(child);
                }
            }
            hbox.getChildren().removeAll(toRemove);
            return hbox;
        }
        return root;
    }

    @FXML
    private void handleEditProfile() {
        try {
            MainFX.setRoot("editProfile");
        } catch (IOException e) {
            showWarning("Navigation impossible");
        }
    }
    
    /**
     * Affiche les détails de l'analyse de fraude pour un utilisateur
     */
    private void showFraudDetails(User user) {
        Optional<FraudDetectionResult> fraudResultOpt = fraudDetectionDAO.findByUserId(user.getId());
        
        if (fraudResultOpt.isEmpty()) {
            showWarning("Aucune analyse de fraude disponible pour cet utilisateur");
            return;
        }
        
        FraudDetectionResult result = fraudResultOpt.get();
        
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Détection de Fraude - " + user.getNomComplet());
        alert.setHeaderText(null);
        
        StringBuilder content = new StringBuilder();
        content.append("=== ANALYSE DE FRAUDE ===\n\n");
        content.append("Utilisateur: ").append(user.getNomComplet()).append("\n");
        content.append("Email: ").append(user.getEmail()).append("\n\n");
        
        content.append("SCORE DE RISQUE: ").append(String.format("%.1f", result.getRiskScore())).append("/100\n");
        content.append("Niveau: ").append(result.getRiskLevel().getLabel()).append("\n");
        content.append("Frauduleux: ").append(result.isFraudulent() ? "OUI" : "NON").append("\n");
        content.append("Recommandation: ").append(result.getRecommendation()).append("\n\n");
        
        content.append("=== INDICATEURS DETECTES ===\n\n");
        
        long detectedCount = result.getIndicators().stream()
                .filter(FraudDetectionResult.FraudIndicator::isDetected)
                .count();
        
        if (detectedCount == 0) {
            content.append("✅ Aucun indicateur de fraude détecté\n");
        } else {
            for (FraudDetectionResult.FraudIndicator indicator : result.getIndicators()) {
                if (indicator.isDetected()) {
                    content.append("⚠️  ").append(indicator.getType()).append(": ")
                           .append(indicator.getDescription()).append("\n");
                }
            }
        }
        
        content.append("\n=== DETAILS DE L'ANALYSE ===\n\n");
        content.append(result.getAnalysisDetails());
        
        alert.setContentText(content.toString());
        
        // Agrandir la fenêtre
        alert.getDialogPane().setPrefWidth(600);
        alert.getDialogPane().setPrefHeight(500);
        
        alert.showAndWait();
    }
}
