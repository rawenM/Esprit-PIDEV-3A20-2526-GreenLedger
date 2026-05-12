# Requirements Document

## Introduction

This feature translates the blockchain, wallet, and marketplace business logic from the Symfony/PHP project into the existing Java/JavaFX desktop application. The Symfony source code is the sole source of truth — no new functionality is invented. Both projects share the same MySQL database schema, so the Java implementation must read and write the same tables with the same column semantics.

The Java project currently contains SQL-only implementations of wallet management, marketplace listings, orders, and transfers. These must be replaced with blockchain-integrated equivalents that mirror what the Symfony services actually do.

The migration covers seven functional areas, each corresponding directly to a Symfony service class:

1. **BlockchainService** — RPC health checks, dev-mode simulation, Node.js script invocation, mint/transfer/retire dispatch
2. **BlockchainBatchIssuanceService** — one-batch-per-project issuance lifecycle with PENDING→SUBMITTED→CONFIRMED states
3. **TransactionService** — `blockchain_transactions` table management and status lifecycle
4. **EventListenerService** — the only component allowed to mutate wallet balances; idempotent event application
5. **MarketplaceService** — listing creation and amount reservation/release using big-integer arithmetic
6. **TradeExecutionService** — pessimistic-lock order processing and blockchain transfer submission
7. **GreenWalletCrudService** — wallet CRUD without direct balance mutation

---

## Glossary

- **Blockchain_Service**: The Java service responsible for communicating with the blockchain node via HTTP/RPC and invoking the Node.js contract script.
- **Issuance_Service**: The Java service that issues one carbon credit batch per project, managing the PENDING→SUBMITTED→CONFIRMED lifecycle.
- **Transaction_Service**: The Java service that manages the `blockchain_transactions` table and its status transitions.
- **Event_Listener_Service**: The Java service that applies confirmed blockchain events to wallet balances and batch state. THE ONLY component allowed to mutate `wallet.available_credits`.
- **Marketplace_Service**: The Java service that manages `marketplace_listings` records, including amount reservation and release.
- **Trade_Execution_Service**: The Java service that processes marketplace orders by submitting blockchain transfer transactions.
- **Wallet_Crud_Service**: The Java service that manages wallet CRUD operations without mutating credit balances directly.
- **Dev_Mode**: A runtime mode controlled by the `APP_BLOCKCHAIN_DEV_MODE` environment variable. When enabled, blockchain calls are simulated locally instead of invoking the Node.js script.
- **Base_Units**: The on-chain representation of credit amounts, equal to credits × 10^18 (analogous to wei in Ethereum). All on-chain arithmetic uses base units.
- **Credits**: The human-readable credit amount, equal to base_units ÷ 10^18.
- **Node_Script**: The Node.js script `blockchain/scripts/carbon-batch-token.js` that submits transactions to the blockchain contract.
- **RPC_URL**: The HTTP endpoint of the blockchain node, configured via environment variable.
- **Contract_Address**: The deployed smart contract address, configured via environment variable.
- **Signer_Key**: The private key used to sign blockchain transactions, configured via environment variable.
- **Tx_Hash**: A unique transaction hash returned by the blockchain or generated with a `dev_` prefix in dev mode.
- **Wallet**: A row in the `wallet` table representing a carbon credit holder, with an optional `blockchain_address`.
- **Carbon_Credit_Batch**: A row in the `carbon_credit_batches` table representing a discrete issuance of credits from a project.
- **Blockchain_Transaction**: A row in the `blockchain_transactions` table tracking the lifecycle of a single on-chain operation.
- **Wallet_Batch_Balance**: A row in the `wallet_batch_balances` table recording how many credits from a specific batch a wallet holds.
- **Marketplace_Listing**: A row in the `marketplace_listings` table representing an active sell offer.
- **Marketplace_Order**: A row in the `marketplace_orders` table representing a buyer's purchase of a listing.

