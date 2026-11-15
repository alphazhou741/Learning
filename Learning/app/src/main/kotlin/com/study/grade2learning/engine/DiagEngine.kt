package com.study.grade2learning.engine

import com.study.grade2learning.model.DependencyEdge
import com.study.grade2learning.model.NodeStatus
import com.study.grade2learning.model.QuestionChoice
import com.study.grade2learning.model.QuestionItem
import com.study.grade2learning.model.RemediationStep
import com.study.grade2learning.model.SubjectDataset
import com.study.grade2learning.model.TopicNode
import com.study.grade2learning.model.TopicRecord
import kotlin.math.abs

data class AnswerOutcome(
    val topicId: String,
    val questionId: String,
    val isCorrect: Boolean,
    val isSkipped: Boolean,
    val updatedRecord: TopicRecord,
    val headline: String,
    val detail: String,
    val backtrackHint: String? = null,
    val suggestedPrereqTopicId: String? = null
)

class DiagEngine(private val dataset: SubjectDataset) {

    val topicById: Map<String, TopicNode> = dataset.topics.associateBy { it.id }
    val questionsByTopic: Map<String, List<QuestionItem>> = dataset.questions.groupBy { it.topicId }

    private val prereqEdgesOf: Map<String, List<DependencyEdge>> = dataset.dependencies.groupBy { it.topicId }
    private val dependentEdgesOf: Map<String, List<DependencyEdge>> = dataset.dependencies.groupBy { it.prerequisiteId }

    fun directPrerequisites(topicId: String): List<DependencyEdge> = prereqEdgesOf[topicId].orEmpty()

    fun allAncestors(topicId: String): List<TopicNode> {
        val visited = linkedSetOf<String>()
        val queue = ArrayDeque<String>()
        directPrerequisites(topicId).forEach {
            if (topicById.containsKey(it.prerequisiteId)) queue.add(it.prerequisiteId)
        }
        while (queue.isNotEmpty()) {
            val cur = queue.removeFirst()
            if (visited.add(cur)) {
                directPrerequisites(cur).forEach { edge ->
                    if (!visited.contains(edge.prerequisiteId) && topicById.containsKey(edge.prerequisiteId)) {
                        queue.add(edge.prerequisiteId)
                    }
                }
            }
        }
        return visited.mapNotNull { topicById[it] }.sortedWith(compareBy({ it.grade }, { it.orderIndex }))
    }

    fun pickQuestion(
        topicId: String,
        records: Map<String, TopicRecord>,
        excludeQuestionIds: Set<String> = emptySet()
    ): QuestionItem? {
        val pool = questionsByTopic[topicId].orEmpty()
        if (pool.isEmpty()) return null
        val rec = records[topicId] ?: TopicRecord()
        val unasked = pool.filter { it.id !in excludeQuestionIds && it.id != rec.lastQuestionId }
        val candidates = unasked.ifEmpty { pool.filter { it.id != rec.lastQuestionId }.ifEmpty { pool } }
        val picked = if (rec.status == NodeStatus.WEAK || rec.wrongCount > 0) {
            candidates.filter { it.difficulty == "basic" }.ifEmpty { candidates }.random()
        } else {
            candidates.random()
        }
        return shuffleChoiceOptions(picked)
    }

    private fun shuffleChoiceOptions(q: QuestionItem): QuestionItem {
        if (q.type != "choice" || q.choices.size < 2) return q
        val targetKey = q.answer.trim()
        val originalRight = q.choices.firstOrNull { it.key.equals(targetKey, ignoreCase = true) } ?: return q
        val shuffledChoices = q.choices.shuffled()
        var newAnswerKey = q.answer
        val remapped = q.choices.mapIndexed { idx, canonicalSlot ->
            val pickedOption = shuffledChoices[idx]
            if (pickedOption === originalRight) {
                newAnswerKey = canonicalSlot.key
            }
            QuestionChoice(
                key = canonicalSlot.key,
                text = pickedOption.text
            )
        }
        return q.copy(
            choices = remapped,
            answer = newAnswerKey
        )
    }

    fun judgeAnswer(question: QuestionItem, rawInput: String): Boolean {
        val cleaned = rawInput.trim()
        if (cleaned.isEmpty()) return false
        if (question.type == "choice") {
            return cleaned.equals(question.answer.trim(), ignoreCase = true)
        }
        if (question.answer.contains('，') || question.answer.contains(',')) {
            return judgeCommaSeparatedAnswer(expectedRaw = question.answer, actualRaw = cleaned)
        }
        if (cleaned.contains('，') || cleaned.contains(',')) {
            return false
        }
        val expectedNum = parseNumericValue(question.answer)
        val actualNum = parseNumericValue(cleaned)
        if (expectedNum != null && actualNum != null) {
            return abs(expectedNum - actualNum) < 1e-6
        }
        return normalizeText(cleaned) == normalizeText(question.answer)
    }

