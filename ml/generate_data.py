import argparse
import json
import os
from dataclasses import dataclass
from typing import Dict

import numpy as np
import pandas as pd


@dataclass
class GenerationConfig:
    rows: int
    seed: int


def _risk_level(score: float) -> str:
    if score >= 7.5:
        return "LOW"
    if score >= 5.5:
        return "MEDIUM"
    if score >= 3.5:
        return "HIGH"
    return "CRITICAL"


def _decision(score: float, policy_block: int) -> str:
    if policy_block == 1:
        return "REJECT"
    if score >= 7.0:
        return "APPROVE"
    if score >= 5.0:
        return "REVIEW"
    return "REJECT"


def generate_dataset(cfg: GenerationConfig) -> pd.DataFrame:
    rng = np.random.default_rng(cfg.seed)

    # Project profile features
    sector = rng.choice(["energy", "industry", "building", "transport", "agri"], cfg.rows)
    region = rng.choice(["EU", "NA", "AF", "APAC", "MEA"], cfg.rows)
    size = rng.choice(["MICRO", "SMALL", "MEDIUM", "LARGE"], cfg.rows, p=[0.2, 0.35, 0.3, 0.15])

    baseline = rng.normal(5000, 1200, cfg.rows).clip(200, 20000)
    target_pct = rng.uniform(5, 50, cfg.rows)

    # Criteria aggregate features (simulate evaluation_resultat / critere_reference)
    avg_note = rng.normal(6.5, 1.4, cfg.rows).clip(1, 10)
    min_note = (avg_note - rng.uniform(0, 3, cfg.rows)).clip(1, 10)
    compliance_rate = rng.uniform(0.5, 1.0, cfg.rows)
    blocking_criteria = (rng.random(cfg.rows) < 0.08).astype(int)

    # Emissions metrics
    scope1 = rng.normal(1200, 300, cfg.rows).clip(50, 5000)
    scope2 = rng.normal(900, 250, cfg.rows).clip(50, 4000)
    scope3 = rng.normal(1500, 600, cfg.rows).clip(80, 8000)
    total = scope1 + scope2 + scope3

    # Scenario sensitivity feature
    scenario_delta = rng.normal(0, 0.6, cfg.rows)

    # Risk score (synthetic formula)
    score = (
        0.55 * avg_note +
        0.25 * (compliance_rate * 10) +
        0.15 * (1 - (total / total.max())) * 10 +
        0.05 * (target_pct / 10)
    )
    score = score.clip(0, 10)

    risk_level = np.vectorize(_risk_level)(score)
    decision = np.vectorize(_decision)(score, blocking_criteria)

    df = pd.DataFrame({
        "sector": sector,
        "region": region,
        "size": size,
        "baseline_emissions_tco2": baseline,
        "target_reduction_pct": target_pct,
        "avg_note": avg_note,
        "min_note": min_note,
        "compliance_rate": compliance_rate,
        "blocking_criteria": blocking_criteria,
        "scope1_tco2": scope1,
        "scope2_tco2": scope2,
        "scope3_tco2": scope3,
        "total_tco2": total,
        "scenario_delta": scenario_delta,
        "score": score,
        "risk_level": risk_level,
        "decision": decision,
    })
    return df


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--rows", type=int, default=5000)
    parser.add_argument("--seed", type=int, default=42)
    parser.add_argument("--out", type=str, default="data/synthetic.csv")
    parser.add_argument("--meta-out", type=str, default="data/synthetic_meta.json")
    args = parser.parse_args()

    cfg = GenerationConfig(rows=args.rows, seed=args.seed)
    df = generate_dataset(cfg)

    os.makedirs(os.path.dirname(args.out), exist_ok=True)
    df.to_csv(args.out, index=False)

    meta = {
        "rows": args.rows,
        "seed": args.seed,
        "columns": df.columns.tolist(),
    }
    os.makedirs(os.path.dirname(args.meta_out), exist_ok=True)
    with open(args.meta_out, "w", encoding="utf-8") as f:
        json.dump(meta, f, indent=2)

    print(f"Generated {len(df)} rows -> {args.out}")


if __name__ == "__main__":
    main()

