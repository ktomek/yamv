# Architecture

## MVI Data Flow

YAMV implements strict unidirectional data flow:

```
┌─────────────────────────────────────────────────────┐
│                   Composable UI                      │
│   val state by vm.state.collectAsStateWithLifecycle()│
│   vm.dispatch(CounterIntention.Increment)            │
└────────────────────┬────────────────────▲────────────┘
                     │ Intention           │ StateFlow<S>
                     ▼                    │
┌─────────────────────────────────────────────────────┐
│              MviViewModel (generated *Store)         │
│   delegates to MviRuntime                            │
└────────────────────┬────────────────────▲────────────┘
                     │                    │
                     ▼                    │
┌─────────────────────────────────────────────────────┐
│                    MviRuntime                        │
│  ┌──────────────┐  ┌──────────────┐  ┌───────────┐  │
│  │  Reducers    │  │   Effects    │  │Intentions │  │
│  │  (Main)      │  │   (Main)     │  │(Main)     │  │
│  └──────┬───────┘  └──────┬───────┘  └─────┬─────┘  │
└─────────│─────────────────│────────────────│─────────┘
          │                 │                │
          └─────────────────▼────────────────┘
                     SharedFlow<Outcome<S>>
                            │
                            ▼
┌─────────────────────────────────────────────────────┐
│                   FeatureRouter                      │
│   intentionFlow: MutableSharedFlow<Any>              │
│   outcomeFlow:   MutableSharedFlow<Outcome<S>>       │
└────────────────────┬────────────────────────────────┘
                     │ (one coroutine per feature)
          ┌──────────┼──────────┐
          ▼          ▼          ▼
     Feature A   Feature B  Feature C
   (Default dispatcher)
```

## Key Types

### Outcomes

Every feature produces `Outcome<S>` values. There are three kinds:

| Type | Interface | Effect |
|------|-----------|--------|
| `StateOutcome<S>` | `fun interface (S) -> S` | Reduces state via `scan()` |
| `EffectOutcome<S>` | marker | Emitted on `effects: Flow<EffectOutcome<S>>` |
| `IntentionOutcome<S>` | has `val intention: Any` | Re-dispatched as a new intention |

### Features

Three abstraction levels (choose the simplest one that fits):

| Type | API | When to use |
|------|-----|-------------|
| `Feature.FlowFeature<S>` | `(Flow<Any>) -> Flow<Outcome<S>>` | Low-level; handles multiple intention types |
| `TypedFeature<S, I>` | `(Flow<I>) -> Flow<Outcome<S>>` | Typed; one feature per intention type |
| `FunctionTypedFeature<S, I>` | `suspend (I) -> Outcome<S>` | Simplest; one outcome per intention |

Use `.wrap()` to convert `TypedFeature` or `FunctionTypedFeature` to `Feature<S>`.
Use `functionTypedFeature<S, I> { }` builder for inline definitions.

## Coroutine Architecture

```
MviRuntime owns CoroutineScope(SupervisorJob() + Main)
│
├── launch(Main) ── Reducer collector: scan outcomes → update StateFlow
├── launch(Main) ── Effect collector: forward EffectOutcomes to effectsFlow
├── launch(Main) ── IntentionOutcome collector: re-dispatch intentions
│
└── FeatureRouter initialized with this scope
    │
    ├── launch(Default) ── Feature A coroutine
    ├── launch(Default) ── Feature B coroutine
    └── launch(Default) ── Feature C coroutine
```

**Key invariant:** `FeatureRouter` uses `CompletableDeferred` to ensure all features are subscribed to the intention `SharedFlow` before the first intention is dispatched. This prevents race conditions at startup.

## Lifecycle

```
MviViewModel created
  → MviRuntime created (in constructor)
    → FeatureRouter.initialize() called
      → Feature coroutines launched
      → All features subscribe to intentionFlow
      → CompletableDeferred completed
  → Ready to dispatch

vm.dispatch(intention)
  → scope.launch(Main) { intentionRouter.dispatchIntention(intention) }
    → subscribed.await() (returns immediately after init)
    → intentionFlow.emit(intention)
      → Features receive intention, produce Outcomes
      → Outcomes flow to MviRuntime collectors

MviViewModel.onCleared()
  → store.clear()
    → scope.cancel() (cancels all 3 runtime collectors + all feature coroutines)
    → FeatureRouter.shutdown() (cancels feature jobs)
```
