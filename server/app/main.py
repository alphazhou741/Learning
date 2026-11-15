from contextlib import asynccontextmanager
import uuid

from fastapi import Depends, FastAPI, HTTPException, Request, status
from sqlalchemy import delete, inspect, select, text
from sqlalchemy.orm import Session

from .admin_routes import record_audit, router as admin_router
from .auth import (
    auth_rate_limiter,
    client_ip_from_request,
    create_access_token,
    generate_recovery_code,
    get_current_user,
    hash_password,
    hash_recovery_code,
    now_ms,
    verify_password,
    verify_recovery_code,
)
from .config import get_admin_password, get_admin_username
from .db import Base, SessionLocal, engine, get_db
from .models import ChildProfile, QuizEventRow, StudyLogRow, TopicRecordRow, User
from .schemas import (
    AuthResponse,
    ChangePasswordRequest,
    ChangePasswordResponse,
    ChildPatchDto,
    ChildProfileDto,
    ChildSnapshotDto,
    CreateChildRequest,
    DayStudyLogDto,
    LoginRequest,
    MeResponse,
    RecoveryCodeResponse,
    RegisterRequest,
    ResetByRecoveryRequest,
    ResetByRecoveryResponse,
    ResetChildRequest,
    RotateRecoveryRequest,
    SyncRequest,
    SyncResponse,
    TopicRecordDto,
    UserDto,
)

ALLOWED_STATUSES = {"UNKNOWN", "MASTERED", "SHAKY", "WEAK"}
ALLOWED_AUDIO_MODES = {"bundled_tts", "tts_only"}
ALLOWED_TERMS = {"上", "下", "全"}
ALLOWED_TONES = {"amber", "blue", "green", "rose"}


def _ensure_schema_columns() -> None:
    inspector = inspect(engine)
    if not inspector.has_table("users"):
        return
    existing_cols = {col["name"] for col in inspector.get_columns("users")}
    ddl_statements: list[str] = []
    if "is_admin" not in existing_cols:
        ddl_statements.append("ALTER TABLE users ADD COLUMN is_admin BOOLEAN NOT NULL DEFAULT FALSE")
    if "is_disabled" not in existing_cols:
        ddl_statements.append("ALTER TABLE users ADD COLUMN is_disabled BOOLEAN NOT NULL DEFAULT FALSE")
    if "token_version" not in existing_cols:
        ddl_statements.append("ALTER TABLE users ADD COLUMN token_version INTEGER NOT NULL DEFAULT 1")
    if "recovery_code_hash" not in existing_cols:
        ddl_statements.append("ALTER TABLE users ADD COLUMN recovery_code_hash VARCHAR(255)")
    if "recovery_code_updated_at_ms" not in existing_cols:
        ddl_statements.append("ALTER TABLE users ADD COLUMN recovery_code_updated_at_ms BIGINT")
    if not ddl_statements:
        return
    with engine.begin() as conn:
        for stmt in ddl_statements:
            conn.execute(text(stmt))


def _bootstrap_admin() -> None:
    admin_password = get_admin_password()
    if not admin_password:
        return
    admin_username = get_admin_username()
    with SessionLocal() as db:
        existing = db.execute(select(User).where(User.username == admin_username)).scalar_one_or_none()
        ts = now_ms()
        if existing is None:
            recovery = generate_recovery_code()
            db.add(
                User(
                    id=str(uuid.uuid4()),
                    username=admin_username,
                    password_hash=hash_password(admin_password),
                    is_admin=True,
                    is_disabled=False,
                    token_version=1,
                    recovery_code_hash=hash_recovery_code(recovery),
                    recovery_code_updated_at_ms=ts,
                    created_at_ms=ts,
                )
            )
            db.commit()
        elif not existing.is_admin:
            existing.is_admin = True
            db.commit()


@asynccontextmanager
async def lifespan(_: FastAPI):
    Base.metadata.create_all(bind=engine)
    _ensure_schema_columns()
    _bootstrap_admin()
    yield


