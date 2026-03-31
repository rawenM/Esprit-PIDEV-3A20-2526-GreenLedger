# Implementation Issues & Fixes

**Date:** March 1, 2026  
**Status:** Analysis Complete, Ready for Implementation

---

## Issue 1: Climatiq API Not Working

### Problem
Emissions calculation API doesn't work because it requires `CLIMATIQ_API_KEY` environment variable.

### Current State
```java
// ClimatiqApiService.java (Line 75)
this.apiKey = System.getenv("CLIMATIQ_API_KEY");
this.enabled = apiKey != null && !apiKey.isEmpty();
```

When `enabled = false`, the service falls back to baseline factors (generic, not project-specific).

### Solution
**You need to:**
1. Get a Climatiq API key from https://www.climatiq.io/
2. Set environment variable `CLIMATIQ_API_KEY=your_key_here`

**Command (Windows):**
```powershell
$env:CLIMATIQ_API_KEY = "your_climatiq_api_key_here"
# Then run your app
mvn clean package
java -jar target/workshopjdbc3a-1.0-SNAPSHOT.jar
```

Or **Set permanently in Windows Environment Variables:**
1. Open Settings → Environment Variables
2. Add new User Variable: `CLIMATIQ_API_KEY` = `your_key`
3. Restart IDE/application

### What It Will Enable
- ✅ Real CO₂ emissions calculations for projects
- ✅ Scope 1/2/3 breakdown accurate to your company's activities
- ✅ Integration with 330,000+ emission factors
- ✅ Grid-aware electricity calculations

---

## Issue 2: Cannot Buy/Pay When Offer Accepted

### Problem
When seller accepts an offer, buyer is notified but **no payment processing happens**. The flow stops at acceptance.

### Current Code Flow
```
Buyer Makes Offer
  ↓
Seller Receives Offer (Status: PENDING)
  ↓
Seller Clicks "Accept Offer"
  ↓
offerService.acceptOffer() called
  ↓
Status → ACCEPTED
  ↓
⚠️ NO PAYMENT PROCESSING ⚠️
  ↓
Credits don't transfer
```

### Missing Implementation
**File:** `src/main/java/Controllers/MarketplaceController.java`  
**Method:** `handleAcceptOffer()` (Line 873)

**Current Code:**
```java
private void handleAcceptOffer() {
    // ... validation ...
    if (offerService.acceptOffer(selected.getId())) {
        showAlert("✓ Offer accepted!\nThe buyer will be notified to complete payment.");
        loadOffers();
        loadMyListings();
    }
}
```

**Needs:** After offer is accepted, system should:
1. Create marketplace order
2. Initialize Stripe payment intent
3. Send payment link to buyer
4. Wait for payment confirmation
5. Transfer credits automatically

### Solution Implementation Required
```java
private void handleAcceptOffer() {
    MarketplaceOffer selected = offersReceivedTable.getSelectionModel().getSelectedItem();
    if (selected == null) {
        showAlert("Please select an offer to accept");
        return;
    }

    double finalPrice = selected.getCounterPriceUsd() != null ? 
                       selected.getCounterPriceUsd() : selected.getOfferPriceUsd();
    
    if (confirmAction("Accept offer?")) {
        try {
            // Step 1: Accept the offer
            if (!offerService.acceptOffer(selected.getId())) {
                showAlert("Error accepting offer");
                return;
            }
            
            // Step 2: Create marketplace order
            int orderId = orderService.createOrder(
                selected.getListingId(),
                selected.getBuyerId(),
                selected.getSellerId(),
                selected.getQuantity(),
                finalPrice
            );
            
            if (orderId <= 0) {
                showAlert("Error creating order");
                return;
            }
            
            // Step 3: Process payment with Stripe
            String paymentLink = stripeService.createPaymentIntent(
                orderId,
                finalPrice * selected.getQuantity(),
                selected.getBuyerId(),
                selected.getSellerId()
            );
            
            // Step 4: Notify buyer
            showAlert("Offer accepted!\nBuyer will receive payment link:\n" + paymentLink);
            
            loadOffers();
            loadMyListings();
            
        } catch (Exception e) {
            showAlert("Error processing offer: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
```

### Required Services
- `MarketplaceOrderService.createOrder()`
- `StripePaymentService.createPaymentIntent()`
- `WebhookHandler` to listen for `payment_intent.succeeded`

---

## Issue 3: Market Pricing Graph Only Has 2 Points

### Problem
The 30-day pricing history chart shows only 2-3 data points instead of ~30 days of history.

### Likely Causes

**A) Data not being collected daily**
- Need scheduled task to capture daily prices
- Currently may store prices only on transaction, not periodically

**B) Chart not fetching historical data**
- Chart query may be filtered incorrectly
- Date range may be too narrow

**C) Insufficient transaction data**
- If few sales happened, fewer price points exist

### Solution

**Create daily price capture cron job:**

```java
// New file: src/main/java/Services/MarketplacePricingHistoryService.java
@Service
@EnableScheduling
public class MarketplacePricingHistoryService {
    
    private final Database db;
    
    @Scheduled(cron = "0 0 0 * * ?")  // Daily at midnight
    public void captureDaily PriceHistory
() {
        // For each active listing:
        // 1. Calculate average price for that asset type
        // 2. Insert into marketplace_price_history table
        // 3. Update 30-day moving average
    }
}
```

