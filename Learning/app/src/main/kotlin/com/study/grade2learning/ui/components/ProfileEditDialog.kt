package com.study.grade2learning.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.study.grade2learning.model.ChildSummary
import com.study.grade2learning.model.TermFilter
import com.study.grade2learning.model.UserProfile
import com.study.grade2learning.ui.theme.AppColors

@Composable
fun ProfileEditDialog(
    profile: UserProfile,
    children: List<ChildSummary>,
    activeChildId: String,
    onDismiss: () -> Unit,
    onSwitchChild: (String) -> Unit,
    onCreateChild: (name: String, grade: String, avatarTone: String, defaultTerm: TermFilter) -> Unit,
    onSave: (name: String, grade: String, avatarTone: String, defaultTerm: TermFilter) -> Unit
) {
    var createMode by remember { mutableStateOf(false) }
    var name by remember(profile, createMode) { mutableStateOf(if (createMode) "" else profile.name) }
    var grade by remember(profile, createMode) { mutableStateOf(if (createMode) "二年级" else profile.grade) }
    var toneKey by remember(profile, createMode) { mutableStateOf(if (createMode) "sage" else profile.avatarTone) }
    var defaultTerm by remember(profile, createMode) { mutableStateOf(if (createMode) TermFilter.UPPER else profile.defaultTerm) }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .widthIn(min = 440.dp, max = 500.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(AppColors.HeaderBg)
                .border(1.dp, AppColors.DividerStrong, RoundedCornerShape(24.dp))
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (createMode) "新增学习者档案" else "切换 / 修改学习者信息",
                    color = AppColors.TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(AppColors.RailBg)
                        .clickable(onClick = onDismiss),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "×",
                        color = AppColors.TextSecondary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Child Switcher Strip
            if (children.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "家庭账号下的孩子（点击切换）",
                        color = AppColors.TextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        children.forEach { child ->
                            val isSelected = !createMode && child.id == activeChildId
                            Box(
                                modifier = Modifier
                                    .height(34.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isSelected) AppColors.MathCardBg else AppColors.CardSurface)
                                    .border(
                                        width = 1.dp,
                                        color = if (isSelected) AppColors.MathPrimary else AppColors.DividerStrong,
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                    .clickable {
                                        if (createMode) {
                                            createMode = false
                                        }
                                        if (child.id != activeChildId) {
                                            onSwitchChild(child.id)
                                        }
                                    }
                                    .padding(horizontal = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${child.name} · ${child.grade}",
                                    color = if (isSelected) AppColors.MathTextDark else AppColors.TextSecondary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .height(34.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (createMode) AppColors.EnCardBg else AppColors.EnBadgeBg)
                                .border(
                                    width = 1.dp,
                                    color = if (createMode) AppColors.EnPrimary else AppColors.EnBorder,
                                    shape = RoundedCornerShape(10.dp)
                                )
                                .clickable { createMode = !createMode }
                                .padding(horizontal = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (createMode) "返回当前档案" else "+ 新增孩子",
                                color = AppColors.EnTextDark,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Name Field
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "孩子昵称",
                    color = AppColors.TextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(AppColors.CardSurface)
                        .border(1.dp, AppColors.DividerStrong, RoundedCornerShape(12.dp))
                        .padding(horizontal = 14.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    BasicTextField(
                        value = name,
                        onValueChange = { if (it.length <= 8) name = it },
                        singleLine = true,
                        textStyle = TextStyle(
                            color = AppColors.TextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        cursorBrush = SolidColor(AppColors.MathPrimary),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // Grade Selector
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "当前年级",
                    color = AppColors.TextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("一年级", "二年级", "三年级").forEach { item ->
                        val active = item == grade
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(36.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (active) AppColors.MathPrimary else AppColors.CardSurface)
                                .border(
                                    1.dp,
                                    if (active) AppColors.MathPrimary else AppColors.DividerStrong,
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable { grade = item },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = item,
                                color = if (active) AppColors.TextOnAccent else AppColors.TextSecondary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Avatar Tone Selector
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "头像徽章配色",
                    color = AppColors.TextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AVATAR_TONES.forEach { tone ->
                        val active = tone.key == toneKey
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(36.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(tone.bg)
                                .border(
                                    width = if (active) 2.dp else 1.dp,
                                    color = if (active) AppColors.MathPrimary else tone.border,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clickable { toneKey = tone.key },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = tone.label,
                                color = tone.text,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Default Term Selector
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "默认展示册别",
                    color = AppColors.TextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TermFilter.entries.forEach { t ->
                        val active = t == defaultTerm
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(36.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (active) AppColors.MathPrimary else AppColors.CardSurface)
                                .border(
                                    1.dp,
                                    if (active) AppColors.MathPrimary else AppColors.DividerStrong,
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable { defaultTerm = t },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = t.label,
                                color = if (active) AppColors.TextOnAccent else AppColors.TextSecondary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .height(38.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(AppColors.RailBg)
                        .clickable(onClick = onDismiss)
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "取消",
                        color = AppColors.TextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Box(modifier = Modifier.size(10.dp))
                Box(
                    modifier = Modifier
                        .height(38.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (createMode) AppColors.EnPrimary else AppColors.MathPrimary)
                        .clickable {
                            if (createMode) {
                                onCreateChild(name, grade, toneKey, defaultTerm)
                            } else {
                                onSave(name, grade, toneKey, defaultTerm)
                            }
                        }
                        .padding(horizontal = 20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (createMode) "确认新增" else "保存修改",
                        color = AppColors.TextOnAccent,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
