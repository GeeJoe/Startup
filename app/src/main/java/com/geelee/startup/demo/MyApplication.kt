package com.geelee.startup.demo

import android.app.Application
import android.content.Context
import android.util.Log
import com.geelee.startup.IStartupLogger
import com.geelee.startup.InitializerRegistry
import com.geelee.startup.Startup
import com.geelee.startup.ktx.processName

/**
 * Created by zhiyueli on 11/13/23 17:28.
 */
class MyApplication : Application() {

    private lateinit var appContext: Context
    private val startupLogger by lazy { StartupLogger() }

    override fun attachBaseContext(base: Context) {
        appContext = base
        super.attachBaseContext(base)
        Startup.build(this, InitializerRegistry, startupLogger).init()
    }

    inner class StartupLogger : IStartupLogger {
        private val tag = "StartupLogger(${appContext.processName()})"

        override fun i(msg: String) {
            Log.i(tag, msg)
        }

        override fun e(msg: String, throwable: Throwable) {
            Log.e(tag, msg, throwable)
        }

        override fun isDebugVersion(): Boolean {
            return true
        }
    }
}

