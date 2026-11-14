package com.study.grade2learning.data

import com.study.grade2learning.BuildConfig
import com.study.grade2learning.model.ChildSnapshot
import com.study.grade2learning.model.ChildSummary
import com.study.grade2learning.model.DayStudyRecord
import com.study.grade2learning.model.EnglishAudioMode
import com.study.grade2learning.model.NodeStatus
import com.study.grade2learning.model.QuizSyncEvent
import com.study.grade2learning.model.TermFilter
import com.study.grade2learning.model.TopicRecord
import com.study.grade2learning.model.UserDto
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

class CloudApiException(
    val statusCode: Int,
    override val message: String
) : Exception(message)

data class AuthApiResult(
    val token: String,
    val user: UserDto,
    val children: List<ChildSummary>
)

data class MeApiResult(
    val user: UserDto,
    val children: List<ChildSummary>
)

data class ChildPatchRequest(
    val name: String? = null,
    val grade: String? = null,
    val avatarTone: String? = null,
    val defaultTerm: TermFilter? = null,
    val masteryStreak: Int? = null,
    val englishAudioMode: EnglishAudioMode? = null,
    val speechRate: Float? = null,
    val updatedAtMs: Long = System.currentTimeMillis()
)

data class SyncApiResult(
    val status: String,
    val snapshot: ChildSnapshot
)

