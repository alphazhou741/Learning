package com.study.grade2learning.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.study.grade2learning.model.DayStudyRecord
import com.study.grade2learning.model.NodeStatus
import com.study.grade2learning.model.SubjectKey
import com.study.grade2learning.model.SyncState
import com.study.grade2learning.model.TermFilter
import com.study.grade2learning.ui.UiState
import com.study.grade2learning.ui.components.TermSegmentedToggle
import com.study.grade2learning.ui.components.TopBarChip
import com.study.grade2learning.ui.components.UnifiedTopBar
import com.study.grade2learning.ui.icons.AppIcons
import com.study.grade2learning.ui.theme.AppColors
import java.time.LocalDate
import java.time.YearMonth

private data class SubjectCardPalette(
    val subject: SubjectKey,
    val icon: ImageVector,
    val cardBg: Color,
    val border: Color,
    val softBg: Color,
    val primary: Color,
    val darkText: Color,
    val progress: Color
)

private val SUBJECT_CARD_PALETTES = listOf(
    SubjectCardPalette(
        subject = SubjectKey.MATH,
        icon = AppIcons.MathSymbol,
        cardBg = AppColors.MathCardBg,
        border = AppColors.MathBorder,
        softBg = AppColors.MathSoftBg,
        primary = AppColors.MathPrimary,
        darkText = AppColors.MathTextDark,
        progress = AppColors.MathProgress
    ),
    SubjectCardPalette(
        subject = SubjectKey.CHINESE,
        icon = AppIcons.Book,
        cardBg = AppColors.ZhCardBg,
        border = AppColors.ZhBorder,
        softBg = AppColors.ZhSoftBg,
        primary = AppColors.ZhPrimary,
        darkText = AppColors.ZhTextDark,
        progress = AppColors.ZhProgress
    ),
    SubjectCardPalette(
        subject = SubjectKey.ENGLISH,
        icon = AppIcons.EnglishAa,
        cardBg = AppColors.EnCardBg,
        border = AppColors.EnBorder,
        softBg = AppColors.EnSoftBg,
        primary = AppColors.EnPrimary,
        darkText = AppColors.EnTextDark,
        progress = AppColors.EnProgress
    )
)

