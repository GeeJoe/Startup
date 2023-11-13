package com.geelee.startup.processor.exception

/**
 * The Runtime Exception thrown by the android.startup library.
 *
 * 当定义的线程信息不合法的时候抛出异常
 *
 * A: 在主线程初始化
 * B: 在子线程初始化
 * A 初始化器不能依赖于 B 初始化器
 */
class IllegalThreadException : RuntimeException {
    constructor(message: String) : super(message) {}
    constructor(throwable: Throwable) : super(throwable) {}
    constructor(message: String, throwable: Throwable) : super(message, throwable) {}
}