---

## Requirements

### Requirement 1: Blockchain Health Check

**User Story:** As a system operator, I want the Java application to verify blockchain connectivity before performing any on-chain operation, so that failures are detected early with a clear diagnosis.

#### Acceptance Criteria

1. THE Blockchain_Service SHALL expose a `getHealthStatus()` method that returns a structured result containing three sub-checks: `rpc` (ready flag, configured URL, issue description), `contract` (ready flag, configured address, issue description), and `signer` (ready flag, issue description).
2. WHEN `getHealthStatus()` is called, THE Blockchain_Service SHALL check RPC reachability by sending an HTTP POST request with method `eth_blockNumber` to the configured RPC_URL and recording whether a valid response is received.
3. WHEN `getHealthStatus()` is called, THE Blockchain_Service SHALL check contract deployment by calling `eth_getCode` for the configured Contract_Address and recording whether the returned code is non-empty and not equal to `0x`.
4. WHEN `getHealthStatus()` is called, THE Blockchain_Service SHALL check signer availability by verifying that the configured Signer_Key environment variable is present and non-empty.
5. THE Blockchain_Service SHALL expose a `preflightCheck()` method that calls `getHealthStatus()` and throws a `RuntimeException` with a descriptive message if any of the three sub-checks reports `ready = false`.
6. WHEN `eth_getCode` returns an empty result or `0x` for the Contract_Address, THE Blockchain_Service SHALL record the contract sub-check as not ready with an issue message indicating the contract is not deployed at that address.

---

### Requirement 2: Dev Mode Detection and Simulation

**User Story:** As a developer, I want the Java application to simulate blockchain calls locally when dev mode is enabled, so that I can test the full workflow without a live blockchain node.

#### Acceptance Criteria

1. THE Blockchain_Service SHALL expose an `isDevModeEnabled()` method that returns `true` when the `APP_BLOCKCHAIN_DEV_MODE` environment variable is set to one of the values `1`, `true`, `yes`, or `on` (case-insensitive), and `false` otherwise.
2. WHEN `isDevModeEnabled()` returns `true` and a mint, transfer, or retire operation is submitted, THE Blockchain_Service SHALL call `buildSimulatedResult()` instead of invoking the Node_Script.
3. WHEN `buildSimulatedResult()` is called, THE Blockchain_Service SHALL generate a Tx_Hash with the prefix `dev_` followed by a deterministic identifier derived from the operation payload.
4. WHEN `buildSimulatedResult()` is called for a mint operation, THE Blockchain_Service SHALL call `Event_Listener_Service.simulateMint()` with the generated Tx_Hash, batch identifier, wallet identifier, and amount in Credits (base_units ÷ 10^18).
5. WHEN `buildSimulatedResult()` is called for a transfer operation, THE Blockchain_Service SHALL call `Event_Listener_Service.simulateTransfer()` with the generated Tx_Hash, batch identifier, from-wallet identifier, to-wallet identifier, and amount in Credits.
6. WHEN `buildSimulatedResult()` is called for a retire operation, THE Blockchain_Service SHALL call `Event_Listener_Service.simulateRetire()` with the generated Tx_Hash, batch identifier, wallet identifier, and amount in Credits.
7. WHEN `isDevModeEnabled()` returns `true` and a wallet has no `blockchain_address`, THE Blockchain_Service SHALL generate a deterministic address by computing `sha256("dev:wallet:" + walletId)` and formatting the first 20 bytes as a `0x`-prefixed hex string.

---

### Requirement 3: Node.js Script Invocation

**User Story:** As a system operator, I want the Java application to invoke the Node.js contract script for live blockchain operations, so that transactions are submitted to the real network.

#### Acceptance Criteria

