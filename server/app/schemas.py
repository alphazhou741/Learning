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


class MeResponse(BaseModel):
    user: UserDto
    children: list[ChildProfileDto]


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
