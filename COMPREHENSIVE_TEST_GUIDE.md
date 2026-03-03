# 🧪 Comprehensive Test Panel - User Guide

## Quick Start

### Method 1: Run Directly from IDE
```
Right-click: Controllers/TestLauncher.java → Run 'TestLauncher.main()'
```

### Method 2: Run from Terminal
```bash
cd d:\PiDev\Pi_Dev
mvn clean compile
mvn exec:java -Dexec.mainClass="Controllers.TestLauncher"
```

### Method 3: Integrate in Main App
Add a menu item or button that calls:
```java
TestLauncher.launchTestPanel();
```

---

## 📋 Test Categories

### 💰 Wallet Operations
- **Create Wallet**: Creates test wallet with specified owner type
- **Get Wallet**: Retrieves and displays wallet details by ID
- **List Wallets**: Shows first 20 wallets in system
- **Update Wallet**: Changes wallet name

**Example Flow:**
1. Enter "ENTERPRISE" in Owner Type field
2. Click "Create Wallet" → Returns wallet ID
3. Use returned ID to "Get Wallet" details

---

### 📦 Batch Operations
- **Issue Credits**: Creates credits with batch traceability
- **List Batches**: Shows all batches for a wallet
- **Batch Lineage**: Displays parent-child batch tree
- **Batch Events**: Shows immutable event log for batch
- **Retire Credits**: Permanently retires credits

**Example Flow:**
1. Use wallet ID from wallet test
2. Enter amount (e.g., 1000)
3. Click "Issue Credits" → Creates PRIMARY batch
4. Click "List Batches" to see batch details
5. Copy batch ID and click "Batch Events" to see history

---

### 🔄 Transfer Operations
- **Transfer (DIRECT)**: Simple credit transfer, reuses existing batches
- **Transfer (SPLIT_CHILD)**: Creates child batches for full traceability
- **Split Batch**: Manually splits batch into parent/child

**Example Flow:**
1. Create two wallets (sender & receiver)
2. Issue credits to sender wallet
3. Enter From/To wallet IDs and amount
4. Click "Transfer (SPLIT_CHILD)" for marketplace-style transfer
5. Check lineage to see parent-child relationship

---

### 🛒 Marketplace Operations
- **Create Listing**: Lists credits for sale
- **List Marketplace**: Shows active listings
- **Place Order**: Creates pending order
- **Complete Order**: Processes payment and transfers credits
- **List Orders**: Shows orders by buyer or all orders

**Example Flow:**
1. Create seller wallet and issue credits
2. Enter wallet ID, quantity, price per unit
3. Click "Create Listing" → Returns listing ID
4. Create buyer and enter listing ID, buyer ID, quantity
5. Click "Place Order" → Returns order ID
6. Click "Complete Order (Mock)" → Transfers credits with batch events

---

### 💳 Payment Integration
- **Test Stripe Config**: Verifies Stripe API setup
- **Create Payment Intent**: Tests payment intent creation
- **Test Checkout Session**: Creates hosted checkout URL
- **Test Webhook**: Validates webhook handler

**Example Flow:**
1. Click "Test Stripe Config" → Should show TEST mode
2. Enter amount (e.g., 100.00)
3. Click "Create Payment Intent" → Returns payment ID
4. Use Stripe dashboard to verify payment appears

---

### 🗄️ Database Integrity
- **Test Connection**: Verifies MySQL connection
- **Check Schema**: Validates all required tables exist
- **Check Indexes**: Lists database indexes
- **Check Constraints**: Shows foreign key relationships
- **Find Orphaned Records**: Detects data integrity issues

**Example Flow:**
1. Click "Test Connection" → Should show green checkmark
2. Click "Check Schema" → Validates 7 core tables
3. Click "Find Orphaned Records" → Should report 0 orphans

---

### 🔗 End-to-End Integration Tests
- **Full Purchase Flow**: Complete marketplace transaction from listing to transfer
- **Batch Lifecycle**: Tests issue → transfer → retire flow
- **Escrow Flow**: Tests large orders (>$10k) with SPLIT_CHILD mode
- **Stress Test**: Runs 100 operations to test performance

**Example Flow:**
1. Click "Full Purchase Flow" → Automated E2E test
2. Watch console log for step-by-step progress
3. Verify all steps pass (wallet creation, listing, order, transfer)

---

## 🛠️ Utilities

### Clear Log
Clears console and resets statistics counter

### Export Log
Saves current test log to text file

### Reset Test Data
⚠️ **DANGER**: Deletes all test data from database
- Use only in development environment
- Requires confirmation dialog

### Options
- ✓ **Verbose Logging**: Shows detailed stack traces on errors
- ✓ **Auto-Scroll**: Keeps latest log entries visible
- ✓ **Show Timestamps**: Adds [HH:mm:ss] to each log line

