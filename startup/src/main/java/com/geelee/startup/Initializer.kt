package com.geelee.startup

import android.content.Context

interface Initializer {
    fun create(context: Context, processName: String)
}