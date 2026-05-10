package Controllers;

import Models.User;
import Services.UserServiceImpl;
import Utils.NavigationContext;
import Utils.SessionManager;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.layout.Region;
import javafx.scene.control.ScrollPane;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Admin Shell Controller — redesigned to match the PHP back-office UI.
 */
public class AdminShellController extends BaseController {

    // ── Sidebar buttons (for active state toggling) ──────────────────────────
    @FXML private Button btnDashboard;
    @FXML private Button btnUsers;
    @FXML private Button btnAudit;
    @FXML private Button btnAuditNav;
    @FXML private Button btnCarteConn;
    @FXML private Button btnProjets;
    @FXML private Button btnEvaluations;
    @FXML private Button btnFraud;
    @FXML private Button btnCredits;
    @FXML private Button btnDispatchWallet;
    @FXML private Button btnCriteres;
    @FXML private Button btnWallet;

    // ── Top bar ─────────────────────────────────────────────────────────────
    @FXML private Label lblPageTitle;
    @FXML private Label lblProfileName;
    @FXML private Label lblProfileType;

    // ── Content area ─────────────────────────────────────────────────────────
    @FXML private StackPane  contentArea;
    @FXML private ScrollPane usersScrollPane;  // the default users view inside contentArea

    // ── KPI labels ───────────────────────────────────────────────────────────
    @FXML private Label statTotal;
    @FXML private Label statActifs;
    @FXML private Label statAttente;
    @FXML private Label statBloques;
    @FXML private Label statSuspendus;
    @FXML private Label statFraud;

    // ── Search controls ──────────────────────────────────────────────────────
    @FXML private TextField        txtSearch;
    @FXML private ComboBox<String> cmbType;
    @FXML private ComboBox<String> cmbStatut;
    @FXML private Label            lblCount;

    // ── Table ────────────────────────────────────────────────────────────────
    @FXML private TableView<User>          tableUsers;
    @FXML private TableColumn<User,String> colId;
    @FXML private TableColumn<User,String> colNom;
    @FXML private TableColumn<User,String> colEmail;
    @FXML private TableColumn<User,String> colType;
    @FXML private TableColumn<User,String> colStatut;
    @FXML private TableColumn<User,String> colDate;
    @FXML private TableColumn<User,String> colFraud;
    @FXML private TableColumn<User,String> colActions;

    // ── Pending section ──────────────────────────────────────────────────────
    @FXML private VBox pendingSection;
    @FXML private VBox pendingList;

    private final UserServiceImpl userService = new UserServiceImpl();
    private static final DateTimeFormatter DATE_FMT =
        DateTimeFormatter.ofPattern("dd/MM/yyyy");

    // ── Init ─────────────────────────────────────────────────────────────────
    @FXML
    public void initialize() {
        super.initialize();
        applyProfile(lblProfileName, lblProfileType);
        setupFilters();
        setupTable();
        loadStats();
        loadUsers(null, null, null);
        // Mark Utilisateurs as active by default
        if (btnUsers != null) setActiveBtn(btnUsers);
    }

    // ── Sidebar navigation ───────────────────────────────────────────────────
    @FXML private void onDashboard()    { setActiveBtn(btnDashboard);  showUsersView(); }
    @FXML private void onUsers()        { setActiveBtn(btnUsers);      showUsersView(); }
    @FXML private void onAudit()        { setActiveBtn(btnAuditNav);   loadContent("fxml/audit_log"); }
    @FXML private void onCarteConn()    { setActiveBtn(btnCarteConn);  loadContent("fxml/user_connection_map"); }
    @FXML private void onProjets()      { setActiveBtn(btnProjets);    loadContent("fxml/GestionProjet"); }
    @FXML private void onEvaluations()  { setActiveBtn(btnEvaluations); loadContent("fxml/expert_carbon_dashboard"); }
    @FXML private void onFraud()           { setActiveBtn(btnFraud);          loadContent("fxml/project_fraud_scoring"); }
    @FXML private void onCriteres()        { setActiveBtn(btnCriteres);        showUsersView(); }
    @FXML private void onCredits()         { setActiveBtn(btnCredits);         loadContent("fxml/admin_green_credits"); }
    @FXML private void onDispatchWallet()  { setActiveBtn(btnDispatchWallet);  loadContent("fxml/admin_dispatch_wallet"); }
    @FXML private void onWallet()          { setActiveBtn(btnWallet);          loadContent("fxml/wallet_supervision"); }
    @FXML private void onNotifications(){ /* notification panel */ }
    @FXML private void onEditProfile()  { navigate("editProfile"); }

