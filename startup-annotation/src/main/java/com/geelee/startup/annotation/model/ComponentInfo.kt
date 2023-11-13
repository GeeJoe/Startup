package com.geelee.startup.annotation.model

import com.geelee.startup.annotation.IInitializerRegistry.Companion.ALL_PROCESS
import com.geelee.startup.annotation.model.ComponentInfo.ThreadMode.MainThread
import com.geelee.startup.annotation.model.ComponentInfo.ThreadMode.WorkThread

/**
 * @param name 初始化组件名字
 * @param supportProcess 支持在指定进程列表初始化
 * @param threadMode  在指定线程下初始化
 * @param dependencies 依赖列表
 * @param instanceProvider 获取实例的 lambda
 */
data class ComponentInfo(
    val name: String,
    val supportProcess: List<String> = emptyList(),
    val threadMode: ThreadMode = MainThread,
    val dependencies: List<String> = emptyList(),
    val instanceProvider: () -> Any
): Comparable<ComponentInfo> {
    val instance by lazy {
        instanceProvider()
    }

    val simpleName by lazy {
        name.split(".").last().replaceFirstChar { char -> char.lowercaseChar() }
    }

    override fun compareTo(other: ComponentInfo): Int {
        return name.compareTo(other.name)
    }

    override fun toString(): String {
        return "ComponentInfo(name='$name', supportProcess=$supportProcess, threadMode=$threadMode)"
    }

    fun toSimpleString(): String {
        return name.split(".").last()
    }

    fun toDetailString(): String {
        val processList = when {
            supportProcess.isEmpty() -> "[MAIN_PROCESS]"
            supportProcess.contains(ALL_PROCESS) -> "[ALL_PROCESS]"
            else -> supportProcess.toTypedArray().contentToString()
        }
        return "${toSimpleString()}$processList"
    }

    /**
     * 所有在 [MainThread] 模式下的初始化器串行执行初始化
     * 所有在 [WorkThread] 模式下的初始化器并行执行初始化
     */
    enum class ThreadMode {
        MainThread,
        WorkThread
    }
}