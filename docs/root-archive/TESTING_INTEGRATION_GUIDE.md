# Testing & Integration Guide

## Overview
This guide explains how to test and integrate the newly implemented GreenWallet and Marketplace features.

---

## PART 1: RUN THE TEST SUITE

### Step 1: Compile Test Suite
```bash
javac -cp "src/main/java:lib/*" src/main/java/Services/MarketplaceServiceTests.java
```

### Step 2: Run Tests
```bash
java -cp "src/main/java:lib/*" Services.MarketplaceServiceTests
```

### Expected Output
```
=== Marketplace Service Test Suite ===

--- Testing MarketplaceListingService ---
  ✓ PASS: getActiveListings returns non-null list
  ✓ PASS: getListingsBySeller returns non-null list
  ✓ PASS: searchListings returns non-null list

--- Testing MarketplaceOrderService ---
  ✓ PASS: getOrderById returns valid order
  ✓ PASS: getBuyerOrders returns non-null list
  ✓ PASS: getSellerOrders returns non-null list

[... more tests ...]

=== Test Summary ===
Total Tests: 23
Passed: 23
Failed: 0
Pass Rate: 100.0%

✓ All tests passed!
```

---

## PART 2: DATA CONSISTENCY CHECK

### Step 1: Run Consistency Validation
```java
// In your main application startup code:
MarketplaceDataConsistencyService service = MarketplaceDataConsistencyService.getInstance();
MarketplaceDataConsistencyService.ConsistencyReport report = service.runFullCheck();
report.printReport();
```

### Step 2: Review Report Output
```
=== Marketplace Data Consistency Report ===
✓ PASS: Orders without listings (0 issues)
✓ PASS: Listings without sellers (0 issues)
✓ PASS: Orders without escrow (0 issues)
✓ PASS: Escrow without orders (0 issues)
✓ PASS: Fees without orders (0 issues)
✓ PASS: Disputes without escrow (0 issues)
✓ PASS: Unmatched transactions (0 issues)
✓ PASS: Duplicate orders (0 issues)
✓ PASS: Balance inconsistencies (0 issues)
Total issues: 0
==========================================
```

### Step 3: Fix Any Issues Found
If issues are found, the error messages in logs will indicate:
- Specific record IDs with problems
- Type of inconsistency found
- Recommended resolution

---

## PART 3: TEST GREENWALLET FEATURES

### Feature 1: Issue Credits (OperationPanelController)
**Test Procedure:**
1. Open GreenWallet application
2. Navigate to "Operations" panel
3. Click "Issue Credits" button
4. Select verification standard from dropdown
5. Enter quantity (between 1-1000)
6. Click "Execute"
7. Confirm in dialog

**Expected Result:**
- Form validation prevents invalid quantities
- BatchIssuedEvent posted to EventBus
- WalletService.quickIssueCredits() called
- User sees success confirmation with batch ID
- Sidebar credits updated
- Log entry: `BATCH_ISSUED: BatchID=X, Quantity=Y.YY`

### Feature 2: Retire Credits
**Test Procedure:**
1. Click "Retire Credits" button
2. View available balance (auto-populated)
3. Enter quantity to retire
4. Click "Execute"
5. Confirm in subsequent dialog

**Expected Result:**
- Available balance shown correctly
- Confirmation dialog prevents accidental deletion
- CreditsRetiredEvent posted
- WalletService.retireCredits() called
- Sidebar updated immediately
- Log entry: `CREDITS_RETIRED: Quantity=Y.YY, WalletID=X`

### Feature 3: Transfer Credits
**Test Procedure:**
1. Click "Transfer Credits" button
2. Select recipient wallet from dropdown
3. Enter transfer quantity
4. Click "Execute"

**Expected Result:**
- Recipient list populated from walletService.getWalletsByOwner()
- Transfer amount validated
- TransferCompletedEvent posted
- Both wallets updated
- Log entry: `TRANSFER_COMPLETED: From=X, To=Y, Quantity=Z.ZZ`

### Feature 4: Navigation & Dashboard
**Test Procedure:**
1. Open GreenWallet dashboard
2. Verify sidebar shows:
   - Available Credits: [number]
   - Retired Credits: [number]
   - Active Listings: [count]
3. Click "Marketplace" button
4. Verify navigation works
5. Click "Export Data" button
6. Save CSV file
7. Verify CSV contains wallet data