app = FastAPI(title="基础学习云同步与管理服务", version="1.1.0", lifespan=lifespan)
app.include_router(admin_router)


def _user_to_dto(u: User) -> UserDto:
    return UserDto(id=u.id, username=u.username, isAdmin=u.is_admin)


def _child_to_dto(c: ChildProfile) -> ChildProfileDto:
    return ChildProfileDto(
        id=c.id,
        name=c.name,
        grade=c.grade,
        avatarTone=c.avatar_tone,
        defaultTerm=c.default_term,
        masteryStreak=c.mastery_streak,
        englishAudioMode=c.en_audio_mode,
        speechRate=c.speech_rate,
        resetVersion=c.reset_version,
        updatedAtMs=c.updated_at_ms,
    )


def _load_owned_child(db: Session, user_id: str, child_id: str) -> ChildProfile:
    child = db.get(ChildProfile, child_id)
    if child is None or child.user_id != user_id:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="孩子档案不存在或无访问权限")
    return child


def _list_children_dto(db: Session, user_id: str) -> list[ChildProfileDto]:
    rows = (
        db.execute(
            select(ChildProfile)
            .where(ChildProfile.user_id == user_id)
            .order_by(ChildProfile.created_at_ms.asc())
        )
        .scalars()
        .all()
    )
    return [_child_to_dto(r) for r in rows]


def _build_snapshot(db: Session, child: ChildProfile) -> ChildSnapshotDto:
    rec_rows = (
        db.execute(select(TopicRecordRow).where(TopicRecordRow.child_id == child.id))
        .scalars()
        .all()
    )
    log_rows = (
        db.execute(
            select(StudyLogRow)
            .where(StudyLogRow.child_id == child.id)
            .order_by(StudyLogRow.study_date.asc())
        )
        .scalars()
        .all()
    )
    return ChildSnapshotDto(
        child=_child_to_dto(child),
        records=[
            TopicRecordDto(
                topicId=r.topic_id,
                status=r.status,
                consecutiveCorrect=r.consecutive_correct,
                attempts=r.attempts,
                wrongCount=r.wrong_count,
                lastQuestionId=r.last_question_id,
                updatedAtMs=r.updated_at_ms,
            )
            for r in rec_rows
        ],
        studyLog=[
            DayStudyLogDto(
                studyDate=l.study_date,
                mathCount=l.math_count,
                chineseCount=l.chinese_count,
                englishCount=l.english_count,
                minutes=l.minutes,
                note=l.note,
                updatedAtMs=l.updated_at_ms,
            )
            for l in log_rows
        ],
        serverTimeMs=now_ms(),
    )


def _apply_child_patch(child: ChildProfile, patch: ChildPatchDto) -> None:
    if patch.updatedAtMs > 0 and patch.updatedAtMs < child.updated_at_ms:
        return
    if patch.name is not None and patch.name.strip():
        child.name = patch.name.strip()
    if patch.grade is not None and patch.grade.strip():
        child.grade = patch.grade.strip()
    if patch.avatarTone in ALLOWED_TONES:
        child.avatar_tone = patch.avatarTone
    if patch.defaultTerm in ALLOWED_TERMS:
        child.default_term = patch.defaultTerm
    if patch.masteryStreak is not None:
        child.mastery_streak = max(2, min(3, patch.masteryStreak))
    if patch.englishAudioMode in ALLOWED_AUDIO_MODES:
        child.en_audio_mode = patch.englishAudioMode
    if patch.speechRate is not None:
        child.speech_rate = max(0.75, min(1.25, float(patch.speechRate)))
    child.updated_at_ms = max(now_ms(), patch.updatedAtMs)


@app.get("/healthz")
def healthz(db: Session = Depends(get_db)):
    db.execute(text("SELECT 1"))
    return {"status": "ok", "db": "ok"}


