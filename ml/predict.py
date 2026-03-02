import argparse
import json
import sys
import joblib
import pandas as pd


def load_input():
    raw = sys.stdin.read().strip()
    if not raw:
        return None
    data = json.loads(raw)
    if isinstance(data, dict):
        return [data]
    return data


def build_recommendation(row: dict, pred: str, confidence: float) -> dict:
    actions = []
    avg_note = float(row.get("avg_note", 0))
    min_note = float(row.get("min_note", 0))
    compliance_rate = float(row.get("compliance_rate", 0))
    total_tco2 = float(row.get("total_tco2", 0))
    target_pct = float(row.get("target_reduction_pct", 0))

    if compliance_rate < 0.75:
        actions.append("Renforcer la conformité des critères critiques (>= 75%).")
    if min_note < 5:
        actions.append("Corriger les critères faibles (note < 5) avec un plan d'action ciblé.")
    if avg_note < 6.5:
        actions.append("Améliorer la performance moyenne des critères (objectif >= 6.5).")
    if total_tco2 > 4000:
        actions.append("Réduire les émissions totales (optimisation procédés/énergie).")
    if target_pct < 15:
        actions.append("Augmenter l'objectif de réduction (>= 15%).")

    if not actions:
        actions.append("Maintenir les bonnes pratiques et documenter les preuves d'impact.")

    summary = "Décision ML: {} (confiance {:.2f})".format(pred, confidence)
    return {"summary": summary, "actions": actions}


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--model", required=True)
    args = parser.parse_args()

    rows = load_input()
    if rows is None:
        print(json.dumps({"error": "empty input"}))
        return

    model = joblib.load(args.model)
    df = pd.DataFrame(rows)

    preds = model.predict(df)
    proba = None
    if hasattr(model, "predict_proba"):
        proba = model.predict_proba(df).max(axis=1).tolist()
    else:
        proba = [0.0 for _ in range(len(preds))]

    # Attach recommendations
    recommendations = []
    for i, row in enumerate(rows):
        recommendations.append(build_recommendation(row, preds[i], proba[i]))

    out = {
        "predictions": preds.tolist(),
        "confidence": proba,
        "recommendations": recommendations
    }
    print(json.dumps(out))


if __name__ == "__main__":
    main()
