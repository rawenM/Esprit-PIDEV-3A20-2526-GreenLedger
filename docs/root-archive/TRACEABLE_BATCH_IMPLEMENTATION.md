# Traceable Carbon Credit Batch Implementation

## Overview

This implementation adds full metadata support and lineage traceability to carbon credit batches, enabling complete auditing from issuance through the entire lifecycle (transfers, marketplace sales, retirement).

## What Was Implemented

### 1. Enhanced Batch Creation with Metadata ✅

**Modified Files:**
- `WalletService.java` - Enhanced core batch creation logic
- `GreenWalletController.java` - Updated UI to capture all metadata fields
- `MarketplaceController.java` - Fixed provenance display bugs

**New Capabilities:**

#### Database Schema Support (Already Exists)
```sql
carbon_credit_batches:
  - verification_standard VARCHAR(100)  -- VCS, Gold Standard, CDM, CAR
  - vintage_year INT                    -- Year credits were issued
  - calculation_audit_id VARCHAR(100)   -- Audit tracking ID
```

#### Service Layer Enhancement

**WalletService.createCreditBatch()** - Now accepts full metadata:
```java
private int createCreditBatch(
    int projectId, 
    int walletId, 
    double amount,
    String calculationAuditId,      // Audit tracking
    Models.BatchType batchType,
    String verificationStandard,    // NEW: VCS, Gold Standard, etc.
    Integer vintageYear             // NEW: Issuance year
)
```

**WalletService.quickIssueCredits()** - Two signatures for backward compatibility:
```java
// Legacy signature (still works, nulls for new fields)
public boolean quickIssueCredits(int walletId, double amount, String description)

// Enhanced signature with full metadata
public boolean quickIssueCredits(
    int walletId, 
    double amount, 
    String description,
    String calculationAuditId,
    String verificationStandard,
    Integer vintageYear
)
```

#### UI Enhancement

**Green Wallet Issue Panel** - Now captures and validates:
- ✅ Amount (1-10,000 tCO₂) - Required
- ✅ Verification Standard - Required (VCS, Gold Standard, CDM, CAR)
- ✅ Vintage Year - Optional, validated (1990-2100)
- ✅ Calculation Audit ID - Optional
- ✅ Reference/Notes - Optional

**Validation Added:**
```java
// Vintage year validation
if (vintageYear < 1990 || vintageYear > 2100) {
    showWarning("Année invalide", "L'année millésime doit être entre 1990 et 2100");
    return;
}
```

### 2. Event Tracking Enhancement ✅

**BatchEventService** now records metadata in events:
```json
{
  "amount": 100.0,
  "wallet_id": 5,
  "description": "[Gold Standard] Solar project phase 1",
  "verification_standard": "Gold Standard",
  "vintage_year": 2024
}
```

Events are SHA-256 hashed and chained immutably for complete audit trail.

### 3. Marketplace Provenance Bug Fixes ✅

**Critical Bug Fixed:**
Marketplace was using `listing.getSellerId()` (user ID) instead of `listing.getWalletId()` (wallet ID) to fetch batches.

**Locations Fixed:**
1. **MarketplaceController.java:165** - Asset type column renderer
2. **MarketplaceController.java:802** - Provenance viewer

