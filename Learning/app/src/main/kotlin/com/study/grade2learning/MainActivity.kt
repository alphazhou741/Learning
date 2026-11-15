package com.study.grade2learning

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.study.grade2learning.model.SubjectKey
import com.study.grade2learning.ui.AppScreen
import com.study.grade2learning.ui.MainViewModel
import com.study.grade2learning.ui.components.ProfileEditDialog
import com.study.grade2learning.ui.components.TabletNavRail
import com.study.grade2learning.ui.screens.HomeScreen
import com.study.grade2learning.ui.screens.LoginScreen
import com.study.grade2learning.ui.screens.ParentScreen
import com.study.grade2learning.ui.screens.QuizScreen
import com.study.grade2learning.ui.screens.ReportScreen
import com.study.grade2learning.ui.screens.UnitsScreen
import com.study.grade2learning.ui.theme.AppColors
import com.study.grade2learning.ui.theme.Theme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller.hide(WindowInsetsCompat.Type.systemBars())

        setContent {
            Theme {
                LearningTabletApp()
            }
        }
    }
}

@Composable
fun LearningTabletApp(viewModel: MainViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.PaperBg)
    ) {
        if (state.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "正在加载语数英知识图谱...",
                    color = AppColors.TextSecondary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        } else if (!state.isLoggedIn) {
            LoginScreen(
                initialRegisterMode = state.preferRegisterTab,
                isLoading = state.authLoading,
                errorMessage = state.authError,
                recoveryLoading = state.recoveryResetLoading,
                recoveryError = state.recoveryResetError,
                recoverySuccessCode = state.recoveryResetSuccessCode,
                onLogin = viewModel::login,
                onRegister = viewModel::register,
                onResetByRecovery = viewModel::resetPasswordByRecovery,
                onClearRecoveryState = viewModel::clearRecoveryResetState,
                onClearError = viewModel::clearAuthError
            )
        } else {
            Row(modifier = Modifier.fillMaxSize()) {
                TabletNavRail(
                    profile = state.profile,
                    currentScreen = state.screen,
                    onSelectScreen = { screen -> viewModel.navigateTo(screen) },
                    onAvatarClick = { viewModel.openProfileDialog() }
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize()
                        .background(AppColors.PaperBg)
                ) {
                    when (state.screen) {
                        AppScreen.HOME -> HomeScreen(
                            state = state,
                            onSelectTerm = viewModel::setTerm,
                            onSelectDate = viewModel::selectDate,
                            onSyncNow = viewModel::syncNow,
                            onOpenUnits = { subj -> viewModel.navigateTo(AppScreen.UNITS, subj) },
                            onStartAdaptiveQuiz = viewModel::startAdaptiveQuiz,
                            onStartBlindspotTopic = viewModel::startTopicPractice
                        )

                        AppScreen.UNITS -> {
                            val units = viewModel.unitsFor(state.subject, state.term)
                            UnitsScreen(
                                state = state,
                                units = units,
                                onSelectSubject = { subj -> viewModel.navigateTo(AppScreen.UNITS, subj) },
                                onSelectTerm = viewModel::setTerm,
                                onSelectUnit = viewModel::selectUnit,
                                onStartUnitQuiz = viewModel::startUnitQuiz,
                                onPracticeTopic = viewModel::startTopicPractice
                            )
                        }

                        AppScreen.QUIZ -> {
                            val currentTopic = state.sessionTopics.getOrNull(state.sessionTopicIdx)
                                ?: state.currentQuestion?.let { q ->
                                    viewModel.engineFor(q.subject)?.topicById?.get(q.topicId)
                                }
                            QuizScreen(
                                state = state,
                                currentTopic = currentTopic,
                                onExitQuiz = { viewModel.navigateTo(AppScreen.UNITS) },
                                onSelectChoice = viewModel::selectChoiceAndSubmit,
                                onPressKeypad = viewModel::pressKeypadKey,
                                onSkip = viewModel::skipCurrentQuestion,
                                onNextStep = viewModel::nextQuizStep,
                                onOpenReport = { viewModel.navigateTo(AppScreen.REPORT) },
                                onPlayEnglishPrompt = viewModel::playEnglishPromptAudio,
                                onPlayEnglishAnswer = viewModel::playEnglishAnswerAudio
                            )
                        }

                        AppScreen.REPORT -> {
                            val steps = viewModel.remediationStepsFor(state.subject)
                            ReportScreen(
                                state = state,
                                remediationSteps = steps,
                                onSelectSubject = { subj -> viewModel.navigateTo(AppScreen.REPORT, subj) },
                                onToggleViewMode = viewModel::setReportParentMode,
                                onPracticeTopic = viewModel::startTopicPractice,
                                onStartRootRemediation = {
                                    val first = steps.firstOrNull()
                                    if (first != null) {
                                        viewModel.startTopicPractice(first.topic.subject, first.topic.id)
                                    } else {
                                        viewModel.startAdaptiveQuiz(state.subject)
                                    }
                                }
                            )
                        }

                        AppScreen.PARENT -> ParentScreen(
                            state = state,
                            onUnlock = viewModel::verifyParentGate,
                            onLock = viewModel::lockParentCenter,
                            onBackHome = { viewModel.navigateTo(AppScreen.HOME) },
                            onEditProfile = viewModel::openProfileDialog,
                            onOpenReport = { viewModel.navigateTo(AppScreen.REPORT, SubjectKey.MATH) },
                            onUpdateMasteryStreak = viewModel::updateMasteryStreak,
                            onUpdateEnglishAudioMode = viewModel::updateEnglishAudioMode,
                            onUpdateSpeechRate = viewModel::updateSpeechRate,
                            onExportBackup = viewModel::exportBackup,
                            onChangePassword = viewModel::changeAccountPassword,
                            onRotateRecoveryCode = viewModel::rotateRecoveryCode,
                            onClearSecurityError = viewModel::clearSecurityOpError,
                            onResetAllData = viewModel::resetAllData,
                            onClearResetError = viewModel::clearResetError,
                            onSyncNow = viewModel::syncNow,
                            onLogout = viewModel::logout,
                            onDismissNotice = viewModel::clearParentNotice
                        )
                    }
                }
            }
        }

        // Audio Toast Overlay
        state.audioToastMessage?.let { msg ->
            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 74.dp, end = 24.dp)
                    .clip(RoundedCornerShape(99.dp))
                    .background(AppColors.TextPrimary.copy(alpha = 0.92f))
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF8FC0A9))
                )
                Text(
                    text = msg,
                    color = AppColors.TextOnAccent,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        if (state.showProfileDialog) {
            ProfileEditDialog(
                profile = state.profile,
                children = state.children,
                activeChildId = state.activeChildId,
                onDismiss = viewModel::closeProfileDialog,
                onSwitchChild = viewModel::switchChild,
                onCreateChild = viewModel::createChild,
                onSave = viewModel::saveProfile
            )
        }

        state.pendingRecoveryCodeDialog?.let { recoveryCode ->
            Dialog(onDismissRequest = viewModel::dismissRecoveryCodeDialog) {
                Column(
                    modifier = Modifier
                        .width(460.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(AppColors.CardSurface)
                        .border(1.dp, AppColors.BorderWarm, RoundedCornerShape(24.dp))
                        .padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "请抄写保存账号恢复码",
                        color = AppColors.TextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = "忘记密码时可凭「用户名 + 此恢复码」在平板或网页端自助重置密码；此恢复码仅本次明文展示，请妥善保管。",
                        color = AppColors.TextSecondary,
                        fontSize = 13.sp,
                        lineHeight = 20.sp
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(AppColors.ZhBadgeBg)
                            .border(1.dp, AppColors.ZhBorder, RoundedCornerShape(14.dp))
                            .padding(vertical = 16.dp, horizontal = 18.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = recoveryCode,
                            color = AppColors.TextPrimary,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(AppColors.MathPrimary)
                            .clickable(onClick = viewModel::dismissRecoveryCodeDialog),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "我已抄写保存，进入使用",
                            color = AppColors.TextOnAccent,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }
        }
    }
}
