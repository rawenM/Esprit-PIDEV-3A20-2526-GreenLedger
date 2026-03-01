from __future__ import annotations

from fastapi import FastAPI, HTTPException
from pydantic import BaseModel, Field

from model import ModelBundle, build_model_bundle

app = FastAPI(title="Carbon Analysis Service", version="1.0.0")
model_bundle: ModelBundle | None = None


class AnalyzeProjectRequest(BaseModel):
    description: str = Field(..., min_length=5)
    budget: float = Field(..., gt=0)
    sector: str = Field(..., min_length=2)
    criteres: list[dict] | None = None


class AnalyzeProjectResponse(BaseModel):
    predicted_esg_score: int
    carbon_risk: str
    credibility_score: int
    recommendations: str


@app.on_event("startup")
def _startup() -> None:
    global model_bundle
    model_bundle = build_model_bundle()


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

    carbon_risk = model_bundle.estimate_risk(esg_score, payload.budget)
    recommendations = model_bundle.recommend(esg_score, payload.description)

    return AnalyzeProjectResponse(
        predicted_esg_score=esg_score,
        carbon_risk=carbon_risk,
        credibility_score=credibility,
        recommendations=recommendations,
    )
