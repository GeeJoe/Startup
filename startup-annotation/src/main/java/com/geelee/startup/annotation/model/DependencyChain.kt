package com.geelee.startup.annotation.model


/**
 * Created by zhiyueli on 2021/8/10.
 *
 * 初始化器依赖链, 单向无循环依赖，用一个有序的 LinkedHashSet 表示
 */
data class DependencyChain(val chain: LinkedHashSet<ComponentInfo>) {

    companion object {
        private const val CHAIN = " -> "
    }

    override fun toString(): String {
        return chain.toReadableString()
    }

    fun toDetailString(): String {
        val sb = StringBuilder()
        chain.reversed().forEach {
            sb.append(it.toDetailString())
            sb.append(CHAIN)
        }
        return sb.removeSuffix(CHAIN).toString()
    }

    private fun Set<ComponentInfo>.toReadableString(): String {
        val sb = StringBuilder()
        this.reversed().forEach {
            sb.append(it.toSimpleString())
            sb.append(CHAIN)
        }
        return sb.removeSuffix(CHAIN).toString()
    }
}