package com.geelee.startup.ktx

import android.content.Context
import com.geelee.startup.annotation.IInitializerRegistry.Companion.ALL_PROCESS
import com.geelee.startup.annotation.model.ComponentInfo

/**
 * 判断进程是否在支持列表中
 */
fun ComponentInfo.isSupportProcess(context: Context, process: String): Boolean {
    // 空代表主进程
    if (this.supportProcess.isEmpty()) {
        return context.mainProcessName() == process
    }
    // ALL_PROCESS 代表所有进程
    if (this.supportProcess.contains(ALL_PROCESS)) {
        return true
    }
    return supportProcess.contains(process)
}