    /** Restore the shell's own users content into contentArea */
    private void showUsersView() {
        if (usersScrollPane != null) {
            contentArea.getChildren().setAll(usersScrollPane);
        }
        loadStats();
        loadUsers(null, null, null);
    }

    @FXML
    private void onLogout() {
        SessionManager.getInstance().invalidate();
        navigate("fxml/login");
    }

    // ── Search / filter ──────────────────────────────────────────────────────
    @FXML
    private void onSearch() {
        String kw  = txtSearch.getText().trim();
        String typ = cmbType.getValue();
        String sta = cmbStatut.getValue();
        loadUsers(kw.isEmpty() ? null : kw, typ, sta);
    }

    @FXML private void onNewUser()   { navigate("fxml/edit_user"); }

    @FXML
    private void onExportCsv() {
        showInfo("Export CSV", "Export en cours…");
    }

    // ── Private helpers ──────────────────────────────────────────────────────

    private void setupFilters() {
        cmbType.setItems(FXCollections.observableArrayList(
            "Tous", "INVESTISSEUR", "PORTEUR_PROJET", "EXPERT_CARBONE", "ADMIN"));
        cmbStatut.setItems(FXCollections.observableArrayList(
            "Tous", "EN_ATTENTE", "ACTIVE", "BLOQUE", "SUSPENDU"));
    }

    private void setupTable() {
        // ID
        colId.setCellValueFactory(c ->
            new SimpleStringProperty(String.valueOf(c.getValue().getId())));

        // Utilisateur — avatar circle + name/email
        colNom.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setGraphic(null); return;
                }
                User u = (User) getTableRow().getItem();
                String initials = initials(u.getNom(), u.getPrenom());
                Label avatar = new Label(initials);
                avatar.getStyleClass().add("gl-user-avatar");

                VBox info = new VBox(1);
                Label name = new Label(u.getNom() + " " + u.getPrenom());
                name.setStyle("-fx-font-weight:600; -fx-text-fill:#e2e8f0; -fx-font-size:13px;");
                info.getChildren().add(name);

