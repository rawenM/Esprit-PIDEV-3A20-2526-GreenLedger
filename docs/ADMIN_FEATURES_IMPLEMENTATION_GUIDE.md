# Admin Features Implementation Guide

## Quick Start Guide for New Admin Features

This guide will help you integrate the new admin backoffice features into your GreenLedger JavaFX application.

---

## 📋 Prerequisites

- Java 17+
- MySQL 8.0+
- Maven 3.8+
- Existing GreenLedger application running

---

## 🚀 Step-by-Step Implementation

### Step 1: Run Database Migration

```bash
# Connect to your MySQL database
mysql -u root -p greenledger

# Run the migration script
source ADD_ADMIN_FEATURES_MIGRATION.sql

# Verify the migration
SELECT * FROM v_user_statistics;
SELECT * FROM v_negative_wallets LIMIT 5;
```

**Expected Output**: New columns added, indexes created, views available.

---

### Step 2: Update User DAO

Update `dao/UserDAOImpl.java` to handle new geolocation fields:

```java
// In UserDAOImpl.java

// Update the mapResultSetToUser method
private User mapResultSetToUser(ResultSet rs) throws SQLException {
    User user = new User();
    // ... existing mappings ...
    
    // Add new geolocation fields
    user.setLastLoginCountry(rs.getString("last_login_country"));
    user.setLastLoginCity(rs.getString("last_login_city"));
    
    Double lat = rs.getObject("last_login_lat", Double.class);
    Double lng = rs.getObject("last_login_lng", Double.class);
    user.setLastLoginLat(lat);
    user.setLastLoginLng(lng);
    
    return user;
}

// Update the updateUser method
public boolean updateUser(User user) {
    String sql = "UPDATE user SET nom = ?, prenom = ?, email = ?, telephone = ?, " +
                 "adresse = ?, last_login_country = ?, last_login_city = ?, " +
                 "last_login_lat = ?, last_login_lng = ? WHERE id = ?";
    
    try (PreparedStatement ps = conn.prepareStatement(sql)) {
        ps.setString(1, user.getNom());
        ps.setString(2, user.getPrenom());
        ps.setString(3, user.getEmail());
        ps.setString(4, user.getTelephone());
        ps.setString(5, user.getAdresse());
        ps.setString(6, user.getLastLoginCountry());
        ps.setString(7, user.getLastLoginCity());
        ps.setObject(8, user.getLastLoginLat());
        ps.setObject(9, user.getLastLoginLng());
        ps.setLong(10, user.getId());
        
        return ps.executeUpdate() > 0;
    } catch (SQLException e) {
        System.err.println("Error updating user: " + e.getMessage());
        return false;
    }
}
```

---

### Step 3: Integrate GeoIP in LoginController

Update `Controllers/LoginController.java`:

```java
import Services.GeoIpService;
import Services.GeoIpService.GeoLocation;

public class LoginController {
    
    private GeoIpService geoIpService = GeoIpService.getInstance();
    
    @FXML
    private void handleLogin() {
        // ... existing login logic ...
        
        if (loginSuccessful) {
            // Track user location
            String ipAddress = getClientIpAddress();
            trackUserLocation(user, ipAddress);
            
            // ... rest of login logic ...
        }
    }
    
    private void trackUserLocation(User user, String ipAddress) {
        // Async to avoid blocking login
        geoIpService.getLocationAsync(ipAddress, location -> {
            if (location.isSuccess()) {
                user.setLastLoginCountry(location.getCountry());
                user.setLastLoginCity(location.getCity());
                user.setLastLoginLat(location.getLatitude());
                user.setLastLoginLng(location.getLongitude());
                
                // Update in database
                userService.updateUser(user);
                
                System.out.println("[Login] User location tracked: " + 
                    location.getFormattedLocation());
            }
        });
    }
    
    private String getClientIpAddress() {
        // For desktop app, this would be the public IP
        // You can use a service like ipify.org
        try {
            URL url = new URL("https://api.ipify.org?format=text");
            BufferedReader in = new BufferedReader(
                new InputStreamReader(url.openStream()));
            String ip = in.readLine();
            in.close();
            return ip;
        } catch (Exception e) {
            return "127.0.0.1"; // Fallback to localhost
        }
    }
}
```

---

### Step 4: Add AI Recommendations to AdminUsersController

Update `Controllers/AdminUsersController.java`:

