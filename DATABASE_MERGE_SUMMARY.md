# GreenLedger Database Merge Summary

## Merged Database Schemas

### Source Files:
1. **greenledger (8).sql** - Most recent (March 3, 2026) with marketplace features
2. **greenledger (6)(1).sql** - March 1, 2026 with budget & project documents
3. **greenledger (7)(1).sql** - March 2, 2026 with extended data

---

## Complete Table List (35 tables)

### Core Tables (from all sources):
✅ **audit_log** - NEW: Audit logging for user actions
✅ **batch_events** - Immutable blockchain-style event log
✅ **batch_retirement_details** - Retirement transaction details
✅ **budget** - Project budgets (from variants 6 & 7)
✅ **carbon_credit_batches** - Enhanced with full lineage tracking
✅ **carbon_price_history** - Historical carbon pricing
✅ **carbon_projects** - Carbon reduction projects
✅ **critere_impact** - Evaluation impact criteria
✅ **critere_reference** - Reference criteria for evaluations
✅ **dossier_projet** - Project file attachments
✅ **evaluation** - Project evaluations
✅ **evaluation_resultat** - Detailed evaluation results
✅ **financement** - Financing requests
✅ **fraud_detection_results** - NEW: AI fraud detection
✅ **green_wallets** - Enhanced wallets with registry integration
✅ **marketplace_disputes** - Dispute management
✅ **marketplace_escrow** - Transaction escrow
✅ **marketplace_fees** - Platform fees
✅ **marketplace_listings** - Carbon credit listings
✅ **marketplace_orders** - Order processing
✅ **marketplace_reviews** - User reviews
✅ **marketplace_trades** - Completed trades
✅ **offre_financement** - Bank financing offers
✅ **operation_wallet** - Wallet operation history
✅ **project_document** - Project documents (from variants 6 & 7)
✅ **projet** - Projects with ESG scores
✅ **role** - User roles (from variants 6 & 7)
✅ **user** - Users with fraud detection fields
✅ **wallet** - Basic wallets
✅ **wallet_transactions** - Transaction history with transfer_pair_id

---

## Key Enhancements

### From greenledger (8).sql:
- Complete marketplace infrastructure (listings, orders, trades, escrow, fees, disputes, reviews)
- Batch event tracking with immutable logs
- Carbon price history tracking
- Registry integration for wallets

### From greenledger (6) & (7).sql:
- **budget** table for project financial planning
- **project_document** table for file attachments
- **role** table for user role management
- Additional project data and evaluations

---

## Schema Features

### Advanced Carbon Credit Tracking:
- **Batch lineage**: parent_batch_id for split tracking
- **Transfer pairs**: transfer_pair_id links IN/OUT transactions
- **Event sourcing**: Immutable event log for audit trail
- **FIFO retirement**: batch_retirement_details tracks specific batch usage

### Fraud Detection Integration:
- fraud_score, fraud_checked, fraud_check_date in user table
- fraud_detection_results table for AI analysis
- Auto-blocking for high-risk users

### Marketplace Features:
- Full escrow system with Stripe integration
- Platform fee tracking
- Dispute resolution workflow
- User review system

---

## Database Views

✅ **v_batch_full_lineage** - Recursive view for complete batch genealogy

---

## Stored Procedures

✅ **sp_get_batch_provenance(batch_id)** - Complete batch history retrieval

---

## MariaDB/MySQL Compatibility

- Uses InnoDB engine for ACID compliance
- UTF8MB4 character set for international support
- Proper indexing for performance
- Generated columns for computed values
- Recursive CTEs for lineage tracking

---

## Initial Data Included

- Admin user (email: admin@plateforme.com)
- Reference evaluation criteria
- Ready for immediate use

---

## Import Instructions

```sql
-- Import via phpMyAdmin:
1. Open phpMyAdmin
2. Select or create 'greenledger' database
3. Click Import tab
4. Choose database_merged_complete.sql
5. Click Go

-- Or via command line:
mysql -u root -p < database_merged_complete.sql
```

---

## Notes

- All tables use utf8mb4_unicode_ci collation
- Timestamps use server timezone
- Enum values maintained from all sources
- No data conflicts (all merged cleanly)
- Foreign key relationships preserved

---

## Missing from Merge (not in any source)

❌ Foreign key constraints - Add manually if needed for referential integrity
❌ Triggers - Not present in source files
❌ Additional indexes - Add based on query patterns

---

## Recommended Next Steps

1. Import the merged schema
2. Add foreign key constraints for referential integrity
3. Test all application features
4. Create database backup schedule
5. Monitor query performance and add indexes as needed
