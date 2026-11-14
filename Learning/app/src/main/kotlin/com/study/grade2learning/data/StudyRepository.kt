package com.study.grade2learning.data

import android.content.Context
import com.study.grade2learning.model.AuthSession
import com.study.grade2learning.model.ChildSnapshot
import com.study.grade2learning.model.ChildSummary
import com.study.grade2learning.model.DayStudyRecord
import com.study.grade2learning.model.DependencyEdge
import com.study.grade2learning.model.EnglishAudioMode
import com.study.grade2learning.model.Grade2Bundle
import com.study.grade2learning.model.NodeStatus
import com.study.grade2learning.model.ParentSettings
import com.study.grade2learning.model.QuestionChoice
import com.study.grade2learning.model.QuestionItem
import com.study.grade2learning.model.QuizSyncEvent
import com.study.grade2learning.model.SubjectDataset
import com.study.grade2learning.model.SubjectKey
import com.study.grade2learning.model.TermFilter
import com.study.grade2learning.model.TopicNode
import com.study.grade2learning.model.TopicRecord
import com.study.grade2learning.model.UserDto
import com.study.grade2learning.model.UserProfile
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

class StudyRepository(context: Context) {

    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences("grade2_study_prefs", Context.MODE_PRIVATE)

    fun loadBundle(): Grade2Bundle {
        val raw = appContext.assets.open("grade2_bundle.json").bufferedReader().use { it.readText() }
        val root = JSONObject(raw)
        val subjectsJson = root.getJSONObject("subjects")
        val map = linkedMapOf<SubjectKey, SubjectDataset>()

        for (subjectKey in SubjectKey.entries) {
            val sObj = subjectsJson.getJSONObject(subjectKey.key)
            val topicsArr = sObj.getJSONArray("topics")
            val topics = buildList {
                for (i in 0 until topicsArr.length()) {
                    val t = topicsArr.getJSONObject(i)
                    val critArr = t.optJSONArray("masteryCriteria") ?: JSONArray()
                    val criteria = List(critArr.length()) { idx -> critArr.getString(idx) }
                    add(
                        TopicNode(
                            id = t.getString("id"),
                            subject = subjectKey,
                            name = t.getString("name"),
                            grade = t.getInt("grade"),
                            term = t.optString("term", "上"),
                            unit = t.optString("unit", "综合单元"),
                            orderIndex = t.optInt("orderIndex", i),
                            isTarget = t.optBoolean("isTarget", true),
                            description = t.optString("description", ""),
                            masteryCriteria = criteria,
                            rootReason = t.optString("rootReason", ""),
                            diagnosticQuestion = t.optString("diagnosticQuestion", "")
                        )
                    )
                }
            }

            val depsArr = sObj.getJSONArray("dependencies")
            val deps = List(depsArr.length()) { idx ->
                val d = depsArr.getJSONObject(idx)
                DependencyEdge(
                    topicId = d.getString("topicId"),
                    prerequisiteId = d.getString("prerequisiteId"),
                    strength = d.optString("strength", "hard"),
                    reason = d.optString("reason", "")
                )
            }

            val qsArr = sObj.getJSONArray("questions")
            val qs = List(qsArr.length()) { idx ->
                val q = qsArr.getJSONObject(idx)
                val chArr = q.optJSONArray("choices") ?: JSONArray()
                val choices = List(chArr.length()) { cIdx ->
                    val c = chArr.getJSONObject(cIdx)
                    QuestionChoice(
                        key = c.getString("key"),
                        text = c.getString("text")
                    )
                }
                QuestionItem(
                    id = q.getString("id"),
                    topicId = q.getString("topicId"),
                    subject = subjectKey,
                    difficulty = q.optString("difficulty", "basic"),
                    type = q.optString("type", "choice"),
                    prompt = q.getString("prompt"),
                    choices = choices,
                    answer = q.getString("answer"),
                    explanation = q.optString("explanation", ""),
                    ttsPromptText = q.optString("ttsPromptText", ""),
                    ttsAnswerText = q.optString("ttsAnswerText", ""),
                    audioFile = q.optString("audioFile", ""),
                    answerAudioFile = q.optString("answerAudioFile", "")
                )
            }

            map[subjectKey] = SubjectDataset(
                key = subjectKey,
                title = sObj.optString("title", subjectKey.title),
                edition = sObj.optString("edition", subjectKey.defaultEdition),
                targetCount = sObj.optInt("targetCount", topics.count { it.isTarget }),
                prerequisiteCount = sObj.optInt("prerequisiteCount", topics.count { !it.isTarget }),
                topics = topics,
                dependencies = deps,
                questions = qs
            )
        }

        return Grade2Bundle(
            version = root.optInt("version", 1),
            subjects = map
        )
    }

