package Controllers;

import Models.User;
import Services.UserServiceImpl;
import Utils.NavigationContext;
import Utils.SessionManager;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;

import java.util.List;

/**
 * Admin Shell Controller
 * Handles all ADMIN role navigation and the default user-management view.
 */
public class AdminShellController extends BaseController {

    // ── Sidebar labels ──────────────────────────────────────────────────────
    @FXML private Label lblPageTitle;
    @FXML private Label lblProfileName;
    @FXML private Label lblProfileType;
    @FXML private Label lblNotifBadge;

    // ── Content area ────────────────────────────────────────────────────────
    @FXML private StackPane contentArea;

    // ── Stats ───────────────────────────────────────────────────────────────
    @FXML private Label statTotal;
    @FXML private Label statActifs;
    @FXML private Label statAttente;
    @FXML private Label statFraud;

    // ── Search controls ─────────────────────────────────────────────────────
    @FXML private TextField txtSearch;
    @FXML private ComboBox<String> cmbType;
    @FXML private ComboBox<String> cmbStatut;

    // ── User table ──────────────────────────────────────────────────────────
    @FXML private TableView<User>          tableUsers;
    @FXML private TableColumn<User,String> colId;
    @FXML private TableColumn<User,String> colNom;
    @FXML private TableColumn<User,String> colEmail;
    @FXML private TableColumn<User,String> colType;
    @FXML private TableColumn<User,String> colStatut;
    @FXML private TableColumn<User,String> colFraud;
    @FXML private TableColumn<User,String> colActions;

    private final UserServiceImpl userService = new UserServiceImpl();

    // ── Init ────────────────────────────────────────────────────────────────

    @FXML
    public void initialize() {
        super.initialize();
        applyProfile(lblProfileName, lblProfileType);
        setupFilters();
        setupTable();
        loadStats();
        loadUsers(null, null, null);
    }

    // ── Sidebar navigation ──────────────────────────────────────────────────

    @FXML private void onUsers()         { navigate("fxml/admin_users"); }
    @FXML private void onAudit()         { navigate("fxml/audit_log"); }
    @FXML private void onProjets()       { navigate("fxml/GestionProjet"); }
    @FXML private void onEvaluations()   { navigate("fxml/expert_carbon_dashboard"); }
    @FXML private void onFraud()         { navigate("fxml/admin_users"); }
    @FXML private void onCredits()       { navigate("fxml/greenwallet"); }
    @FXML private void onCriteres()      { navigate("fxml/admin_users"); }
    @FXML private void onWallet()        { navigate("fxml/greenwallet"); }
    @FXML private void onNotifications() { /* show notification panel */ }
    @FXML private void onEditProfile()   { navigate("editProfile"); }

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

    // ── User management actions ─────────────────────────────────────────────

    @FXML
    private void onSearch() {
        String keyword = txtSearch.getText().trim();
        String type    = cmbType.getValue();
        String statut  = cmbStatut.getValue();
        loadUsers(keyword.isEmpty() ? null : keyword, type, statut);
    }

    @FXML
    private void onNewUser() { navigate("fxml/edit_user"); }

    @FXML
    private void onExportCsv() {
        showInfo("Export CSV", "Export en cours…");
        // TODO: call userService.exportCsv()
    }

    // ── Private helpers ─────────────────────────────────────────────────────

    private void setupFilters() {
        cmbType.setItems(FXCollections.observableArrayList(
            "Tous", "INVESTISSEUR", "PORTEUR_PROJET", "EXPERT_CARBONE", "ADMIN"
        ));
        cmbStatut.setItems(FXCollections.observableArrayList(
            "Tous", "EN_ATTENTE", "ACTIVE", "BLOQUE", "SUSPENDU"
        ));
    }

