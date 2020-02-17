package com.hakito.netcar.base

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.LayoutRes
import androidx.fragment.app.Fragment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancelChildren

abstract class BaseFragment(@LayoutRes val layoutRes: Int) : Fragment(), CoroutineScope {

    override val coroutineContext = Dispatchers.Main

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(layoutRes, container, false)

    override fun onDestroy() {
        coroutineContext.cancelChildren()
        super.onDestroy()
    }
}