package com.hakito.netcar.sender

import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class CarSenderImplTest {

    private lateinit var sender: CarSenderImpl

    @Before
    fun setup() {
        sender = CarSenderImpl()
    }

    @Test
    fun buildUrl() {
        val params = CarParams(75, 150)

        val url = sender.buildUrl(params).toString()

        assertEquals("http://192.168.4.1:81/car?steer=75&throttle=150", url)
    }
}