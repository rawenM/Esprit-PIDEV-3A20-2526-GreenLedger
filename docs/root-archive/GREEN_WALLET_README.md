# Green Wallet System - Carbon Credit Management

## Overview

The **Green Wallet System** is a comprehensive carbon credit ledger designed for tracking, issuing, and retiring carbon credits within the GreenLedger platform. It provides complete traceability from project verification through credit issuance to final retirement.

## System Architecture

### Core Components

1. **Models** (`src/main/java/Models/`)
   - `Wallet.java` - Carbon credit wallet entity
   - `CarbonCreditBatch.java` - Batch of credits from verified projects
   - `OperationWallet.java` - Transaction/operation records

2. **Services** (`src/main/java/Services/`)
   - `WalletService.java` - Business logic for all wallet operations

3. **Controllers** (`src/main/java/Controllers/`)
   - `GreenWalletController.java` - UI controller with dialog-based operations

4. **Views** (`src/main/resources/`)
   - `greenwallet.fxml` - Main wallet interface

5. **Database** 
   - `database_schema_green_wallet.sql` - Complete schema with sample data

---

## Database Schema

### Tables

#### 1. **green_wallets**
Stores wallet information for enterprises and banks.

| Column | Type | Description |
|--------|------|-------------|
| id | INT | Primary key |
| wallet_number | VARCHAR(50) | Unique wallet identifier (auto-generated) |
| holder_name | VARCHAR(150) | Name of wallet owner |
| owner_type | ENUM | ENTERPRISE or BANK |
| owner_id | INT | Reference to user/enterprise (nullable) |
| available_credits | DECIMAL(15,2) | Credits available for use |
| retired_credits | DECIMAL(15,2) | Permanently retired credits |
| status | ENUM | ACTIVE, PENDING_REVIEW, INACTIVE |
| registry_id | VARCHAR(100) | External registry ID (optional) |
| is_external | BOOLEAN | Internal vs external wallet flag |
| created_at | TIMESTAMP | Creation timestamp |

#### 2. **carbon_credit_batches**
Tracks batches of credits issued from verified projects.

| Column | Type | Description |
|--------|------|-------------|
| id | INT | Primary key |
| project_id | INT | Reference to carbon_projects |
| wallet_id | INT | Reference to green_wallets |
| total_amount | DECIMAL(15,2) | Total credits in batch |
| remaining_amount | DECIMAL(15,2) | Credits not yet retired |
| status | ENUM | AVAILABLE, PARTIALLY_RETIRED, FULLY_RETIRED |
| issued_at | TIMESTAMP | Issuance timestamp |

#### 3. **wallet_transactions**
Immutable audit trail of all credit movements.

| Column | Type | Description |
|--------|------|-------------|
| id | INT | Primary key |
| wallet_id | INT | Wallet affected |
| batch_id | INT | Batch involved (nullable) |
| type | ENUM | ISSUE, RETIRE, TRANSFER |
| amount | DECIMAL(15,2) | Number of credits |
| reference_note | TEXT | Description/reason |
| created_at | TIMESTAMP | Transaction timestamp |

#### 4. **carbon_projects** (Extended)
Carbon reduction projects that generate credits.

| Column | Type | Description |
|--------|------|-------------|
| id | INT | Primary key |
| enterprise_id | INT | Project owner |
| project_name | VARCHAR(200) | Project name |
| estimated_reduction | DECIMAL(15,2) | Estimated CO₂ reduction |
| verified_reduction | DECIMAL(15,2) | Verified reduction |
| status | ENUM | PENDING, VERIFIED, REJECTED, EXPIRED |
| verification_date | TIMESTAMP | Date of verification |

---

## Credit Flow: From Project to Retirement

### **Step 1: Project Verification**
1. Enterprise submits a carbon reduction project
2. Admin/auditor verifies the project
3. Project status changes from `PENDING` → `VERIFIED`
4. `verified_reduction` field is set (e.g., 4500.00 tCO₂)

