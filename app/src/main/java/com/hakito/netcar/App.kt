package com.hakito.netcar

import android.app.Application
import com.hakito.netcar.di.Modules
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class App : Application() {

    override fun onCreate() {
        super.onCreate()
        initKoin()
    }

    private fun initKoin() {
        startKoin {
            androidContext(this@App)
            modules(Modules.appModule)
        }
    }
}