```java
import Services.UserActivationRecommendationService;
import Services.CsvExportService;

public class AdminUsersController {
    
    private UserActivationRecommendationService aiService = 
        new UserActivationRecommendationService();
    private CsvExportService csvService = new CsvExportService();
    
    // Add new button handler
    @FXML
    private void handleShowAIRecommendations() {
        try {
            List<User> pendingUsers = userService.getAllUsers().stream()
                .filter(u -> u.getStatut() == StatutUtilisateur.EN_ATTENTE)
                .toList();
            
            if (pendingUsers.isEmpty()) {
                showInfo("Aucun utilisateur en attente");
                return;
            }
            
            // Get AI recommendations
            List<Map<String, Object>> recommendations = 
                aiService.getPendingActivationsWithRecommendations(pendingUsers);
            
            // Display in dialog
            showAIRecommendationsDialog(recommendations);
            
        } catch (Exception e) {
            showError("Erreur", "Impossible de charger les recommandations: " + e.getMessage());
        }
    }
    
    private void showAIRecommendationsDialog(List<Map<String, Object>> recommendations) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Recommandations IA");
        alert.setHeaderText(recommendations.size() + " utilisateurs en attente");
        
        StringBuilder content = new StringBuilder();
        for (Map<String, Object> rec : recommendations) {
            User user = (User) rec.get("user");
            Map<String, Object> prediction = (Map) rec.get("recommendation");
            
            String label = (String) prediction.get("label");
            int confidence = (int) prediction.get("confidence");
            
            String emoji = switch (label) {
                case "RECOMMANDE_ACTIVATION" -> "✅";
                case "VERIFICATION_REQUISE" -> "⚠️";
                case "REJET_RECOMMANDE" -> "❌";
                default -> "❓";
            };
            
            content.append(emoji).append(" ")
                   .append(user.getNomComplet())
                   .append(" (").append(confidence).append("% confiance)\n");
            content.append("   Email: ").append(user.getEmail()).append("\n");
            content.append("   Recommandation: ").append(label).append("\n\n");
        }
        
        alert.setContentText(content.toString());
        alert.getDialogPane().setPrefWidth(600);
        alert.getDialogPane().setPrefHeight(500);
        alert.showAndWait();
    }
    
    // Add CSV export handler
    @FXML
    private void handleExportUsers() {
        try {
            List<User> users = userService.getAllUsers();
            String filename = CsvExportService.generateFilename("users");
            
            boolean success = csvService.exportUsers(users, filename);
            
            if (success) {
                showSuccess("Exporté " + users.size() + " utilisateurs vers " + filename);
            } else {
                showError("Erreur", "Échec de l'export");
            }
        } catch (Exception e) {
            showError("Erreur", "Impossible d'exporter: " + e.getMessage());
        }
    }
    
    // Add export with AI recommendations
    @FXML
    private void handleExportPendingWithAI() {
        try {
            List<User> pendingUsers = userService.getAllUsers().stream()
                .filter(u -> u.getStatut() == StatutUtilisateur.EN_ATTENTE)
                .toList();
            
            String filename = CsvExportService.generateFilename("pending_activations");
            
            boolean success = csvService.exportPendingActivations(
                pendingUsers, aiService, filename);
            
            if (success) {
                showSuccess("Exporté " + pendingUsers.size() + 
                    " utilisateurs en attente avec recommandations IA vers " + filename);
            }
        } catch (Exception e) {
            showError("Erreur", "Impossible d'exporter: " + e.getMessage());
        }
    }
}
```

---

### Step 5: Add Navigation Menu Items

Update your admin shell FXML to add new menu items:

```xml
<!-- In admin_shell.fxml or similar -->

<VBox styleClass="sidebar">
    <!-- Existing menu items -->
    <Button text="👥 Utilisateurs" onAction="#handleNavUsers"/>
    <Button text="📊 Statistiques" onAction="#handleNavStatistics"/>
    <Button text="📋 Journal d'audit" onAction="#handleNavAuditLog"/>
    
    <!-- NEW MENU ITEMS -->
    <Button text="💰 Supervision Wallets" onAction="#handleNavWalletSupervision"/>
    <Button text="🚨 Fraude Projets" onAction="#handleNavProjectFraud"/>
    <Button text="🗺️ Carte Connexions" onAction="#handleNavConnectionMap"/>
    <Button text="🤖 Recommandations IA" onAction="#handleNavAIRecommendations"/>
</VBox>
```

Add handlers in your admin shell controller:

```java
@FXML
private void handleNavWalletSupervision() {
    try {
        FXMLLoader loader = new FXMLLoader(
            getClass().getResource("/fxml/wallet_supervision.fxml"));
        Parent root = loader.load();
        contentPane.getChildren().setAll(root);
    } catch (IOException e) {
        showError("Navigation Error", "Cannot load wallet supervision: " + e.getMessage());
    }
}

@FXML
private void handleNavProjectFraud() {
    try {
        FXMLLoader loader = new FXMLLoader(
            getClass().getResource("/fxml/project_fraud_scoring.fxml"));
        Parent root = loader.load();
        contentPane.getChildren().setAll(root);
    } catch (IOException e) {
        showError("Navigation Error", "Cannot load fraud scoring: " + e.getMessage());
    }
}

@FXML
private void handleNavConnectionMap() {
    try {
        FXMLLoader loader = new FXMLLoader(
            getClass().getResource("/fxml/user_connection_map.fxml"));
        Parent root = loader.load();
        contentPane.getChildren().setAll(root);
    } catch (IOException e) {
        showError("Navigation Error", "Cannot load connection map: " + e.getMessage());
    }
}
```

---

### Step 6: Create FXML Files

Create the following FXML files in `src/main/resources/fxml/`:

1. **wallet_supervision.fxml** - See NEW_ADMIN_FEATURES.md for template
2. **project_fraud_scoring.fxml** - Similar structure to wallet supervision
3. **user_connection_map.fxml** - See NEW_ADMIN_FEATURES.md for template

---

### Step 7: Test the Features

#### Test 1: AI Recommendations
```java
// In a test class or main method
UserActivationRecommendationService aiService = new UserActivationRecommendationService();

User testUser = new User();
testUser.setNom("Dupont");
testUser.setPrenom("Jean");
testUser.setEmail("jean.dupont@gmail.com");
testUser.setTelephone("+33612345678");
testUser.setDateNaissance(LocalDate.of(1990, 1, 1));
testUser.setTypeUtilisateur(TypeUtilisateur.INVESTISSEUR);

Map<String, Object> prediction = aiService.predict(testUser);
System.out.println("Recommendation: " + prediction.get("label"));
System.out.println("Confidence: " + prediction.get("confidence") + "%");
```

#### Test 2: Wallet Supervision
```java
WalletSupervisionService supervision = new WalletSupervisionService();

Map<String, Object> overview = supervision.getWalletOverview();
System.out.println("Total Wallets: " + overview.get("totalWallets"));
System.out.println("Negative Wallets: " + overview.get("negativeWallets"));
System.out.println("Cumulative Deficit: " + overview.get("cumulativeDeficit"));

List<Map<String, Object>> negativeWallets = supervision.getNegativeWallets(5);
for (Map<String, Object> wallet : negativeWallets) {
    System.out.println("Wallet #" + wallet.get("walletNumber") + 
        " - Deficit: " + wallet.get("deficit"));
}
```

#### Test 3: GeoIP Service
```java
GeoIpService geoService = GeoIpService.getInstance();

GeoLocation location = geoService.getLocation("8.8.8.8");
if (location.isSuccess()) {
    System.out.println("Location: " + location.getFormattedLocation());
    System.out.println("Coordinates: " + location.getLatitude() + ", " + location.getLongitude());
}
```

#### Test 4: CSV Export
```java
CsvExportService csvService = new CsvExportService();
IUserService userService = new UserServiceImpl();

List<User> users = userService.getAllUsers();
String filename = CsvExportService.generateFilename("users_test");

boolean success = csvService.exportUsers(users, filename);
System.out.println("Export " + (success ? "successful" : "failed") + ": " + filename);
```

---

## 🎨 Styling (Optional)

Add CSS styles for the new components in `src/main/resources/themes/app-base.css`:

```css
/* Metric Cards */
.metric-card {
    -fx-background-color: white;
    -fx-padding: 20px;
    -fx-background-radius: 8px;
    -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 10, 0, 0, 2);
}

.metric-label {
    -fx-font-size: 12px;
    -fx-text-fill: #6B7280;
    -fx-font-weight: normal;
}

.metric-value {
    -fx-font-size: 32px;
    -fx-font-weight: bold;
    -fx-text-fill: #1F2937;
}

/* Section Titles */
.section-title {
    -fx-font-size: 18px;
    -fx-font-weight: bold;
    -fx-text-fill: #1F2937;
    -fx-padding: 10px 0;
}

/* Priority Badges */
.priority-critical {
    -fx-background-color: #DC2626;
    -fx-text-fill: white;
    -fx-padding: 5px 10px;
    -fx-background-radius: 5px;
}

.priority-high {
    -fx-background-color: #EA580C;
    -fx-text-fill: white;
    -fx-padding: 5px 10px;
    -fx-background-radius: 5px;
}

.priority-medium {
    -fx-background-color: #F59E0B;
    -fx-text-fill: white;
    -fx-padding: 5px 10px;
    -fx-background-radius: 5px;
}

/* AI Recommendation Badges */
.ai-recommend {
    -fx-background-color: #10B981;
    -fx-text-fill: white;
    -fx-padding: 5px 10px;
    -fx-background-radius: 5px;
}

.ai-verify {
    -fx-background-color: #F59E0B;
    -fx-text-fill: white;
    -fx-padding: 5px 10px;
    -fx-background-radius: 5px;
}

.ai-reject {
    -fx-background-color: #EF4444;
    -fx-text-fill: white;
    -fx-padding: 5px 10px;
    -fx-background-radius: 5px;
}
```