1. WHEN `isDevModeEnabled()` returns `false` and a blockchain operation is submitted, THE Blockchain_Service SHALL invoke the Node_Script by writing the operation payload as JSON to a temporary file and executing `node blockchain/scripts/carbon-batch-token.js <tempfile>` as a subprocess.
2. WHEN the Node_Script subprocess exits with status 0, THE Blockchain_Service SHALL parse the subprocess's standard output as JSON and return the result.
3. IF the Node_Script subprocess exits with a non-zero status or produces output that cannot be parsed as JSON, THEN THE Blockchain_Service SHALL throw a `RuntimeException` containing the raw output and exit code.
4. THE Blockchain_Service SHALL delete the temporary payload file after the subprocess completes, regardless of success or failure.

---

### Requirement 4: Mint, Transfer, and Retire Dispatch

**User Story:** As a system component, I want the Blockchain_Service to dispatch mint, transfer, and retire operations with the correct wallet addresses and payload, so that on-chain state matches the intended credit movements.

#### Acceptance Criteria

1. WHEN `mintBatch(walletId, batchId, amountBaseUnits, metadata)` is called, THE Blockchain_Service SHALL look up the `blockchain_address` for `walletId` from the `wallet` table and call `submitTransaction` with `method=mint`, the resolved address, `batchId`, `amountBaseUnits`, and `metadata`.
2. WHEN `transferBatch(fromWalletId, toWalletId, batchId, amountBaseUnits, metadata)` is called, THE Blockchain_Service SHALL look up `blockchain_address` for both `fromWalletId` and `toWalletId` from the `wallet` table and call `submitTransaction` with `method=transfer`, both resolved addresses, `batchId`, `amountBaseUnits`, and `metadata`.
3. WHEN `retireBatch(walletId, batchId, amountBaseUnits, reason, metadata)` is called, THE Blockchain_Service SHALL call `submitTransaction` with `method=burn`, the resolved address for `walletId`, `batchId`, `amountBaseUnits`, `reason`, and `metadata`.
4. IF a wallet referenced by `walletId` does not have a `blockchain_address` and dev mode is disabled, THEN THE Blockchain_Service SHALL throw a `RuntimeException` indicating the wallet has no blockchain address.
5. WHEN `submitTransaction(payload)` is called, THE Blockchain_Service SHALL route to `buildSimulatedResult(payload)` when dev mode is enabled, or to the Node_Script invocation when dev mode is disabled.

---

### Requirement 5: Carbon Credit Batch Issuance Lifecycle

**User Story:** As a project owner, I want issuing credits for a project to be idempotent and to follow a strict PENDING→SUBMITTED→CONFIRMED lifecycle, so that retries are safe and the database always reflects the true state.

#### Acceptance Criteria

1. WHEN `issueProjectCredits(projectId, creditsAmount)` is called, THE Issuance_Service SHALL load the project from the `projet` table and resolve the owner wallet by checking `entreprise_id`, `created_by`, and `user_id` fields in that order.
2. WHEN the resolved owner has no wallet, THE Issuance_Service SHALL create one by calling `Wallet_Crud_Service.createWallet()` before proceeding.
3. THE Issuance_Service SHALL convert `creditsAmount` to base units by multiplying by 10^18 using arbitrary-precision integer arithmetic (no floating-point).
4. WHEN `issueProjectCredits` is called and a `carbon_credit_batches` row already exists for `projectId` with `issuance_status = FAILED`, THE Issuance_Service SHALL retry the failed batch instead of creating a new one.
5. WHEN `issueProjectCredits` is called and a `carbon_credit_batches` row already exists for `projectId` with any status other than FAILED, THE Issuance_Service SHALL return the existing issuance payload without creating a new batch or submitting a new transaction.
6. WHEN creating a new batch, THE Issuance_Service SHALL insert a row into `carbon_credit_batches` with `issuance_status = PENDING_TX` and insert a corresponding row into `blockchain_transactions` with `status = PENDING` inside a single database transaction, and commit that transaction BEFORE calling `Blockchain_Service.mintBatch()`.
7. The `carbon_credit_batches` table SHALL enforce a UNIQUE constraint on `project_id` so that at most one batch exists per project.
8. WHEN `Blockchain_Service.mintBatch()` returns successfully, THE Issuance_Service SHALL update the batch row to `issuance_status = SUBMITTED` and store the returned Tx_Hash.
9. IF `Blockchain_Service.mintBatch()` throws an exception, THEN THE Issuance_Service SHALL update the batch row to `issuance_status = FAILED` and update the `blockchain_transactions` row to `status = FAILED` with the error message.
10. THE Issuance_Service SHALL set the following fields on the new `carbon_credit_batches` row: `project_id`, `wallet_id`, `total_amount`, `remaining_amount`, `total_amount_base_units`, `remaining_amount_base_units`, `status = AVAILABLE`, `batch_type = ISSUANCE`.

