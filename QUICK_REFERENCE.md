# GreenLedger Admin Features - Quick Reference Card

## 🚀 Quick Start (5 Minutes)

### 1. Run Database Migration
```bash
mysql -u root -p greenledger < ADD_ADMIN_FEATURES_MIGRATION.sql
```

### 2. Test AI Recommendations
```java
UserActivationRecommendationService ai = new UserActivationRecommendationService();
Map<String, Object> result = ai.predict(user);
System.out.println(result.get("label") + " - " + result.get("confidence") + "%");
```

### 3. Test Wallet Supervision
```java
WalletSupervisionService ws = new WalletSupervisionService();
Map<String, Object> overview = ws.getWalletOverview();
System.out.println("Negative wallets: " + overview.get("negativeWallets"));
```

---

## 📋 Cheat Sheet

### AI Recommendations
```java
// Single user
UserActivationRecommendationService ai = new UserActivationRecommendationService();
Map<String, Object> pred = ai.predict(user);

// Batch processing
Map<Long, Map<String, Object>> preds = ai.predictBatch(users);

// Pending users with recommendations
List<Map<String, Object>> recs = ai.getPendingActivationsWithRecommendations(pendingUsers);
```

**Output Labels**:
- `RECOMMANDE_ACTIVATION` ✅ (Safe to activate)
- `VERIFICATION_REQUISE` ⚠️ (Manual review needed)
- `REJET_RECOMMANDE` ❌ (High risk, reject)

---

### Wallet Supervision
```java
WalletSupervisionService ws = new WalletSupervisionService();

// Overview metrics
Map<String, Object> overview = ws.getWalletOverview();
// Keys: totalWallets, negativeWallets, atRiskWallets, cumulativeDeficit

// Top 25 negative wallets
List<Map<String, Object>> negative = ws.getNegativeWallets(25);

// Priority owners
List<Map<String, Object>> owners = ws.getPriorityOwners(10);

// At-risk wallets
List<Map<String, Object>> atRisk = ws.getAtRiskWallets(20);

// Owner health
Map<String, Object> health = ws.getOwnerWalletHealth("USER", 123);
```

**Priority Levels**: CRITICAL (≥1000), HIGH (≥500), MEDIUM (≥100), LOW (<100)

---

### CSV Export
```java
CsvExportService csv = new CsvExportService();

// Export users
csv.exportUsers(users, "users.csv");

// Export audit logs
csv.exportAuditLogs(logs, "audit.csv");

// Export wallets
csv.exportWallets(wallets, "wallets.csv");

// Export users with fraud
csv.exportUsersWithFraud(users, "users_fraud.csv");

// Export pending with AI
csv.exportPendingActivations(pending, aiService, "pending_ai.csv");

// Generate filename with timestamp
String filename = CsvExportService.generateFilename("users");
// Returns: users_20260508_143022.csv
```

---

### GeoIP Service
```java
GeoIpService geo = GeoIpService.getInstance();

// Synchronous lookup
GeoLocation loc = geo.getLocation("8.8.8.8");
if (loc.isSuccess()) {
    System.out.println(loc.getCountry() + ", " + loc.getCity());
    System.out.println(loc.getLatitude() + ", " + loc.getLongitude());
}

// Asynchronous lookup
geo.getLocationAsync("8.8.8.8", location -> {
    System.out.println(location.getFormattedLocation());
});

// Cache management
int size = geo.getCacheSize();
geo.clearCache();
```

**Rate Limit**: 45 requests/minute (IP-API free tier)

---

### User Model (New Fields)
```java
// Set location
user.setLastLoginCountry("France");
user.setLastLoginCity("Paris");
user.setLastLoginLat(48.8566);
user.setLastLoginLng(2.3522);

// Get location
String country = user.getLastLoginCountry();
String city = user.getLastLoginCity();
Double lat = user.getLastLoginLat();
Double lng = user.getLastLoginLng();
```

---

## 🎯 Common Use Cases

### Use Case 1: Auto-Activate Safe Users
```java
List<User> pending = userService.getPendingUsers();
UserActivationRecommendationService ai = new UserActivationRecommendationService();

for (User user : pending) {
    Map<String, Object> pred = ai.predict(user);
    if ("RECOMMANDE_ACTIVATION".equals(pred.get("label")) 
        && (int)pred.get("confidence") >= 85) {
        userService.validateAccount(user.getId());
    }
}
```

### Use Case 2: Alert on Critical Deficits
```java
WalletSupervisionService ws = new WalletSupervisionService();
List<Map<String, Object>> negative = ws.getNegativeWallets(25);

for (Map<String, Object> wallet : negative) {
    if ("CRITICAL".equals(wallet.get("priority"))) {
        sendAlert("Critical deficit: Wallet #" + wallet.get("walletNumber"));
    }
}
```

### Use Case 3: Track User Login Location
```java
// In LoginController after successful login
GeoIpService geo = GeoIpService.getInstance();
String ip = getClientIpAddress();

geo.getLocationAsync(ip, location -> {
    if (location.isSuccess()) {
        user.setLastLoginCountry(location.getCountry());
        user.setLastLoginCity(location.getCity());
        user.setLastLoginLat(location.getLatitude());
        user.setLastLoginLng(location.getLongitude());
        userService.updateUser(user);
    }
});
```

### Use Case 4: Export Weekly Report
```java
List<User> users = userService.getAllUsers();
CsvExportService csv = new CsvExportService();

String filename = "reports/" + CsvExportService.generateFilename("weekly_users");
csv.exportUsersWithFraud(users, filename);

// Email to management
emailService.sendReport("management@company.com", filename);
```

---

