package com.fzc.luckyspin

import android.os.Handler
import android.os.Looper

object HandlerUtils {

    private val sMainHandler: Handler = Handler(Looper.getMainLooper())

    fun getMainHandler(): Handler {
        return sMainHandler
    }

    fun runOnUiThread(runnable: () -> Unit) {
        if (isOnUiThread()) {
            runnable.invoke()
        } else {
            sMainHandler.post(runnable)
        }
    }

    fun isOnUiThread(): Boolean {
        return Thread.currentThread().id == Looper.getMainLooper().thread.id
    }
}