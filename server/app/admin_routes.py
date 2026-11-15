from datetime import date, timedelta
from pathlib import Path
import uuid

from fastapi import APIRouter, Depends, HTTPException, Query, Request, status
from fastapi.responses import FileResponse
from sqlalchemy import delete, func, or_, select
from sqlalchemy.orm import Session

from .auth import (
    auth_rate_limiter,
    client_ip_from_request,
    create_access_token,
    generate_recovery_code,
    generate_temp_password,
    get_current_admin,
    hash_password,
    hash_recovery_code,
    now_ms,
    verify_password,
)
from .curriculum import load_topic_meta_map
from .db import get_db
from .models import AdminAuditLog, ChildProfile, QuizEventRow, StudyLogRow, TopicRecordRow, User
from .schemas import (
    AdminAuditLogDto,
    AdminChildDetailDto,
    AdminChildPatchRequest,
    AdminChildSummaryDto,
    AdminCreateUserRequest,
    AdminCreateUserResponse,
    AdminDailyTrendDto,
    AdminGradeBucketDto,
    AdminLoginResponse,
    AdminOverviewResponse,
    AdminPatchUserRequest,
    AdminQuizEventDetailDto,
    AdminResetPasswordRequest,
    AdminResetPasswordResponse,
    AdminTopicRecordDetailDto,
    AdminUserDetailResponse,
    AdminUserSummaryDto,
    AdminUsersPageResponse,
    ChangePasswordRequest,
    ChangePasswordResponse,
    DayStudyLogDto,
    LoginRequest,
    RecoveryCodeResponse,
    UserDto,
)

ALLOWED_AUDIO_MODES = {"bundled_tts", "tts_only"}
ALLOWED_TERMS = {"上", "下", "全"}
ALLOWED_TONES = {"amber", "blue", "green", "rose"}
ALLOWED_GRADES = {"一年级", "二年级", "三年级"}

router = APIRouter()
_ADMIN_HTML_PATH = Path(__file__).resolve().parent / "static" / "admin.html"


def record_audit(
    db: Session,
    actor_username: str,
    action: str,
    target_type: str,
    target_id: str,
    summary: str,
) -> None:
    db.add(
        AdminAuditLog(
            id=str(uuid.uuid4()),
            actor_username=actor_username,
            action=action,
            target_type=target_type,
            target_id=target_id,
            summary=summary,
            created_at_ms=now_ms(),
        )
    )


def _audit_to_dto(row: AdminAuditLog) -> AdminAuditLogDto:
    return AdminAuditLogDto(
        id=row.id,
        actorUsername=row.actor_username,
        action=row.action,
        targetType=row.target_type,
        targetId=row.target_id,
        summary=row.summary,
        createdAtMs=row.created_at_ms,
    )


def _summarize_child(db: Session, child: ChildProfile) -> AdminChildSummaryDto:
    rec_rows = (
        db.execute(select(TopicRecordRow).where(TopicRecordRow.child_id == child.id))
        .scalars()
        .all()
    )
    mastered = sum(1 for r in rec_rows if r.status == "MASTERED")
    shaky = sum(1 for r in rec_rows if r.status == "SHAKY")
    weak = sum(1 for r in rec_rows if r.status == "WEAK")
    total_attempts = sum(r.attempts for r in rec_rows)

    total_events = int(
        db.execute(
            select(func.count(QuizEventRow.event_id)).where(QuizEventRow.child_id == child.id)
        ).scalar_one()
        or 0
    )
    last_date = db.execute(
        select(func.max(StudyLogRow.study_date)).where(StudyLogRow.child_id == child.id)
    ).scalar_one_or_none()

    return AdminChildSummaryDto(
        id=child.id,
        name=child.name,
        grade=child.grade,
        avatarTone=child.avatar_tone,
        defaultTerm=child.default_term,
        masteryStreak=child.mastery_streak,
        englishAudioMode=child.en_audio_mode,
        speechRate=child.speech_rate,
        resetVersion=child.reset_version,
        updatedAtMs=child.updated_at_ms,
        masteredCount=mastered,
        shakyCount=shaky,
        weakCount=weak,
        totalAttempts=total_attempts,
        totalQuizEvents=total_events,
        lastStudyDate=last_date,
    )