## 🗄️ Database Quick Reference

### New User Columns
```sql
last_login_country VARCHAR(100)
last_login_city VARCHAR(100)
last_login_lat DECIMAL(10, 8)
last_login_lng DECIMAL(11, 8)
fraud_score DOUBLE
fraud_checked BOOLEAN
```

### New Project Columns
```sql
fraud_risk_score DECIMAL(5, 2)
fraud_anomaly_score DECIMAL(5, 2)
fraud_flag TINYINT(1)
fraud_reasons TEXT
fraud_model_version VARCHAR(50)
fraud_scored_at TIMESTAMP
```

### Useful Views
```sql
SELECT * FROM v_negative_wallets LIMIT 10;
SELECT * FROM v_at_risk_wallets LIMIT 10;
SELECT * FROM v_user_statistics;
SELECT * FROM v_project_fraud_statistics;
```

### Useful Queries
```sql
-- Users by country
SELECT last_login_country, COUNT(*) 
FROM user 
WHERE last_login_country IS NOT NULL 
GROUP BY last_login_country 
ORDER BY COUNT(*) DESC;

-- High fraud risk users
SELECT nom, prenom, email, fraud_score 
FROM user 
WHERE fraud_checked = TRUE AND fraud_score >= 75 
ORDER BY fraud_score DESC;

-- Suspected projects
SELECT id, nom, fraud_risk_score, fraud_reasons 
FROM projet 
WHERE fraud_flag = 1 
ORDER BY fraud_risk_score DESC;

-- Wallet deficit summary
SELECT 
    owner_type,
    COUNT(*) as negative_count,
    SUM(ABS(available_credits)) as total_deficit
FROM wallet 
WHERE available_credits < 0 
GROUP BY owner_type;
```

---

## 🎨 UI Integration

### Add to Admin Menu
```xml
<Button text="💰 Supervision Wallets" onAction="#handleNavWalletSupervision"/>
<Button text="🚨 Fraude Projets" onAction="#handleNavProjectFraud"/>
<Button text="🗺️ Carte Connexions" onAction="#handleNavConnectionMap"/>
<Button text="🤖 Recommandations IA" onAction="#handleNavAIRecommendations"/>
<Button text="📥 Export CSV" onAction="#handleExportCSV"/>
```

### Navigation Handlers
```java
@FXML
private void handleNavWalletSupervision() {
    loadView("/fxml/wallet_supervision.fxml");
}

@FXML
private void handleNavProjectFraud() {
    loadView("/fxml/project_fraud_scoring.fxml");
}

@FXML
private void handleNavConnectionMap() {
    loadView("/fxml/user_connection_map.fxml");
}
```

---

## 🔧 Troubleshooting

### Problem: GeoIP returns localhost
**Solution**: User is on local network. Use public IP or test with real IP.

### Problem: AI always returns same result
**Solution**: Check user data completeness. Ensure fraud_score is set.

### Problem: CSV export fails
**Solution**: Create output directory first:
```java
new File("exports").mkdirs();
```

### Problem: Map doesn't load
**Solution**: Check internet connection (Leaflet loads from CDN).

### Problem: Negative wallets query slow
**Solution**: Ensure indexes exist:
```sql
CREATE INDEX idx_wallet_available_credits ON wallet(available_credits);
```

---

## 📊 Performance Tips

1. **Batch AI Predictions**: Use `predictBatch()` for >10 users
2. **Cache GeoIP**: Service has built-in caching
3. **Limit Results**: Use LIMIT in queries (default: 25)
4. **Use Views**: Pre-computed views are faster
5. **Index Columns**: Migration script adds all needed indexes

---

## 🔒 Security Checklist

- [ ] Verify admin role before access
- [ ] Log all admin actions via AuditLogService
- [ ] Validate file paths in CSV export
- [ ] Use HTTPS for GeoIP API
- [ ] Sanitize user input in filters
- [ ] Encrypt sensitive exports
- [ ] Rate limit CSV exports

---

## 📞 Quick Help

| Issue | Solution |
|-------|----------|
| Can't find new services | Check package: `Services/` |
| Database error | Run migration script |
| AI not working | Verify user data completeness |
| Map blank | Check internet connection |
| CSV empty | Verify data exists in database |
| GeoIP fails | Check API rate limit (45/min) |

---

## 📚 Documentation Files

1. **ADMIN_FEATURES_SUMMARY.md** - Overview and benefits
2. **NEW_ADMIN_FEATURES.md** - Complete feature documentation
3. **ADMIN_FEATURES_IMPLEMENTATION_GUIDE.md** - Step-by-step integration
4. **ADD_ADMIN_FEATURES_MIGRATION.sql** - Database migration
5. **QUICK_REFERENCE.md** - This file (cheat sheet)

---

## ✅ Integration Checklist

- [ ] Run database migration
- [ ] Update UserDAOImpl with geolocation fields
- [ ] Add GeoIP to LoginController
- [ ] Add AI recommendations to AdminUsersController
- [ ] Add CSV export buttons
- [ ] Create FXML files (3 files)
- [ ] Update navigation menu
- [ ] Test all features
- [ ] Apply CSS styling
- [ ] Verify security

---

## 🎯 Key Metrics

| Metric | Value |
|--------|-------|
| AI Accuracy | 83.33% |
| Features Analyzed | 9 |
| Training Examples | 208 |
| GeoIP Rate Limit | 45/min |
| Default Wallet Limit | 25 |
| CSV Encoding | UTF-8 |
| Date Format | yyyy-MM-dd HH:mm:ss |

---

**Print this page for quick reference while coding!**

**Version**: 1.0 | **Date**: May 8, 2026