**Ensure database has price history table:**
```sql
CREATE TABLE marketplace_price_history (
    id INT AUTO_INCREMENT PRIMARY KEY,
    asset_type ENUM('CARBON_CREDITS', 'WALLET') NOT NULL,
    average_price_usd DECIMAL(10, 4) NOT NULL,
    min_price_usd DECIMAL(10, 4),
    max_price_usd DECIMAL(10, 4),
    transaction_count INT DEFAULT 0,
    recorded_date DATE NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY (asset_type, recorded_date)
);
```

---

## Issue 4: Scope Analysis Chart Shows No Data

### Problem
The "Décomposition par Scope" (Scope breakdown) charts appear but show no data visualization.

### Located In
**File:** `src/main/java/Controllers/greenwallet/ScopeAnalysisController.java`  
**FXML:** `src/main/resources/greenwallet.fxml`

### Likely Causes

**A) Climatiq API disabled** → Uses baseline factors only → Shows unrepresentative data  
**B) Data not flowing to charts** → Service returns data but controller doesn't bind to UI  
**C) Chart initialization timing** → Charts render before data loads  

### Debug Steps

1. **Check if Climatiq enabled:**
```java
System.out.println("Climatiq enabled: " + climatiqService.isEnabled());
```

2. **Verify data is being fetched:**
```java
// In ScopeAnalysisController.loadScopeData() method
List<CarbonReport> reports = scopeCalculatorService.calculateScopes();
System.out.println("Scope reports: " + reports.size());
reports.forEach(r -> System.out.println(r.getScope() + ": " + r.getCo2eAmount()));
```

3. **Check chart binding:**
```java
// Ensure XYChart has data series defined
XYChart.Series<String, Number> scope1Series = new XYChart.Series<>();
scope1Series.setName("Scope 1");
scope1Series.getData().add(new XYChart.Data<>("Q1", scope1Data));
scopeChart.getData().add(scope1Series);
```

### Solution
Fix chart data binding in `ScopeAnalysisController`:

```java
private void populateScopeChart() {
    try {
        List<CarbonReport> reports = scopeCalculatorService.calculateScopes();
        
        // Clear existing data
        scopeChart.getData().clear();
        
        // Create series for each scope
        XYChart.Series<String, Number> scope1 = new XYChart.Series<>();
        scope1.setName("Scope 1");
        
        XYChart.Series<String, Number> scope2 = new XYChart.Series<>();
        scope2.setName("Scope 2");
        
        XYChart.Series<String, Number> scope3 = new XYChart.Series<>();
        scope3.setName("Scope 3");
        
        // Populate data
        for (CarbonReport report : reports) {
            String category = report.getCategory();
            double amount = report.getCo2eAmount().doubleValue();
            
            switch(report.getScope()) {
                case 1:
                    scope1.getData().add(new XYChart.Data<>(category, amount));
                    break;
                case 2:
                    scope2.getData().add(new XYChart.Data<>(category, amount));
                    break;
                case 3:
                    scope3.getData().add(new XYChart.Data<>(category, amount));
                    break;
            }
        }
        
        // Add to chart
        scopeChart.getData().addAll(scope1, scope2, scope3);
        
    } catch (Exception e) {
        System.err.println("Error populating scope chart: " + e.getMessage());
        e.printStackTrace();
    }
}
```

---

## Issue 5: Remove Unwanted UI Buttons

### Buttons Currently in UI

**Expert Project View (`expertProjet.fxml`):**
- `btnGestionProjets` - "📁 Voir Projets"
- `btnGestionEvaluations` - "📋 Évaluations"
- `btnSettings` - "⚙ Settings"
- "✏️ Modifier profil" button

**Financement View (`financement.fxml`):**
- `btnNewFinancement` - "➕ Nouveau Financement"
- `btnNewOffre` - "➕ Nouvelle Offre"
- `btnRefresh` - "🔄 Actualiser"
- `btnNavSettings` - "⚙️ Paramètres"
- `btnAddFinancement` - Add button in table
- `btnEditFinancement` - Edit button in table
- `btnDeleteFinancement` - Delete button in table

### Which to Remove?
**Please specify which buttons you want removed. Examples:**
- "Remove all Settings buttons"
- "Remove Nouveau Financement button" 
- "Remove table action buttons (Edit/Delete)"
- "Keep only core marketplace buttons"

Once you specify, I'll:
1. Remove from FXML files
2. Remove from Controller handlers
3. Remove any event listeners

---

## Summary of Changes Needed

| Issue | File(s) | Type | Priority |
|-------|---------|------|----------|
| Climatiq API | Config | Environment | 🔴 HIGH |
| Payment Flow | MarketplaceController.java | Logic | 🔴 HIGH |
| Price History | New Service | New Code | 🟡 MEDIUM |
| Scope Charts | ScopeAnalysisController.java | Logic | 🟡 MEDIUM |
| UI Buttons | *.fxml + Controllers | UI/Code | 🟢 LOW |

---

## Next Steps

1. **Set Climatiq API key** (enables real emissions data)
2. **Implement payment processing** (enables actual purchases)
3. **Specify which buttons to remove** (UI cleanup)
4. **Add price history tracking** (enables chart data)
5. **Fix scope chart binding** (enables visualization)

Would you like me to proceed with implementing any of these fixes?
