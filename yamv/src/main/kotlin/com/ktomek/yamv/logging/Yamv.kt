package com.ktomek.yamv.logging

object Yamv {
    var logLevel: YamvLogLevel = YamvLogLevel.NONE
    var logger: YamvLogger = YamvLogger { level, tag, message ->
        println("[$level] $tag: $message")
    }

    fun log(level: YamvLogLevel, tag: String, message: String) {
        if (level == YamvLogLevel.NONE) return
        if (level.ordinal >= logLevel.ordinal) {
            logger.log(level, tag, message)
        }
    }

    // Test helper — resets to defaults between tests
    internal fun reset() {
        logLevel = YamvLogLevel.NONE
        logger = YamvLogger { level, tag, message -> println("[$level] $tag: $message") }
    }
}
