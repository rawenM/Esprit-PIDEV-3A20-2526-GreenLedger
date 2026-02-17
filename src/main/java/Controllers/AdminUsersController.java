package Controllers;


import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import Models.StatutUtilisateur;
import Models.TypeUtilisateur;
import Models.User;
import Services.IUserService;
import Services.UserServiceImpl;
import Utils.SessionManager;
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

    @FXML private TextField searchField;
    @FXML private ComboBox<String> filterStatutCombo;
    @FXML private ComboBox<String> filterTypeCombo;

    @FXML private Label totalUsersLabel;
    @FXML private Label activeUsersLabel;
    @FXML private Label pendingUsersLabel;
    @FXML private Label blockedUsersLabel;
    @FXML private Label lblProfileName;
    @FXML private Label lblProfileType;

    @FXML private StackPane contentPane;
    @FXML private VBox usersContent;

    private final IUserService userService = new UserServiceImpl();
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

        // Colonne Actions avec boutons
        actionsColumn.setCellFactory(column -> new TableCell<User, Void>() {
            private final Button validateBtn = new Button("✓");
            private final Button blockBtn = new Button("[CLEAN]");
            private final Button deleteBtn = new Button("🗑");
            private final Button editBtn = new Button("✏️");
            private final HBox container = new HBox(5, validateBtn, blockBtn, deleteBtn, /*edit*/ editBtn);

            {
                validateBtn.setStyle("-fx-background-color: #10B981; -fx-text-fill: white;");
                blockBtn.setStyle("-fx-background-color: #EF4444; -fx-text-fill: white;");
                deleteBtn.setStyle("-fx-background-color: #6B7280; -fx-text-fill: white;");
                editBtn.setStyle("-fx-background-color: #F59E0B; -fx-text-fill: white;");

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
}
