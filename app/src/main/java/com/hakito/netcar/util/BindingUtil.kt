package com.hakito.netcar.util

import android.widget.CheckBox
import android.widget.EditText
import androidx.core.widget.addTextChangedListener
import com.hakito.netcar.widget.LimitedSeekBar
import kotlin.reflect.KMutableProperty

fun CheckBox.bindToBoolean(property: KMutableProperty<Boolean>) {
    isChecked = property.getter.call()
    setOnCheckedChangeListener { _, isChecked -> property.setter.call(isChecked) }
}

fun LimitedSeekBar.bindToFloat(property: KMutableProperty<Float>) {
    percentProgress = property.getter.call()
    onProgressChangedListener = { property.setter.call(it) }
}

fun EditText.bindToInt(property: KMutableProperty<Int>) {
    setText(property.getter.call().toString())
    addTextChangedListener {
        property.setter.call(text.toString().toIntOrNull() ?: 0)
    }
}

fun EditText.bindToFloat(property: KMutableProperty<Float>) {
    setText(property.getter.call().toString())
    addTextChangedListener {
        property.setter.call(text.toString().toFloatOrNull() ?: 0f)
    }
}