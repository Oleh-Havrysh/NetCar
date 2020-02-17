package com.hakito.netcar.util

import com.hakito.netcar.base.BaseFragment
import kotlinx.coroutines.launch

fun BaseFragment.launchWithProgressAndErrorHandling(block: suspend () -> Unit) {
    launch { withProgress { withErrorHandler(block) } }
}