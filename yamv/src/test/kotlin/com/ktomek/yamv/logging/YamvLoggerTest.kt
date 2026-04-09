package com.ktomek.yamv.logging

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

class YamvLoggerTest {

    @Test
    fun `log levels are ordered from verbose to none`() {
        val levels = YamvLogLevel.entries.toList()
        assertThat(levels).containsExactly(
            YamvLogLevel.VERBOSE,
            YamvLogLevel.DEBUG,
            YamvLogLevel.INFO,
            YamvLogLevel.WARN,
            YamvLogLevel.ERROR,
            YamvLogLevel.NONE,
        ).inOrder()
    }

    @Test
    fun `NONE ordinal is greater than ERROR ordinal`() {
        assertThat(YamvLogLevel.NONE.ordinal).isGreaterThan(YamvLogLevel.ERROR.ordinal)
    }

    @Test
    fun `default log level is NONE`() {
        Yamv.reset()
        assertThat(Yamv.logLevel).isEqualTo(YamvLogLevel.NONE)
    }

    @Test
    fun `logger is not called when level is below configured threshold`() {
        Yamv.reset()
        Yamv.logLevel = YamvLogLevel.WARN
        val received = mutableListOf<String>()
        Yamv.logger = YamvLogger { _, _, message -> received.add(message) }

        Yamv.log(YamvLogLevel.DEBUG, "tag", "should not appear")
        Yamv.log(YamvLogLevel.INFO, "tag", "should not appear either")

        assertThat(received).isEmpty()
    }

    @Test
    fun `logger is called when level meets configured threshold`() {
        Yamv.reset()
        Yamv.logLevel = YamvLogLevel.WARN
        val received = mutableListOf<String>()
        Yamv.logger = YamvLogger { _, _, message -> received.add(message) }

        Yamv.log(YamvLogLevel.WARN, "tag", "warn message")
        Yamv.log(YamvLogLevel.ERROR, "tag", "error message")

        assertThat(received).containsExactly("warn message", "error message").inOrder()
    }

    @Test
    fun `logger receives correct level tag and message`() {
        Yamv.reset()
        Yamv.logLevel = YamvLogLevel.VERBOSE
        val calls = mutableListOf<Triple<YamvLogLevel, String, String>>()
        Yamv.logger = YamvLogger { level, tag, message -> calls.add(Triple(level, tag, message)) }

        Yamv.log(YamvLogLevel.INFO, "FeatureRouter", "initialized")

        assertThat(calls).hasSize(1)
        assertThat(calls[0].first).isEqualTo(YamvLogLevel.INFO)
        assertThat(calls[0].second).isEqualTo("FeatureRouter")
        assertThat(calls[0].third).isEqualTo("initialized")
    }

    @Test
    fun `NONE level never logs`() {
        Yamv.reset()
        Yamv.logLevel = YamvLogLevel.VERBOSE
        val received = mutableListOf<String>()
        Yamv.logger = YamvLogger { _, _, message -> received.add(message) }

        Yamv.log(YamvLogLevel.NONE, "tag", "this should never appear")

        assertThat(received).isEmpty()
    }
}
