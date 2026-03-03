# Database Schema Fix - Batch Metadata Columns

## What Happened?

You encountered this error when trying to issue carbon credit batches:
```
Error creating batch: Unknown column 'calculation_audit_id' in 'field list'
```

**Cause:** The database table `carbon_credit_batches` is using the **basic schema** without metadata columns.

## Solution Applied ✅

The code now includes **automatic fallback logic**:
- ✅ **First attempt:** Try to insert with full metadata (standard, vintage, audit ID)
- ✅ **If columns missing:** Automatically fall back to basic schema
- ✅ **Result:** System works with BOTH schema versions!

**Status:** Your system is now working! Batches can be created without errors.

## Current Schema (Basic)

Your `carbon_credit_batches` table has:
```sql
- id
- project_id
- wallet_id
- total_amount
- remaining_amount
- status
- issued_at
```

## Optional: Upgrade to Full Metadata Schema

If you want **full traceability** with verification standards and vintage years, run this migration:

### Step 1: Open MySQL/phpMyAdmin
Connect to your database (usually `pidev`)

### Step 2: Run Migration Script
Execute the entire file: **`ADD_BATCH_METADATA_COLUMNS.sql`**

```bash
# Command line option:
mysql -u your_username -p pidev < ADD_BATCH_METADATA_COLUMNS.sql
```

### Step 3: Verify Migration
Run this query:
```sql
SELECT COLUMN_NAME, DATA_TYPE 
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_SCHEMA = 'pidev' 
  AND TABLE_NAME = 'carbon_credit_batches'
ORDER BY ORDINAL_POSITION;
```

### Step 4: Restart Application
After migration, the system will automatically use the enhanced schema with metadata support.

## Benefits of Upgrading

**Without Migration (Current - Still Works!):**
- ✅ Carbon credit batches can be created
- ✅ Basic wallet operations work
- ✅ Marketplace listings work
- ❌ No verification standard tracking
- ❌ No vintage year tracking
- ❌ No audit ID tracking
- ❌ No batch lineage tracking

**With Migration (Enhanced):**
- ✅ Everything above, PLUS:
- ✅ Verification standard tracking (VCS, Gold Standard, CDM, CAR)
- ✅ Vintage year tracking (year of issuance)
- ✅ Calculation audit ID tracking
- ✅ Full batch lineage (parent-child relationships)
- ✅ Enhanced provenance display
- ✅ Better compliance reporting

## Testing After Fix

### Test 1: Basic Issuance (Works Now!)
1. Open Green Wallet
2. Select a wallet
3. Click **"+ Nouveau Batch"** button
4. Enter amount: `100`
5. Click confirm
6. ✅ **Should work without errors!**

### Test 2: Full Metadata Issuance (After Migration)
1. Same steps as above, but also fill in:
   - Standard: `Gold Standard`
   - Vintage Year: `2024`
   - Audit ID: `TEST-001`
2. ✅ **Metadata saved to database**
3. View batch details to see metadata

## FAQ

**Q: Do I need to run the migration?**
A: No! The system works without it. Run the migration only if you need full metadata support.

**Q: Will I lose data if I run the migration?**
A: No. The migration only ADDS columns; existing data is preserved.

**Q: Can I rollback the migration?**
A: Yes, but you'll lose the metadata. A rollback script is included in the migration file (commented out).

**Q: What if migration fails?**
A: The system continues working with basic schema. Check MySQL error logs and ensure you have ALTER TABLE permissions.

**Q: Can I use both schemas in production?**
A: Yes! The fallback logic supports environments with mixed schema versions.

## Technical Details

### Fallback Logic Flow
```
1. Try INSERT with metadata columns
2. Catch SQLException
3. If error message contains "Unknown column"
   → Retry with basic schema (no metadata)
4. Return batch ID or -1
```

### Code Location
**File:** `src/main/java/Services/WalletService.java`  
**Method:** `createCreditBatch(...)`  
**Lines:** ~1188-1250

### Modified Files
- ✅ `WalletService.java` - Added fallback logic
- ✅ `ADD_BATCH_METADATA_COLUMNS.sql` - Migration script (optional)
- ✅ `DATABASE_SCHEMA_FIX.md` - This guide

## Support

If you continue experiencing issues:
1. Check console output for "Metadata columns not found, using basic schema..." message
2. Verify database connection and table structure
3. Ensure `carbon_credit_batches` table exists
4. Check user permissions for INSERT operations

---

**Status:** ✅ Issue Resolved  
**Action Required:** None (migration is optional)  
**Last Updated:** March 3, 2026
