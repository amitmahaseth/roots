package com.rootsrecipes.utils

import android.content.Context
import android.view.View
import android.widget.Toast
import java.util.Calendar

var toast: Toast? = null
fun View.gone() {
    this.visibility = View.GONE
}

fun View.visible() {
    this.visibility = View.VISIBLE
}

fun View.inVisible() {
    this.visibility = View.INVISIBLE
}

fun Context.makeToast(msg: String) {
    toast?.cancel()
    toast = Toast.makeText(this, msg, Toast.LENGTH_SHORT)
    toast!!.show()
}
fun Long.isSameDay(otherTime: Long): Boolean {
    val cal1 = Calendar.getInstance().apply {
        timeInMillis = this@isSameDay
    }
    val cal2 = Calendar.getInstance().apply {
        timeInMillis = otherTime
    }
    return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) && cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(
        Calendar.DAY_OF_YEAR
    )
}