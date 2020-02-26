package com.hakito.netcar.util

class ErrorsController {

    private val errorsMap = mutableMapOf<String, Int>()

    fun onError(throwable: Throwable) {
        throwable.printStackTrace()
        val errorName = throwable.message!!
        val count = errorsMap[errorName] ?: 0
        errorsMap[errorName] = count + 1
    }

    fun getText() = errorsMap.toString()
}