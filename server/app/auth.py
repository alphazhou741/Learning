from collections import defaultdict, deque
from datetime import datetime, timedelta, timezone
import re
import secrets
from threading import Lock
import time

import jwt
from argon2 import PasswordHasher
from argon2.exceptions import VerifyMismatchError
from fastapi import Depends, HTTPException, Request, status
from fastapi.security import HTTPAuthorizationCredentials, HTTPBearer
from sqlalchemy.orm import Session

from .config import get_jwt_expire_days, get_jwt_secret
from .db import get_db
from .models import User

_hasher = PasswordHasher()
_bearer = HTTPBearer(auto_error=False)
_RECOVERY_ALPHABET = "23456789ABCDEFGHJKMNPQRSTUVWXYZ"
_PASSWORD_ALPHABET = "23456789abcdefghjkmnpqrstuvwxyzABCDEFGHJKMNPQRSTUVWXYZ"


def now_ms() -> int:
    return int(time.time() * 1000)


def hash_password(password: str) -> str:
    return _hasher.hash(password)


def verify_password(password: str, password_hash: str) -> bool:
    try:
        return _hasher.verify(password_hash, password)
    except (VerifyMismatchError, Exception):
        return False


def normalize_recovery_code(raw_code: str) -> str:
    return re.sub(r"[^A-Z0-9]", "", (raw_code or "").upper())


def generate_recovery_code() -> str:
    groups = [
        "".join(secrets.choice(_RECOVERY_ALPHABET) for _ in range(4))
        for _ in range(3)
    ]
    return "-".join(groups)


def hash_recovery_code(raw_code: str) -> str:
    return _hasher.hash(normalize_recovery_code(raw_code))


def verify_recovery_code(raw_code: str, stored_hash: str | None) -> bool:
    normalized = normalize_recovery_code(raw_code)
    if not normalized or not stored_hash:
        return False
    return verify_password(normalized, stored_hash)


def generate_temp_password(length: int = 10) -> str:
    return "".join(secrets.choice(_PASSWORD_ALPHABET) for _ in range(max(8, length)))


def client_ip_from_request(request: Request) -> str:
    forwarded = request.headers.get("x-forwarded-for", "").split(",")[0].strip()
    if forwarded:
        return forwarded
    return request.client.host if request.client else "unknown"


class SlidingWindowLimiter:
    def __init__(self, max_failures: int = 10, window_seconds: int = 300) -> None:
        self.max_failures = max_failures
        self.window_seconds = window_seconds
        self._buckets: dict[str, deque[float]] = defaultdict(deque)
        self._lock = Lock()

    def _prune(self, dq: deque[float], now: float) -> None:
        cutoff = now - self.window_seconds
        while dq and dq[0] < cutoff:
            dq.popleft()

    def ensure_allowed(self, key: str) -> None:
        now = time.monotonic()
        with self._lock:
            dq = self._buckets[key]
            self._prune(dq, now)
            if len(dq) >= self.max_failures:
                raise HTTPException(
                    status_code=status.HTTP_429_TOO_MANY_REQUESTS,
                    detail="尝试次数过多，请 5 分钟后再试。",
                )

    def record_failure(self, key: str) -> None:
        now = time.monotonic()
        with self._lock:
            dq = self._buckets[key]
            self._prune(dq, now)
            dq.append(now)

    def clear(self, key: str) -> None:
        with self._lock:
            self._buckets.pop(key, None)


auth_rate_limiter = SlidingWindowLimiter(max_failures=10, window_seconds=300)


def create_access_token(
    user_id: str,
    username: str,
    token_version: int = 1,
    is_admin: bool = False,
) -> str:
    now = datetime.now(timezone.utc)
    exp = now + timedelta(days=get_jwt_expire_days())
    payload = {
        "sub": user_id,
        "username": username,
        "tv": token_version,
        "adm": is_admin,
        "iat": int(now.timestamp()),
        "exp": int(exp.timestamp()),
    }
    return jwt.encode(payload, get_jwt_secret(), algorithm="HS256")


def get_current_user(
    credentials: HTTPAuthorizationCredentials | None = Depends(_bearer),
    db: Session = Depends(get_db),
) -> User:
    if credentials is None or not credentials.credentials:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="未登录或凭证已失效，请重新登录。",
        )
    token = credentials.credentials
    try:
        payload = jwt.decode(token, get_jwt_secret(), algorithms=["HS256"])
        user_id = payload.get("sub")
        if not user_id:
            raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail="无效登录凭证")
    except jwt.ExpiredSignatureError:
        raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail="登录状态已过期，请重新登录。")
    except jwt.InvalidTokenError:
        raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail="登录凭证校验失败，请重新登录。")

    user = db.get(User, user_id)
    if user is None:
        raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail="账号不存在或已注销")
    if user.is_disabled:
        raise HTTPException(status_code=status.HTTP_403_FORBIDDEN, detail="该账号已被管理员停用")
    token_ver = int(payload.get("tv", 1))
    if token_ver != user.token_version:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="密码或账号安全状态已变更，请重新登录。",
        )
    return user


def get_current_admin(current_user: User = Depends(get_current_user)) -> User:
    if not current_user.is_admin:
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="需要管理员权限才可访问后台看板。",
        )
    return current_user
