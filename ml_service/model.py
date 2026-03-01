from __future__ import annotations

from dataclasses import dataclass
from typing import Iterable

import math
import random

import numpy as np
import pandas as pd
from sklearn.base import BaseEstimator, TransformerMixin
from sklearn.compose import ColumnTransformer
from sklearn.linear_model import Ridge
from sklearn.pipeline import Pipeline
from sklearn.preprocessing import FunctionTransformer, OneHotEncoder, StandardScaler
from sklearn.feature_extraction.text import TfidfVectorizer


def _try_import_nltk():
    try:
        import nltk
        return nltk
    except Exception:
        return None


class NltkFeatureExtractor(BaseEstimator, TransformerMixin):
    def __init__(self, keywords: Iterable[str]):
        self.keywords = tuple(keywords)
        self.nltk = None

    def fit(self, X, y=None):
        self.nltk = _try_import_nltk()
        if self.nltk is not None:
            try:
                self.nltk.data.find("tokenizers/punkt")
            except Exception:
                try:
                    self.nltk.download("punkt", quiet=True)
                except Exception:
                    self.nltk = None
        return self

    def transform(self, X):
        keyword_set = {str(k).lower() for k in self.keywords}
        values = []
        for text in X:
            tokens = self._tokenize(str(text))
            count = len(tokens)
            unique = len(set(tokens))
            hits = sum(1 for t in tokens if t in keyword_set)
            ratio = hits / max(1, count)
            unique_ratio = unique / max(1, count)
            values.append([count, ratio, unique_ratio])
        return np.array(values, dtype=float)

    def _tokenize(self, text: str) -> list[str]:
        clean = text.lower()
        if self.nltk is None:
            return [t for t in clean.split() if t]
        try:
            return [t for t in self.nltk.word_tokenize(clean) if t.isalnum()]
        except Exception:
            return [t for t in clean.split() if t]


@dataclass
class ModelBundle:
    esg_model: Pipeline
    credibility_model: Pipeline

    def predict(self, description: str, budget: float, sector: str, criteres: list[dict]) -> tuple[int, int]:
        stats = _critere_stats(criteres)
        df = pd.DataFrame([{
            "description": description,
            "budget": float(budget),
            "sector": sector,
            "avg_note": stats["avg_note"],
            "min_note": stats["min_note"],
            "max_note": stats["max_note"],
            "compliance_rate": stats["compliance_rate"],
            "criteria_count": stats["criteria_count"],
        }])
        esg_pred = float(self.esg_model.predict(df)[0])
        cred_pred = float(self.credibility_model.predict(df)[0])
        esg_score = int(round(_clip(esg_pred, 0, 100)))
        credibility = int(round(_clip(cred_pred, 0, 100)))
        return esg_score, credibility

    def estimate_risk(self, esg_score: int, budget: float) -> str:
        if esg_score >= 75 and budget <= 500000:
            return "Low"
        if esg_score >= 60:
            return "Medium"
        return "High"

    def recommend(self, esg_score: int, description: str) -> str:
        desc = description.lower()
        if esg_score < 55:
            return "Improve reporting on CO2 reduction metrics and formalize an ESG action plan."
        if "scope" not in desc and "emission" not in desc:
            return "Add scope 1/2/3 emissions tracking and publish reduction targets."
        if "audit" not in desc and "report" not in desc:
            return "Introduce third-party ESG audits and publish periodic progress reports."
        return "Maintain transparency and track reductions against declared ESG targets."