    private val authPrefs = appContext.getSharedPreferences("grade2_auth_prefs", Context.MODE_PRIVATE)

    fun loadAuthSession(): AuthSession? {
        val token = authPrefs.getString(KEY_AUTH_TOKEN, null)?.takeIf { it.isNotBlank() } ?: return null
        val userId = authPrefs.getString(KEY_AUTH_USER_ID, null)?.takeIf { it.isNotBlank() } ?: return null
        val username = authPrefs.getString(KEY_AUTH_USERNAME, null)?.takeIf { it.isNotBlank() } ?: return null
        val childrenJson = authPrefs.getString(KEY_AUTH_CHILDREN, null) ?: return null
        val children = decodeChildren(childrenJson)
        if (children.isEmpty()) return null
        val activeId = authPrefs.getString(KEY_AUTH_ACTIVE_CHILD_ID, null)
            ?.takeIf { id -> children.any { it.id == id } }
            ?: children.first().id
        return AuthSession(
            token = token,
            user = UserDto(id = userId, username = username),
            children = children,
            activeChildId = activeId
        )
    }

    fun saveAuthSession(session: AuthSession) {
        authPrefs.edit()
            .putString(KEY_AUTH_TOKEN, session.token)
            .putString(KEY_AUTH_USER_ID, session.user.id)
            .putString(KEY_AUTH_USERNAME, session.user.username)
            .putString(KEY_AUTH_CHILDREN, encodeChildren(session.children))
            .putString(KEY_AUTH_ACTIVE_CHILD_ID, session.activeChildId)
            .apply()
    }

    fun clearAllLocalState() {
        authPrefs.edit().clear().commit()
        prefs.edit().clear().commit()
        deleteBackupFile()
    }

    fun loadProfile(childId: String, fallback: UserProfile = UserProfile()): UserProfile {
        val p = childPrefix(childId)
        if (!prefs.contains("${p}$KEY_PROFILE_NAME")) return fallback
        return UserProfile(
            name = prefs.getString("${p}$KEY_PROFILE_NAME", fallback.name) ?: fallback.name,
            grade = prefs.getString("${p}$KEY_PROFILE_GRADE", fallback.grade) ?: fallback.grade,
            avatarTone = prefs.getString("${p}$KEY_PROFILE_TONE", fallback.avatarTone) ?: fallback.avatarTone,
            defaultTerm = TermFilter.fromKey(prefs.getString("${p}$KEY_PROFILE_TERM", fallback.defaultTerm.key) ?: fallback.defaultTerm.key),
            updatedAtMs = prefs.getLong("${p}$KEY_PROFILE_UPDATED_AT", fallback.updatedAtMs)
        )
    }

    fun saveProfile(childId: String, profile: UserProfile) {
        val p = childPrefix(childId)
        prefs.edit()
            .putString("${p}$KEY_PROFILE_NAME", profile.name)
            .putString("${p}$KEY_PROFILE_GRADE", profile.grade)
            .putString("${p}$KEY_PROFILE_TONE", profile.avatarTone)
            .putString("${p}$KEY_PROFILE_TERM", profile.defaultTerm.key)
            .putLong("${p}$KEY_PROFILE_UPDATED_AT", profile.updatedAtMs)
            .apply()
    }

    fun loadParentSettings(childId: String, fallback: ParentSettings = ParentSettings()): ParentSettings {
        val p = childPrefix(childId)
        if (!prefs.contains("${p}$KEY_MASTERY_STREAK")) return fallback
        return ParentSettings(
            masteryStreak = prefs.getInt("${p}$KEY_MASTERY_STREAK", fallback.masteryStreak).coerceIn(2, 3),
            englishAudioMode = EnglishAudioMode.fromKey(prefs.getString("${p}$KEY_EN_AUDIO_MODE", fallback.englishAudioMode.key)),
            speechRate = prefs.getFloat("${p}$KEY_SPEECH_RATE", fallback.speechRate).coerceIn(0.75f, 1.25f),
            updatedAtMs = prefs.getLong("${p}$KEY_SETTINGS_UPDATED_AT", fallback.updatedAtMs)
        )
    }