def _summarize_user(db: Session, user: User) -> AdminUserSummaryDto:
    children = (
        db.execute(
            select(ChildProfile)
            .where(ChildProfile.user_id == user.id)
            .order_by(ChildProfile.created_at_ms.asc())
        )
        .scalars()
        .all()
    )
    return AdminUserSummaryDto(
        id=user.id,
        username=user.username,
        isAdmin=user.is_admin,
        isDisabled=user.is_disabled,
        tokenVersion=user.token_version,
        hasRecoveryCode=bool(user.recovery_code_hash),
        recoveryCodeUpdatedAtMs=user.recovery_code_updated_at_ms,
        createdAtMs=user.created_at_ms,
        children=[_summarize_child(db, c) for c in children],
    )


@router.get("/admin", include_in_schema=False)
@router.get("/admin/", include_in_schema=False)
def admin_dashboard_page():
    if not _ADMIN_HTML_PATH.exists():
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="管理看板页面不存在")
    return FileResponse(
        _ADMIN_HTML_PATH,
        media_type="text/html; charset=utf-8",
        headers={"Cache-Control": "no-store"},
    )


@router.post("/api/v1/admin/login", response_model=AdminLoginResponse)
def admin_login(req: LoginRequest, request: Request, db: Session = Depends(get_db)):
    username = req.username.strip()
    limiter_key = f"admin_login:{client_ip_from_request(request)}:{username}"
    auth_rate_limiter.ensure_allowed(limiter_key)

    user = db.execute(select(User).where(User.username == username)).scalar_one_or_none()
    if user is None or not verify_password(req.password, user.password_hash):
        auth_rate_limiter.record_failure(limiter_key)
        raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail="管理员账号或密码错误")
    if user.is_disabled:
        raise HTTPException(status_code=status.HTTP_403_FORBIDDEN, detail="该账号已被停用")
    if not user.is_admin:
        auth_rate_limiter.record_failure(limiter_key)
        raise HTTPException(status_code=status.HTTP_403_FORBIDDEN, detail="该账号不是系统管理员")

    auth_rate_limiter.clear(limiter_key)
    token = create_access_token(
        user.id,
        user.username,
        token_version=user.token_version,
        is_admin=True,
    )
    return AdminLoginResponse(
        token=token,
        user=UserDto(id=user.id, username=user.username, isAdmin=True),
    )


@router.post("/api/v1/admin/change-password", response_model=ChangePasswordResponse)
def admin_change_own_password(
    req: ChangePasswordRequest,
    admin: User = Depends(get_current_admin),
    db: Session = Depends(get_db),
):
    if not verify_password(req.currentPassword, admin.password_hash):
        raise HTTPException(status_code=status.HTTP_403_FORBIDDEN, detail="原密码验证失败")

    admin.password_hash = hash_password(req.newPassword)
    admin.token_version += 1
    record_audit(
        db,
        actor_username=admin.username,
        action="ADMIN_CHANGE_SELF_PASSWORD",
        target_type="user",
        target_id=admin.id,
        summary=f"管理员 {admin.username} 修改了自己的登录密码",
    )
    db.commit()
    db.refresh(admin)

    new_token = create_access_token(
        admin.id,
        admin.username,
        token_version=admin.token_version,
        is_admin=True,
    )
    return ChangePasswordResponse(token=new_token, message="管理员密码已更新")


