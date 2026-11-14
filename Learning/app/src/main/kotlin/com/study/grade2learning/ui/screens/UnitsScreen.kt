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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.study.grade2learning.model.NodeStatus
import com.study.grade2learning.model.SubjectKey
import com.study.grade2learning.model.TermFilter
import com.study.grade2learning.model.TopicNode
import com.study.grade2learning.model.UnitGroup
import com.study.grade2learning.ui.UiState
import com.study.grade2learning.ui.components.TermSegmentedToggle
import com.study.grade2learning.ui.components.TopBarPillButton
import com.study.grade2learning.ui.components.UnifiedTopBar
import com.study.grade2learning.ui.icons.AppIcons
import com.study.grade2learning.ui.theme.AppColors

@Composable
fun UnitsScreen(
    state: UiState,
    units: List<UnitGroup>,
    onSelectSubject: (SubjectKey) -> Unit,
    onSelectTerm: (TermFilter) -> Unit,
    onSelectUnit: (Int) -> Unit,
    onStartUnitQuiz: (UnitGroup) -> Unit,
    onPracticeTopic: (SubjectKey, String) -> Unit
) {
    val safeIndex = state.selectedUnitIdx.coerceIn(0, (units.size - 1).coerceAtLeast(0))
    val currentUnit = units.getOrNull(safeIndex)
    val dataset = state.activeDataset

    Column(modifier = Modifier.fillMaxSize()) {
        UnifiedTopBar(
            leftContent = {
                SubjectKey.entries.forEach { subj ->
                    val selected = subj == state.subject
                    val icon = when (subj) {
                        SubjectKey.MATH -> AppIcons.MathSymbol
                        SubjectKey.CHINESE -> AppIcons.Book
                        SubjectKey.ENGLISH -> AppIcons.EnglishAa
                    }
                    TopBarPillButton(
                        text = state.bundle?.subjects?.get(subj)?.edition ?: subj.defaultEdition,
                        bg = if (selected) AppColors.MathPrimary else AppColors.PillMutedBg,
                        textColor = if (selected) AppColors.TextOnAccent else AppColors.TextSecondary,
                        leadingIcon = icon,
                        onClick = { onSelectSubject(subj) }
                    )
                }
            },
            rightContent = {
                TermSegmentedToggle(
                    currentTerm = state.term,
                    onSelectTerm = onSelectTerm
                )
            }
        )

        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxSize()
        ) {
            // Left Pane: Unit List (5/12)
            Column(
                modifier = Modifier
                    .weight(5f)
                    .fillMaxHeight()
                    .background(AppColors.SubPanelBg)
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "教材单元目录（共 ${units.size} 个单元）",
                    color = AppColors.TextMuted,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 8.dp)
                ) {
                    itemsIndexed(units) { idx, unit ->
                        val active = idx == safeIndex
                        val masteredCount = unit.topics.count {
                            state.records[it.id]?.status == NodeStatus.MASTERED
                        }
                        val allDone = masteredCount == unit.topics.size && unit.topics.isNotEmpty()
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (active) AppColors.CardSurface else AppColors.HeaderBg)
                                .border(
                                    width = if (active) 1.5.dp else 1.dp,
                                    color = if (active) AppColors.MathProgress else AppColors.DividerWarm,
                                    shape = RoundedCornerShape(16.dp)
                                )
                                .clickable { onSelectUnit(idx) }
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "${unit.term}册",
                                    color = AppColors.TextSubtle,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "$masteredCount/${unit.topics.size} 已掌握",
                                    color = if (allDone) AppColors.MasteredText else AppColors.TextMuted,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                text = unit.title,
                                color = AppColors.TextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            VerticalDivider(thickness = 1.dp, color = AppColors.DividerWarm)

            // Right Pane: Topic Nodes Detail (7/12)
            Column(
                modifier = Modifier
                    .weight(7f)
                    .fillMaxHeight()
                    .background(AppColors.PaperBg)
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (currentUnit != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                text = "${dataset?.edition ?: state.subject.defaultEdition} · ${currentUnit.term}册",
                                color = AppColors.MathPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = currentUnit.title,
                                color = AppColors.TextPrimary,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.ExtraBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Box(
                            modifier = Modifier
                                .height(36.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(AppColors.MathPrimary)
                                .clickable { onStartUnitQuiz(currentUnit) }
                                .padding(horizontal = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "诊断本单元漏洞",
                                color = AppColors.TextOnAccent,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(bottom = 8.dp)
                    ) {
                        items(currentUnit.topics, key = { it.id }) { topic ->
                            val status = state.records[topic.id]?.status ?: NodeStatus.UNKNOWN
                            TopicDetailCard(
                                topic = topic,
                                status = status,
                                onPractice = { onPracticeTopic(state.subject, topic.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TopicDetailCard(
    topic: TopicNode,
    status: NodeStatus,
    onPractice: () -> Unit
) {
    val (bg, border, dot, text) = remember(status) {
        when (status) {
            NodeStatus.MASTERED -> listOf(AppColors.MasteredBg, AppColors.MasteredBorder, AppColors.MasteredDot, AppColors.MasteredText)
            NodeStatus.SHAKY -> listOf(AppColors.ShakyBg, AppColors.ShakyBorder, AppColors.ShakyDot, AppColors.ShakyText)
            NodeStatus.WEAK -> listOf(AppColors.WeakSurface, AppColors.WeakBorder, AppColors.WeakPrimary, AppColors.WeakTextDark)
            NodeStatus.UNKNOWN -> listOf(AppColors.UnknownBg, AppColors.UnknownBorder, AppColors.UnknownDot, AppColors.UnknownText)
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(AppColors.CardSurface)
            .border(1.dp, AppColors.BorderWarm, RoundedCornerShape(16.dp))
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.Top
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
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(dot)
                )
                Text(
                    text = topic.name,
                    color = AppColors.TextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(bg)
                        .border(1.dp, border, RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = status.label,
                        color = text,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            val criteria = topic.masteryCriteria.ifEmpty { listOf(topic.description.ifBlank { "完成该知识点核心练习题" }) }
            criteria.take(3).forEach { criterion ->
                Text(
                    text = "• $criterion",
                    color = AppColors.TextSecondary,
                    fontSize = 12.sp,
                    lineHeight = 18.sp
                )
            }
        }

        Row(
            modifier = Modifier
                .height(36.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(AppColors.PillMutedBg)
                .clickable(onClick = onPractice)
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "去练习",
                color = AppColors.TextPrimary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
            Icon(
                imageVector = AppIcons.ArrowRight,
                contentDescription = null,
                tint = AppColors.TextPrimary,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}
