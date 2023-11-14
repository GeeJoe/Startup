package com.geelee.startup.demo

import android.content.Context
import android.util.Log
import com.geelee.startup.Initializer

/**
 * Created by zhiyueli on 11/13/23 17:52.
 */
open class BaseLogInitializer : Initializer {
    override fun create(context: Context, processName: String) {
        Log.i(
            "Startup",
            "${this::class.simpleName} curThread=${Thread.currentThread()} curProcess=${processName}"
        )
    }
}