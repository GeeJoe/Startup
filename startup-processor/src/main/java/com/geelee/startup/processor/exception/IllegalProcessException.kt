package com.geelee.startup.processor.exception

/**
 * The Runtime Exception thrown by the android.startup library.
 *
 * 当定义的进程信息不合法的时候抛出异常
 *
 * A 初始化器依赖于 B 初始化器
 * A: 子初始化器
 * B: 父初始化器
 * 父初始化器的 supportProcess 必须大于等于子初始化器.
 */
class IllegalProcessException : RuntimeException {
    constructor(message: String) : super(message) {}
    constructor(throwable: Throwable) : super(throwable) {}
    constructor(message: String, throwable: Throwable) : super(message, throwable) {}
}