### **Step 2: Credit Issuance**
1. User selects a wallet and clicks **"Émettre Crédits"**
2. Provides:
   - Project ID (verified project)
   - Amount (e.g., 2000.00 tCO₂)
   - Reference note
3. System creates:
   - New `carbon_credit_batches` record
   - `wallet_transactions` record (type: ISSUE)
4. Updates wallet: `available_credits += 2000.00`

**Code Flow:**
```java
walletService.issueCredits(walletId, projectId, amount, reference)
  → createCreditBatch()        // Insert into carbon_credit_batches
  → UPDATE green_wallets        // Increment available_credits
  → recordTransaction()         // Log in wallet_transactions
```

### **Step 3: Credit Retirement**
1. User clicks **"Retirer Crédits"**
2. Provides:
   - Amount to retire (e.g., 500.00 tCO₂)
   - Reason (e.g., "Q1 2026 emissions offset")
3. System applies **FIFO** (First In, First Out):
   - Retires oldest available batches first
   - Updates batch `remaining_amount`
   - Changes batch status if fully retired
4. Updates wallet:
   - `available_credits -= 500.00`
   - `retired_credits += 500.00`
5. Records transaction (type: RETIRE)

**Code Flow:**
```java
walletService.retireCredits(walletId, amount, reference)
  → getAvailableBatches()       // Fetch batches with remaining_amount > 0
  → updateBatchRetirement()     // FIFO: retire from oldest batches
  → UPDATE green_wallets        // Transfer credits: available → retired
  → recordTransaction()         // Log retirement
```

### **Step 4: Traceability**
Every credit can be traced:
- **Origin**: Which project generated it (`carbon_projects`)
- **Batch**: When it was issued (`carbon_credit_batches`)
- **Wallet**: Who owns it (`green_wallets`)
- **Retirement**: When/why it was retired (`wallet_transactions`)

---

## Features Implemented

### ✅ **CRUD Operations**

#### Create Wallet
- **Internal Wallet**: Auto-generates `wallet_number` (e.g., `GW-1234567890`)
- **External Wallet**: User provides registry ID → status = `PENDING_REVIEW`

```java
Wallet wallet = new Wallet("EcoTech Industries", "ENTERPRISE", 1);
int walletId = walletService.createWallet(wallet);
```

#### Read Wallet
- Get all wallets: `walletService.getAllWallets()`
- Get by ID: `walletService.getWalletById(id)`
- Get by number: `walletService.getWalletByNumber("GW-1234567890")`

#### Update Wallet
- Modify holder name, status, registry ID
- **Cannot** directly modify credit balances (must use issue/retire operations)

```java
wallet.setStatus("ACTIVE");
walletService.updateWallet(wallet);
```

#### Delete Wallet
- **Soft delete**: Sets status to `INACTIVE`
- **Requirement**: Wallet must have 0 total credits

```java
walletService.deleteWallet(walletId);
```

### ✅ **Credit Operations**

#### Issue Credits
```java
boolean success = walletService.issueCredits(
    walletId,      // Target wallet
    projectId,     // Verified carbon project
    amount,        // tCO₂ (e.g., 1000.00)
    reference      // "Solar Farm Phase 2 verification"
);
```

**Transaction**: Atomic with rollback on failure
1. Creates batch
2. Updates wallet available credits
3. Records transaction

#### Retire Credits
```java
boolean success = walletService.retireCredits(
    walletId,      // Wallet to retire from
    amount,        // tCO₂ (e.g., 500.00)
    reference      // "Offset Q1 2026 Scope 2 emissions"
);
```

**FIFO Retirement**: Credits retired from oldest batches first

### ✅ **Transaction History**
- View all transactions for a wallet
- Filter by type (ISSUE/RETIRE)
- Immutable audit trail

```java
List<OperationWallet> transactions = walletService.getWalletTransactions(walletId);
```