**Expected Result:**
- Sidebar stats calculated correctly from wallet objects
- Navigation switches to marketplace.fxml
- CSV export creates file with headers: WalletNumber, Name, Owner, AvailableCredits, RetiredCredits
- Back button returns to previous view

### Feature 5: Scope Analysis Visualization
**Test Procedure:**
1. Open "Scope Analysis" tab
2. View waterfall chart showing:
   - Scope 1 emissions (red bar)
   - Scope 2 emissions (orange bar)
   - Scope 3 emissions (yellow bar)
3. Click on each bar to drill down
4. View data quality badge (Tier indicator)

**Expected Result:**
- Chart renders with correct proportions
- Bar colors match scope categories
- Click triggers drill-down view
- NotificationEvent posted on click
- Data quality badge shows tier with emoji
- No JavaScript errors in console

### Feature 6: Map Integration
**Test Procedure:**
1. Open "Map" tab
2. Verify Leaflet map loads with:
   - Project markers (green pins)
   - Pollution points (color-coded)
3. Click on markers
4. View air quality data overlay
5. Zoom and pan map

**Expected Result:**
- Map initializes with Leaflet 
- JavaScript bridge establishes successfully
- Pollution points load from AirQualityService
- Markers show proper positions
- Click handlers fire MapProjectSelectedEvent
- No console JavaScript errors
- Fallback HTML works if pollution_map.html not found

---

## PART 4: TEST MARKETPLACE FEATURES

### Feature 1: Browse & Purchase
**Test Procedure:**
1. Open Marketplace → "Browse Listings" tab
2. View listings in table
3. Select a listing
4. Click "Buy Now" button
5. Enter quantity (e.g., 10)
6. Click OK
7. Review purchase confirmation: $XX.XX
8. Click OK to proceed
9. Enter test card: 4242 4242 4242 4242, 12/25, 123 CVC
10. Payment processed message appears

**Expected Result:**
- Listings load from MarketplaceListingService
- Input validation: quantity must be positive and ≤ available
- KYC check: user within transaction limits
- Order created with PENDING status
- Payment intent created with Stripe
- Card details dialog appears for payment
- Payment succeeds (test card)
- Order marked COMPLETED
- Escrow holds funds
- Order added to history

**Logs Generated:**
```
ORDER_PLACED: ID=123, Buyer=456, Seller=789, Listing=100, Amount=$250.00
PAYMENT_PROCESSED: OrderID=123, PaymentID=pi_123abc, Amount=$250.00, Status=succeeded
ESCROW_HELD: ID=1, OrderID=123, Amount=$250.00
```

### Feature 2: Make Offer & Negotiate
**Test Procedure:**
1. Open "Browse Listings" tab
2. Select a listing
3. Click "Make Offer" button
4. Enter quantity (e.g., 5)
5. Enter offer price (e.g., $30/unit)
6. Click OK

**Test Seller Side:**
1. Open "My Listings" tab
2. Click "Offers Received" tab
3. Select the pending offer
4. Option A: Click "Accept Offer" → confirm
5. Option B: Click "Counter Offer" → enter new price → send
6. Option C: Click "Reject Offer" → confirm

**Expected Result (Buy Side):**
- Quantity dialog validates: 0 < qty ≤ available
- Price dialog shows listing minimum price (if set)
- Auto-accept triggers if within auto_accept_price_usd threshold
- Successful offer shows: Offer ID, expiration time
- Offer appears in "Offers Sent" tab

**Expected Result (Sell Side):**
- Offer appears in "Offers Received" tab
- Counter-offer updates price, offer sent back to buyer
- Accept triggers payment prompt from buyer
- Reject removes offer

**Logs Generated:**
```
OFFER_CREATED: OfferID=555, ListingID=100, Buyer=456, Qty=5.00, Price=$30.00
OFFER_ACCEPTED: OfferID=555, SellerID=789
```

### Feature 3: Create & Edit Listings
**Test Procedure:**
1. Open "My Listings" tab
2. Click "Create Listing" button
3. Create Listing Form appears:
   - Select asset type (CARBON_CREDITS, etc.)
   - Select wallet to sell from
   - Enter quantity to list
   - Enter asking price
   - Optional: Min price, Auto-accept price
   - Optional: Description
4. Click "Create"
5. Listing appears in "My Listings" tab
6. Select the listing
7. Click "Edit Listing" button
8. Edit Dialog appears:
   - Change price to $50/unit
   - Add description
   - Click "Save"

