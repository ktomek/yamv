# Architecture

## MVI Data Flow

YAMV implements strict unidirectional data flow:

```
┌──────────────────────────────────────────────────────────┐
│                       Composable UI                      │
│   val state by store.state.collectAsStateWithLifecycle() │
│   store.dispatch(CounterIntention.Increment)             │
└─────────────────────────┬──────────────────▲─────────────┘
                          │ Intention        │ StateFlow<S>
                          ▼                  │
┌──────────────────────────────────────────────────────────┐
│             MviRetainedStore (generated *Store)           │
│   delegates to MviRuntime                                │
└─────────────────────────┬──────────────────▲─────────────┘
                          │                  │
                          ▼                  │
┌──────────────────────────────────────────────────────────┐
│                       MviRuntime                         │
│   ┌──────────────┐  ┌──────────────┐  ┌───────────────┐ │
│   │  Reducers    │  │   Effects    │  │  Intentions   │ │
│   │  (Main)      │  │   (Main)     │  │  (Main)       │ │
│   └──────┬───────┘  └──────┬───────┘  └───────┬───────┘ │
└──────────│─────────────────│──────────────────│──────────┘
           │                 │                  │
           └─────────────────▼──────────────────┘
                      SharedFlow<Outcome<S>>
                             │
                             ▼
┌──────────────────────────────────────────────────────────┐
│                      FeatureRouter                       │
│   intentionFlow: MutableSharedFlow<Any>                  │
│   outcomeFlow:   MutableSharedFlow<Outcome<S>>           │
└─────────────────────────┬────────────────────────────────┘
                          │ (one coroutine per feature)
               ┌──────────┼──────────┐
               ▼          ▼          ▼
          Feature A   Feature B  Feature C
        (Default dispatcher, or per-feature)
```

## Key Types

### Outcomes

Every feature produces `Outcome<S>` values. There are three kinds:

| Type | Interface | Effect |
|------|-----------|--------|
| `StateOutcome<S>` | `fun interface (S) -> S` | Reduces state via `scan()` |
| `EffectOutcome<S>` | marker | Emitted on `effects: Flow<EffectOutcome<S>>` |
| `IntentionOutcome<S>` | has `val intention: Any` | Re-dispatched as a new intention |

Declare outcome subclasses as **standalone classes** — they are decoupled from features and testable in isolation. See [Features Guide](features.md#outcomes-as-separate-classes).

### Features

Three abstraction levels (choose the simplest one that fits):

| Type | API | When to use |
|------|-----|-------------|
| `Feature.FlowFeature<S>` | `(Flow<Any>) -> Flow<Outcome<S>>` | Low-level; handles multiple intention types |
| `TypedFeature<S, I>` | `(Flow<I>) -> Flow<Outcome<S>>` | Typed; one feature per intention type |
| `FunctionTypedFeature<S, I>` | `suspend (I) -> Outcome<S>` | Simplest; one outcome per intention |

Use `.wrap()` to convert `TypedFeature` or `FunctionTypedFeature` to `Feature<S>` when wiring manually. With `@AutoFeature` (Hilt) or `mviStore {}` DSL (Koin), wrapping is automatic.

Use `functionTypedFeature<S, I> { }` builder for inline definitions.

## Dispatcher Architecture

`CoroutineDispatcherConfig` controls which dispatcher each part of the pipeline runs on:

```
MviRuntime owns CoroutineScope(SupervisorJob() + reducerDispatcher)
│
├── launch(reducerDispatcher)    ── Reducer collector: scan outcomes → update StateFlow
├── launch(reducerDispatcher)    ── Effect collector: forward EffectOutcomes
├── launch(intentionDispatcher)  ── IntentionOutcome collector: re-dispatch
│
└── FeatureRouter initialized with this scope
    │
    ├── launch(featureDispatcher) ── Feature A coroutine
    ├── launch(featureDispatcher) ── Feature B coroutine
    └── launch(featureDispatcher) ── Feature C coroutine
```

Default dispatchers (`DefaultCoroutineDispatcherConfig`):

| Operation | Dispatcher | Rationale |
|-----------|-----------|-----------|
| Intention dispatch | `Main` | UI-safe, async launch |
| Reducers (`scan`) | `Main` | State mutations must be serialized |
| Effects | `Main` | Observers expect UI thread |
| Features | `Default` | Non-blocking, concurrent processing |

### Customizing Dispatchers

Override `CoroutineDispatcherConfig` to control dispatcher assignment per intention or feature:

```kotlin
class CustomDispatcherConfig : CoroutineDispatcherConfig {
    // Single-threaded dispatcher for sequential feature processing
    private val singleThread = Dispatchers.Default.limitedParallelism(1)

    override fun provideIntentionDispatcher(intention: Any?) = Dispatchers.Main
    override fun provideReducerDispatcher() = Dispatchers.Main
    override fun provideFeatureDispatcher(feature: Any) = when (feature) {
        is NetworkFeature -> Dispatchers.IO
        else -> singleThread
    }
}
```

**Per-feature dispatcher:** Features can implement `HasFeatureDispatcher` to declare their own dispatcher, which takes precedence over `CoroutineDispatcherConfig`:

```kotlin
class NetworkFeature : TypedFeature<MyState, FetchData>, HasFeatureDispatcher {
    override val featureDispatcher = Dispatchers.IO
    // ...
}
```

Features with `HasFeatureScope` (via `DefaultFeatureScope`) automatically expose the scope's dispatcher.

**Per-state config (Hilt):** Use the `@MviDispatcherConfig` qualifier to inject a custom config per state type:

```kotlin
@Module
@InstallIn(ViewModelComponent::class)
object MyDispatcherModule {
    @Provides @MviDispatcherConfig(CounterState::class)
    fun provide(): CoroutineDispatcherConfig = CustomDispatcherConfig()
}
```

**Key invariant:** `FeatureRouter` uses `CompletableDeferred` to ensure all features are subscribed to the intention `SharedFlow` before the first intention is dispatched. This prevents race conditions at startup.

## Lifecycle

```
MviRetainedStore created (ViewModel)
  → MviRuntime created (in constructor)
    → FeatureRouter.initialize() called
      → Feature coroutines launched (one per feature)
      → All features subscribe to intentionFlow
      → CompletableDeferred completed
  → Ready to dispatch

store.dispatch(intention)
  → scope.launch(intentionDispatcher) { intentionRouter.dispatchIntention(intention) }
    → subscribed.await() (returns immediately after init)
    → intentionFlow.emit(intention)
      → Features receive intention, produce Outcomes
      → Outcomes flow to MviRuntime collectors

MviRetainedStore.onCleared()
  → store.clear()
    → scope.cancel() (cancels all 3 runtime collectors + all feature coroutines)
    → FeatureRouter.shutdown() (cancels feature jobs + feature scopes)
```
