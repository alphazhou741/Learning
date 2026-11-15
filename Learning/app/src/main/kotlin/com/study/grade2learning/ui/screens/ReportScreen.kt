package com.study.grade2learning.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.study.grade2learning.model.NodeStatus
import com.study.grade2learning.model.QuestionItem
import com.study.grade2learning.model.RemediationStep
import com.study.grade2learning.model.SubjectKey
import com.study.grade2learning.model.TopicNode
import com.study.grade2learning.model.TopicRecord
import com.study.grade2learning.ui.UiState
import com.study.grade2learning.ui.components.UnifiedTopBar
import com.study.grade2learning.ui.icons.AppIcons
import com.study.grade2learning.ui.theme.AppColors

private data class WrongNotebookEntry(
    val topic: TopicNode,
    val record: TopicRecord,
    val question: QuestionItem
)

@Composable
fun ReportScreen(
    state: UiState,
    remediationSteps: List<RemediationStep>,
    onSelectSubject: (SubjectKey) -> Unit,
    onToggleViewMode: (Boolean) -> Unit,
    onPracticeTopic: (SubjectKey, String) -> Unit,
    onStartRootRemediation: () -> Unit
) {
    val isParentMode = state.reportParentMode
    val dataset = state.activeDataset
    var showWrongNotebook by remember { mutableStateOf(false) }

    val counts = remember(dataset, state.records) {
        val topics = dataset?.topics.orEmpty()
        var mastered = 0
        var shaky = 0
        var weak = 0
        topics.forEach { t ->
            when (state.records[t.id]?.status ?: NodeStatus.UNKNOWN) {
                NodeStatus.MASTERED -> mastered++
                NodeStatus.SHAKY -> shaky++
                NodeStatus.WEAK -> weak++
                NodeStatus.UNKNOWN -> Unit
            }
        }
        Triple(mastered, shaky, weak)
    }

    val wrongEntries = remember(dataset, state.records) {
        val ds = dataset ?: return@remember emptyList()
        val questionsById = ds.questions.associateBy { it.id }
        val questionsByTopic = ds.questions.groupBy { it.topicId }
        ds.topics.mapNotNull { topic ->
            val rec = state.records[topic.id] ?: return@mapNotNull null
            if (rec.wrongCount <= 0 && rec.status != NodeStatus.WEAK) return@mapNotNull null
            val q = rec.lastQuestionId?.let { questionsById[it] }
                ?: questionsByTopic[topic.id]?.firstOrNull { it.difficulty == "basic" }
                ?: questionsByTopic[topic.id]?.firstOrNull()
                ?: return@mapNotNull null
            WrongNotebookEntry(topic = topic, record = rec, question = q)
        }.sortedWith(
            compareBy<WrongNotebookEntry>(
                { if (it.record.status == NodeStatus.WEAK) 0 else 1 },
                { -it.record.wrongCount },
                { -it.record.updatedAtMs },
                { it.topic.grade },
                { it.topic.orderIndex }
            )
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        UnifiedTopBar(
            leftContent = {
                Icon(
                    imageVector = AppIcons.Pulse,
                    contentDescription = null,
                    tint = AppColors.MathPrimary,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "学习成果与先修溯源报告",
                    color = AppColors.TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = "档案与规则请点击左下角「家长」",
                    color = AppColors.TextSubtle,
                    fontSize = 12.sp
                )
            },
            rightContent = {
                Row(
                    modifier = Modifier
                        .height(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(AppColors.PillSegmentTrack)
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    listOf(false to "孩子视角", true to "先修根因阶梯").forEach { (mode, label) ->
                        val active = !showWrongNotebook && isParentMode == mode
                        Box(
                            modifier = Modifier
                                .height(32.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (active) AppColors.CardSurface else Color.Transparent)
                                .clickable {
                                    showWrongNotebook = false
                                    onToggleViewMode(mode)
                                }
                                .padding(horizontal = 14.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                color = if (active) AppColors.TextPrimary else AppColors.TextSecondary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    val notebookActive = showWrongNotebook
                    Box(
                        modifier = Modifier
                            .height(32.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (notebookActive) AppColors.CardSurface else Color.Transparent)
                            .clickable { showWrongNotebook = true }
                            .padding(horizontal = 14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "错题本 (${wrongEntries.size})",
                            color = if (notebookActive) AppColors.WeakTextDark else AppColors.TextSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        )

        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxSize()
        ) {
            // Left Pane (5/12): Summary & Subject Filter
            Column(
                modifier = Modifier
                    .weight(5f)
                    .fillMaxHeight()
                    .background(AppColors.SubPanelBg)
                    .padding(20.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    // Subject tabs inside left pane
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SubjectKey.entries.forEach { subj ->
                            val active = subj == state.subject
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(34.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (active) AppColors.MathPrimary else AppColors.CardSurface)
                                    .border(
                                        1.dp,
                                        if (active) AppColors.MathPrimary else AppColors.BorderWarm,
                                        RoundedCornerShape(10.dp)
                                    )
                                    .clickable { onSelectSubject(subj) },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = subj.title,
                                    color = if (active) AppColors.TextOnAccent else AppColors.TextSecondary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // Summary Card
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(24.dp))
                            .background(AppColors.CardSurface)
                            .border(1.dp, AppColors.BorderWarm, RoundedCornerShape(24.dp))
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(62.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .background(if (isParentMode) AppColors.MathBadgeBg else AppColors.ZhBadgeBg),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isParentMode) AppIcons.Chart else AppIcons.Trophy,
                                contentDescription = null,
                                tint = if (isParentMode) AppColors.MathPrimary else AppColors.ZhPrimary,
                                modifier = Modifier.size(34.dp)
                            )
                        }
                        Text(
                            text = if (isParentMode) {
                                "${dataset?.edition ?: state.subject.defaultEdition} · 诊断完成"
                            } else {
                                "真棒！已拿下 ${counts.first} 个知识点"
                            },
                            color = AppColors.TextPrimary,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.ExtraBold,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = if (isParentMode) {
                                val firstStep = remediationSteps.firstOrNull()
                                if (firstStep != null) {
                                    "引擎按先修依赖回溯，建议优先从${firstStep.topic.grade}年级「${firstStep.topic.name}」开始补牢根基。"
                                } else {
                                    "当前该学科知识图谱状态良好，可继续按单元节奏巩固进阶。"
                                }
                            } else {
                                if (remediationSteps.isNotEmpty()) {
                                    "已经攻克 ${counts.first} 个关卡，再补好右边的 ${remediationSteps.size} 个小难点就能通关啦。"
                                } else {
                                    "当前没有待攻克的小漏洞，继续保持好节奏！"
                                }
                            },
                            color = AppColors.TextSecondary,
                            fontSize = 12.sp,
                            lineHeight = 19.sp,
                            textAlign = TextAlign.Center
                        )
                    }

                    // 3 Stat Tiles
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ReportStatTile(
                            modifier = Modifier.weight(1f),
                            count = counts.first,
                            label = "已掌握",
                            bg = AppColors.MasteredBg,
                            border = AppColors.MasteredBorder,
                            countColor = AppColors.MasteredText,
                            labelColor = AppColors.EnTextDark
                        )
                        ReportStatTile(
                            modifier = Modifier.weight(1f),
                            count = counts.second,
                            label = "有些不稳",
                            bg = AppColors.ShakyBg,
                            border = AppColors.ShakyBorder,
                            countColor = Color(0xFF9B6C29),
                            labelColor = AppColors.ZhTextDark
                        )
                        ReportStatTile(
                            modifier = Modifier.weight(1f),
                            count = counts.third,
                            label = "薄弱待补",
                            bg = AppColors.WeakSurface,
                            border = AppColors.WeakBorder,
                            countColor = Color(0xFF9B4C4C),
                            labelColor = Color(0xFF823F3F)
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(AppColors.MathPrimary)
                        .clickable(onClick = onStartRootRemediation),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (remediationSteps.isNotEmpty()) "按根因顺序一键开始补习" else "开始本学科巩固练习",
                        color = AppColors.TextOnAccent,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Icon(
                        imageVector = AppIcons.ArrowRight,
                        contentDescription = null,
                        tint = AppColors.TextOnAccent,
                        modifier = Modifier
                            .padding(start = 6.dp)
                            .size(16.dp)
                    )
                }
            }

            VerticalDivider(thickness = 1.dp, color = AppColors.DividerWarm)

            // Right Pane (7/12): Prerequisite Remediation Ladder OR Wrong-Answer Notebook
            Column(
                modifier = Modifier
                    .weight(7f)
                    .fillMaxHeight()
                    .background(AppColors.PaperBg)
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (showWrongNotebook) {
                            "专属错题本（记录答错过的考点原题与解析）"
                        } else {
                            "最短补习阶梯（按年级从低到高排列）"
                        },
                        color = AppColors.TextSecondary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = when {
                            showWrongNotebook -> "攻克后连续答对达标即可转化为已掌握"
                            state.subject == SubjectKey.MATH -> "已自动排除高于当前年级的超纲边"
                            else -> "先修根因 → 关联薄弱 → 不稳巩固"
                        },
                        color = AppColors.TextMuted,
                        fontSize = 12.sp
                    )
                }

                if (showWrongNotebook) {
                    if (wrongEntries.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(20.dp))
                                .background(AppColors.MasteredBg)
                                .border(1.dp, AppColors.MasteredBorder, RoundedCornerShape(20.dp))
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = AppIcons.CheckCircle,
                                    contentDescription = null,
                                    tint = AppColors.MasteredText,
                                    modifier = Modifier.size(36.dp)
                                )
                                Text(
                                    text = "太棒了！本学科错题本目前为空",
                                    color = AppColors.MasteredText,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                Text(
                                    text = "练习中答错或跳过的题目会自动收录到这里，方便随时复盘。",
                                    color = AppColors.TextSecondary,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            contentPadding = PaddingValues(bottom = 8.dp)
                        ) {
                            items(wrongEntries, key = { it.topic.id }) { entry ->
                                WrongNotebookCard(
                                    entry = entry,
                                    onPractice = { onPracticeTopic(entry.topic.subject, entry.topic.id) }
                                )
                            }
                        }
                    }
                } else if (remediationSteps.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(20.dp))
                            .background(AppColors.MasteredBg)
                            .border(1.dp, AppColors.MasteredBorder, RoundedCornerShape(20.dp))
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = AppIcons.CheckCircle,
                                contentDescription = null,
                                tint = AppColors.MasteredText,
                                modifier = Modifier.size(36.dp)
                            )
                            Text(
                                text = "该学科目前没有薄弱或待巩固知识点",
                                color = AppColors.MasteredText,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                text = "可在首页选择「做本册自适应体检」继续拓展新单元。",
                                color = AppColors.TextSecondary,
                                fontSize = 12.sp
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(bottom = 8.dp)
                    ) {
                        itemsIndexed(remediationSteps) { idx, step ->
                            if (idx > 0 && remediationSteps[idx - 1].isRootCause) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 2.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = AppIcons.ArrowDown,
                                        contentDescription = null,
                                        tint = AppColors.TextSubtle,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = "  掌握上面根因，再来攻克下面  ",
                                        color = AppColors.TextSubtle,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Icon(
                                        imageVector = AppIcons.ArrowDown,
                                        contentDescription = null,
                                        tint = AppColors.TextSubtle,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                            RemediationLadderCard(
                                step = step,
                                onPractice = { onPracticeTopic(step.topic.subject, step.topic.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WrongNotebookCard(
    entry: WrongNotebookEntry,
    onPractice: () -> Unit
) {
    val isWeak = entry.record.status == NodeStatus.WEAK
    val q = entry.question
    val formattedRightAnswer = remember(q) {
        if (q.type == "choice" && q.choices.isNotEmpty()) {
            val rightChoice = q.choices.firstOrNull { it.key.equals(q.answer.trim(), ignoreCase = true) }
            if (rightChoice != null) "${rightChoice.key}. ${rightChoice.text}" else q.answer
        } else {
            q.answer
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(AppColors.CardSurface)
            .border(
                1.dp,
                if (isWeak) AppColors.WeakBorder else AppColors.ShakyBorder,
                RoundedCornerShape(16.dp)
            )
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isWeak) AppColors.WeakSurface else AppColors.ShakyBg)
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "${entry.topic.grade}年级${entry.topic.term} · ${entry.topic.unit}",
                        color = if (isWeak) AppColors.WeakTextDark else AppColors.ZhTextDark,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = entry.topic.name,
                    color = AppColors.TextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = "累计错 ${entry.record.wrongCount.coerceAtLeast(1)} 次",
                    color = AppColors.WeakTextDark,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Box(
                modifier = Modifier
                    .height(34.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(AppColors.MathPrimary)
                    .clickable(onClick = onPractice)
                    .padding(horizontal = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "重练此题",
                    color = AppColors.TextOnAccent,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Text(
            text = "原题：${q.prompt}",
            color = AppColors.TextPrimary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            lineHeight = 21.sp
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFFF3EFE5))
                .border(1.dp, AppColors.BorderWarm, RoundedCornerShape(12.dp))
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "正确答案：$formattedRightAnswer",
                color = AppColors.EnTextDark,
                fontSize = 13.sp,
                fontWeight = FontWeight.ExtraBold
            )
            if (q.explanation.isNotBlank()) {
                Text(
                    text = "解题思路：${q.explanation}",
                    color = AppColors.TextSecondary,
                    fontSize = 12.sp,
                    lineHeight = 18.sp
                )
            }
        }
    }
}

@Composable
private fun ReportStatTile(
    modifier: Modifier,
    count: Int,
    label: String,
    bg: Color,
    border: Color,
    countColor: Color,
    labelColor: Color
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(bg)
            .border(1.dp, border, RoundedCornerShape(16.dp))
            .padding(vertical = 12.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = count.toString(),
            color = countColor,
            fontSize = 22.sp,
            fontWeight = FontWeight.ExtraBold
        )
        Text(
            text = label,
            color = labelColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

private data class LadderCardStyle(
    val cardBg: Color,
    val cardBorder: Color,
    val badgeBg: Color,
    val badgeFg: Color,
    val btnBg: Color,
    val btnFg: Color,
    val btnText: String
)

@Composable
private fun RemediationLadderCard(
    step: RemediationStep,
    onPractice: () -> Unit
) {
    val isWeak = step.status == NodeStatus.WEAK
    val style = when {
        step.isRootCause -> LadderCardStyle(
            cardBg = AppColors.WeakSurface,
            cardBorder = Color(0xFFE4C3C3),
            badgeBg = AppColors.WeakPrimary,
            badgeFg = AppColors.TextOnAccent,
            btnBg = AppColors.WeakPrimary,
            btnFg = AppColors.TextOnAccent,
            btnText = "先补这关"
        )
        isWeak -> LadderCardStyle(
            cardBg = AppColors.CardSurface,
            cardBorder = AppColors.WeakBorder,
            badgeBg = AppColors.PillMutedBg,
            badgeFg = AppColors.TextSecondary,
            btnBg = AppColors.PillMutedBg,
            btnFg = AppColors.TextPrimary,
            btnText = "去练习"
        )
        else -> LadderCardStyle(
            cardBg = AppColors.ShakyBg,
            cardBorder = AppColors.ShakyBorder,
            badgeBg = Color(0xFFF5EAD7),
            badgeFg = AppColors.ZhTextDark,
            btnBg = Color(0xFFFAF3E6),
            btnFg = AppColors.ZhTextDark,
            btnText = "巩固一下"
        )
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(style.cardBg)
            .border(1.dp, style.cardBorder, RoundedCornerShape(16.dp))
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(style.badgeBg)
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = step.roleLabel,
                        color = style.badgeFg,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(if (isWeak) AppColors.WeakPrimary else AppColors.ShakyDot)
                    )
                    Text(
                        text = step.status.label,
                        color = if (isWeak) AppColors.WeakTextDark else AppColors.ZhTextDark,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Text(
                text = step.topic.name,
                color = AppColors.TextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                text = step.reason,
                color = AppColors.TextSecondary,
                fontSize = 12.sp,
                lineHeight = 18.sp
            )
        }

        Box(
            modifier = Modifier
                .height(36.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(style.btnBg)
                .then(
                    if (!step.isRootCause && !isWeak) {
                        Modifier.border(1.dp, AppColors.ShakyBorder, RoundedCornerShape(12.dp))
                    } else Modifier
                )
                .clickable(onClick = onPractice)
                .padding(horizontal = 14.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = style.btnText,
                color = style.btnFg,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
