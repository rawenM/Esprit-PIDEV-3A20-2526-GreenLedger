# 🔧 CRITICAL DATABASE FIX - READ THIS FIRST!

## ⚠️ IMPORTANT: Your System Won't Work Without This Fix!

The database schema is missing critical updates. This is causing:
- ❌ **Retiring credits to fail** (batch_id NOT NULL error)
- ❌ **Transfer operations to fail** (TRANSFER types not in enum)
- ❌ **Name field missing** (wallet names can't be saved)

---

## 🚀 Quick Fix (Copy & Paste These Commands)

### Step 1: Open MySQL Command Line or phpMyAdmin

**Option A - MySQL Command Line:**
```bash
mysql -u root -p
```
Enter your password when prompted.

**Option B - phpMyAdmin:**
- Open phpMyAdmin in browser (http://localhost/phpmyadmin)
- Click "greenledger" database
- Go to "SQL" tab

---

### Step 2: Run These SQL Commands

Copy and paste **ALL** of these commands:

```sql
USE greenledger;

-- Fix 1: Add name column to wallet table
ALTER TABLE wallet 
ADD COLUMN name VARCHAR(255) AFTER wallet_number;

-- Fix 2: Make batch_id NULLABLE (required for TRANSFER operations)
ALTER TABLE wallet_transactions 
MODIFY COLUMN batch_id BIGINT NULL;

-- Fix 3: Add TRANSFER types to enum
ALTER TABLE wallet_transactions 
MODIFY COLUMN type ENUM('ISSUE', 'RETIRE', 'TRANSFER_IN', 'TRANSFER_OUT') NOT NULL;

-- Verify the changes
DESCRIBE wallet;
DESCRIBE wallet_transactions;
```

---

### Step 3: Verify Success

After running the commands, you should see:

**For `wallet` table:**
```
+-------------------+--------------+------+-----+-------------------+
| Field             | Type         | Null | Key | Default           |
+-------------------+--------------+------+-----+-------------------+
| id                | int          | NO   | PRI | NULL              |
| wallet_number     | int          | YES  | UNI | NULL              |
| name              | varchar(255) | YES  |     | NULL              | ✅ NEW!
| owner_type        | varchar(10)  | NO   |     | NULL              |
| owner_id          | int          | NO   |     | NULL              |
| available_credits | double       | NO   |     | NULL              |
| retired_credits   | double       | NO   |     | NULL              |
| created_at        | timestamp    | YES  |     | CURRENT_TIMESTAMP |
+-------------------+--------------+------+-----+-------------------+
```

**For `wallet_transactions` table:**
```
+----------------+--------------------------------------------------------+------+-----+-------------------+
| Field          | Type                                                   | Null | Key | Default           |
+----------------+--------------------------------------------------------+------+-----+-------------------+
| id             | bigint                                                 | NO   | PRI | NULL              |
| wallet_id      | bigint                                                 | NO   | MUL | NULL              |
| batch_id       | bigint                                                 | YES  | MUL | NULL              | ✅ NOW NULLABLE!
| type           | enum('ISSUE','RETIRE','TRANSFER_IN','TRANSFER_OUT')   | NO   |     | NULL              | ✅ HAS TRANSFER!
| amount         | decimal(15,2)                                          | NO   |     | NULL              |
| reference_note | text                                                   | YES  |     | NULL              |
| created_at     | timestamp                                              | YES  |     | CURRENT_TIMESTAMP |
+----------------+--------------------------------------------------------+------+-----+-------------------+
```

---

## ✅ After Running the Fix, You Can:

✨ **Create wallets** with names
✨ **Issue credits** (quick or with projects)
✨ **Retire credits** (will now work properly!)
✨ **Transfer credits** between wallets
✨ **Edit wallet** name and type
✨ **Delete wallets** (only if balance is 0)

---

## 🐛 Troubleshooting

### Error: "Column 'name' already exists"
**Solution:** The name column was already added. Skip Fix 1, continue with Fix 2 and 3.

### Error: "Access denied"
**Solution:** You need administrator/root access to modify tables. Contact your database administrator.

### Error: "Unknown database 'greenledger'"
**Solution:** The database doesn't exist. Check your database name in MyConnection.java.

### Operations still failing?
**Solution:** 
1. Verify all 3 fixes were applied: `DESCRIBE wallet;` and `DESCRIBE wallet_transactions;`
2. Restart your Java application
3. Check console for error messages
4. Verify MyConnection.java has correct database credentials

---

## 📊 What Each Fix Does

### Fix 1: Add name column
**Why:** Allows wallets to have descriptive names instead of just numbers
**Example:** "Projet Solaire 2026" instead of just "Wallet #123456"

### Fix 2: Make batch_id NULLABLE
**Why:** Transfer operations don't have batches, but old schema required batch_id
**Impact:** RETIRE and TRANSFER operations can now record transactions properly

### Fix 3: Add TRANSFER types
**Why:** Old schema only had ISSUE and RETIRE, missing TRANSFER_IN and TRANSFER_OUT
**Impact:** Transfer functionality can now work and be tracked

---

## 🔄 Alternative: Run the SQL File

If you prefer, you can run the provided SQL file:

```bash
mysql -u root -p greenledger < update_wallet_schema.sql
```

---

## 📝 Test After Fix

1. **Create a wallet:**
   - Open application → Click "Créer Wallet"
   - Enter name "Test Wallet"
   - Leave number blank (auto-generate)
   - Click Create

2. **Issue some credits:**
   - Select the wallet → Click "Émettre Crédits"
   - Enter amount: 1000
   - Choose source preset
   - Click Émettre

3. **Retire credits (THIS SHOULD NOW WORK!):**
   - Click "Retirer Crédits"
   - Enter amount: 100
   - Choose reason
   - Click Retirer
   - ✅ Should succeed without errors!

4. **Create a second wallet and transfer:**
   - Create another wallet
   - Select first wallet → Click "Transférer"
   - Select second wallet as destination
   - Enter amount: 200
   - Click Transférer
   - ✅ Should succeed!

---

## 🎯 Success Checklist

After running the fixes, verify these work:

- [ ] Create wallet with custom name
- [ ] Create wallet with auto-generated number
- [ ] Issue credits (quick mode)
- [ ] **Retire credits** (MAIN FIX - should work now!)
- [ ] Transfer credits between wallets
- [ ] Edit wallet name
- [ ] Delete empty wallet
- [ ] View transaction history (shows ISSUE, RETIRE, TRANSFER_IN, TRANSFER_OUT)
- [ ] Transaction colors work (green, amber, blue)

---

## 🆘 Still Having Issues?

Check the console output when you try to retire credits. If you see:

```
Error retiring credits: Column 'batch_id' cannot be null
```
→ Fix 2 wasn't applied. Run it again.

```
Error recording transaction: Data truncated for column 'type'
```
→ Fix 3 wasn't applied. Run it again.

```
Unknown column 'name' in 'field list'
```
→ Fix 1 wasn't applied. Run it again.

---

**🔥 CRITICAL: Run these fixes BEFORE using the application!**

All features have been implemented in the code, but they won't work without the database schema updates!