---

### Requirement 6: Blockchain Transaction Table Management

**User Story:** As a system component, I want a dedicated service to manage the `blockchain_transactions` table, so that every on-chain operation has a tracked lifecycle record.

#### Acceptance Criteria

1. THE Transaction_Service SHALL auto-create the `blockchain_transactions` table if it does not exist when the service initializes.
2. THE Transaction_Service SHALL auto-create the `blockchain_sync_state` table if it does not exist when the service initializes.
3. THE Transaction_Service SHALL auto-create the `blockchain_event_log` table if it does not exist when the service initializes.
4. WHEN `createPendingTransaction(type, walletId, batchId, amountBaseUnits, requestPayload, fromWalletId, toWalletId, reason)` is called, THE Transaction_Service SHALL insert a row into `blockchain_transactions` with `status = PENDING` and return the new row identifier.
5. WHEN `markSubmitted(transactionId, txHash, blockNumber, logIndex)` is called, THE Transaction_Service SHALL UPDATE the row WHERE `id = transactionId AND status = PENDING`, setting `status = SUBMITTED`, `tx_hash`, `block_number`, and `log_index`.
6. WHEN `markFailed(transactionId, error)` is called, THE Transaction_Service SHALL UPDATE the row WHERE `id = transactionId AND status = PENDING`, setting `status = FAILED` and `error_message`.
7. WHEN `markConfirmedByTxHash(txHash, blockNumber, logIndex)` is called, THE Transaction_Service SHALL UPDATE the row WHERE `tx_hash = txHash AND status = SUBMITTED`, setting `status = CONFIRMED`, `block_number`, and `log_index`.
8. WHEN `recordEventIfNew(txHash, logIndex, eventName, blockNumber)` is called, THE Transaction_Service SHALL execute an INSERT IGNORE into `blockchain_event_log` so that duplicate events are silently discarded.
9. THE Transaction_Service SHALL expose `getSyncState(listenerName)` and `saveSyncState(listenerName, lastProcessedBlock, lastProcessedTxHash, lastProcessedLogIndex)` methods that read and write rows in the `blockchain_sync_state` table keyed by `listener_name`.
10. THE Transaction_Service SHALL expose `findStaleSubmittedTransactions(olderThanMinutes)` that returns all rows from `blockchain_transactions` with `status = SUBMITTED` and `created_at` older than the specified number of minutes.

---

### Requirement 7: Event Application and Wallet Balance Mutation

**User Story:** As a system component, I want a single authoritative service to apply confirmed blockchain events to wallet balances and batch state, so that balance mutations are idempotent and traceable.

#### Acceptance Criteria

