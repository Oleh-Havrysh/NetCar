package com.hakito.netcar.voice.indication

import android.content.Context
import android.speech.tts.TextToSpeech

class VoiceIndicator(private val context: Context) {

    private var textToSpeech: TextToSpeech? = null

    private var inited = false

    fun initialize() {
        textToSpeech = TextToSpeech(context) {
            inited = it == TextToSpeech.SUCCESS
        }
    }

    fun shutdown() {
        textToSpeech?.shutdown()
    }

    fun batteryLow() {
        say("Battery low")
    }

    private fun say(text: String) {
        if (inited) {
            textToSpeech?.speak(text, TextToSpeech.QUEUE_ADD, hashMapOf<String, String>())
        }
    }
}