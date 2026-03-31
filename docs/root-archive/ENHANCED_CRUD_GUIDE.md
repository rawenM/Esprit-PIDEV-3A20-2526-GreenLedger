# 🌱 Green Wallet - Enhanced CRUD System

## Overview
A comprehensive carbon credit wallet management system with creative, realistic features for testing and demonstration purposes.

## ✨ New Features Implemented

### 1. **Create Wallet Dialog** 🎨
Located in: `GreenWalletController.showCreateWalletDialog()`

**Features:**
- 📝 **Wallet Name Field**: Give your wallet a memorable name
  - Example prompts: "Projet Solaire 2026", "Reforestation Amazonie"
- 🔢 **Smart Wallet Number**:
  - Leave blank → Auto-generates unique 6-digit number (100000-999999)
  - Or enter your own custom number
  - Automatic uniqueness validation
- 🏢 **Owner Type Selection**:
  - ENTERPRISE
  - BANK
  - NGO
  - GOVERNMENT
- 💰 **Initial Credits**: Set starting balance

**UI Highlights:**
- Emoji icons for visual appeal
- Creative prompts and examples
- Clean, professional GridPane layout

---

### 2. **Edit Wallet Dialog** ✏️
Located in: `GreenWalletController.showEditWalletDialog()`

**Features:**
- Modify wallet name
- Change owner type
- Wallet number displayed (read-only)
- Current balance shown
- Updates preserved in database

**Button:** `btnEditWallet` in main interface

---

### 3. **Quick Issue Credits Dialog** 🚀
Located in: `GreenWalletController.showQuickIssueDialog()`

**Features:**
- 📊 **Quick Amount Presets**:
  - 100.00 tCO₂
  - 500.00 tCO₂
  - 1000.00 tCO₂
  - 5000.00 tCO₂
  - Custom amount
- 🏭 **Source Presets**:
  - 🌞 Installation Solaire - Phase 1
  - 🌲 Reforestation Amazonie
  - 💨 Capture CO₂ Industrielle
  - ⚡ Parc Éolien Offshore
  - 🛰️ Vérification Projet Tiers
- Auto-fills description based on source selection
- Simplified issuance for testing (no project validation required)

**Backend:** `WalletService.quickIssueCredits()`

---

### 4. **Transfer Credits Dialog** 🔄
Located in: `GreenWalletController.showTransferDialog()`

**Features:**
- 🎯 **Destination Selector**: Dropdown of all wallets (excluding source)
- 📊 **Quick Amount Buttons**:
  - 25%, 50%, 75%, 100% of available credits
  - Custom amount option
- 📝 **Transfer Reason**: Mandatory description field
- Real-time balance display
- Dual transaction logging (TRANSFER_OUT + TRANSFER_IN)

**Backend:** `WalletService.transferCredits()`

**Transaction Colors:**
- 🟢 ISSUE = Green
- 🟠 RETIRE = Amber
- 🔵 TRANSFER = Blue

---

### 5. **Enhanced Retire Credits Dialog** ♻️
Located in: `GreenWalletController.showRetireCreditsDialog()`

**Features:**
- 📊 **Quick Amount Presets**:
  - 25%, 50%, 75%, 100% of available credits
  - Custom amount
- 🏷️ **Reason Presets**:
  - 🌍 Compensation empreinte carbone entreprise
  - ✈️ Neutralité carbone - Voyage aérien
  - 🏭 Compensation production industrielle
  - 🚗 Neutralisation émissions transport
  - 🏢 Bilan carbone annuel - Neutralité
  - 🎯 Objectif Net-Zero atteint
- Creative prompts for detailed reasoning
- Confirmation message emphasizing permanent retirement

---

## 🗄️ Database Schema Update

**IMPORTANT:** Run this SQL before using the system:

```sql
USE greenledger;

ALTER TABLE wallet 
ADD COLUMN IF NOT EXISTS name VARCHAR(255) AFTER wallet_number;
```

Or execute the provided file:
```bash
mysql -u [username] -p greenledger < update_wallet_schema.sql
```

**Updated Wallet Table Structure:**
```sql
CREATE TABLE wallet (
  id INT AUTO_INCREMENT PRIMARY KEY,
  wallet_number INT DEFAULT NULL,
  name VARCHAR(255),                    -- NEW FIELD
  owner_type VARCHAR(10) NOT NULL,
  owner_id INT NOT NULL,
  available_credits DOUBLE NOT NULL,
  retired_credits DOUBLE NOT NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY (wallet_number)
);
```

