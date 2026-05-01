from __future__ import annotations

import json
import math
from pathlib import Path
from typing import Any

import numpy as np
import pandas as pd
from fastapi import FastAPI, HTTPException
from pydantic import BaseModel, Field

from model import ModelBundle, build_model_bundle

# ---------------------------------------------------------------------------
# Optional: load trained scikit-learn artifacts if they exist on disk
# ---------------------------------------------------------------------------
_ARTIFACTS_ESG   = Path("../tools/ml_dataset/artifacts")
_ARTIFACTS_FRAUD = Path("../tools/ml_fraud/artifacts")
_ARTIFACTS_REC   = Path("../tools/ml_recommendation/artifacts")

try:
    import joblib
    _joblib_available = True
except ImportError:
    _joblib_available = False


def _load_joblib(path: Path) -> Any | None:
    if _joblib_available and path.exists():
        try:
            return joblib.load(path)
        except Exception as exc:
            print(f"[ML] Could not load {path}: {exc}")
    return None


# ---------------------------------------------------------------------------
# App
# ---------------------------------------------------------------------------
app = FastAPI(title="GreenLedger ML Service", version="2.0.0")

model_bundle: ModelBundle | None = None
_esg_pipeline: Any | None = None
_fraud_pipeline: Any | None = None
_rec_artifact: dict | None = None
_fraud_profile: dict | None = None
_esg_profile: dict | None = None


@app.on_event("startup")
def _startup() -> None:
    global model_bundle, _esg_pipeline, _fraud_pipeline, _rec_artifact
    global _fraud_profile, _esg_profile

    # Always build the in-memory bundle (no disk dependency)
    model_bundle = build_model_bundle()

    # Try to load trained artifacts from disk
    _esg_pipeline = _load_joblib(_ARTIFACTS_ESG / "model.joblib")
    if _esg_pipeline:
        print("[ML] Loaded trained ESG model from disk")

    _fraud_pipeline = _load_joblib(_ARTIFACTS_FRAUD / "fraud_model.joblib")
    if _fraud_pipeline:
        print("[ML] Loaded trained fraud model from disk")

    rec_raw = _load_joblib(_ARTIFACTS_REC / "recommender.joblib")
    if isinstance(rec_raw, dict):
        _rec_artifact = rec_raw
        print("[ML] Loaded trained recommendation model from disk")

    # Load profiles
    fraud_profile_path = _ARTIFACTS_FRAUD / "fraud_profile.json"
    if fraud_profile_path.exists():
        with fraud_profile_path.open() as f:
            _fraud_profile = json.load(f)

    esg_schema_path = _ARTIFACTS_ESG / "inference_profile.json"
    if esg_schema_path.exists():
        with esg_schema_path.open() as f:
            _esg_profile = json.load(f)


# ---------------------------------------------------------------------------
# Shared feature columns (must match training scripts)
# ---------------------------------------------------------------------------
NUMERIC_FEATURES = [
    "consommation_energie",
    "distance_transport",
    "quantite_materiau",
    "consommation_eau",
    "dechets_generes",
    "emissions_estimees",
]

CATEGORICAL_FEATURES = [
    "secteur",
    "type_projet",
    "localisation",
    "type_transport",
    "type_materiau",
    "source_emissions",
]

RECOMMENDATION_LABELS = [
    "optimize_energy",
    "optimize_transport",
    "optimize_materials",
    "reduce_waste",
    "improve_data_quality",
    "audit_emissions",
    "mitigation_plan",
    "resubmission_pack",
]

# ---------------------------------------------------------------------------
# Request / Response schemas
# ---------------------------------------------------------------------------

class ProjectFeatures(BaseModel):
    """Environmental features shared by all endpoints."""
    consommation_energie: float | None = Field(None, description="Energy consumption (kWh or MWh)")
    distance_transport:   float | None = Field(None, description="Transport distance (km)")
    quantite_materiau:    float | None = Field(None, description="Material quantity (tonnes)")
    consommation_eau:     float | None = Field(None, description="Water consumption (m³)")
    dechets_generes:      float | None = Field(None, description="Waste generated (tonnes)")
    emissions_estimees:   float | None = Field(None, description="Declared emissions (tCO2e)")
    secteur:              str | None = None
    type_projet:          str | None = None
    localisation:         str | None = None
    type_transport:       str | None = None
    type_materiau:        str | None = None
    source_emissions:     str | None = None


class PredictRequest(ProjectFeatures):
    """ESG prediction request (matches /predict endpoint)."""
    pass


class PredictResponse(BaseModel):
    predicted_esg_score: int
    credibility_score:   int
    carbon_risk:         str
    decision:            str
    recommendations:     str
    model_source:        str  # "trained" | "fallback"


class FraudRequest(ProjectFeatures):
    """Fraud assessment request (matches /fraud/assess endpoint)."""
    pass


