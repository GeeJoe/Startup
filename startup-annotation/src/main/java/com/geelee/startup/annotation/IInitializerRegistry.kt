package com.geelee.startup.annotation

import com.geelee.startup.annotation.model.ComponentInfo
import com.geelee.startup.annotation.model.DependencyChain


/**
 * Created by zhiyueli on 2021/5/29.
 */
interface IInitializerRegistry {
    companion object {
        const val GENERATED_CLASS_PACKAGE_NAME = "com.geelee.startup"
        const val GENERATED_CLASS_NAME = "InitializerRegistry"
        const val ALL_PROCESS = "*"
        const val MAIN_PROCESS = "MAIN_PROCESS"
    }

    /**
     * 获取所有注册的初始化器信息
     */
    fun getAllInitializer(): Map<String, ComponentInfo>

    /**
     * 获取指定名称的初始化器信息
     */
    fun getInitializerByName(name: String): ComponentInfo? {
        return getAllInitializer()[name]
    }

    fun getComponentChainList(): List<DependencyChain>
}