---

## 🔧 Backend Service Layer

### WalletService.java

**New/Enhanced Methods:**

1. **`createWallet(Wallet wallet)`**
   - Generates unique 6-digit wallet number if not provided
   - Inserts wallet with name field
   - Returns created wallet with ID

2. **`updateWallet(Wallet wallet)`**
   - Updates name and owner_type
   - Preserves wallet number (immutable)

3. **`quickIssueCredits(int walletId, double amount, String description)`**
   - Simple credit addition for testing
   - No project/batch validation required
   - Records transaction with ISSUE type

4. **`transferCredits(int fromWalletId, int toWalletId, double amount, String referenceNote)`**
   - Validates source has sufficient credits
   - Atomic transaction with rollback protection
   - Records dual transactions:
     * TRANSFER_OUT from source (with destination wallet number in note)
     * TRANSFER_IN to destination (with source wallet number in note)

5. **`generateUniqueWalletNumber()`**
   - Generates random number between 100000-999999
   - Checks database for uniqueness
   - Max 50 attempts to avoid collisions
   - Fallback to timestamp-based number

6. **`walletNumberExists(int walletNumber)`**
   - Database query to check uniqueness
   - Used by generation algorithm

---

## 📋 Model Updates

### Wallet.java

**New Field:**
```java
private String name;
```

**Enhanced `toString()`:**
```java
"Wallet[%d - %s - %s: %.2f credits available]"
// Shows: ID - Name (or "Unnamed") - Type - Available Credits
```

---

## 🎨 UI Enhancements

### Transaction Table
Color-coded transaction types:
- **ISSUE** → 🟢 Green (`#2B6A4A`)
- **RETIRE** → 🟠 Amber (`#D97706`)
- **TRANSFER_IN/OUT** → 🔵 Blue (`#3B82F6`)

### Wallet Selector
Enhanced display format:
```
#123456 - Projet Solaire 2026 (ENTERPRISE)
```

### Dialog Design Philosophy
- **Emoji Icons**: Visual distinction and modern appeal
- **Preset Options**: Speed up common operations
- **Realistic Prompts**: Guide users with examples
- **Validation**: Prevent invalid inputs
- **Feedback**: Clear success/error messages

---

## 🚀 Usage Guide

### Creating a Wallet

1. Click **"Créer Wallet"** button
2. Enter wallet name (or leave for "Unnamed Wallet")
3. Optionally enter wallet number (or leave blank for auto-generation)
4. Select owner type (ENTERPRISE/BANK/NGO/GOVERNMENT)
5. Enter owner ID
6. Set initial credits (optional, defaults to 0)
7. Click **"✓ Créer"**

### Issuing Credits (Quick Mode)

1. Select a wallet from dropdown
2. Click **"Émettre Credits"** (sidebar or main)
3. Select preset amount or enter custom
4. Choose source project or enter custom
5. Enter/review description
6. Click **"✓ Émettre"**

### Transferring Credits

1. Select source wallet
2. Click **"Transférer"** button
3. Select destination wallet from dropdown
4. Choose percentage (25/50/75/100%) or enter custom amount
5. Enter transfer reason (mandatory)
6. Click **"➡️ Transférer"**

### Retiring Credits

1. Select wallet
2. Click **"Retirer Credits"**
3. Select percentage or custom amount
4. Choose reason preset or enter custom
5. Enter detailed reason
6. Click **"🔒 Retirer"**

### Editing Wallet

1. Select wallet
2. Click **"Modifier"** button (if available)
3. Update name and/or owner type
4. Click **"💾 Sauvegarder"**

---

## ⚠️ Important Notes

### Wallet Number Generation
- **Auto-generated range**: 100000 to 999999 (6 digits)
- **Uniqueness**: Validated against database
- **Manual entry**: Allowed, must be unique
- **Collision handling**: Max 50 generation attempts, fallback to timestamp

### Credit Operations
- **Data type**: All credits stored as `double`
- **Precision**: Displayed with 2 decimal places (tCO₂)
- **Validation**: Negative amounts prevented
- **Transaction safety**: All operations use database transactions with rollback

### Transfer Transactions
- Creates TWO transaction records:
  1. **TRANSFER_OUT** (source wallet, negative amount)
  2. **TRANSFER_IN** (destination wallet, positive amount)
- Reference notes include opposite wallet number for traceability

### Retirement
- **Permanent**: Retired credits cannot be un-retired
- **FIFO**: Uses First-In-First-Out batch retirement (batches created first are retired first)
- **Tracking**: Moves from `available_credits` to `retired_credits`