class FraudResponse(BaseModel):
    risk_score:    float
    anomaly_score: float
    fraud_flag:    bool
    reasons:       list[str]
    model_source:  str


class RecommendRequest(ProjectFeatures):
    """Recommendation request (matches /recommend endpoint)."""
    pass


class RecommendResponse(BaseModel):
    recommendations: list[str]
    model_source:    str


# Legacy request/response kept for backward compatibility
class AnalyzeProjectRequest(BaseModel):
    description: str = Field(..., min_length=5)
    budget:      float = Field(..., gt=0)
    sector:      str = Field(..., min_length=2)
    criteres:    list[dict] | None = None


class AnalyzeProjectResponse(BaseModel):
    predicted_esg_score: int
    carbon_risk:         str
    credibility_score:   int
    recommendations:     str


# ---------------------------------------------------------------------------
# Helpers
# ---------------------------------------------------------------------------

def _to_df(req: ProjectFeatures) -> pd.DataFrame:
    """Convert request to a single-row DataFrame matching training columns."""
    row: dict[str, Any] = {}
    for col in NUMERIC_FEATURES:
        val = getattr(req, col, None)
        row[col] = float(val) if val is not None else np.nan
    for col in CATEGORICAL_FEATURES:
        val = getattr(req, col, None)
        row[col] = str(val).strip() if val else np.nan
    return pd.DataFrame([row])


def _autoscale_energy(df: pd.DataFrame) -> pd.DataFrame:
    """If energy < 10 000 treat as MWh → convert to kWh (matches Symfony logic)."""
    col = "consommation_energie"
    mask = df[col].notna() & (df[col] > 0) & (df[col] < 10_000)
    df.loc[mask, col] = df.loc[mask, col] * 1_000
    return df


def _compute_carbon_metrics(df: pd.DataFrame) -> dict[str, float]:
    """Compute tCO2e values using Symfony emission factors."""
    energy    = float(df["consommation_energie"].fillna(0).iloc[0])
    transport = float(df["distance_transport"].fillna(0).iloc[0])
    material  = float(df["quantite_materiau"].fillna(0).iloc[0])
    waste     = float(df["dechets_generes"].fillna(0).iloc[0])
    declared  = float(df["emissions_estimees"].fillna(0).iloc[0])

    actual = (energy * 0.0005) + (transport * 0.0001) + (material * 2.0) + (waste * 0.5)
    baseline = (energy * 0.0006) + (transport * 0.00012) + (material * 0.0015) + (waste * 0.0009)
    avoided = max(0.0, baseline - actual)

    return {
        "actual_tco2":    actual,
        "baseline_tco2":  baseline,
        "avoided_tco2":   avoided,
        "declared_tco2":  declared,
        "energy":         energy,
        "transport":      transport,
        "material":       material,
        "waste":          waste,
    }


def _esg_fallback(total_tco2: float, block_count: int, quality: float = 70.0) -> dict:
    """Pure-Java-equivalent ESG formula."""
    score = 9.5 - (total_tco2 / 25.0) - (block_count * 0.6)
    score = max(0.0, min(10.0, score))
    credibility = max(0, min(100, int(quality - block_count * 10)))

    if total_tco2 >= 50:
        risk = "HIGH"
    elif total_tco2 >= 20:
        risk = "MEDIUM"
    else:
        risk = "LOW"

    if risk == "HIGH":
        decision = "REJECTED"
    elif score >= 7 and risk == "LOW":
        decision = "APPROVED"
    elif score >= 5:
        decision = "REVISION_REQUIRED"
    else:
        decision = "REJECTED"

    return {
        "predicted_esg_score": int(round(score)),
        "credibility_score":   credibility,
        "carbon_risk":         risk,
        "decision":            decision,
    }


