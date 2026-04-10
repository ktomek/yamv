package com.ktomek.yamv.feature

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import org.junit.jupiter.api.Test

class HasFeatureDispatcherTest {

    @Test
    fun `DefaultFeatureScope exposes scope dispatcher as featureDispatcher`() {
        val dispatcher = StandardTestDispatcher()
        val scope = DefaultFeatureScope(dispatcher)

        assertThat(scope.featureDispatcher).isSameInstanceAs(dispatcher)
    }

    @Test
    fun `DefaultFeatureScope without explicit dispatcher returns Default`() {
        val scope = DefaultFeatureScope()

        assertThat(scope.featureDispatcher).isEqualTo(Dispatchers.Default)
    }

    @Test
    fun `HasFeatureScope default implementation extracts dispatcher from scope`() {
        val dispatcher = StandardTestDispatcher()
        val feature = object : HasFeatureScope {
            override val featureScope = kotlinx.coroutines.CoroutineScope(
                kotlinx.coroutines.SupervisorJob() + dispatcher
            )
        }

        assertThat(feature.featureDispatcher).isSameInstanceAs(dispatcher)
    }
}
