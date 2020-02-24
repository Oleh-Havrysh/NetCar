package com.hakito.netcar.wifi

import android.annotation.SuppressLint
import android.content.Context
import android.net.wifi.WifiManager

class WifiHelper(appContext: Context) {

    @SuppressLint("WifiManagerPotentialLeak")
    private val wifiManager =
        appContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

    fun getWifiNetworks() = wifiManager.scanResults.map { it.SSID }
}