package com.geelee.startup.processor


/**
 * Created by zhiyueli on 2021/6/30.
 */
interface ILogger {
    fun i(msg: String)
    fun e(msg: String, throwable: Throwable)
}