---

## User Interface

### Main Dashboard
- **Wallet Selector**: Dropdown to switch between wallets
- **Wallet Info Card**: Number, holder, type, status
- **Credit Stats**:
  - **Available**: Green-highlighted, ready to use
  - **Retired**: Amber-highlighted, permanently offset
  - **Total**: Cumulative amount
- **Transaction Table**: Complete history with type, amount, date, reference

### Dialogs

#### Create Wallet Dialog
- Holder name
- Owner type (ENTERPRISE/BANK)
- External wallet checkbox
- Registry ID (if external)

#### Issue Credits Dialog
- Project ID (verified projects only)
- Amount (tCO₂)
- Reference note

#### Retire Credits Dialog
- Amount (tCO₂)
- Reason (required for audit)

---

## Business Rules

### 🔒 **Constraints**

1. **Credits can only be issued from VERIFIED projects**
   - Project status must be `VERIFIED`
   - Cannot issue more than `verified_reduction`

2. **Cannot retire more credits than available**
   ```java
   if (wallet.getAvailableCredits().compareTo(retireAmount) < 0) {
       throw new InsufficientCreditsException();
   }
   ```

3. **Transaction history is immutable**
   - No DELETE or UPDATE on `wallet_transactions`
   - Audit trail preserved permanently

4. **Retired credits cannot be reversed**
   - Once retired, credits are permanently removed from circulation
   - Cannot transfer retired → available

5. **External wallets require admin approval**
   - Status: `PENDING_REVIEW` → Admin reviews → `ACTIVE`

6. **Wallet deletion only if zero credits**
   ```java
   if (wallet.getTotalCredits().compareTo(BigDecimal.ZERO) > 0) {
       return false;  // Cannot delete
   }
   ```

---

## Setup Instructions

### 1. **Database Setup**
```bash
# Connect to MariaDB/MySQL
mysql -u root -p

# Create database (if not exists)
CREATE DATABASE greenledger;
USE greenledger;

# Run schema
SOURCE database_schema_green_wallet.sql;
```

### 2. **Verify Database Connection**
Ensure `DataBase/MyConnection.java` is configured:
```java
Connection conn = DriverManager.getConnection(
    "jdbc:mysql://localhost:3306/greenledger",
    "username",
    "password"
);
```

### 3. **Run Application**
```bash
# Navigate to project
cd D:\PiDev\Pi_Dev

# Run with Maven
mvn clean javafx:run
```

### 4. **Test with Sample Data**
The schema includes sample wallets:
- `GW-1000001` - EcoTech Industries (1250.50 available, 749.50 retired)
- `GW-1000002` - Green Finance Bank (5000.00 available, 2000.00 retired)
- `EXT-REGISTRY-789` - External wallet (pending review)

---

## API Reference

### WalletService Methods

| Method | Parameters | Returns | Description |
|--------|-----------|---------|-------------|
| `createWallet()` | Wallet | int | Create new wallet, returns ID |
| `getAllWallets()` | - | List<Wallet> | Get all wallets |
| `getWalletById()` | int id | Wallet | Get wallet by ID |
| `getWalletByNumber()` | String number | Wallet | Get wallet by number |
| `updateWallet()` | Wallet | boolean | Update wallet info |
| `deleteWallet()` | int id | boolean | Soft delete wallet |
| `issueCredits()` | walletId, projectId, amount, ref | boolean | Issue credits from project |
| `retireCredits()` | walletId, amount, ref | boolean | Retire credits |
| `getWalletTransactions()` | int walletId | List<OperationWallet> | Get transaction history |
| `getWalletBatches()` | int walletId | List<CarbonCreditBatch> | Get credit batches |

---

## Example Workflows