    fun saveParentSettings(childId: String, settings: ParentSettings) {
        val p = childPrefix(childId)
        prefs.edit()
            .putInt("${p}$KEY_MASTERY_STREAK", settings.masteryStreak)
            .putString("${p}$KEY_EN_AUDIO_MODE", settings.englishAudioMode.key)
            .putFloat("${p}$KEY_SPEECH_RATE", settings.speechRate)
            .putLong("${p}$KEY_SETTINGS_UPDATED_AT", settings.updatedAtMs)
            .apply()
    }

    fun loadResetVersion(childId: String, fallback: Int = 1): Int {
        return prefs.getInt("${childPrefix(childId)}$KEY_RESET_VERSION", fallback)
    }

    fun saveResetVersion(childId: String, resetVersion: Int) {
        prefs.edit().putInt("${childPrefix(childId)}$KEY_RESET_VERSION", resetVersion).apply()
    }

    fun loadRecords(childId: String): Map<String, TopicRecord> {
        val raw = prefs.getString("${childPrefix(childId)}$KEY_RECORDS_JSON", null)
        if (!raw.isNullOrBlank()) {
            return decodeRecords(raw)
        }
        return emptyMap()
    }

    fun saveRecords(childId: String, records: Map<String, TopicRecord>) {
        val root = JSONObject()
        records.forEach { (id, rec) ->
            root.put(
                id,
                JSONObject()
                    .put("status", rec.status.name)
                    .put("consecutiveCorrect", rec.consecutiveCorrect)
                    .put("attempts", rec.attempts)
                    .put("wrongCount", rec.wrongCount)
                    .put("lastQuestionId", rec.lastQuestionId ?: JSONObject.NULL)
                    .put("updatedAtMs", rec.updatedAtMs)
            )
        }
        prefs.edit().putString("${childPrefix(childId)}$KEY_RECORDS_JSON", root.toString()).apply()
    }

    fun loadStudyLog(childId: String): Map<String, DayStudyRecord> {
        val raw = prefs.getString("${childPrefix(childId)}$KEY_STUDY_LOG_JSON", null)
        if (!raw.isNullOrBlank()) {
            return decodeStudyLog(raw)
        }
        return emptyMap()
    }

    fun saveStudyLog(childId: String, log: Map<String, DayStudyRecord>) {
        val root = JSONObject()
        log.forEach { (date, item) ->
            root.put(
                date,
                JSONObject()
                    .put("mathCount", item.mathCount)
                    .put("chineseCount", item.chineseCount)
                    .put("englishCount", item.englishCount)
                    .put("minutes", item.minutes)
                    .put("note", item.note)
                    .put("updatedAtMs", item.updatedAtMs)
            )
        }
        prefs.edit().putString("${childPrefix(childId)}$KEY_STUDY_LOG_JSON", root.toString()).apply()
    }

    fun loadPendingEvents(childId: String): List<QuizSyncEvent> {
        val raw = prefs.getString("${childPrefix(childId)}$KEY_PENDING_EVENTS", null) ?: return emptyList()
        return runCatching {
            val arr = JSONArray(raw)
            val list = ArrayList<QuizSyncEvent>(arr.length())
            for (i in 0 until arr.length()) {
                val item = arr.getJSONObject(i)
                list.add(
                    QuizSyncEvent(
                        eventId = item.getString("eventId"),
                        subject = SubjectKey.fromKey(item.getString("subject")),
                        topicId = item.getString("topicId"),
                        questionId = item.getString("questionId"),
                        isCorrect = item.getBoolean("isCorrect"),
                        isSkipped = item.optBoolean("isSkipped", false),
                        studyDate = item.getString("studyDate"),
                        note = item.optString("note", ""),
                        createdAtMs = item.getLong("createdAtMs")
                    )
                )
            }
            list
        }.getOrDefault(emptyList())
    }

    fun savePendingEvents(childId: String, events: List<QuizSyncEvent>) {
        val arr = JSONArray()
        events.forEach { ev ->
            arr.put(
                JSONObject()
                    .put("eventId", ev.eventId)
                    .put("subject", ev.subject.key)
                    .put("topicId", ev.topicId)
                    .put("questionId", ev.questionId)
                    .put("isCorrect", ev.isCorrect)
                    .put("isSkipped", ev.isSkipped)
                    .put("studyDate", ev.studyDate)
                    .put("note", ev.note)
                    .put("createdAtMs", ev.createdAtMs)
            )
        }
        prefs.edit().putString("${childPrefix(childId)}$KEY_PENDING_EVENTS", arr.toString()).apply()
    }

