package com.hakito.netcar.di

import com.hakito.netcar.BatteryProcessor
import com.hakito.netcar.ControlPreferences
import com.hakito.netcar.StabilizationController
import com.hakito.netcar.sender.CarSender
import com.hakito.netcar.sender.CarSenderImpl
import com.hakito.netcar.util.ErrorsController
import com.hakito.netcar.util.ResponseTimeGraphController
import com.hakito.netcar.util.StatisticsController
import com.hakito.netcar.util.WheelRpmGraphController
import com.hakito.netcar.wifi.WifiHelper
import org.koin.dsl.module

object Modules {

    val appModule = module {
        single { ControlPreferences(get()) }
        single { CarSenderImpl() as CarSender }
        single { WifiHelper(get()) }
        single { StabilizationController(get()) }
        single { BatteryProcessor(get()) }
        single { ErrorsController() }
        single { WheelRpmGraphController() }
        single { ResponseTimeGraphController() }
        single { StatisticsController(get()) }
    }
}