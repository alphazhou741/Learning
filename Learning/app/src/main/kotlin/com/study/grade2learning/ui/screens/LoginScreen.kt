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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.study.grade2learning.ui.icons.AppIcons
import com.study.grade2learning.ui.theme.AppColors

@Composable
fun LoginScreen(
    initialRegisterMode: Boolean,
    isLoading: Boolean,
    errorMessage: String,
    onLogin: (username: String, password: String) -> Unit,
    onRegister: (username: String, password: String, childName: String, childGrade: String) -> Unit,
    onClearError: () -> Unit
) {
    var isRegisterMode by remember(initialRegisterMode) { mutableStateOf(initialRegisterMode) }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var childName by remember { mutableStateOf("小宇") }
    var childGrade by remember { mutableStateOf("二年级") }
    val focusManager = LocalFocusManager.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.PaperBg)
            .imePadding()
            .padding(horizontal = 36.dp, vertical = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier
                .widthIn(max = 1040.dp)
                .fillMaxWidth()
                .fillMaxHeight(),
            horizontalArrangement = Arrangement.spacedBy(32.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left Hero / Brand Overview
            Column(
                modifier = Modifier
                    .weight(1.05f)
                    .padding(vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(AppColors.MathCardBg)
                            .border(1.dp, AppColors.MathBorder, RoundedCornerShape(18.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = AppIcons.Book,
                            contentDescription = null,
                            tint = AppColors.MathPrimary,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "基础学习",
                            color = AppColors.TextPrimary,
                            fontSize = 26.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = "二年级数学 · 语文 · 英语知识溯源平板端",
                            color = AppColors.TextMuted,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Text(
                    text = "请先登录家长或家庭账号，随时记录孩子每个知识点的掌握轨迹，跨设备实时同步学习日历与薄弱盲区。",
                    color = AppColors.TextSecondary,
                    fontSize = 15.sp,
                    lineHeight = 23.sp
                )

                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    FeatureRow(
                        icon = AppIcons.Route,
                        tint = AppColors.MathPrimary,
                        bg = AppColors.MathBadgeBg,
                        border = AppColors.MathBorder,
                        title = "知识图谱溯源",
                        subtitle = "覆盖二年级三科知识点，错题自动回溯一年级前置基础"
                    )
                    FeatureRow(
                        icon = AppIcons.Pulse,
                        tint = AppColors.EnPrimary,
                        bg = AppColors.EnBadgeBg,
                        border = AppColors.EnBorder,
                        title = "云端实时同步",
                        subtitle = "答题即刻上报云端保存，更换平板登录即刻恢复全部记录"
                    )
                    FeatureRow(
                        icon = AppIcons.Shield,
                        tint = AppColors.ZhPrimary,
                        bg = AppColors.ZhBadgeBg,
                        border = AppColors.ZhBorder,
                        title = "多孩档案与家长管控",
                        subtitle = "同一家庭账号可管理多位孩子档案，重置需家长密码确认"
                    )
                }
            }

            // Right Auth Form Card
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .clip(RoundedCornerShape(28.dp))
                    .background(AppColors.CardSurface)
                    .border(1.dp, AppColors.BorderWarm, RoundedCornerShape(28.dp))
                    .padding(26.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Mode Tabs
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(AppColors.PillSegmentTrack)
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    AuthTabButton(
                        text = "账号登录",
                        selected = !isRegisterMode,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            isRegisterMode = false
                            onClearError()
                        }
                    )
                    AuthTabButton(
                        text = "注册新账号",
                        selected = isRegisterMode,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            isRegisterMode = true
                            onClearError()
                        }
                    )
                }

                LabeledInputField(
                    label = "家庭账号用户名",
                    value = username,
                    placeholder = "至少 2 个字符（如 zhangjia）",
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Ascii,
                        autoCorrectEnabled = false,
                        imeAction = ImeAction.Next
                    ),
                    onValueChange = {
                        username = it
                        onClearError()
                    }
                )

                LabeledInputField(
                    label = "登录密码",
                    value = password,
                    placeholder = if (isRegisterMode) "至少 6 位密码" else "请输入登录密码",
                    isPassword = !showPassword,
                    trailingText = if (showPassword) "隐藏" else "显示",
                    onTrailingClick = { showPassword = !showPassword },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        autoCorrectEnabled = false,
                        imeAction = if (isRegisterMode) ImeAction.Next else ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            focusManager.clearFocus()
                            if (!isRegisterMode && !isLoading) {
                                onLogin(username, password)
                            }
                        }
                    ),
                    onValueChange = {
                        password = it
                        onClearError()
                    }
                )

                if (isRegisterMode) {
                    LabeledInputField(
                        label = "第一位孩子昵称",
                        value = childName,
                        placeholder = "例如：小宇",
                        onValueChange = { childName = it.take(12) }
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "当前所在年级",
                            color = AppColors.TextSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("一年级", "二年级", "三年级").forEach { g ->
                                val selected = childGrade == g
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(38.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (selected) AppColors.MathCardBg else AppColors.PaperBg)
                                        .border(
                                            1.dp,
                                            if (selected) AppColors.MathPrimary else AppColors.BorderWarm,
                                            RoundedCornerShape(12.dp)
                                        )
                                        .clickable { childGrade = g },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = g,
                                        color = if (selected) AppColors.MathTextDark else AppColors.TextSecondary,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }

                if (errorMessage.isNotBlank()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(AppColors.WeakSurface)
                            .border(1.dp, AppColors.WeakBorder, RoundedCornerShape(12.dp))
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = AppIcons.InfoCircle,
                            contentDescription = null,
                            tint = AppColors.WeakTextDark,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = errorMessage,
                            color = AppColors.WeakTextDark,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (isLoading) AppColors.MathPrimary.copy(alpha = 0.65f) else AppColors.MathPrimary)
                        .clickable(enabled = !isLoading) {
                            if (isRegisterMode) {
                                onRegister(username, password, childName, childGrade)
                            } else {
                                onLogin(username, password)
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (isLoading) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            CircularProgressIndicator(
                                color = AppColors.TextOnAccent,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = if (isRegisterMode) "正在创建账号并同步..." else "正在登录并恢复数据...",
                                color = AppColors.TextOnAccent,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    } else {
                        Text(
                            text = if (isRegisterMode) "注册并进入学习" else "登录并同步学习进度",
                            color = AppColors.TextOnAccent,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }

                Text(
                    text = "数据安全加密保存于 study.19930216.xyz 云端服务器",
                    color = AppColors.TextMuted,
                    fontSize = 11.sp
                )
            }
        }
    }
}

@Composable
private fun AuthTabButton(
    text: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .height(36.dp)
            .clip(RoundedCornerShape(11.dp))
            .background(if (selected) AppColors.CardSurface else Color.Transparent)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (selected) AppColors.TextPrimary else AppColors.TextMuted,
            fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.Bold
        )
    }
}

