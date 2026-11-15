package com.study.grade2learning.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.study.grade2learning.model.QuestionItem
import com.study.grade2learning.model.SubjectKey
import com.study.grade2learning.model.TopicNode
import com.study.grade2learning.ui.UiState
import com.study.grade2learning.ui.components.TopBarChip
import com.study.grade2learning.ui.components.TopBarPillButton
import com.study.grade2learning.ui.components.UnifiedTopBar
import com.study.grade2learning.ui.icons.AppIcons
import com.study.grade2learning.ui.theme.AppColors

@Composable
fun QuizScreen(
    state: UiState,
    currentTopic: TopicNode?,
    onExitQuiz: () -> Unit,
    onSelectChoice: (String) -> Unit,
    onConfirmChoice: () -> Unit,
    onPressKeypad: (String) -> Unit,
    onSkip: () -> Unit,
    onNextStep: () -> Unit,
    onOpenReport: () -> Unit,
    onPlayEnglishPrompt: () -> Unit,
    onPlayEnglishAnswer: () -> Unit
) {
    val q = state.currentQuestion
    val outcome = state.answerOutcome
    val totalSteps = state.sessionTopics.size.coerceAtLeast(1)
    val stepNum = (state.sessionTopicIdx + 1).coerceAtMost(totalSteps)

    Column(modifier = Modifier.fillMaxSize()) {
        UnifiedTopBar(
            leftContent = {
                TopBarPillButton(
                    text = "← 退出练习",
                    bg = AppColors.PillMutedBg,
                    textColor = AppColors.TextSecondary,
                    onClick = onExitQuiz
                )
                val edition = state.activeDataset?.edition ?: state.subject.defaultEdition
                val topicLabel = currentTopic?.name ?: "自适应诊断"
                TopBarChip(
                    text = "$edition · $topicLabel",
                    bg = AppColors.MathBadgeBg,
                    textColor = AppColors.MathTextDark,
                    borderColor = Color(0xFFD4E1EC)
                )
            },
            rightContent = {
                // Uniform 36dp progress pill
                Row(
                    modifier = Modifier
                        .height(36.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(AppColors.SelectedChipBg)
                        .border(1.dp, AppColors.BorderWarm, RoundedCornerShape(12.dp))
                        .padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "进度 $stepNum / $totalSteps",
                        color = AppColors.TextMuted,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        for (i in 0 until totalSteps.coerceAtMost(5)) {
                            val pillColor = when {
                                i < state.sessionTopicIdx -> AppColors.MasteredDot
                                i == state.sessionTopicIdx -> AppColors.MathProgress
                                else -> Color(0xFFDCD7C9)
                            }
                            Box(
                                modifier = Modifier
                                    .size(width = 18.dp, height = 8.dp)
                                    .clip(RoundedCornerShape(99.dp))
                                    .background(pillColor)
                            )
                        }
                    }
                }

                // Audio playback ONLY for English (never Chinese or Math)
                if (q?.subject == SubjectKey.ENGLISH) {
                    TopBarPillButton(
                        text = "朗读英文题干",
                        bg = AppColors.EnBadgeBg,
                        textColor = AppColors.EnTextDark,
                        borderColor = AppColors.EnBorder,
                        leadingIcon = AppIcons.Speaker,
                        onClick = onPlayEnglishPrompt
                    )
                }
            }
        )

        if (q == null) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize()
                    .background(AppColors.PaperBg),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "暂无题目可加载，请返回单元地图选择科目。",
                    color = AppColors.TextMuted,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            return
        }

        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxSize()
        ) {
            // Left Pane (7/12): Question & Feedback
            Column(
                modifier = Modifier
                    .weight(7f)
                    .fillMaxHeight()
                    .background(AppColors.PaperBg)
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val diffText = when (q.difficulty) {
                        "basic" -> "基础"
                        "challenge" -> "拓展"
                        else -> "标准"
                    }
                    Text(
                        text = "题目难度：$diffText",
                        color = AppColors.TextMuted,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "连续答对 ${state.parentSettings.masteryStreak} 题即可掌握",
                        color = AppColors.TextMuted,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                // Prompt Card
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(AppColors.CardSurface)
                        .border(1.dp, AppColors.BorderWarm, RoundedCornerShape(24.dp))
                        .padding(22.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = q.prompt,
                        color = AppColors.TextPrimary,
                        fontSize = 22.sp,
                        lineHeight = 32.sp,
                        fontWeight = FontWeight.Bold
                    )
                    if (q.subject == SubjectKey.ENGLISH) {
                        Row(
                            modifier = Modifier
                                .height(36.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(AppColors.EnBadgeBg)
                                .border(1.dp, AppColors.EnBorder, RoundedCornerShape(12.dp))
                                .clickable(onClick = onPlayEnglishPrompt)
                                .padding(horizontal = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = AppIcons.Speaker,
                                contentDescription = null,
                                tint = AppColors.EnTextDark,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "点读英文原声（预置离线发音）",
                                color = AppColors.EnTextDark,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Choice Options vs Numeric Input Box
                if (q.type == "choice" && q.choices.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        q.choices.chunked(2).forEach { rowChoices ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                rowChoices.forEach { choice ->
                                    val isRightAns = choice.key.equals(q.answer, ignoreCase = true)
                                    val isPicked = state.selectedChoiceKey == choice.key
                                    val (bg, border, textColor) = when {
                                        outcome != null && isRightAns -> listOf(
                                            AppColors.MasteredBg,
                                            AppColors.MasteredDot,
                                            AppColors.EnTextDark
                                        )
                                        outcome != null && isPicked && !outcome.isCorrect -> listOf(
                                            AppColors.WeakSurface,
                                            AppColors.WeakPrimary,
                                            AppColors.WeakTextDark
                                        )
                                        outcome == null && isPicked -> listOf(
                                            AppColors.MathBadgeBg,
                                            AppColors.MathPrimary,
                                            AppColors.MathTextDark
                                        )
                                        else -> listOf(
                                            AppColors.CardSurface,
                                            AppColors.BorderWarm,
                                            AppColors.TextPrimary
                                        )
                                    }
                                    val badgeBg = when {
                                        outcome == null && isPicked -> AppColors.MathPrimary
                                        outcome != null && isRightAns -> AppColors.MasteredDot
                                        outcome != null && isPicked && !outcome.isCorrect -> AppColors.WeakPrimary
                                        else -> AppColors.SelectedChipBg
                                    }
                                    val badgeTextColor = when {
                                        (outcome == null && isPicked) ||
                                            (outcome != null && (isRightAns || (isPicked && !outcome.isCorrect))) ->
                                            AppColors.TextOnAccent
                                        else -> AppColors.TextSecondary
                                    }

                                    Row(
                                        modifier = Modifier
                                            .weight(1f)
                                            .heightIn(min = 64.dp)
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(bg)
                                            .border(
                                                if (outcome == null && isPicked) 2.dp else 1.5.dp,
                                                border,
                                                RoundedCornerShape(16.dp)
                                            )
                                            .clickable(enabled = outcome == null) {
                                                onSelectChoice(choice.key)
                                            }
                                            .padding(horizontal = 14.dp, vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(34.dp)
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(badgeBg),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = choice.key,
                                                color = badgeTextColor,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.ExtraBold
                                            )
                                        }
                                        Text(
                                            text = choice.text,
                                            color = textColor,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                                if (rowChoices.size == 1) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(62.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(AppColors.CardSurface)
                            .border(2.dp, AppColors.MathProgress, RoundedCornerShape(16.dp))
                            .padding(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (q.subject == SubjectKey.MATH) {
                                "填写答案（可用右侧草稿纸算好再填）："
                            } else {
                                "请用右侧数字键盘输入答案："
                            },
                            color = AppColors.TextMuted,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = state.numericInput.ifBlank { "右侧点击数字输入" },
                            color = if (state.numericInput.isBlank()) AppColors.TextSubtle else AppColors.MathTextDark,
                            fontSize = if (state.numericInput.isBlank()) 15.sp else 24.sp,
                            fontWeight = if (state.numericInput.isBlank()) FontWeight.Normal else FontWeight.ExtraBold
                        )
                    }
                }

                // Feedback or Helper Footer
                if (outcome != null) {
                    val fbBg = if (outcome.isCorrect) AppColors.MasteredBg else AppColors.ShakyBg
                    val fbBorder = if (outcome.isCorrect) AppColors.MasteredBorder else AppColors.ShakyBorder
                    val fbAccent = if (outcome.isCorrect) AppColors.EnTextDark else AppColors.ZhTextDark

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(fbBg)
                            .border(1.dp, fbBorder, RoundedCornerShape(16.dp))
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = if (outcome.isCorrect) AppIcons.CheckCircle else AppIcons.InfoCircle,
                                contentDescription = null,
                                tint = fbAccent,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = outcome.headline,
                                color = fbAccent,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }

                        if (!outcome.isCorrect && q.type != "choice") {
                            Text(
                                text = "正确答案：${q.answer}",
                                color = fbAccent,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Text(
                            text = outcome.detail,
                            color = AppColors.TextSecondary,
                            fontSize = 13.sp,
                            lineHeight = 20.sp
                        )

                        if (outcome.backtrackHint != null) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(AppColors.CardSurface)
                                    .border(1.dp, AppColors.WeakBorder, RoundedCornerShape(12.dp))
                                    .padding(10.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Icon(
                                    imageVector = AppIcons.Route,
                                    contentDescription = null,
                                    tint = AppColors.WeakTextDark,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = outcome.backtrackHint,
                                    color = AppColors.WeakTextDark,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (q.subject == SubjectKey.ENGLISH) {
                                Row(
                                    modifier = Modifier
                                        .height(36.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(AppColors.EnBadgeBg)
                                        .border(1.dp, AppColors.EnBorder, RoundedCornerShape(12.dp))
                                        .clickable(onClick = onPlayEnglishAnswer)
                                        .padding(horizontal = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    Icon(
                                        imageVector = AppIcons.Speaker,
                                        contentDescription = null,
                                        tint = AppColors.EnTextDark,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = "听答案发音",
                                        color = AppColors.EnTextDark,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                            }

                            Box(
                                modifier = Modifier
                                    .height(36.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(AppColors.CardSurface)
                                    .border(1.dp, Color(0xFFDEDAD0), RoundedCornerShape(12.dp))
                                    .clickable(onClick = onOpenReport)
                                    .padding(horizontal = 14.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "查看诊断溯源报告",
                                    color = AppColors.TextSecondary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            Row(
                                modifier = Modifier
                                    .height(36.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(AppColors.MathPrimary)
                                    .clickable(onClick = onNextStep)
                                    .padding(horizontal = 16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "下一题",
                                    color = AppColors.TextOnAccent,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Icon(
                                    imageVector = AppIcons.ArrowRight,
                                    contentDescription = null,
                                    tint = AppColors.TextOnAccent,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                } else {
                    val isChoiceQuestion = q.type == "choice" && q.choices.isNotEmpty()
                    val selectedKey = state.selectedChoiceKey
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = when {
                                isChoiceQuestion && selectedKey != null ->
                                    "已选 $selectedKey 项，可点其他选项修改或再次点击取消，确认无误后提交"
                                isChoiceQuestion ->
                                    "先点击选择一个答案，再点击右侧「确认提交」按钮"
                                q.subject == SubjectKey.MATH ->
                                    "可用右侧竖式草稿纸演算后点击数字填写"
                                q.subject == SubjectKey.CHINESE ->
                                    "仔细阅读短文后选择正确选项"
                                else ->
                                    "可点击上方或题干旁的英文点读听标准发音"
                            },
                            color = if (isChoiceQuestion && selectedKey != null) AppColors.MathTextDark else AppColors.TextMuted,
                            fontSize = 12.sp,
                            fontWeight = if (isChoiceQuestion && selectedKey != null) FontWeight.SemiBold else FontWeight.Normal,
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 12.dp)
                        )

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier
                                    .height(38.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(AppColors.SelectedChipBg)
                                    .clickable(onClick = onSkip)
                                    .padding(horizontal = 14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = AppIcons.Bulb,
                                    contentDescription = null,
                                    tint = AppColors.TextSecondary,
                                    modifier = Modifier.size(15.dp)
                                )
                                Text(
                                    text = "不会，跳过",
                                    color = AppColors.TextSecondary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            if (isChoiceQuestion) {
                                val canSubmit = selectedKey != null
                                Row(
                                    modifier = Modifier
                                        .height(38.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (canSubmit) AppColors.MathPrimary else AppColors.PillMutedBg)
                                        .border(
                                            1.dp,
                                            if (canSubmit) AppColors.MathPrimary else AppColors.BorderWarm,
                                            RoundedCornerShape(12.dp)
                                        )
                                        .clickable(enabled = canSubmit, onClick = onConfirmChoice)
                                        .padding(horizontal = 18.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = AppIcons.CheckCircle,
                                        contentDescription = null,
                                        tint = if (canSubmit) AppColors.TextOnAccent else AppColors.TextSubtle,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Text(
                                        text = if (canSubmit) "确认提交 ($selectedKey)" else "请先选择选项",
                                        color = if (canSubmit) AppColors.TextOnAccent else AppColors.TextSubtle,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            VerticalDivider(thickness = 1.dp, color = AppColors.DividerWarm)

            // Right Pane (5/12): Scratchpad + Keypad (Math or Numeric English) vs Criteria Card (Chinese/Choice English)
            Column(
                modifier = Modifier
                    .weight(5f)
                    .fillMaxHeight()
                    .background(AppColors.RailBg)
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (q.subject == SubjectKey.MATH || q.type == "numeric") {
                    MathScratchpadCard(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        resetSeed = state.scratchResetSeed
                    )
                    NumericKeypadGrid(
                        onPressKey = onPressKeypad
                    )
                } else {
                    KnowledgeCriteriaRightCard(
                        question = q,
                        topic = currentTopic,
                        gradeLabel = state.profile.grade,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}

@Composable
private fun MathScratchpadCard(
    modifier: Modifier,
    resetSeed: Int
) {
    val strokes = remember { mutableStateListOf<List<Offset>>() }
    var activeStroke by remember { mutableStateOf<List<Offset>>(emptyList()) }

    LaunchedEffect(resetSeed) {
        strokes.clear()
        activeStroke = emptyList()
    }

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(AppColors.ScratchGridBg)
            .border(1.dp, Color(0xFFD4E1EC), RoundedCornerShape(16.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(38.dp)
                .background(Color(0xFFF5F3EA))
                .padding(horizontal = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = AppIcons.Pen,
                    contentDescription = null,
                    tint = AppColors.MathTextDark,
                    modifier = Modifier.size(15.dp)
                )
                Text(
                    text = "常驻竖式草稿纸（直接写画）",
                    color = AppColors.MathTextDark,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val canUndo = strokes.isNotEmpty()
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (canUndo) Color(0xFFEAE6DB) else Color(0xFFF1EEE6))
                        .clickable(enabled = canUndo) {
                            if (strokes.isNotEmpty()) {
                                strokes.removeAt(strokes.lastIndex)
                            }
                        }
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "撤销",
                        color = if (canUndo) AppColors.TextSecondary else AppColors.TextSubtle,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFEAE6DB))
                        .clickable {
                            strokes.clear()
                            activeStroke = emptyList()
                        }
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "清空",
                        color = AppColors.TextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        HorizontalDivider(thickness = 1.dp, color = Color(0xFFE0E6EC))

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clipToBounds()
                .pointerInput(resetSeed) {
                    detectTapGestures { pos ->
                        strokes.add(listOf(pos, pos + Offset(0.5f, 0.5f)))
                    }
                }
                .pointerInput(resetSeed) {
                    detectDragGestures(
                        onDragStart = { start ->
                            activeStroke = listOf(start)
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            activeStroke = activeStroke + change.position
                        },
                        onDragEnd = {
                            if (activeStroke.isNotEmpty()) {
                                strokes.add(activeStroke)
                            }
                            activeStroke = emptyList()
                        },
                        onDragCancel = {
                            activeStroke = emptyList()
                        }
                    )
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val step = 22.dp.toPx()
                val gridColor = Color(0xFF4E7FA8).copy(alpha = 0.08f)
                var x = step
                while (x < size.width) {
                    drawLine(gridColor, Offset(x, 0f), Offset(x, size.height), strokeWidth = 1f)
                    x += step
                }
                var y = step
                while (y < size.height) {
                    drawLine(gridColor, Offset(0f, y), Offset(size.width, y), strokeWidth = 1f)
                    y += step
                }

                val strokeStyle = Stroke(
                    width = 3.dp.toPx(),
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )

                fun drawPoints(pts: List<Offset>) {
                    if (pts.isEmpty()) return
                    val p = Path().apply {
                        moveTo(pts.first().x, pts.first().y)
                        for (i in 1 until pts.size) {
                            lineTo(pts[i].x, pts[i].y)
                        }
                    }
                    drawPath(p, color = Color(0xFF262930), style = strokeStyle)
                }

                strokes.forEach { drawPoints(it) }
                drawPoints(activeStroke)
            }
        }
    }
}

@Composable
private fun NumericKeypadGrid(
    onPressKey: (String) -> Unit
) {
    val rows = listOf(
        listOf("1", "2", "3", "DEL"),
        listOf("4", "5", "6", "."),
        listOf("7", "8", "9", "/"),
        listOf("-", "0", "，", "OK")
    )

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        rows.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                row.forEach { key ->
                    val isOk = key == "OK"
                    val isDel = key == "DEL"
                    val bg = when {
                        isOk -> AppColors.MathPrimary
                        isDel -> AppColors.ShakyBg
                        else -> AppColors.CardSurface
                    }
                    val border = when {
                        isOk -> AppColors.MathPrimary
                        isDel -> AppColors.ShakyBorder
                        else -> Color(0xFFDED9CC)
                    }
                    val fg = when {
                        isOk -> AppColors.TextOnAccent
                        isDel -> AppColors.ZhTextDark
                        else -> AppColors.TextPrimary
                    }
                    val label = when {
                        isOk -> "确定"
                        isDel -> "退格"
                        else -> key
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(bg)
                            .border(1.dp, border, RoundedCornerShape(12.dp))
                            .clickable { onPressKey(key) },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isDel) {
                            Icon(
                                imageVector = AppIcons.Backspace,
                                contentDescription = "退格",
                                tint = fg,
                                modifier = Modifier.size(20.dp)
                            )
                        } else {
                            Text(
                                text = label,
                                color = fg,
                                fontSize = if (isOk) 14.sp else 17.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun KnowledgeCriteriaRightCard(
    question: QuestionItem,
    topic: TopicNode?,
    gradeLabel: String,
    modifier: Modifier = Modifier
) {
    val isEnglish = question.subject == SubjectKey.ENGLISH
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(AppColors.CardSurface)
            .border(1.dp, AppColors.BorderWarm, RoundedCornerShape(16.dp))
            .padding(20.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "本关学习要点",
                    color = AppColors.TextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = "来自知识图谱",
                    color = AppColors.TextSubtle,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFFF3EFE5))
                    .border(1.dp, AppColors.BorderWarm, RoundedCornerShape(16.dp))
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "掌握标准（Mastery Criteria）",
                    color = AppColors.TextMuted,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                val criteria = topic?.masteryCriteria.orEmpty()
                    .ifEmpty { listOf(topic?.description ?: "按考点要求准确完成练习") }
                criteria.forEach { c ->
                    Text(
                        text = "• $c",
                        color = AppColors.TextSecondary,
                        fontSize = 13.sp,
                        lineHeight = 20.sp
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (isEnglish) AppColors.EnBadgeBg else AppColors.ZhBadgeBg)
                    .border(
                        1.dp,
                        if (isEnglish) AppColors.EnBorder else AppColors.ShakyBorder,
                        RoundedCornerShape(16.dp)
                    )
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = if (isEnglish) AppIcons.Speaker else AppIcons.Book,
                        contentDescription = null,
                        tint = AppColors.TextPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = if (isEnglish) "英语点读提示" else "语文阅读建议",
                        color = AppColors.TextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = if (isEnglish) {
                        "英语模块内置 382 段预置离线发音音频，并配备系统 Android TTS 兜底，无网络也能听标准发音。"
                    } else {
                        "语文题目不配语音读题，鼓励${gradeLabel.ifBlank { "当前年级" }}孩子自主识字并从短文中直接找出关键信息。"
                    },
                    color = AppColors.TextSecondary,
                    fontSize = 12.sp,
                    lineHeight = 19.sp
                )
            }
        }

        Text(
            text = "每次提交都会实时更新学习日历与掌握度",
            color = AppColors.TextSubtle,
            fontSize = 12.sp
        )
    }
}
