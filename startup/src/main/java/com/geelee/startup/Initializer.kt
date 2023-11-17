package com.geelee.startup

import android.content.Context

interface Initializer {
    fun init(context: Context, processName: String)
}