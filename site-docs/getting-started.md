# Getting Started

## Prerequisites

- Android Studio Hedgehog or newer
- Kotlin 2.0+
- KSP plugin (for `@AutoState`/`@AutoFeature` code generation)

## Installation

### Step 1: Add JitPack repository

In your project's `settings.gradle.kts`:

```kotlin
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}
```

### Step 2: Add KSP plugin

In your app module's `build.gradle.kts`:

```kotlin
plugins {
    id("com.google.devtools.ksp") version "2.0.21-1.0.28"
}
```

### Step 3: Add dependencies

=== "Hilt (Android)"

    ```kotlin
    dependencies {
        implementation("com.github.ktomek.yamv:core:VERSION")
        implementation("com.github.ktomek.yamv:yamv:VERSION")
        implementation("com.github.ktomek.yamv:yamv-viewmodel:VERSION")
        implementation("com.github.ktomek.yamv:yamv-hilt:VERSION")
        ksp("com.github.ktomek.yamv:processor-hilt:VERSION")
    }
    ```

=== "Koin (Multiplatform)"

    ```kotlin
    // commonMain
    dependencies {
        implementation("com.github.ktomek.yamv:core:VERSION")
        implementation("com.github.ktomek.yamv:yamv:VERSION")
        implementation("com.github.ktomek.yamv:yamv-viewmodel:VERSION")
        implementation("com.github.ktomek.yamv:yamv-koin:VERSION")
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

`@AutoState` triggers KSP to generate `CounterStateStore` — a `@HiltViewModel` subclass of `MviViewModel<CounterState, Any>`.

### 2. Define Intentions

```kotlin
sealed class CounterIntention {
    data object Increment : CounterIntention()
    data object Decrement : CounterIntention()
    data class SetValue(val value: Int) : CounterIntention()
}
```

### 3. Write Features

Each feature handles one type of intention:

```kotlin
import com.ktomek.yamv.annotations.AutoFeature
import com.ktomek.yamv.feature.TypedFeature
import com.ktomek.yamv.core.StateOutcome
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@AutoFeature
class IncrementFeature : TypedFeature<CounterState, CounterIntention.Increment> {
    override fun invoke(
        intentions: Flow<CounterIntention.Increment>
    ): Flow<Outcome<CounterState>> =
        intentions.map { StateOutcome { state -> state.copy(count = state.count + 1) } }
}

@AutoFeature
class DecrementFeature : TypedFeature<CounterState, CounterIntention.Decrement> {
    override fun invoke(
        intentions: Flow<CounterIntention.Decrement>
    ): Flow<Outcome<CounterState>> =
        intentions.map { StateOutcome { state -> state.copy(count = state.count - 1) } }
}
```

### 4. Collect State in Compose

=== "Hilt"

    ```kotlin
    @Composable
    fun CounterScreen(vm: CounterStateStore = hiltMviStore()) {
        val state by vm.state.collectAsStateWithLifecycle()
        CounterContent(
            count = state.count,
            onIncrement = { vm.dispatch(CounterIntention.Increment) },
            onDecrement = { vm.dispatch(CounterIntention.Decrement) },
        )
    }
    ```

=== "Koin"

    ```kotlin
    @Composable
    fun CounterScreen(vm: CounterStateStore = koinMviStore()) {
        val state by vm.state.collectAsStateWithLifecycle()
        // same as above
    }
    ```

## Running the Sample App

The `:app:hilt` module is a working counter demo:

```bash
./gradlew :app:hilt:installDebug
```