@app.post("/api/v1/auth/register", response_model=AuthResponse, status_code=status.HTTP_201_CREATED)
def register(req: RegisterRequest, db: Session = Depends(get_db)):
    username = req.username.strip()
    if len(username) < 2:
        raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail="账号至少需要 2 个字符")

    existing = db.execute(select(User).where(User.username == username)).scalar_one_or_none()
    if existing is not None:
        raise HTTPException(status_code=status.HTTP_409_CONFLICT, detail="该账号已注册，请直接登录。")

    ts = now_ms()
    recovery_code = generate_recovery_code()
    user = User(
        id=str(uuid.uuid4()),
        username=username,
        password_hash=hash_password(req.password),
        is_admin=False,
        is_disabled=False,
        token_version=1,
        recovery_code_hash=hash_recovery_code(recovery_code),
        recovery_code_updated_at_ms=ts,
        created_at_ms=ts,
    )
    child = ChildProfile(
        id=str(uuid.uuid4()),
        user_id=user.id,
        name=req.initialChildName.strip() or "小宇",
        grade=req.initialGrade.strip() or "二年级",
        avatar_tone="amber",
        default_term="上",
        mastery_streak=2,
        en_audio_mode="bundled_tts",
        speech_rate=0.92,
        reset_version=0,
        updated_at_ms=ts,
        created_at_ms=ts,
    )
    db.add(user)
    db.add(child)
    db.commit()

    token = create_access_token(
        user.id,
        user.username,
        token_version=user.token_version,
        is_admin=False,
    )
    return AuthResponse(
        token=token,
        user=_user_to_dto(user),
        children=[_child_to_dto(child)],
        recoveryCode=recovery_code,
    )


@app.post("/api/v1/auth/login", response_model=AuthResponse)
def login(req: LoginRequest, request: Request, db: Session = Depends(get_db)):
    username = req.username.strip()
    limiter_key = f"login:{client_ip_from_request(request)}:{username}"
    auth_rate_limiter.ensure_allowed(limiter_key)

    user = db.execute(select(User).where(User.username == username)).scalar_one_or_none()
    if user is None or not verify_password(req.password, user.password_hash):
        auth_rate_limiter.record_failure(limiter_key)
        raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail="账号或密码错误")
    if user.is_disabled:
        raise HTTPException(status_code=status.HTTP_403_FORBIDDEN, detail="该账号已被管理员停用")

    auth_rate_limiter.clear(limiter_key)
    children = _list_children_dto(db, user.id)
    if not children:
        ts = now_ms()
        fallback = ChildProfile(
            id=str(uuid.uuid4()),
            user_id=user.id,
            name="小宇",
            grade="二年级",
            avatar_tone="amber",
            default_term="上",
            mastery_streak=2,
            en_audio_mode="bundled_tts",
            speech_rate=0.92,
            reset_version=0,
            updated_at_ms=ts,
            created_at_ms=ts,
        )
        db.add(fallback)
        db.commit()
        children = [_child_to_dto(fallback)]

    token = create_access_token(
        user.id,
        user.username,
        token_version=user.token_version,
        is_admin=user.is_admin,
    )
    return AuthResponse(
        token=token,
        user=_user_to_dto(user),
        children=children,
    )


@app.post("/api/v1/auth/reset-by-recovery", response_model=ResetByRecoveryResponse)
def reset_password_by_recovery(
    req: ResetByRecoveryRequest,
    request: Request,
    db: Session = Depends(get_db),
):
    username = req.username.strip()
    limiter_key = f"recovery:{client_ip_from_request(request)}:{username}"
    auth_rate_limiter.ensure_allowed(limiter_key)

    user = db.execute(select(User).where(User.username == username)).scalar_one_or_none()
    if user is None or not verify_recovery_code(req.recoveryCode, user.recovery_code_hash):
        auth_rate_limiter.record_failure(limiter_key)
        raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail="用户名或恢复码不正确")
    if user.is_disabled:
        raise HTTPException(status_code=status.HTTP_403_FORBIDDEN, detail="该账号已被停用，请联系管理员")

    auth_rate_limiter.clear(limiter_key)
    ts = now_ms()
    new_code = generate_recovery_code()
    user.password_hash = hash_password(req.newPassword)
    user.token_version += 1
    user.recovery_code_hash = hash_recovery_code(new_code)
    user.recovery_code_updated_at_ms = ts

    record_audit(
        db,
        actor_username=user.username,
        action="PASSWORD_RESET_BY_RECOVERY",
        target_type="user",
        target_id=user.id,
        summary=f"账号 {user.username} 通过恢复码自助重置了密码并轮换新恢复码",
    )
    db.commit()

    return ResetByRecoveryResponse(
        message="密码已重置，旧设备需使用新密码重新登录，请保存新的恢复码。",
        newRecoveryCode=new_code,
    )


