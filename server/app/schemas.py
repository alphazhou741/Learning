from pydantic import BaseModel, Field


class RegisterRequest(BaseModel):
    username: str = Field(min_length=2, max_length=40)
    password: str = Field(min_length=6, max_length=128)
    initialChildName: str = Field(default="小宇", min_length=1, max_length=24)
    initialGrade: str = Field(default="二年级", min_length=1, max_length=16)


class LoginRequest(BaseModel):
    username: str = Field(min_length=2, max_length=40)
    password: str = Field(min_length=1, max_length=128)


class UserDto(BaseModel):
    id: str
    username: str
    isAdmin: bool = False


class ChildProfileDto(BaseModel):
    id: str
    name: str
    grade: str
    avatarTone: str
    defaultTerm: str
    masteryStreak: int
    englishAudioMode: str
    speechRate: float
    resetVersion: int
    updatedAtMs: int


class AuthResponse(BaseModel):
    token: str
    user: UserDto
    children: list[ChildProfileDto]
    recoveryCode: str | None = None


class MeResponse(BaseModel):
    user: UserDto
    children: list[ChildProfileDto]


class ResetByRecoveryRequest(BaseModel):
    username: str = Field(min_length=2, max_length=40)
    recoveryCode: str = Field(min_length=6, max_length=64)
    newPassword: str = Field(min_length=6, max_length=128)


class ResetByRecoveryResponse(BaseModel):
    message: str
    newRecoveryCode: str


class ChangePasswordRequest(BaseModel):
    currentPassword: str = Field(min_length=1, max_length=128)
    newPassword: str = Field(min_length=6, max_length=128)


class ChangePasswordResponse(BaseModel):
    token: str
    message: str


class RotateRecoveryRequest(BaseModel):
    currentPassword: str = Field(min_length=1, max_length=128)


class RecoveryCodeResponse(BaseModel):
    recoveryCode: str
    updatedAtMs: int


class CreateChildRequest(BaseModel):
    name: str = Field(min_length=1, max_length=24)
    grade: str = Field(default="二年级", min_length=1, max_length=16)
    avatarTone: str = Field(default="amber")
    defaultTerm: str = Field(default="上")


class ChildPatchDto(BaseModel):
    name: str | None = Field(default=None, min_length=1, max_length=24)
    grade: str | None = Field(default=None, min_length=1, max_length=16)
    avatarTone: str | None = None
    defaultTerm: str | None = None
    masteryStreak: int | None = Field(default=None, ge=2, le=3)
    englishAudioMode: str | None = None
    speechRate: float | None = Field(default=None, ge=0.75, le=1.25)
    updatedAtMs: int = 0


class TopicRecordDto(BaseModel):
    topicId: str
    status: str
    consecutiveCorrect: int
    attempts: int
    wrongCount: int
    lastQuestionId: str | None = None
    updatedAtMs: int = 0


class DayStudyLogDto(BaseModel):
    studyDate: str
    mathCount: int
    chineseCount: int
    englishCount: int
    minutes: int
    note: str = ""
    updatedAtMs: int = 0


class QuizEventDto(BaseModel):
    eventId: str
    subject: str
    topicId: str
    questionId: str
    isCorrect: bool
    isSkipped: bool = False
    studyDate: str
    note: str = ""
    createdAtMs: int


class SyncRequest(BaseModel):
    clientResetVersion: int = 0
    profilePatch: ChildPatchDto | None = None
    records: list[TopicRecordDto] = Field(default_factory=list)
    quizEvents: list[QuizEventDto] = Field(default_factory=list)


class ChildSnapshotDto(BaseModel):
    child: ChildProfileDto
    records: list[TopicRecordDto]
    studyLog: list[DayStudyLogDto]
    serverTimeMs: int


class SyncResponse(BaseModel):
    status: str  # "ok" | "stale_reset"
    snapshot: ChildSnapshotDto


class ResetChildRequest(BaseModel):
    accountPassword: str = Field(min_length=1, max_length=128)


# --- Admin Dashboard DTOs ---


class AdminLoginResponse(BaseModel):
    token: str
    user: UserDto


class AdminGradeBucketDto(BaseModel):
    grade: str
    count: int


