package com.hakito.netcar

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.*
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

class MainActivity : AppCompatActivity() {

    private var sendingJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    override fun onResume() {
        super.onResume()
        startSending()
    }

    override fun onPause() {
        super.onPause()
        stopSending()
    }

    private fun startSending() {
        sendingJob = GlobalScope.launch(Dispatchers.IO) {

            while (true) {
                try {
                    sendValue(seekBar.progress)
                } catch (e: IOException) {
                    e.printStackTrace()
                }

                delay(100)
            }
        }
    }

    private fun sendValue(value: Int) {
        val connection = URL("http://192.168.4.1/car?car=${value}")
            .openConnection() as HttpURLConnection
        connection.apply {
            requestMethod = "GET"
            connectTimeout = 200
            readTimeout = 200
        }

        if (connection.responseCode == 200) {
            Log.d("SEND", "Success")
        } else {
            Log.e("SEND", "Failure")
        }
    }

    private fun stopSending() {
        sendingJob?.cancel()
    }
}