def _fraud_fallback(cm: dict, block_count: int = 0, warn_count: int = 0) -> dict:
    """Pure-Java-equivalent fraud formula."""
    energy    = cm["energy"]
    transport = cm["transport"]
    material  = cm["material"]
    declared  = cm["declared_tco2"]
    computed  = cm["actual_tco2"]

    missing_numeric  = sum(1 for v in [energy, declared] if v == 0)
    missing_optional = sum(1 for v in [transport, material] if v == 0)

    dist_per_mat = transport / material if material > 0 else 0.0
    em_per_en    = declared / energy    if energy   > 0 else 0.0
    gap_base     = max(computed, declared, 1.0)
    carbon_gap   = abs(declared - computed) / gap_base

    risk = 0.0
    risk += min(0.35, block_count * 0.14)
    risk += min(0.12, warn_count  * 0.02)
    risk += min(0.10, missing_numeric  * 0.08)
    risk += min(0.06, missing_optional * 0.015)
    risk += min(0.10, carbon_gap * 0.10)

    if computed >= 80:   risk += 0.25
    elif computed >= 50: risk += 0.12

    if dist_per_mat > 14:   risk += 0.12
    elif dist_per_mat > 10: risk += 0.06

    if em_per_en > 0.06:   risk += 0.10
    elif em_per_en > 0.04: risk += 0.05

    risk = max(0.0, min(1.0, risk))
    anomaly = max(0.0, min(3.0, 1.05 * risk + block_count * 0.08))

    hard = (
        block_count >= 3
        or missing_numeric >= 2
        or dist_per_mat > 20
        or em_per_en > 0.08
        or computed >= 80
    )
    fraud_flag = hard or risk >= 0.65

    reasons: list[str] = []
    if missing_numeric >= 2:
        reasons.append("Données environnementales critiques manquantes")
    if carbon_gap > 0.35:
        reasons.append("Écart élevé entre émissions déclarées et calculées")
    if computed >= 50:
        reasons.append("Risque carbone élevé (total tCO2e)")
    if dist_per_mat > 8:
        reasons.append("Distance transport très élevée par rapport à la quantité de matériau")
    if block_count > 0:
        reasons.append("Des contrôles bloquants ont été détectés")

    return {
        "risk_score":    round(risk, 4),
        "anomaly_score": round(anomaly, 6),
        "fraud_flag":    fraud_flag,
        "reasons":       reasons,
    }


def _rec_fallback(cm: dict, esg_score: int, fraud_risk: float) -> list[str]:
    """Rule-based recommendations when trained model is unavailable."""
    labels: list[str] = []
    if cm["energy"] > 0:
        labels.append("optimize_energy")
    if cm["transport"] > 0:
        labels.append("optimize_transport")
    if cm["material"] > 0:
        labels.append("optimize_materials")
    if cm["waste"] > 0:
        labels.append("reduce_waste")
    if fraud_risk >= 0.4:
        labels.append("improve_data_quality")
        labels.append("audit_emissions")
    if esg_score < 7:
        labels.append("mitigation_plan")
    if esg_score < 5 or fraud_risk >= 0.55:
        labels.append("resubmission_pack")
    if not labels:
        labels.append("mitigation_plan")
    return labels


# ---------------------------------------------------------------------------
# Endpoints
# ---------------------------------------------------------------------------

@app.get("/health")
def health() -> dict:
    return {
        "status": "ok",
        "esg_model":   "trained" if _esg_pipeline   else "fallback",
        "fraud_model": "trained" if _fraud_pipeline  else "fallback",
        "rec_model":   "trained" if _rec_artifact    else "fallback",
    }


@app.get("/model-info")
def model_info() -> dict:
    info: dict[str, Any] = {
        "version": "2.0.0",
        "esg_model_loaded":   _esg_pipeline   is not None,
        "fraud_model_loaded": _fraud_pipeline  is not None,
        "rec_model_loaded":   _rec_artifact    is not None,
    }
    if _esg_profile:
        info["esg_reference_mae"] = _esg_profile.get("reference_mae")
    if _fraud_profile:
        info["fraud_contamination"] = _fraud_profile.get("contamination")
        info["fraud_threshold_q95"] = _fraud_profile.get("anomaly_score_quantiles", {}).get("q95")
    return info


@app.post("/predict", response_model=PredictResponse)
def predict(req: PredictRequest) -> PredictResponse:
    """ESG score prediction — uses trained model when available, falls back to formula."""
    df = _to_df(req)
    df = _autoscale_energy(df)
    cm = _compute_carbon_metrics(df)

    if _esg_pipeline is not None:
        try:
            x = df[NUMERIC_FEATURES + CATEGORICAL_FEATURES]
            raw_score = float(_esg_pipeline.predict(x)[0])
            esg_score = int(round(max(0.0, min(10.0, raw_score))))
            fb = _esg_fallback(cm["actual_tco2"], 0)
            credibility = fb["credibility_score"]
            carbon_risk = fb["carbon_risk"]
            decision    = fb["decision"]
            source = "trained"
        except Exception as exc:
            print(f"[ML] ESG trained model failed: {exc}, using fallback")
            fb = _esg_fallback(cm["actual_tco2"], 0)
            esg_score, credibility, carbon_risk, decision = (
                fb["predicted_esg_score"], fb["credibility_score"],
                fb["carbon_risk"], fb["decision"],
            )
            source = "fallback"
    else:
        fb = _esg_fallback(cm["actual_tco2"], 0)
        esg_score, credibility, carbon_risk, decision = (
            fb["predicted_esg_score"], fb["credibility_score"],
            fb["carbon_risk"], fb["decision"],
        )
        source = "fallback"

    rec_labels = _rec_fallback(cm, esg_score, 0.0)
    recommendations = " | ".join(rec_labels)

    return PredictResponse(
        predicted_esg_score=esg_score,
        credibility_score=credibility,
        carbon_risk=carbon_risk,
        decision=decision,
        recommendations=recommendations,
        model_source=source,
    )


