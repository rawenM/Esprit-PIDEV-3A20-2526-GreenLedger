from fastapi.testclient import TestClient

from app import app


def main() -> None:
    payload = {
        "description": "Solar retrofit with renewable energy and audit reporting",
        "budget": 200000,
        "sector": "Energy",
        "criteres": [
            {"name": "Emissions", "note": 7, "respect": True},
            {"name": "Energie", "note": 5, "respect": False},
            {"name": "Conformite", "note": 6, "respect": True},
        ],
    }
    with TestClient(app) as client:
        response = client.post("/analyze-project", json=payload)
        print("Status:", response.status_code)
        print("Body:", response.json())


if __name__ == "__main__":
    main()
