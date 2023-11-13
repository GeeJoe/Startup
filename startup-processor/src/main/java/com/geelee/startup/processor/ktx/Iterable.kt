package com.geelee.startup.processor.ktx

operator fun <T> Iterable<T>.contains(condition: (T) -> Boolean): Boolean {
    return find {
        condition(it)
    } != null
}