@app.post("/api/v1/auth/change-password", response_model=ChangePasswordResponse)
def change_password(
    req: ChangePasswordRequest,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    if not verify_password(req.currentPassword, current_user.password_hash):
        raise HTTPException(status_code=status.HTTP_403_FORBIDDEN, detail="当前登录密码验证失败")

    current_user.password_hash = hash_password(req.newPassword)
    current_user.token_version += 1
    db.commit()
    db.refresh(current_user)

    token = create_access_token(
        current_user.id,
        current_user.username,
        token_version=current_user.token_version,
        is_admin=current_user.is_admin,
    )
    return ChangePasswordResponse(token=token, message="登录密码修改成功，其他设备需重新登录。")


@app.post("/api/v1/auth/rotate-recovery-code", response_model=RecoveryCodeResponse)
def rotate_recovery_code(
    req: RotateRecoveryRequest,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    if not verify_password(req.currentPassword, current_user.password_hash):
        raise HTTPException(status_code=status.HTTP_403_FORBIDDEN, detail="当前登录密码验证失败")

    code = generate_recovery_code()
    ts = now_ms()
    current_user.recovery_code_hash = hash_recovery_code(code)
    current_user.recovery_code_updated_at_ms = ts
    db.commit()
    return RecoveryCodeResponse(recoveryCode=code, updatedAtMs=ts)


@app.get("/api/v1/auth/me", response_model=MeResponse)
def me(current_user: User = Depends(get_current_user), db: Session = Depends(get_db)):
    return MeResponse(
        user=_user_to_dto(current_user),
        children=_list_children_dto(db, current_user.id),
    )


@app.get("/api/v1/children", response_model=list[ChildProfileDto])
def list_children(current_user: User = Depends(get_current_user), db: Session = Depends(get_db)):
    return _list_children_dto(db, current_user.id)


@app.post("/api/v1/children", response_model=ChildProfileDto, status_code=status.HTTP_201_CREATED)
def create_child(
    req: CreateChildRequest,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    ts = now_ms()
    child = ChildProfile(
        id=str(uuid.uuid4()),
        user_id=current_user.id,
        name=req.name.strip() or "新学习者",
        grade=req.grade.strip() or "二年级",
        avatar_tone=req.avatarTone if req.avatarTone in ALLOWED_TONES else "amber",
        default_term=req.defaultTerm if req.defaultTerm in ALLOWED_TERMS else "上",
        mastery_streak=2,
        en_audio_mode="bundled_tts",
        speech_rate=0.92,
        reset_version=0,
        updated_at_ms=ts,
        created_at_ms=ts,
    )
    db.add(child)
    db.commit()
    db.refresh(child)
    return _child_to_dto(child)


@app.patch("/api/v1/children/{child_id}", response_model=ChildProfileDto)
def patch_child(
    child_id: str,
    patch: ChildPatchDto,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    child = _load_owned_child(db, current_user.id, child_id)
    _apply_child_patch(child, patch)
    db.commit()
    db.refresh(child)
    return _child_to_dto(child)


@app.get("/api/v1/children/{child_id}/snapshot", response_model=ChildSnapshotDto)
def get_snapshot(
    child_id: str,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    child = _load_owned_child(db, current_user.id, child_id)
    return _build_snapshot(db, child)


@app.post("/api/v1/children/{child_id}/sync", response_model=SyncResponse)
def sync_child(
    child_id: str,
    payload: SyncRequest,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    child = _load_owned_child(db, current_user.id, child_id)

    if payload.clientResetVersion < child.reset_version:
        return SyncResponse(status="stale_reset", snapshot=_build_snapshot(db, child))

    if payload.profilePatch is not None:
        _apply_child_patch(child, payload.profilePatch)

    for rec in payload.records:
        safe_status = rec.status if rec.status in ALLOWED_STATUSES else "UNKNOWN"
        existing = db.get(TopicRecordRow, (child.id, rec.topicId))
        if existing is None:
            db.add(
                TopicRecordRow(
                    child_id=child.id,
                    topic_id=rec.topicId,
                    status=safe_status,
                    consecutive_correct=max(0, rec.consecutiveCorrect),
                    attempts=max(0, rec.attempts),
                    wrong_count=max(0, rec.wrongCount),
                    last_question_id=rec.lastQuestionId,
                    updated_at_ms=rec.updatedAtMs or now_ms(),
                )
            )
        elif (rec.updatedAtMs or 0) >= existing.updated_at_ms:
            existing.status = safe_status
            existing.consecutive_correct = max(0, rec.consecutiveCorrect)
            existing.attempts = max(0, rec.attempts)
            existing.wrong_count = max(0, rec.wrongCount)
            existing.last_question_id = rec.lastQuestionId
            existing.updated_at_ms = max(existing.updated_at_ms, rec.updatedAtMs or now_ms())

    seen_events: set[str] = set()
    for ev in payload.quizEvents:
        if not ev.eventId or ev.eventId in seen_events:
            continue
        seen_events.add(ev.eventId)
        already = db.get(QuizEventRow, ev.eventId)
        if already is not None:
            continue

        db.add(
            QuizEventRow(
                event_id=ev.eventId,
                child_id=child.id,
                subject=ev.subject,
                topic_id=ev.topicId,
                question_id=ev.questionId,
                is_correct=ev.isCorrect,
                is_skipped=ev.isSkipped,
                study_date=ev.studyDate,
                note=ev.note[:128],
                created_at_ms=ev.createdAtMs or now_ms(),
            )
        )

        day_row = db.get(StudyLogRow, (child.id, ev.studyDate))
        if day_row is None:
            day_row = StudyLogRow(
                child_id=child.id,
                study_date=ev.studyDate,
                math_count=0,
                chinese_count=0,
                english_count=0,
                minutes=0,
                note="",
                updated_at_ms=now_ms(),
            )
            db.add(day_row)
            db.flush()

        if ev.subject == "math":
            day_row.math_count += 1
        elif ev.subject == "chinese":
            day_row.chinese_count += 1
        elif ev.subject == "english":
            day_row.english_count += 1
        day_row.minutes += 1
        if ev.note.strip():
            day_row.note = ev.note.strip()[:128]
        day_row.updated_at_ms = max(day_row.updated_at_ms, now_ms())

    db.commit()
    db.refresh(child)
    return SyncResponse(status="ok", snapshot=_build_snapshot(db, child))


@app.post("/api/v1/children/{child_id}/reset", response_model=SyncResponse)
def reset_child(
    child_id: str,
    req: ResetChildRequest,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    if not verify_password(req.accountPassword, current_user.password_hash):
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="登录密码校验失败，无法重置云端学习数据。",
        )

    child = _load_owned_child(db, current_user.id, child_id)
    db.execute(delete(QuizEventRow).where(QuizEventRow.child_id == child.id))
    db.execute(delete(TopicRecordRow).where(TopicRecordRow.child_id == child.id))
    db.execute(delete(StudyLogRow).where(StudyLogRow.child_id == child.id))

    child.name = "小宇"
    child.grade = "二年级"
    child.avatar_tone = "amber"
    child.default_term = "上"
    child.mastery_streak = 2
    child.en_audio_mode = "bundled_tts"
    child.speech_rate = 0.92
    child.reset_version += 1
    child.updated_at_ms = now_ms()

    db.commit()
    db.refresh(child)
    return SyncResponse(status="ok", snapshot=_build_snapshot(db, child))
