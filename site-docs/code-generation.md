# Code Generation

YAMV uses [KSP (Kotlin Symbol Processing)](https://github.com/google/ksp) to generate boilerplate at compile time. You write the annotations; YAMV generates the ViewModel and DI wiring.

## @AutoState

Annotate a `State` subclass to generate a `*Store` ViewModel:

```kotlin
@AutoState
data class CounterState(val count: Int = 0) : State
```

**Generated** `CounterStateStore.kt`:
```kotlin
@HiltViewModel
class CounterStateStore @Inject constructor(
    features: Set<@JvmSuppressWildcards Feature<CounterState>>,
    stateHandle: StateHandle,
) : MviViewModel<CounterState, Any>() {
    override val store: MviStore<CounterState, Any> = MviRuntime(
        features = features,
        defaultState = CounterState(),
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
    @Binds
    @IntoSet
    @ViewModelScoped
    fun bindIncrementFeature(feature: IncrementFeature): Feature<CounterState>

    companion object {
        @Provides
        @IntoSet
        @ViewModelScoped
        fun provideIncrementFeatureWrapped(feature: IncrementFeature): Feature<CounterState> =
            feature.wrap()
    }
}
```

## Using @AutoFeature on Properties

If your feature is a lambda or builder result, annotate the property:

```kotlin
object CounterFeatures {
    @AutoFeature
    val incrementFeature = functionTypedFeature<CounterState, CounterIntention.Increment> { _ ->
        StateOutcome { state -> state.copy(count = state.count + 1) }
    }
}
```

## Processor Modules

| Module | Annotation | Output |
|--------|-----------|--------|
| `processor-hilt` | `@AutoState` | `{Name}Store` (`@HiltViewModel`) |
| `processor-hilt` | `@AutoFeature` | `{Name}FeaturesModule` (Hilt `@Module`) |

## Without Code Generation

If you prefer manual wiring (or use Koin without KSP), extend `MviViewModel` directly:

```kotlin
class CounterViewModel(features: Set<Feature<CounterState>>)
    : MviViewModel<CounterState, Any>() {
    override val store = MviRuntime(
        features = features,
        defaultState = CounterState(),
    )
}
```
