# YAMV — Yet Another MVI Framework

[![Build, Test](https://github.com/ktomek/yamv/actions/workflows/ci.yml/badge.svg)](https://github.com/ktomek/yamv/actions/workflows/ci.yml)
[![JitPack](https://jitpack.io/v/ktomek/yamv.svg)](https://jitpack.io/#ktomek/yamv)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

A Kotlin-first **MVI (Model-View-Intent)** framework for Android & Kotlin Multiplatform with compile-time code generation via KSP.

- **Reactive** — state management via Kotlin Coroutines & Flow
- **Type-safe** — sealed intentions, typed features, compile-time generation
- **Multiplatform** — Android (Hilt or Koin) + iOS (Compose Multiplatform + Koin)
- **Zero boilerplate** — `@AutoState` + `@AutoFeature` generate the retained store and Hilt/Koin modules

## Quick Start

### 1. Add JitPack repository

```kotlin
// settings.gradle.kts
dependencyResolutionManagement {
    repositories {
        maven { url = uri("https://jitpack.io") }
    }
}
```

### 2. Add dependencies

```kotlin
// build.gradle.kts (app module)
dependencies {
    // Core framework
    implementation("com.github.ktomek.yamv:yamv:VERSION")

    // Android ViewModel integration
    implementation("com.github.ktomek.yamv:yamv-retainer:VERSION")

    // Choose your DI integration:
    implementation("com.github.ktomek.yamv:yamv-hilt:VERSION")   // Hilt
    // or
    implementation("com.github.ktomek.yamv:yamv-koin:VERSION")   // Koin (multiplatform)

    // KSP code generation (Hilt only)
    ksp("com.github.ktomek.yamv:processor-hilt:VERSION")
}
```

Replace `VERSION` with the latest badge version above.

### 3. Define your state and intentions

```kotlin
@AutoState
data class CounterState(val count: Int = 0) : State

sealed class CounterIntention {
    data object Increment : CounterIntention()
    data object Decrement : CounterIntention()
}
```

### 4. Define reducers and features

Declare reducers as separate classes — decoupled from features, independently testable:

```kotlin
// Reducer — pure (S) -> S, tested without coroutines
class IncrementReducer : StateOutcome<CounterState> {
    override fun reduce(prevState: CounterState) =
        prevState.copy(count = prevState.count + 1)
}

// Feature — maps intentions to outcomes
@AutoFeature
class IncrementFeature : TypedFeature<CounterState, CounterIntention.Increment> {
    override fun invoke(intentions: Flow<CounterIntention.Increment>): Flow<Outcome<CounterState>> =
        intentions.map { IncrementReducer() }
}
```

### 5. Collect state in your Composable

```kotlin
@Composable
fun CounterScreen(vm: CounterStateStore = hiltMviStore()) {
    val state by vm.state.collectAsStateWithLifecycle()

    Column {
        Text("Count: ${state.count}")
        Button(onClick = { vm.dispatch(CounterIntention.Increment) }) {
            Text("+")
        }
    }
}
```

That's it. `@AutoState` generates `CounterStateStore` and `@AutoFeature` generates the Hilt bindings.

## Modules

| Module | Description | Platform |
|--------|-------------|----------|
| `core` | Marker interfaces & annotations (`State`, `Outcome`, `@AutoState`, `@AutoFeature`) | Pure Kotlin |
| `yamv` | Core runtime (`MviRuntime`, `MviStore`, `FeatureRouter`) | Kotlin Multiplatform |
| `yamv-retainer` | `MviRetainedStore` — lifecycle-retained ViewModel base | Android + iOS |
| `yamv-hilt` | `hiltMviStore()` Compose helper | Android |
| `yamv-koin` | `koinMviStore()` / `mviStore {}` DSL | Kotlin Multiplatform |
| `processor-core` | KSP utilities (DI-agnostic) | JVM |
| `processor-hilt` | Hilt KSP processor — generates `*Store` + `*FeaturesModule` | JVM |

## Documentation

Full documentation: **[ktomek.github.io/yamv](https://ktomek.github.io/yamv)**

- [Getting Started](https://ktomek.github.io/yamv/getting-started/)
- [Architecture](https://ktomek.github.io/yamv/architecture/)
- [Features Guide](https://ktomek.github.io/yamv/features/)
- [Code Generation](https://ktomek.github.io/yamv/code-generation/)
- [Multiplatform Setup](https://ktomek.github.io/yamv/multiplatform/)

## License

```
MIT License — see LICENSE file
```
