package com.geelee.startup.processor

import com.geelee.startup.annotation.IInitializerRegistry.Companion.ALL_PROCESS
import com.geelee.startup.annotation.IInitializerRegistry.Companion.MAIN_PROCESS
import com.geelee.startup.annotation.model.ComponentInfo
import com.geelee.startup.annotation.model.DependencyChain
import com.geelee.startup.processor.exception.CycleDependencyException
import com.geelee.startup.processor.exception.IllegalProcessException
import com.geelee.startup.processor.ktx.contains
import com.geelee.startup.processor.ktx.toSimpleString
import java.util.concurrent.CopyOnWriteArraySet


/**
 * Created by zhiyueli on 2021/8/12.
 */
class DependencyChainBuilder(
    private val logger: ILogger,
    private val allComponentList: Map<String, ComponentInfo>
) {

    private val building = CopyOnWriteArraySet<ComponentInfo>()
    private val built = CopyOnWriteArraySet<ComponentInfo>()

    /**
     * 构建初始化器依赖链列表
     *
     * 比如 A、B、C、D、E、F、G 可能构建出以下依赖链列表
     *
     * A -> B -> C
     * D -> E
     * F
     * G
     *
     * @param components 待构造依赖链列表的 Component 列表
     */
    fun buildComponentChainList(components: List<ComponentInfo>): List<DependencyChain> {
        val result = mutableListOf<DependencyChain>()
        components.forEach {
            val dependencyChain = DependencyChain(linkedSetOf())
            dfsBuildDirectedAcyclicGraph(
                it,
                null,
                building = building,
                built = built,
                dependencyChain = dependencyChain
            )
            if (dependencyChain.chain.isNotEmpty()) {
                result.add(dependencyChain)
            }
        }
        logger.i(
            String.format(
                "buildComponentChainList(${Thread.currentThread()}) ->\n- input=%s\n- output=%s",
                components.toSimpleString(),
                result.toTypedArray().contentToString()
            )
        )
        return result
    }

    /**
     * dfs 构建依赖链（有向无环图）
     * @param component 当前 Component 节点
     * @param subComponent 当前 Component 节点的依赖节点
     * @param building 全局记录正在构建依赖链的 Component 列表
     * @param built 全局记录已经确定链位置的 Component 列表
     * @param dependencyChain 记录本次确定链位置的 Component 列表, 是本次依赖链产物
     */
    private fun dfsBuildDirectedAcyclicGraph(
        component: ComponentInfo,
        subComponent: ComponentInfo?,
        building: MutableSet<ComponentInfo>,
        built: MutableSet<ComponentInfo>,
        dependencyChain: DependencyChain
    ) {
        // 断言非循环依赖
        assertNotCycleDependency(component, building)
        // 已经构建好了就跳过
        if (built.contains { it.name == component.name }) {
            return
        }
        building.add(component)
        // 先初始化依赖的 Component
        chainDependencyComponent(component, building, built, dependencyChain)
        // 断言非异常进程定义
        assertLegalProcess(component.name, subComponent?.name)
        // 断言非异常线程定义
        assertLegalThread(component.name, subComponent?.name)
        // 构建
        building.remove(component)
        built.add(component)
        dependencyChain.chain.add(component)
    }

    /**
     * 断言非循环依赖
     *
     * @param componentInfo 当前 Component 节点
     * @param building 全局记录正在构建依赖链的 Component 列表
     */
    private fun assertNotCycleDependency(
        componentInfo: ComponentInfo,
        building: MutableSet<ComponentInfo>
    ) {
        if (building.contains { it.name == componentInfo.name }) {
            val message = String.format(
                "Cannot initialize %s. Cycle detected.", componentInfo.name
            )
            throw CycleDependencyException(message)
        }
    }

    /**
     * 断言合法定义的进程信息：
     * A 初始化器依赖于 B 初始化器
     * A: 子初始化器
     * B: 父初始化器
     * 父初始化器的 supportProcess 必须大于等于子初始化器.
     *
     * @param componentName 父初始化器
     * @param subComponentName 子初始化器
     */
    private fun assertLegalProcess(
        componentName: String,
        subComponentName: String?
    ) {
        if ((subComponentName != null) &&
            !getSupportProcessList(componentName)
                .contains(getSupportProcessList(subComponentName))
        ) {
            val message = String.format(
                "父 Initializer(%s) 的 supportProcess 必须大于等于子 Initializer(%s).",
                componentName,
                subComponentName
            )
            throw IllegalProcessException(message)
        }
    }

    /**
     * 断言合法定义的线程信息：
     * A: 在主线程初始化
     * B: 在子线程初始化
     * A 初始化器不能依赖于 B 初始化器
     *
     * @param componentName 父初始化器
     * @param subComponentName 子初始化器
     */
    private fun assertLegalThread(
        componentName: String,
        subComponentName: String?
    ) {
        if (subComponentName.isNullOrEmpty()) {
            return
        }
        val component = allComponentList[componentName]
        val subComponent = allComponentList[subComponentName]
        if (component?.threadMode == ComponentInfo.ThreadMode.WorkThread &&
            subComponent?.threadMode == ComponentInfo.ThreadMode.MainThread
        ) {
            val message = String.format(
                "运行在主线程的 Initializer(%s) 不能依赖于运行在子线程的 Initializer(%s).",
                subComponent,
                component
            )
            throw IllegalThreadStateException(message)
        }
    }

    /**
     * 链接当前结点的所有依赖节点
     *
     * @param componentInfo 当前 Component 节点
     * @param building 全局记录正在构建依赖链的 Component 列表
     * @param built 全局记录已经确定链位置的 Component 列表
     * @param dependencyChain 记录本次确定链位置的 Component 列表, 是本次依赖链产物
     */
    private fun chainDependencyComponent(
        componentInfo: ComponentInfo,
        building: MutableSet<ComponentInfo>,
        built: MutableSet<ComponentInfo>,
        dependencyChain: DependencyChain
    ) {
        val dependencies = componentInfo.dependencies
        if (dependencies.isEmpty()) {
            return
        }
        for (clazzName in dependencies) {
            val dependencyComponentInfo = allComponentList[clazzName]
            if (dependencyComponentInfo == null || built.contains { it.name == clazzName }) {
                continue
            }
            dfsBuildDirectedAcyclicGraph(
                dependencyComponentInfo,
                componentInfo,
                building,
                built,
                dependencyChain
            )
        }
    }

    /**
     * 判断当前列表包含的内容是否大于等于 targetList
     * 如果当前列表包含 [ALL_PROCESS] 代表肯定包含 targetList
     */
    private fun List<String>.contains(targetList: List<String>): Boolean {
        if (this.contains(ALL_PROCESS)) return true
        targetList.forEach {
            if (!this.contains(it)) {
                return false
            }
        }
        return true
    }

    /**
     * 正常情况下这里不会为空，除非注解中真的没有定义 supportProcess, 那默认就在主进程初始化
     *
     * @param componentName 当前初始化器名字
     */
    private fun getSupportProcessList(componentName: String): List<String> {
        val processList =
            allComponentList[componentName]?.supportProcess ?: emptyList()
        if (processList.isEmpty()) {
            return arrayListOf(MAIN_PROCESS)
        }
        return processList
    }
}