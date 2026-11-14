package com.study.grade2learning.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.study.grade2learning.model.TermFilter
import com.study.grade2learning.model.UserProfile
import com.study.grade2learning.ui.AppScreen
import com.study.grade2learning.ui.icons.AppIcons
import com.study.grade2learning.ui.theme.AppColors

data class AvatarToneStyle(
    val key: String,
    val label: String,
    val bg: Color,
    val border: Color,
    val text: Color
)

val AVATAR_TONES = listOf(
    AvatarToneStyle("amber", "暖杏", AppColors.ZhBadgeBg, AppColors.ShakyBorder, AppColors.ZhTextDark),
    AvatarToneStyle("blue", "湖蓝", AppColors.MathBadgeBg, AppColors.MathBorder, AppColors.MathTextDark),
    AvatarToneStyle("green", "竹绿", AppColors.EnBadgeBg, AppColors.EnBorder, AppColors.EnTextDark),
    AvatarToneStyle("rose", "浅梅", AppColors.WeakSurface, AppColors.WeakBorder, AppColors.WeakTextDark)
)

fun avatarToneFor(key: String): AvatarToneStyle =
    AVATAR_TONES.firstOrNull { it.key == key } ?: AVATAR_TONES.first()

@Composable
fun TabletNavRail(
    profile: UserProfile,
    currentScreen: AppScreen,
    onSelectScreen: (AppScreen) -> Unit,
    onAvatarClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tone = avatarToneFor(profile.avatarTone)
    val shortName = if (profile.name.length > 2) profile.name.takeLast(2) else profile.name

    Row(modifier = modifier.fillMaxHeight()) {
        Column(
            modifier = Modifier
                .width(88.dp)
                .fillMaxHeight()
                .background(AppColors.RailBg)
                .padding(vertical = 16.dp, horizontal = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // Clickable Learner Avatar Badge
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .clickable(onClick = onAvatarClick)
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Box(contentAlignment = Alignment.BottomEnd) {
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(tone.bg)
                                .border(1.dp, tone.border, RoundedCornerShape(16.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = shortName,
                                color = tone.text,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.ExtraBold,
                                maxLines = 1
                            )
                        }
                        Box(
                            modifier = Modifier
                                .offset(x = 3.dp, y = 3.dp)
                                .size(18.dp)
                                .clip(CircleShape)
                                .background(AppColors.HeaderBg)
                                .border(1.dp, AppColors.DividerStrong, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = AppIcons.Pen,
                                contentDescription = "编辑资料",
                                tint = AppColors.TextMuted,
                                modifier = Modifier.size(10.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = profile.grade,
                        color = AppColors.TextMuted,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1
                    )
                }

                HorizontalDivider(
                    modifier = Modifier.width(36.dp),
                    thickness = 1.dp,
                    color = Color(0xFFDFD9CC)
                )

                NavRailItem(
                    label = "首页",
                    icon = AppIcons.Home,
                    selected = currentScreen == AppScreen.HOME,
                    onClick = { onSelectScreen(AppScreen.HOME) }
                )
                NavRailItem(
                    label = "单元",
                    icon = AppIcons.Units,
                    selected = currentScreen == AppScreen.UNITS,
                    onClick = { onSelectScreen(AppScreen.UNITS) }
                )
                NavRailItem(
                    label = "答题",
                    icon = AppIcons.Pen,
                    selected = currentScreen == AppScreen.QUIZ,
                    onClick = { onSelectScreen(AppScreen.QUIZ) }
                )
                NavRailItem(
                    label = "报告",
                    icon = AppIcons.Report,
                    selected = currentScreen == AppScreen.REPORT,
                    onClick = { onSelectScreen(AppScreen.REPORT) }
                )
            }

            // Bottom Independent Parent Center Entry
            val parentSelected = currentScreen == AppScreen.PARENT
            Column(
                modifier = Modifier
                    .width(64.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (parentSelected) AppColors.MathPrimary else AppColors.HeaderBg)
                    .border(
                        1.dp,
                        if (parentSelected) AppColors.MathPrimary else AppColors.DividerStrong,
                        RoundedCornerShape(16.dp)
                    )
                    .clickable { onSelectScreen(AppScreen.PARENT) }
                    .padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Icon(
                    imageVector = AppIcons.Lock,
                    contentDescription = "家长",
                    tint = if (parentSelected) AppColors.TextOnAccent else AppColors.TextSecondary,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "家长",
                    color = if (parentSelected) AppColors.TextOnAccent else AppColors.TextSecondary,
                    fontSize = 12.sp,
                    fontWeight = if (parentSelected) FontWeight.Bold else FontWeight.SemiBold
                )
            }
        }
        VerticalDivider(thickness = 1.dp, color = Color(0xFFE0DCD0))
    }
}

@Composable
private fun NavRailItem(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(if (selected) AppColors.MathPrimary else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (selected) AppColors.TextOnAccent else AppColors.TextSecondary,
            modifier = Modifier.size(20.dp)
        )
        Text(
            text = label,
            color = if (selected) AppColors.TextOnAccent else AppColors.TextSecondary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun UnifiedTopBar(
    leftContent: @Composable RowScope.() -> Unit,
    rightContent: @Composable RowScope.() -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .background(AppColors.HeaderBg)
                .padding(horizontal = 24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                content = leftContent
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                content = rightContent
            )
        }
        HorizontalDivider(thickness = 1.dp, color = AppColors.DividerWarm)
    }
}

@Composable
fun TermSegmentedToggle(
    currentTerm: TermFilter,
    onSelectTerm: (TermFilter) -> Unit
) {
    Row(
        modifier = Modifier
            .height(40.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(AppColors.PillSegmentTrack)
            .padding(4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        TermFilter.entries.forEach { term ->
            val selected = term == currentTerm
            Box(
                modifier = Modifier
                    .height(32.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (selected) AppColors.CardSurface else Color.Transparent)
                    .clickable { onSelectTerm(term) }
                    .padding(horizontal = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = term.pillLabel,
                    color = if (selected) AppColors.TextPrimary else AppColors.TextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
fun TopBarPillButton(
    text: String,
    bg: Color,
    textColor: Color,
    borderColor: Color? = null,
    leadingIcon: ImageVector? = null,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .height(36.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .then(
                if (borderColor != null) Modifier.border(1.dp, borderColor, RoundedCornerShape(12.dp))
                else Modifier
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        if (leadingIcon != null) {
            Icon(
                imageVector = leadingIcon,
                contentDescription = null,
                tint = textColor,
                modifier = Modifier.size(16.dp)
            )
        }
        Text(
            text = text,
            color = textColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun TopBarChip(
    text: String,
    bg: Color,
    textColor: Color,
    borderColor: Color,
    padding: PaddingValues = PaddingValues(horizontal = 14.dp)
) {
    Box(
        modifier = Modifier
            .height(36.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .border(1.dp, borderColor, RoundedCornerShape(12.dp))
            .padding(padding),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = textColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
