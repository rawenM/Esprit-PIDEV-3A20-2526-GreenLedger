# QUICK START: TESTING YOUR SYSTEM TODAY

## TL;DR - Get Started in 5 Minutes

### Step 1: Verify Compilation
```bash
cd d:\PiDev\Pi_Dev
mvn clean compile
```
✓ Expected: `BUILD SUCCESS`  
✗ If error: Check Java 17 + Maven 3.x installed

### Step 2: Load System Test Console
1. Run your GreenWallet application
2. Add this to your main menu/dashboard:
   ```java
   Button testBtn = new Button("System Integration Test");
   testBtn.setOnAction(e -> openTestConsole());
   ```
3. Implement the method:
   ```java
   private void openTestConsole() {
       try {
           Stage testStage = new Stage();
           FXMLLoader loader = new FXMLLoader(
               getClass().getResource("/SystemTest.fxml")
           );
           Parent root = loader.load();
           Scene scene = new Scene(root, 700, 900);
           testStage.setScene(scene);
           testStage.setTitle("System Integration Test");
           testStage.show();
       } catch (IOException ex) {
           ex.printStackTrace();
       }
   }
   ```

### Step 3: Run Test Sequence
**In Test Console:**

#### Test 1: Create Wallet
```
Button: "Create New Wallet"
Output: ✓ Wallet created successfully
         Wallet ID: [copy this ID]
```

#### Test 2: Issue Batch
```
Wallet ID: [paste from Test 1]
Project ID: 1
Amount: 500
Button: "Issue Credits to Wallet"

Output: ✓ Batch issued successfully
        Project: 1
        Wallet: [ID]
        Amount: 500 credits
```

#### Test 3: View Balance
```
Wallet ID: [same as above]
Button: "View Wallet & Batch History"

Output: ✓ Wallet details:
        Available Credits: 500.00
        Retired Credits: 0.00
        Total: 500.00
```

#### Test 4: Transfer Credits
```
From Wallet ID: [first wallet]
To Wallet ID: [need new wallet - repeat Test 1 first!]
Amount: 100
Button: "Transfer Credits (Atomic)"

Output: ✓ Transfer successful
        Transfer ID: [ID]
        From Wallet: [ID1]
        To Wallet: [ID2]
        Amount: 100 credits
```

#### Test 5: Verify Transfer
```
Wallet ID: [first wallet]
Button: "View Wallet & Batch History"

Output: ✓ Wallet details:
        Available Credits: 400.00  ← Decreased by 100
```

---

## DETAILED TEST SCENARIOS

### Scenario 1: Single Wallet Lifecycle
**Goal**: Create wallet → Issue credits → Retire credits → Verify balance

1. **Create Wallet**
   ```
   Button: Create New Wallet
   Note: Wallet ID for next steps
   ```

2. **Issue Credits**
   ```
   Wallet ID: [from step 1]
   Project ID: 101
   Amount: 1000
   Button: Issue Credits
   ```

3. **View Wallet**
   ```
   Wallet ID: [from step 1]
   Button: View Wallet
   Expected: Available Credits = 1000
   ```

### Scenario 2: Two-Wallet Transfer
**Goal**: Verify atomic transfer (debits = credits)

1. **Create Wallet 1**
   ```
   Button: Create New Wallet
   Note: Wallet ID = W1
   ```

2. **Create Wallet 2**
   ```
   Button: Create New Wallet
   Note: Wallet ID = W2
   ```

3. **Issue Batch to W1**
   ```
   Wallet ID: W1
   Project ID: 101
   Amount: 500
   Button: Issue Credits
   ```

4. **Transfer 200 Credits (W1 → W2)**
   ```
   From Wallet ID: W1
   To Wallet ID: W2
   Amount: 200
   Button: Transfer Credits
   ```

5. **Verify Balances**
   ```
   View W1:
   ├─ Available Credits = 300 (500 - 200)
   └─ Status: ✓ CORRECT
   
   View W2:
   ├─ Available Credits = 200 (0 + 200)
   └─ Status: ✓ CORRECT
   ```

### Scenario 3: Multiple Transfers
**Goal**: Verify credit conservation across multiple transfers

1. Create 3 wallets (W1, W2, W3)
2. Issue 1000 credits to W1
3. Transfer 300 W1 → W2
4. Transfer 200 W1 → W3
5. Transfer 100 W2 → W3
6. **Verify Total Conservation**
   ```
   W1: 500 (1000 - 300 - 200)
   W2: 200 (300 - 100)
   W3: 300 (200 + 100)
   Sum: 1000 ✓ (unchanged)
   ```

### Scenario 4: Error Handling
**Goal**: Verify error rollback (transfer with insufficient balance)

1. Create Wallet
2. Issue 100 credits (only)
3. Attempt transfer of 500 credits
4. **Expected Error**
   ```
   ✗ Transfer failed: Insufficient credits in source wallet
   Status: FAILED
   ```

5. **Verify Balance Unchanged**
   ```
   Available Credits: 100 (unchanged)
   ``` 
   → Rollback worked! ✓

---

## DATABASE VERIFICATION QUERIES

**Run these in database client to verify operations:**

### Check Wallets
```sql
SELECT id, wallet_number, holder_name, available_credits, retired_credits
FROM green_wallets
ORDER BY id DESC;
```
Expected: New wallets appearing

### Check Batches
```sql
SELECT id, batch_type, wallet_id, status, initial_credits
FROM carbon_credit_batches
ORDER BY id DESC;
```
Expected: PRIMARY batches from Test 2

