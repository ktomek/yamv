# Getting Started

## Prerequisites

- Android Studio Hedgehog or newer
- Kotlin 2.0+
- KSP plugin (for `@AutoState`/`@AutoFeature` code generation)

## Installation

### Step 1: Add KSP plugin

In your app module's `build.gradle.kts`:

```kotlin
plugins {
    id("com.google.devtools.ksp") version "2.0.21-1.0.28"
}
```

### Step 2: Add dependencies

=== "Hilt (Android)"

    ```kotlin
    dependencies {
        implementation("io.github.ktomek:core:VERSION")
        implementation("io.github.ktomek:yamv:VERSION")
        implementation("io.github.ktomek:yamv-retainer:VERSION")
        implementation("io.github.ktomek:yamv-hilt:VERSION")
        ksp("io.github.ktomek:processor-hilt:VERSION")
    }
    ```

=== "Koin (Multiplatform)"

    ```kotlin
    // commonMain
    dependencies {
        implementation("io.github.ktomek:core:VERSION")
        implementation("io.github.ktomek:yamv:VERSION")
        implementation("io.github.ktomek:yamv-retainer:VERSION")
        implementation("io.github.ktomek:yamv-koin:VERSION")
    }
    ```

## Your First Feature

### 1. Define State

```kotlin
import com.ktomek.yamv.core.State
import com.ktomek.yamv.annotations.AutoState

@AutoState
data class CounterState(val count: Int = 0) : State
```

`@AutoState` triggers KSP to generate `CounterStateStore` — a `@HiltViewModel` subclass of `MviRetainedStore<CounterState, Any>`.

### 2. Define Intentions

```kotlin
sealed class CounterIntention {
    data object Increment : CounterIntention()
    data object Decrement : CounterIntention()
    data class SetValue(val value: Int) : CounterIntention()
}
```

### 3. Define Outcomes

Declare outcomes as standalone classes. They are pure `(S) -> S` functions, decoupled from features and testable without coroutines:

```kotlin
import com.ktomek.yamv.core.StateOutcome

class IncrementOutcome : StateOutcome<CounterState> {
    override fun reduce(prevState: CounterState) =
        prevState.copy(count = prevState.count + 1)
}

class DecrementOutcome : StateOutcome<CounterState> {
    override fun reduce(prevState: CounterState) =
        prevState.copy(count = prevState.count - 1)
}

class SetValueOutcome(private val value: Int) : StateOutcome<CounterState> {
    override fun reduce(prevState: CounterState) =
        prevState.copy(count = value)
}
```

### 4. Write Features

Each feature maps intentions to outcomes:

```kotlin
import com.ktomek.yamv.annotations.AutoFeature
import com.ktomek.yamv.feature.TypedFeature
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@AutoFeature
class IncrementFeature : TypedFeature<CounterState, CounterIntention.Increment> {
    override fun invoke(
        intentions: Flow<CounterIntention.Increment>
    ): Flow<Outcome<CounterState>> =
        intentions.map { IncrementOutcome() }
}

@AutoFeature
class DecrementFeature : TypedFeature<CounterState, CounterIntention.Decrement> {
    override fun invoke(
        intentions: Flow<CounterIntention.Decrement>
    ): Flow<Outcome<CounterState>> =
        intentions.map { DecrementOutcome() }
}
```

### 5. Collect State in Compose

=== "Hilt"

    ```kotlin
    @Composable
    fun CounterScreen(store: CounterStateStore = hiltMviStore()) {
        val state by store.state.collectAsStateWithLifecycle()
        CounterContent(
            count = state.count,
            onIncrement = { store.dispatch(CounterIntention.Increment) },
            onDecrement = { store.dispatch(CounterIntention.Decrement) },
        )
    }
    ```

=== "Koin"

    ```kotlin
    @Composable
    fun CounterScreen(store: CounterStateStore = koinMviStore()) {
        val state by store.state.collectAsStateWithLifecycle()
        // same as above
    }
    ```

## Running the Sample App

The `:app:hilt` module is a working counter demo:

```bash
./gradlew :app:hilt:installDebug
```