1. THE Event_Listener_Service SHALL be the ONLY component in the Java application that writes to `wallet.available_credits` or `wallet.retired_credits`.
2. WHEN `applyMintEvent(txHash, batchId, walletId, amount)` is called, THE Event_Listener_Service SHALL first check idempotency by calling `isEventAlreadyApplied(txHash, MINT)`; if the event has already been applied, it SHALL return without making any changes.
3. WHEN `applyTransferEvent(txHash, batchId, fromWalletId, toWalletId, amount)` is called, THE Event_Listener_Service SHALL first check idempotency by calling `isEventAlreadyApplied(txHash, TRANSFER)`; if already applied, it SHALL return without changes.
4. WHEN `applyRetireEvent(txHash, batchId, walletId, amount)` is called, THE Event_Listener_Service SHALL first check idempotency by calling `isEventAlreadyApplied(txHash, RETIRE)`; if already applied, it SHALL return without changes.
5. WHEN a mint event body is applied, THE Event_Listener_Service SHALL: increment `wallet.available_credits` by `amount` for `walletId`; upsert a `wallet_batch_balances` row adding `amount` to the balance; set `carbon_credit_batches.status = active`; call `Transaction_Service.markConfirmedByTxHash(txHash)`; and insert a row into `wallet_transactions` with `type = MINT`.
6. WHEN a transfer event body is applied, THE Event_Listener_Service SHALL: decrement `wallet.available_credits` by `amount` for `fromWalletId`; increment `wallet.available_credits` by `amount` for `toWalletId`; upsert `wallet_batch_balances` for both wallets; call `Transaction_Service.markConfirmedByTxHash(txHash)`; UPDATE `marketplace_orders SET status = COMPLETED WHERE transfer_tx_hash = txHash AND status IN (PAID, SUBMITTED)`; and insert two rows into `wallet_transactions` with types `TRANSFER_OUT` and `TRANSFER_IN`.
7. WHEN a retire event body is applied, THE Event_Listener_Service SHALL: decrement `wallet.available_credits` by `amount` for `walletId`; upsert `wallet_batch_balances` subtracting `amount`; set `carbon_credit_batches.status = retired`; call `Transaction_Service.markConfirmedByTxHash(txHash)`; and insert a row into `wallet_transactions` with `type = RETIRE`.
8. THE Event_Listener_Service SHALL expose `simulateMint()`, `simulateTransfer()`, and `simulateRetire()` methods that apply the same mutations as their `apply*` counterparts but skip the idempotency check, for use in dev mode only.
9. WHEN `upsertWalletBatchBalance(walletId, batchId, delta, deltaBaseUnits)` is called, THE Event_Listener_Service SHALL INSERT a new row into `wallet_batch_balances` if none exists for `(walletId, batchId)`, or UPDATE the existing row by adding `delta` to `balance` and `deltaBaseUnits` to `balance_base_units`.
10. WHEN `isEventAlreadyApplied(txHash, eventType)` is called, THE Event_Listener_Service SHALL return `true` if a row exists in `blockchain_transactions` WHERE `tx_hash = txHash AND type = eventType AND status = CONFIRMED`.
11. THE Event_Listener_Service SHALL expose `backfillWalletSummaryColumns()` that recalculates `wallet.available_credits` for every wallet by summing the `balance` column from `wallet_batch_balances` grouped by `wallet_id`, and updating the `wallet` table accordingly.

---

### Requirement 8: Marketplace Listing Creation and Reservation

**User Story:** As a seller, I want to create marketplace listings and have the system reserve and release credit amounts atomically, so that the same credits cannot be sold twice.

#### Acceptance Criteria