@Composable
fun HomeScreen(
    state: UiState,
    onSelectTerm: (TermFilter) -> Unit,
    onSelectDate: (String) -> Unit,
    onSyncNow: () -> Unit,
    onOpenUnits: (SubjectKey) -> Unit,
    onStartAdaptiveQuiz: (SubjectKey) -> Unit,
    onStartBlindspotTopic: (SubjectKey, String) -> Unit
) {
    val bundle = state.bundle
    val todayDate = remember { LocalDate.now() }
    val yearMonth = remember(todayDate) { YearMonth.from(todayDate) }
    val todayDay = todayDate.dayOfMonth
    val selectedDay = remember(state.selectedDate, yearMonth) {
        val parsed = runCatching { LocalDate.parse(state.selectedDate) }.getOrNull()
        if (parsed != null && YearMonth.from(parsed) == yearMonth) parsed.dayOfMonth else todayDay
    }

    val flaggedTopics = remember(bundle, state.records) {
        if (bundle == null) emptyList()
        else bundle.subjects.values.flatMap { ds ->
            ds.topics.filter { t ->
                val st = state.records[t.id]?.status ?: NodeStatus.UNKNOWN
                st == NodeStatus.WEAK || st == NodeStatus.SHAKY
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        UnifiedTopBar(
            leftContent = {
                Text(
                    text = "下午好，${state.profile.name}！有空来学一点",
                    color = AppColors.TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                TopBarChip(
                    text = "苏教数学 + 人教语文 + PEP 英语（${state.term.label}）",
                    bg = AppColors.MathBadgeBg,
                    textColor = AppColors.MathTextDark,
                    borderColor = Color(0xFFD4E1EC)
                )
            },
            rightContent = {
                val (syncBg, syncTextColor, syncBorder) = when (state.syncState) {
                    SyncState.IDLE -> Triple(AppColors.EnBadgeBg, AppColors.EnTextDark, AppColors.MasteredBorder)
                    SyncState.SYNCING -> Triple(AppColors.MathBadgeBg, AppColors.MathTextDark, AppColors.MathBorder)
                    SyncState.PENDING -> Triple(AppColors.ZhBadgeBg, AppColors.ZhTextDark, AppColors.ZhBorder)
                    SyncState.OFFLINE -> Triple(AppColors.WeakSurface, AppColors.WeakTextDark, AppColors.WeakBorder)
                }
                val syncLabel = if (state.pendingSyncCount > 0 && state.syncState != SyncState.SYNCING) {
                    "${state.syncState.label}(${state.pendingSyncCount})"
                } else {
                    state.syncState.label
                }
                Box(
                    modifier = Modifier
                        .height(32.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(syncBg)
                        .border(1.dp, syncBorder, RoundedCornerShape(10.dp))
                        .clickable(onClick = onSyncNow)
                        .padding(horizontal = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = syncLabel,
                        color = syncTextColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                TermSegmentedToggle(
                    currentTerm = state.term,
                    onSelectTerm = onSelectTerm
                )
            }
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Top Row: Study Calendar (7) + Blindspots (5)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(216.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                StudyCalendarCard(
                    modifier = Modifier
                        .weight(7f)
                        .fillMaxHeight(),
                    yearMonth = yearMonth,
                    todayDay = todayDay,
                    selectedDay = selectedDay,
                    studyLog = state.studyLog,
                    onSelectDay = { day -> onSelectDate(yearMonth.atDay(day).toString()) },
                    onQuickPractice = { onStartAdaptiveQuiz(SubjectKey.MATH) }
                )

                BlindspotCard(
                    modifier = Modifier
                        .weight(5f)
                        .fillMaxHeight(),
                    flaggedCount = flaggedTopics.size,
                    flaggedNames = flaggedTopics.take(3).map { it.name },
                    onStartBlindspot = {
                        val first = flaggedTopics.firstOrNull()
                        if (first != null) {
                            onStartBlindspotTopic(first.subject, first.id)
                        } else {
                            onStartAdaptiveQuiz(SubjectKey.MATH)
                        }
                    }
                )
            }

            // Bottom Row: 3 Subject Cards filling remaining space cleanly
            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                SUBJECT_CARD_PALETTES.forEach { palette ->
                    val ds = bundle?.subjects?.get(palette.subject)
                    val termTargets = remember(ds, state.term) {
                        ds?.topics?.filter {
                            it.isTarget && (state.term == TermFilter.ALL || it.term == state.term.key)
                        }.orEmpty()
                    }
                    val total = termTargets.size.coerceAtLeast(1)
                    val mastered = termTargets.count {
                        state.records[it.id]?.status == NodeStatus.MASTERED
                    }
                    val pct = (mastered * 100) / total
                    val nextRec = termTargets.firstOrNull {
                        state.records[it.id]?.status != NodeStatus.MASTERED
                    }?.name ?: termTargets.firstOrNull()?.name ?: "已完成当前全部考点"

                    SubjectOverviewCard(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        palette = palette,
                        editionBadge = "${ds?.edition ?: palette.subject.defaultEdition} · ${state.term.label}",
                        subjectName = ds?.title ?: palette.subject.title,
                        mastered = mastered,
                        total = termTargets.size,
                        pct = pct,
                        recommendedTopic = nextRec,
                        onOpenUnits = { onOpenUnits(palette.subject) },
                        onStartQuiz = { onStartAdaptiveQuiz(palette.subject) }
                    )
                }
            }
        }
    }
}

@Composable
private fun StudyCalendarCard(
    modifier: Modifier,
    yearMonth: YearMonth,
    todayDay: Int,
    selectedDay: Int,
    studyLog: Map<String, DayStudyRecord>,
    onSelectDay: (Int) -> Unit,
    onQuickPractice: () -> Unit
) {
    val monthPrefix = yearMonth.toString()
    val monthLogs = studyLog.filterKeys { it.startsWith(monthPrefix) }
    val totalQuestions = monthLogs.values.sumOf { it.totalQuestions }
    val selectedDateKey = yearMonth.atDay(selectedDay.coerceIn(1, yearMonth.lengthOfMonth())).toString()
    val selectedRecord = studyLog[selectedDateKey]
    val firstWeekdayOffset = remember(yearMonth) { yearMonth.atDay(1).dayOfWeek.value - 1 }
    val daysInMonth = remember(yearMonth) { yearMonth.lengthOfMonth() }

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(AppColors.HeaderBg)
            .border(1.dp, AppColors.BorderWarm, RoundedCornerShape(18.dp))
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Left Summary & Legend
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = AppIcons.Calendar,
                        contentDescription = null,
                        tint = AppColors.MathPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "${yearMonth.monthValue}月学习日历",
                        color = AppColors.TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFE6EFF6))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "本月学了 ${monthLogs.size} 天 · 共 $totalQuestions 题",
                        color = AppColors.MathTextDark,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // Selected Day Details Box
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(AppColors.SelectedChipBg)
                    .border(1.dp, Color(0xFFE0DCD0), RoundedCornerShape(12.dp))
                    .padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "${yearMonth.monthValue}月${selectedDay}日" + if (selectedDay == todayDay) "（今天）" else "",
                        color = AppColors.TextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = when {
                            selectedRecord != null -> "约 ${selectedRecord.minutes} 分钟"
                            selectedDay > todayDay -> "尚未到这一天"
                            else -> "自由休息日"
                        },
                        color = AppColors.TextMuted,
                        fontSize = 12.sp
                    )
                }
                if (selectedRecord != null) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (selectedRecord.mathCount > 0) {
                            DaySubjectChip("数学 ${selectedRecord.mathCount} 题", AppColors.MathBadgeBg, AppColors.MathTextDark)
                        }
                        if (selectedRecord.chineseCount > 0) {
                            DaySubjectChip("语文 ${selectedRecord.chineseCount} 题", AppColors.ZhBadgeBg, AppColors.ZhTextDark)
                        }
                        if (selectedRecord.englishCount > 0) {
                            DaySubjectChip("英语 ${selectedRecord.englishCount} 题", AppColors.EnBadgeBg, AppColors.EnTextDark)
                        }
                    }
                    Text(
                        text = selectedRecord.note.ifBlank { "按自己节奏完成巩固练习" },
                        color = AppColors.TextSecondary,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                } else {
                    Text(
                        text = if (selectedDay > todayDay) {
                            "留给自己自由安排，有空再来学。"
                        } else {
                            "这一天没有做题记录，点击绿色格子查看学习足迹。"
                        },
                        color = AppColors.TextMuted,
                        fontSize = 12.sp,
                        maxLines = 2
                    )
                }
            }

            // 3-State Legend + Quick CTA
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    CalendarLegendItem("已学习", AppColors.CalStudied, null)
                    CalendarLegendItem("今天", AppColors.CalToday, null)
                    CalendarLegendItem("未学习", AppColors.CalUnstudied, Color(0xFFD6D1C3))
                }
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(onClick = onQuickPractice)
                        .padding(horizontal = 4.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "随时练几题",
                        color = AppColors.MathPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Icon(
                        imageVector = AppIcons.ArrowRight,
                        contentDescription = null,
                        tint = AppColors.MathPrimary,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }

        // Right Interactive Calendar Grid
        Column(
            modifier = Modifier
                .width(236.dp)
                .fillMaxHeight()
                .clip(RoundedCornerShape(14.dp))
                .background(AppColors.SelectedChipBg)
                .border(1.dp, Color(0xFFE0DCD0), RoundedCornerShape(14.dp))
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                listOf("一", "二", "三", "四", "五", "六", "日").forEach { w ->
                    Text(
                        text = w,
                        color = AppColors.TextMuted,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.width(26.dp)
                    )
                }
            }

            val totalCells = firstWeekdayOffset + daysInMonth
            val rows = (totalCells + 6) / 7
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                for (r in 0 until rows) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        for (c in 0 until 7) {
                            val cellIdx = r * 7 + c
                            val dayNum = cellIdx - firstWeekdayOffset + 1
                            if (dayNum in 1..daysInMonth) {
                                val dayKey = yearMonth.atDay(dayNum).toString()
                                val hasLog = studyLog.containsKey(dayKey)
                                val isToday = dayNum == todayDay
                                val isFuture = dayNum > todayDay
                                val isSelected = dayNum == selectedDay

                                val bg = when {
                                    isToday -> AppColors.CalToday
                                    hasLog -> AppColors.CalStudied
                                    isFuture -> Color.Transparent
                                    else -> AppColors.CalUnstudied
                                }
                                val fg = when {
                                    isToday || hasLog -> AppColors.CardSurface
                                    isFuture -> AppColors.CalFutureText
                                    else -> AppColors.CalUnstudiedText
                                }
                                Box(
                                    modifier = Modifier
                                        .size(width = 26.dp, height = 22.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(bg)
                                        .then(
                                            if (isSelected) {
                                                Modifier.border(
                                                    1.5.dp,
                                                    AppColors.MathTextDark,
                                                    RoundedCornerShape(6.dp)
                                                )
                                            } else Modifier
                                        )
                                        .clickable { onSelectDay(dayNum) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = dayNum.toString(),
                                        color = fg,
                                        fontSize = 12.sp,
                                        fontWeight = if (isToday || hasLog) FontWeight.Bold else FontWeight.Medium
                                    )
                                }
                            } else {
                                Spacer(modifier = Modifier.size(width = 26.dp, height = 22.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DaySubjectChip(label: String, bg: Color, fg: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bg)
            .padding(horizontal = 8.dp, vertical = 2.dp)
    ) {
        Text(text = label, color = fg, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun CalendarLegendItem(label: String, color: Color, borderColor: Color?) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(color)
                .then(
                    if (borderColor != null) Modifier.border(1.dp, borderColor, RoundedCornerShape(3.dp))
                    else Modifier
                )
        )
        Text(text = label, color = AppColors.TextMuted, fontSize = 12.sp)
    }
}

@Composable
private fun BlindspotCard(
    modifier: Modifier,
    flaggedCount: Int,
    flaggedNames: List<String>,
    onStartBlindspot: () -> Unit
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(AppColors.BlindspotCardBg)
            .border(1.dp, AppColors.BlindspotBorder, RoundedCornerShape(18.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = AppIcons.Target,
                        contentDescription = null,
                        tint = AppColors.WeakTextDark,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "$flaggedCount 个待消灭盲区",
                        color = Color(0xFF7D3C3C),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFFBF4F4))
                        .border(1.dp, Color(0xFFEAD4D4), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = if (flaggedCount > 0) "优先巩固" else "暂无盲区",
                        color = AppColors.WeakTextDark,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            Text(
                text = if (flaggedNames.isNotEmpty()) {
                    flaggedNames.joinToString(" · ")
                } else {
                    "目前还没有待巩固薄弱点，做题遇到卡壳会自动加入这里。"
                },
                color = Color(0xFF945858),
                fontSize = 12.sp,
                lineHeight = 18.sp,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(Color(0xFFEBD5D5))
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (flaggedCount > 0) "短时复习约 5 分钟" else "完成体检或单元练习后生成",
                    color = Color(0xFF945858),
                    fontSize = 12.sp
                )
                Box(
                    modifier = Modifier
                        .height(36.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(AppColors.WeakPrimary)
                        .clickable(onClick = onStartBlindspot)
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (flaggedCount > 0) "一键重练" else "开始摸底",
                        color = AppColors.TextOnAccent,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun SubjectOverviewCard(
    modifier: Modifier,
    palette: SubjectCardPalette,
    editionBadge: String,
    subjectName: String,
    mastered: Int,
    total: Int,
    pct: Int,
    recommendedTopic: String,
    onOpenUnits: () -> Unit,
    onStartQuiz: () -> Unit
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(palette.cardBg)
            .border(1.dp, palette.border, RoundedCornerShape(24.dp))
            .padding(20.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(palette.softBg)
                        .border(1.dp, palette.border, RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = editionBadge,
                        color = palette.darkText,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(palette.softBg)
                        .border(1.dp, palette.border, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = palette.icon,
                        contentDescription = subjectName,
                        tint = palette.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = subjectName,
                    color = AppColors.TextPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = "$pct%",
                    color = palette.darkText,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "已掌握 $mastered / $total 个知识点",
                    color = AppColors.TextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(7.dp)
                        .clip(RoundedCornerShape(99.dp))
                        .background(Color(0xFFF7F5EF))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth((pct / 100f).coerceIn(0f, 1f))
                            .clip(RoundedCornerShape(99.dp))
                            .background(palette.progress)
                    )
                }
            }

            Text(
                text = "推荐关卡：$recommendedTopic",
                color = AppColors.TextSecondary,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(palette.softBg)
                    .border(1.dp, palette.border, RoundedCornerShape(12.dp))
                    .clickable(onClick = onOpenUnits),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "进入单元地图",
                    color = palette.darkText,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(palette.primary)
                    .clickable(onClick = onStartQuiz),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "做本册自适应体检",
                    color = AppColors.TextOnAccent,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
