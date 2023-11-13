package com.geelee.startup.processor.ktx

import com.geelee.startup.annotation.model.ComponentInfo
import com.geelee.startup.annotation.model.DependencyChain


/**
 * 根据 ComponentInfo 构造实例化代码
 */
internal fun ComponentInfo.toLiteral(): String {
    val instanceLambda = "{ ${name}() }"
    val threadMode = "${ComponentInfo.ThreadMode::class.qualifiedName}.${threadMode}"
    return StringBuilder()
        .appendln("${ComponentInfo::class.simpleName}(")
        .appendln("\"${name}\",")
        .appendln("${supportProcess.toLiteral()},")
        .appendln("${threadMode},")
        .appendln("${dependencies.toLiteral()},")
        .append("${instanceLambda})")
        .toString()
}

/**
 * 根据 String 列表构造 listOf 代码块
 */
internal fun List<String>.toLiteral(): String {
    if (this.isEmpty()) return "emptyList()"
    val sb = StringBuilder()
    sb.append("listOf(")
    this.forEach {
        sb.append("\"${it}\", ")
    }
    sb.replace(sb.length - 2, sb.length, ")")
    return sb.toString()
}

/**
 * 根据 Component 列表构造 mapOf 代码块
 */
internal fun List<ComponentInfo>.toMapLiteral(): String {
    if (this.isEmpty()) return "emptyMap()"
    val sb = StringBuilder("mapOf(").appendln()
    this.forEach {
        sb.append("\"${it.name}\" to ${it.simpleName}").appendln(", ")
    }
    sb.replace(sb.length - 2, sb.length, ")")
    return sb.toString()
}

/**
 * 根据 Component 列表构造 listOf 代码块
 */
internal fun List<DependencyChain>.toListLiteral(): String {
    if (this.isEmpty()) return "emptyList()"
    val sb = StringBuilder("listOf(").appendln()
    this.forEach {
        sb.append(it.toLiteral()).appendln(", ")
    }
    sb.replace(sb.length - 2, sb.length, ")")
    return sb.toString()
}

/**
 * 根据 DependencyChain 构造实例化代码
 */
internal fun DependencyChain.toLiteral(): String {
    val set = StringBuilder("linkedSetOf(")
    chain.forEach { componentInfo ->
        val propertyName = componentInfo.simpleName
        set.append(propertyName).append(", ")
    }
    set.replace(set.length - 2, set.length, ")")
    return "${DependencyChain::class.simpleName}($set)"
}
