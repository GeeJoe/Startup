package com.geelee.startup

import android.content.Context
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.AndroidJUnit4
import com.geelee.startup.annotation.IInitializerRegistry
import com.geelee.startup.annotation.IInitializerRegistry.Companion.ALL_PROCESS
import com.geelee.startup.annotation.model.ComponentInfo
import com.geelee.startup.annotation.model.DependencyChain
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.core.IsEqual
import org.hamcrest.core.IsInstanceOf
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * Created by zhiyueli on 2021/8/9.
 */
@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
class StartupTest {

    private class MockException(msg: String) : RuntimeException(msg)

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val logger = object : IStartupLogger {
        override fun i(msg: String) {
            println(msg)
        }

        override fun e(msg: String, throwable: Throwable) {
            println(msg)
            errorExceptionInLog = throwable
        }

        override fun isDebugVersion(): Boolean {
            return isDebugVersion
        }
    }
    private var isDebugVersion = false
    private var errorExceptionInLog: Throwable? = null

    private val registry = object : IInitializerRegistry {
        override fun getAllInitializer(): Map<String, ComponentInfo> {
            return allInitializer
        }

        override fun getMainThreadComponentChainList(): List<DependencyChain> {
            return mainThreadComponentChainList
        }

        override fun getWorkThreadComponentChainList(): List<DependencyChain> {
            return emptyList()
        }
    }

    private lateinit var allInitializer: Map<String, ComponentInfo>
    private lateinit var mainThreadComponentChainList: List<DependencyChain>

    var cInitialized = false
    var dInitialized = false

    inner class InitializerC : Initializer {
        override fun create(context: Context, processName: String) {
            cInitialized = true
        }
    }

    inner class InitializerD : Initializer {
        override fun create(context: Context, processName: String) {
            dInitialized = true
        }
    }

    inner class InitializerWithException : Initializer {
        override fun create(context: Context, processName: String) {
            throw MockException("mock exception")
        }
    }

    /**
     * C 初始化器在所有进程初始化
     * D 初始化器只在 processA 进程初始化
     *
     * 期望：C 能初始化，D 不能初始化
     */
    @Test
    fun `测试在非指定进程是否能初始化`() {
        // given
        val c = ComponentInfo(
            name = InitializerC::class.java.name,
            instanceProvider = { InitializerC() },
            supportProcess = listOf(ALL_PROCESS) // 所有进程
        )
        val d = ComponentInfo(
            name = InitializerD::class.java.name,
            instanceProvider = { InitializerD() },
            supportProcess = listOf("processA") // 只在 processA 进程初始化
        )
        allInitializer = mapOf(
            InitializerC::class.java.name to c,
            InitializerD::class.java.name to d
        )
        mainThreadComponentChainList =
            listOf(DependencyChain(linkedSetOf(c)), DependencyChain(linkedSetOf(d)))
        // when
        Startup.build(context, registry, logger).initInMainThread("MAIN_PROCESS")
        // then
        assertThat(cInitialized, IsEqual(true))
        assertThat(dInitialized, IsEqual(false))
    }


    @Test(expected = MockException::class)
    fun `测试 debug 版初始化异常`() {
        // given
        isDebugVersion = true
        val a = ComponentInfo(
            name = InitializerWithException::class.java.name,
            instanceProvider = { InitializerWithException() },
            supportProcess = listOf(ALL_PROCESS) // 所有进程
        )
        allInitializer = mapOf(
            InitializerWithException::class.java.name to a
        )
        mainThreadComponentChainList = listOf(DependencyChain(linkedSetOf(a)))
        // when
        Startup.build(context, registry, logger).initInMainThread("MAIN_PROCESS")
    }

    @Test
    fun `测试非 debug 版初始化异常`() {
        // given
        isDebugVersion = false
        val a = ComponentInfo(
            name = InitializerWithException::class.java.name,
            instanceProvider = { InitializerWithException() },
            supportProcess = listOf(ALL_PROCESS) // 所有进程
        )
        allInitializer = mapOf(
            InitializerWithException::class.java.name to a
        )
        mainThreadComponentChainList = listOf(DependencyChain(linkedSetOf(a)))
        // when
        Startup.build(context, registry, logger).initInMainThread("MAIN_PROCESS")
        // then
        assertThat(errorExceptionInLog, IsInstanceOf(MockException::class.java))
    }
}