    fun applyAnswer(
        topicId: String,
        question: QuestionItem,
        rawInput: String,
        records: Map<String, TopicRecord>,
        masteryStreak: Int = 2
    ): Pair<Map<String, TopicRecord>, AnswerOutcome> {
        val ok = judgeAnswer(question, rawInput)
        return transitionRecord(
            topicId = topicId,
            question = question,
            isCorrect = ok,
            isSkipped = false,
            records = records,
            masteryStreak = masteryStreak.coerceIn(1, 5)
        )
    }

    fun skipQuestion(
        topicId: String,
        question: QuestionItem,
        records: Map<String, TopicRecord>,
        masteryStreak: Int = 2
    ): Pair<Map<String, TopicRecord>, AnswerOutcome> {
        return transitionRecord(
            topicId = topicId,
            question = question,
            isCorrect = false,
            isSkipped = true,
            records = records,
            masteryStreak = masteryStreak.coerceIn(1, 5)
        )
    }

    private fun transitionRecord(
        topicId: String,
        question: QuestionItem,
        isCorrect: Boolean,
        isSkipped: Boolean,
        records: Map<String, TopicRecord>,
        masteryStreak: Int
    ): Pair<Map<String, TopicRecord>, AnswerOutcome> {
        val prev = records[topicId] ?: TopicRecord()
        val updatedMap = records.toMutableMap()

        if (isCorrect) {
            val nextStreak = prev.consecutiveCorrect + 1
            val mastered = nextStreak >= masteryStreak
            val nextStatus = if (mastered) NodeStatus.MASTERED else NodeStatus.SHAKY
            val updated = prev.copy(
                status = nextStatus,
                consecutiveCorrect = nextStreak,
                attempts = prev.attempts + 1,
                lastQuestionId = question.id
            )
            updatedMap[topicId] = updated

            val headline = if (mastered) {
                "回答正确！本知识点已掌握（连对 $nextStreak / $masteryStreak 题）"
            } else {
                "答对了！再连对 ${masteryStreak - nextStreak} 题即可彻底掌握"
            }
            val detail = question.explanation.ifBlank { "继续巩固下一个同考点题目，确保完全稳固。" }
            return updatedMap to AnswerOutcome(
                topicId = topicId,
                questionId = question.id,
                isCorrect = true,
                isSkipped = false,
                updatedRecord = updated,
                headline = headline,
                detail = detail
            )
        } else {
            val nextWrong = prev.wrongCount + 1
            val nextStatus = if ((prev.status == NodeStatus.MASTERED || prev.consecutiveCorrect > 0) && !isSkipped) {
                NodeStatus.SHAKY
            } else {
                NodeStatus.WEAK
            }
            val updated = prev.copy(
                status = nextStatus,
                consecutiveCorrect = 0,
                attempts = prev.attempts + 1,
                wrongCount = nextWrong,
                lastQuestionId = question.id
            )
            updatedMap[topicId] = updated

            val backtrack = findBacktrackTarget(topicId, updatedMap)
            val topicName = topicById[topicId]?.name ?: "当前知识点"
            val headline = if (isSkipped) "已跳过并记录为待加强" else "差一点点，思路再理顺一下"
            val detail = buildString {
                if (question.explanation.isNotBlank()) {
                    append(question.explanation)
                } else {
                    append("建议复盘「$topicName」的基本步骤后再试一题。")
                }
            }
            return updatedMap to AnswerOutcome(
                topicId = topicId,
                questionId = question.id,
                isCorrect = false,
                isSkipped = isSkipped,
                updatedRecord = updated,
                headline = headline,
                detail = detail,
                backtrackHint = backtrack?.second,
                suggestedPrereqTopicId = backtrack?.first?.id
            )
        }
    }

    private fun findBacktrackTarget(
        topicId: String,
        records: Map<String, TopicRecord>
    ): Pair<TopicNode, String>? {
        val ancestors = allAncestors(topicId)
        val unmastered = ancestors.filter {
            val st = records[it.id]?.status ?: NodeStatus.UNKNOWN
            st != NodeStatus.MASTERED
        }
        val candidate = unmastered.firstOrNull { (records[it.id]?.status ?: NodeStatus.UNKNOWN) == NodeStatus.WEAK }
            ?: unmastered.firstOrNull()
            ?: return null

        val directEdge = directPrerequisites(topicId).firstOrNull { it.prerequisiteId == candidate.id }
        val reason = directEdge?.reason?.takeIf { it.isNotBlank() }
            ?: "引擎回溯建议：先检查${candidate.grade}年级「${candidate.name}」是否完全扎实。"
        return candidate to reason
    }