**Impact:**
- ❌ Before: Provenance viewer showed batches from wrong wallet (user's default wallet)
- ✅ After: Correctly shows batches from the listing's source wallet

## Complete Traceability Flow

### 1. Batch Issuance (PRIMARY)

**User Actions:**
1. Open Green Wallet
2. Select wallet from dropdown
3. Click **"➕ Émettre Crédits"** main button OR **"+ Nouveau Batch"** sidebar button
4. Fill in form:
   - Amount: `100.50` tCO₂
   - Standard: `Gold Standard`
   - Vintage Year: `2024`
   - Audit ID: `AUDIT-2024-001` (optional)
   - Reference: `Solar installation Phase 1`
5. Click **"✓ Confirmer Émission"**

**System Actions:**
```
CREATE carbon_credit_batches:
  - batch_id: 123
  - type: PRIMARY
  - amount: 100.50
  - verification_standard: "Gold Standard"
  - vintage_year: 2024
  - calculation_audit_id: "AUDIT-2024-001"
  - serial_number: CC-2024-000123

RECORD batch_events:
  - event_type: ISSUED
  - batch_id: 123
  - event_data: {"amount": 100.5, "standard": "Gold Standard", "vintage_year": 2024}
  - event_hash: sha256(previous_hash + event_data)

UPDATE wallet:
  - available_credits += 100.50

CREATE wallet_transactions:
  - type: ISSUE
  - amount: 100.50
  - batch_id: 123
```

### 2. Batch Listing on Marketplace

**User Actions:**
1. Go to Marketplace → My Listings
2. Click **"+ Nouvelle Offre"**
3. Select source wallet (contains batch 123)
4. Set price: `25.00` USD/tCO₂
5. Set quantity: `100.50` tCO₂
6. Click **"Publier"**

**System Actions:**
```
CREATE marketplace_listings:
  - wallet_id: 5              ✅ Source wallet (not user ID!)
  - amount: 100.50
  - price_per_unit: 25.00
  - status: ACTIVE

VERIFY provenance:
  - Get batches from listing.getWalletId() ✅ FIXED
  - Display batch serial numbers
  - Show verification standard: "Gold Standard"
  - Show vintage year: 2024
```

### 3. Marketplace Purchase & Transfer

**Buyer Actions:**
1. Browse marketplace, select listing
2. Click **"Voir Provenance"** - sees Gold Standard, 2024 vintage ✅
3. View batch lineage (double-click batch) - sees ISSUED event chain
4. Click **"Acheter"**, complete Stripe checkout

**System Actions:**
```
CREATE marketplace_orders:
  - listing_id: 456
  - buyer_wallet_id: 8
  - amount: 100.50
  - total_price: 2512.50

TRANSFER credits:
  - Mode: SPLIT_CHILD (creates new child batch for traceability)
  
CREATE carbon_credit_batches:
  - batch_id: 124
  - type: SECONDARY
  - amount: 100.50
  - parent_batch_id: 123           ✅ Lineage link
  - verification_standard: "Gold Standard"  ✅ Inherited
  - vintage_year: 2024             ✅ Inherited
  - wallet_id: 8

RECORD batch_events:
  - batch_id: 124
  - event_type: TRANSFERRED
  - event_data: {"from_wallet": 5, "to_wallet": 8, "amount": 100.5, "order_id": 789}
  
RECORD batch_events:
  - batch_id: 123
  - event_type: MARKETPLACE_SOLD
  - event_data: {"buyer_wallet": 8, "amount": 100.5, "price": 2512.50}
```

### 4. Viewing Lineage

**User Actions:**
1. In Green Wallet batch list, double-click any batch
2. Lineage viewer opens showing parent-child chain

**System Display:**
```
Batch CC-2024-000124 (SECONDARY, 100.50 tCO₂)
  ↓ from parent
Batch CC-2024-000123 (PRIMARY, 100.50 tCO₂)
  ├─ Standard: Gold Standard
  ├─ Vintage: 2024
  ├─ Audit: AUDIT-2024-001
  └─ Events:
      1. ISSUED at 2024-01-15 10:30:00
      2. MARKETPLACE_SOLD at 2024-01-20 14:45:00
```

## Testing the Implementation

### Test Flow 1: Basic Issuance with Metadata

```
1. Launch app, login, go to Green Wallet
2. Select a wallet
3. Click "➕ Émettre Crédits"
4. Enter:
   - Amount: 100
   - Standard: Gold Standard
   - Vintage: 2024
   - Audit ID: TEST-001
   - Reference: Test batch
5. Confirm → Verify batch appears in list
6. Double-click batch → View events (should see ISSUED event)
```

### Test Flow 2: Marketplace Provenance

```
1. Create listing from wallet with batches
2. As another user, browse marketplace
3. Click "Voir Provenance" on listing
4. ✅ Verify correct batches displayed (not buyer's own batches!)
5. ✅ Verify standard and vintage year shown
6. Purchase listing
7. View buyer wallet → double-click received batch
8. ✅ Verify parent_batch_id links to seller's original batch
```

### Test Flow 3: Lineage Chain

```
1. Issue batch A (100 tCO₂, Gold Standard, 2024)
2. Split batch A → creates B1 (50 tCO₂), B2 (50 tCO₂)
3. Transfer B1 to another wallet → creates C (50 tCO₂)
4. View C's lineage:
   C (SECONDARY) ← B1 (SECONDARY) ← A (PRIMARY)
5. ✅ Verify all batches inherit standard/vintage
6. ✅ Verify event chain is complete
```

## Backward Compatibility

All existing code continues to work:

**3-Parameter quickIssueCredits (Legacy):**
```java
walletService.quickIssueCredits(walletId, 100.0, "Legacy issue");
// Creates batch with null standard/vintage (still traceable!)
```

**6-Parameter quickIssueCredits (New):**
```java
walletService.quickIssueCredits(
    walletId, 
    100.0, 
    "Detailed issue",
    "AUDIT-001",        // audit ID
    "Gold Standard",    // standard
    2024               // vintage
);
// Creates batch with full metadata
```

**Impact on Existing Code:**
- ✅ ComprehensiveTestController - continues working (26+ tests)
- ✅ BatchCarbonTestController - continues working
- ✅ showQuickIssueDialog() - continues working (3-param version)
- ✅ Any other issuance code - continues working

## Key Files Modified

| File | Lines Changed | Purpose |
|------|---------------|---------|
| `WalletService.java` | 314-365, 1172-1210 | Enhanced batch creation + event tracking |
| `GreenWalletController.java` | 2452-2500 | UI metadata capture + validation |
| `MarketplaceController.java` | 165, 802 | Fixed provenance bugs |

## Database Migration Status

**Schema Status:** ✅ Already migrated

The database columns already exist (from `green_wallet_schema_v2.sql`):
- `verification_standard VARCHAR(100)`
- `vintage_year INT`

**No migration needed!** The implementation uses existing schema.

## Production Checklist

- [x] Service layer accepts metadata
- [x] UI captures and validates metadata
- [x] Database columns exist and are used
- [x] Events include metadata
- [x] Marketplace provenance fixed (2 bugs)
- [x] Backward compatibility maintained
- [x] No compilation errors
- [x] Existing tests still work

## Usage Examples

### Example 1: Issue with Full Metadata
```java
boolean success = walletService.quickIssueCredits(
    5,                          // wallet ID
    250.75,                     // amount
    "[VCS] Reforestation",      // description
    "CALC-2024-VCS-042",       // audit ID
    "VCS (Verified Carbon Standard)",  // standard
    2024                        // vintage year
);
```

### Example 2: Simple Issue (Legacy)
```java
boolean success = walletService.quickIssueCredits(
    5,                      // wallet ID
    100.0,                  // amount
    "Quick issue"           // description
    // standard=null, vintage=null, audit=null
);
```

### Example 3: View Batch Metadata
```java
List<CarbonCreditBatch> batches = walletService.getWalletBatches(walletId);
for (CarbonCreditBatch batch : batches) {
    System.out.println("Batch: " + batch.getSerialNumber());
    System.out.println("Standard: " + batch.getVerificationStandard());
    System.out.println("Vintage: " + batch.getVintageYear());
    System.out.println("Audit: " + batch.getCalculationAuditId());
}
```

## Next Steps (Optional Enhancements)

### 1. Search by Metadata
Add filters to batch list:
- Filter by verification standard
- Filter by vintage year range
- Filter by audit ID

### 2. Marketplace Filters
Allow buyers to search:
- Only Gold Standard listings
- Only 2023-2024 vintage
- Specific audit trails

### 3. Reporting
Generate audit reports:
- Batch issuance summary by standard
- Vintage year distribution
- Compliance reports (standard + vintage required)

### 4. API Integration
Expose metadata via REST API:
```json
GET /api/batches/{id}
{
  "batch_id": 123,
  "serial_number": "CC-2024-000123",
  "verification_standard": "Gold Standard",
  "vintage_year": 2024,
  "calculation_audit_id": "AUDIT-2024-001",
  "lineage": [
    {"batch_id": 123, "type": "PRIMARY", "event": "ISSUED"},
    {"batch_id": 124, "type": "SECONDARY", "event": "TRANSFERRED"}
  ]
}
```

## Support & Troubleshooting

### Issue: Batch created without standard/vintage
**Cause:** Using legacy 3-parameter quickIssueCredits  
**Solution:** Use 6-parameter version for metadata

### Issue: Marketplace shows wrong batches
**Cause:** Listing has invalid wallet_id  
**Solution:** Verify listing.getWalletId() returns correct wallet

### Issue: Lineage doesn't show parent
**Cause:** Transfer used DIRECT mode instead of SPLIT_CHILD  
**Solution:** Use transferCreditsWithMode("SPLIT_CHILD") for transfers

### Issue: Vintage year validation fails
**Cause:** Year outside 1990-2100 range  
**Solution:** Enter valid year or leave blank (optional field)

---

**Implementation Date:** $(date)  
**Status:** ✅ Production Ready  
**Testing:** ✅ Backward Compatible  
**Documentation:** ✅ Complete
