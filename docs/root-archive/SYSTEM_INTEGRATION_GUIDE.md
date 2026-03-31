## GREEN WALLET + BATCH + MARKETPLACE SYSTEM INTEGRATION GUIDE

### SYSTEM OVERVIEW

This system harmonizes three interconnected subsystems:
1. **Green Wallet** - Carbon credit management and holder account
2. **Batch System** - Immutable traceability with event sourcing
3. **Marketplace** - Credit trading with atomic payment + transfer

All three systems operate as ONE cohesive, ACID-compliant platform.

---

## ARCHITECTURE

### Data Flow

```
Project Evaluation
  │
  ├→ Issue Credits to Wallet
  │   ├→ Create CarbonCreditBatch
  │   ├→ Record ISSUED event (blockchain-ready)
  │   └→ Increase wallet.available_credits
  │
  ├→ Wallet Holds Credits
  │   ├→ Transfer Between Wallets (atomic)
  │   ├→ Retire Credits (permanent removal)
  │   └→ All operations tracked in batch_events
  │
  └→ Marketplace Trading
      ├→ Seller Creates Listing (references batch)
      ├→ Buyer Makes Offer
      ├→ Payment Processing (Stripe)
      ├→ ATOMIC: Transfer + Escrow + Event
      └→ Record MARKETPLACE_SOLD event
```

### Atomic Operations

**Transfer Credits (Wallet to Wallet)**
```
1. Lock both wallets (prevent concurrent access)
2. Verify source has sufficient credits
3. Deduct from source.available_credits
4. Add to destination.available_credits
5. Record wallet_transactions (TRANSFER_OUT, TRANSFER_IN)
6. Record batch_events (TRANSFERRED)
7. Commit or Rollback (ALL-OR-NOTHING)
```

**Complete Marketplace Order**
```
1. Verify payment succeeded in Stripe
2. Create escrow hold (lock funds)
3. Call TransferService.transferCredits()  ← Atomic
   a. Lock wallets
   b. Deduct from seller
   c. Add to buyer
   d. Record transactions
   e. Record events
4. Record marketplace_order_batches linkage
5. Release escrow to seller
6. Mark order COMPLETED
7. Commit or Rollback (ALL-OR-NOTHING)
```

If ANY step fails → Entire transaction rolls back
→ No orphaned credits, no double-spending, no lost funds

---

## KEY COMPONENTS

### 1. WalletService
- Core wallet CRUD operations
- Issue and retire credits
- Transfer between wallets (delegates to TransferService)
- Batch management

**Key Methods:**
- `createWallet(Wallet)`
- `issueCredits(walletId, projectId, amount, ...)`
- `retireCredits(walletId, amount, ...)`
- `transferCreditsWithMode(fromId, toId, amount, ...)`

### 2. TransferService ⭐ **CRITICAL FOR HARMONY**
- Handles ATOMIC wallet-to-wallet transfers
- Uses database locks to prevent race conditions
- Records immutable transaction trail
- **Ensures consistency between wallet + batch systems**

**Key Method:**
```java
TransferResult transferCredits(int fromWalletId, int toWalletId, 
                               double amount, String referenceNote)
```

Returns: `TransferResult` with success status and transfer ID

### 3. CarbonCreditBatch + BatchType
- Represents immutable lot of credits
- `BatchType.PRIMARY` - Issued from project
- `BatchType.SECONDARY` - Created via marketplace/split
- Links to parent batch for lineage

### 4. BatchEventService
- Records blockchain-ready events
- SHA-256 hashing with event chaining
- Full audit trail for compliance
- Events types: ISSUED, TRANSFERRED, RETIRED, MARKETPLACE_SOLD, SPLIT

### 5. MarketplaceOrderService
- Orchestrates payment + credit transfer
- Calls TransferService in atomic transaction
- Manages escrow and fund releases
- Records order-batch linkages

---

## DATABASE SCHEMA UNITY

All systems use SAME tables:

| Table | Purpose | Integration |
|-------|---------|-------------|
| `green_wallets` | Credit holders | Referenced by all operations |
| `carbon_credit_batches` | Credit lots | PRIMARY or SECONDARY type |
| `batch_events` | Immutable log | ISSUED, TRANSFERRED, RETIRED, MARKETPLACE_SOLD |
| `wallet_transactions` | Transactions | TRANSFER_OUT, TRANSFER_IN, ISSUE, RETIRE |
| `marketplace_listings` | Sell offers | References wallet + batches |
| `marketplace_orders` | Trades | Triggers atomic transfer |
| `marketplace_escrow` | Fund holds | Synced with order completion |

**Key Field Mappings:**

