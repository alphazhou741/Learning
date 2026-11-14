package com.study.grade2learning.data

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.PlaybackParams
import android.speech.tts.TextToSpeech
import com.study.grade2learning.model.EnglishAudioMode
import com.study.grade2learning.model.ParentSettings
import com.study.grade2learning.model.QuestionItem
import java.util.Locale

class EnglishAudioPlayer(context: Context) {

    private val appContext = context.applicationContext
    private var mediaPlayer: MediaPlayer? = null
    private var tts: TextToSpeech? = null
    private var ttsReady = false
    private var pendingTtsText: String? = null
    private var pendingRate: Float = 0.92f

    private fun ensureTts() {
        if (tts != null) return
        tts = TextToSpeech(appContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val res = tts?.setLanguage(Locale.US) ?: TextToSpeech.LANG_MISSING_DATA
                ttsReady = res != TextToSpeech.LANG_MISSING_DATA && res != TextToSpeech.LANG_NOT_SUPPORTED
                pendingTtsText?.let { text ->
                    pendingTtsText = null
                    speakWithTts(text, pendingRate)
                }
            }
        }
    }

    fun playQuestionPrompt(question: QuestionItem, settings: ParentSettings, onStatus: (String) -> Unit) {
        val text = question.ttsPromptText.ifBlank { question.prompt }
        playAssetOrTts(question.audioFile, text, settings, onStatus)
    }

    fun playAnswerPronunciation(question: QuestionItem, settings: ParentSettings, onStatus: (String) -> Unit) {
        val text = question.ttsAnswerText.ifBlank { question.ttsPromptText }
        playAssetOrTts(question.answerAudioFile, text, settings, onStatus)
    }

    private fun playAssetOrTts(
        assetPath: String,
        fallbackText: String,
        settings: ParentSettings,
        onStatus: (String) -> Unit
    ) {
        stopPlayback()
        if (settings.englishAudioMode == EnglishAudioMode.BUNDLED_WITH_TTS && assetPath.isNotBlank()) {
            try {
                val afd = appContext.assets.openFd(assetPath)
                val mp = MediaPlayer().apply {
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .build()
                    )
                    setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                    afd.close()
                    prepare()
                    val clampedSpeed = settings.speechRate.coerceIn(0.6f, 1.5f)
                    if (clampedSpeed != 1.0f) {
                        playbackParams = PlaybackParams().setSpeed(clampedSpeed)
                    }
                    setOnCompletionListener {
                        it.release()
                        if (mediaPlayer === it) mediaPlayer = null
                    }
                    start()
                }
                mediaPlayer = mp
                onStatus("英文预置音频朗读中（${settings.speechRate}x）...")
                return
            } catch (_: Throwable) {
                // Fall back to Android TextToSpeech cleanly
            }
        }
        onStatus("正在使用系统 Android TTS 朗读英文...")
        speakWithTts(fallbackText, settings.speechRate)
    }

    private fun speakWithTts(text: String, rate: Float) {
        ensureTts()
        if (!ttsReady) {
            pendingTtsText = text
            pendingRate = rate
            return
        }
        tts?.setSpeechRate(rate.coerceIn(0.6f, 1.5f))
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "en_${System.currentTimeMillis()}")
    }

    fun stopPlayback() {
        mediaPlayer?.let {
            try {
                if (it.isPlaying) it.stop()
            } catch (_: Throwable) {
            }
            it.release()
        }
        mediaPlayer = null
        tts?.stop()
    }

    fun release() {
        stopPlayback()
        tts?.shutdown()
        tts = null
        ttsReady = false
    }
}
