# Java/Web DB Bridge Test

This guide lets you test data synchronization between web and Java through MySQL.

## 1) Apply migration

Run this SQL file on the same schema used by both apps (example: `greenledger`):

- `database_java_web_bridge.sql`

## 2) What was added

- Read methods in:
  - `src/main/java/Services/MlPredictionService.java`
  - `src/main/java/Services/MlDecisionSnapshotService.java`
  - `src/main/java/Services/PdfExportService.java`
- New read service:
- Smoke test runner:
  - `src/main/java/tools/DbBridgeSmokeTest.java`

## 3) Smoke test usage

Run with a project id (and optional evaluation id):

`DbBridgeSmokeTest <projectId> [evaluationId]`

Expected output shows latest rows fetched from:
- `carbon_metric`
- `ml_predictions`
- `ml_decision_snapshots`
- `pdf_exports`

## 4) Integration rule

Always fetch latest DB state after create/update actions in either app.
The database is the shared source of truth.

