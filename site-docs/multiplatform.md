# Multiplatform Setup

YAMV supports Kotlin Multiplatform for sharing state logic between Android and iOS via Compose Multiplatform.

## Supported Targets

| Target | Status |
|--------|--------|
| Android (Hilt) | Stable |
| Android (Koin) | Stable |
| iOS (Koin + Compose Multiplatform) | Stable |

## iOS + Koin Setup

### 1. Add Koin dependencies to commonMain

```kotlin
// shared/build.gradle.kts
kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation("com.github.ktomek.yamv:yamv:VERSION")
            implementation("com.github.ktomek.yamv:yamv-retainer:VERSION")
            implementation("com.github.ktomek.yamv:yamv-koin:VERSION")
        }
    }
}
```

### 2. Create your Koin module

Using the `mviStore {}` DSL — features are wrapped automatically:

```kotlin
// commonMain
val counterModule = module {
    factoryOf(::IncrementFeature)
    factoryOf(::DecrementFeature)
    mviStore(defaultState = CounterState()) {
        feature { get<IncrementFeature>() }
        feature { get<DecrementFeature>() }
    }
}
```

Or manual wiring with explicit `.wrap()`:

```kotlin
val counterModule = module {
    factory<MviStore<CounterState, Any>> {
        MviRuntime(
            features = setOf(
                IncrementFeature().wrap(),
                DecrementFeature().wrap(),
            ),
            defaultState = CounterState(),
        )
    }
}
```

### 3. Use in Compose

```kotlin
// commonMain
@Composable
fun CounterScreen(vm: KoinMviRetainedStore<CounterState> = koinMviStore()) {
    val state by vm.state.collectAsStateWithLifecycle()
    // ...
}
```

### 4. iOS entry point

```kotlin
// iosMain
fun MainViewController() = ComposeUIViewController {
    App()
}
```