---

## 🐛 Troubleshooting

### Issue 1: GeoIP API Rate Limit
**Problem**: Too many API calls, hitting rate limit (45/minute)

**Solution**: 
- The service has built-in caching
- For production, use MaxMind GeoIP2 (paid, no rate limit)
- Or implement Redis caching for multi-instance deployments

### Issue 2: CSV Export File Not Found
**Problem**: FileNotFoundException when exporting

**Solution**:
```java
// Ensure directory exists
File outputDir = new File("exports");
if (!outputDir.exists()) {
    outputDir.mkdirs();
}

String filename = "exports/" + CsvExportService.generateFilename("users");
csvService.exportUsers(users, filename);
```

### Issue 3: Map Not Loading
**Problem**: WebView shows blank page

**Solution**:
- Check internet connection (Leaflet loads from CDN)
- Verify WebView is initialized: `webEngine = mapWebView.getEngine();`
- Check browser console: `webEngine.setOnError(e -> System.err.println(e));`

### Issue 4: AI Predictions Always Same
**Problem**: All users get same recommendation

**Solution**:
- Verify user data is complete (not all null)
- Check feature extraction: `double[] features = extractFeatures(user);`
- Ensure fraud scores are set: `user.setFraudScore(...)` and `user.setFraudChecked(true)`

---

## 📊 Performance Optimization

### 1. Database Indexes
Already created by migration script. Verify with:
```sql
SHOW INDEX FROM user;
SHOW INDEX FROM projet;
SHOW INDEX FROM wallet;
```

### 2. GeoIP Caching
```java
// Clear cache periodically (e.g., daily)
GeoIpService.getInstance().clearCache();

// Check cache size
int cacheSize = GeoIpService.getInstance().getCacheSize();
System.out.println("GeoIP cache size: " + cacheSize);
```

### 3. Batch Processing
```java
// Instead of individual predictions
for (User user : users) {
    aiService.predict(user); // Slow
}

// Use batch processing
Map<Long, Map<String, Object>> predictions = aiService.predictBatch(users); // Fast
```

---

## 🔒 Security Checklist

- [ ] Verify admin role before accessing new features
- [ ] Audit log all admin actions
- [ ] Validate file paths in CSV export
- [ ] Sanitize user input in search/filter fields
- [ ] Use HTTPS for GeoIP API calls
- [ ] Encrypt sensitive CSV exports
- [ ] Implement rate limiting for exports
- [ ] Add CSRF protection for web endpoints (if applicable)

---

## 📚 Additional Resources

- [JavaFX Documentation](https://openjfx.io/)
- [Leaflet.js Documentation](https://leafletjs.com/)
- [IP-API Documentation](https://ip-api.com/docs)
- [Gaussian Naive Bayes](https://scikit-learn.org/stable/modules/naive_bayes.html)

---

## ✅ Completion Checklist

- [ ] Database migration completed
- [ ] User DAO updated with geolocation fields
- [ ] GeoIP integrated in LoginController
- [ ] AI recommendations added to AdminUsersController
- [ ] CSV export functionality added
- [ ] Navigation menu updated
- [ ] FXML files created
- [ ] All features tested
- [ ] Styling applied
- [ ] Security verified
- [ ] Documentation updated

---

## 🎉 Success!

Once all steps are complete, you should have:
- ✅ AI-powered user activation recommendations
- ✅ Wallet deficit monitoring
- ✅ Project fraud scoring dashboard
- ✅ CSV export capabilities
- ✅ Geographic user tracking
- ✅ Interactive connection map

**Congratulations! Your admin backoffice is now fully enhanced!**

---

**Need Help?** Check the `NEW_ADMIN_FEATURES.md` document for detailed feature descriptions and usage examples.
