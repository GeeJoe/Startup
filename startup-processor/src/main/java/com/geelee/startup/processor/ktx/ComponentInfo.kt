package com.geelee.startup.processor.ktx

import com.geelee.startup.annotation.model.ComponentInfo

internal fun List<ComponentInfo>.toFullString(): String {
    val sb = StringBuilder("[")
    this.forEach {
        sb.append(it.toString())
            .append(",")
    }
    sb.removeSuffix(",")
    sb.append("]")
    return sb.toString()
}

internal fun List<ComponentInfo>.toSimpleString(): String {
    val sb = StringBuilder("[")
    this.forEach {
        sb.append(it.toSimpleString())
            .append(",")
    }
    sb.removeSuffix(",")
    sb.append("]")
    return sb.toString()
}
