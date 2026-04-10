# Code Generation

YAMV uses [KSP (Kotlin Symbol Processing)](https://github.com/google/ksp) to generate boilerplate at compile time. You write the annotations; YAMV generates the retained store and DI wiring.

## @AutoState

Annotate a `State` subclass to generate a `*Store` retained store:

```kotlin
@AutoState
data class CounterState(val count: Int = 0) : State
```

**Generated** `CounterStateStore.kt`:
```kotlin
@HiltViewModel
class CounterStateStore @Inject constructor(
    features: Set<@JvmSuppressWildcards Feature<CounterState>>,
    @MviDispatcherConfig(CounterState::class)
    optionalDispatcherConfig: Optional<CoroutineDispatcherConfig>,
) : MviRetainedStore<CounterState, Any>() {
    override val dispatcherConfig: CoroutineDispatcherConfig =
        optionalDispatcherConfig.orElseGet { DefaultCoroutineDispatcherConfig() }
    override val store: MviStore<CounterState, Any> = MviRuntime(
        features = features,
        defaultState = CounterState(),
        dispatcherConfig = dispatcherConfig,
    )
}
```

The `defaultState` is inferred from the primary constructor's default values.

## @AutoFeature

Annotate a `Feature` implementation to include it in the Hilt multibinding set:

```kotlin
@AutoFeature
class IncrementFeature : TypedFeature<CounterState, CounterIntention.Increment> { ... }
```

**Generated** inside `CounterStateFeaturesModule.kt`:
```kotlin
@Module
@InstallIn(ViewModelComponent::class)
interface CounterStateFeaturesModule {
    @Binds @IntoSet @ViewModelScoped
    fun bindIncrementFeature(feature: IncrementFeature): Feature<CounterState>

    @BindsOptionalOf @MviDispatcherConfig(CounterState::class)
    fun bindOptionalDispatcherConfig(): CoroutineDispatcherConfig

    // @Multibinds declares the empty set so Dagger doesn't fail when no features are bound
    @Multibinds
    fun featureSet(): Set<Feature<CounterState>>

    companion object {
        // FunctionTypedFeature classes need .wrap() — goes in companion:
        @Provides @IntoSet @ViewModelScoped
        fun provideDecreaseFeature(it: DecreaseFeature): Feature<CounterState> = it.wrap()
    }
}
```

`@Binds` (abstract) methods go in the interface body; `@Provides` + `.wrap()` methods go in the companion object (Dagger constraint).

## Using @AutoFeature on Properties

If your feature is a lambda or builder result, annotate the property:

```kotlin
object CounterFeatures {
    @AutoFeature
    val incrementFeature = functionTypedFeature<CounterState, CounterIntention.Increment> { _ ->
        IncrementOutcome()
    }
}
```

## Processor Modules

| Module | Annotation | Output |
|--------|-----------|--------|
| `processor-hilt` | `@AutoState` | `{Name}Store` (`@HiltViewModel`) |
| `processor-hilt` | `@AutoFeature` | `{Name}FeaturesModule` (Hilt `@Module`) |

## Without Code Generation

If you prefer manual wiring (or use Koin without KSP), extend `MviRetainedStore` directly and call `.wrap()` on typed features:

```kotlin
class CounterViewModel(features: Set<Feature<CounterState>>)
    : MviRetainedStore<CounterState, Any>() {
    override val store = MviRuntime(
        features = features,
        defaultState = CounterState(),
    )
}

// Manual wiring — .wrap() required for TypedFeature / FunctionTypedFeature
val store = MviRuntime(
    features = setOf(
        IncrementFeature().wrap(),
        DecrementFeature().wrap(),
    ),
    defaultState = CounterState(),
)
```
