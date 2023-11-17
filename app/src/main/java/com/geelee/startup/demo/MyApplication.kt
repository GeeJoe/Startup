package com.geelee.startup.demo

import android.app.Application
import android.content.Context
import com.geelee.startup.Startup

/**
 * Created by zhiyueli on 11/13/23 17:28.
 */
class MyApplication : Application() {

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(base)
        Startup.init(base)
    }
}

