import argparse
import json
import os

import joblib
import numpy as np
import pandas as pd
from sklearn.compose import ColumnTransformer
from sklearn.metrics import classification_report, accuracy_score
from sklearn.model_selection import train_test_split
from sklearn.pipeline import Pipeline
from sklearn.preprocessing import OneHotEncoder, StandardScaler
from sklearn.ensemble import RandomForestClassifier


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--in", dest="input_path", required=True)
    parser.add_argument("--model-out", dest="model_out", default="models/carbon_model.joblib")
    parser.add_argument("--report-out", dest="report_out", default="reports/metrics.json")
    parser.add_argument("--seed", type=int, default=42)
    args = parser.parse_args()

    df = pd.read_csv(args.input_path)

    target = df["decision"]
    features = df.drop(columns=["decision", "risk_level"])

    cat_cols = ["sector", "region", "size"]
    num_cols = [c for c in features.columns if c not in cat_cols]

    preprocessor = ColumnTransformer(
        transformers=[
            ("cat", OneHotEncoder(handle_unknown="ignore"), cat_cols),
            ("num", StandardScaler(), num_cols),
        ]
    )

    model = RandomForestClassifier(
        n_estimators=200,
        max_depth=12,
        random_state=args.seed,
        class_weight="balanced"
    )

    pipeline = Pipeline(steps=[
        ("prep", preprocessor),
        ("model", model)
    ])

    X_train, X_test, y_train, y_test = train_test_split(
        features, target, test_size=0.2, random_state=args.seed, stratify=target
    )

    pipeline.fit(X_train, y_train)
    preds = pipeline.predict(X_test)

    report = {
        "accuracy": float(accuracy_score(y_test, preds)),
        "classification_report": classification_report(y_test, preds, output_dict=True),
        "feature_columns": features.columns.tolist(),
        "categorical_columns": cat_cols,
        "numeric_columns": num_cols,
    }

    os.makedirs(os.path.dirname(args.model_out), exist_ok=True)
    joblib.dump(pipeline, args.model_out)

    os.makedirs(os.path.dirname(args.report_out), exist_ok=True)
    with open(args.report_out, "w", encoding="utf-8") as f:
        json.dump(report, f, indent=2)

    print(f"Model saved -> {args.model_out}")
    print(f"Metrics saved -> {args.report_out}")


if __name__ == "__main__":
    main()