@router.get("/api/v1/admin/overview", response_model=AdminOverviewResponse)
def admin_overview(
    _: User = Depends(get_current_admin),
    db: Session = Depends(get_db),
):
    total_users = int(db.execute(select(func.count(User.id))).scalar_one() or 0)
    disabled_users = int(
        db.execute(select(func.count(User.id)).where(User.is_disabled.is_(True))).scalar_one() or 0
    )
    active_users = total_users - disabled_users
    total_children = int(db.execute(select(func.count(ChildProfile.id))).scalar_one() or 0)

    total_mastered = int(
        db.execute(
            select(func.count(TopicRecordRow.topic_id)).where(TopicRecordRow.status == "MASTERED")
        ).scalar_one()
        or 0
    )
    total_weak = int(
        db.execute(
            select(func.count(TopicRecordRow.topic_id)).where(TopicRecordRow.status == "WEAK")
        ).scalar_one()
        or 0
    )
    total_quiz_events = int(
        db.execute(select(func.count(QuizEventRow.event_id))).scalar_one() or 0
    )

    today_str = date.today().isoformat()
    today_events = int(
        db.execute(
            select(func.count(QuizEventRow.event_id)).where(QuizEventRow.study_date == today_str)
        ).scalar_one()
        or 0
    )
    today_active_children = int(
        db.execute(
            select(func.count(func.distinct(QuizEventRow.child_id))).where(
                QuizEventRow.study_date == today_str
            )
        ).scalar_one()
        or 0
    )

    grade_rows = db.execute(
        select(ChildProfile.grade, func.count(ChildProfile.id)).group_by(ChildProfile.grade)
    ).all()
    grade_map = {str(g): int(cnt) for g, cnt in grade_rows}
    ordered_grades = ["一年级", "二年级", "三年级"]
    grade_dist = [
        AdminGradeBucketDto(grade=g, count=grade_map.get(g, 0))
        for g in ordered_grades
    ]
    for g, cnt in grade_map.items():
        if g not in ordered_grades:
            grade_dist.append(AdminGradeBucketDto(grade=g, count=cnt))

    days = [(date.today() - timedelta(days=offset)).isoformat() for offset in range(13, -1, -1)]
    log_rows = (
        db.execute(select(StudyLogRow).where(StudyLogRow.study_date >= days[0]))
        .scalars()
        .all()
    )
    by_day: dict[str, list[int]] = {d: [0, 0, 0] for d in days}
    for row in log_rows:
        bucket = by_day.get(row.study_date)
        if bucket is not None:
            bucket[0] += row.math_count
            bucket[1] += row.chinese_count
            bucket[2] += row.english_count

    trends = [
        AdminDailyTrendDto(
            studyDate=d,
            mathCount=by_day[d][0],
            chineseCount=by_day[d][1],
            englishCount=by_day[d][2],
            totalCount=sum(by_day[d]),
        )
        for d in days
    ]

    audit_rows = (
        db.execute(select(AdminAuditLog).order_by(AdminAuditLog.created_at_ms.desc()).limit(25))
        .scalars()
        .all()
    )

    return AdminOverviewResponse(
        totalUsers=total_users,
        activeUsers=active_users,
        disabledUsers=disabled_users,
        totalChildren=total_children,
        totalMasteredTopics=total_mastered,
        totalWeakTopics=total_weak,
        totalQuizEvents=total_quiz_events,
        todayQuizEvents=today_events,
        todayActiveChildren=today_active_children,
        gradeDistribution=grade_dist,
        dailyTrends=trends,
        recentAuditLogs=[_audit_to_dto(a) for a in audit_rows],
    )


@router.get("/api/v1/admin/users", response_model=AdminUsersPageResponse)
def admin_list_users(
    q: str = Query(default=""),
    grade: str = Query(default=""),
    accountStatus: str = Query(default="all"),
    page: int = Query(default=1, ge=1),
    pageSize: int = Query(default=20, ge=1, le=100),
    _: User = Depends(get_current_admin),
    db: Session = Depends(get_db),
):
    stmt = select(User)
    keyword = q.strip()
    if keyword:
        pattern = f"%{keyword}%"
        matching_child_user_ids = select(ChildProfile.user_id).where(
            ChildProfile.name.ilike(pattern)
        )
        stmt = stmt.where(or_(User.username.ilike(pattern), User.id.in_(matching_child_user_ids)))

    clean_grade = grade.strip()
    if clean_grade:
        grade_user_ids = select(ChildProfile.user_id).where(ChildProfile.grade == clean_grade)
        stmt = stmt.where(User.id.in_(grade_user_ids))

    if accountStatus == "active":
        stmt = stmt.where(User.is_disabled.is_(False))
    elif accountStatus == "disabled":
        stmt = stmt.where(User.is_disabled.is_(True))

    total = int(db.execute(select(func.count()).select_from(stmt.subquery())).scalar_one() or 0)
    rows = (
        db.execute(
            stmt.order_by(User.created_at_ms.desc())
            .offset((page - 1) * pageSize)
            .limit(pageSize)
        )
        .scalars()
        .all()
    )
    return AdminUsersPageResponse(
        total=total,
        page=page,
        pageSize=pageSize,
        items=[_summarize_user(db, u) for u in rows],
    )