```
Wallet:
  - id (PK)
  - wallet_number (UNIQUE, e.g., "GW-1000001")
  - holder_name (NOT "name" - was misaligned, NOW FIXED)
  - available_credits (updated atomically)
  - retired_credits (updated atomically)

Batch:
  - id (PK)
  - batch_type ENUM('PRIMARY', 'SECONDARY') ← NOW EXISTS (BatchType.java)
  - wallet_id (current holder)
  - status ENUM('AVAILABLE', 'PARTIALLY_RETIRED', 'FULLY_RETIRED')

Event:
  - batch_id (what changed)
  - event_type (ISSUED, TRANSFERRED, RETIRED, MARKETPLACE_SOLD)
  - event_data_json (full context)
  - event_hash (SHA-256 for blockchain)
  - previous_event_hash (chain integrity)
```

---

## FIXED ISSUES

✅ **Compilation Errors**
- [FIX] Created `Models/BatchType.java` enum (was missing)
- [FIX] Added `import Models.BatchType` to WalletService, GreenWalletController

✅ **Schema Misalignment**
- [FIX] Table name: `wallet` → `green_wallets` (ALL 20+ occurrences)
- [FIX] Column name: `name` → `holder_name` (Wallet model maps correctly)
- [FIX] Removed fallback methods (`createWalletWithoutName`) - schema is now deterministic

✅ **Service Integration**
- [NEW] Created `TransferService` - atomic wallet transfers
- [NEW] Created `SystemTestController` - integration testing UI
- [ENHANCED] MarketplaceOrderService now properly calls TransferService

---

## HOW TO TEST

### Option 1: Using SystemTestController (Recommended for Quick Testing)

1. Add this to your GUI (e.g., GreenWalletController or main menu):
```java
// Click button to open test panel
Stage testStage = new Stage();
FXMLLoader loader = new FXMLLoader(getClass().getResource("/SystemTest.fxml"));
Parent root = loader.load();
testStage.setScene(new Scene(root, 600, 800));
testStage.setTitle("System Integration Test");
testStage.show();
```

2. Use test console to:
   - Create wallet
   - Issue batch (simulates project eval)
   - Transfer credits
   - View wallet balance
   - Verify batch events

### Option 2: Unit Tests
```bash
mvn test -Dtest=TransferServiceTest
```

Tests cover:
- Atomic transfer success
- Insufficient balance error
- Rollback on failure
- Concurrent access prevention

### Option 3: Manual Marketplace Test
1. Create 2 wallets
2. Issue batch to seller wallet
3. Create marketplace listing
4. Create offer + order
5. Process payment
6. **System automatically**:
   - Transfers credits from seller to buyer
   - Records batch events
   - Updates wallet balances
   - Releases escrow
7. Verify: Seller -100 credits, Buyer +100 credits, Events recorded

---

## CONSISTENCY VERIFICATION

After any operation, verify these  invariants:

### Invariant 1: Credit Conservation
```
SELECT SUM(available_credits) + SUM(retired_credits) FROM green_wallets
  = Original Total (immutable)
```

### Invariant 2: Batch Event Chain
```
For each batch:
  - Get all events in order
  - Each event.previous_event_hash = prior event.event_hash
  - No gaps or reordering
```

### Invariant 3: Marketplace Order Consistency
```
For each order:
  - order.status = COMPLETED
  - escrow.status = RELEASED_TO_SELLER  (or REFUNDED_TO_BUYER)
  - wallet transfers occurred (deduct seller, add buyer)
  - batch_events contain MARKETPLACE_SOLD entry
```

---

## INTEGRATION CHECKLIST

- [x] All three systems share same database tables
- [x] Atomic transfers prevent data corruption
- [x] Batch events log ALL operations
- [x] Marketplace orders trigger credit transfers
- [x] Schema is unified (no "wallet" vs "green_wallets" confusion)
- [x] BatchType enum exists
- [x] Column mappings are consistent (holder_name, not name)
- [x] TransferService ensures ACID properties
- [x] Event chain is blockchain-ready (SHA-256)

---

## NEXT STEPS FOR PRODUCTION

1. **Integration Tests** - Run full test suite
2. **Schema Migration** - Apply `migrate_batch_traceability.sql` if not done
3. **API Endpoints** - Create REST API for wallet/batch/marketplace operations
4. **Dashboard** - Display wallet balance, batch lineage, order history
5. **Compliance Report** - Generate audit trail for regulators
6. **Performance Tuning** - Index optimization for large datasets

---

## TROUBLESHOOTING

### "Wallet not found"
→ Check: Wallet ID exists in `green_wallets` table

### "Insufficient credits"
→ Check: `green_wallets.available_credits >= transfer_amount`

### "Transfer failed" (then rolled back)
→Check: Both wallets locked properly, no concurrent access

### "Batch events missing"
→ Check: BatchEventService.recordEvent() called from WalletService

### "Marketplace order stuck"
→ Check: Stripe payment confirmed, escrow released

---

## SUPPORT

For issues:
1. Check test console for error details
2. Verify database schema matches greenledger(7).sql
3. Check logs for transaction rollback messages
4. Review batch_events table for operation history

All operations are FULLY TRACEABLE via immutable event log.