1. WHEN `createListing(payload)` is called, THE Marketplace_Service SHALL INSERT a row into `marketplace_listings` with the fields: `seller_id`, `seller_wallet_id`, `batch_id`, `asset_type`, `wallet_id`, `quantity_or_id`, `price_per_unit`, `currency_code`, `status = ACTIVE`, `total_amount_base_units`, `reserved_amount_base_units = 0`, `filled_amount_base_units = 0`.
2. WHEN `getListing(listingId)` is called, THE Marketplace_Service SHALL SELECT the row from `marketplace_listings WHERE id = listingId` and return it.
3. WHEN `reserveListingAmount(listingId, amountBaseUnits, reservationMinutes)` is called, THE Marketplace_Service SHALL acquire a `SELECT FOR UPDATE` lock on the listing row before performing any arithmetic.
4. WHEN computing available amount during a reservation, THE Marketplace_Service SHALL calculate `available = total_amount_base_units - filled_amount_base_units - reserved_amount_base_units` using string-based big-integer arithmetic with no floating-point operations.
5. IF `available < amountBaseUnits` during a reservation attempt, THEN THE Marketplace_Service SHALL throw an exception indicating insufficient available amount without modifying the listing row.
6. WHEN a reservation succeeds, THE Marketplace_Service SHALL UPDATE `reserved_amount_base_units` by adding `amountBaseUnits`, set `reservation_expires_at` to the current time plus `reservationMinutes`, and set `status` to `RESERVED` if the full amount is reserved or `PARTIALLY_RESERVED` if a partial amount remains available.
7. WHEN `releaseReservation(listingId, amountBaseUnits)` is called, THE Marketplace_Service SHALL decrement `reserved_amount_base_units` by `amountBaseUnits` and restore `status` to `ACTIVE` if the listing is not fully filled, or `FILLED` if `filled_amount_base_units = total_amount_base_units`.
8. THE Marketplace_Service SHALL perform all amount arithmetic using pure string big-integer operations, with no dependency on floating-point types or BCMath-equivalent libraries for correctness.

---

### Requirement 9: Trade Execution with Pessimistic Locking

**User Story:** As a marketplace participant, I want order transfers to be executed with pessimistic database locks, so that concurrent order processing cannot cause double-spending or inconsistent state.

#### Acceptance Criteria

1. WHEN `submitOrderTransfer(orderId)` is called, THE Trade_Execution_Service SHALL acquire a `SELECT FOR UPDATE` lock on the `marketplace_orders` row for `orderId` before reading any order fields.
2. WHEN processing an order transfer, THE Trade_Execution_Service SHALL validate that `payment_status = CONFIRMED` and `status IN (PAID, SUBMITTED)` before proceeding; if either condition is not met, it SHALL throw an exception without modifying any state.
3. WHEN the order passes validation, THE Trade_Execution_Service SHALL read `buyer_wallet_id`, `seller_wallet_id`, `batch_id`, and `amount_base_units` from the locked order row.
4. WHEN processing an order transfer, THE Trade_Execution_Service SHALL verify that the seller's `wallet_batch_balances` row for `(seller_wallet_id, batch_id)` has `balance_base_units >= amount_base_units` before submitting the blockchain transaction.
5. WHEN the seller balance check passes, THE Trade_Execution_Service SHALL call `Transaction_Service.createPendingTransaction()` to create a PENDING `blockchain_transactions` row, then call `Blockchain_Service.transferBatch(sellerWalletId, buyerWalletId, batchId, amountBaseUnits, metadata)`.
6. WHEN `Blockchain_Service.transferBatch()` returns successfully, THE Trade_Execution_Service SHALL call `Transaction_Service.markSubmitted()` with the returned Tx_Hash and UPDATE `marketplace_orders SET status = SUBMITTED, transfer_tx_hash = txHash`.
7. THE Trade_Execution_Service SHALL expose `retryOrderTransfer(orderId)` that checks whether `transfer_tx_hash` is already set on the order (idempotency guard) before calling the transfer submission logic.
8. THE Trade_Execution_Service SHALL expose `retryPendingTransfers(limit)` that queries `marketplace_orders` for rows with `status = PAID` and `payment_status = CONFIRMED`, up to `limit` rows, and calls `submitOrderTransfer()` for each.
9. WHEN `submitOrderTransfer()` fails due to insufficient seller inventory and dev mode is enabled, THE Trade_Execution_Service SHALL attempt to reissue batch inventory for the order and then retry the transfer once.

---

### Requirement 10: Wallet CRUD Without Direct Balance Mutation