    fun loadPendingProfilePatch(childId: String): ChildPatchRequest? {
        val raw = prefs.getString("${childPrefix(childId)}$KEY_PENDING_PATCH", null) ?: return null
        return runCatching {
            val obj = JSONObject(raw)
            ChildPatchRequest(
                name = if (obj.has("name")) obj.getString("name") else null,
                grade = if (obj.has("grade")) obj.getString("grade") else null,
                avatarTone = if (obj.has("avatarTone")) obj.getString("avatarTone") else null,
                defaultTerm = if (obj.has("defaultTerm")) TermFilter.fromKey(obj.getString("defaultTerm")) else null,
                masteryStreak = if (obj.has("masteryStreak")) obj.getInt("masteryStreak") else null,
                englishAudioMode = if (obj.has("englishAudioMode")) EnglishAudioMode.fromKey(obj.getString("englishAudioMode")) else null,
                speechRate = if (obj.has("speechRate")) obj.getDouble("speechRate").toFloat() else null,
                updatedAtMs = obj.optLong("updatedAtMs", System.currentTimeMillis())
            )
        }.getOrNull()
    }

    fun savePendingProfilePatch(childId: String, patch: ChildPatchRequest?) {
        val key = "${childPrefix(childId)}$KEY_PENDING_PATCH"
        if (patch == null) {
            prefs.edit().remove(key).apply()
            return
        }
        val obj = JSONObject().apply {
            patch.name?.let { put("name", it) }
            patch.grade?.let { put("grade", it) }
            patch.avatarTone?.let { put("avatarTone", it) }
            patch.defaultTerm?.let { put("defaultTerm", it.key) }
            patch.masteryStreak?.let { put("masteryStreak", it) }
            patch.englishAudioMode?.let { put("englishAudioMode", it.key) }
            patch.speechRate?.let { put("speechRate", it.toDouble()) }
            put("updatedAtMs", patch.updatedAtMs)
        }
        prefs.edit().putString(key, obj.toString()).apply()
    }

    fun saveSnapshot(snapshot: ChildSnapshot) {
        val cid = snapshot.child.id
        saveProfile(cid, snapshot.child.toUserProfile())
        saveParentSettings(cid, snapshot.child.toParentSettings())
        saveResetVersion(cid, snapshot.child.resetVersion)
        saveRecords(cid, snapshot.records)
        saveStudyLog(cid, snapshot.studyLog)
    }

    fun clearChildLocalProgress(childId: String, snapshot: ChildSnapshot) {
        savePendingEvents(childId, emptyList())
        savePendingProfilePatch(childId, null)
        saveSnapshot(snapshot)
        deleteBackupFile()
    }

    fun exportBackupJson(
        profile: UserProfile,
        settings: ParentSettings,
        records: Map<String, TopicRecord>,
        studyLog: Map<String, DayStudyRecord>
    ): String {
        val root = JSONObject()
        root.put(
            "profile",
            JSONObject()
                .put("name", profile.name)
                .put("grade", profile.grade)
                .put("avatarTone", profile.avatarTone)
                .put("defaultTerm", profile.defaultTerm.key)
        )
        root.put(
            "settings",
            JSONObject()
                .put("masteryStreak", settings.masteryStreak)
                .put("englishAudioMode", settings.englishAudioMode.key)
                .put("speechRate", settings.speechRate)
        )
        val recJson = JSONObject()
        records.forEach { (id, rec) ->
            recJson.put(id, rec.status.name)
        }
        root.put("records", recJson)
        val logJson = JSONObject()
        studyLog.forEach { (date, item) ->
            logJson.put(
                date,
                JSONObject()
                    .put("mathCount", item.mathCount)
                    .put("chineseCount", item.chineseCount)
                    .put("englishCount", item.englishCount)
                    .put("minutes", item.minutes)
                    .put("note", item.note)
            )
        }
        root.put("studyLog", logJson)

        val dir = appContext.getExternalFilesDir(null) ?: appContext.filesDir
        val outFile = File(dir, "grade2_progress_backup.json")
        outFile.writeText(root.toString(2))
        return outFile.absolutePath
    }

    private fun deleteBackupFile() {
        val dir = appContext.getExternalFilesDir(null) ?: appContext.filesDir
        val backupFile = File(dir, "grade2_progress_backup.json")
        if (backupFile.exists()) {
            backupFile.delete()
        }
    }

    private fun childPrefix(childId: String): String = "c_${childId}_"