@app.post("/predict/esg", response_model=PredictResponse)
def predict_esg(req: PredictRequest) -> PredictResponse:
    """Alias for /predict — ESG prediction only."""
    return predict(req)


@app.post("/predict/fraud", response_model=FraudResponse)
def predict_fraud(req: FraudRequest) -> FraudResponse:
    """Alias for /fraud/assess — fraud detection only."""
    return fraud_assess(req)


class BothResponse(BaseModel):
    esg:   PredictResponse
    fraud: FraudResponse


@app.post("/predict/both", response_model=BothResponse)
def predict_both(req: PredictRequest) -> BothResponse:
    """ESG + Fraud in one call — used by Java ExpertWorkflowService."""
    esg_result   = predict(req)
    fraud_result = fraud_assess(FraudRequest(**req.model_dump()))
    return BothResponse(esg=esg_result, fraud=fraud_result)


@app.post("/fraud/assess", response_model=FraudResponse)
def fraud_assess(req: FraudRequest) -> FraudResponse:
    """Fraud / anomaly detection — uses Isolation Forest when available."""
    df = _to_df(req)
    df = _autoscale_energy(df)
    cm = _compute_carbon_metrics(df)

    if _fraud_pipeline is not None:
        try:
            x = df[NUMERIC_FEATURES + CATEGORICAL_FEATURES]
            decision_score = float(_fraud_pipeline.decision_function(x)[0])
            anomaly_score  = float(-decision_score)
            pred           = int(_fraud_pipeline.predict(x)[0])

            # Calibrate risk_score to 0-1 using stored quantiles
            q95 = 1.0
            if _fraud_profile:
                q95 = _fraud_profile.get("anomaly_score_quantiles", {}).get("q95", 1.0) or 1.0
            risk_score = max(0.0, min(1.0, anomaly_score / q95))

            fraud_flag = pred == -1 or risk_score >= 0.65
            fb_reasons = _fraud_fallback(cm)["reasons"]
            source = "trained"

            return FraudResponse(
                risk_score=round(risk_score, 4),
                anomaly_score=round(anomaly_score, 6),
                fraud_flag=fraud_flag,
                reasons=fb_reasons,
                model_source=source,
            )
        except Exception as exc:
            print(f"[ML] Fraud trained model failed: {exc}, using fallback")

    # Fallback
    fb = _fraud_fallback(cm)
    return FraudResponse(**fb, model_source="fallback")


@app.post("/recommend", response_model=RecommendResponse)
def recommend(req: RecommendRequest) -> RecommendResponse:
    """Multi-label recommendation — uses trained classifier when available."""
    df = _to_df(req)
    df = _autoscale_energy(df)
    cm = _compute_carbon_metrics(df)

    if _rec_artifact is not None:
        try:
            model  = _rec_artifact["model"]
            labels = _rec_artifact.get("labels", RECOMMENDATION_LABELS)
            x = df[NUMERIC_FEATURES + CATEGORICAL_FEATURES]
            pred = model.predict(x)[0]  # binary array
            active = [labels[i] for i, v in enumerate(pred) if v == 1]
            if not active:
                active = ["mitigation_plan"]
            return RecommendResponse(recommendations=active, model_source="trained")
        except Exception as exc:
            print(f"[ML] Recommendation trained model failed: {exc}, using fallback")

    # Fallback — need ESG score for rule-based recs
    fb_esg = _esg_fallback(cm["actual_tco2"], 0)
    fb_fraud = _fraud_fallback(cm)
    active = _rec_fallback(cm, fb_esg["predicted_esg_score"], fb_fraud["risk_score"])
    return RecommendResponse(recommendations=active, model_source="fallback")


# ---------------------------------------------------------------------------
# Legacy endpoint — kept for backward compatibility
# ---------------------------------------------------------------------------

@app.post("/analyze-project", response_model=AnalyzeProjectResponse)
def analyze_project(payload: AnalyzeProjectRequest) -> AnalyzeProjectResponse:
    if model_bundle is None:
        raise HTTPException(status_code=503, detail="Model not ready")

    esg_score, credibility = model_bundle.predict(
        description=payload.description,
        budget=payload.budget,
        sector=payload.sector,
        criteres=payload.criteres or [],
    )
    carbon_risk    = model_bundle.estimate_risk(esg_score, payload.budget)
    recommendations = model_bundle.recommend(esg_score, payload.description)

    return AnalyzeProjectResponse(
        predicted_esg_score=esg_score,
        carbon_risk=carbon_risk,
        credibility_score=credibility,
        recommendations=recommendations,
    )
