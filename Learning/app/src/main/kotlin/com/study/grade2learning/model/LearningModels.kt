package com.study.grade2learning.model

enum class SubjectKey(
    val key: String,
    val title: String,
    val defaultEdition: String
) {
    MATH("math", "数学", "苏教版二年级"),
    CHINESE("chinese", "语文", "人教版二年级"),
    ENGLISH("english", "英语", "PEP 启蒙（三年级起点）");

    companion object {
        fun fromKey(key: String): SubjectKey = entries.firstOrNull { it.key == key } ?: MATH
    }
}

enum class TermFilter(val key: String, val label: String, val pillLabel: String) {
    UPPER("上", "上册", "上册（当前学期）"),
    LOWER("下", "下册", "下册"),
    ALL("全", "全学年", "全学年");

    companion object {
        fun fromKey(key: String): TermFilter = entries.firstOrNull { it.key == key } ?: UPPER
    }
}

enum class NodeStatus(val label: String) {
    UNKNOWN("未测"),
    MASTERED("已掌握"),
    SHAKY("不稳"),
    WEAK("薄弱")
}

data class TopicNode(
    val id: String,
    val subject: SubjectKey,
    val name: String,
    val grade: Int,
    val term: String,
    val unit: String,
    val orderIndex: Int,
    val isTarget: Boolean,
    val description: String,
    val masteryCriteria: List<String>,
    val rootReason: String,
    val diagnosticQuestion: String
)

data class DependencyEdge(
    val topicId: String,
    val prerequisiteId: String,
    val strength: String,
    val reason: String
)

data class QuestionChoice(
    val key: String,
    val text: String
)

data class QuestionItem(
    val id: String,
    val topicId: String,
    val subject: SubjectKey,
    val difficulty: String,
    val type: String, // "choice" | "numeric"
    val prompt: String,
    val choices: List<QuestionChoice>,
    val answer: String,
    val explanation: String,
    val ttsPromptText: String = "",
    val ttsAnswerText: String = "",
    val audioFile: String = "",
    val answerAudioFile: String = ""
)

data class SubjectDataset(
    val key: SubjectKey,
    val title: String,
    val edition: String,
    val targetCount: Int,
    val prerequisiteCount: Int,
    val topics: List<TopicNode>,
    val dependencies: List<DependencyEdge>,
    val questions: List<QuestionItem>
)

data class Grade2Bundle(
    val version: Int,
    val subjects: Map<SubjectKey, SubjectDataset>
)

data class TopicRecord(
    val status: NodeStatus = NodeStatus.UNKNOWN,
    val consecutiveCorrect: Int = 0,
    val attempts: Int = 0,
    val wrongCount: Int = 0,
    val lastQuestionId: String? = null,
    val updatedAtMs: Long = 0L
)

data class UserProfile(
    val name: String = "小宇",
    val grade: String = "二年级",
    val avatarTone: String = "amber",
    val defaultTerm: TermFilter = TermFilter.UPPER,
    val updatedAtMs: Long = 0L
)

enum class EnglishAudioMode(val key: String, val title: String) {
    BUNDLED_WITH_TTS("bundled_tts", "预置英文音频 + TTS 兜底（默认）"),
    TTS_ONLY("tts_only", "仅平板系统 Android TTS");

    companion object {
        fun fromKey(key: String?): EnglishAudioMode = entries.firstOrNull {
            it.key.equals(key, ignoreCase = true) || it.name.equals(key, ignoreCase = true)
        } ?: BUNDLED_WITH_TTS
    }
}

data class ParentSettings(
    val masteryStreak: Int = 2,
    val englishAudioMode: EnglishAudioMode = EnglishAudioMode.BUNDLED_WITH_TTS,
    val speechRate: Float = 0.92f,
    val updatedAtMs: Long = 0L
)

data class DayStudyRecord(
    val studyDate: String,
    val mathCount: Int = 0,
    val chineseCount: Int = 0,
    val englishCount: Int = 0,
    val minutes: Int = 0,
    val note: String = "",
    val updatedAtMs: Long = 0L
) {
    val dayOfMonth: Int
        get() = studyDate.substringAfterLast('-', "0").toIntOrNull() ?: 0

    val totalQuestions: Int get() = mathCount + chineseCount + englishCount
}

data class UserDto(
    val id: String,
    val username: String
)

data class ChildSummary(
    val id: String,
    val name: String,
    val grade: String = "二年级",
    val avatarTone: String = "amber",
    val defaultTerm: TermFilter = TermFilter.UPPER,
    val masteryStreak: Int = 2,
    val englishAudioMode: EnglishAudioMode = EnglishAudioMode.BUNDLED_WITH_TTS,
    val speechRate: Float = 0.92f,
    val resetVersion: Int = 1,
    val updatedAtMs: Long = 0L
) {
    fun toUserProfile(): UserProfile = UserProfile(
        name = name,
        grade = grade,
        avatarTone = avatarTone,
        defaultTerm = defaultTerm,
        updatedAtMs = updatedAtMs
    )

    fun toParentSettings(): ParentSettings = ParentSettings(
        masteryStreak = masteryStreak,
        englishAudioMode = englishAudioMode,
        speechRate = speechRate,
        updatedAtMs = updatedAtMs
    )
}

data class AuthSession(
    val token: String,
    val user: UserDto,
    val children: List<ChildSummary>,
    val activeChildId: String
)

data class QuizSyncEvent(
    val eventId: String,
    val subject: SubjectKey,
    val topicId: String,
    val questionId: String,
    val isCorrect: Boolean,
    val isSkipped: Boolean,
    val studyDate: String,
    val note: String,
    val createdAtMs: Long
)

data class ChildSnapshot(
    val child: ChildSummary,
    val records: Map<String, TopicRecord>,
    val studyLog: Map<String, DayStudyRecord>,
    val serverTimeMs: Long
)

enum class SyncState(val label: String) {
    IDLE("云端已同步"),
    SYNCING("同步中"),
    PENDING("待同步"),
    OFFLINE("离线本地保存")
}

data class UnitGroup(
    val subject: SubjectKey,
    val term: String,
    val title: String,
    val topics: List<TopicNode>
)

data class RemediationStep(
    val topic: TopicNode,
    val status: NodeStatus,
    val roleLabel: String,
    val reason: String,
    val isRootCause: Boolean
)