class AdminDailyTrendDto(BaseModel):
    studyDate: str
    mathCount: int
    chineseCount: int
    englishCount: int
    totalCount: int


class AdminAuditLogDto(BaseModel):
    id: str
    actorUsername: str
    action: str
    targetType: str
    targetId: str
    summary: str
    createdAtMs: int


class AdminOverviewResponse(BaseModel):
    totalUsers: int
    activeUsers: int
    disabledUsers: int
    totalChildren: int
    totalMasteredTopics: int
    totalWeakTopics: int
    totalQuizEvents: int
    todayQuizEvents: int
    todayActiveChildren: int
    gradeDistribution: list[AdminGradeBucketDto]
    dailyTrends: list[AdminDailyTrendDto]
    recentAuditLogs: list[AdminAuditLogDto]


class AdminChildSummaryDto(BaseModel):
    id: str
    name: str
    grade: str
    avatarTone: str
    defaultTerm: str
    masteryStreak: int
    englishAudioMode: str
    speechRate: float
    resetVersion: int
    updatedAtMs: int
    masteredCount: int
    shakyCount: int
    weakCount: int
    totalAttempts: int
    totalQuizEvents: int
    lastStudyDate: str | None = None


class AdminUserSummaryDto(BaseModel):
    id: str
    username: str
    isAdmin: bool
    isDisabled: bool
    tokenVersion: int
    hasRecoveryCode: bool
    recoveryCodeUpdatedAtMs: int | None = None
    createdAtMs: int
    children: list[AdminChildSummaryDto]


class AdminUsersPageResponse(BaseModel):
    total: int
    page: int
    pageSize: int
    items: list[AdminUserSummaryDto]


class AdminTopicRecordDetailDto(BaseModel):
    topicId: str
    title: str
    subject: str
    grade: int | None = None
    term: str = ""
    unit: str = ""
    status: str
    consecutiveCorrect: int
    attempts: int
    wrongCount: int
    lastQuestionId: str | None = None
    updatedAtMs: int


class AdminQuizEventDetailDto(BaseModel):
    eventId: str
    subject: str
    topicId: str
    topicTitle: str
    questionId: str
    isCorrect: bool
    isSkipped: bool
    studyDate: str
    note: str
    createdAtMs: int


class AdminChildDetailDto(BaseModel):
    summary: AdminChildSummaryDto
    records: list[AdminTopicRecordDetailDto]
    studyLog: list[DayStudyLogDto]
    recentEvents: list[AdminQuizEventDetailDto]


class AdminUserDetailResponse(BaseModel):
    user: AdminUserSummaryDto
    children: list[AdminChildDetailDto]
    auditLogs: list[AdminAuditLogDto]


class AdminCreateUserRequest(BaseModel):
    username: str = Field(min_length=2, max_length=40)
    password: str | None = Field(default=None, min_length=6, max_length=128)
    initialChildName: str = Field(default="小宇", min_length=1, max_length=24)
    initialGrade: str = Field(default="二年级", min_length=1, max_length=16)
    isAdmin: bool = False


class AdminCreateUserResponse(BaseModel):
    user: AdminUserSummaryDto
    initialPassword: str
    recoveryCode: str


class AdminPatchUserRequest(BaseModel):
    username: str | None = Field(default=None, min_length=2, max_length=40)
    isDisabled: bool | None = None
    isAdmin: bool | None = None


class AdminResetPasswordRequest(BaseModel):
    newPassword: str | None = Field(default=None, max_length=128)
    alsoRotateRecoveryCode: bool = False


class AdminResetPasswordResponse(BaseModel):
    temporaryPassword: str
    tokenVersion: int
    recoveryCode: str | None = None


class AdminChildPatchRequest(BaseModel):
    name: str | None = Field(default=None, min_length=1, max_length=24)
    grade: str | None = Field(default=None, min_length=1, max_length=16)
    avatarTone: str | None = None
    defaultTerm: str | None = None
    masteryStreak: int | None = Field(default=None, ge=2, le=3)
    englishAudioMode: str | None = None
    speechRate: float | None = Field(default=None, ge=0.75, le=1.25)
