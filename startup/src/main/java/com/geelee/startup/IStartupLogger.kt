package com.geelee.startup


/**
 * Created by zhiyueli on 2021/6/30.
 */
interface IStartupLogger {
    fun i(msg: String)
    fun e(msg: String, throwable: Throwable)
    fun isDebugVersion(): Boolean
}