    private void setupTable() {
        colId.setCellValueFactory(c ->
            new SimpleStringProperty(String.valueOf(c.getValue().getId())));
        colNom.setCellValueFactory(c ->
            new SimpleStringProperty(c.getValue().getNom() + " " + c.getValue().getPrenom()));
        colEmail.setCellValueFactory(c ->
            new SimpleStringProperty(c.getValue().getEmail()));
        colType.setCellValueFactory(c ->
            new SimpleStringProperty(c.getValue().getTypeUtilisateur() != null
                ? c.getValue().getTypeUtilisateur().name() : "—"));
        colStatut.setCellValueFactory(c ->
            new SimpleStringProperty(c.getValue().getStatut() != null
                ? c.getValue().getStatut().name() : "—"));
        colFraud.setCellValueFactory(c ->
            new SimpleStringProperty(c.getValue().isFraudChecked()
                ? String.format("%.0f%%", c.getValue().getFraudScore() * 100) : "—"));

        // Actions column with buttons
        colActions.setCellFactory(col -> new TableCell<>() {
            private final Button btnEdit   = new Button("✏️");
            private final Button btnToggle = new Button("⛔");
            private final Button btnDelete = new Button("🗑");

            {
                btnEdit.setOnAction(e -> {
                    User u = getTableView().getItems().get(getIndex());
                    NavigationContext.getInstance().setCurrentUserId(u.getId().intValue());
                    navigate("fxml/edit_user");
                });                btnToggle.setOnAction(e -> {
                    User u = getTableView().getItems().get(getIndex());
                    toggleUserStatus(u);
                });
                btnDelete.setOnAction(e -> {
                    User u = getTableView().getItems().get(getIndex());
                    confirmAndDelete(u);
                });
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) { setGraphic(null); return; }
                javafx.scene.layout.HBox box = new javafx.scene.layout.HBox(4, btnEdit, btnToggle, btnDelete);
                setGraphic(box);
            }
        });
    }

    private void loadStats() {
        try {
            List<User> all = userService.getAllUsers();
            statTotal.setText(String.valueOf(all.size()));
            statActifs.setText(String.valueOf(all.stream()
                .filter(u -> "ACTIVE".equals(u.getStatut() != null ? u.getStatut().name() : "")).count()));
            statAttente.setText(String.valueOf(all.stream()
                .filter(u -> "EN_ATTENTE".equals(u.getStatut() != null ? u.getStatut().name() : "")).count()));
            statFraud.setText(String.valueOf(all.stream()
                .filter(u -> u.getFraudScore() >= 0.65).count()));
        } catch (Exception e) {
            System.err.println("[AdminShell] Stats error: " + e.getMessage());
        }
    }

    private void loadUsers(String keyword, String type, String statut) {
        try {
            List<User> users = userService.getAllUsers();

            if (keyword != null) {
                String kw = keyword.toLowerCase();
                users = users.stream()
                    .filter(u -> u.getNom().toLowerCase().contains(kw)
                              || u.getEmail().toLowerCase().contains(kw))
                    .collect(java.util.stream.Collectors.toList());
            }
            if (type != null && !"Tous".equals(type)) {
                users = users.stream()
                    .filter(u -> u.getTypeUtilisateur() != null
                              && type.equals(u.getTypeUtilisateur().name()))
                    .collect(java.util.stream.Collectors.toList());
            }
            if (statut != null && !"Tous".equals(statut)) {
                users = users.stream()
                    .filter(u -> u.getStatut() != null
                              && statut.equals(u.getStatut().name()))
                    .collect(java.util.stream.Collectors.toList());
            }

            tableUsers.setItems(FXCollections.observableArrayList(users));
        } catch (Exception e) {
            System.err.println("[AdminShell] Load users error: " + e.getMessage());
        }
    }

    private void toggleUserStatus(User user) {
        try {
            String current = user.getStatut() != null ? user.getStatut().name() : "EN_ATTENTE";
            String next    = "ACTIVE".equals(current) ? "BLOQUE" : "ACTIVE";
            userService.updateUserStatus(user.getId(), next);
            loadUsers(null, null, null);
            loadStats();
        } catch (Exception e) {
            showError("Erreur", "Impossible de modifier le statut: " + e.getMessage());
        }
    }

    private void confirmAndDelete(User user) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
            "Supprimer " + user.getNom() + " ?", ButtonType.YES, ButtonType.NO);
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.YES) {
                try {
                    userService.deleteUser(user.getId());
                    loadUsers(null, null, null);
                    loadStats();
                } catch (Exception e) {
                    showError("Erreur", "Suppression impossible: " + e.getMessage());
                }
            }
        });
    }
    private void navigate(String fxml) {
        try { org.GreenLedger.MainFX.setRoot(fxml); }
        catch (Exception e) { System.err.println("[AdminShell] Nav error: " + e.getMessage()); }
    }

    private void showInfo(String title, String msg) {
        new Alert(Alert.AlertType.INFORMATION, msg, ButtonType.OK).showAndWait();
    }

    private void showError(String title, String msg) {
        new Alert(Alert.AlertType.ERROR, msg, ButtonType.OK).showAndWait();
    }
}
