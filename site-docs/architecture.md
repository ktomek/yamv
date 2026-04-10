# Architecture

## MVI Data Flow

YAMV implements strict unidirectional data flow:

``` mermaid
flowchart TB
    subgraph UI ["🖥️ Composable UI"]
        direction LR
        dispatch["store.dispatch(Intention)"]
        observe["store.state.collectAsStateWithLifecycle()"]
    end

    subgraph Store ["🏪 MviRetainedStore · generated *Store"]
        delegates["delegates to MviRuntime"]
    end

    subgraph Runtime ["⚙️ MviRuntime"]
        direction LR
        reducers["🔄 Reducers\n`scan() → StateFlow`\n**Main**"]
        effects["⚡ Effects\n`effectsFlow`\n**Main**"]
        redispatch["🔁 Re-dispatch\n`IntentionOutcome`\n**Main**"]
    end

    subgraph Router ["🚦 FeatureRouter"]
        intentionFlow["intentionFlow\n`SharedFlow‹Any›`"]
        outcomeFlow["outcomeFlow\n`SharedFlow‹Outcome‹S››`"]
    end

    subgraph Features ["🧩 Features · one coroutine each"]
        direction LR
        fa["Feature A"]
        fb["Feature B"]
        fc["Feature C"]
    end

    dispatch -->|"Intention"| Store
    Store --> intentionFlow
    intentionFlow --> fa & fb & fc
    fa & fb & fc --> outcomeFlow
    outcomeFlow --> reducers & effects & redispatch
    reducers -->|"StateFlow‹S›"| observe
    redispatch -.->|"re-dispatch"| intentionFlow

    style UI fill:#7c4dff,color:#fff,stroke:#7c4dff
    style Store fill:#651fff,color:#fff,stroke:#651fff
    style Runtime fill:#6200ea,color:#fff,stroke:#6200ea
    style Router fill:#aa00ff,color:#fff,stroke:#aa00ff
    style Features fill:#d500f9,color:#fff,stroke:#d500f9

    style dispatch fill:#b388ff,color:#000,stroke:#7c4dff
    style observe fill:#b388ff,color:#000,stroke:#7c4dff
    style delegates fill:#b39ddb,color:#000,stroke:#651fff
    style reducers fill:#ce93d8,color:#000,stroke:#6200ea
    style effects fill:#ce93d8,color:#000,stroke:#6200ea
    style redispatch fill:#ce93d8,color:#000,stroke:#6200ea
    style intentionFlow fill:#ea80fc,color:#000,stroke:#aa00ff
    style outcomeFlow fill:#ea80fc,color:#000,stroke:#aa00ff
    style fa fill:#f3e5f5,color:#000,stroke:#d500f9
    style fb fill:#f3e5f5,color:#000,stroke:#d500f9
    style fc fill:#f3e5f5,color:#000,stroke:#d500f9
```

## Outcome Types

``` mermaid
flowchart LR
    feature["🧩 Feature"] --> outcome{"Outcome‹S›"}
    outcome -->|StateOutcome| reducer["🔄 `(S) → S`\nreduces state via scan()"]
    outcome -->|EffectOutcome| effect["⚡ side effect\nnavigation, toast, etc."]
    outcome -->|IntentionOutcome| intention["🔁 re-dispatches\nanother intention"]

    style feature fill:#d500f9,color:#fff,stroke:#d500f9
    style outcome fill:#aa00ff,color:#fff,stroke:#aa00ff
    style reducer fill:#7c4dff,color:#fff,stroke:#7c4dff
    style effect fill:#651fff,color:#fff,stroke:#651fff
    style intention fill:#6200ea,color:#fff,stroke:#6200ea
```

