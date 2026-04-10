# Features Guide

A **Feature** transforms a stream of intentions into a stream of outcomes. Each feature is responsible for one piece of logic.

## FunctionTypedFeature — simplest

For one-shot operations (no streaming, no background work):

```kotlin
val incrementFeature = functionTypedFeature<CounterState, CounterIntention.Increment> { _ ->
    StateOutcome { state -> state.copy(count = state.count + 1) }
}
```

Or as a class:

```kotlin
class IncrementFeature : FunctionTypedFeature<CounterState, CounterIntention.Increment> {
    override suspend fun invoke(intention: CounterIntention.Increment): Outcome<CounterState> =
        StateOutcome { state -> state.copy(count = state.count + 1) }
}
```

Use `.wrap()` to convert to `Feature<S>`:
```kotlin
IncrementFeature().wrap()
```

> **Note:** Multiple concurrent intentions are processed concurrently. If your `invoke` does async work (network, DB), each intention gets its own coroutine automatically.

## TypedFeature — typed streaming

For features that transform a stream of intentions:

```kotlin
class FetchDataFeature(
    private val repository: DataRepository,
) : TypedFeature<AppState, AppIntention.Fetch> {
    override fun invoke(
        intentions: Flow<AppIntention.Fetch>
    ): Flow<Outcome<AppState>> = channelFlow {
        intentions.collect { intention ->
            try {
                val data = repository.fetch(intention.id)  // suspend call
                send(StateOutcome { state -> state.copy(data = data) })
            } catch (e: Exception) {
                send(StateOutcome { state -> state.copy(error = e.message) })
            }
        }
    }
}
```

Use `.wrap()` to convert:
```kotlin
FetchDataFeature(repository).wrap()
```

## TypedFeature with background work

Use `channelFlow { }` when you need to launch background coroutines (timers, polling):

```kotlin
class AutoIncrementFeature : TypedFeature<CounterState, CounterIntention.StartAuto> {
    override fun invoke(
        intentions: Flow<CounterIntention.StartAuto>
    ): Flow<Outcome<CounterState>> = channelFlow {
        var timerJob: Job? = null
        intentions.collect { _ ->
            timerJob?.cancel()
            timerJob = launch {
                while (isActive) {
                    delay(1_000)
                    send(StateOutcome { state -> state.copy(count = state.count + 1) })
                }
            }
        }
    }
}
```

The `channelFlow` block's `this` is a `ProducerScope` (a `CoroutineScope` + `send()`). Any `launch { }` inside is a child of the feature's coroutine — cancelled automatically when the ViewModel is cleared.

## FlowFeature — low-level

For features that need to handle multiple intention types or work with the raw `Flow<Any>`:

```kotlin
class LoggingFeature : Feature.FlowUnitFeature<AppState> {
    override fun invoke(intentions: Flow<Any>): Flow<Unit> =
        intentions.onEach { intention ->
            Log.d("YAMV", "Intention dispatched: $intention")
        }
}
```

## Combining Outcomes

A single feature can emit multiple outcome types:

```kotlin
class LoginFeature(
    private val auth: AuthService,
) : TypedFeature<AppState, AppIntention.Login> {
    override fun invoke(
        intentions: Flow<AppIntention.Login>
    ): Flow<Outcome<AppState>> = channelFlow {
        intentions.collect { intention ->
            try {
                val user = auth.login(intention.email, intention.password)
                send(StateOutcome { state -> state.copy(user = user, isLoading = false) })
                send(object : IntentionOutcome<AppState> {
                    override val intention = AppIntention.LoadDashboard
                })
            } catch (e: AuthException) {
                send(StateOutcome { state -> state.copy(error = e.message, isLoading = false) })
                send(object : EffectOutcome<AppState> {})  // trigger snackbar, etc.
            }
        }
    }
}
```
