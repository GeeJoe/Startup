package com.geelee.startup

import com.geelee.startup.annotation.IInitializerRegistry.Companion.ALL_PROCESS
import com.geelee.startup.annotation.model.ComponentInfo
import com.geelee.startup.annotation.model.DependencyChain
import com.geelee.startup.processor.DependencyChainBuilder
import com.geelee.startup.processor.ILogger
import com.geelee.startup.processor.exception.CycleDependencyException
import com.geelee.startup.processor.exception.IllegalProcessException
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.core.IsEqual
import org.junit.Test


/**
 * Created by zhiyueli on 2021/8/13.
 */
class DependencyChainBuilderTest {

    private val logger = object : ILogger {
        override fun i(msg: String) {
            println(msg)
        }

        override fun e(msg: String, throwable: Throwable) {
            println(msg)
        }
    }

    @Test(expected = CycleDependencyException::class)
    fun `测试自己依赖自己`() {
        // given
        val allComponentMap = mapOf(
            "a" to ComponentInfo(
                name = "a",
                dependencies = listOf("a"),
                instanceProvider = {})
        )
        val builder = DependencyChainBuilder(logger, allComponentMap)
        // when
        builder.buildComponentChainList(allComponentMap.map { it.value })
    }

    /**
     * A -> B
     * B -> C
     * C -> D
     * D -> A (有一个循环依赖)
     */
    @Test(expected = CycleDependencyException::class)
    fun `测试不合法的链式循环依赖1`() {
        // given
        val allComponentMap = mapOf(
            "a" to ComponentInfo(
                name = "a",
                dependencies = listOf("b"),
                instanceProvider = {}),
            "b" to ComponentInfo(
                name = "b",
                dependencies = listOf("c"),
                instanceProvider = {}),
            "c" to ComponentInfo(
                name = "c",
                dependencies = listOf("d"),
                instanceProvider = {}),
            "d" to ComponentInfo(
                name = "d",
                dependencies = listOf("a"),
                instanceProvider = {}),
        )
        val builder = DependencyChainBuilder(logger, allComponentMap)
        // when
        builder.buildComponentChainList(allComponentMap.map { it.value })
    }

    /**
     * F -> G
     * G -> H, I
     * I -> F (循环依赖)
     */
    @Test(expected = CycleDependencyException::class)
    fun `测试不合法的链式循环依赖2`() {
        // given
        val allComponentMap = mapOf(
            "f" to ComponentInfo(
                name = "f",
                dependencies = listOf("g"),
                instanceProvider = {}),
            "g" to ComponentInfo(
                name = "g",
                dependencies = listOf("h", "i"),
                instanceProvider = {}),
            "i" to ComponentInfo(
                name = "i",
                dependencies = listOf("f"),
                instanceProvider = {}),
            "h" to ComponentInfo(
                name = "h",
                instanceProvider = {}),
        )
        val builder = DependencyChainBuilder(logger, allComponentMap)
        // when
        builder.buildComponentChainList(allComponentMap.map { it.value })
    }

    /**
     * F -> G
     * G -> H, I
     * J -> H
     */
    @Test
    fun `测试合法的链式依赖`() {
        // given
        val allComponentMap = mapOf(
            "f" to ComponentInfo(
                name = "f",
                dependencies = listOf("g"),
                instanceProvider = {}),
            "g" to ComponentInfo(
                name = "g",
                dependencies = listOf("h", "i"),
                instanceProvider = {}),
            "j" to ComponentInfo(
                name = "j",
                dependencies = listOf("h"),
                instanceProvider = {}),
            "h" to ComponentInfo(
                name = "h",
                instanceProvider = {}),
        )
        val builder = DependencyChainBuilder(logger, allComponentMap)
        // when
        builder.buildComponentChainList(allComponentMap.map { it.value })
    }

    /**
     * A 初始化器依赖于 B 初始化器
     * A: 子初始化器 processB
     * B: 父初始化器 processA
     * 父初始化器的 supportProcess 不包含子初始化器 -> 不合法
     */
    @Test(expected = IllegalProcessException::class)
    fun `测试被依赖的进程列表不包含依赖的进程列表1`() {
        // given
        val allComponentMap = mapOf(
            "a" to ComponentInfo(
                name = "a",
                supportProcess = listOf("processB"),
                dependencies = listOf("b"),
                instanceProvider = {}),
            "b" to ComponentInfo(
                name = "b",
                supportProcess = listOf("processA"),
                instanceProvider = {})
        )
        val builder = DependencyChainBuilder(logger, allComponentMap)
        // when
        builder.buildComponentChainList(allComponentMap.map { it.value })
    }

    /**
     * A 初始化器依赖于 B 初始化器
     * A: 子初始化器 allProcess
     * B: 父初始化器 processA
     * 父初始化器的 supportProcess 不包含子初始化器 -> 不合法
     */
    @Test(expected = IllegalProcessException::class)
    fun `测试被依赖的进程列表不包含依赖的进程列表2`() {
        // given
        val allComponentMap = mapOf(
            "a" to ComponentInfo(
                name = "a",
                supportProcess = listOf(ALL_PROCESS),
                dependencies = listOf("b"),
                instanceProvider = {}),
            "b" to ComponentInfo(
                name = "b",
                supportProcess = listOf("processA"),
                instanceProvider = {})
        )
        val builder = DependencyChainBuilder(logger, allComponentMap)
        // when
        builder.buildComponentChainList(allComponentMap.map { it.value })
    }

