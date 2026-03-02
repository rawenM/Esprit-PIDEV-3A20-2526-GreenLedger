# Carbon Expert ML (Synthetic + Augmentation)

This package trains a baseline ML model to evaluate projects using synthetic data and augmentation.

## Files
- `generate_data.py`: synthetic dataset generator based on carbon analytics schema
- `augment_data.py`: data augmentation (noise, feature scaling, label flip)
- `train_model.py`: train/evaluate model and export artifacts
- `extract_from_db.py`: extract training data from MySQL (evaluation/projet)
- `predict.py`: run model inference from JSON (stdin)
- `run_pipeline.py`: generate large synthetic data + augment + train
- `requirements.txt`: Python dependencies

## Quick start
```powershell
python -m venv .venv
.venv\Scripts\Activate.ps1
pip install -r ml\requirements.txt
python ml\generate_data.py --rows 5000 --out data\synthetic.csv
python ml\augment_data.py --in data\synthetic.csv --out data\augmented.csv --noise 0.05 --flip 0.02
python ml\train_model.py --in data\augmented.csv --model-out models\carbon_model.joblib --report-out reports\metrics.json
```

## Large synthetic dataset
```powershell
python ml\run_pipeline.py --rows 100000
```

## Extract from DB
Set env vars (or pass flags):
- `DB_HOST`, `DB_PORT`, `DB_USER`, `DB_PASSWORD`, `DB_NAME`

```powershell
python ml\extract_from_db.py --out data\db_extract.csv
python ml\augment_data.py --in data\db_extract.csv --out data\db_augmented.csv --noise 0.05 --flip 0.02
python ml\train_model.py --in data\db_augmented.csv --model-out models\carbon_model.joblib --report-out reports\metrics.json
```

## Java API prediction endpoint
Endpoint:
- `POST /predict`

Default port: `8081` (set `API_PORT` to override).
Set `ML_API_BASE_URL` if the UI should call a different host/port.

Env vars for Java process:
- `PYTHON` (default: `python`)
- `CARBON_MODEL_PATH` (default: `models/carbon_model.joblib`)
- `API_PORT` (default: `8081`)
- `ML_API_BASE_URL` (default: `http://localhost:8081`)

Response includes:
- `predictions` (decision)
- `confidence`
- `recommendations` with `summary` + `actions`

Example request body (JSON):
```json
{
  "sector": "energy",
  "region": "EU",
  "size": "MEDIUM",
  "baseline_emissions_tco2": 4200,
  "target_reduction_pct": 18,
  "avg_note": 7.4,
  "min_note": 5.0,
  "compliance_rate": 0.86,
  "blocking_criteria": 0,
  "scope1_tco2": 1400,
  "scope2_tco2": 1200,
  "scope3_tco2": 1600,
  "total_tco2": 4200,
  "scenario_delta": 0.1,
  "score": 7.2
}
```

## Troubleshooting predictor failed
If `/predict` returns `Predictor failed`, set the environment variables below **before** starting the app:

```powershell
$env:PYTHON="C:\Users\Mega-PC\AppData\Local\Programs\Python\Python312\python.exe"
$env:CARBON_MODEL_PATH="C:\Users\Mega-PC\Desktop\Pi_Dev\models\carbon_model.joblib"
$env:API_PORT="8081"
```

Then restart the app so the API server picks them up.

## Running from Desktop
If the app is started from a different working directory, set:

```powershell
$env:PROJECT_ROOT="C:\Users\Mega-PC\Desktop\Pi_Dev"
$env:PYTHON="C:\Users\Mega-PC\AppData\Local\Programs\Python\Python312\python.exe"
$env:CARBON_MODEL_PATH="C:\Users\Mega-PC\Desktop\Pi_Dev\models\carbon_model.joblib"
$env:API_PORT="8081"
```

The predictor uses `PROJECT_ROOT` to resolve `ml/predict.py` and the model path.

## Schema check before evaluation
The UI now verifies required DB columns before creating or modifying evaluations.
If any column is missing, you will see a clear error message.
