import os
import tempfile

import pytest
from fastapi.testclient import TestClient


@pytest.fixture()
def client():
    with tempfile.NamedTemporaryFile(suffix=".db") as f:
        os.environ["DATABASE_URL"] = f"sqlite:///{f.name}"
        os.environ["JWT_SECRET"] = "test-secret-key-for-pytest-suite-1234567890"

        from app import db as db_mod
        from app import main as main_mod

        test_engine = db_mod.build_engine(os.environ["DATABASE_URL"])
        db_mod.engine = test_engine
        main_mod.engine = test_engine
        db_mod.SessionLocal.configure(bind=test_engine)
        db_mod.Base.metadata.create_all(bind=test_engine)

        with TestClient(main_mod.app) as c:
            yield c


def test_register_login_and_multi_child_sync(client: TestClient):
    r = client.post(
        "/api/v1/auth/register",
        json={
            "username": "parent_zhou",
            "password": "secret123",
            "initialChildName": "小宇",
            "initialGrade": "二年级",
        },
    )
    assert r.status_code == 201, r.text
    data = r.json()
    token = data["token"]
    child_id = data["children"][0]["id"]
    headers = {"Authorization": f"Bearer {token}"}

    # Health check
    assert client.get("/healthz").json() == {"status": "ok", "db": "ok"}

    # Login from "new device"
    login_res = client.post(
        "/api/v1/auth/login",
        json={"username": "parent_zhou", "password": "secret123"},
    )
    assert login_res.status_code == 200
    assert login_res.json()["children"][0]["id"] == child_id

    # Create second child
    c2 = client.post(
        "/api/v1/children",
        headers=headers,
        json={"name": "小朵", "grade": "二年级", "avatarTone": "green", "defaultTerm": "下"},
    )
    assert c2.status_code == 201
    children = client.get("/api/v1/children", headers=headers).json()
    assert len(children) == 2

    # Push sync payload with duplicate eventId -> studyLog increments once
    sync_payload = {
        "clientResetVersion": 0,
        "profilePatch": {"name": "小宇同学", "masteryStreak": 3, "updatedAtMs": 2000000000000},
        "records": [
            {
                "topicId": "cnm_p038",
                "status": "SHAKY",
                "consecutiveCorrect": 1,
                "attempts": 1,
                "wrongCount": 0,
                "lastQuestionId": "cnm_p038_q1",
                "updatedAtMs": 2000000000000,
            }
        ],
        "quizEvents": [
            {
                "eventId": "ev-001",
                "subject": "math",
                "topicId": "cnm_p038",
                "questionId": "cnm_p038_q1",
                "isCorrect": True,
                "isSkipped": False,
                "studyDate": "2026-11-14",
                "note": "乘法的初步认识",
                "createdAtMs": 2000000000000,
            },
            {
                "eventId": "ev-001",
                "subject": "math",
                "topicId": "cnm_p038",
                "questionId": "cnm_p038_q1",
                "isCorrect": True,
                "isSkipped": False,
                "studyDate": "2026-11-14",
                "note": "乘法的初步认识",
                "createdAtMs": 2000000000000,
            },
        ],
    }
    s1 = client.post(f"/api/v1/children/{child_id}/sync", headers=headers, json=sync_payload)
    assert s1.status_code == 200
    snap = s1.json()["snapshot"]
    assert snap["child"]["name"] == "小宇同学"
    assert snap["child"]["masteryStreak"] == 3
    assert len(snap["records"]) == 1
    assert snap["studyLog"][0]["mathCount"] == 1

    # Reset requires valid password
    bad_reset = client.post(
        f"/api/v1/children/{child_id}/reset",
        headers=headers,
        json={"accountPassword": "wrong_password"},
    )
    assert bad_reset.status_code == 403

    good_reset = client.post(
        f"/api/v1/children/{child_id}/reset",
        headers=headers,
        json={"accountPassword": "secret123"},
    )
    assert good_reset.status_code == 200
    reset_snap = good_reset.json()["snapshot"]
    assert reset_snap["child"]["resetVersion"] == 1
    assert reset_snap["records"] == []
    assert reset_snap["studyLog"] == []

    # Stale client (resetVersion 0) sync must return stale_reset and NOT resurrect old records
    stale = client.post(
        f"/api/v1/children/{child_id}/sync",
        headers=headers,
        json={
            "clientResetVersion": 0,
            "records": sync_payload["records"],
            "quizEvents": [
                {
                    "eventId": "ev-002",
                    "subject": "math",
                    "topicId": "cnm_p038",
                    "questionId": "cnm_p038_q2",
                    "isCorrect": True,
                    "isSkipped": False,
                    "studyDate": "2026-11-14",
                    "note": "stale",
                    "createdAtMs": 2000000001000,
                }
            ],
        },
    )
    assert stale.status_code == 200
    assert stale.json()["status"] == "stale_reset"
    assert stale.json()["snapshot"]["records"] == []
    assert stale.json()["snapshot"]["studyLog"] == []