---

## 📊 Statistics Display

Bottom bar shows:
- **Tests Run**: Total number of tests executed
- **Pass**: Successfully completed tests
- **Fail**: Failed or errored tests
- **Success Rate**: Percentage of passing tests

---

## 🎯 Common Test Scenarios

### Scenario 1: Test Basic System Integration
```
1. Create Wallet → Note ID (e.g., 123)
2. Issue Credits → Enter wallet 123, amount 1000
3. List Batches → Verify batch created
4. Batch Events → See ISSUED event
```

### Scenario 2: Test Marketplace Flow
```
1. Create 2 wallets (seller & buyer)
2. Issue credits to seller
3. Create Listing with seller wallet
4. Place Order as buyer
5. Complete Order → Verify credit transfer
6. Check buyer's batches → Should see MARKETPLACE_SOLD event
```

### Scenario 3: Test Transfer Modes
```
1. Create 2 wallets
2. Issue 1000 credits to wallet A
3. Test DIRECT transfer (300 credits)
4. Test SPLIT_CHILD transfer (400 credits)
5. Compare batch lineage between modes
   - DIRECT: Reuses existing batches
   - SPLIT_CHILD: Creates child batches
```

### Scenario 4: Verify Database Integrity
```
1. Test Connection → Green ✓
2. Check Schema → All 7 tables ✓
3. Check Indexes → Performance indexes exist
4. Check Constraints → Foreign keys intact
5. Find Orphaned Records → Should be 0
```

---

## 🐛 Troubleshooting

### "Invalid integer" or "Invalid number" errors
- Make sure numeric fields contain only digits
- Don't leave required fields empty

### "Wallet not found" errors
- Use "List Wallets" to find valid wallet IDs
- Create new wallet if needed

### Database connection errors
- Check MySQL server is running
- Verify `config.properties` database credentials
- Run "Test Connection" to diagnose

### Compilation errors
- Run `mvn clean compile` before launching
- Check all service dependencies are in classpath

### Stripe test failures
- Verify `api-config.properties` has Stripe test key
- Use test card: 4242 4242 4242 4242
- Check Stripe dashboard for test mode status

---

## 💡 Tips

1. **Use verbose logging** when debugging failures
2. **Export logs** before running destructive tests
3. **Start simple**: Test wallet → batch → transfer → marketplace
4. **Copy generated IDs** from output to use in other tests
5. **Auto-scroll enabled** keeps latest results visible
6. **Timestamps help** when analyzing test timing issues

---

## 🔒 Safety Notes

⚠️ **Reset Test Data** is PERMANENT - use with caution
⚠️ Database tests run on live database - don't use in production
⚠️ Payment tests use Stripe TEST mode - verify before running
⚠️ Stress tests may slow down system temporarily

---

## 📝 Test Results Interpretation

### ✓ PASSED (123ms)
Test completed successfully within 123 milliseconds

### ✗ FAILED (45ms)
Test logic returned false or assertion failed

### ✗ EXCEPTION: SQLException
Test threw an exception (enable verbose for stack trace)

---

## System Architecture Tested

```
┌─────────────────────────────────────────┐
│         Comprehensive Test Panel         │
└──────────────┬──────────────────────────┘
               │
    ┌──────────┼──────────┐
    ▼          ▼          ▼
 Wallet    Batch    Marketplace
Service   Events     Orders
    │          │          │
    └──────────┴──────────┘
               │
         ┌─────┴─────┐
         ▼           ▼
    Database    Stripe API
```

---

## Need Help?

- **Check logs**: Enable verbose logging for detailed errors
- **Database issues**: Run "Check Schema" and "Test Connection"
- **Integration issues**: Run E2E tests to isolate problem area
- **Payment issues**: Verify Stripe config and test mode

**All tests isolated**: Each test creates its own data, no dependencies between tests.

---

## Quick Reference Card

| Task | Input Fields | Button |
|------|--------------|--------|
| Create wallet | Owner Type | Create Wallet |
| Issue credits | Wallet ID, Amount | Issue Credits |
| Transfer (simple) | From, To, Amount | Transfer (DIRECT) |
| Transfer (traceable) | From, To, Amount | Transfer (SPLIT_CHILD) |
| Create listing | Wallet ID, Qty, Price | Create Listing |
| Buy credits | Listing ID, Buyer ID, Qty | Place Order → Complete Order |
| Check database | (none) | Test Connection |
| Run full test | (none) | Full Purchase Flow |

---

**Version**: 1.0  
**Last Updated**: 2024  
**Tested Systems**: Wallet • Batch • Marketplace • Payment • Database
