package com.example.engine

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

class JarvisVoiceRecognizer(
    private val context: Context,
    private val onCommandReceived: (String) -> Unit
) {

    private var speechRecognizer: SpeechRecognizer? = null
    private val handler = Handler(Looper.getMainLooper())

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _audioRms = MutableStateFlow(0f)
    val audioRms: StateFlow<Float> = _audioRms.asStateFlow()

    private val _lastRecognizedText = MutableStateFlow("")
    val lastRecognizedText: StateFlow<String> = _lastRecognizedText.asStateFlow()

    private var isContinuousListening = true
    private var isDestroyed = false

    init {
        initRecognizer()
    }

    private fun initRecognizer() {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            return
        }

        handler.post {
            try {
                speechRecognizer?.destroy()
                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                    setRecognitionListener(createListener())
                }
            } catch (e: Exception) {
                // Ignore initialization failures gracefully
            }
        }
    }

    private fun createListener(): RecognitionListener {
        return object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                _isListening.value = true
            }

            override fun onBeginningOfSpeech() {
                _isListening.value = true
            }

            override fun onRmsChanged(rmsdB: Float) {
                // RMS range is typically -2 to 10 dB. Normalize roughly to 0f..1f
                val normalized = ((rmsdB + 2f) / 12f).coerceIn(0f, 1f)
                _audioRms.value = normalized
            }

            override fun onBufferReceived(buffer: ByteArray?) {}

            override fun onEndOfSpeech() {
                _isListening.value = false
                _audioRms.value = 0f
            }

            override fun onError(error: Int) {
                _isListening.value = false
                _audioRms.value = 0f

                // In continuous mode, restart listening after brief delay if not permanently destroyed
                if (isContinuousListening && !isDestroyed) {
                    handler.postDelayed({
                        if (isContinuousListening && !isDestroyed) {
                            startListening()
                        }
                    }, 800)
                }
            }

            override fun onResults(results: Bundle?) {
                _isListening.value = false
                _audioRms.value = 0f
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val recognized = matches?.firstOrNull() ?: ""
                if (recognized.isNotBlank()) {
                    _lastRecognizedText.value = recognized
                    onCommandReceived(recognized)
                }

                if (isContinuousListening && !isDestroyed) {
                    handler.postDelayed({
                        if (isContinuousListening && !isDestroyed) {
                            startListening()
                        }
                    }, 500)
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                matches?.firstOrNull()?.let {
                    _lastRecognizedText.value = it
                }
            }

            override fun onEvent(eventType: Int, params: Bundle?) {}
        }
    }

    fun startListening() {
        if (isDestroyed) return
        handler.post {
            try {
                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
                }
                speechRecognizer?.startListening(intent)
                _isListening.value = true
            } catch (e: Exception) {
                _isListening.value = false
            }
        }
    }

    fun stopListening() {
        handler.post {
            try {
                speechRecognizer?.stopListening()
                _isListening.value = false
                _audioRms.value = 0f
            } catch (e: Exception) {
                // Ignore
            }
        }
    }

    fun setContinuous(continuous: Boolean) {
        isContinuousListening = continuous
        if (!continuous) {
            stopListening()
        } else {
            startListening()
        }
    }

    fun destroy() {
        isDestroyed = true
        isContinuousListening = false
        handler.post {
            try {
                speechRecognizer?.destroy()
                speechRecognizer = null
            } catch (e: Exception) {
                // Ignore
            }
        }
    }
}
