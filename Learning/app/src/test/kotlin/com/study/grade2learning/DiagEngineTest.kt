package com.study.grade2learning.engine

import com.study.grade2learning.model.DependencyEdge
import com.study.grade2learning.model.NodeStatus
import com.study.grade2learning.model.QuestionChoice
import com.study.grade2learning.model.QuestionItem
import com.study.grade2learning.model.SubjectDataset
import com.study.grade2learning.model.SubjectKey
import com.study.grade2learning.model.TopicNode
import com.study.grade2learning.model.TopicRecord
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DiagEngineTest {

    private val g1Topic = TopicNode(
        id = "cnm_p020",
        subject = SubjectKey.MATH,
        name = "100以内的笔算减法",
        grade = 1,
        term = "下",
        unit = "两位数加减",
        orderIndex = 10,
        isTarget = false,
        description = "",
        masteryCriteria = listOf("掌握两位数退位减法"),
        rootReason = "先修计算基础",
        diagnosticQuestion = ""
    )

    private val g2Topic = TopicNode(
        id = "cnm_p055",
        subject = SubjectKey.MATH,
        name = "三位数退位减法",
        grade = 2,
        term = "下",
        unit = "六 两、三位数的加法和减法",
        orderIndex = 40,
        isTarget = true,
        description = "",
        masteryCriteria = listOf("掌握连续退位减法"),
        rootReason = "十位为0时连续退位容易漏减",
        diagnosticQuestion = ""
    )

    private val numQuestion = QuestionItem(
        id = "q_num_1",
        topicId = "cnm_p055",
        subject = SubjectKey.MATH,
        difficulty = "basic",
        type = "numeric",
        prompt = "503 - 268 = ?",
        choices = emptyList(),
        answer = "235",
        explanation = "十位不够减需向百位借 1"
    )

    private val fracQuestion = QuestionItem(
        id = "q_frac_1",
        topicId = "cnm_p055",
        subject = SubjectKey.MATH,
        difficulty = "basic",
        type = "numeric",
        prompt = "四分之三写成小数或分数？",
        choices = emptyList(),
        answer = "3/4",
        explanation = "3/4 = 0.75"
    )

    private val choiceQuestion = QuestionItem(
        id = "q_en_1",
        topicId = "cnm_p055",
        subject = SubjectKey.ENGLISH,
        difficulty = "basic",
        type = "choice",
        prompt = "Which letter is after D?",
        choices = listOf(QuestionChoice("A", "C"), QuestionChoice("D", "E")),
        answer = "D",
        explanation = "D is followed by E."
    )

    private val engine = DiagEngine(
        SubjectDataset(
            key = SubjectKey.MATH,
            title = "数学",
            edition = "苏教版二年级",
            targetCount = 1,
            prerequisiteCount = 1,
            topics = listOf(g1Topic, g2Topic),
            dependencies = listOf(
                DependencyEdge(
                    topicId = "cnm_p055",
                    prerequisiteId = "cnm_p020",
                    strength = "hard",
                    reason = "三位数退位减法需要先掌握 100 以内笔算减法。"
                )
            ),
            questions = listOf(numQuestion, fracQuestion, choiceQuestion)
        )
    )

    @Test
    fun `two consecutive correct answers master topic when threshold is 2`() {
        var records = emptyMap<String, TopicRecord>()
        val (r1, o1) = engine.applyAnswer("cnm_p055", numQuestion, "235", records, masteryStreak = 2)
        records = r1
        assertTrue(o1.isCorrect)
        assertEquals(NodeStatus.SHAKY, o1.updatedRecord.status)

        val (r2, o2) = engine.applyAnswer("cnm_p055", numQuestion, "235", records, masteryStreak = 2)
        assertTrue(o2.isCorrect)
        assertEquals(NodeStatus.MASTERED, o2.updatedRecord.status)
        assertEquals(NodeStatus.MASTERED, r2["cnm_p055"]?.status)
    }

    @Test
    fun `threshold 3 requires third consecutive correct answer`() {
        var records = emptyMap<String, TopicRecord>()
        records = engine.applyAnswer("cnm_p055", numQuestion, "235", records, masteryStreak = 3).first
        val (r2, o2) = engine.applyAnswer("cnm_p055", numQuestion, "235", records, masteryStreak = 3)
        assertEquals(NodeStatus.SHAKY, o2.updatedRecord.status)

        val (r3, o3) = engine.applyAnswer("cnm_p055", numQuestion, "235", r2, masteryStreak = 3)
        assertEquals(NodeStatus.MASTERED, o3.updatedRecord.status)
        assertEquals(3, r3["cnm_p055"]?.consecutiveCorrect)
    }

    @Test
    fun `wrong answer marks weak and backtracks to grade 1 prerequisite`() {
        val (records, outcome) = engine.applyAnswer("cnm_p055", numQuestion, "245", emptyMap(), masteryStreak = 2)
        assertFalse(outcome.isCorrect)
        assertEquals(NodeStatus.WEAK, records["cnm_p055"]?.status)
        assertEquals("cnm_p020", outcome.suggestedPrereqTopicId)
        assertNotNull(outcome.backtrackHint)
    }

    @Test
    fun `numeric judge supports fractions and decimals equivalence`() {
        assertTrue(engine.judgeAnswer(fracQuestion, "0.75"))
        assertTrue(engine.judgeAnswer(fracQuestion, "6/8"))
        assertFalse(engine.judgeAnswer(fracQuestion, "0.5"))
        assertTrue(engine.judgeAnswer(choiceQuestion, "d"))
    }

    @Test
    fun `remediation ladder orders grade 1 root cause before grade 2 weak topic`() {
        val records = mapOf(
            "cnm_p055" to TopicRecord(status = NodeStatus.WEAK, wrongCount = 2),
            "cnm_p020" to TopicRecord(status = NodeStatus.WEAK, wrongCount = 1)
        )
        val ladder = engine.buildRemediationLadder(records)
        assertEquals(2, ladder.size)
        assertEquals("cnm_p020", ladder[0].topic.id)
        assertTrue(ladder[0].isRootCause)
        assertEquals("cnm_p055", ladder[1].topic.id)
    }
}
