import argparse
import os
import json
import mysql.connector
import pandas as pd


def get_conn(cfg):
    return mysql.connector.connect(
        host=cfg["host"],
        port=cfg["port"],
        user=cfg["user"],
        password=cfg["password"],
        database=cfg["database"],
    )


def load_config(args):
    return {
        "host": args.host,
        "port": args.port,
        "user": args.user,
        "password": args.password,
        "database": args.database,
    }


def extract(cfg):
    conn = get_conn(cfg)
    cur = conn.cursor(dictionary=True)

    # Base evaluation/project data
    cur.execute(
        """
        SELECT
            e.id_evaluation,
            e.id_projet,
            e.score_final,
            e.est_valide,
            p.budget,
            p.activityType
        FROM evaluation e
        JOIN projet p ON p.id = e.id_projet
        """
    )
    base = cur.fetchall()
    base_df = pd.DataFrame(base)

    # Criteria aggregation
    cur.execute(
        """
        SELECT
            er.id_evaluation,
            AVG(er.note) AS avg_note,
            MIN(er.note) AS min_note,
            AVG(er.est_respecte) AS compliance_rate
        FROM evaluation_resultat er
        GROUP BY er.id_evaluation
        """
    )
    agg = cur.fetchall()
    agg_df = pd.DataFrame(agg)

    cur.close()
    conn.close()

    df = base_df.merge(agg_df, on="id_evaluation", how="left")

    # Fill missing aggregate values
    df["avg_note"] = df["avg_note"].fillna(0.0)
    df["min_note"] = df["min_note"].fillna(0.0)
    df["compliance_rate"] = df["compliance_rate"].fillna(0.0)

    # Feature engineering to align with synthetic schema
    df["sector"] = df["activityType"].fillna("unknown")
    df["region"] = "UNK"
    df["size"] = "MEDIUM"
    df["baseline_emissions_tco2"] = (df["budget"].fillna(0) / 1000.0).clip(lower=0)
    df["target_reduction_pct"] = (df["avg_note"] / 10.0 * 30.0).clip(0, 100)

    # Minimal emissions breakdown (placeholders)
    df["scope1_tco2"] = df["baseline_emissions_tco2"] * 0.4
    df["scope2_tco2"] = df["baseline_emissions_tco2"] * 0.3
    df["scope3_tco2"] = df["baseline_emissions_tco2"] * 0.3
    df["total_tco2"] = df["scope1_tco2"] + df["scope2_tco2"] + df["scope3_tco2"]

    df["scenario_delta"] = 0.0
    df["blocking_criteria"] = 0

    # Label mapping
    df["decision"] = df["est_valide"].apply(lambda v: "APPROVE" if int(v) == 1 else "REJECT")
    df["risk_level"] = "MEDIUM"

    # Select final columns
    final_cols = [
        "sector", "region", "size",
        "baseline_emissions_tco2", "target_reduction_pct",
        "avg_note", "min_note", "compliance_rate", "blocking_criteria",
        "scope1_tco2", "scope2_tco2", "scope3_tco2", "total_tco2",
        "scenario_delta", "score_final", "risk_level", "decision"
    ]

    # Align column names with training script
    df = df.rename(columns={"score_final": "score"})

    return df[final_cols]


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--host", default=os.getenv("DB_HOST", "localhost"))
    parser.add_argument("--port", type=int, default=int(os.getenv("DB_PORT", "3306")))
    parser.add_argument("--user", default=os.getenv("DB_USER", "root"))
    parser.add_argument("--password", default=os.getenv("DB_PASSWORD", ""))
    parser.add_argument("--database", default=os.getenv("DB_NAME", "green_wallet"))
    parser.add_argument("--out", default="data/db_extract.csv")
    args = parser.parse_args()

    cfg = load_config(args)
    df = extract(cfg)

    os.makedirs(os.path.dirname(args.out), exist_ok=True)
    df.to_csv(args.out, index=False)
    print(f"Extracted {len(df)} rows -> {args.out}")


if __name__ == "__main__":
    main()

