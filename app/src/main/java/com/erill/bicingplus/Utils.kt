@file:Suppress("unused")

package com.erill.bicingplus

import android.app.Activity
import android.content.Context
import android.support.annotation.IdRes
import android.support.annotation.LayoutRes
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast

fun Context.toast(message: String, length: Int = Toast.LENGTH_SHORT) {
    Toast.makeText(this, message, length).show()
}

fun ViewGroup.inflate(@LayoutRes layoutRes: Int, attachToRoot: Boolean = false): View {
    return LayoutInflater.from(context).inflate(layoutRes, this, attachToRoot)
}

fun Context.convertToPixels(dp: Int): Float {
    val conversionScale = resources.displayMetrics.density
    return (dp * conversionScale + 0.5f)
}

inline fun <reified T : View> View.find(@IdRes idRes: Int): T = findViewById(idRes) as T

fun View.show() = run { visibility = View.VISIBLE }
fun View.hide() = run { visibility = View.GONE }

val Activity.app: App
    get() = application as App