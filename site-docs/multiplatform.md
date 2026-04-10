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
            implementation("com.github.ktomek.yamv:yamv-viewmodel:VERSION")
            implementation("com.github.ktomek.yamv:yamv-koin:VERSION")
        }
    }
}
```

### 2. Create your Koin module

```kotlin
// commonMain
val counterModule = module {
    factory { CounterState() }
    factoryOf(::IncrementFeature)
    factoryOf(::DecrementFeature)
    factory<MviStore<CounterState, Any>> {
        MviRuntime(
            features = setOf(get<IncrementFeature>().wrap(), get<DecrementFeature>().wrap()),
            defaultState = CounterState(),
        )
    }
}
```

### 3. Use in Compose

```kotlin
// commonMain
@Composable
fun CounterScreen(vm: CounterStateStore = koinMviStore()) {
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

## StateHandle on iOS

`MviViewModel` uses `StateHandle` for saved state restoration. On iOS, `IosStateHandle` provides a no-op implementation (iOS ViewModel lifecycle manages this differently).