**User Story:** As a system component, I want wallet creation, listing, lookup, update, and deletion to be handled by a dedicated service that never directly modifies credit balances, so that balance integrity is enforced through the Event_Listener_Service exclusively.

#### Acceptance Criteria

1. WHEN `createWallet(name, walletNumber, ownerType, ownerId)` is called, THE Wallet_Crud_Service SHALL INSERT a row into the `wallet` table with the provided fields; if `walletNumber` is null, it SHALL generate a unique wallet number before inserting.
2. WHEN `listWallets(isAdmin, ownerId)` is called, THE Wallet_Crud_Service SHALL SELECT from the `wallet` table using COALESCE to handle null `name` and `blockchain_address` values; if `isAdmin` is false, it SHALL filter by `owner_id = ownerId`.
3. WHEN `findWalletById(walletId, isAdmin, ownerId)` is called and `isAdmin` is false, THE Wallet_Crud_Service SHALL include a WHERE clause checking `owner_id = ownerId` in addition to `id = walletId`, and return null if no matching row is found.
4. WHEN `updateWalletName(walletId, name, ownerType, isAdmin, ownerId)` is called, THE Wallet_Crud_Service SHALL UPDATE `wallet.name` for the specified wallet, applying the owner check when `isAdmin` is false.
5. WHEN `deleteWallet(walletId, isAdmin, ownerId)` is called, THE Wallet_Crud_Service SHALL SELECT the wallet's `available_credits` before deleting; IF `available_credits > 0`, THEN it SHALL throw an exception indicating the wallet cannot be deleted while it holds credits.
6. WHEN `deleteWallet` passes the balance check, THE Wallet_Crud_Service SHALL DELETE the row from the `wallet` table, applying the owner check when `isAdmin` is false.
7. THE Wallet_Crud_Service SHALL expose `transferWalletOwnership(walletId, newOwnerId, newOwnerType)` that UPDATEs `wallet.owner_id` and `wallet.owner_type` for the specified wallet.
8. THE Wallet_Crud_Service SHALL NOT expose any method that directly writes to `wallet.available_credits` or `wallet.retired_credits`; any attempt to add such a method SHALL be rejected as a violation of the balance-mutation boundary enforced by Event_Listener_Service.

---

### Requirement 11: Replacement of Existing Java SQL-Only Services

**User Story:** As a developer, I want the existing SQL-only Java services to be replaced by their blockchain-integrated equivalents, so that the Java application uses the same credit lifecycle logic as the Symfony project.

#### Acceptance Criteria

1. THE existing `WalletService.java` SQL-only implementation of `issueCredits`, `retireCredits`, and `transferCredits` SHALL be replaced; credit mutations SHALL only occur through `Event_Listener_Service` in response to confirmed or simulated blockchain events.
2. THE existing `MarketplaceListingService.java` SHALL be replaced by an implementation backed by `Marketplace_Service` that uses `SELECT FOR UPDATE` reservation logic and big-integer amount arithmetic.
3. THE existing `MarketplaceOrderService.java` order processing logic that directly updates balances SHALL be replaced by `Trade_Execution_Service` which submits blockchain transfer transactions and relies on `Event_Listener_Service` to apply the resulting balance changes.
4. THE existing `TransferService.java` SQL-only atomic transfer logic SHALL be replaced; all credit transfers between wallets SHALL go through `Blockchain_Service.transferBatch()` and be applied to balances only by `Event_Listener_Service`.
5. WHEN the Java application starts, THE Blockchain_Service SHALL call `preflightCheck()` and log the result of `getHealthStatus()` so that misconfiguration is surfaced immediately at startup.
6. WHERE `APP_BLOCKCHAIN_DEV_MODE` is not set or is set to a falsy value, THE Blockchain_Service SHALL require a reachable RPC_URL, a deployed contract at Contract_Address, and a non-empty Signer_Key before accepting any mint, transfer, or retire operation.
