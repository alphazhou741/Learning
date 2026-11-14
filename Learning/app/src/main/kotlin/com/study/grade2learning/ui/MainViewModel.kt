package com.study.grade2learning.ui

import android.app.Application
import androidx.compose.runtime.Immutable
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.study.grade2learning.data.ChildPatchRequest
import com.study.grade2learning.data.CloudApiClient
import com.study.grade2learning.data.CloudApiException
import com.study.grade2learning.data.EnglishAudioPlayer
import com.study.grade2learning.data.StudyRepository
import com.study.grade2learning.engine.AnswerOutcome
import com.study.grade2learning.engine.DiagEngine
import com.study.grade2learning.model.AuthSession
import com.study.grade2learning.model.ChildSummary
import com.study.grade2learning.model.DayStudyRecord
import com.study.grade2learning.model.EnglishAudioMode
import com.study.grade2learning.model.Grade2Bundle
import com.study.grade2learning.model.NodeStatus
import com.study.grade2learning.model.ParentSettings
import com.study.grade2learning.model.QuestionItem
import com.study.grade2learning.model.QuizSyncEvent
import com.study.grade2learning.model.RemediationStep
import com.study.grade2learning.model.SubjectDataset
import com.study.grade2learning.model.SubjectKey
import com.study.grade2learning.model.SyncState
import com.study.grade2learning.model.TermFilter
import com.study.grade2learning.model.TopicNode
import com.study.grade2learning.model.TopicRecord
import com.study.grade2learning.model.UnitGroup
import com.study.grade2learning.model.UserProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.util.UUID

enum class AppScreen {
    HOME, UNITS, QUIZ, REPORT, PARENT
}

