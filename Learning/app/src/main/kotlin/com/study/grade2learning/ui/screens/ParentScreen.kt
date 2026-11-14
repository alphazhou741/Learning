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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.study.grade2learning.model.EnglishAudioMode
import com.study.grade2learning.model.SubjectKey
import com.study.grade2learning.ui.UiState
import com.study.grade2learning.ui.components.TopBarChip
import com.study.grade2learning.ui.components.TopBarPillButton
import com.study.grade2learning.ui.components.UnifiedTopBar
import com.study.grade2learning.ui.components.avatarToneFor
import com.study.grade2learning.ui.icons.AppIcons
import com.study.grade2learning.ui.theme.AppColors

@Composable
fun ParentScreen(
    state: UiState,
    onUnlock: (String, Boolean) -> Unit,
    onLock: () -> Unit,
    onBackHome: () -> Unit,
    onEditProfile: () -> Unit,
    onOpenReport: () -> Unit,
    onUpdateMasteryStreak: (Int) -> Unit,
    onUpdateEnglishAudioMode: (EnglishAudioMode) -> Unit,
    onUpdateSpeechRate: (Float) -> Unit,
    onExportBackup: () -> Unit,
    onResetAllData: (accountPassword: String, onSuccess: () -> Unit) -> Unit,
    onClearResetError: () -> Unit,
    onSyncNow: () -> Unit,
    onLogout: () -> Unit,
    onDismissNotice: () -> Unit
) {
    if (!state.parentUnlocked) {
        ParentLockedView(
            hasError = state.parentGateError,
            onVerify = { input -> onUnlock(input, false) },
            onBypass = { onUnlock("", true) },
            onBackHome = onBackHome
        )
        return
    }

    val tone = avatarToneFor(state.profile.avatarTone)
    val shortName = if (state.profile.name.length > 2) state.profile.name.takeLast(2) else state.profile.name
    val bundle = state.bundle
    var showResetConfirmDialog by remember { mutableStateOf(false) }
    var resetPassword by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize()) {
        UnifiedTopBar(
            leftContent = {
                Icon(
                    imageVector = AppIcons.Shield,
                    contentDescription = null,
                    tint = AppColors.EnTextDark,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "家长中心 · 规则与档案管理",
                    color = AppColors.TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Box(
                    modifier = Modifier
                        .height(30.dp)
                        .clip(RoundedCornerShape(99.dp))
                        .background(AppColors.EnBadgeBg)
                        .border(1.dp, AppColors.MasteredBorder, RoundedCornerShape(99.dp))
                        .padding(horizontal = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "已解锁",
                        color = AppColors.EnTextDark,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            rightContent = {
                TopBarPillButton(
                    text = "立即同步",
                    bg = AppColors.EnBadgeBg,
                    textColor = AppColors.EnTextDark,
                    borderColor = AppColors.EnBorder,
                    onClick = onSyncNow
                )
                TopBarPillButton(
                    text = "导出备份 JSON",
                    bg = AppColors.CardSurface,
                    textColor = AppColors.TextSecondary,
                    borderColor = AppColors.DividerStrong,
                    leadingIcon = AppIcons.Download,
                    onClick = onExportBackup
                )
                TopBarPillButton(
                    text = "切换账号 / 退出",
                    bg = AppColors.WeakSurface,
                    textColor = AppColors.WeakTextDark,
                    borderColor = AppColors.WeakBorder,
                    onClick = onLogout
                )
                TopBarPillButton(
                    text = "立即锁定",
                    bg = AppColors.PillMutedBg,
                    textColor = AppColors.TextSecondary,
                    onClick = onLock
                )
            }
        )

        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxSize()
        ) {
            // Left Column (5/12): Learner Profile & Bundle Coverage
            Column(
                modifier = Modifier
                    .weight(5f)
                    .fillMaxHeight()
                    .background(AppColors.SubPanelBg)
                    .padding(20.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(24.dp))
                            .background(AppColors.CardSurface)
                            .border(1.dp, AppColors.BorderWarm, RoundedCornerShape(24.dp))
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    text = "当前学习者档案",
                                    color = AppColors.TextMuted,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "账号 ${state.username} · 共 ${state.children.size} 位孩子 · ${state.syncState.label}",
                                    color = AppColors.TextSubtle,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(AppColors.MathBadgeBg)
                                    .border(1.dp, Color(0xFFD2E1EE), RoundedCornerShape(10.dp))
                                    .clickable(onClick = onEditProfile)
                                    .padding(horizontal = 12.dp, vertical = 5.dp)
                            ) {
                                Text(
                                    text = "切换 / 修改",
                                    color = AppColors.MathTextDark,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(RoundedCornerShape(18.dp))
                                    .background(tone.bg)
                                    .border(1.dp, tone.border, RoundedCornerShape(18.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = shortName,
                                    color = tone.text,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = state.profile.name,
                                    color = AppColors.TextPrimary,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                Text(
                                    text = "${state.profile.grade} · 默认展示 ${state.profile.defaultTerm.pillLabel}",
                                    color = AppColors.TextMuted,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    // Bundle coverage card
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .background(AppColors.CardSurface)
                            .border(1.dp, AppColors.BorderWarm, RoundedCornerShape(18.dp))
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "本地离线题库与图谱覆盖",
                            color = AppColors.TextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        val mathDs = bundle?.subjects?.get(SubjectKey.MATH)
                        val zhDs = bundle?.subjects?.get(SubjectKey.CHINESE)
                        val enDs = bundle?.subjects?.get(SubjectKey.ENGLISH)
                        CoverageRow(
                            "苏教版二年级数学（+一年级先修）",
                            "${mathDs?.targetCount ?: 43} + ${mathDs?.prerequisiteCount ?: 32} 点",
                            AppColors.MathTextDark
                        )
                        CoverageRow(
                            "人教版二年级语文（+一年级先修）",
                            "${zhDs?.targetCount ?: 79} + ${zhDs?.prerequisiteCount ?: 79} 点",
                            AppColors.ZhTextDark
                        )
                        CoverageRow(
                            "PEP 英语启蒙（三年级起点）",
                            "${enDs?.targetCount ?: 71} 点 / ${enDs?.questions?.size ?: 284} 题",
                            AppColors.EnTextDark
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(42.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(AppColors.MathPrimary)
                        .clickable(onClick = onOpenReport),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "前往「报告」查看先修根因阶梯",
                        color = AppColors.TextOnAccent,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Icon(
                        imageVector = AppIcons.ArrowRight,
                        contentDescription = null,
                        tint = AppColors.TextOnAccent,
                        modifier = Modifier
                            .padding(start = 6.dp)
                            .size(14.dp)
                    )
                }
            }

            VerticalDivider(thickness = 1.dp, color = AppColors.DividerWarm)

            // Right Column (7/12): Rules & Data Reset
            Column(
                modifier = Modifier
                    .weight(7f)
                    .fillMaxHeight()
                    .background(AppColors.PaperBg)
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                if (state.parentNotice.isNotBlank()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(AppColors.EnBadgeBg)
                            .border(1.dp, AppColors.MasteredBorder, RoundedCornerShape(14.dp))
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = state.parentNotice,
                            color = AppColors.EnTextDark,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = "×",
                            color = AppColors.MasteredText,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .clickable(onClick = onDismissNotice)
                                .padding(start = 10.dp)
                        )
                    }
                }

                // 1. Rules Card
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(AppColors.CardSurface)
                        .border(1.dp, AppColors.BorderWarm, RoundedCornerShape(18.dp))
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "1. 学习判定与英语点读设置",
                        color = AppColors.TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold
                    )

                    // Mastery Streak Choice
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "知识点「已掌握」判定条件",
                                color = AppColors.TextSecondary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "作用于诊断引擎实时判定",
                                color = AppColors.TextSubtle,
                                fontSize = 12.sp
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            val streak2 = state.parentSettings.masteryStreak == 2
                            RuleOptionCard(
                                modifier = Modifier.weight(1f),
                                title = "连续答对 2 题（推荐）",
                                subtitle = "轻量紧凑，适合二年级日常复习",
                                selected = streak2,
                                selectedBg = AppColors.MathBadgeBg,
                                selectedBorder = AppColors.MathProgress,
                                selectedText = AppColors.MathTextDark,
                                onClick = { onUpdateMasteryStreak(2) }
                            )
                            RuleOptionCard(
                                modifier = Modifier.weight(1f),
                                title = "连续答对 3 题（严格）",
                                subtitle = "需连续稳固做对三次才通关",
                                selected = !streak2,
                                selectedBg = AppColors.MathBadgeBg,
                                selectedBorder = AppColors.MathProgress,
                                selectedText = AppColors.MathTextDark,
                                onClick = { onUpdateMasteryStreak(3) }
                            )
                        }
                    }

                    HorizontalDivider(thickness = 1.dp, color = Color(0xFFECE7DC))

                    // English Audio Source
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "英语语音方案（语文/数学已关闭语音）",
                                color = AppColors.TextSecondary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "仅英语展示点读按钮",
                                color = AppColors.TextSubtle,
                                fontSize = 12.sp
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            EnglishAudioMode.entries.forEach { mode ->
                                val active = state.parentSettings.englishAudioMode == mode
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (active) AppColors.EnBadgeBg else AppColors.PaperBg)
                                        .border(
                                            1.dp,
                                            if (active) AppColors.EnProgress else AppColors.BorderWarm,
                                            RoundedCornerShape(12.dp)
                                        )
                                        .clickable { onUpdateEnglishAudioMode(mode) }
                                        .padding(12.dp)
                                ) {
                                    Text(
                                        text = mode.title,
                                        color = if (active) AppColors.EnTextDark else AppColors.TextSecondary,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    HorizontalDivider(thickness = 1.dp, color = Color(0xFFECE7DC))

                    // Speech Rate
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "英语点读语速",
                            color = AppColors.TextSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Row(
                            modifier = Modifier
                                .height(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(AppColors.PillMutedBg)
                                .padding(3.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            listOf(0.85f to "0.85x", 0.92f to "0.92x 标准", 1.0f to "1.0x").forEach { (rate, label) ->
                                val active = kotlin.math.abs(state.parentSettings.speechRate - rate) < 0.02f
                                Box(
                                    modifier = Modifier
                                        .height(30.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (active) AppColors.CardSurface else Color.Transparent)
                                        .clickable { onUpdateSpeechRate(rate) }
                                        .padding(horizontal = 12.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = label,
                                        color = if (active) AppColors.TextPrimary else AppColors.TextMuted,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }

                // 2. Progress Reset Card
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(AppColors.CardSurface)
                        .border(1.dp, AppColors.BorderWarm, RoundedCornerShape(18.dp))
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "2. 学习数据重置管理",
                        color = AppColors.TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(AppColors.WeakSurface)
                            .border(1.dp, AppColors.WeakBorder, RoundedCornerShape(14.dp))
                            .clickable {
                                resetPassword = ""
                                onClearResetError()
                                showResetConfirmDialog = true
                            }
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "重置所有学习数据",
                                color = AppColors.WeakTextDark,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                text = "清空数学、语文、英语三科全部掌握进度、错题盲区、学习日历足迹，并恢复默认档案与设置（仅限家长操作，需二次确认）",
                                color = AppColors.WeakTextDark.copy(alpha = 0.85f),
                                fontSize = 12.sp,
                                lineHeight = 18.sp
                            )
                        }
                        Box(
                            modifier = Modifier
                                .padding(start = 12.dp)
                                .height(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(AppColors.WeakPrimary)
                                .padding(horizontal = 14.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "全部重置",
                                color = AppColors.TextOnAccent,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }

    if (showResetConfirmDialog) {
        Dialog(onDismissRequest = {
            if (!state.resetInProgress) {
                showResetConfirmDialog = false
                onClearResetError()
            }
        }) {
            Column(
                modifier = Modifier
                    .width(440.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(AppColors.CardSurface)
                    .border(1.dp, AppColors.BorderWarm, RoundedCornerShape(22.dp))
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(AppColors.WeakSurface)
                            .border(1.dp, AppColors.WeakBorder, RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = AppIcons.InfoCircle,
                            contentDescription = null,
                            tint = AppColors.WeakTextDark,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Text(
                        text = "确认重置当前孩子云端与本地数据？",
                        color = AppColors.TextPrimary,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }

                Text(
                    text = "将彻底清空「${state.profile.name}」在云端和本地所有设备上的三科知识点进度、待消灭盲区、学习日历足迹以及规则设置，恢复空白状态。请输入家长账号「${state.username}」的登录密码确认：",
                    color = AppColors.TextSecondary,
                    fontSize = 13.sp,
                    lineHeight = 20.sp
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(AppColors.PaperBg)
                        .border(1.dp, AppColors.DividerStrong, RoundedCornerShape(12.dp))
                        .padding(horizontal = 14.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (resetPassword.isEmpty()) {
                        Text(
                            text = "请输入账号登录密码进行二次验证",
                            color = AppColors.TextSubtle,
                            fontSize = 13.sp
                        )
                    }
                    BasicTextField(
                        value = resetPassword,
                        onValueChange = {
                            resetPassword = it
                            onClearResetError()
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            autoCorrectEnabled = false,
                            imeAction = ImeAction.Done
                        ),
                        visualTransformation = PasswordVisualTransformation(),
                        textStyle = TextStyle(
                            color = AppColors.TextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        ),
                        cursorBrush = SolidColor(AppColors.WeakPrimary),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                if (state.resetError.isNotBlank()) {
                    Text(
                        text = state.resetError,
                        color = AppColors.WeakTextDark,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .height(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(AppColors.PillMutedBg)
                            .clickable(enabled = !state.resetInProgress) {
                                showResetConfirmDialog = false
                                onClearResetError()
                            }
                            .padding(horizontal = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "取消",
                            color = AppColors.TextSecondary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Box(
                        modifier = Modifier
                            .height(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (state.resetInProgress) AppColors.WeakPrimary.copy(alpha = 0.6f) else AppColors.WeakPrimary)
                            .clickable(enabled = !state.resetInProgress) {
                                onResetAllData(resetPassword) {
                                    showResetConfirmDialog = false
                                    resetPassword = ""
                                }
                            }
                            .padding(horizontal = 18.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (state.resetInProgress) "正在重置..." else "确认全部重置",
                            color = AppColors.TextOnAccent,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ParentLockedView(
    hasError: Boolean,
    onVerify: (String) -> Unit,
    onBypass: () -> Unit,
    onBackHome: () -> Unit
) {
    var answerInput by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize()) {
        UnifiedTopBar(
            leftContent = {
                Icon(
                    imageVector = AppIcons.Shield,
                    contentDescription = null,
                    tint = AppColors.MathPrimary,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "家长中心 · 安全验证",
                    color = AppColors.TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            },
            rightContent = {
                TopBarChip(
                    text = "管理孩子档案、判定规则与本地学习数据",
                    bg = AppColors.SelectedChipBg,
                    textColor = AppColors.TextMuted,
                    borderColor = AppColors.BorderWarm
                )
            }
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxSize()
                .background(AppColors.PaperBg)
                .padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .width(460.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(AppColors.CardSurface)
                    .border(1.dp, Color(0xFFE0DBD0), RoundedCornerShape(24.dp))
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(AppColors.MathBadgeBg)
                        .border(1.dp, Color(0xFFD2E1EE), RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = AppIcons.Lock,
                        contentDescription = null,
                        tint = AppColors.MathPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "请输入算术验证码进入家长中心",
                        color = AppColors.TextPrimary,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = "防止孩子误修改学习规则或误清空本地练习记录",
                        color = AppColors.TextMuted,
                        fontSize = 12.sp
                    )
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(AppColors.SubPanelBg)
                        .border(1.dp, AppColors.BorderWarm, RoundedCornerShape(16.dp))
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "28 + 15 = ？",
                        color = AppColors.MathTextDark,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(AppColors.CardSurface)
                                .border(1.dp, Color(0xFFD6D0C0), RoundedCornerShape(12.dp))
                                .padding(horizontal = 14.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (answerInput.isEmpty()) {
                                Text(
                                    text = "请输入计算结果",
                                    color = AppColors.TextSubtle,
                                    fontSize = 14.sp
                                )
                            }
                            BasicTextField(
                                value = answerInput,
                                onValueChange = { if (it.length <= 4) answerInput = it },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Number,
                                    imeAction = ImeAction.Done
                                ),
                                keyboardActions = KeyboardActions(onDone = { onVerify(answerInput) }),
                                textStyle = TextStyle(
                                    color = AppColors.TextPrimary,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    textAlign = TextAlign.Center
                                ),
                                cursorBrush = SolidColor(AppColors.MathPrimary),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        Box(
                            modifier = Modifier
                                .height(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(AppColors.MathPrimary)
                                .clickable { onVerify(answerInput) }
                                .padding(horizontal = 20.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "验证进入",
                                color = AppColors.TextOnAccent,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    if (hasError) {
                        Text(
                            text = "答案不正确，请重新计算",
                            color = Color(0xFF9B4C4C),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                HorizontalDivider(thickness = 1.dp, color = Color(0xFFECE7DC))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Start,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "← 返回首页",
                        color = AppColors.TextMuted,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.clickable(onClick = onBackHome)
                    )
                }
            }
        }
    }
}

@Composable
private fun CoverageRow(label: String, value: String, valueColor: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, color = AppColors.TextSecondary, fontSize = 12.sp)
        Text(text = value, color = valueColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun RuleOptionCard(
    modifier: Modifier,
    title: String,
    subtitle: String,
    selected: Boolean,
    selectedBg: Color,
    selectedBorder: Color,
    selectedText: Color,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) selectedBg else AppColors.PaperBg)
            .border(
                1.dp,
                if (selected) selectedBorder else AppColors.BorderWarm,
                RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = title,
            color = if (selected) selectedText else AppColors.TextSecondary,
            fontSize = 12.sp,
            fontWeight = FontWeight.ExtraBold
        )
        Text(
            text = subtitle,
            color = if (selected) selectedText.copy(alpha = 0.85f) else AppColors.TextMuted,
            fontSize = 12.sp
        )
    }
}