@router.post(
    "/api/v1/admin/users",
    response_model=AdminCreateUserResponse,
    status_code=status.HTTP_201_CREATED,
)
def admin_create_user(
    req: AdminCreateUserRequest,
    admin: User = Depends(get_current_admin),
    db: Session = Depends(get_db),
):
    username = req.username.strip()
    if len(username) < 2:
        raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail="账号至少需要 2 个字符")

    existing = db.execute(select(User).where(User.username == username)).scalar_one_or_none()
    if existing is not None:
        raise HTTPException(status_code=status.HTTP_409_CONFLICT, detail="该用户名已存在")

    raw_password = req.password.strip() if req.password and req.password.strip() else generate_temp_password(10)
    recovery_code = generate_recovery_code()
    ts = now_ms()
    user = User(
        id=str(uuid.uuid4()),
        username=username,
        password_hash=hash_password(raw_password),
        is_admin=req.isAdmin,
        is_disabled=False,
        token_version=1,
        recovery_code_hash=hash_recovery_code(recovery_code),
        recovery_code_updated_at_ms=ts,
        created_at_ms=ts,
    )
    db.add(user)

    if not req.isAdmin:
        grade_val = req.initialGrade.strip() if req.initialGrade.strip() in ALLOWED_GRADES else "二年级"
        db.add(
            ChildProfile(
                id=str(uuid.uuid4()),
                user_id=user.id,
                name=req.initialChildName.strip() or "小宇",
                grade=grade_val,
                avatar_tone="amber",
                default_term="上",
                mastery_streak=2,
                en_audio_mode="bundled_tts",
                speech_rate=0.92,
                reset_version=0,
                updated_at_ms=ts,
                created_at_ms=ts,
            )
        )

    record_audit(
        db,
        actor_username=admin.username,
        action="ADMIN_CREATE_USER",
        target_type="user",
        target_id=user.id,
        summary=f"后台创建账号 {user.username}（is_admin={user.is_admin}）",
    )
    db.commit()
    db.refresh(user)
    return AdminCreateUserResponse(
        user=_summarize_user(db, user),
        initialPassword=raw_password,
        recoveryCode=recovery_code,
    )


@router.get("/api/v1/admin/users/{user_id}", response_model=AdminUserDetailResponse)
def admin_user_detail(
    user_id: str,
    _: User = Depends(get_current_admin),
    db: Session = Depends(get_db),
):
    user = db.get(User, user_id)
    if user is None:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="用户不存在")

    meta_map = load_topic_meta_map()
    children_rows = (
        db.execute(
            select(ChildProfile)
            .where(ChildProfile.user_id == user.id)
            .order_by(ChildProfile.created_at_ms.asc())
        )
        .scalars()
        .all()
    )

    child_details: list[AdminChildDetailDto] = []
    child_ids: list[str] = []
    for child in children_rows:
        child_ids.append(child.id)
        summary = _summarize_child(db, child)
        rec_rows = (
            db.execute(
                select(TopicRecordRow)
                .where(TopicRecordRow.child_id == child.id)
                .order_by(TopicRecordRow.updated_at_ms.desc())
            )
            .scalars()
            .all()
        )
        records: list[AdminTopicRecordDetailDto] = []
        for r in rec_rows:
            meta = meta_map.get(r.topic_id)
            records.append(
                AdminTopicRecordDetailDto(
                    topicId=r.topic_id,
                    title=meta["title"] if meta else r.topic_id,
                    subject=meta["subject"] if meta else "",
                    grade=meta["grade"] if meta else None,
                    term=meta["term"] if meta else "",
                    unit=meta["unit"] if meta else "",
                    status=r.status,
                    consecutiveCorrect=r.consecutive_correct,
                    attempts=r.attempts,
                    wrongCount=r.wrong_count,
                    lastQuestionId=r.last_question_id,
                    updatedAtMs=r.updated_at_ms,
                )
            )

        log_rows = (
            db.execute(
                select(StudyLogRow)
                .where(StudyLogRow.child_id == child.id)
                .order_by(StudyLogRow.study_date.desc())
                .limit(30)
            )
            .scalars()
            .all()
        )
        study_logs = [
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
        ]

        ev_rows = (
            db.execute(
                select(QuizEventRow)
                .where(QuizEventRow.child_id == child.id)
                .order_by(QuizEventRow.created_at_ms.desc())
                .limit(50)
            )
            .scalars()
            .all()
        )
        recent_events = []
        for ev in ev_rows:
            meta = meta_map.get(ev.topic_id)
            recent_events.append(
                AdminQuizEventDetailDto(
                    eventId=ev.event_id,
                    subject=ev.subject,
                    topicId=ev.topic_id,
                    topicTitle=meta["title"] if meta else ev.topic_id,
                    questionId=ev.question_id,
                    isCorrect=ev.is_correct,
                    isSkipped=ev.is_skipped,
                    studyDate=ev.study_date,
                    note=ev.note,
                    createdAtMs=ev.created_at_ms,
                )
            )

        child_details.append(
            AdminChildDetailDto(
                summary=summary,
                records=records,
                studyLog=study_logs,
                recentEvents=recent_events,
            )
        )

    target_ids = [user.id, *child_ids]
    audit_rows = (
        db.execute(
            select(AdminAuditLog)
            .where(AdminAuditLog.target_id.in_(target_ids))
            .order_by(AdminAuditLog.created_at_ms.desc())
            .limit(30)
        )
        .scalars()
        .all()
    )

    return AdminUserDetailResponse(
        user=_summarize_user(db, user),
        children=child_details,
        auditLogs=[_audit_to_dto(a) for a in audit_rows],
    )


