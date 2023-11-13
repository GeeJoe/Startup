package com.geelee.startup.ktx

import android.app.ActivityManager
import android.content.Context
import android.os.Process


fun Context.processName() = getProcessName(this, Process.myPid()) ?: ""
internal fun Context.mainProcessName() = this.packageName

private fun getProcessName(cxt: Context, pid: Int): String? {
    val am = cxt.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    val runningApps = am.runningAppProcesses ?: return null
    for (processInfo in runningApps) {
        if (processInfo.pid == pid) {
            return processInfo.processName
        }
    }
    return null
}
