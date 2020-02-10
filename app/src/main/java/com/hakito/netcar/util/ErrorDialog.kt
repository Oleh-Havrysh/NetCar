package com.hakito.netcar.util

import android.content.Context
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment

suspend fun Context.withErrorHandler(block: suspend () -> Unit) {
    try {
        block()
    } catch (t: Throwable) {
        AlertDialog.Builder(this)
            .setTitle("Error")
            .setMessage(t.message)
            .setPositiveButton("OK", { _, _ -> })
            .show()
    }
}

suspend fun Fragment.withErrorHandler(block: suspend () -> Unit) = context!!.withErrorHandler(block)