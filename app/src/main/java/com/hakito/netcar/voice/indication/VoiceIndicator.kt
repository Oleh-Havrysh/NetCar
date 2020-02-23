package com.hakito.netcar.voice.indication

import android.content.Context
import android.speech.tts.TextToSpeech

class VoiceIndicator(context: Context) {

    private val textToSpeech: TextToSpeech

    private var inited = false

    init {
        textToSpeech = TextToSpeech(context) {
            inited = it == TextToSpeech.SUCCESS
        }
    }

    fun shutdown() {
        textToSpeech.shutdown()
    }

    fun batteryLow() {
        say("Battery low")
    }

    private fun say(text: String) {
        if (inited) {
            textToSpeech.speak(text, TextToSpeech.QUEUE_ADD, hashMapOf<String, String>())
        }
    }
}