@router.patch("/api/v1/admin/users/{user_id}", response_model=AdminUserSummaryDto)
def admin_patch_user(
    user_id: str,
    req: AdminPatchUserRequest,
    admin: User = Depends(get_current_admin),
    db: Session = Depends(get_db),
):
    user = db.get(User, user_id)
    if user is None:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="用户不存在")

    changes: list[str] = []
    if req.username is not None:
        clean_name = req.username.strip()
        if len(clean_name) < 2:
            raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail="用户名至少 2 个字符")
        if clean_name != user.username:
            dup = db.execute(select(User).where(User.username == clean_name)).scalar_one_or_none()
            if dup is not None:
                raise HTTPException(status_code=status.HTTP_409_CONFLICT, detail="目标用户名已被占用")
            changes.append(f"用户名 {user.username} -> {clean_name}")
            user.username = clean_name
            user.token_version += 1

    if req.isDisabled is not None:
        if user.id == admin.id and req.isDisabled:
            raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail="不能停用当前管理员自己")
        if req.isDisabled != user.is_disabled:
            user.is_disabled = req.isDisabled
            if req.isDisabled:
                user.token_version += 1
            changes.append("停用账号" if req.isDisabled else "恢复启用账号")

    if req.isAdmin is not None:
        if user.id == admin.id and not req.isAdmin:
            raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail="不能取消自己的管理员权限")
        if req.isAdmin != user.is_admin:
            user.is_admin = req.isAdmin
            user.token_version += 1
            changes.append(f"管理员权限={req.isAdmin}")

    if changes:
        record_audit(
            db,
            actor_username=admin.username,
            action="ADMIN_UPDATE_USER",
            target_type="user",
            target_id=user.id,
            summary=f"修改账号 {user.username}：" + "；".join(changes),
        )
        db.commit()
        db.refresh(user)

    return _summarize_user(db, user)


@router.post("/api/v1/admin/users/{user_id}/reset-password", response_model=AdminResetPasswordResponse)
def admin_reset_user_password(
    user_id: str,
    req: AdminResetPasswordRequest,
    admin: User = Depends(get_current_admin),
    db: Session = Depends(get_db),
):
    user = db.get(User, user_id)
    if user is None:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="用户不存在")

    raw_new = (req.newPassword or "").strip()
    if raw_new and len(raw_new) < 6:
        raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail="新密码至少 6 位，或留空自动生成")

    temp_password = raw_new or generate_temp_password(10)
    user.password_hash = hash_password(temp_password)
    user.token_version += 1

    new_recovery: str | None = None
    if req.alsoRotateRecoveryCode:
        new_recovery = generate_recovery_code()
        user.recovery_code_hash = hash_recovery_code(new_recovery)
        user.recovery_code_updated_at_ms = now_ms()

    record_audit(
        db,
        actor_username=admin.username,
        action="ADMIN_RESET_PASSWORD",
        target_type="user",
        target_id=user.id,
        summary=(
            f"管理员重置账号 {user.username} 登录密码（token_version->{user.token_version}"
            + ("，并轮换恢复码" if new_recovery else "")
            + "）"
        ),
    )
    db.commit()

    return AdminResetPasswordResponse(
        temporaryPassword=temp_password,
        tokenVersion=user.token_version,
        recoveryCode=new_recovery,
    )


@router.post("/api/v1/admin/users/{user_id}/rotate-recovery-code", response_model=RecoveryCodeResponse)
def admin_rotate_user_recovery_code(
    user_id: str,
    admin: User = Depends(get_current_admin),
    db: Session = Depends(get_db),
):
    user = db.get(User, user_id)
    if user is None:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="用户不存在")

    code = generate_recovery_code()
    ts = now_ms()
    user.recovery_code_hash = hash_recovery_code(code)
    user.recovery_code_updated_at_ms = ts

    record_audit(
        db,
        actor_username=admin.username,
        action="ADMIN_ROTATE_RECOVERY_CODE",
        target_type="user",
        target_id=user.id,
        summary=f"管理员为账号 {user.username} 重新生成了账号恢复码",
    )
    db.commit()
    return RecoveryCodeResponse(recoveryCode=code, updatedAtMs=ts)