**Expected Result:**
- Listing created with ACTIVE status
- Listing visible in marketplace browse
- Edit dialog loads current values
- Price validation: must be > 0
- Min price validation: must be ≤ asking price
- Auto-accept price validation: must be ≥ min price
- Listing updated immediately
- Delete button deactivates listing

**Logs Generated:**
```
LISTING_CREATED: ID=100, Seller=789, Asset=CARBON_CREDITS, Qty=100.00, Price=$45.00
```

### Feature 4: Payment & Escrow
**Test Procedure:**
1. Buyer completes purchase → payment succeeds
2. Funds held in escrow for 24 hours
3. Seller ships/transfers credits
4. Buyer confirms receipt
5. Escrow releases to seller

**Test Admin Functions:**
1. Access escrow management (if available)
2. View held funds by order
3. Manually release/refund if needed

**Expected Result:**
- Escrow created with status HELD
- Auto-release trigger set for 24 hours
- Early manual release available if buyer confirms
- MarketplaceEscrowService creates records
- If payment fails: auto-refund to buyer
- If dispute filed: status → DISPUTED, no auto-release

**Logs Generated:**
```
ESCROW_HELD: ID=1, OrderID=123, Amount=$250.00
ESCROW_RELEASED: ID=1, ReleasedTo=seller
```

### Feature 5: Disputes & Refunds
**Test Procedure:**
1. Place order → pay → receive defective item
2. Open order in history
3. Click "File Dispute" button
4. Select reason: "Item not as described" or "Non-delivery"
5. Add details message
6. Submit dispute

**Test Admin Resolution:**
1. Access Disputes admin panel
2. View pending disputes in queue
3. Review dispute details
4. Select resolution: "REFUND_TO_BUYER", "RELEASE_TO_SELLER", "SPLIT_FUNDS"
5. Add admin notes
6. Click "Resolve"

**Expected Result:**
- Dispute filed with PENDING status
- Escrow marked as DISPUTED
- Admin notified
- Admin reviews and selects resolution
- If REFUND_TO_BUYER: escrow releases to buyer
- If RELEASE_TO_SELLER: escrow releases to seller
- If SPLIT_FUNDS: manual processing required
- Both parties notified of resolution

**Logs Generated:**
```
DISPUTE_CREATED: ID=20, OrderID=123, Reporter=456, Reason="Item not as described"
DISPUTE_RESOLVED: ID=20, Resolution=REFUND_TO_BUYER, ResolvedBy=admin_id
```

### Feature 6: Fee Tracking & Reporting
**Test Procedure:**
1. Complete multiple purchases
2. Access Financial Reports → Marketplace Fees
3. View total fees collected
4. View monthly breakdown
5. View seller-specific fees

**Expected Result:**
- Fees recorded automatically on each transaction
- Default fee: 2.5% + $0.30
- Monthly summary shows:
  - Count of transactions
  - Total fees collected
  - Average fee
- Seller can view their fees paid
- Reports generate in real-time

**Sample Report Output:**
```
Month 1: 15 transactions, $37.50 total, $2.50 average
Month 2: 22 transactions, $55.00 total, $2.50 average
Total YTD: $92.50
```

---

## PART 5: WEBHOOK TESTING

### Setup Stripe Webhook Endpoint

**1. Stripe Dashboard Configuration:**
- Go to Developers → Webhooks
- Add endpoint: `https://your-domain/webhooks/stripe`
- Select events to listen for:
  - `payment_intent.succeeded`
  - `payment_intent.payment_failed`
  - `charge.refunded`
  - `charge.dispute.created`
- Copy Webhook Secret (store in environment variable)

**2. Environment Configuration:**
```
STRIPE_WEBHOOK_SECRET=whsec_test_xxxxx
SK_TEST=sk_test_xxxxx
PK_TEST=pk_test_xxxxx
```

### Test Webhook Events

**Scenario 1: Successful Payment**
1. Buyer completes purchase
2. Payment intent status: `succeeded`
3. Stripe sends webhook event
4. Webhook handler receives event
5. Order marked COMPLETED
6. Escrow released to seller

**Scenario 2: Failed Payment**
1. Buyer uses declined test card: 4000 0000 0000 0002
2. Payment intent status: `payment_failed`
3. Stripe sends webhook event
4. Webhook handler processes
5. Buyer notified of payment failure
6. Escrow released back to buyer (if held)
7. Order marked FAILED

**Scenario 3: Refund**
1. Admin initiates refund
2. Charge refunded in Stripe
3. `charge.refunded` event sent
4. Webhook handler processes
5. Refund amount logged
6. Buyer notified of refund

