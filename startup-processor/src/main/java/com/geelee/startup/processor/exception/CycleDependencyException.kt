package com.geelee.startup.processor.exception

/**
 * The Runtime Exception thrown by the android.startup library.
 *
 * 当有循环依赖的时候抛出异常
 */
class CycleDependencyException : RuntimeException {
    constructor(message: String) : super(message) {}
    constructor(throwable: Throwable) : super(throwable) {}
    constructor(message: String, throwable: Throwable) : super(message, throwable) {}
}