package com.hakito.netcar.di

import com.hakito.netcar.BatteryProcessor
import com.hakito.netcar.ControlPreferences
import com.hakito.netcar.StabilizationController
import com.hakito.netcar.cloud.CloudRepository
import com.hakito.netcar.sender.CarSender
import com.hakito.netcar.sender.CarSenderImpl
import com.hakito.netcar.util.ErrorsController
import com.hakito.netcar.util.ResponseTimeGraphController
import com.hakito.netcar.util.StatisticsController
import com.hakito.netcar.util.WheelRpmGraphController
import com.hakito.netcar.voice.indication.VoiceIndicator
import com.hakito.netcar.wifi.WifiHelper
import com.hakito.netcar.work.CarEnabledChecker
import org.koin.dsl.module

object Modules {

    val appModule = module {
        single { ControlPreferences(get()) }
        single { CarSenderImpl(get()) as CarSender }
        single { CarEnabledChecker(get()) }
        single { WifiHelper(get()) }
        single { VoiceIndicator(get()) }
        single { StabilizationController(get()) }
        single { CloudRepository() }
        single { BatteryProcessor(get()) }
        single { ErrorsController() }
        single { WheelRpmGraphController() }
        single { ResponseTimeGraphController() }
        single { StatisticsController(get()) }
    }
}