    /**
     * 输入：
     * F -> G
     * L
     * M
     * G -> H, K
     * J -> H
     * N
     * O
     *
     * 输出：
     * F -> G -> K -> H
     * L
     * M
     * J
     * N
     * O
     */
    @Test
    fun `测试构建依赖链是否正常`() {
        // given
        val f = ComponentInfo(
            name = "f",
            dependencies = listOf("g"),
            instanceProvider = {})
        val k = ComponentInfo(
            name = "k",
            instanceProvider = {})
        val h = ComponentInfo(
            name = "h",
            instanceProvider = {})
        val l = ComponentInfo(
            name = "l",
            instanceProvider = {})
        val m = ComponentInfo(
            name = "m",
            instanceProvider = {})
        val g = ComponentInfo(
            name = "g",
            dependencies = listOf("h", "k"),
            instanceProvider = {})
        val j = ComponentInfo(
            name = "j",
            dependencies = listOf("h"),
            instanceProvider = {})
        val n = ComponentInfo(
            name = "n",
            instanceProvider = {})
        val o = ComponentInfo(
            name = "o",
            instanceProvider = {})
        val allComponentMap = mapOf(
            "f" to f,
            "k" to k,
            "h" to h,
            "l" to l,
            "m" to m,
            "g" to g,
            "j" to j,
            "n" to n,
            "o" to o,
        )
        val builder = DependencyChainBuilder(logger, allComponentMap)
        // when
        val chainList = builder.buildComponentChainList(allComponentMap.map { it.value })
        chainList.forEachIndexed { index, dependencyChain ->
            when (index) {
                0 -> {
                    assertThat(
                        dependencyChain.toString(),
                        IsEqual(DependencyChain(linkedSetOf(h, k, g, f)).toString())
                    )
                }

                1 -> {
                    assertThat(
                        dependencyChain.toString(),
                        IsEqual(DependencyChain(linkedSetOf(l)).toString())
                    )
                }

                2 -> {
                    assertThat(
                        dependencyChain.toString(),
                        IsEqual(DependencyChain(linkedSetOf(m)).toString())
                    )
                }

                3 -> {
                    assertThat(
                        dependencyChain.toString(),
                        IsEqual(DependencyChain(linkedSetOf(j)).toString())
                    )
                }

                4 -> {
                    assertThat(
                        dependencyChain.toString(),
                        IsEqual(DependencyChain(linkedSetOf(n)).toString())
                    )
                }

                5 -> {
                    assertThat(
                        dependencyChain.toString(),
                        IsEqual(DependencyChain(linkedSetOf(o)).toString())
                    )
                }

                else -> {
                    throw AssertionError("expect size is 6 but now is ${chainList.size}")
                }
            }
        }
    }

    /**
     * A 初始化器依赖于 B 初始化器
     * A: 子初始化器 mainThread
     * B: 父初始化器 workThread
     * A 初始化器不能依赖于 B 初始化器
     */
    @Test(expected = IllegalThreadStateException::class)
    fun `测试线程定义异常`() {
        // given
        val allComponentMap = mapOf(
            "a" to ComponentInfo(
                name = "a",
                threadMode = ComponentInfo.ThreadMode.MainThread,
                dependencies = listOf("b"),
                instanceProvider = {}),
            "b" to ComponentInfo(
                name = "b",
                threadMode = ComponentInfo.ThreadMode.WorkThread,
                instanceProvider = {})
        )
        val builder = DependencyChainBuilder(logger, allComponentMap)
        // when
        builder.buildComponentChainList(allComponentMap.map { it.value })
    }

    /**
     * 输入：
     * A -> B
     * B -> C
     * C -> D
     * D -> E
     * E -> F
     * F -> G
     *
     * 输出：
     * A -> B -> C -> D -> E -> F -> G
     */
    @Test
    fun `测试构建依赖链顺序是否正常`() {
        // given
        val a = ComponentInfo(
            name = "a",
            dependencies = listOf("b"),
            instanceProvider = {})
        val b = ComponentInfo(
            name = "b",
            dependencies = listOf("c"),
            instanceProvider = {})
        val c = ComponentInfo(
            name = "c",
            dependencies = listOf("d"),
            instanceProvider = {})
        val d = ComponentInfo(
            name = "d",
            dependencies = listOf("e"),
            instanceProvider = {})
        val e = ComponentInfo(
            name = "e",
            dependencies = listOf("f"),
            instanceProvider = {})
        val f = ComponentInfo(
            name = "f",
            dependencies = listOf("g"),
            instanceProvider = {})
        val g = ComponentInfo(
            name = "g",
            instanceProvider = {})
        val allComponentMap = mapOf(
            "a" to a,
            "b" to b,
            "c" to c,
            "d" to d,
            "e" to e,
            "f" to f,
            "g" to g
        )
        val builder = DependencyChainBuilder(logger, allComponentMap)
        // when
        val chainList = builder.buildComponentChainList(allComponentMap.map { it.value })
        val actual =
            chainList.first().chain.toList().map { it.name }.toTypedArray().contentToString()
        val expect =
            listOf("a", "b", "c", "d", "e", "f", "g").reversed().toTypedArray().contentToString()
        assertThat(actual, IsEqual(expect))
    }
}