package cy.ac.ucy.cs.anyplace.smas.ui.chat.theme.utils

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.util.*

class VoiceRecognition {

    private var recognizer: SpeechRecognizer? = null
    private var recognizer_intent: Intent? = null

    private var result : String? by mutableStateOf(null)

    fun getVoiceRecognitionResult() : String?{
        return result
    }

    fun clearResults() {
        result = null
    }

    private fun startListening(){
        recognizer?.startListening(recognizer_intent)
    }

    fun stopListening(){
        recognizer?.stopListening()
        recognizer?.destroy()
    }

    fun startVoiceRecognition(ctx: Context){

        recognizer = SpeechRecognizer.createSpeechRecognizer(ctx)
        recognizer_intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)

        recognizer_intent!!.putExtra(
            RecognizerIntent.EXTRA_LANGUAGE_MODEL,
            RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
        )

        recognizer_intent!!.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.ENGLISH)

        startListening()

        recognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onResults(results: Bundle) {
                val data = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                result = data!![0]
            }

            override fun onReadyForSpeech(params: Bundle) {}
            override fun onBeginningOfSpeech() {
                Toast.makeText(ctx, "listening...", Toast.LENGTH_SHORT).show()
            }

            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray) {}
            override fun onEndOfSpeech() {}
            override fun onError(error: Int) {}
            override fun onPartialResults(partialResults: Bundle) {}
            override fun onEvent(eventType: Int, params: Bundle) {}
        })
    }
}