    fun pickAdaptiveSessionTopics(
        termFilterKey: String,
        records: Map<String, TopicRecord>,
        limit: Int = 5
    ): List<TopicNode> {
        val targets = dataset.topics.filter { t ->
            t.isTarget && (termFilterKey == "全" || t.term == termFilterKey)
        }
        val weak = targets.filter { (records[it.id]?.status ?: NodeStatus.UNKNOWN) == NodeStatus.WEAK }
        val shaky = targets.filter { (records[it.id]?.status ?: NodeStatus.UNKNOWN) == NodeStatus.SHAKY }
        val unknown = targets.filter { (records[it.id]?.status ?: NodeStatus.UNKNOWN) == NodeStatus.UNKNOWN }
        val mastered = targets.filter { (records[it.id]?.status ?: NodeStatus.UNKNOWN) == NodeStatus.MASTERED }
        return (weak + shaky + unknown + mastered).take(limit)
    }

    fun buildRemediationLadder(records: Map<String, TopicRecord>): List<RemediationStep> {
        val flagged = dataset.topics.filter { t ->
            val st = records[t.id]?.status ?: NodeStatus.UNKNOWN
            st == NodeStatus.WEAK || st == NodeStatus.SHAKY
        }

        val steps = flagged.sortedWith(
            compareBy<TopicNode>(
                { it.grade },
                { if (it.isTarget) 1 else 0 },
                { if (records[it.id]?.status == NodeStatus.WEAK) 0 else 1 },
                { it.orderIndex }
            )
        )

        return steps.mapIndexed { idx, topic ->
            val st = records[topic.id]?.status ?: NodeStatus.UNKNOWN
            val downstreamEdge = dependentEdgesOf[topic.id]?.firstOrNull { edge ->
                val depStatus = records[edge.topicId]?.status ?: NodeStatus.UNKNOWN
                depStatus == NodeStatus.WEAK || depStatus == NodeStatus.SHAKY
            }
            val isRoot = !topic.isTarget && st == NodeStatus.WEAK
            val roleLabel = when {
                isRoot -> "${idx + 1}. 根因源头 · ${topic.grade}年级${topic.term}"
                st == NodeStatus.WEAK -> "${idx + 1}. 关联薄弱 · ${topic.unit}"
                else -> "${idx + 1}. 巩固强化 · ${topic.unit}"
            }
            val reason = downstreamEdge?.reason?.takeIf { it.isNotBlank() }
                ?: topic.rootReason.takeIf { it.isNotBlank() }
                ?: topic.masteryCriteria.firstOrNull()
                ?: "建议完成连续巩固练习以达标。"
            RemediationStep(
                topic = topic,
                status = st,
                roleLabel = roleLabel,
                reason = reason,
                isRootCause = isRoot
            )
        }
    }

    companion object {
        private val COMMA_SPLIT_REGEX = Regex("[,，]+")

        private fun judgeCommaSeparatedAnswer(expectedRaw: String, actualRaw: String): Boolean {
            val expectedParts = expectedRaw.split(COMMA_SPLIT_REGEX).map { it.trim() }.filter { it.isNotEmpty() }
            val actualParts = actualRaw.split(COMMA_SPLIT_REGEX).map { it.trim() }.filter { it.isNotEmpty() }
            if (expectedParts.size < 2 || expectedParts.size != actualParts.size) {
                return false
            }
            for (i in expectedParts.indices) {
                val expNum = parseNumericValue(expectedParts[i])
                val actNum = parseNumericValue(actualParts[i])
                val partMatch = if (expNum != null && actNum != null) {
                    abs(expNum - actNum) < 1e-6
                } else {
                    normalizeText(expectedParts[i]) == normalizeText(actualParts[i])
                }
                if (!partMatch) return false
            }
            return true
        }

        fun parseNumericValue(raw: String): Double? {
            val s = raw.trim().replace(" ", "")
            if (s.isEmpty() || s.contains('，') || s.contains(',')) return null
            if ('/' in s) {
                val parts = s.split('/')
                if (parts.size == 2) {
                    val num = parts[0].toDoubleOrNull() ?: return null
                    val den = parts[1].toDoubleOrNull() ?: return null
                    if (abs(den) < 1e-12) return null
                    return num / den
                }
            }
            return s.toDoubleOrNull()
        }

        private fun normalizeText(s: String): String =
            s.trim().lowercase().replace(Regex("\\s+"), "")
    }
}
