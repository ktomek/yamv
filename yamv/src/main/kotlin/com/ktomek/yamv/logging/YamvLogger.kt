package com.ktomek.yamv.logging

fun interface YamvLogger {
    fun log(level: YamvLogLevel, tag: String, message: String)
}