@Immutable
data class UiState(
    val isLoading: Boolean = true,
    val isLoggedIn: Boolean = false,
    val preferRegisterTab: Boolean = true,
    val username: String = "",
    val children: List<ChildSummary> = emptyList(),
    val activeChildId: String = "",
    val resetVersion: Int = 1,
    val syncState: SyncState = SyncState.IDLE,
    val pendingSyncCount: Int = 0,
    val authLoading: Boolean = false,
    val authError: String = "",
    val resetInProgress: Boolean = false,
    val resetError: String = "",
    val screen: AppScreen = AppScreen.HOME,
    val subject: SubjectKey = SubjectKey.MATH,
    val term: TermFilter = TermFilter.UPPER,
    val profile: UserProfile = UserProfile(),
    val showProfileDialog: Boolean = false,
    val parentSettings: ParentSettings = ParentSettings(),
    val parentUnlocked: Boolean = false,
    val parentGateError: Boolean = false,
    val parentNotice: String = "",
    val reportParentMode: Boolean = true,
    val selectedDate: String = LocalDate.now().toString(),
    val selectedUnitIdx: Int = 0,
    val records: Map<String, TopicRecord> = emptyMap(),
    val studyLog: Map<String, DayStudyRecord> = emptyMap(),
    val bundle: Grade2Bundle? = null,
    // Active Quiz State
    val sessionTopics: List<TopicNode> = emptyList(),
    val sessionTopicIdx: Int = 0,
    val currentQuestion: QuestionItem? = null,
    val askedQuestionIds: Set<String> = emptySet(),
    val selectedChoiceKey: String? = null,
    val numericInput: String = "",
    val answerOutcome: AnswerOutcome? = null,
    val scratchResetSeed: Int = 0,
    val audioToastMessage: String? = null
) {
    val activeDataset: SubjectDataset?
        get() = bundle?.subjects?.get(subject)
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = StudyRepository(application)
    private val cloud = CloudApiClient()
    private val audioPlayer = EnglishAudioPlayer(application)
    private val engines = mutableMapOf<SubjectKey, DiagEngine>()
    private val queueLock = Any()
    private val syncMutex = Mutex()

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val bundle = withContext(Dispatchers.IO) { repo.loadBundle() }
            bundle.subjects.forEach { (k, ds) -> engines[k] = DiagEngine(ds) }
            val session = withContext(Dispatchers.IO) { repo.loadAuthSession() }

            if (session == null) {
                val firstInstall = withContext(Dispatchers.IO) { !repo.hasLoggedInBefore() }
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isLoggedIn = false,
                        preferRegisterTab = firstInstall,
                        bundle = bundle,
                        selectedDate = LocalDate.now().toString()
                    )
                }
            } else {
                hydrateFromLocalCache(session = session, bundle = bundle)
                syncActiveChild(refreshChildren = true)
            }
        }
    }

    private fun hydrateFromLocalCache(session: AuthSession, bundle: Grade2Bundle?) {
        val activeChild = session.children.firstOrNull { it.id == session.activeChildId } ?: session.children.first()
        val cid = activeChild.id
        val profile = repo.loadProfile(cid, activeChild.toUserProfile())
        val settings = repo.loadParentSettings(cid, activeChild.toParentSettings())
        val resetVer = repo.loadResetVersion(cid, activeChild.resetVersion)
        val records = repo.loadRecords(cid)
        val log = repo.loadStudyLog(cid)
        val pendingCount = synchronized(queueLock) {
            repo.loadPendingEvents(cid).size + if (repo.loadPendingProfilePatch(cid) != null) 1 else 0
        }
        _uiState.update {
            it.copy(
                isLoading = false,
                isLoggedIn = true,
                username = session.user.username,
                children = session.children,
                activeChildId = cid,
                resetVersion = resetVer,
                syncState = if (pendingCount > 0) SyncState.PENDING else SyncState.IDLE,
                pendingSyncCount = pendingCount,
                authLoading = false,
                authError = "",
                bundle = bundle ?: it.bundle,
                profile = profile,
                term = profile.defaultTerm,
                parentSettings = settings,
                records = records,
                studyLog = log,
                selectedDate = LocalDate.now().toString(),
                selectedUnitIdx = 0
            )
        }
    }

    fun clearAuthError() {
        _uiState.update { it.copy(authError = "") }
    }

    fun login(username: String, password: String) {
        val cleanUser = username.trim()
        if (cleanUser.length < 2 || password.isEmpty()) {
            _uiState.update { it.copy(authError = "请输入至少 2 位用户名和密码") }
            return
        }
        _uiState.update { it.copy(authLoading = true, authError = "") }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val res = cloud.login(cleanUser, password)
                val firstChild = res.children.firstOrNull()
                    ?: throw CloudApiException(500, "账号下暂无孩子档案")
                val session = AuthSession(
                    token = res.token,
                    user = res.user,
                    children = res.children,
                    activeChildId = firstChild.id
                )
                repo.clearAllLocalState()
                repo.saveAuthSession(session)
                val snapshot = cloud.fetchSnapshot(res.token, firstChild.id)
                repo.saveSnapshot(snapshot)
                hydrateFromLocalCache(session = session, bundle = null)
            } catch (e: CloudApiException) {
                _uiState.update { it.copy(authLoading = false, authError = e.message) }
            } catch (e: Exception) {
                _uiState.update { it.copy(authLoading = false, authError = "登录失败，请稍后重试") }
            }
        }
    }

    fun register(username: String, password: String, childName: String, childGrade: String) {
        val cleanUser = username.trim()
        val cleanChild = childName.trim().ifEmpty { "小宇" }.take(12)
        if (cleanUser.length < 2) {
            _uiState.update { it.copy(authError = "用户名至少 2 个字符") }
            return
        }
        if (password.length < 6) {
            _uiState.update { it.copy(authError = "密码至少 6 位") }
            return
        }
        _uiState.update { it.copy(authLoading = true, authError = "") }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val res = cloud.register(cleanUser, password, cleanChild, childGrade)
                val firstChild = res.children.firstOrNull()
                    ?: throw CloudApiException(500, "初始化孩子档案失败")
                val session = AuthSession(
                    token = res.token,
                    user = res.user,
                    children = res.children,
                    activeChildId = firstChild.id
                )
                repo.clearAllLocalState()
                repo.saveAuthSession(session)
                val snapshot = cloud.fetchSnapshot(res.token, firstChild.id)
                repo.saveSnapshot(snapshot)
                hydrateFromLocalCache(session = session, bundle = null)
            } catch (e: CloudApiException) {
                _uiState.update { it.copy(authLoading = false, authError = e.message) }
            } catch (e: Exception) {
                _uiState.update { it.copy(authLoading = false, authError = "注册失败，请稍后重试") }
            }
        }
    }

    fun logout() {
        audioPlayer.stopPlayback()
        val firstInstall = !repo.hasLoggedInBefore()
        viewModelScope.launch(Dispatchers.IO) {
            repo.clearAllLocalState()
        }
        _uiState.update { st ->
            UiState(
                isLoading = false,
                isLoggedIn = false,
                preferRegisterTab = firstInstall,
                bundle = st.bundle,
                selectedDate = LocalDate.now().toString()
            )
        }
    }

    fun syncNow() {
        syncActiveChild(refreshChildren = true)
    }

    fun engineFor(subjectKey: SubjectKey): DiagEngine? = engines[subjectKey]

    fun navigateTo(screen: AppScreen, subject: SubjectKey? = null) {
        audioPlayer.stopPlayback()
        _uiState.update { st ->
            val nextSubject = subject ?: st.subject
            val nextState = st.copy(
                screen = screen,
                subject = nextSubject,
                selectedUnitIdx = if (subject != null && subject != st.subject) 0 else st.selectedUnitIdx,
                parentUnlocked = if (screen == AppScreen.PARENT) st.parentUnlocked else false,
                parentGateError = if (screen == AppScreen.PARENT) st.parentGateError else false,
                parentNotice = ""
            )
            if (screen == AppScreen.QUIZ && nextState.currentQuestion == null) {
                prepareAdaptiveSession(nextState, nextSubject)
            } else {
                nextState
            }
        }
    }

    fun setTerm(term: TermFilter) {
        _uiState.update { st ->
            st.copy(term = term, selectedUnitIdx = 0)
        }
    }

    fun selectDate(date: String) {
        _uiState.update { it.copy(selectedDate = date) }
    }

    fun selectUnit(index: Int) {
        _uiState.update { it.copy(selectedUnitIdx = index.coerceAtLeast(0)) }
    }

    fun setReportParentMode(parentMode: Boolean) {
        _uiState.update { it.copy(reportParentMode = parentMode) }
    }

    fun openProfileDialog() {
        _uiState.update { it.copy(showProfileDialog = true) }
    }

    fun closeProfileDialog() {
        _uiState.update { it.copy(showProfileDialog = false) }
    }

    fun switchChild(childId: String) {
        audioPlayer.stopPlayback()
        val session = repo.loadAuthSession() ?: return
        if (session.children.none { it.id == childId }) return
        val updatedSession = session.copy(activeChildId = childId)
        repo.saveAuthSession(updatedSession)
        _uiState.update {
            it.copy(
                showProfileDialog = false,
                screen = if (it.screen == AppScreen.QUIZ) AppScreen.HOME else it.screen,
                sessionTopics = emptyList(),
                sessionTopicIdx = 0,
                currentQuestion = null,
                askedQuestionIds = emptySet(),
                selectedChoiceKey = null,
                numericInput = "",
                answerOutcome = null
            )
        }
        hydrateFromLocalCache(session = updatedSession, bundle = null)
        syncActiveChild(refreshChildren = false)
    }

    fun createChild(
        name: String,
        grade: String,
        avatarTone: String,
        defaultTerm: TermFilter
    ) {
        val cleanName = name.trim().ifEmpty { "宝贝" }.take(12)
        val session = repo.loadAuthSession() ?: return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val created = cloud.createChild(
                    token = session.token,
                    name = cleanName,
                    grade = grade,
                    avatarTone = avatarTone,
                    defaultTerm = defaultTerm
                )
                val updatedChildren = session.children + created
                val updatedSession = session.copy(children = updatedChildren, activeChildId = created.id)
                repo.saveAuthSession(updatedSession)
                val snapshot = cloud.fetchSnapshot(session.token, created.id)
                repo.saveSnapshot(snapshot)
                _uiState.update {
                    it.copy(
                        showProfileDialog = false,
                        sessionTopics = emptyList(),
                        sessionTopicIdx = 0,
                        currentQuestion = null,
                        askedQuestionIds = emptySet(),
                        selectedChoiceKey = null,
                        numericInput = "",
                        answerOutcome = null,
                        parentNotice = "已新增学习者「${created.name}」并切换为当前档案。"
                    )
                }
                hydrateFromLocalCache(session = updatedSession, bundle = null)
            } catch (e: CloudApiException) {
                showAudioToast(if (e.statusCode == 0) "新增学习者需联网保存，请检查网络" else e.message)
            }
        }
    }

    fun saveProfile(name: String, grade: String, avatarTone: String, defaultTerm: TermFilter) {
        val childId = _uiState.value.activeChildId
        if (childId.isBlank()) return
        val now = System.currentTimeMillis()
        val cleanName = name.trim().ifEmpty { "小宇" }.take(8)
        val updated = UserProfile(
            name = cleanName,
            grade = grade,
            avatarTone = avatarTone,
            defaultTerm = defaultTerm,
            updatedAtMs = now
        )
        repo.saveProfile(childId, updated)
        enqueueProfilePatch(
            childId = childId,
            patch = ChildPatchRequest(
                name = cleanName,
                grade = grade,
                avatarTone = avatarTone,
                defaultTerm = defaultTerm,
                updatedAtMs = now
            )
        )
        _uiState.update {
            it.copy(
                profile = updated,
                children = it.children.map { child ->
                    if (child.id == childId) {
                        child.copy(
                            name = cleanName,
                            grade = grade,
                            avatarTone = avatarTone,
                            defaultTerm = defaultTerm,
                            updatedAtMs = now
                        )
                    } else child
                },
                term = defaultTerm,
                selectedUnitIdx = 0,
                showProfileDialog = false
            )
        }
        syncActiveChild(refreshChildren = false)
    }

    fun startAdaptiveQuiz(subject: SubjectKey) {
        audioPlayer.stopPlayback()
        _uiState.update { st ->
            prepareAdaptiveSession(st.copy(screen = AppScreen.QUIZ, subject = subject), subject)
        }
    }

    fun startTopicPractice(subject: SubjectKey, topicId: String) {
        audioPlayer.stopPlayback()
        val eng = engines[subject] ?: return
        val topic = eng.topicById[topicId] ?: return
        _uiState.update { st ->
            val otherQueue = eng.pickAdaptiveSessionTopics(st.term.key, st.records, limit = 6)
                .filter { it.id != topic.id }
            val session = (listOf(topic) + otherQueue).take(5)
            val q = eng.pickQuestion(topic.id, st.records)
            st.copy(
                screen = AppScreen.QUIZ,
                subject = subject,
                sessionTopics = session,
                sessionTopicIdx = 0,
                currentQuestion = q,
                askedQuestionIds = q?.let { setOf(it.id) } ?: emptySet(),
                selectedChoiceKey = null,
                numericInput = "",
                answerOutcome = null,
                scratchResetSeed = st.scratchResetSeed + 1
            )
        }
    }

    fun startUnitQuiz(unit: UnitGroup) {
        audioPlayer.stopPlayback()
        val eng = engines[unit.subject] ?: return
        _uiState.update { st ->
            val session = unit.topics.take(5)
            val firstTopic = session.firstOrNull() ?: return@update st
            val q = eng.pickQuestion(firstTopic.id, st.records)
            st.copy(
                screen = AppScreen.QUIZ,
                subject = unit.subject,
                sessionTopics = session,
                sessionTopicIdx = 0,
                currentQuestion = q,
                askedQuestionIds = q?.let { setOf(it.id) } ?: emptySet(),
                selectedChoiceKey = null,
                numericInput = "",
                answerOutcome = null,
                scratchResetSeed = st.scratchResetSeed + 1
            )
        }
    }

    private fun prepareAdaptiveSession(st: UiState, subject: SubjectKey): UiState {
        val eng = engines[subject] ?: return st
        val session = eng.pickAdaptiveSessionTopics(st.term.key, st.records, limit = 5)
        val firstTopic = session.firstOrNull() ?: return st
        val q = eng.pickQuestion(firstTopic.id, st.records)
        return st.copy(
            sessionTopics = session,
            sessionTopicIdx = 0,
            currentQuestion = q,
            askedQuestionIds = q?.let { setOf(it.id) } ?: emptySet(),
            selectedChoiceKey = null,
            numericInput = "",
            answerOutcome = null,
            scratchResetSeed = st.scratchResetSeed + 1
        )
    }

    fun selectChoiceAndSubmit(choiceKey: String) {
        val st = _uiState.value
        if (st.answerOutcome != null) return
        val q = st.currentQuestion ?: return
        submitRawAnswer(choiceKey, selectedChoice = choiceKey)
    }

    fun pressKeypadKey(key: String) {
        val st = _uiState.value
        if (st.answerOutcome != null) return
        when (key) {
            "DEL" -> _uiState.update { it.copy(numericInput = it.numericInput.dropLast(1)) }
            "OK" -> if (st.numericInput.isNotBlank()) submitRawAnswer(st.numericInput, selectedChoice = null)
            else -> if (st.numericInput.length < 12) {
                _uiState.update { it.copy(numericInput = it.numericInput + key) }
            }
        }
    }

    fun skipCurrentQuestion() {
        val st = _uiState.value
        if (st.answerOutcome != null) return
        val q = st.currentQuestion ?: return
        val eng = engines[q.subject] ?: return
        val (updatedRecords, outcome) = eng.skipQuestion(
            topicId = q.topicId,
            question = q,
            records = st.records,
            masteryStreak = st.parentSettings.masteryStreak
        )
        val stampedRecords = stampTopicRecord(updatedRecords, q.topicId)
        persistProgressAndLog(q, outcome, stampedRecords)
        _uiState.update {
            it.copy(
                records = stampedRecords,
                answerOutcome = outcome
            )
        }
    }

    private fun submitRawAnswer(rawInput: String, selectedChoice: String?) {
        val st = _uiState.value
        val q = st.currentQuestion ?: return
        val eng = engines[q.subject] ?: return
        val (updatedRecords, outcome) = eng.applyAnswer(
            topicId = q.topicId,
            question = q,
            rawInput = rawInput,
            records = st.records,
            masteryStreak = st.parentSettings.masteryStreak
        )
        val stampedRecords = stampTopicRecord(updatedRecords, q.topicId)
        persistProgressAndLog(q, outcome, stampedRecords)
        _uiState.update {
            it.copy(
                records = stampedRecords,
                selectedChoiceKey = selectedChoice,
                answerOutcome = outcome
            )
        }
    }

    private fun stampTopicRecord(
        records: Map<String, TopicRecord>,
        topicId: String
    ): Map<String, TopicRecord> {
        val prev = records[topicId] ?: return records
        val now = maxOf(System.currentTimeMillis(), prev.updatedAtMs + 1L)
        return records.toMutableMap().apply {
            this[topicId] = prev.copy(updatedAtMs = now)
        }
    }

    private fun persistProgressAndLog(
        question: QuestionItem,
        outcome: AnswerOutcome,
        updatedRecords: Map<String, TopicRecord>
    ) {
        val childId = _uiState.value.activeChildId
        if (childId.isBlank()) return
        val todayStr = LocalDate.now().toString()
        val now = System.currentTimeMillis()
        val currentLog = _uiState.value.studyLog.toMutableMap()
        val prevDay = currentLog[todayStr] ?: DayStudyRecord(studyDate = todayStr, note = "自主自适应练习")
        val nextDay = when (question.subject) {
            SubjectKey.MATH -> prevDay.copy(
                mathCount = prevDay.mathCount + 1,
                minutes = (prevDay.minutes + 2).coerceAtLeast(2),
                updatedAtMs = now
            )
            SubjectKey.CHINESE -> prevDay.copy(
                chineseCount = prevDay.chineseCount + 1,
                minutes = (prevDay.minutes + 2).coerceAtLeast(2),
                updatedAtMs = now
            )
            SubjectKey.ENGLISH -> prevDay.copy(
                englishCount = prevDay.englishCount + 1,
                minutes = (prevDay.minutes + 2).coerceAtLeast(2),
                updatedAtMs = now
            )
        }
        currentLog[todayStr] = nextDay

        val syncEvent = QuizSyncEvent(
            eventId = UUID.randomUUID().toString(),
            subject = question.subject,
            topicId = question.topicId,
            questionId = question.id,
            isCorrect = outcome.isCorrect,
            isSkipped = outcome.isSkipped,
            studyDate = todayStr,
            note = "自主自适应练习",
            createdAtMs = now
        )

        val pendingCount = synchronized(queueLock) {
            repo.saveRecords(childId, updatedRecords)
            repo.saveStudyLog(childId, currentLog)
            val nextEvents = repo.loadPendingEvents(childId) + syncEvent
            repo.savePendingEvents(childId, nextEvents)
            nextEvents.size + if (repo.loadPendingProfilePatch(childId) != null) 1 else 0
        }

        _uiState.update {
            it.copy(
                studyLog = currentLog,
                selectedDate = todayStr,
                pendingSyncCount = pendingCount,
                syncState = SyncState.PENDING
            )
        }
        syncActiveChild(refreshChildren = false)
    }

    fun nextQuizStep() {
        audioPlayer.stopPlayback()
        val st = _uiState.value
        val eng = engines[st.subject] ?: return
        val outcome = st.answerOutcome

        // If backtracking to a Grade 1 prerequisite was suggested, insert/jump smoothly!
        val suggestedPrereq = outcome?.suggestedPrereqTopicId?.let { eng.topicById[it] }
        if (outcome != null && !outcome.isCorrect && suggestedPrereq != null &&
            st.sessionTopics.none { it.id == suggestedPrereq.id }
        ) {
            val nextQueue = st.sessionTopics.toMutableList()
            val insertAt = (st.sessionTopicIdx + 1).coerceAtMost(nextQueue.size)
            nextQueue.add(insertAt, suggestedPrereq)
            val nextQ = eng.pickQuestion(suggestedPrereq.id, st.records, st.askedQuestionIds)
            _uiState.update {
                it.copy(
                    sessionTopics = nextQueue,
                    sessionTopicIdx = insertAt,
                    currentQuestion = nextQ,
                    askedQuestionIds = nextQ?.let { q -> st.askedQuestionIds + q.id } ?: st.askedQuestionIds,
                    selectedChoiceKey = null,
                    numericInput = "",
                    answerOutcome = null,
                    scratchResetSeed = st.scratchResetSeed + 1
                )
            }
            return
        }

        // If same topic needs a confirmation question to reach masteryStreak
        val curTopic = st.sessionTopics.getOrNull(st.sessionTopicIdx)
        val curStatus = curTopic?.let { st.records[it.id]?.status }
        if (outcome != null && outcome.isCorrect && curStatus == NodeStatus.SHAKY && curTopic != null) {
            val followQ = eng.pickQuestion(curTopic.id, st.records, st.askedQuestionIds)
            if (followQ != null && followQ.id !in st.askedQuestionIds) {
                _uiState.update {
                    it.copy(
                        currentQuestion = followQ,
                        askedQuestionIds = st.askedQuestionIds + followQ.id,
                        selectedChoiceKey = null,
                        numericInput = "",
                        answerOutcome = null,
                        scratchResetSeed = st.scratchResetSeed + 1
                    )
                }
                return
            }
        }

        // Otherwise move to next topic in queue or wrap into a fresh adaptive set
        val nextIdx = st.sessionTopicIdx + 1
        if (nextIdx < st.sessionTopics.size) {
            val nextTopic = st.sessionTopics[nextIdx]
            val q = eng.pickQuestion(nextTopic.id, st.records, st.askedQuestionIds)
            _uiState.update {
                it.copy(
                    sessionTopicIdx = nextIdx,
                    currentQuestion = q,
                    askedQuestionIds = q?.let { item -> st.askedQuestionIds + item.id } ?: st.askedQuestionIds,
                    selectedChoiceKey = null,
                    numericInput = "",
                    answerOutcome = null,
                    scratchResetSeed = st.scratchResetSeed + 1
                )
            }
        } else {
            _uiState.update { state -> prepareAdaptiveSession(state.copy(askedQuestionIds = emptySet()), state.subject) }
        }
    }

    fun playEnglishPromptAudio() {
        val st = _uiState.value
        val q = st.currentQuestion ?: return
        if (q.subject != SubjectKey.ENGLISH) return
        audioPlayer.playQuestionPrompt(q, st.parentSettings) { msg ->
            showAudioToast(msg)
        }
    }

    fun playEnglishAnswerAudio() {
        val st = _uiState.value
        val q = st.currentQuestion ?: return
        if (q.subject != SubjectKey.ENGLISH) return
        audioPlayer.playAnswerPronunciation(q, st.parentSettings) { msg ->
            showAudioToast(msg)
        }
    }

    private fun showAudioToast(message: String) {
        _uiState.update { it.copy(audioToastMessage = message) }
        viewModelScope.launch {
            delay(2600)
            _uiState.update { st ->
                if (st.audioToastMessage == message) st.copy(audioToastMessage = null) else st
            }
        }
    }

    // Parent Center Actions
    fun verifyParentGate(input: String, bypass: Boolean = false) {
        if (bypass || input.trim() == "43") {
            _uiState.update { it.copy(parentUnlocked = true, parentGateError = false) }
        } else {
            _uiState.update { it.copy(parentGateError = true) }
        }
    }

    fun lockParentCenter() {
        _uiState.update { it.copy(parentUnlocked = false, parentGateError = false, parentNotice = "") }
    }

    fun updateMasteryStreak(streak: Int) {
        val childId = _uiState.value.activeChildId
        if (childId.isBlank()) return
        val now = System.currentTimeMillis()
        val updated = _uiState.value.parentSettings.copy(
            masteryStreak = streak.coerceIn(2, 3),
            updatedAtMs = now
        )
        repo.saveParentSettings(childId, updated)
        enqueueProfilePatch(
            childId = childId,
            patch = ChildPatchRequest(masteryStreak = updated.masteryStreak, updatedAtMs = now)
        )
        _uiState.update {
            it.copy(
                parentSettings = updated,
                parentNotice = "已更新掌握判定规则：连续答对 ${updated.masteryStreak} 题标记为「已掌握」。"
            )
        }
        syncActiveChild(refreshChildren = false)
    }

    fun updateEnglishAudioMode(mode: EnglishAudioMode) {
        val childId = _uiState.value.activeChildId
        if (childId.isBlank()) return
        val now = System.currentTimeMillis()
        val updated = _uiState.value.parentSettings.copy(
            englishAudioMode = mode,
            updatedAtMs = now
        )
        repo.saveParentSettings(childId, updated)
        enqueueProfilePatch(
            childId = childId,
            patch = ChildPatchRequest(englishAudioMode = mode, updatedAtMs = now)
        )
        _uiState.update {
            it.copy(
                parentSettings = updated,
                parentNotice = if (mode == EnglishAudioMode.BUNDLED_WITH_TTS) {
                    "已切换为「预置英文音频 + TTS 兜底」。"
                } else {
                    "已切换为「仅平板系统 Android TTS」。"
                }
            )
        }
        syncActiveChild(refreshChildren = false)
    }

    fun updateSpeechRate(rate: Float) {
        val childId = _uiState.value.activeChildId
        if (childId.isBlank()) return
        val now = System.currentTimeMillis()
        val updated = _uiState.value.parentSettings.copy(
            speechRate = rate,
            updatedAtMs = now
        )
        repo.saveParentSettings(childId, updated)
        enqueueProfilePatch(
            childId = childId,
            patch = ChildPatchRequest(speechRate = rate, updatedAtMs = now)
        )
        _uiState.update {
            it.copy(
                parentSettings = updated,
                parentNotice = "英语点读语速已调整为 ${rate}x。"
            )
        }
        syncActiveChild(refreshChildren = false)
    }

    fun clearParentNotice() {
        _uiState.update { it.copy(parentNotice = "") }
    }

    fun clearResetError() {
        _uiState.update { it.copy(resetError = "") }
    }

    fun exportBackup() {
        val st = _uiState.value
        viewModelScope.launch(Dispatchers.IO) {
            val path = repo.exportBackupJson(st.profile, st.parentSettings, st.records, st.studyLog)
            _uiState.update {
                it.copy(parentNotice = "已导出离线备份文件：$path")
            }
        }
    }

    fun resetAllData(accountPassword: String, onSuccess: () -> Unit = {}) {
        val st = _uiState.value
        if (!st.parentUnlocked) return
        if (accountPassword.isEmpty()) {
            _uiState.update { it.copy(resetError = "请输入登录密码以确认重置") }
            return
        }
        val session = repo.loadAuthSession() ?: return
        val childId = session.activeChildId

        _uiState.update { it.copy(resetInProgress = true, resetError = "") }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                syncMutex.withLock {
                    val res = cloud.resetChild(session.token, childId, accountPassword)
                    synchronized(queueLock) {
                        repo.clearChildLocalProgress(childId, res.snapshot)
                    }
                    val updatedChildren = session.children.map {
                        if (it.id == childId) res.snapshot.child else it
                    }
                    repo.saveAuthSession(session.copy(children = updatedChildren))

                    val freshChild = res.snapshot.child
                    val freshProfile = freshChild.toUserProfile()
                    val freshSettings = freshChild.toParentSettings()
                    _uiState.update { cur ->
                        cur.copy(
                            resetInProgress = false,
                            resetError = "",
                            children = updatedChildren,
                            resetVersion = freshChild.resetVersion,
                            syncState = SyncState.IDLE,
                            pendingSyncCount = 0,
                            profile = freshProfile,
                            term = freshProfile.defaultTerm,
                            parentSettings = freshSettings,
                            selectedDate = LocalDate.now().toString(),
                            selectedUnitIdx = 0,
                            records = emptyMap(),
                            studyLog = emptyMap(),
                            sessionTopics = emptyList(),
                            sessionTopicIdx = 0,
                            currentQuestion = null,
                            askedQuestionIds = emptySet(),
                            selectedChoiceKey = null,
                            numericInput = "",
                            answerOutcome = null,
                            parentNotice = "已验证密码并清空当前孩子在云端与本地的全部学习数据。"
                        )
                    }
                }
                withContext(Dispatchers.Main) { onSuccess() }
            } catch (e: CloudApiException) {
                val msg = if (e.statusCode == 0) {
                    "重置云端数据需要联网验证，请连接网络后重试"
                } else {
                    e.message
                }
                _uiState.update { it.copy(resetInProgress = false, resetError = msg) }
            } catch (e: Exception) {
                _uiState.update { it.copy(resetInProgress = false, resetError = "重置失败，请稍后重试") }
            }
        }
    }

    private fun enqueueProfilePatch(childId: String, patch: ChildPatchRequest) {
        val pendingCount = synchronized(queueLock) {
            val prev = repo.loadPendingProfilePatch(childId)
            val merged = ChildPatchRequest(
                name = patch.name ?: prev?.name,
                grade = patch.grade ?: prev?.grade,
                avatarTone = patch.avatarTone ?: prev?.avatarTone,
                defaultTerm = patch.defaultTerm ?: prev?.defaultTerm,
                masteryStreak = patch.masteryStreak ?: prev?.masteryStreak,
                englishAudioMode = patch.englishAudioMode ?: prev?.englishAudioMode,
                speechRate = patch.speechRate ?: prev?.speechRate,
                updatedAtMs = maxOf(patch.updatedAtMs, prev?.updatedAtMs ?: 0L)
            )
            repo.savePendingProfilePatch(childId, merged)
            repo.loadPendingEvents(childId).size + 1
        }
        _uiState.update {
            it.copy(
                pendingSyncCount = pendingCount,
                syncState = SyncState.PENDING
            )
        }
    }

    private fun syncActiveChild(refreshChildren: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            syncMutex.withLock {
                var session = repo.loadAuthSession() ?: return@withLock
                val targetChildId = session.activeChildId
                _uiState.update { it.copy(syncState = SyncState.SYNCING) }

                try {
                    if (refreshChildren) {
                        val me = cloud.me(session.token)
                        val activeId = me.children.firstOrNull { it.id == targetChildId }?.id
                            ?: me.children.firstOrNull()?.id
                            ?: targetChildId
                        session = session.copy(
                            user = me.user,
                            children = me.children,
                            activeChildId = activeId
                        )
                        repo.saveAuthSession(session)
                    }

                    val cid = session.activeChildId
                    for (attempt in 0 until 3) {
                        val (pendingEvents, pendingPatch, localRecords, resetVer) = synchronized(queueLock) {
                            listOf(
                                repo.loadPendingEvents(cid),
                                repo.loadPendingProfilePatch(cid),
                                repo.loadRecords(cid),
                                repo.loadResetVersion(cid)
                            )
                        }
                        @Suppress("UNCHECKED_CAST")
                        val eventsList = pendingEvents as List<QuizSyncEvent>
                        val patchObj = pendingPatch as ChildPatchRequest?
                        @Suppress("UNCHECKED_CAST")
                        val recordsMap = localRecords as Map<String, TopicRecord>
                        val clientVer = resetVer as Int

                        val res = cloud.syncChild(
                            token = session.token,
                            childId = cid,
                            clientResetVersion = clientVer,
                            profilePatch = patchObj,
                            records = recordsMap,
                            quizEvents = eventsList
                        )

                        var shouldContinue = false
                        var pendingTotal = 0
                        synchronized(queueLock) {
                            if (res.status == "stale_reset") {
                                repo.clearChildLocalProgress(cid, res.snapshot)
                                val updatedChildren = session.children.map {
                                    if (it.id == cid) res.snapshot.child else it
                                }
                                session = session.copy(children = updatedChildren)
                                repo.saveAuthSession(session)
                                if (_uiState.value.activeChildId == cid) {
                                    val freshProfile = res.snapshot.child.toUserProfile()
                                    _uiState.update { cur ->
                                        cur.copy(
                                            children = updatedChildren,
                                            resetVersion = res.snapshot.child.resetVersion,
                                            profile = freshProfile,
                                            term = freshProfile.defaultTerm,
                                            parentSettings = res.snapshot.child.toParentSettings(),
                                            records = res.snapshot.records,
                                            studyLog = res.snapshot.studyLog,
                                            pendingSyncCount = 0,
                                            syncState = SyncState.IDLE,
                                            parentNotice = "检测到云端已由家长重置学习数据，本地已同步清空。"
                                        )
                                    }
                                }
                                return@withLock
                            }

                            val currentEvents = repo.loadPendingEvents(cid)
                            val sentIds = eventsList.mapTo(HashSet()) { it.eventId }
                            val remainingEvents = currentEvents.filterNot { it.eventId in sentIds }
                            repo.savePendingEvents(cid, remainingEvents)

                            val currentPatch = repo.loadPendingProfilePatch(cid)
                            val remainingPatch = if (
                                currentPatch != null &&
                                patchObj != null &&
                                currentPatch.updatedAtMs == patchObj.updatedAtMs
                            ) {
                                repo.savePendingProfilePatch(cid, null)
                                null
                            } else {
                                currentPatch
                            }

                            pendingTotal = remainingEvents.size + if (remainingPatch != null) 1 else 0
                            if (pendingTotal == 0) {
                                repo.saveSnapshot(res.snapshot)
                            } else {
                                shouldContinue = true
                            }
                        }

                        val updatedChildren = session.children.map {
                            if (it.id == cid) res.snapshot.child else it
                        }
                        session = session.copy(children = updatedChildren)
                        repo.saveAuthSession(session)

                        if (!shouldContinue && _uiState.value.activeChildId == cid) {
                            val syncedProfile = res.snapshot.child.toUserProfile()
                            _uiState.update { cur ->
                                cur.copy(
                                    children = updatedChildren,
                                    resetVersion = res.snapshot.child.resetVersion,
                                    profile = syncedProfile,
                                    parentSettings = res.snapshot.child.toParentSettings(),
                                    records = res.snapshot.records,
                                    studyLog = res.snapshot.studyLog,
                                    pendingSyncCount = 0,
                                    syncState = SyncState.IDLE
                                )
                            }
                            break
                        } else if (!shouldContinue) {
                            break
                        }
                    }
                } catch (e: CloudApiException) {
                    if (e.statusCode == 401) {
                        repo.clearAllLocalState()
                        _uiState.update { cur ->
                            UiState(
                                isLoading = false,
                                isLoggedIn = false,
                                preferRegisterTab = false,
                                authError = "登录凭证已过期，请重新登录",
                                bundle = cur.bundle,
                                selectedDate = LocalDate.now().toString()
                            )
                        }
                    } else {
                        val pending = synchronized(queueLock) {
                            repo.loadPendingEvents(session.activeChildId).size +
                                if (repo.loadPendingProfilePatch(session.activeChildId) != null) 1 else 0
                        }
                        _uiState.update {
                            it.copy(
                                pendingSyncCount = pending,
                                syncState = SyncState.OFFLINE
                            )
                        }
                    }
                } catch (_: Exception) {
                    val pending = synchronized(queueLock) {
                        repo.loadPendingEvents(session.activeChildId).size +
                            if (repo.loadPendingProfilePatch(session.activeChildId) != null) 1 else 0
                    }
                    _uiState.update {
                        it.copy(
                            pendingSyncCount = pending,
                            syncState = SyncState.OFFLINE
                        )
                    }
                }
            }
        }
    }

    fun remediationStepsFor(subject: SubjectKey): List<RemediationStep> {
        val eng = engines[subject] ?: return emptyList()
        return eng.buildRemediationLadder(_uiState.value.records)
    }

    fun unitsFor(subject: SubjectKey, term: TermFilter): List<UnitGroup> {
        val ds = _uiState.value.bundle?.subjects?.get(subject) ?: return emptyList()
        val allowedTerms = if (term == TermFilter.ALL) listOf("上", "下") else listOf(term.key)
        val groups = linkedMapOf<Pair<String, String>, MutableList<TopicNode>>()
        ds.topics.filter { it.isTarget && it.term in allowedTerms }.forEach { t ->
            groups.getOrPut(t.term to t.unit) { mutableListOf() }.add(t)
        }
        return groups.map { (k, list) ->
            UnitGroup(subject = subject, term = k.first, title = k.second, topics = list)
        }
    }

    override fun onCleared() {
        super.onCleared()
        audioPlayer.release()
    }
}
