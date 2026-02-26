package Controllers;

import Models.Wallet;
import Models.OperationWallet;
import Models.TypeUtilisateur;
import Models.User;
import Services.WalletService;
import Services.ExternalCarbonApiService;
import Services.AirQualityService;
import Models.dto.external.CarbonEstimateResponse;
import Models.dto.external.AirPollutionResponse;
import Utils.SessionManager;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import org.GreenLedger.MainFX;

import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Controller for Green Wallet - Carbon Credit Management System.
 */
public class GreenWalletController extends BaseController {

    // Services
    private WalletService walletService;
    private ExternalCarbonApiService carbonApiService;
    private AirQualityService airQualityService;
    private Wallet currentWallet;

    // Sidebar Buttons
    @FXML private Button btnWalletOverview;
    @FXML private Button btnGestionProjets;
    @FXML private Button btnMarketplace;
    @FXML private Button btnTransactions;
    @FXML private Button btnBatches;
    @FXML private Button btnIssueCredits;
    @FXML private Button btnRetireCredits;
    @FXML private Button btnCreateWallet;
    @FXML private Button btnSettings;
    @FXML private Label lblProfileName;
    @FXML private Label lblProfileType;

    // Wallet Selector
    @FXML private ComboBox<Wallet> cmbWalletSelector;

    // Wallet Info Labels
    @FXML private Label lblWalletNumber;
    @FXML private Label lblHolderName;
    @FXML private Label lblOwnerType;
    @FXML private Label lblStatus;

    // Stat Cards
    @FXML private Label lblAvailableCredits;
    @FXML private Label lblRetiredCredits;
    @FXML private Label lblTotalCredits;

    // Transactions Table
    @FXML private TableView<OperationWallet> tableTransactions;
    @FXML private TableColumn<OperationWallet, Integer> colTransactionId;
    @FXML private TableColumn<OperationWallet, String> colTransactionType;
    @FXML private TableColumn<OperationWallet, Double> colTransactionAmount;
    @FXML private TableColumn<OperationWallet, String> colTransactionDate;
    @FXML private TableColumn<OperationWallet, String> colTransactionReference;

    // Action Buttons
    @FXML private Button btnIssueCreditsMain;
    @FXML private Button btnRetireCreditsMain;
    @FXML private Button btnTransferCredits;
    @FXML private Button btnEditWallet;
    @FXML private Button btnDeleteWallet;
    @FXML private Button btnExport;
    @FXML private Button btnRefresh;
    @FXML private Button btnTestAdd25;
    @FXML private Button btnTestAdd100;
    @FXML private Button btnTestAdd500;
    @FXML private Button btnTestAdd1000;
    
    // API Integration Components
    @FXML private Button btnCalculateElectricity;
    @FXML private Button btnCalculateFuel;
    @FXML private Button btnCalculateShipping;
    @FXML private Button btnCheckAirQuality;
    @FXML private TextArea txtApiResults;

    // Content Pane
    @FXML private VBox contentPane;

    @FXML
    public void initialize() {
        super.initialize();
        walletService = new WalletService();
        carbonApiService = new ExternalCarbonApiService();
        airQualityService = new AirQualityService();

        applyProfile(lblProfileName, lblProfileType);
        
        setupTableColumns();
        setupWalletSelector();
        setupListeners();
        setupApiListeners();
        loadWallets();

        if (btnWalletOverview != null) {
            Platform.runLater(() -> btnWalletOverview.requestFocus());
        }
    }

    private void setupTableColumns() {
        colTransactionId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colTransactionType.setCellValueFactory(new PropertyValueFactory<>("type"));
        colTransactionAmount.setCellValueFactory(new PropertyValueFactory<>("amount"));
        
        // Format date column
        colTransactionDate.setCellValueFactory(cellData -> {
            if (cellData.getValue().getCreatedAt() != null) {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
                return new javafx.beans.property.SimpleStringProperty(
                    cellData.getValue().getCreatedAt().format(formatter)
                );
            }
            return new javafx.beans.property.SimpleStringProperty("—");
        });
        
        colTransactionReference.setCellValueFactory(new PropertyValueFactory<>("referenceNote"));
        
        // Style type column
        colTransactionType.setCellFactory(column -> new TableCell<OperationWallet, String>() {
            @Override
            protected void updateItem(String type, boolean empty) {
                super.updateItem(type, empty);
                if (empty || type == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(type);
                    if (type.equals("ISSUE")) {
                        setStyle("-fx-text-fill: #2B6A4A; -fx-font-weight: bold;");
                    } else if (type.equals("RETIRE")) {
                        setStyle("-fx-text-fill: #D97706; -fx-font-weight: bold;");
                    } else if (type.contains("TRANSFER")) {
                        setStyle("-fx-text-fill: #3B82F6; -fx-font-weight: bold;");
                    }
                }
            }
        });
    }

