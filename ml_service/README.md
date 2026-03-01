# Carbon Analysis Service

FastAPI microservice that predicts ESG score, carbon risk, credibility score, and recommendations.

## Endpoints

- `POST /analyze-project`

### Request body

```json
{
  "description": "...",
  "budget": 200000,
  "sector": "Energy",
  "criteres": [
    {"name": "Emissions", "note": 7, "respect": true},
    {"name": "Energie", "note": 5, "respect": false}
  ]
}
```

### Response

```json
{
  "predicted_esg_score": 78,
  "carbon_risk": "Low",
  "credibility_score": 85,
  "recommendations": "Improve reporting on CO2 reduction metrics"
}
```

## Run

```powershell
python -m venv .venv
.\.venv\Scripts\Activate.ps1
pip install -r requirements.txt
python -m uvicorn app:app --reload --port 8082
```

## Quick test

```powershell
python smoke_test.py
```
