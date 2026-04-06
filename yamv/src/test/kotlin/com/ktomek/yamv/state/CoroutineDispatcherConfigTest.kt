package com.ktomek.yamv.state

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class CoroutineDispatcherConfigTest {

    @Test
    fun `GIVEN DefaultCoroutineDispatcherConfig WHEN provideIntentionDispatcher called THEN returns Main dispatcher`() {
        // Arrange
        val config = DefaultCoroutineDispatcherConfig()

        // Act
        val result = config.provideIntentionDispatcher(null)

        // Assert
        assertEquals(Dispatchers.Main, result)
    }

    @Test
    fun `GIVEN DefaultCoroutineDispatcherConfig WHEN provideIntentionDispatcher called with intention THEN returns Main dispatcher`() {
        // Arrange
        val config = DefaultCoroutineDispatcherConfig()

        // Act
        val result = config.provideIntentionDispatcher("some_intention")

        // Assert
        assertEquals(Dispatchers.Main, result)
    }

    @Test
    fun `GIVEN DefaultCoroutineDispatcherConfig WHEN provideReducerDispatcher called THEN returns Main dispatcher`() {
        // Arrange
        val config = DefaultCoroutineDispatcherConfig()

        // Act
        val result = config.provideReducerDispatcher()

        // Assert
        assertEquals(Dispatchers.Main, result)
    }

    @Test
    fun `GIVEN DefaultCoroutineDispatcherConfig WHEN provideFeatureDispatcher called THEN returns Default dispatcher`() {
        // Arrange
        val config = DefaultCoroutineDispatcherConfig()

        // Act
        val result = config.provideFeatureDispatcher("some_feature")

        // Assert
        assertEquals(Dispatchers.Default, result)
    }

    @Test
    fun `GIVEN DefaultCoroutineDispatcherConfig with custom dispatchers WHEN dispatchers requested THEN returns custom ones`() {
        // Arrange
        val customDefault = StandardTestDispatcher()
        val customUI = StandardTestDispatcher()
        val config = DefaultCoroutineDispatcherConfig(
            default = customDefault,
            ui = customUI
        )

        // Act
        val intentionDispatcher = config.provideIntentionDispatcher(null)
        val reducerDispatcher = config.provideReducerDispatcher()
        val featureDispatcher = config.provideFeatureDispatcher("test")

        // Assert
        assertEquals(customUI, intentionDispatcher)
        assertEquals(customUI, reducerDispatcher)
        assertEquals(customDefault, featureDispatcher)
    }
}
