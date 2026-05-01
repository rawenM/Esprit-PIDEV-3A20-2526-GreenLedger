# GreenLedger

GreenLedger is a JavaFX platform for carbon governance, sustainability operations, and climate-finance workflows.
It combines project evaluation, carbon-credit lifecycle management, trading, auditability, and AI-assisted analysis in one application.

The codebase includes:

- A desktop application (JavaFX + Java 17)
- A MySQL-backed domain model (wallets, batches, listings, offers, orders, audit trails)
- Optional Python ML components for scoring and prediction services

## Why This Project Exists

Most sustainability tools split data across spreadsheets, isolated dashboards, and external marketplaces.
GreenLedger centralizes those operations so teams can:

- Evaluate projects with ESG and carbon indicators
- Mint, transfer, retire, and trace carbon credits
- Trade credits through a controlled marketplace
- Preserve operational and compliance evidence
- Add AI support where it creates measurable value

## Product Modules

### 1. Carbon and ESG Evaluation

Core project-evaluation workflows are implemented in the carbon and project controllers/services.
Capabilities include:

- Carbon report generation and enrichment with external data
- ESG-oriented decision support and policy-style outcomes
- Risk-oriented recommendations and scoring flows
- Owner and expert evaluation views

### 2. Green Wallet (Full Carbon Credit Lifecycle)

The Green Wallet module is one of the central pillars of the platform.

It provides:

- Wallet creation, update, and controlled deletion
- Credit issuance into wallets (including quick issuance flows)
- Credit retirement with reason and trace tracking
- Credit transfer between wallets with atomic operations
- Live wallet metrics (available, retired, total, progress)
- Scope-oriented climate dashboard (Scope 1, 2, 3 breakdown)
- Carbon batch listing per wallet with serial and remaining amount visibility
- Batch lineage and traceability exploration

The wallet interface also integrates external climate signals:

- Interactive pollution map inside JavaFX WebView (Leaflet + OpenStreetMap)
- Background air-quality refresh and cached results persisted on disk
- Emission calculators for electricity, fuel, and shipping scenarios

In practice, Green Wallet acts as an operational console for credit inventory, climate context, and action history.

### 3. Marketplace and Trading

GreenLedger includes a built-in marketplace connected to wallet assets.

Supported flows include:

- Listing creation and management
- Browse and filter listings by asset and price
- Offer and counter-offer negotiation (buyer/seller views)
- Order history and transactional visibility
- Dynamic carbon pricing views and chart updates

Payment and risk controls include:

- Stripe-hosted checkout integration
- KYC-driven transaction limits
- Escrow-oriented behavior for larger transactions (instant vs escrow path)
- Escrow/dispute management controllers and services

### 4. Financing and Investment

Financing modules cover project funding workflows and investor views, including:

- Financing offers and service-level management
- Investment dashboard and investor-focused screens
- Risk-analysis components tied to financing decisions

### 5. Security, Trust, and Governance

The platform includes governance-oriented controls across identity and operations:

- Authentication and account management flows
- CAPTCHA and puzzle-based validation flows
- Fraud-detection service integration for user-risk analysis
- Audit-log model/service/controller for traceability use cases

### 6. AI and ML Extensions

AI/ML support exists at two levels:

- Java-side decision helpers (prediction snapshots, risk inference, NLP services)
- Python-side training and service runtime:
	- `ml`: data prep, augmentation, training, pipeline scripts
	- `ml_service`: FastAPI inference microservice

These modules are optional, but they enable richer analytics and automated decision support.

## Technology Stack

- Java 17
- Maven
- JavaFX 20 (`controls`, `fxml`, `web`, `media`, `swing`)
- MySQL Connector/J
- Gson, Apache HttpClient, SLF4J
- Stripe Java SDK
- PDF tooling (PDFBox, DynamicPDF integration)
- Twilio, Gmail API / Jakarta Mail (feature-dependent)
- Optional ML ecosystem (Python + FastAPI)

## Repository Structure

- `src/main/java`: controllers, services, models, DAO, app entrypoints
- `src/main/resources`: FXML, CSS, assets, templates, sample config
- `src/test`: Java tests
- `ml`: data + model pipeline scripts
- `ml_service`: Python API service for model serving
- `docs`: design and implementation notes
- `data`, `models`, `reports`, `output`: generated artifacts and outputs

## Prerequisites

- JDK 17
- Maven 3.8+
- MySQL server with required schema
- Python 3.10+ (only if using `ml` or `ml_service`)

## Quick Start

### 1. Build the Java application

```powershell
mvn clean compile
```

### 2. Configure environment values

Create a local `.env` file at project root and set only the keys needed by your active features.
Typical keys used in this project include:

```env
# SMTP / Email
SMTP_HOST=smtp.gmail.com
SMTP_PORT=587
SMTP_USERNAME=your_email@example.com
SMTP_PASSWORD=your_app_password
SMTP_FROM=GreenLedger Team <your_email@example.com>

# Password reset link prefix
APP_RESET_URL_PREFIX=http://127.0.0.1:8088/reset?token=

# CAPTCHA
RECAPTCHA_SITE_KEY=your_site_key
RECAPTCHA_SECRET_KEY=your_secret_key
RECAPTCHA_VERIFY_URL=https://www.google.com/recaptcha/api/siteverify

# Optional PDF provider
DPDF_API_KEY=your_dynamicpdf_api_key

# Optional OpenAI integration
OPENAI_API_URL=https://api.openai.com/v1
OPENAI_API_KEY=your_openai_api_key
```

### 3. Run the application

Recommended:

```powershell
mvn javafx:run
```

Alternative:

```powershell
.\run.bat
```

Main class:

- `org.GreenLedger.MainFX`

## Build, Test, Package

Compile:

```powershell
mvn clean compile
```

Run tests:

```powershell
mvn test
```

Create artifact:

```powershell
mvn package
```

## Optional ML Workflows

### Offline training and data pipeline

```powershell
python -m venv .venv
.venv\Scripts\Activate.ps1
pip install -r ml\requirements.txt
python ml\run_pipeline.py --rows 100000
```

### Run the FastAPI ML service

```powershell
cd ml_service
python -m venv .venv
.\.venv\Scripts\Activate.ps1
pip install -r requirements.txt
python -m uvicorn app:app --reload --port 8082
```

Smoke test:

```powershell
python smoke_test.py
```

## Useful Developer Entry Points

The `src/main/java/tools` package includes practical launchers and checks for:

- PDF integrations (Adobe, DynamicPDF, REST checks)
- environment loading
- email flow testing
- fraud-detection validation
- captcha and reset-password related tests

## Troubleshooting

If the app fails at startup:

- Verify Java and Maven versions (`java -version`, `mvn -version`)
- Rebuild from scratch (`mvn clean compile`)
- Prefer `mvn javafx:run` over manual module wiring when debugging JavaFX issues

If wallet/marketplace features fail:

- Verify database connectivity and schema consistency
- Check that wallet, batch, listing, offer, order, and audit-related tables exist
- Confirm that payment-related environment variables are correctly configured

If climate API features fail:

- Validate API keys and outbound connectivity
- Check fallback behavior and local cache files for air-quality data

## Contribution Guidelines

1. Create a branch per feature or fix.
2. Keep commits focused and reviewable.
3. Run at least `mvn clean compile` before opening a pull request.
4. Include test or manual validation evidence for behavior changes.

## Status

GreenLedger is an active, feature-rich codebase under ongoing development.
The Green Wallet, marketplace, financing, and carbon-evaluation modules are integrated and evolving together.
Use environment-scoped configuration and verify module-specific flows before production delivery.