**Scenario 4: Chargeback Dispute**
1. Buyer files chargeback in bank
2. `charge.dispute.created` event sent
3. Webhook handler creates MarketplaceDispute
4. Admin queue updated with dispute
5. Admin can review and resolve

### Verify Webhook Execution

**Check Logs:**
```
[MarketplaceLogger] WEBHOOK_EVENT: Type=payment_intent.succeeded, EventID=evt_xxxxx, Status=processed
[MarketplaceLogger] PAYMENT_PROCESSED: OrderID=123, PaymentID=pi_xxxxx, Amount=$250.00, Status=succeeded
[MarketplaceLogger] ESCROW_RELEASED: ID=1, ReleasedTo=seller
```

**Check Database:**
```sql
-- Verify order status changed
SELECT * FROM marketplace_orders WHERE id = 123;
-- Should show: status='COMPLETED', stripe_payment_id='pi_xxxxx'

-- Verify escrow released
SELECT * FROM marketplace_escrow WHERE order_id = 123;
-- Should show: status='RELEASED_TO_SELLER'
```

---

## PART 6: INPUT VALIDATION TESTING

### Test Validation Layer

**Quantity Validation:**
```java
MarketplaceValidator.validateQuantity(0);      // Throws INVALID_QUANTITY
MarketplaceValidator.validateQuantity(-5);     // Throws INVALID_QUANTITY
MarketplaceValidator.validateQuantity(100);    // OK
MarketplaceValidator.validateQuantity(50, 30); // Throws INVALID_QUANTITY (exceeds max)
```

**Price Validation:**
```java
MarketplaceValidator.validatePrice(0);        // Throws INVALID_PRICE
MarketplaceValidator.validatePrice(-10);      // Throws INVALID_PRICE
MarketplaceValidator.validatePrice(150);      // OK
MarketplaceValidator.validatePrice(40, 50, 100); // Throws PRICE_BELOW_MINIMUM (40 < 50)
```

**Email Validation:**
```java
MarketplaceValidator.validateEmail("test@example.com");    // OK
MarketplaceValidator.validateEmail("invalid-email");       // Throws exception
MarketplaceValidator.validateEmail("@example.com");        // Throws exception
```

**KYC Validation:**
```java
MarketplaceValidator.validateKycForAmount(true, 10000);    // OK (verified user)
MarketplaceValidator.validateKycForAmount(false, 3000);    // OK (under limit)
MarketplaceValidator.validateKycForAmount(false, 10000);   // Throws KYC_FAILED
```

**Authorization Validation:**
```java
MarketplaceValidator.validateOwnership(123, 123);  // OK (same owner)
MarketplaceValidator.validateOwnership(123, 456);  // Throws UNAUTHORIZED
```

---

## PART 7: TROUBLESHOOTING

### Issue: "Connection cannot be resolved" Errors
**Cause:** Pre-existing Java compilation issue in workspace  
**Solution:** This is in existing WalletService.java, not in new code. All new implementations compile successfully.

### Issue: Webhook Events Not Received
**Cause 1:** Webhook secret misconfigured  
**Solution:** Verify STRIPE_WEBHOOK_SECRET matches Stripe dashboard

**Cause 2:** Endpoint not accessible  
**Solution:** Verify HTTPS endpoint, firewall rules

**Cause 3:** Event type not selected  
**Solution:** Check Stripe webhook settings include required events

### Issue: Validation Throwing Exception
**Expected Behavior:** This is correct. Catch MarketplaceException and display error to user.

**Example:**
```java
try {
    MarketplaceValidator.validateQuantity(quantity);
} catch (MarketplaceException e) {
    showAlert("Error: " + e.getUserMessage());
}
```

### Issue: Audit Logs Not Writing
**Cause:** File handler not initialized  
**Solution:** MarketplaceLogger initializes FileHandler in static block. Check file permissions for `marketplace_operations.log`

---

## NEXT STEPS

1. ✅ Run `MarketplaceServiceTests` - verify 23/23 tests pass
2. ✅ Run `DataConsistencyService.runFullCheck()` - verify 0 issues
3. ✅ Configure Stripe webhook keys in environment
4. ✅ Test GreenWallet features in JavaFX UI
5. ✅ Test Marketplace purchase flow end-to-end
6. ✅ Test dispute resolution workflow
7. ✅ Monitor `marketplace_operations.log` for audit trail
8. ✅ Perform load testing with concurrent transactions

---

**Document Version:** 1.0  
**Created:** 2024  
**Status:** Ready for Testing