                HBox row = new HBox(9, avatar, info);
                row.setAlignment(Pos.CENTER_LEFT);
                setGraphic(row);
            }
        });
        colNom.setCellValueFactory(c ->
            new SimpleStringProperty(c.getValue().getNom()));

        // Email
        colEmail.setCellValueFactory(c ->
            new SimpleStringProperty(c.getValue().getEmail()));

        // Rôle — colored badge
        colType.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setGraphic(null); return;
                }
                User u = (User) getTableRow().getItem();
                String type = u.getTypeUtilisateur() != null ? u.getTypeUtilisateur().name() : "—";
                Label badge = new Label(type.replace("_", " "));
                badge.getStyleClass().add(roleBadgeClass(type));
                setGraphic(badge);
            }
        });
        colType.setCellValueFactory(c ->
            new SimpleStringProperty(c.getValue().getTypeUtilisateur() != null
                ? c.getValue().getTypeUtilisateur().name() : "—"));

        // Statut — colored badge
        colStatut.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setGraphic(null); return;
                }
                User u = (User) getTableRow().getItem();
                String st = u.getStatut() != null ? u.getStatut().name() : "—";
                Label badge = new Label(st.replace("_", " "));
                badge.getStyleClass().add(statusBadgeClass(st));
                setGraphic(badge);
            }
        });
        colStatut.setCellValueFactory(c ->
            new SimpleStringProperty(c.getValue().getStatut() != null
                ? c.getValue().getStatut().name() : "—"));

        // Inscription date
        colDate.setCellValueFactory(c -> {
            User u = c.getValue();
            String d = "—";
            try {
                if (u.getDateNaissance() != null)
                    d = u.getDateNaissance().format(DATE_FMT);
            } catch (Exception ignored) {}
            return new SimpleStringProperty(d);
        });

        // Fraude score
        colFraud.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setText(null); setGraphic(null); return;
                }
                User u = (User) getTableRow().getItem();
                if (!u.isFraudChecked()) { setText("—"); setGraphic(null); return; }
                double score = u.getFraudScore();
                String txt = String.format("%.0f%%", score * 100);
                Label lbl = new Label(txt);
                lbl.getStyleClass().add(score >= 0.5 ? "fraud-high" : "fraud-low");
                setGraphic(lbl); setText(null);
            }
        });
        colFraud.setCellValueFactory(c ->
            new SimpleStringProperty(c.getValue().isFraudChecked()
                ? String.format("%.0f%%", c.getValue().getFraudScore() * 100) : "—"));

        // Actions
        colActions.setCellFactory(col -> new TableCell<>() {
            private final Button btnVoir    = new Button("Voir");
            private final Button btnEdit    = new Button("Éditer");
            private final Button btnToggle  = new Button("Activer");
            private final Button btnDelete  = new Button("Suppr.");

            {
                btnVoir.getStyleClass().add("gl-btn-view");
                btnEdit.getStyleClass().add("gl-btn-edit");
                btnToggle.getStyleClass().add("gl-btn-toggle");
                btnDelete.getStyleClass().add("gl-btn-danger");

                btnEdit.setOnAction(e -> {
                    User u = getTableView().getItems().get(getIndex());
                    NavigationContext.getInstance().setCurrentUserId(u.getId().intValue());
                    navigate("fxml/edit_user");
                });
                btnToggle.setOnAction(e -> {
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
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setGraphic(null); return;
                }
                User u = (User) getTableRow().getItem();
                String st = u.getStatut() != null ? u.getStatut().name() : "";
                if ("ACTIVE".equals(st)) {
                    btnToggle.setText("Bloquer");
                    btnToggle.getStyleClass().setAll("gl-btn-toggle-block");
                } else {
                    btnToggle.setText("Activer");
                    btnToggle.getStyleClass().setAll("gl-btn-toggle");
                }
                HBox box = new HBox(5, btnVoir, btnEdit, btnToggle, btnDelete);
                box.setAlignment(Pos.CENTER_LEFT);
                setGraphic(box);
            }
        });
        colActions.setCellValueFactory(c -> new SimpleStringProperty(""));
    }

    private void loadStats() {
        try {
            List<User> all = userService.getAllUsers();
            statTotal.setText(String.valueOf(all.size()));
            statActifs.setText(String.valueOf(count(all, "ACTIVE")));
            statAttente.setText(String.valueOf(count(all, "EN_ATTENTE")));
            statBloques.setText(String.valueOf(count(all, "BLOQUE")));
            statSuspendus.setText(String.valueOf(count(all, "SUSPENDU")));
            statFraud.setText(String.valueOf(
                all.stream().filter(u -> u.getFraudScore() >= 0.65).count()));
        } catch (Exception e) {
            System.err.println("[AdminShell] Stats error: " + e.getMessage());
        }
    }

    private long count(List<User> list, String status) {
        return list.stream()
            .filter(u -> u.getStatut() != null && status.equals(u.getStatut().name()))
            .count();
    }

    private void loadUsers(String keyword, String type, String statut) {
        try {
            List<User> users = userService.getAllUsers();

            if (keyword != null) {
                String kw = keyword.toLowerCase();
                users = users.stream()
                    .filter(u -> (u.getNom() != null && u.getNom().toLowerCase().contains(kw))
                              || (u.getEmail() != null && u.getEmail().toLowerCase().contains(kw)))
                    .collect(Collectors.toList());
            }
            if (type != null && !"Tous".equals(type)) {
                users = users.stream()
                    .filter(u -> u.getTypeUtilisateur() != null
                              && type.equals(u.getTypeUtilisateur().name()))
                    .collect(Collectors.toList());
            }
            if (statut != null && !"Tous".equals(statut)) {
                users = users.stream()
                    .filter(u -> u.getStatut() != null
                              && statut.equals(u.getStatut().name()))
                    .collect(Collectors.toList());
            }

            tableUsers.setItems(FXCollections.observableArrayList(users));

            if (lblCount != null)
                lblCount.setText(users.size() + " utilisateur(s) trouvé(s)");

            // Pending section
            buildPendingSection(users.stream()
                .filter(u -> u.getStatut() != null && "EN_ATTENTE".equals(u.getStatut().name()))
                .collect(Collectors.toList()));

        } catch (Exception e) {
            System.err.println("[AdminShell] Load users error: " + e.getMessage());
        }
    }

    private void buildPendingSection(List<User> pending) {
        if (pendingList == null || pendingSection == null) return;
        pendingList.getChildren().clear();
        if (pending.isEmpty()) {
            pendingSection.setVisible(false);
            pendingSection.setManaged(false);
            return;
        }
        pendingSection.setVisible(true);
        pendingSection.setManaged(true);

        for (User u : pending) {
            HBox row = new HBox(12);
            row.getStyleClass().add("gl-pending-row");
            row.setAlignment(Pos.CENTER_LEFT);

            String initials = initials(u.getNom(), u.getPrenom());
            Label avatar = new Label(initials);
            avatar.getStyleClass().add("gl-user-avatar");

            VBox info = new VBox(2);
            Label name  = new Label(u.getNom() + " " + u.getPrenom());
            name.getStyleClass().add("gl-pending-name");
            Label email = new Label(u.getEmail());
            email.getStyleClass().add("gl-pending-email");
            info.getChildren().addAll(name, email);

            Region spacer = new Region();
            HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);

            Button btnActivate = new Button("✓ Activer");
            btnActivate.getStyleClass().add("gl-btn-activate");
            btnActivate.setOnAction(e -> {
                try {
                    userService.updateUserStatus(u.getId(), "ACTIVE");
                    loadUsers(null, null, null);
                    loadStats();
                } catch (Exception ex) {
                    showError("Erreur", ex.getMessage());
                }
            });

            Button btnReject = new Button("✕ Rejeter");
            btnReject.getStyleClass().add("gl-btn-reject");
            btnReject.setOnAction(e -> {
                try {
                    userService.updateUserStatus(u.getId(), "BLOQUE");
                    loadUsers(null, null, null);
                    loadStats();
                } catch (Exception ex) {
                    showError("Erreur", ex.getMessage());
                }
            });

            row.getChildren().addAll(avatar, info, spacer, btnActivate, btnReject);
            pendingList.getChildren().add(row);
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
            "Supprimer " + user.getNom() + " " + user.getPrenom() + " ?",
            ButtonType.YES, ButtonType.NO);
        confirm.setHeaderText("Confirmer la suppression");
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

    // ── Utility ──────────────────────────────────────────────────────────────

    private static String initials(String nom, String prenom) {
        String n = (nom   != null && !nom.isEmpty())   ? nom.substring(0,1).toUpperCase()   : "";
        String p = (prenom != null && !prenom.isEmpty()) ? prenom.substring(0,1).toUpperCase() : "";
        return n + p;
    }

    private static String roleBadgeClass(String type) {
        if (type == null) return "badge-investisseur";
        return switch (type) {
            case "INVESTISSEUR"  -> "badge-investisseur";
            case "PORTEUR_PROJET"-> "badge-porteur";
            case "EXPERT_CARBONE"-> "badge-expert";
            case "ADMIN"         -> "badge-admin";
            default              -> "badge-investisseur";
        };
    }

    private static String statusBadgeClass(String st) {
        if (st == null) return "badge-attente";
        return switch (st) {
            case "ACTIVE"    -> "badge-active";
            case "EN_ATTENTE"-> "badge-attente";
            case "BLOQUE"    -> "badge-bloque";
            case "SUSPENDU"  -> "badge-suspendu";
            default          -> "badge-attente";
        };
    }

    // ── Content loading (keeps sidebar visible) ──────────────────────────────

    private void loadContent(String fxmlPath) {
        try {
            java.net.URL resource = getClass().getResource("/" + fxmlPath + ".fxml");
            if (resource == null) {
                System.err.println("[AdminShell] FXML not found: " + fxmlPath);
                return;
            }
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(resource);
            javafx.scene.Parent content = loader.load();
            contentArea.getChildren().setAll(content);
        } catch (Exception e) {
            System.err.println("[AdminShell] loadContent error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void setActiveBtn(Button active) {
        java.util.List<Button> allBtns = java.util.Arrays.asList(
            btnDashboard, btnUsers, btnAuditNav, btnCarteConn,
            btnProjets, btnEvaluations, btnFraud, btnCredits,
            btnDispatchWallet, btnCriteres, btnWallet
        );
        for (Button b : allBtns) {
            if (b == null) continue;
            b.getStyleClass().removeAll("gl-nav-active");
            if (!b.getStyleClass().contains("gl-nav-btn"))
                b.getStyleClass().add("gl-nav-btn");
        }
        if (active != null) {
            active.getStyleClass().add("gl-nav-active");
        }
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
