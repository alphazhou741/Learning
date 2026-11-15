import os


def get_database_url() -> str:
    return os.getenv(
        "DATABASE_URL",
        "sqlite:///./study_dev.db",
    )


def get_jwt_secret() -> str:
    return os.getenv(
        "JWT_SECRET",
        "dev-only-insecure-secret-change-in-production",
    )


def get_jwt_expire_days() -> int:
    try:
        return max(1, int(os.getenv("JWT_EXPIRE_DAYS", "90")))
    except ValueError:
        return 90


def get_admin_username() -> str:
    return (os.getenv("ADMIN_USERNAME", "admin") or "admin").strip() or "admin"


def get_admin_password() -> str:
    return os.getenv("ADMIN_PASSWORD", "").strip()