### **Workflow 1: Issue Credits**
```
1. User: Select wallet "GW-1000001"
2. User: Click "Émettre Crédits"
3. User: Enter project ID = 1, amount = 500.00, reference = "Q2 solar verification"
4. System: Validates project is VERIFIED
5. System: Creates batch #2 (project=1, wallet=1, total=500, remaining=500)
6. System: Updates wallet: available_credits = 1250.50 + 500 = 1750.50
7. System: Records transaction (ISSUE, 500.00, "Q2 solar verification")
8. UI: Refreshes to show new balance and transaction
```

### **Workflow 2: Retire Credits**
```
1. User: Select wallet "GW-1000001" (available: 1750.50)
2. User: Click "Retirer Crédits"
3. User: Enter amount = 300.00, reason = "Offset March 2026 fleet emissions"
4. System: Validates sufficient credits
5. System: Finds oldest available batch (FIFO)
6. System: Updates batch: remaining_amount -= 300.00
7. System: Updates wallet: available = 1450.50, retired = 1049.50
8. System: Records transaction (RETIRE, 300.00, "Offset March 2026...")
9. UI: Shows updated balances
```

### **Workflow 3: External Wallet Registration**
```
1. User: Click "Nouveau Wallet"
2. User: Enter holder name = "Carbon Registry Co."
3. User: Select type = ENTERPRISE
4. User: Check "Wallet Externe"
5. User: Enter registry ID = "REG-789-EXTERNAL"
6. System: Creates wallet with status = PENDING_REVIEW
7. Admin: Reviews supporting documents
8. Admin: Approve → status = ACTIVE
9. User: Can now issue/retire credits
```

---

## Technical Notes

### **FIFO Credit Retirement**
When retiring credits, the system uses First-In-First-Out:
```java
List<CarbonCreditBatch> batches = getAvailableBatches(walletId);  // ORDER BY issued_at ASC
BigDecimal remainingToRetire = retireAmount;

for (CarbonCreditBatch batch : batches) {
    if (remainingToRetire == 0) break;
    BigDecimal retireFromBatch = min(remainingToRetire, batch.getRemainingAmount());
    updateBatchRetirement(batch.getId(), retireFromBatch);
    remainingToRetire -= retireFromBatch;
}
```

### **Transaction Safety**
All credit operations use database transactions:
```java
try {
    conn.setAutoCommit(false);
    // 1. Create batch
    // 2. Update wallet
    // 3. Record transaction
    conn.commit();
} catch (SQLException ex) {
    conn.rollback();  // Rollback on any failure
}
```

### **Wallet Number Generation**
```java
private String generateWalletNumber() {
    return "GW-" + System.currentTimeMillis();  // e.g., GW-1707846234567
}
```

---

## Future Enhancements

### Planned Features
- [ ] **Credit Transfers**: Transfer credits between wallets
- [ ] **Batch Details View**: View all batches with project traceability
- [ ] **Export to CSV**: Export transaction history
- [ ] **Charts**: Pie chart showing available vs retired credits
- [ ] **External Wallet Document Upload**: File upload for registry credentials
- [ ] **Admin Review Panel**: Approve/reject external wallets
- [ ] **Credit Expiration**: Auto-expire credits after X years
- [ ] **Multi-currency**: Support for different carbon standards (VCS, Gold Standard, etc.)

---

## Troubleshooting

### Issue: "Cannot connect to database"
**Solution**: Check `MyConnection.java` configuration and ensure MySQL is running

### Issue: "Cannot issue credits - project not verified"
**Solution**: Ensure project status = 'VERIFIED' in `carbon_projects` table

### Issue: "Insufficient credits for retirement"
**Solution**: Check `available_credits` in wallet - must be >= retire amount

### Issue: "Foreign key constraint fails"
**Solution**: Ensure referenced project/wallet IDs exist before inserting batches

---

## Credits

Developed for **GreenLedger Carbon Accounting Platform**  
JavaFX 20.0.2 | MariaDB/MySQL | Java 17+

---

**Contact**: For questions or issues, refer to the project documentation or open an issue in the repository.