@router.delete("/api/v1/admin/users/{user_id}")
def admin_delete_user(
    user_id: str,
    admin: User = Depends(get_current_admin),
    db: Session = Depends(get_db),
):
    user = db.get(User, user_id)
    if user is None:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="用户不存在")
    if user.id == admin.id:
        raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail="不能删除当前登录的管理员账号")

    username = user.username
    db.delete(user)
    record_audit(
        db,
        actor_username=admin.username,
        action="ADMIN_DELETE_USER",
        target_type="user",
        target_id=user_id,
        summary=f"管理员删除账号 {username} 及其全部学习数据",
    )
    db.commit()
    return {"status": "deleted", "userId": user_id}


@router.patch("/api/v1/admin/children/{child_id}", response_model=AdminChildSummaryDto)
def admin_patch_child(
    child_id: str,
    req: AdminChildPatchRequest,
    admin: User = Depends(get_current_admin),
    db: Session = Depends(get_db),
):
    child = db.get(ChildProfile, child_id)
    if child is None:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="孩子档案不存在")

    changes: list[str] = []
    if req.name is not None and req.name.strip():
        clean = req.name.strip()
        if clean != child.name:
            changes.append(f"昵称 {child.name}->{clean}")
            child.name = clean
    if req.grade is not None and req.grade.strip():
        clean_g = req.grade.strip()
        if clean_g not in ALLOWED_GRADES:
            raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail="年级仅支持 一年级/二年级/三年级")
        if clean_g != child.grade:
            changes.append(f"年级 {child.grade}->{clean_g}")
            child.grade = clean_g
    if req.avatarTone in ALLOWED_TONES and req.avatarTone != child.avatar_tone:
        child.avatar_tone = req.avatarTone
        changes.append(f"头像色->{req.avatarTone}")
    if req.defaultTerm in ALLOWED_TERMS and req.defaultTerm != child.default_term:
        child.default_term = req.defaultTerm
        changes.append(f"学期->{req.defaultTerm}")
    if req.masteryStreak is not None and req.masteryStreak != child.mastery_streak:
        child.mastery_streak = max(2, min(3, req.masteryStreak))
        changes.append(f"连对门槛->{child.mastery_streak}")
    if req.englishAudioMode in ALLOWED_AUDIO_MODES and req.englishAudioMode != child.en_audio_mode:
        child.en_audio_mode = req.englishAudioMode
        changes.append(f"英语语音->{req.englishAudioMode}")
    if req.speechRate is not None:
        rate = max(0.75, min(1.25, float(req.speechRate)))
        if abs(rate - child.speech_rate) > 1e-4:
            child.speech_rate = rate
            changes.append(f"语速->{rate:.2f}")

    if changes:
        child.updated_at_ms = now_ms()
        record_audit(
            db,
            actor_username=admin.username,
            action="ADMIN_UPDATE_CHILD",
            target_type="child",
            target_id=child.id,
            summary=f"修改孩子档案「{child.name}」：" + "；".join(changes),
        )
        db.commit()
        db.refresh(child)

    return _summarize_child(db, child)


@router.post("/api/v1/admin/children/{child_id}/reset", response_model=AdminChildSummaryDto)
def admin_reset_child(
    child_id: str,
    admin: User = Depends(get_current_admin),
    db: Session = Depends(get_db),
):
    child = db.get(ChildProfile, child_id)
    if child is None:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="孩子档案不存在")

    db.execute(delete(QuizEventRow).where(QuizEventRow.child_id == child.id))
    db.execute(delete(TopicRecordRow).where(TopicRecordRow.child_id == child.id))
    db.execute(delete(StudyLogRow).where(StudyLogRow.child_id == child.id))

    child.reset_version += 1
    child.updated_at_ms = now_ms()

    record_audit(
        db,
        actor_username=admin.username,
        action="ADMIN_RESET_CHILD",
        target_type="child",
        target_id=child.id,
        summary=f"管理员远程清空孩子「{child.name}」全部学习数据（reset_version->{child.reset_version}）",
    )
    db.commit()
    db.refresh(child)
    return _summarize_child(db, child)
