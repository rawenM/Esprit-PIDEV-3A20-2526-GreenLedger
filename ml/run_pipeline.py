import argparse
import os
import subprocess


def run(cmd):
    print(" ".join(cmd))
    subprocess.check_call(cmd)


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--rows", type=int, default=100000)
    args = parser.parse_args()

    os.makedirs("data", exist_ok=True)
    os.makedirs("models", exist_ok=True)
    os.makedirs("reports", exist_ok=True)

    run(["python", "ml/generate_data.py", "--rows", str(args.rows), "--out", "data/synthetic.csv"])
    run(["python", "ml/augment_data.py", "--in", "data/synthetic.csv", "--out", "data/augmented.csv", "--noise", "0.05", "--flip", "0.02"])
    run(["python", "ml/train_model.py", "--in", "data/augmented.csv", "--model-out", "models/carbon_model.joblib", "--report-out", "reports/metrics.json"])


if __name__ == "__main__":
    main()