---

## 🧪 Testing Scenarios

### Scenario 1: Complete Workflow
```
1. Create "Projet Solaire 2026" wallet (ENTERPRISE)
2. Quick issue 1000 tCO₂ from "Installation Solaire - Phase 1"
3. Create "Banque Verte" wallet (BANK)
4. Transfer 400 tCO₂ to "Banque Verte"
5. Retire 200 tCO₂ for "Neutralité carbone - Voyage aérien"
6. Verify transaction history shows all operations
7. Verify balances: Solaire=400, Banque=400, Retired=200
```

### Scenario 2: Multiple Wallets
```
1. Create 5 wallets with different owner types
2. Issue credits to each (varying amounts)
3. Create transfer chain (A→B→C→D→E)
4. Retire from various wallets
5. Export transaction history (when implemented)
```

### Scenario 3: Stress Testing
```
1. Create wallet with auto-generated number
2. Quick issue 5000 tCO₂
3. Transfer 25% to 4 different wallets
4. Retire 50% from each recipient
5. Verify total system credits = initial - total retired
```

---

## 📊 Data Visualization

### Transaction Types Distribution
Monitor the balance of operations:
- How many issues vs retirements?
- Transfer volume between wallet types
- Most active wallets

### Credit Flow Analysis
- Track credit journey from issuance to retirement
- Identify intermediary wallets (high transfer activity)
- Measure retirement rate

---

## 🔮 Future Enhancements

### Planned Features
- **Delete Wallet**: Only if balance = 0
- **Batch Details View**: See all batches with FIFO queue
- **Export Functionality**: CSV/PDF reports
- **Advanced Filtering**: Filter transactions by type, date range
- **Dashboard Analytics**: Charts and graphs
- **Multi-wallet Operations**: Bulk transfers, retirements
- **Audit Trail**: Complete history with timestamps
- **Project Integration**: Link to actual verified carbon projects

---

## 🐛 Troubleshooting

### "Name column doesn't exist"
**Solution**: Run `update_wallet_schema.sql`

### "Wallet number already exists"
**Solution**: Leave number field blank for auto-generation, or choose different number

### "Insufficient credits for transfer"
**Check**: Verify source wallet has enough available credits

### Dialog doesn't appear
**Check**: Ensure a wallet is selected in dropdown

### Transaction not showing
**Solution**: Click refresh button or reselect wallet

---

## 📁 File Structure

```
src/main/java/
├── Models/
│   ├── Wallet.java                    [Enhanced with name field]
│   ├── CarbonCreditBatch.java
│   └── OperationWallet.java
├── Services/
│   └── WalletService.java             [All CRUD + transfer operations]
├── Controllers/
│   └── GreenWalletController.java     [5 creative dialogs]
└── Utils/
    └── NavigationContext.java
    
src/main/resources/
└── greenwallet.fxml                   [UI layout]

Root/
├── update_wallet_schema.sql           [Database migration]
└── ENHANCED_CRUD_GUIDE.md            [This file]
```

---

## 🎯 Key Achievements

✅ Name field support with persistence
✅ Smart wallet number generation (unique 6-digit)
✅ Creative dialog UIs with emoji icons
✅ Quick amount presets (percentages + fixed amounts)
✅ Realistic source/reason presets
✅ Full transfer functionality with dual transactions
✅ Enhanced retire dialog with creative prompts
✅ Color-coded transaction types
✅ Comprehensive validation and error handling
✅ Zero compilation errors
✅ Professional user experience

---

## 💡 Design Philosophy

**Testing-Friendly:**
- Quick presets for rapid data entry
- Realistic scenarios built-in
- Easy to create diverse test cases

**User-Centric:**
- Visual feedback (emojis, colors)
- Helpful prompts and examples
- Clear error messages
- Confirmation dialogs for important actions

**Developer-Friendly:**
- Clean separation of concerns (MVC)
- Service layer abstraction
- Transaction safety with rollback
- Comprehensive error handling

**Production-Ready Foundation:**
- Database constraints (UNIQUE wallet numbers)
- Atomic transactions
- Audit trail (all operations logged)
- Scalable architecture

---

## 📞 Support

For issues or questions:
1. Check database schema is updated
2. Verify all files compiled without errors
3. Check console for error messages
4. Review transaction history for operation results

---

**Version:** 1.0  
**Last Updated:** 2024  
**Status:** ✅ Fully Functional - Ready for Testing