Declare outcome subclasses as **standalone classes** — they are decoupled from features and testable in isolation. See [Features Guide](features.md#outcomes-as-separate-classes).

## Feature Abstraction Levels

Three levels — choose the simplest one that fits:

``` mermaid
flowchart LR
    subgraph simple ["Simplest"]
        func["**FunctionTypedFeature‹S, I›**\n`suspend (I) → Outcome‹S›`"]
    end
    subgraph typed ["Typed streaming"]
        tf["**TypedFeature‹S, I›**\n`(Flow‹I›) → Flow‹Outcome‹S››`"]
    end
    subgraph raw ["Low-level"]
        ff["**Feature.FlowFeature‹S›**\n`(Flow‹Any›) → Flow‹Outcome‹S››`"]
    end

    func -.->|".wrap()"| tf -.->|".wrap()"| ff

    style simple fill:#c5cae9,color:#000,stroke:#7c4dff
    style typed fill:#b39ddb,color:#000,stroke:#7c4dff
    style raw fill:#ce93d8,color:#000,stroke:#7c4dff
    style func fill:#e8eaf6,color:#000,stroke:#5c6bc0
    style tf fill:#ede7f6,color:#000,stroke:#7e57c2
    style ff fill:#f3e5f5,color:#000,stroke:#ab47bc
```

Use `.wrap()` to convert `TypedFeature` or `FunctionTypedFeature` to `Feature<S>` when wiring manually. With `@AutoFeature` (Hilt) or `mviStore {}` DSL (Koin), wrapping is automatic.

Use `functionTypedFeature<S, I> { }` builder for inline definitions.

## Dispatcher Architecture

`CoroutineDispatcherConfig` controls which dispatcher each part of the pipeline runs on:

``` mermaid
flowchart TB
    subgraph scope ["MviRuntime · CoroutineScope(SupervisorJob)"]
        direction TB
        subgraph main ["Dispatchers.Main"]
            r["🔄 Reducer collector\nscan → StateFlow"]
            e["⚡ Effect collector\nforward EffectOutcomes"]
            i["🔁 IntentionOutcome collector\nre-dispatch"]
        end
    end

    subgraph router ["FeatureRouter"]
        subgraph feat ["Dispatchers.Default · or per-feature"]
            direction LR
            f1["Feature A"]
            f2["Feature B"]
            f3["Feature C"]
        end
    end

    scope --> router

    style scope fill:#ede7f6,color:#000,stroke:#7c4dff
    style main fill:#d1c4e9,color:#000,stroke:#651fff
    style router fill:#f3e5f5,color:#000,stroke:#aa00ff
    style feat fill:#fce4ec,color:#000,stroke:#d500f9
    style r fill:#b39ddb,color:#000,stroke:#651fff
    style e fill:#b39ddb,color:#000,stroke:#651fff
    style i fill:#b39ddb,color:#000,stroke:#651fff
    style f1 fill:#f8bbd0,color:#000,stroke:#d500f9
    style f2 fill:#f8bbd0,color:#000,stroke:#d500f9
    style f3 fill:#f8bbd0,color:#000,stroke:#d500f9
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

!!! info "Subscription safety"
    `FeatureRouter` uses `CompletableDeferred` to ensure all features are subscribed to the intention `SharedFlow` before the first intention is dispatched. This prevents race conditions at startup.

## Lifecycle

``` mermaid
sequenceDiagram
    participant UI as 🖥️ UI
    participant Store as 🏪 MviRetainedStore
    participant Runtime as ⚙️ MviRuntime
    participant Router as 🚦 FeatureRouter
    participant F as 🧩 Features

    Note over Store,Runtime: Construction
    Store->>Runtime: create MviRuntime
    Runtime->>Router: initialize(scope, config)
    Router->>F: launch coroutine per feature
    F-->>Router: subscribe to intentionFlow
    Note over Router: CompletableDeferred ✅

    Note over UI,F: Dispatch
    UI->>Store: dispatch(intention)
    Store->>Runtime: scope.launch { dispatchIntention }
    Runtime->>Router: intentionFlow.emit(intention)
    Router->>F: intention broadcast
    F-->>Router: Outcome‹S›
    Router-->>Runtime: outcomeFlow
    Runtime-->>Runtime: scan → StateFlow
    Runtime-->>UI: StateFlow‹S› updated

    Note over UI,F: Cleanup
    UI->>Store: onCleared()
    Store->>Runtime: clear()
    Runtime->>Router: shutdown()
    Router->>F: cancel all jobs + scopes
```