def build_model_bundle(seed: int = 42) -> ModelBundle:
    rng = random.Random(seed)
    sectors = ["Energy", "Transport", "Manufacturing", "Agriculture", "Tech", "Construction"]
    keywords_good = ["renewable", "efficiency", "audit", "report", "scope", "co2", "offset"]
    keywords_bad = ["diesel", "coal", "waste", "leak", "emission", "spill"]

    rows = []
    for _ in range(800):
        sector = rng.choice(sectors)
        budget = rng.uniform(50000, 1200000)
        good = rng.random() > 0.35
        words = keywords_good if good else keywords_bad
        desc = _make_description(rng, sector, words)
        criteria_count = rng.randint(4, 9)
        crit_notes = [rng.randint(3, 10) if good else rng.randint(1, 7) for _ in range(criteria_count)]
        crit_respected = [rng.random() > (0.2 if good else 0.5) for _ in range(criteria_count)]
        stats = _critere_stats([
            {"note": n, "respect": r}
            for n, r in zip(crit_notes, crit_respected)
        ])
        base = 70 if good else 45
        sector_adj = 5 if sector in {"Energy", "Tech"} else -2
        budget_adj = -5 if budget > 800000 else 5
        score = base + sector_adj + budget_adj + (stats["avg_note"] - 6) * 3 + rng.uniform(-6, 6)
        credibility = base + (8 if "audit" in desc else -3) + (stats["compliance_rate"] - 0.6) * 20 + rng.uniform(-5, 5)
        rows.append({
            "description": desc,
            "budget": budget,
            "sector": sector,
            "avg_note": stats["avg_note"],
            "min_note": stats["min_note"],
            "max_note": stats["max_note"],
            "compliance_rate": stats["compliance_rate"],
            "criteria_count": stats["criteria_count"],
            "esg_score": _clip(score, 0, 100),
            "credibility": _clip(credibility, 0, 100),
        })

    df = pd.DataFrame(rows)

    transformer = ColumnTransformer(
        transformers=[
            (
                "text_tfidf",
                TfidfVectorizer(max_features=300, ngram_range=(1, 2)),
                "description",
            ),
            (
                "text_nltk",
                Pipeline([
                    ("flatten", FunctionTransformer(lambda x: x.ravel(), validate=False)),
                    ("nltk", NltkFeatureExtractor(keywords_good + keywords_bad)),
                ]),
                "description",
            ),
            (
                "num",
                Pipeline([
                    ("log_budget", FunctionTransformer(lambda x: np.log1p(x), validate=False)),
                    ("scale", StandardScaler()),
                ]),
                ["budget", "avg_note", "min_note", "max_note", "compliance_rate", "criteria_count"],
            ),
            (
                "sector",
                OneHotEncoder(handle_unknown="ignore"),
                ["sector"],
            ),
        ],
        remainder="drop",
    )

    esg_model = Pipeline([
        ("features", transformer),
        ("reg", Ridge(alpha=1.2)),
    ])
    credibility_model = Pipeline([
        ("features", transformer),
        ("reg", Ridge(alpha=1.0)),
    ])

    esg_model.fit(df[["description", "budget", "sector", "avg_note", "min_note", "max_note", "compliance_rate", "criteria_count"]], df["esg_score"])
    credibility_model.fit(df[["description", "budget", "sector", "avg_note", "min_note", "max_note", "compliance_rate", "criteria_count"]], df["credibility"])

    return ModelBundle(esg_model=esg_model, credibility_model=credibility_model)


def _make_description(rng: random.Random, sector: str, keywords: list[str]) -> str:
    verbs = ["improves", "reduces", "enhances", "plans", "tracks", "monitors"]
    nouns = ["emissions", "energy", "waste", "efficiency", "reporting", "compliance"]
    parts = [sector, rng.choice(verbs), rng.choice(nouns)]
    parts.extend(rng.sample(keywords, k=min(3, len(keywords))))
    return " ".join(parts)


def _clip(value: float, low: float, high: float) -> float:
    return max(low, min(high, value))


def _critere_stats(criteres: list[dict]) -> dict:
    if not criteres:
        return {
            "avg_note": 6.0,
            "min_note": 6.0,
            "max_note": 6.0,
            "compliance_rate": 0.7,
            "criteria_count": 0,
        }
    notes = [float(c.get("note", 0)) for c in criteres if c.get("note") is not None]
    respects = [bool(c.get("respect", c.get("respected", True))) for c in criteres]
    if not notes:
        notes = [0.0]
    avg_note = sum(notes) / len(notes)
    min_note = min(notes)
    max_note = max(notes)
    compliance_rate = sum(1 for r in respects if r) / max(1, len(respects))
    return {
        "avg_note": avg_note,
        "min_note": min_note,
        "max_note": max_note,
        "compliance_rate": compliance_rate,
        "criteria_count": len(notes),
    }