### Check Transactions
```sql
SELECT id, wallet_id, transaction_type, amount, batch_id
FROM wallet_transactions
ORDER BY id DESC LIMIT 10;
```
Expected: ISSUE, TRANSFER_OUT, TRANSFER_IN entries

### Check Event Chain
```sql
SELECT batch_id, event_type, event_hash, previous_event_hash
FROM batch_events
ORDER BY batch_id, id;
```
Expected: Chain links valid (previous_hash matches prior event_hash)

### Verify Credit Conservation
```sql
SELECT 
  SUM(available_credits) as available,
  SUM(retired_credits) as retired,
  SUM(available_credits + retired_credits) as total
FROM green_wallets;
```
Expected: Total = Initial issued (conservation)

---

## TROUBLESHOOTING

### Problem: "Cannot find symbol" compilation error
**Solution**:
```bash
mvn clean compile
```

### Problem: "Wallet not found" in test
**Solution**:
```
Action: Click "Create New Wallet" first
Note: Copy the Wallet ID from the output
```

### Problem: "Insufficient credits" error when transferring
**Solution**:
```
Check: Source wallet has enough credits
Action: Run "View Wallet" to check balance first
```

### Problem: Test console won't open
**Solution**:
```
Check: SystemTest.fxml exists in src/main/resources
Check: SystemTestController.java is compiled
Try: Clean build: mvn clean compile
```

### Problem: Database connection error
**Solution**:
```
Check: MariaDB/MySQL running
Check: Database credentials in config.properties
Check: green_wallets table exists
Query: DESCRIBE green_wallets;
```

---

## SUCCESS CRITERIA

### ✅ All Tests Pass If:
1. ✓ Wallet creation returns valid ID
2. ✓ Issue credits increases available_credits
3. ✓ Transfer decreases source, increases destination
4. ✓ Balances conserve (sum unchanged)
5. ✓ Event chain is valid (hashes match)

### 🔴 System Not Ready If:
1. ✗ Compilation fails
2. ✗ Wallet creation fails
3. ✗ Credits don't transfer
4. ✗ Balances don't match (conservation broken)
5. ✗ Event chain has gaps

---

## NEXT STEPS

### If Tests PASS ✅
1. Test marketplace order flow
   - Create seller wallet + issue batch
   - Create marketplace listing
   - Create buyer + order
   - Process payment → verify auto-transfer
   
2. Deploy to production
   - Run full integration tests
   - Configure monitoring
   - Set up backups

### If Tests FAIL ❌
1. Check compilation errors
2. Verify database schema
3. Review error logs
4. Check IMPLEMENTATION_COMPLETION_REPORT.md

---

## SYSTEM TEST CONSOLE FIELDS

| Field | Purpose | Example |
|-------|---------|---------|
| **Wallet ID** | For issue batch | 1 |
| **Project ID** | Simulation of project | 101 |
| **Amount** | Credits | 500 |
| **From Wallet ID** | Source in transfer | 1 |
| **To Wallet ID** | Destination | 2 |
| **Transfer Amount** | Credits moved | 100 |
| **View Wallet ID** | Balance lookup | 1 |

---

## COMMAND REFERENCE

### Check Database
```bash
mysql -u root -p greenledger
mysql> DESCRIBE green_wallets;
mysql> SELECT * FROM green_wallets;
mysql> SELECT * FROM batch_events ORDER BY id DESC LIMIT 5;
```

### Rebuild Project
```bash
mvn clean
mvn compile
mvn test
mvn package
```

### Clear Test Data (if needed)
```sql
DELETE FROM wallet_transactions;
DELETE FROM batch_events;
DELETE FROM carbon_credit_batches;
DELETE FROM green_wallets;
```

---

## EXPECTED OUTPUT EXAMPLES

### Successful Test Run
```
=== Green Wallet System Test Console Initialized ===
Use this panel to test wallet, batch, and marketplace operations
All changes are atomic and fully traceable

[User clicks: Create New Wallet]
✓ Wallet created successfully
  Wallet ID: 1

[User enters: Wallet ID=1, Project=101, Amount=500]
[User clicks: Issue Credits to Wallet]
✓ Batch issued successfully
  Project: 101
  Wallet: 1
  Amount: 500 credits

[User clicks: View Wallet]
✓ Wallet details:
  ID: 1
  Wallet Number: GW-1000001
  Name: Test Enterprise Wallet
  Owner Type: ENTERPRISE
  Available Credits: 500.00
  Retired Credits: 0.00
  Total: 500.00

Status: Retrieved wallet 1: 500.00 available
```

---

## PERFORMANCE EXPECTATIONS

| Operation | Time |Throughput |
|-----------|------|-----------|
| Create Wallet | < 100ms | 10+/sec |
| Issue Batch | < 200ms | 5+/sec |
| Transfer Credits | < 150ms | 6+/sec |
| View Wallet | < 50ms | 20+/sec |
| Event Chain Verify | < 500ms | 2+/sec |

---

## PRODUCTION READINESS CHECKLIST

- [ ] Compilation successful (mvn compile)
- [ ] All test scenarios passing
- [ ] No database errors
- [ ] Credit conservation verified
- [ ] Event chain integrity checked
- [ ] Error handling working
- [ ] Marketplace flow tested
- [ ] Performance acceptable
- [ ] Monitoring configured
- [ ] Backups scheduled

Once all items checked → **READY FOR PRODUCTION**

---

**Last Updated**: Today  
**Status**: Ready for Testing  
**Confidence**: 95%
