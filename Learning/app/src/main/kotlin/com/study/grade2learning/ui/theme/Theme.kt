package com.study.grade2learning.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val WarmPaperColorScheme = lightColorScheme(
    primary = AppColors.MathPrimary,
    onPrimary = AppColors.TextOnAccent,
    primaryContainer = AppColors.MathBadgeBg,
    onPrimaryContainer = AppColors.MathTextDark,
    secondary = AppColors.ZhPrimary,
    onSecondary = AppColors.TextOnAccent,
    secondaryContainer = AppColors.ZhBadgeBg,
    onSecondaryContainer = AppColors.ZhTextDark,
    tertiary = AppColors.EnPrimary,
    onTertiary = AppColors.TextOnAccent,
    tertiaryContainer = AppColors.EnBadgeBg,
    onTertiaryContainer = AppColors.EnTextDark,
    background = AppColors.PaperBg,
    onBackground = AppColors.TextPrimary,
    surface = AppColors.CardSurface,
    onSurface = AppColors.TextPrimary,
    surfaceVariant = AppColors.SubPanelBg,
    onSurfaceVariant = AppColors.TextSecondary,
    outline = AppColors.BorderWarm,
    outlineVariant = AppColors.DividerWarm,
    error = AppColors.WeakPrimary,
    onError = AppColors.TextOnAccent
)

@Composable
fun Theme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = WarmPaperColorScheme,
        typography = Typography,
        content = content
    )
}