class CloudApiClient(
    private val baseUrl: String = BuildConfig.API_BASE_URL.trimEnd('/')
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    fun register(
        username: String,
        password: String,
        initialChildName: String,
        initialGrade: String
    ): AuthApiResult {
        val body = JSONObject().apply {
            put("username", username)
            put("password", password)
            put("initialChildName", initialChildName)
            put("initialGrade", initialGrade)
        }
        val json = executeJson("POST", "/api/v1/auth/register", body = body)
        return parseAuthResult(json)
    }

    fun login(username: String, password: String): AuthApiResult {
        val body = JSONObject().apply {
            put("username", username)
            put("password", password)
        }
        val json = executeJson("POST", "/api/v1/auth/login", body = body)
        return parseAuthResult(json)
    }

    fun me(token: String): MeApiResult {
        val json = executeJson("GET", "/api/v1/auth/me", token = token)
        return MeApiResult(
            user = parseUser(json.getJSONObject("user")),
            children = parseChildren(json.getJSONArray("children"))
        )
    }

    fun createChild(
        token: String,
        name: String,
        grade: String,
        avatarTone: String,
        defaultTerm: TermFilter
    ): ChildSummary {
        val body = JSONObject().apply {
            put("name", name)
            put("grade", grade)
            put("avatarTone", avatarTone)
            put("defaultTerm", defaultTerm.key)
        }
        val json = executeJson("POST", "/api/v1/children", token = token, body = body)
        return parseChild(json)
    }

    fun fetchSnapshot(token: String, childId: String): ChildSnapshot {
        val json = executeJson("GET", "/api/v1/children/$childId/snapshot", token = token)
        return parseSnapshot(json)
    }

    fun syncChild(
        token: String,
        childId: String,
        clientResetVersion: Int,
        profilePatch: ChildPatchRequest?,
        records: Map<String, TopicRecord>,
        quizEvents: List<QuizSyncEvent>
    ): SyncApiResult {
        val body = JSONObject().apply {
            put("clientResetVersion", clientResetVersion)
            if (profilePatch != null) {
                put("profilePatch", JSONObject().apply {
                    profilePatch.name?.let { put("name", it) }
                    profilePatch.grade?.let { put("grade", it) }
                    profilePatch.avatarTone?.let { put("avatarTone", it) }
                    profilePatch.defaultTerm?.let { put("defaultTerm", it.key) }
                    profilePatch.masteryStreak?.let { put("masteryStreak", it) }
                    profilePatch.englishAudioMode?.let { put("englishAudioMode", it.key) }
                    profilePatch.speechRate?.let { put("speechRate", it.toDouble()) }
                    put("updatedAtMs", profilePatch.updatedAtMs)
                })
            }
            put("records", JSONArray().apply {
                records.forEach { (topicId, rec) ->
                    put(JSONObject().apply {
                        put("topicId", topicId)
                        put("status", rec.status.name)
                        put("consecutiveCorrect", rec.consecutiveCorrect)
                        put("attempts", rec.attempts)
                        put("wrongCount", rec.wrongCount)
                        if (rec.lastQuestionId != null) put("lastQuestionId", rec.lastQuestionId)
                        put("updatedAtMs", rec.updatedAtMs)
                    })
                }
            })
            put("quizEvents", JSONArray().apply {
                quizEvents.forEach { ev ->
                    put(JSONObject().apply {
                        put("eventId", ev.eventId)
                        put("subject", ev.subject.key)
                        put("topicId", ev.topicId)
                        put("questionId", ev.questionId)
                        put("isCorrect", ev.isCorrect)
                        put("isSkipped", ev.isSkipped)
                        put("studyDate", ev.studyDate)
                        put("note", ev.note)
                        put("createdAtMs", ev.createdAtMs)
                    })
                }
            })
        }
        val json = executeJson("POST", "/api/v1/children/$childId/sync", token = token, body = body)
        return SyncApiResult(
            status = json.optString("status", "ok"),
            snapshot = parseSnapshot(json.getJSONObject("snapshot"))
        )
    }

    fun resetChild(token: String, childId: String, accountPassword: String): SyncApiResult {
        val body = JSONObject().apply {
            put("accountPassword", accountPassword)
        }
        val json = executeJson("POST", "/api/v1/children/$childId/reset", token = token, body = body)
        return SyncApiResult(
            status = json.optString("status", "ok"),
            snapshot = parseSnapshot(json.getJSONObject("snapshot"))
        )
    }

    private fun executeJson(
        method: String,
        path: String,
        token: String? = null,
        body: JSONObject? = null
    ): JSONObject {
        val builder = Request.Builder().url("$baseUrl$path")
        if (!token.isNullOrBlank()) {
            builder.header("Authorization", "Bearer $token")
        }
        val requestBody = body?.toString()?.toRequestBody(jsonMediaType)
        when (method) {
            "GET" -> builder.get()
            "POST" -> builder.post(requestBody ?: "{}".toRequestBody(jsonMediaType))
            "PATCH" -> builder.patch(requestBody ?: "{}".toRequestBody(jsonMediaType))
            else -> builder.method(method, requestBody)
        }

        val response = try {
            client.newCall(builder.build()).execute()
        } catch (e: IOException) {
            throw CloudApiException(0, "网络连接失败，请检查平板网络后重试")
        }

        response.use { resp ->
            val raw = resp.body?.string().orEmpty()
            if (!resp.isSuccessful) {
                val detail = runCatching {
                    JSONObject(raw).optString("detail")
                }.getOrNull().orEmpty()
                val msg = when {
                    detail.isNotBlank() -> detail
                    resp.code == 401 -> "登录已过期，请重新登录"
                    resp.code == 403 -> "验证未通过，请检查账号密码"
                    else -> "服务器响应异常 (${resp.code})"
                }
                throw CloudApiException(resp.code, msg)
            }
            return if (raw.isBlank()) JSONObject() else JSONObject(raw)
        }
    }

    private fun parseAuthResult(json: JSONObject): AuthApiResult = AuthApiResult(
        token = json.getString("token"),
        user = parseUser(json.getJSONObject("user")),
        children = parseChildren(json.getJSONArray("children"))
    )

    private fun parseUser(json: JSONObject): UserDto = UserDto(
        id = json.getString("id"),
        username = json.getString("username")
    )

    private fun parseChildren(array: JSONArray): List<ChildSummary> {
        val list = ArrayList<ChildSummary>(array.length())
        for (i in 0 until array.length()) {
            list.add(parseChild(array.getJSONObject(i)))
        }
        return list
    }

    private fun parseChild(json: JSONObject): ChildSummary = ChildSummary(
        id = json.getString("id"),
        name = json.optString("name", "小宇"),
        grade = json.optString("grade", "二年级"),
        avatarTone = json.optString("avatarTone", "amber"),
        defaultTerm = TermFilter.fromKey(json.optString("defaultTerm", TermFilter.UPPER.key)),
        masteryStreak = json.optInt("masteryStreak", 2).coerceIn(2, 3),
        englishAudioMode = EnglishAudioMode.fromKey(json.optString("englishAudioMode")),
        speechRate = json.optDouble("speechRate", 0.92).toFloat().coerceIn(0.75f, 1.25f),
        resetVersion = json.optInt("resetVersion", 1),
        updatedAtMs = json.optLong("updatedAtMs", 0L)
    )

    private fun parseSnapshot(json: JSONObject): ChildSnapshot {
        val child = parseChild(json.getJSONObject("child"))
        val recordsArr = json.optJSONArray("records") ?: JSONArray()
        val records = LinkedHashMap<String, TopicRecord>(recordsArr.length())
        for (i in 0 until recordsArr.length()) {
            val item = recordsArr.getJSONObject(i)
            val topicId = item.getString("topicId")
            val status = runCatching {
                NodeStatus.valueOf(item.getString("status"))
            }.getOrDefault(NodeStatus.UNKNOWN)
            records[topicId] = TopicRecord(
                status = status,
                consecutiveCorrect = item.optInt("consecutiveCorrect", 0),
                attempts = item.optInt("attempts", 0),
                wrongCount = item.optInt("wrongCount", 0),
                lastQuestionId = item.optString("lastQuestionId").takeIf { it.isNotBlank() && it != "null" },
                updatedAtMs = item.optLong("updatedAtMs", 0L)
            )
        }

        val logArr = json.optJSONArray("studyLog") ?: JSONArray()
        val studyLog = LinkedHashMap<String, DayStudyRecord>(logArr.length())
        for (i in 0 until logArr.length()) {
            val item = logArr.getJSONObject(i)
            val date = item.getString("studyDate")
            studyLog[date] = DayStudyRecord(
                studyDate = date,
                mathCount = item.optInt("mathCount", 0),
                chineseCount = item.optInt("chineseCount", 0),
                englishCount = item.optInt("englishCount", 0),
                minutes = item.optInt("minutes", 0),
                note = item.optString("note", ""),
                updatedAtMs = item.optLong("updatedAtMs", 0L)
            )
        }

        return ChildSnapshot(
            child = child,
            records = records,
            studyLog = studyLog,
            serverTimeMs = json.optLong("serverTimeMs", 0L)
        )
    }
}
