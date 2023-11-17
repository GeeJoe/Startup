package com.geelee.startup

import android.content.Context
import android.util.Log
import com.geelee.startup.annotation.IInitializerRegistry
import com.geelee.startup.annotation.model.ComponentInfo
import com.geelee.startup.annotation.model.DependencyChain
import com.geelee.startup.ktx.isSupportProcess
import com.geelee.startup.ktx.processName
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 扩展自官方的 [App Startup](https://developer.android.com/topic/libraries/app-startup)
 * - 1. 提供组件初始化能力
 * - 2. 支持配置初始化依赖顺序
 * - 3. 解决官方使用 `ContentProvider` 无法支持多进程的问题
 * - 4. 解决官方需要手动注册初始化器的问题, 支持注解方式自动注册
 * - 5. 支持指定进程初始化
 *
 * 使用方式
 * Step 1: 实现 [com.geelee.startup.Initializer] 接口，在实现类中添加自己组件的初始化逻辑
 * Step 2: 实现类加上注解 [com.geelee.startup.annotation.AppInitializer]
 */
class Startup private constructor(
    private val appContext: Context,
    private val registry: IInitializerRegistry,
    private val logger: IStartupLogger
) {

    private val currentProcess = appContext.processName()

    fun init() {
        initInMainThread(currentProcess)
        MainScope().launch {
            initInWorkThreadAsync(currentProcess)
        }
    }

    /**
     * 子线程的依赖链之间并行执行
     */
    private suspend fun initInWorkThreadAsync(currentProcess: String) =
        withContext(Dispatchers.Default) {
            val taskInParallel = mutableListOf<Deferred<Unit>>()
            registry.getWorkThreadComponentChainList().forEach {
                taskInParallel.add(async { it.initOneByOneAsync(currentProcess) })
            }
            taskInParallel.forEach { it.await() }
        }

    /**
     * 主线程的依赖链之间串行执行
     */
    fun initInMainThread(currentProcess: String) {
        registry.getMainThreadComponentChainList().forEach {
            it.initOneByOne(currentProcess)
        }
    }

    /**
     * 依次执行链表中每个初始化器的初始化逻辑
     */
    private fun DependencyChain.initOneByOne(currentProcess: String) {
        this.chain.forEach { it.init(currentProcess) }
    }

    /**
     * 依次执行链表中每个初始化器的初始化逻辑(异步方法)
     */
    private suspend fun DependencyChain.initOneByOneAsync(currentProcess: String) {
        val dependencyChain = this
        withContext(Dispatchers.Default) {
            dependencyChain.initOneByOne(currentProcess)
        }
    }

    /**
     * 执行单个初始化器的初始化操作
     */
    private fun ComponentInfo.init(currentProcess: String) {
        if (!isSupportProcess(appContext, currentProcess)) {
            logger.i(
                "Skip ${this.name} cause it suppose to init at process${
                    this.supportProcess.toTypedArray().contentToString()
                } but the current process is $currentProcess"
            )
            return
        }
        try {
            val initializer = this.instance as Initializer
            logger.i(String.format("Initializing %s at %s", this.name, Thread.currentThread()))
            initializer.create(appContext, currentProcess)
            logger.i(String.format("Initialized %s at %s", this.name, Thread.currentThread()))
        } catch (e: Throwable) {
            if (logger.isDebugVersion()) {
                throw e
            } else {
                logger.e("init failed: ${e.message}", e)
            }
        }
    }

    companion object {
        @JvmStatic
        fun build(
            context: Context,
            registry: IInitializerRegistry,
            logger: IStartupLogger = DefaultLogger()
        ): Startup {
            val appContext = context.applicationContext ?: context
            return Startup(appContext, registry, logger)
        }
    }
}

private class DefaultLogger: IStartupLogger {
    override fun i(msg: String) {
        Log.i("Startup-Default-Logger", msg)
    }

    override fun e(msg: String, throwable: Throwable) {
        Log.e("Startup-Default-Logger", msg, throwable)
    }

    override fun isDebugVersion(): Boolean {
        return BuildConfig.DEBUG
    }

}