@Composable
private fun LabeledInputField(
    label: String,
    value: String,
    placeholder: String,
    isPassword: Boolean = false,
    trailingText: String? = null,
    onTrailingClick: (() -> Unit)? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    onValueChange: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = label,
            color = AppColors.TextSecondary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(AppColors.PaperBg)
                .border(1.dp, AppColors.BorderWarm, RoundedCornerShape(12.dp))
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.CenterStart
            ) {
                if (value.isEmpty()) {
                    Text(
                        text = placeholder,
                        color = AppColors.TextSubtle,
                        fontSize = 13.sp
                    )
                }
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    singleLine = true,
                    keyboardOptions = keyboardOptions,
                    keyboardActions = keyboardActions,
                    visualTransformation = if (isPassword) PasswordVisualTransformation() else VisualTransformation.None,
                    textStyle = TextStyle(
                        color = AppColors.TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    ),
                    cursorBrush = SolidColor(AppColors.MathPrimary),
                    modifier = Modifier.fillMaxWidth()
                )
            }
            if (trailingText != null && onTrailingClick != null) {
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = trailingText,
                    color = AppColors.MathPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable(onClick = onTrailingClick)
                )
            }
        }
    }
}

@Composable
private fun FeatureRow(
    icon: ImageVector,
    tint: Color,
    bg: Color,
    border: Color,
    title: String,
    subtitle: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(AppColors.HeaderBg)
            .border(1.dp, AppColors.BorderWarm, RoundedCornerShape(16.dp))
            .padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(11.dp))
                .background(bg)
                .border(1.dp, border, RoundedCornerShape(11.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(19.dp)
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = title,
                color = AppColors.TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = subtitle,
                color = AppColors.TextMuted,
                fontSize = 12.sp
            )
        }
    }
}