    private fun encodeChildren(children: List<ChildSummary>): String {
        val arr = JSONArray()
        children.forEach { c ->
            arr.put(
                JSONObject()
                    .put("id", c.id)
                    .put("name", c.name)
                    .put("grade", c.grade)
                    .put("avatarTone", c.avatarTone)
                    .put("defaultTerm", c.defaultTerm.key)
                    .put("masteryStreak", c.masteryStreak)
                    .put("englishAudioMode", c.englishAudioMode.key)
                    .put("speechRate", c.speechRate.toDouble())
                    .put("resetVersion", c.resetVersion)
                    .put("updatedAtMs", c.updatedAtMs)
            )
        }
        return arr.toString()
    }

    private fun decodeChildren(raw: String): List<ChildSummary> = runCatching {
        val arr = JSONArray(raw)
        val list = ArrayList<ChildSummary>(arr.length())
        for (i in 0 until arr.length()) {
            val c = arr.getJSONObject(i)
            list.add(
                ChildSummary(
                    id = c.getString("id"),
                    name = c.optString("name", "小宇"),
                    grade = c.optString("grade", "二年级"),
                    avatarTone = c.optString("avatarTone", "amber"),
                    defaultTerm = TermFilter.fromKey(c.optString("defaultTerm", TermFilter.UPPER.key)),
                    masteryStreak = c.optInt("masteryStreak", 2).coerceIn(2, 3),
                    englishAudioMode = EnglishAudioMode.fromKey(c.optString("englishAudioMode")),
                    speechRate = c.optDouble("speechRate", 0.92).toFloat().coerceIn(0.75f, 1.25f),
                    resetVersion = c.optInt("resetVersion", 1),
                    updatedAtMs = c.optLong("updatedAtMs", 0L)
                )
            )
        }
        list
    }.getOrDefault(emptyList())

    private fun decodeRecords(raw: String): Map<String, TopicRecord> {
        val obj = JSONObject(raw)
        val result = linkedMapOf<String, TopicRecord>()
        val keys = obj.keys()
        while (keys.hasNext()) {
            val id = keys.next()
            val item = obj.getJSONObject(id)
            val status = runCatching { NodeStatus.valueOf(item.getString("status")) }.getOrDefault(NodeStatus.UNKNOWN)
            val lastQ = if (item.isNull("lastQuestionId")) null else item.optString("lastQuestionId")
            result[id] = TopicRecord(
                status = status,
                consecutiveCorrect = item.optInt("consecutiveCorrect", 0),
                attempts = item.optInt("attempts", 0),
                wrongCount = item.optInt("wrongCount", 0),
                lastQuestionId = lastQ,
                updatedAtMs = item.optLong("updatedAtMs", 0L)
            )
        }
        return result
    }

    private fun decodeStudyLog(raw: String): Map<String, DayStudyRecord> {
        val obj = JSONObject(raw)
        val result = linkedMapOf<String, DayStudyRecord>()
        val keys = obj.keys()
        while (keys.hasNext()) {
            val date = keys.next()
            if (!date.contains('-')) continue
            val item = obj.getJSONObject(date)
            result[date] = DayStudyRecord(
                studyDate = date,
                mathCount = item.optInt("mathCount", 0),
                chineseCount = item.optInt("chineseCount", 0),
                englishCount = item.optInt("englishCount", 0),
                minutes = item.optInt("minutes", 0),
                note = item.optString("note", ""),
                updatedAtMs = item.optLong("updatedAtMs", 0L)
            )
        }
        return result
    }

    companion object {
        private const val KEY_AUTH_TOKEN = "auth_token"
        private const val KEY_AUTH_USER_ID = "auth_user_id"
        private const val KEY_AUTH_USERNAME = "auth_username"
        private const val KEY_AUTH_CHILDREN = "auth_children"
        private const val KEY_AUTH_ACTIVE_CHILD_ID = "auth_active_child_id"

        private const val KEY_PROFILE_NAME = "profile_name"
        private const val KEY_PROFILE_GRADE = "profile_grade"
        private const val KEY_PROFILE_TONE = "profile_tone"
        private const val KEY_PROFILE_TERM = "profile_term"
        private const val KEY_PROFILE_UPDATED_AT = "profile_updated_at"
        private const val KEY_MASTERY_STREAK = "mastery_streak"
        private const val KEY_EN_AUDIO_MODE = "en_audio_mode"
        private const val KEY_SPEECH_RATE = "speech_rate"
        private const val KEY_SETTINGS_UPDATED_AT = "settings_updated_at"
        private const val KEY_RESET_VERSION = "reset_version"
        private const val KEY_RECORDS_JSON = "records_json"
        private const val KEY_STUDY_LOG_JSON = "study_log_json"
        private const val KEY_PENDING_EVENTS = "pending_events_json"
        private const val KEY_PENDING_PATCH = "pending_patch_json"
    }
}
