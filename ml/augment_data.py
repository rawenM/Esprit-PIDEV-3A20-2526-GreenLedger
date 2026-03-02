import argparse
import os
import numpy as np
import pandas as pd


def augment(df: pd.DataFrame, noise: float, flip: float, seed: int) -> pd.DataFrame:
    rng = np.random.default_rng(seed)
    out = df.copy()

    numeric_cols = [
        "baseline_emissions_tco2", "target_reduction_pct", "avg_note", "min_note",
        "compliance_rate", "scope1_tco2", "scope2_tco2", "scope3_tco2",
        "total_tco2", "scenario_delta", "score"
    ]

    # Add gaussian noise to numeric columns
    for col in numeric_cols:
        sigma = out[col].std() * noise
        out[col] = out[col] + rng.normal(0, sigma, len(out))

    # Clip known bounds
    out["avg_note"] = out["avg_note"].clip(1, 10)
    out["min_note"] = out["min_note"].clip(1, 10)
    out["compliance_rate"] = out["compliance_rate"].clip(0, 1)
    out["target_reduction_pct"] = out["target_reduction_pct"].clip(0, 100)

    # Random label flips (simulate human error)
    flip_mask = rng.random(len(out)) < flip
    out.loc[flip_mask, "decision"] = out.loc[flip_mask, "decision"].map({
        "APPROVE": "REJECT",
        "REJECT": "APPROVE",
        "REVIEW": "REJECT",
    }).fillna(out.loc[flip_mask, "decision"])

    return out


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--in", dest="input_path", required=True)
    parser.add_argument("--out", dest="output_path", required=True)
    parser.add_argument("--noise", type=float, default=0.05)
    parser.add_argument("--flip", type=float, default=0.02)
    parser.add_argument("--seed", type=int, default=42)
    args = parser.parse_args()

    df = pd.read_csv(args.input_path)
    out = augment(df, args.noise, args.flip, args.seed)

    os.makedirs(os.path.dirname(args.output_path), exist_ok=True)
    out.to_csv(args.output_path, index=False)
    print(f"Augmented {len(out)} rows -> {args.output_path}")


if __name__ == "__main__":
    main()