    private void setupWalletSelector() {
        cmbWalletSelector.setConverter(new javafx.util.StringConverter<Wallet>() {
            @Override
            public String toString(Wallet wallet) {
                if (wallet == null) return null;
                String name = wallet.getName() != null ? wallet.getName() : "Unnamed Wallet";
                return String.format("#%s - %s (%s)", formatWalletNumber(wallet.getWalletNumber()), name, wallet.getOwnerType());
            }

            @Override
            public Wallet fromString(String string) {
                return null;
            }
        });
        
        cmbWalletSelector.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                loadWallet(newVal.getId());
            }
        });
    }

    private void setupListeners() {
        if (btnWalletOverview != null) {
            btnWalletOverview.setOnAction(e -> showWalletOverview());
        }
        if (btnMarketplace != null) {
            btnMarketplace.setOnAction(e -> showMarketplace());
        }
        if (btnTransactions != null) {
            btnTransactions.setOnAction(e -> showTransactions());
        }
        if (btnBatches != null) {
            btnBatches.setOnAction(e -> showBatches());
        }
        
        if (btnIssueCredits != null) {
            btnIssueCredits.setOnAction(e -> showQuickIssueDialog());
        }
        if (btnRetireCredits != null) {
            btnRetireCredits.setOnAction(e -> showRetireCreditsDialog());
        }
        if (btnCreateWallet != null) {
            btnCreateWallet.setOnAction(e -> showCreateWalletDialog());
        }
        
        if (btnIssueCreditsMain != null) {
            btnIssueCreditsMain.setOnAction(e -> showQuickIssueDialog());
        }
        if (btnRetireCreditsMain != null) {
            btnRetireCreditsMain.setOnAction(e -> showRetireCreditsDialog());
        }
        
        if (btnTransferCredits != null) {
            btnTransferCredits.setOnAction(e -> showTransferDialog());
        }
        if (btnEditWallet != null) {
            btnEditWallet.setOnAction(e -> showEditWalletDialog());
        }
        if (btnDeleteWallet != null) {
            btnDeleteWallet.setOnAction(e -> showDeleteWalletDialog());
        }
        
        if (btnExport != null) {
            btnExport.setOnAction(e -> exportData());
        }
        if (btnRefresh != null) {
            btnRefresh.setOnAction(e -> refreshData());
        }

        if (btnTestAdd25 != null) {
            btnTestAdd25.setOnAction(e -> addTestCredits(25.0));
        }
        if (btnTestAdd100 != null) {
            btnTestAdd100.setOnAction(e -> addTestCredits(100.0));
        }
        if (btnTestAdd500 != null) {
            btnTestAdd500.setOnAction(e -> addTestCredits(500.0));
        }
        if (btnTestAdd1000 != null) {
            btnTestAdd1000.setOnAction(e -> addTestCredits(1000.0));
        }
        
        if (btnSettings != null) {
            btnSettings.setOnAction(e -> showSettings());
        }
    }
    
    private void setupApiListeners() {
        if (btnCalculateElectricity != null) {
            btnCalculateElectricity.setOnAction(e -> calculateElectricityEmissions());
        }
        if (btnCalculateFuel != null) {
            btnCalculateFuel.setOnAction(e -> calculateFuelEmissions());
        }
        if (btnCalculateShipping != null) {
            btnCalculateShipping.setOnAction(e -> calculateShippingEmissions());
        }
        if (btnCheckAirQuality != null) {
            btnCheckAirQuality.setOnAction(e -> checkAirQuality());
        }
    }

    // ==================== WALLET LOADING ====================

    private void loadWallets() {
        try {
            List<Wallet> wallets = getScopedWallets();
            ObservableList<Wallet> walletList = FXCollections.observableArrayList(wallets);
            cmbWalletSelector.setItems(walletList);
            
            // Select first wallet if available
            if (!wallets.isEmpty()) {
                cmbWalletSelector.getSelectionModel().select(0);
            } else {
                currentWallet = null;
                clearWalletDisplay();
            }
        } catch (Exception e) {
            showError("Erreur lors du chargement des wallets", e.getMessage());
        }
    }

    private void loadWallet(int walletId) {
        try {
            currentWallet = walletService.getWalletById(walletId);
            if (currentWallet != null) {
                updateWalletDisplay();
                loadTransactions();
            }
        } catch (Exception e) {
            showError("Erreur lors du chargement du wallet", e.getMessage());
        }
    }

    private void updateWalletDisplay() {
        if (currentWallet == null) {
            clearWalletDisplay();
            return;
        }
        
        lblWalletNumber.setText(formatWalletNumber(currentWallet.getWalletNumber()));
        lblHolderName.setText(currentWallet.getName() != null ? currentWallet.getName() : "Unnamed Wallet");
        lblOwnerType.setText(currentWallet.getOwnerType());
        lblStatus.setText("Active");
        
        // Update credit stats
        lblAvailableCredits.setText(formatCredits(currentWallet.getAvailableCredits()));
        lblRetiredCredits.setText(formatCredits(currentWallet.getRetiredCredits()));
        lblTotalCredits.setText(formatCredits(currentWallet.getTotalCredits()));
    }

    private void clearWalletDisplay() {
        lblWalletNumber.setText("—");
        lblHolderName.setText("—");
        lblOwnerType.setText("—");
        lblStatus.setText("—");
        lblAvailableCredits.setText("0.00 tCO₂");
        lblRetiredCredits.setText("0.00 tCO₂");
        lblTotalCredits.setText("0.00 tCO₂");
        tableTransactions.setItems(FXCollections.observableArrayList());
    }

    private void loadTransactions() {
        if (currentWallet == null) return;
        
        try {
            List<OperationWallet> transactions = walletService.getWalletTransactions(currentWallet.getId());
            ObservableList<OperationWallet> transactionList = FXCollections.observableArrayList(transactions);
            tableTransactions.setItems(transactionList);
        } catch (Exception e) {
            showError("Erreur lors du chargement des transactions", e.getMessage());
        }
    }

    // ==================== ACTIONS ====================

    private void showCreateWalletDialog() {
        User user = SessionManager.getInstance().getCurrentUser();
        Integer userId = getCurrentUserIdAsInt(user);
        if (user == null || userId == null) {
            showWarning("Session invalide", "Impossible de créer un wallet sans utilisateur connecté.");
            return;
        }

        Dialog<Wallet> dialog = new Dialog<>();
        dialog.setTitle("🌱 Créer un Nouveau Wallet Carbone");
        dialog.setHeaderText("Créez un wallet lié à votre compte");

        ButtonType createButtonType = new ButtonType("✓ Créer Wallet", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(createButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(12);
        grid.setPadding(new Insets(20));

        TextField walletName = new TextField();
        walletName.setPromptText("Ex: Wallet Projet Solaire 2026");
        walletName.setPrefWidth(300);
        
        TextField walletNumber = new TextField();
        walletNumber.setPromptText("Laissez vide pour génération automatique");
        
        TextField initialCredits = new TextField();
        initialCredits.setPromptText("0.00");
        initialCredits.setText("0");

        Label ownerLabel = new Label(user.getNomComplet() + " (" + user.getTypeUtilisateur().getLibelle() + ")");
        ownerLabel.setStyle("-fx-font-weight: 600; -fx-text-fill: #2D5F3F;");

        grid.add(new Label("📛 Nom du Wallet:"), 0, 0);
        grid.add(walletName, 1, 0);
        grid.add(new Label("🔢 Numéro (optionnel):"), 0, 1);
        grid.add(walletNumber, 1, 1);
        grid.add(new Label("👤 Propriétaire:"), 0, 2);
        grid.add(ownerLabel, 1, 2);
        grid.add(new Label("💰 Crédits initiaux (tCO₂):"), 0, 3);
        grid.add(initialCredits, 1, 3);

        dialog.getDialogPane().setContent(grid);

        Button createButton = (Button) dialog.getDialogPane().lookupButton(createButtonType);
        createButton.addEventFilter(ActionEvent.ACTION, event -> {
            String name = walletName.getText() == null ? "" : walletName.getText().trim();
            String walletNumberText = walletNumber.getText() == null ? "" : walletNumber.getText().trim();
            String creditsText = initialCredits.getText() == null ? "" : initialCredits.getText().trim();

            if (name.isEmpty()) {
                event.consume();
                showWarning("Nom requis", "Veuillez saisir un nom de wallet.");
                return;
            }

            if (!walletNumberText.isEmpty()) {
                try {
                    int number = Integer.parseInt(walletNumberText);
                    if (number <= 0) {
                        event.consume();
                        showWarning("Numéro invalide", "Le numéro du wallet doit être un entier positif.");
                        return;
                    }
                } catch (NumberFormatException ex) {
                    event.consume();
                    showWarning("Numéro invalide", "Le numéro du wallet doit être numérique.");
                    return;
                }
            }

            try {
                double credits = Double.parseDouble((creditsText.isEmpty() ? "0" : creditsText).replace(',', '.'));
                if (credits < 0) {
                    event.consume();
                    showWarning("Crédits invalides", "Les crédits initiaux ne peuvent pas être négatifs.");
                }
            } catch (NumberFormatException ex) {
                event.consume();
                showWarning("Crédits invalides", "Veuillez saisir une valeur numérique valide.");
            }
        });

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == createButtonType) {
                Wallet wallet = new Wallet();
                wallet.setName(walletName.getText().trim());

                String walletNumberText = walletNumber.getText() == null ? "" : walletNumber.getText().trim();
                if (!walletNumberText.isEmpty()) {
                    wallet.setWalletNumber(parseIntegerOrNull(walletNumberText));
                }

                String creditsText = initialCredits.getText() == null ? "0" : initialCredits.getText().trim();
                double credits = Double.parseDouble((creditsText.isEmpty() ? "0" : creditsText).replace(',', '.'));

                wallet.setOwnerType(resolveOwnerTypeForUser(user));
                wallet.setOwnerId(userId);
                wallet.setAvailableCredits(Math.max(0, credits));
                wallet.setRetiredCredits(0.0);

                return wallet;
            }
            return null;
        });

        Optional<Wallet> result = dialog.showAndWait();
        result.ifPresent(wallet -> {
            try {
                int id = walletService.createWallet(wallet);
                if (id > 0) {
                    showInfo("Succès", "Wallet créé avec succès!");
                    loadWallets();
                } else {
                    showError("Erreur", "Impossible de créer le wallet");
                }
            } catch (Exception e) {
                showError("Erreur lors de la création", e.getMessage());
            }
        });
    }

    private void showQuickIssueDialog() {
        if (currentWallet == null) {
            showWarning("Aucun wallet sélectionné", "Veuillez sélectionner un wallet");
            return;
        }

        Dialog<String[]> dialog = new Dialog<>();
        dialog.setTitle("🌱 Émettre des Crédits Carbone");
        dialog.setHeaderText(String.format("Wallet: %s\nDisponible: %.2f tCO₂", 
            currentWallet.getName(), currentWallet.getAvailableCredits()));

        ButtonType issueButtonType = new ButtonType("✓ Émettre", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(issueButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(12);
        grid.setPadding(new Insets(20));

        TextField amount = new TextField();
        amount.setPromptText("Montant (tCO₂)");
        
        ComboBox<String> presetAmounts = new ComboBox<>();
        presetAmounts.getItems().addAll("100.00", "500.00", "1000.00", "5000.00", "Personnalisé");
        presetAmounts.setValue("Personnalisé");
        presetAmounts.setOnAction(e -> {
            String val = presetAmounts.getValue();
            if (!val.equals("Personnalisé")) {
                amount.setText(val);
            }
        });
        
        ComboBox<String> sourcePresets = new ComboBox<>();
        sourcePresets.getItems().addAll(
            "🌞 Installation Solaire - Phase 1",
            "🌲 Reforestation Amazonie",
            "💨 Capture CO₂ Industrielle",
            "⚡ Parc Éolien Offshore",
            "🛰️ Vérification Projet Tiers",
            "Autre source..."
        );
        sourcePresets.setValue("Autre source...");
        
        TextArea reference = new TextArea();
        reference.setPromptText("Description de l'émission...");
        reference.setPrefRowCount(3);
        
        sourcePresets.setOnAction(e -> {
            String selected = sourcePresets.getValue();
            if (!selected.equals("Autre source...")) {
                reference.setText("Crédits émis depuis: " + selected);
            }
        });

        grid.add(new Label("📊 Montant Rapide:"), 0, 0);
        grid.add(presetAmounts, 1, 0);
        grid.add(new Label("💰 Montant Exact (tCO₂):"), 0, 1);
        grid.add(amount, 1, 1);
        grid.add(new Label("🏭 Source:"), 0, 2);
        grid.add(sourcePresets, 1, 2);
        grid.add(new Label("📝 Référence:"), 0, 3);
        grid.add(reference, 1, 3);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == issueButtonType) {
                return new String[]{amount.getText(), reference.getText()};
            }
            return null;
        });

        Optional<String[]> result = dialog.showAndWait();
        result.ifPresent(data -> {
            try {
                double amt = Double.parseDouble(data[0]);
                String ref = data[1].isEmpty() ? "Émission de crédits carbone" : data[1];
                
                boolean success = walletService.quickIssueCredits(currentWallet.getId(), amt, ref);
                if (success) {
                    showInfo("✔ Succès", String.format("%.2f tCO₂ émis avec succès!", amt));
                    refreshData();
                } else {
                    showError("Erreur", "Impossible d'émettre les crédits");
                }
            } catch (Exception e) {
                showError("Erreur lors de l'émission", e.getMessage());
            }
        });
    }

    private void showEditWalletDialog() {
        if (currentWallet == null) {
            showWarning("Aucun wallet sélectionné", "Veuillez sélectionner un wallet");
            return;
        }

        Dialog<Wallet> dialog = new Dialog<>();
        dialog.setTitle("✏️ Modifier le Wallet");
        dialog.setHeaderText("Modification du Wallet #" + formatWalletNumber(currentWallet.getWalletNumber()));

        ButtonType saveButtonType = new ButtonType("💾 Sauvegarder", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(12);
        grid.setPadding(new Insets(20));

        TextField walletName = new TextField(currentWallet.getName());
        walletName.setPrefWidth(300);
        
        ComboBox<String> ownerType = new ComboBox<>();
        ownerType.getItems().addAll("ENTERPRISE", "BANK", "NGO", "GOVERNMENT");
        ownerType.setValue(currentWallet.getOwnerType());
        
        Label walletNumberLabel = new Label("#" + formatWalletNumber(currentWallet.getWalletNumber()));
        walletNumberLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14;");
        
        Label creditsLabel = new Label(String.format("%.2f tCO₂ disponibles", currentWallet.getAvailableCredits()));
        creditsLabel.setStyle("-fx-text-fill: #2B6A4A;");

        grid.add(new Label("🔢 Numéro Wallet:"), 0, 0);
        grid.add(walletNumberLabel, 1, 0);
        grid.add(new Label("📛 Nom:"), 0, 1);
        grid.add(walletName, 1, 1);
        grid.add(new Label("🏢 Type:"), 0, 2);
        grid.add(ownerType, 1, 2);
        grid.add(new Label("💰 Solde:"), 0, 3);
        grid.add(creditsLabel, 1, 3);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                Wallet updated = new Wallet();
                updated.setId(currentWallet.getId());
                updated.setName(walletName.getText());
                updated.setOwnerType(ownerType.getValue());
                return updated;
            }
            return null;
        });

        Optional<Wallet> result = dialog.showAndWait();
        result.ifPresent(wallet -> {
            try {
                boolean success = walletService.updateWallet(wallet);
                if (success) {
                    showInfo("✔ Succès", "Wallet modifié avec succès!");
                    refreshData();
                } else {
                    showError("Erreur", "Impossible de modifier le wallet");
                }
            } catch (Exception e) {
                showError("Erreur lors de la modification", e.getMessage());
            }
        });
    }

    private void showTransferDialog() {
        if (currentWallet == null) {
            showWarning("Aucun wallet sélectionné", "Veuillez sélectionner un wallet source");
            return;
        }

        if (currentWallet.getAvailableCredits() <= 0) {
            showWarning("Crédits insuffisants", "Ce wallet n'a pas de crédits disponibles pour le transfert");
            return;
        }

        Dialog<String[]> dialog = new Dialog<>();
        dialog.setTitle("🔄 Transférer des Crédits");
        dialog.setHeaderText(String.format("Source: %s\nDisponible: %.2f tCO₂", 
            currentWallet.getName(), currentWallet.getAvailableCredits()));

        ButtonType transferButtonType = new ButtonType("➡️ Transférer", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(transferButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(12);
        grid.setPadding(new Insets(20));

        ComboBox<Wallet> destinationWallet = new ComboBox<>();
        List<Wallet> allWallets = getScopedWallets();
        allWallets.removeIf(w -> w.getId() == currentWallet.getId());
        destinationWallet.setItems(FXCollections.observableArrayList(allWallets));
        destinationWallet.setConverter(new javafx.util.StringConverter<Wallet>() {
            @Override
            public String toString(Wallet w) {
                if (w == null) return null;
                return String.format("#%s - %s", formatWalletNumber(w.getWalletNumber()), w.getName());
            }
            @Override
            public Wallet fromString(String s) { return null; }
        });
        
        TextField amount = new TextField();
        amount.setPromptText("Montant à transférer");
        
        ComboBox<String> quickAmounts = new ComboBox<>();
        quickAmounts.getItems().addAll("25%", "50%", "75%", "100%", "Personnalisé");
        quickAmounts.setValue("Personnalisé");
        quickAmounts.setOnAction(e -> {
            String val = quickAmounts.getValue();
            if (!val.equals("Personnalisé")) {
                double percentage = Double.parseDouble(val.replace("%", "")) / 100.0;
                double amt = currentWallet.getAvailableCredits() * percentage;
                amount.setText(String.format("%.2f", amt));
            }
        });
        
        TextArea reference = new TextArea();
        reference.setPromptText("Raison du transfert (obligatoire)");
        reference.setPrefRowCount(3);

        grid.add(new Label("🎯 Destination:"), 0, 0);
        grid.add(destinationWallet, 1, 0);
        grid.add(new Label("📊 Montant Rapide:"), 0, 1);
        grid.add(quickAmounts, 1, 1);
        grid.add(new Label("💰 Montant (tCO₂):"), 0, 2);
        grid.add(amount, 1, 2);
        grid.add(new Label("📝 Raison:"), 0, 3);
        grid.add(reference, 1, 3);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == transferButtonType) {
                Wallet dest = destinationWallet.getValue();
                if (dest == null) {
                    showWarning("Destination requise", "Veuillez sélectionner un wallet de destination");
                    return null;
                }
                return new String[]{String.valueOf(dest.getId()), amount.getText(), reference.getText()};
            }
            return null;
        });

        Optional<String[]> result = dialog.showAndWait();
        result.ifPresent(data -> {
            try {
                int destId = Integer.parseInt(data[0]);
                double amt = Double.parseDouble(data[1]);
                String ref = data[2];
                
                if (ref.trim().isEmpty()) {
                    showWarning("Référence requise", "Veuillez indiquer la raison du transfert");
                    return;
                }
                
                boolean success = walletService.transferCredits(currentWallet.getId(), destId, amt, ref);
                if (success) {
                    showInfo("✔ Transfert Réussi", String.format("%.2f tCO₂ transférés avec succès!", amt));
                    refreshData();
                } else {
                    showError("Erreur", "Impossible de transférer les crédits");
                }
            } catch (Exception e) {
                showError("Erreur lors du transfert", e.getMessage());
            }
        });
    }

    private void showIssueCreditsDialog() {
        if (currentWallet == null) {
            showWarning("Aucun wallet sélectionné", "Veuillez sélectionner un wallet");
            return;
        }

        Dialog<double[]> dialog = new Dialog<>();
        dialog.setTitle("Émettre des Crédits Carbone");
        dialog.setHeaderText("Émission de crédits pour: Wallet #" + formatWalletNumber(currentWallet.getWalletNumber()));

        ButtonType issueButtonType = new ButtonType("Émettre", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(issueButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));

        TextField projectId = new TextField();
        projectId.setPromptText("ID du Projet");
        
        TextField amount = new TextField();
        amount.setPromptText("Montant (tCO₂)");
        
        TextArea reference = new TextArea();
        reference.setPromptText("Note de référence");
        reference.setPrefRowCount(3);

        grid.add(new Label("ID Projet:"), 0, 0);
        grid.add(projectId, 1, 0);
        grid.add(new Label("Montant:"), 0, 1);
        grid.add(amount, 1, 1);
        grid.add(new Label("Référence:"), 0, 2);
        grid.add(reference, 1, 2);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == issueButtonType) {
                try {
                    int projId = Integer.parseInt(projectId.getText());
                    double amt = Double.parseDouble(amount.getText());
                    return new double[]{projId, amt};
                } catch (NumberFormatException e) {
                    return null;
                }
            }
            return null;
        });

        Optional<double[]> result = dialog.showAndWait();
        result.ifPresent(data -> {
            try {
                int projId = (int) data[0];
                double amt = data[1];
                String ref = reference.getText();
                
                boolean success = walletService.issueCredits(currentWallet.getId(), projId, amt, ref);
                if (success) {
                    showInfo("Succès", amt + " tCO₂ émis avec succès!");
                    refreshData();
                } else {
                    showError("Erreur", "Impossible d'émettre les crédits");
                }
            } catch (Exception e) {
                showError("Erreur lors de l'émission", e.getMessage());
            }
        });
    }

    private void showRetireCreditsDialog() {
        if (currentWallet == null) {
            showWarning("Aucun wallet sélectionné", "Veuillez sélectionner un wallet");
            return;
        }

        if (currentWallet.getAvailableCredits() <= 0) {
            showWarning("Aucun crédit disponible", "Ce wallet n'a pas de crédits disponibles à retirer");
            return;
        }

        Dialog<String[]> dialog = new Dialog<>();
        dialog.setTitle("♻️ Retirer des Crédits Carbone");
        dialog.setHeaderText(String.format("Wallet: %s\nDisponible: %.2f tCO₂", 
            currentWallet.getName(), currentWallet.getAvailableCredits()));

        ButtonType retireButtonType = new ButtonType("🔒 Retirer", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(retireButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(12);
        grid.setPadding(new Insets(20));

        ComboBox<String> quickAmounts = new ComboBox<>();
        quickAmounts.getItems().addAll("25%", "50%", "75%", "100%", "Personnalisé");
        quickAmounts.setValue("Personnalisé");
        
        TextField amount = new TextField();
        amount.setPromptText("Montant (tCO₂)");
        
        quickAmounts.setOnAction(e -> {
            String val = quickAmounts.getValue();
            if (!val.equals("Personnalisé")) {
                double percentage = Double.parseDouble(val.replace("%", "")) / 100.0;
                double amt = currentWallet.getAvailableCredits() * percentage;
                amount.setText(String.format("%.2f", amt));
            }
        });
        
        ComboBox<String> reasonPresets = new ComboBox<>();
        reasonPresets.getItems().addAll(
            "🌍 Compensation empreinte carbone entreprise",
            "✈️ Neutralité carbone - Voyage aérien",
            "🏭 Compensation production industrielle",
            "🚗 Neutralisation émissions transport",
            "🏢 Bilan carbone annuel - Neutralité",
            "🎯 Objectif Net-Zero atteint",
            "Autre raison..."
        );
        reasonPresets.setValue("Autre raison...");
        
        TextArea reference = new TextArea();
        reference.setPromptText("Ex: Compensation carbone pour conférence internationale à Paris, 200 participants...");
        reference.setPrefRowCount(4);
        
        reasonPresets.setOnAction(e -> {
            String selected = reasonPresets.getValue();
            if (!selected.equals("Autre raison...")) {
                reference.setText(selected);
            }
        });

        grid.add(new Label("📊 Montant Rapide:"), 0, 0);
        grid.add(quickAmounts, 1, 0);
        grid.add(new Label("💰 Montant Exact (tCO₂):"), 0, 1);
        grid.add(amount, 1, 1);
        grid.add(new Label("🏷️ Raison Type:"), 0, 2);
        grid.add(reasonPresets, 1, 2);
        grid.add(new Label("📝 Raison détaillée:"), 0, 3);
        grid.add(reference, 1, 3);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == retireButtonType) {
                return new String[]{amount.getText(), reference.getText()};
            }
            return null;
        });

        Optional<String[]> result = dialog.showAndWait();
        result.ifPresent(data -> {
            try {
                double amt = Double.parseDouble(data[0]);
                String ref = data[1];
                
                if (ref.trim().isEmpty()) {
                    showWarning("Référence requise", "Veuillez indiquer la raison du retirement");
                    return;
                }
                
                boolean success = walletService.retireCredits(currentWallet.getId(), amt, ref);
                if (success) {
                    showInfo("Retrait effectué", String.format("%.2f tCO₂ retirés avec succès!\n\nCes crédits sont maintenant définitivement retirés du marché.", amt));
                    refreshData();
                } else {
                    showError("Erreur", "Impossible de retirer les crédits");
                }
            } catch (Exception e) {
                showError("Erreur lors du retirement", e.getMessage());
            }
        });
    }

    private void showDeleteWalletDialog() {
        if (currentWallet == null) {
            showWarning("Aucun wallet sélectionné", "Veuillez sélectionner un wallet");
            return;
        }

        // Safety check: Can only delete wallets with zero balance
        if (currentWallet.getTotalCredits() > 0) {
            showWarning(
                "Suppression impossible", 
                String.format(
                    "Ce wallet contient encore des crédits:\n\n" +
                    "💰 Disponibles: %.2f tCO₂\n" +
                    "♻️ Retirés: %.2f tCO₂\n" +
                    "📊 Total: %.2f tCO₂\n\n" +
                    "Vous devez d'abord transférer ou retirer tous les crédits disponibles.",
                    currentWallet.getAvailableCredits(),
                    currentWallet.getRetiredCredits(),
                    currentWallet.getTotalCredits()
                )
            );
            return;
        }

        // Confirmation dialog
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Confirmer la suppression");
        confirmation.setHeaderText("Supprimer le wallet #" + formatWalletNumber(currentWallet.getWalletNumber()) + "?");
        confirmation.setContentText(
            String.format(
                "Wallet: %s\n" +
                "Type: %s\n" +
                "Cette action est IRRÉVERSIBLE!\n" +
                "Toutes les transactions associées seront également supprimées.\n\n" +
                "Êtes-vous sûr de vouloir continuer?",
                currentWallet.getName(),
                currentWallet.getOwnerType()
            )
        );

        ButtonType btnDelete = new ButtonType("Supprimer", ButtonBar.ButtonData.OK_DONE);
        ButtonType btnCancel = new ButtonType("Annuler", ButtonBar.ButtonData.CANCEL_CLOSE);
        confirmation.getButtonTypes().setAll(btnDelete, btnCancel);

        Optional<ButtonType> result = confirmation.showAndWait();
        if (result.isPresent() && result.get() == btnDelete) {
            try {
                boolean success = walletService.deleteWallet(currentWallet.getId());
                if (success) {
                    showInfo("Wallet supprimé", "Le wallet a été supprimé avec succès!");
                    currentWallet = null;
                    loadWallets();
                    clearWalletDisplay();
                } else {
                    showError("Erreur", "Impossible de supprimer le wallet");
                }
            } catch (Exception e) {
                showError("Erreur lors de la suppression", e.getMessage());
            }
        }
    }

    private void showWalletOverview() {
        // Current view is already overview
        refreshData();
    }

    private void showTransactions() {
        // Already showing transactions in main view
        refreshData();
    }

    private void showBatches() {
        showInfo("Bientôt disponible", "La vue des batches sera implémentée prochainement");
    }

    private void exportData() {
        showInfo("Bientôt disponible", "La fonction d'export sera implémentée prochainement");
    }

    private void refreshData() {
        if (currentWallet != null) {
            loadWallet(currentWallet.getId());
        }
    }

    private void addTestCredits(double amount) {
        if (currentWallet == null) {
            showWarning("Aucun wallet sélectionné", "Veuillez sélectionner un wallet");
            return;
        }

        String note = String.format("[TEST] Quick top-up %.2f tCO₂", amount);
        boolean success = walletService.quickIssueCredits(currentWallet.getId(), amount, note);
        if (success) {
            showInfo("🧪 Crédit Test Ajouté", String.format("%.2f tCO₂ ajoutés au wallet %s", amount, currentWallet.getName()));
            refreshData();
        } else {
            showError("Erreur", "Impossible d'ajouter les crédits de test");
        }
    }

    private void showSettings() {
        try {
            MainFX.setRoot("settings");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void showMarketplace() {
        try {
            MainFX.setRoot("fxml/marketplace");
        } catch (IOException e) {
            try {
                MainFX.setRoot("marketplace");
                return;
            } catch (IOException ignored) {
                // fall through to error handling
            }
            showError("Erreur", "Impossible d'ouvrir le marketplace: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void onMarketplace() {
        showMarketplace();
    }

    @FXML
    private void onGestionProjets() {
        try {
            MainFX.setRoot("GestionProjet");
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
    private void onBack() {
        try {
            MainFX.setRoot("fxml/dashboard");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // ==================== UTILITY METHODS ====================

    private String formatCredits(double credits) {
        return String.format("%.2f tCO₂", credits);
    }

    private String formatWalletNumber(Integer walletNumber) {
        return walletNumber == null ? "—" : String.valueOf(walletNumber);
    }

    private Integer parseIntegerOrNull(String value) {
        if (value == null) {
            return null;
        }
        String text = value.trim();
        if (text.isEmpty()) {
            return null;
        }
        try {
            return Integer.parseInt(text);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private List<Wallet> getScopedWallets() {
        List<Wallet> allWallets = walletService.getAllWallets();
        User user = SessionManager.getInstance().getCurrentUser();
        Integer currentUserId = getCurrentUserIdAsInt(user);

        if (user == null || currentUserId == null || user.getTypeUtilisateur() == TypeUtilisateur.ADMIN) {
            return allWallets;
        }

        return allWallets.stream()
                .filter(wallet -> wallet.getOwnerId() == currentUserId)
                .collect(Collectors.toList());
    }

    private Integer getCurrentUserIdAsInt(User user) {
        if (user == null || user.getId() == null) {
            return null;
        }
        try {
            return Math.toIntExact(user.getId());
        } catch (ArithmeticException ex) {
            return null;
        }
    }

    private String resolveOwnerTypeForUser(User user) {
        if (user == null || user.getTypeUtilisateur() == null) {
            return "ENTERPRISE";
        }

        if (user.getTypeUtilisateur() == TypeUtilisateur.INVESTISSEUR) {
            return "BANK";
        }
        return "ENTERPRISE";
    }

    private void showInfo(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    private void showError(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    private void showWarning(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
    
    // ==================== CARBON API INTEGRATION ====================
    
    private void calculateElectricityEmissions() {
        if (!ensureCarbonApiAvailable()) {
            return;
        }
        Dialog<Double> dialog = new Dialog<>();
        dialog.setTitle("⚡ Calcul Émissions Électricité");
        dialog.setHeaderText("Estimer les émissions CO₂ de la consommation électrique");
        
        ButtonType calculateButtonType = new ButtonType("Calculer", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(calculateButtonType, ButtonType.CANCEL);
        
        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(12);
        grid.setPadding(new Insets(20));
        
        TextField electricityValue = new TextField("1000");
        electricityValue.setPromptText("Valeur");
        
        ComboBox<String> electricityUnit = new ComboBox<>();
        electricityUnit.getItems().addAll("kwh", "mwh");
        electricityUnit.setValue("kwh");
        
        TextField country = new TextField("us");
        country.setPromptText("Code pays (ex: us, fr)");
        
        TextField state = new TextField();
        state.setPromptText("État (optionnel, ex: fl)");
        
        grid.add(new Label("⚡ Consommation:"), 0, 0);
        grid.add(electricityValue, 1, 0);
        grid.add(new Label("📊 Unité:"), 0, 1);
        grid.add(electricityUnit, 1, 1);
        grid.add(new Label("🌍 Pays:"), 0, 2);
        grid.add(country, 1, 2);
        grid.add(new Label("📍 État:"), 0, 3);
        grid.add(state, 1, 3);
        
        dialog.getDialogPane().setContent(grid);
        
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == calculateButtonType) {
                try {
                    return Double.parseDouble(electricityValue.getText());
                } catch (NumberFormatException e) {
                    return null;
                }
            }
            return null;
        });
        
        Optional<Double> result = dialog.showAndWait();
        result.ifPresent(value -> {
            appendToApiResults("⚡ Calcul d'émissions d'électricité en cours...\n");
            
            new Thread(() -> {
                try {
                    String countryCode = country.getText().trim();
                    
                    CarbonEstimateResponse response = carbonApiService.estimateElectricity(
                        value,
                        electricityUnit.getValue(),
                        countryCode
                    );
                    
                    Platform.runLater(() -> {
                        if (response != null && response.getAttributes() != null) {
                            appendToApiResults(formatCarbonEstimateResponse(response));
                        } else {
                            appendToApiResults(buildCarbonApiErrorMessage());
                        }
                    });
                } catch (Exception e) {
                    Platform.runLater(() -> appendToApiResults("❌ Erreur: " + e.getMessage() + "\n"));
                }
            }).start();
        });
    }
    
    private void calculateFuelEmissions() {
        if (!ensureCarbonApiAvailable()) {
            return;
        }
        Dialog<Double> dialog = new Dialog<>();
        dialog.setTitle("⛽ Calcul Émissions Carburant");
        dialog.setHeaderText("Estimer les émissions CO₂ de la combustion de carburant");
        
        ButtonType calculateButtonType = new ButtonType("Calculer", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(calculateButtonType, ButtonType.CANCEL);
        
        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(12);
        grid.setPadding(new Insets(20));
        
        ComboBox<String> fuelSourceType = new ComboBox<>();
        fuelSourceType.getItems().addAll("dfo", "rfo", "lng", "lpg", "cng", "coal", "petcoke");
        fuelSourceType.setValue("dfo");
        
        TextField fuelSourceValue = new TextField("100");
        fuelSourceValue.setPromptText("Valeur");
        
        ComboBox<String> fuelSourceUnit = new ComboBox<>();
        fuelSourceUnit.getItems().addAll("litre", "gallon", "tonne");
        fuelSourceUnit.setValue("litre");
        
        grid.add(new Label("⛽ Type Carburant:"), 0, 0);
        grid.add(fuelSourceType, 1, 0);
        grid.add(new Label("📊 Quantité:"), 0, 1);
        grid.add(fuelSourceValue, 1, 1);
        grid.add(new Label("📏 Unité:"), 0, 2);
        grid.add(fuelSourceUnit, 1, 2);
        
        Label infoLabel = new Label("dfo=Diesel, rfo=Fuel Heavy, lng=Gaz Naturel Liquéfié");
        infoLabel.setStyle("-fx-font-size: 10; -fx-opacity: 0.7;");
        grid.add(infoLabel, 0, 3, 2, 1);
        
        dialog.getDialogPane().setContent(grid);
        
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == calculateButtonType) {
                try {
                    return Double.parseDouble(fuelSourceValue.getText());
                } catch (NumberFormatException e) {
                    return null;
                }
            }
            return null;
        });
        
        Optional<Double> result = dialog.showAndWait();
        result.ifPresent(value -> {
            appendToApiResults("⛽ Calcul d'émissions de carburant en cours...\n");
            
            new Thread(() -> {
                try {
                    CarbonEstimateResponse response = carbonApiService.estimateFuel(
                        fuelSourceType.getValue(),
                        value,
                        fuelSourceUnit.getValue()
                    );
                    
                    Platform.runLater(() -> {
                        if (response != null && response.getAttributes() != null) {
                            appendToApiResults(formatCarbonEstimateResponse(response));
                        } else {
                            appendToApiResults(buildCarbonApiErrorMessage());
                        }
                    });
                } catch (Exception e) {
                    Platform.runLater(() -> appendToApiResults("❌ Erreur: " + e.getMessage() + "\n"));
                }
            }).start();
        });
    }
    
    private void calculateShippingEmissions() {
        if (!ensureCarbonApiAvailable()) {
            return;
        }
        Dialog<Double> dialog = new Dialog<>();
        dialog.setTitle("🚢 Calcul Émissions Transport");
        dialog.setHeaderText("Estimer les émissions CO₂ du transport maritime");
        
        ButtonType calculateButtonType = new ButtonType("Calculer", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(calculateButtonType, ButtonType.CANCEL);
        
        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(12);
        grid.setPadding(new Insets(20));
        
        TextField weightValue = new TextField("1000");
        weightValue.setPromptText("Poids");
        
        ComboBox<String> weightUnit = new ComboBox<>();
        weightUnit.getItems().addAll("kg", "lb", "mt", "g");
        weightUnit.setValue("kg");
        
        TextField distanceValue = new TextField("1000");
        distanceValue.setPromptText("Distance");
        
        ComboBox<String> distanceUnit = new ComboBox<>();
        distanceUnit.getItems().addAll("km", "mi");
        distanceUnit.setValue("km");
        
        ComboBox<String> transportMethod = new ComboBox<>();
        transportMethod.getItems().addAll("ship", "train", "truck", "plane");
        transportMethod.setValue("ship");
        
        grid.add(new Label("📦 Poids:"), 0, 0);
        grid.add(weightValue, 1, 0);
        grid.add(new Label("⚖️ Unité Poids:"), 0, 1);
        grid.add(weightUnit, 1, 1);
        grid.add(new Label("📏 Distance:"), 0, 2);
        grid.add(distanceValue, 1, 2);
        grid.add(new Label("📐 Unité Distance:"), 0, 3);
        grid.add(distanceUnit, 1, 3);
        grid.add(new Label("🚚 Mode Transport:"), 0, 4);
        grid.add(transportMethod, 1, 4);
        
        dialog.getDialogPane().setContent(grid);
        
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == calculateButtonType) {
                try {
                    return Double.parseDouble(weightValue.getText());
                } catch (NumberFormatException e) {
                    return null;
                }
            }
            return null;
        });
        
        Optional<Double> result = dialog.showAndWait();
        result.ifPresent(weight -> {
            appendToApiResults("🚢 Calcul d'émissions de transport en cours...\n");
            
            new Thread(() -> {
                try {
                    double distance = Double.parseDouble(distanceValue.getText());
                    double weightKg = convertWeightToKg(weight, weightUnit.getValue());
                    double distanceKm = convertDistanceToKm(distance, distanceUnit.getValue());
                    
                    CarbonEstimateResponse response = carbonApiService.estimateShipping(
                        weightKg,
                        distanceKm,
                        transportMethod.getValue()
                    );
                    
                    Platform.runLater(() -> {
                        if (response != null && response.getAttributes() != null) {
                            appendToApiResults(formatCarbonEstimateResponse(response));
                        } else {
                            appendToApiResults(buildCarbonApiErrorMessage());
                        }
                    });
                } catch (Exception e) {
                    Platform.runLater(() -> appendToApiResults("❌ Erreur: " + e.getMessage() + "\n"));
                }
            }).start();
        });
    }
    
    private void checkAirQuality() {
        Dialog<String[]> dialog = new Dialog<>();
        dialog.setTitle("🌫️ Vérification Qualité de l'Air");
        dialog.setHeaderText("Obtenir les données de qualité de l'air pour une localisation");
        
        ButtonType checkButtonType = new ButtonType("Vérifier", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(checkButtonType, ButtonType.CANCEL);
        
        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(12);
        grid.setPadding(new Insets(20));
        
        TextField latitude = new TextField("40.7128");
        latitude.setPromptText("Latitude");
        
        TextField longitude = new TextField("-74.0060");
        longitude.setPromptText("Longitude");
        
        Label exampleLabel = new Label("Exemple: New York = 40.7128, -74.0060");
        exampleLabel.setStyle("-fx-font-size: 10; -fx-opacity: 0.7;");
        
        HBox presetBox = new HBox(8);
        Button btnParis = new Button("🇫🇷 Paris");
        Button btnNewYork = new Button("🇺🇸 New York");
        Button btnTokyo = new Button("🇯🇵 Tokyo");
        
        btnParis.setOnAction(e -> {
            latitude.setText("48.8566");
            longitude.setText("2.3522");
        });
        btnNewYork.setOnAction(e -> {
            latitude.setText("40.7128");
            longitude.setText("-74.0060");
        });
        btnTokyo.setOnAction(e -> {
            latitude.setText("35.6762");
            longitude.setText("139.6503");
        });
        
        presetBox.getChildren().addAll(btnParis, btnNewYork, btnTokyo);
        
        grid.add(new Label("📍 Latitude:"), 0, 0);
        grid.add(latitude, 1, 0);
        grid.add(new Label("📍 Longitude:"), 0, 1);
        grid.add(longitude, 1, 1);
        grid.add(exampleLabel, 0, 2, 2, 1);
        grid.add(new Label("🗺️ Préréglages:"), 0, 3);
        grid.add(presetBox, 1, 3);
        
        dialog.getDialogPane().setContent(grid);
        
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == checkButtonType) {
                return new String[]{latitude.getText(), longitude.getText()};
            }
            return null;
        });
        
        Optional<String[]> result = dialog.showAndWait();
        result.ifPresent(coords -> {
            appendToApiResults("🌫️ Vérification de la qualité de l'air en cours...\n");
            
            new Thread(() -> {
                try {
                    double lat = Double.parseDouble(coords[0]);
                    double lon = Double.parseDouble(coords[1]);
                    
                    AirPollutionResponse response = airQualityService.getCurrentAirQuality(lat, lon);
                    
                    Platform.runLater(() -> {
                        if (response != null && response.getList() != null && !response.getList().isEmpty()) {
                            appendToApiResults(formatAirQualityResponse(response, lat, lon));
                        } else {
                            appendToApiResults("❌ Aucune donnée de qualité de l'air retournée.\n");
                        }
                    });
                } catch (Exception e) {
                    Platform.runLater(() -> appendToApiResults("❌ Erreur: " + e.getMessage() + "\n"));
                }
            }).start();
        });
    }
    
    private String formatCarbonEstimateResponse(CarbonEstimateResponse response) {
        StringBuilder sb = new StringBuilder();
        sb.append("═══════════════════════════════════════════════════\n");
        sb.append("✅ RÉSULTAT DU CALCUL D'ÉMISSIONS CO₂\n");
        sb.append("═══════════════════════════════════════════════════\n\n");
        
        if (response.getAttributes() != null) {
            var attrs = response.getAttributes();
            
            sb.append(String.format("🌍 Émissions CO₂: %.3f kg\n", attrs.getCarbonKg()));
            sb.append(String.format("📊 Équivalent: %.6f tonnes\n", attrs.getCarbonMt()));
            
            if (attrs.getEstimatedAt() != null) {
                sb.append(String.format("⏰ Calculé le: %s\n", attrs.getEstimatedAt()));
            }
        }
        
        sb.append("\n═══════════════════════════════════════════════════\n\n");
        return sb.toString();
    }
    
    private String formatAirQualityResponse(AirPollutionResponse response, double lat, double lon) {
        StringBuilder sb = new StringBuilder();
        sb.append("═══════════════════════════════════════════════════\n");
        sb.append("✅ QUALITÉ DE L'AIR\n");
        sb.append("═══════════════════════════════════════════════════\n\n");
        
        sb.append(String.format("📍 Localisation: %.4f, %.4f\n\n", lat, lon));
        
        if (!response.getList().isEmpty()) {
            var data = response.getList().get(0);
            
            if (data.getMain() != null) {
                int aqi = data.getMain().getAqi();
                sb.append(String.format("🌫️ Indice Qualité Air (AQI): %d - %s\n\n", 
                    aqi, getAqiDescription(aqi)));
            }
            
            if (data.getComponents() != null) {
                var comp = data.getComponents();
                sb.append("📊 COMPOSANTS (μg/m³):\n");
                sb.append(String.format("  • CO (Monoxyde de carbone): %.2f\n", comp.getCo()));
                sb.append(String.format("  • NO₂ (Dioxyde d'azote): %.2f\n", comp.getNo2()));
                sb.append(String.format("  • O₃ (Ozone): %.2f\n", comp.getO3()));
                sb.append(String.format("  • PM2.5 (Particules fines): %.2f\n", comp.getPm2_5()));
                sb.append(String.format("  • PM10 (Particules): %.2f\n", comp.getPm10()));
                sb.append(String.format("  • SO₂ (Dioxyde de soufre): %.2f\n", comp.getSo2()));
            }
        }
        
        sb.append("\n═══════════════════════════════════════════════════\n\n");
        return sb.toString();
    }
    
    private String getAqiDescription(int aqi) {
        switch (aqi) {
            case 1: return "Bon ✅";
            case 2: return "Moyen 🟡";
            case 3: return "Modéré 🟠";
            case 4: return "Mauvais 🔴";
            case 5: return "Très mauvais ⛔";
            default: return "Inconnu";
        }
    }
    
    private void appendToApiResults(String text) {
        if (txtApiResults != null) {
            txtApiResults.appendText(text);
        }
    }

    private String buildCarbonApiErrorMessage() {
        String lastError = carbonApiService != null ? carbonApiService.getLastError() : null;
        if (lastError == null || lastError.trim().isEmpty()) {
            return "❌ Aucune donnée retournée par l'API.\n";
        }
        return "❌ API Carbon Error: " + lastError + "\n";
    }

    private boolean ensureCarbonApiAvailable() {
        if (carbonApiService == null || !carbonApiService.isEnabled()) {
            appendToApiResults("❌ Carbon API non configurée. Ajoutez CARBON_API_KEY ou carbon.api.key.\n");
            showWarning("API non configurée", "La clé Carbon API est manquante. Ajoutez CARBON_API_KEY ou carbon.api.key.");
            return false;
        }
        return true;
    }

    private double convertWeightToKg(double value, String unit) {
        if (unit == null) return value;
        switch (unit) {
            case "lb":
                return value * 0.453592;
            case "g":
                return value / 1000.0;
            case "mt":
                return value * 1000.0;
            case "kg":
            default:
                return value;
        }
    }

    private double convertDistanceToKm(double value, String unit) {
        if (unit == null) return value;
        switch (unit) {
            case "mi":
                return value * 1.60934;
            case "km":
            default:
                return value;
        }
    }
}
