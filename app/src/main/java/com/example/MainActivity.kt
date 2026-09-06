package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.engine.JarvisCommandEngine
import com.example.engine.JarvisCommandResult
import com.example.engine.JarvisSpeaker
import com.example.engine.JarvisVoiceRecognizer
import com.example.ui.JarvisDashboardScreen
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

    private lateinit var speaker: JarvisSpeaker
    private lateinit var commandEngine: JarvisCommandEngine
    private var voiceRecognizer: JarvisVoiceRecognizer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        speaker = JarvisSpeaker(this)
        commandEngine = JarvisCommandEngine(this)

        initVoiceRecognizer()

        speaker.speak("Jarvis online and ready, sir. Mobile automation protocols active.")

        setContent {
            MyApplicationTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    var currentRecognizer by remember { mutableStateOf(voiceRecognizer) }

                    JarvisDashboardScreen(
                        speaker = speaker,
                        commandEngine = commandEngine,
                        voiceRecognizer = currentRecognizer,
                        onRestartVoice = {
                            initVoiceRecognizer()
                            currentRecognizer = voiceRecognizer
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }

    private fun initVoiceRecognizer() {
        voiceRecognizer?.destroy()
        voiceRecognizer = JarvisVoiceRecognizer(this) { spokenCommand ->
            val result = commandEngine.processCommand(spokenCommand)
            when (result) {
                is JarvisCommandResult.Success -> {
                    speaker.speak(result.speechResponse)
                }
                is JarvisCommandResult.Error -> {
                    speaker.speak(result.speechResponse)
                }
            }
        }
        voiceRecognizer?.startListening()
    }

    override fun onResume() {
        super.onResume()
        voiceRecognizer?.startListening()
    }

    override fun onDestroy() {
        super.onDestroy()
        voiceRecognizer?.destroy()
        speaker.shutdown()
    }
}

