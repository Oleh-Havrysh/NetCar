package com.hakito.netcar.util

import android.app.ProgressDialog
import android.content.Context
import androidx.fragment.app.Fragment

suspend fun Context.withProgress(block: suspend () -> Unit) {
    val dialog = ProgressDialog(this).apply {
        setCancelable(false)
        show()
    }
    try {
        block()
    } finally {
        dialog.cancel()
    }
}

suspend fun Fragment.withProgress(block: suspend () -> Unit) = context!!.withProgress(block)