package com.ktomek.yamv.feature

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * Default implementation of [HasFeatureScope] for use with interface delegation.
 *
 * Creates a [CoroutineScope] backed by a [SupervisorJob], using [dispatcher] if provided
 * or [Dispatchers.Default] otherwise.
 *
 * @param dispatcher Optional dispatcher for the scope. Defaults to [Dispatchers.Default].
 */
class DefaultFeatureScope(
    dispatcher: CoroutineDispatcher? = null,
) : HasFeatureScope {
    override val featureScope: CoroutineScope =
        CoroutineScope(SupervisorJob() + (dispatcher ?: Dispatchers.Default))
}
