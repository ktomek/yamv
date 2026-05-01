# Getting Started

## Prerequisites

- Android Studio Hedgehog or newer
- Kotlin 2.1+ (project itself builds on 2.3.20)
- KSP plugin (for `@AutoState`/`@AutoFeature` code generation) — pin a version that matches your Kotlin version

## Installation

### Step 1: Add KSP plugin

In your app module's `build.gradle.kts`:

```kotlin
plugins {
    // Use a KSP version that matches your Kotlin version (e.g. "2.3.20-2.3.2")
    id("com.google.devtools.ksp") version "<KSP_VERSION>"
}
```

### Step 2: Add dependencies

=== "Hilt (Android)"

    ```kotlin
    dependencies {
        implementation("io.github.ktomek:yamv-core:VERSION")
        implementation("io.github.ktomek:yamv:VERSION")
        implementation("io.github.ktomek:yamv-retainer:VERSION")
        implementation("io.github.ktomek:yamv-hilt:VERSION")
        ksp("io.github.ktomek:yamv-processor-hilt:VERSION")
    }
    ```

=== "Koin (Multiplatform)"

    ```kotlin
    // commonMain
    dependencies {
        implementation("io.github.ktomek:yamv-core:VERSION")
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

### App-lifetime state across navigation

By default both `hiltMviStore()` and `koinMviStore()` resolve through `LocalViewModelStoreOwner.current`, which Compose Navigation overrides per back-stack entry — every destination gets its own store. For state that should survive navigation (auth, session, preferences):

=== "Hilt"

    ```kotlin
    @Composable
    fun AuthScreen(store: AuthStateStore = hiltMviAppStore()) { ... }
    ```

    Resolves the host `ComponentActivity` automatically and uses it as the `MviStoreOwner` — same instance shared across the activity's lifetime, no setup required.

    For non-default scopes (a specific Fragment, a custom `NavBackStackEntry`, tests) wrap the subtree with `ProvideAppMviStoreOwner(owner) { … }` to override `LocalAppMviStoreOwner`.

=== "Koin"

    ```kotlin
    @Composable
    fun App() {
        ProvideAppMviStoreOwner {
            NavHost(...) { ... }
        }
    }

    @Composable
    fun AuthScreen(store: MviStore<AuthState, AuthIntention> = koinMviAppStore()) { ... }
    ```

    `ProvideAppMviStoreOwner` (placed above `NavHost`) captures the root owner; `koinMviAppStore` resolves through it.

Both `hiltMviStore` and `koinMviStore` also accept an explicit `owner` parameter when you need to scope to something other than the default.

## Running the Sample App

The `:app:hilt` module is a working counter demo:

```bash
./gradlew :app:hilt:installDebug
```
