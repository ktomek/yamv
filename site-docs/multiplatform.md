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
            implementation("io.github.ktomek:yamv-core:VERSION")
            implementation("io.github.ktomek:yamv:VERSION")
            implementation("io.github.ktomek:yamv-retainer:VERSION")
            implementation("io.github.ktomek:yamv-koin:VERSION")
        }
    }
}
```

### 2. Create your Koin module

Using the `mviStore {}` DSL — typed features are wrapped automatically via `add()`:

```kotlin
// commonMain
val counterModule = module {
    factoryOf(::IncrementFeature)
    factoryOf(::DecrementFeature)
    mviStore<CounterState, CounterIntention>(defaultState = CounterState()) {
        add(get<IncrementFeature>())   // .wrap() applied automatically
        add(get<DecrementFeature>())
    }
}
```

`mviStore<S, I>` takes two type params: the state type and the intention type. Use `Any` for `I` if you prefer untyped dispatch.

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
fun CounterScreen(
    store: MviStore<CounterState, CounterIntention> = koinMviStore(),
) {
    val state by store.state.collectAsStateWithLifecycle()
    // ...
}
```

### 4. Sharing state across navigation

By default `koinMviStore()` resolves through `LocalViewModelStoreOwner.current`, which Compose Multiplatform's `NavHost` overrides per back-stack entry — so every `composable<Route> { ... }` gets its own store. For app-lifetime state (auth, session, preferences) that needs to survive navigation:

```kotlin
@Composable
fun App() {
    ProvideAppMviStoreOwner {
        NavHost(...) {
            composable<AuthGraph.SignIn> { AuthScreen() }
            composable<HomeGraph.Root> { HomeScreen() }
        }
    }
}

@Composable
fun AuthScreen(
    // Same instance everywhere the app can reach it
    store: MviStore<AuthState, AuthIntention> = koinMviAppStore(),
) { ... }
```

Screens that don't call `koinMviAppStore()` continue to use the per-destination scope. You can also pass an explicit owner: `koinMviStore(myOwner)`.

### 5. iOS entry point

```kotlin
// iosMain
fun MainViewController() = ComposeUIViewController {
    App()
}
```
