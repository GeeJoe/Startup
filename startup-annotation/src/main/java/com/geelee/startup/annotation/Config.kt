package com.geelee.startup.annotation

import com.geelee.startup.annotation.model.ComponentInfo
import kotlin.reflect.KClass


/**
 * Created by zhiyueli on 2021/5/29.
 *
 * 用于标注一个 Initializer, Startup 库会自动将此 Initializer 注册到 [IInitializerRegistry] 中
 *
 * @param dependencies 依赖列表
 * @param threadMode 在指定线程下初始化, 默认主线程
 * @param supportProcess 支持在哪些进程初始化，如果不指定进程，默认只在主进程初始化, "*" 表示所有进程
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
annotation class Config(
    val dependencies: Array<KClass<*>> = [],
    val threadMode: ComponentInfo.ThreadMode = ComponentInfo.ThreadMode.MainThread,
    val supportProcess: Array<String> = []
)
