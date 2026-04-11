# Graph Report - .  (2026-04-11)

## Corpus Check
- 101 files · ~22,410 words
- Verdict: corpus is large enough that graph structure adds value.

## Summary
- 441 nodes · 357 edges · 84 communities detected
- Extraction: 100% EXTRACTED · 0% INFERRED · 0% AMBIGUOUS
- Token cost: 0 input · 0 output

## God Nodes (most connected - your core abstractions)
1. `FeaturesModuleGenerator` - 13 edges
2. `FeatureRouterTest` - 11 edges
3. `MviRuntimeLifecycleStressTest` - 10 edges
4. `YamvLoggerTest` - 9 edges
5. `FeatureRouter` - 8 edges
6. `KoinMviRetainedStoreDispatcherConfigTest` - 8 edges
7. `FeatureFilter` - 7 edges
8. `ViewModelGenerator` - 7 edges
9. `MviRuntimeTest` - 6 edges
10. `CoroutineDispatcherConfigTest` - 6 edges

## Surprising Connections (you probably didn't know these)
- None detected - all connections are within the same source files.

## Communities

### Community 0 - "Community 0"
Cohesion: 0.11
Nodes (6): ChainTo, Crash, Increment, LifecycleIntention, LifecycleState, MviRuntimeLifecycleStressTest

### Community 1 - "Community 1"
Cohesion: 0.13
Nodes (6): FastAction, Increment, MviRuntimeStressTest, SlowAction, StressIntention, StressState

### Community 2 - "Community 2"
Cohesion: 0.13
Nodes (4): InspectableMviRetainedStore, KoinMviRetainedStoreDispatcherConfigTest, TestState, TrackingCoroutineDispatcherConfig

### Community 3 - "Community 3"
Cohesion: 0.14
Nodes (1): FeaturesModuleGenerator

### Community 4 - "Community 4"
Cohesion: 0.14
Nodes (2): FeatureRouterTest, TestState

### Community 5 - "Community 5"
Cohesion: 0.14
Nodes (3): DefaultMviRegistry, MutableMviRegistry, MviRegistry

### Community 6 - "Community 6"
Cohesion: 0.18
Nodes (2): EmptyStateHandle, StateHandle

### Community 7 - "Community 7"
Cohesion: 0.2
Nodes (1): ViewModelGenerator

### Community 8 - "Community 8"
Cohesion: 0.2
Nodes (3): MviRuntimeTest, TestState, TestStateImpl

### Community 9 - "Community 9"
Cohesion: 0.2
Nodes (4): ActionTestState, ActionTypedFeatureWrapperTest, DoAction, OtherAction

### Community 10 - "Community 10"
Cohesion: 0.2
Nodes (1): YamvLoggerTest

### Community 11 - "Community 11"
Cohesion: 0.22
Nodes (1): FeatureRouter

### Community 12 - "Community 12"
Cohesion: 0.22
Nodes (2): CoroutineDispatcherConfig, DefaultCoroutineDispatcherConfig

### Community 13 - "Community 13"
Cohesion: 0.22
Nodes (4): MviRetainedStoreTest, TestMviRetainedStore, TestState, TestStateImpl

### Community 14 - "Community 14"
Cohesion: 0.25
Nodes (1): FeatureFilter

### Community 15 - "Community 15"
Cohesion: 0.25
Nodes (3): MviRegistryTest, TestState1, TestState1Impl

### Community 16 - "Community 16"
Cohesion: 0.25
Nodes (2): LogTestState, MviRuntimeLoggingTest

### Community 17 - "Community 17"
Cohesion: 0.25
Nodes (2): DefaultFeatureScopeTest, ScopedFeatureState

### Community 18 - "Community 18"
Cohesion: 0.25
Nodes (2): MviRuntimeBuilder, MviRuntimeConfig

### Community 19 - "Community 19"
Cohesion: 0.29
Nodes (5): EffectOutcome, IntentionOutcome, Outcome, Reducer, StateOutcome

### Community 20 - "Community 20"
Cohesion: 0.29
Nodes (3): AutoDecreaseCounterOutcome, AutoIncreaseOutcome, ChangeCounterOutcome

### Community 21 - "Community 21"
Cohesion: 0.29
Nodes (6): AutoDecreaseCounterIntention, AutoIncreaseCounterIntention, DecreaseCounterIntention, IncreaseCounterIntention, StopAutoDecreaseCounterIntention, StopAutoIncreaseCounterIntention

### Community 22 - "Community 22"
Cohesion: 0.29
Nodes (4): ComposeView, ContentView, UIViewControllerRepresentable, View

### Community 23 - "Community 23"
Cohesion: 0.29
Nodes (2): DispatcherTestState, FeatureRouterDispatcherTest

### Community 24 - "Community 24"
Cohesion: 0.29
Nodes (2): FeatureRouterLoggingTest, LoggingTestState

### Community 25 - "Community 25"
Cohesion: 0.29
Nodes (1): CoroutineDispatcherConfigTest

### Community 26 - "Community 26"
Cohesion: 0.33
Nodes (1): DispatcherConfigModuleGenerator

### Community 27 - "Community 27"
Cohesion: 0.33
Nodes (2): ConcurrencyTestState, FunctionTypedFeatureConcurrencyTest

### Community 28 - "Community 28"
Cohesion: 0.33
Nodes (3): Feature, FlowFeature, FlowUnitFeature

### Community 29 - "Community 29"
Cohesion: 0.33
Nodes (1): IosStateHandle

### Community 30 - "Community 30"
Cohesion: 0.33
Nodes (1): AndroidStateHandle

### Community 31 - "Community 31"
Cohesion: 0.4
Nodes (2): YamvProcessor, YamvProcessorProvider

### Community 32 - "Community 32"
Cohesion: 0.4
Nodes (1): IntentionRouter

### Community 33 - "Community 33"
Cohesion: 0.4
Nodes (1): MviRuntime

### Community 34 - "Community 34"
Cohesion: 0.4
Nodes (3): ErrorSource, MviErrorContext, MviExceptionHandler

### Community 35 - "Community 35"
Cohesion: 0.4
Nodes (3): TypedFeature, TypedFeatureHolder, TypedUnitFeatureHolder

### Community 36 - "Community 36"
Cohesion: 0.4
Nodes (1): MviRetainedStore

### Community 37 - "Community 37"
Cohesion: 0.4
Nodes (1): FeatureRegistrar

### Community 38 - "Community 38"
Cohesion: 0.4
Nodes (1): KoinMviRetainedStore

### Community 39 - "Community 39"
Cohesion: 0.5
Nodes (3): FeatureFqns, HiltClassNames, YamvClassNames

### Community 40 - "Community 40"
Cohesion: 0.5
Nodes (1): AutoDecreaseFeature

### Community 41 - "Community 41"
Cohesion: 0.5
Nodes (1): AutoIncreaseFeature

### Community 42 - "Community 42"
Cohesion: 0.5
Nodes (1): KoinApp

### Community 43 - "Community 43"
Cohesion: 0.5
Nodes (1): YamvApp

### Community 44 - "Community 44"
Cohesion: 0.5
Nodes (2): App, iOSApp

### Community 45 - "Community 45"
Cohesion: 0.5
Nodes (1): HasFeatureDispatcherTest

### Community 46 - "Community 46"
Cohesion: 0.5
Nodes (1): MviStore

### Community 47 - "Community 47"
Cohesion: 0.5
Nodes (1): Yamv

### Community 48 - "Community 48"
Cohesion: 0.5
Nodes (1): AutoFeatureDiscovery

### Community 49 - "Community 49"
Cohesion: 0.67
Nodes (1): YamvStateHandleModule

### Community 50 - "Community 50"
Cohesion: 0.67
Nodes (1): YamvDispatcherModule

### Community 51 - "Community 51"
Cohesion: 0.67
Nodes (2): AutoState, NoDefaultState

### Community 52 - "Community 52"
Cohesion: 0.67
Nodes (0): 

### Community 53 - "Community 53"
Cohesion: 0.67
Nodes (1): DecreaseFeature

### Community 54 - "Community 54"
Cohesion: 0.67
Nodes (1): MainActivity

### Community 55 - "Community 55"
Cohesion: 0.67
Nodes (1): FunctionTypedFeature

### Community 56 - "Community 56"
Cohesion: 0.67
Nodes (1): ActionTypedFeature

### Community 57 - "Community 57"
Cohesion: 0.67
Nodes (0): 

### Community 58 - "Community 58"
Cohesion: 0.67
Nodes (1): YamvLogger

### Community 59 - "Community 59"
Cohesion: 0.67
Nodes (1): AutoStateDiscovery

### Community 60 - "Community 60"
Cohesion: 0.67
Nodes (1): AutoDispatcherConfigDiscovery

### Community 61 - "Community 61"
Cohesion: 1.0
Nodes (0): 

### Community 62 - "Community 62"
Cohesion: 1.0
Nodes (1): MviDispatcherConfig

### Community 63 - "Community 63"
Cohesion: 1.0
Nodes (1): State

### Community 64 - "Community 64"
Cohesion: 1.0
Nodes (1): AutoDispatcherConfig

### Community 65 - "Community 65"
Cohesion: 1.0
Nodes (0): 

### Community 66 - "Community 66"
Cohesion: 1.0
Nodes (1): CounterState

### Community 67 - "Community 67"
Cohesion: 1.0
Nodes (0): 

### Community 68 - "Community 68"
Cohesion: 1.0
Nodes (0): 

### Community 69 - "Community 69"
Cohesion: 1.0
Nodes (0): 

### Community 70 - "Community 70"
Cohesion: 1.0
Nodes (0): 

### Community 71 - "Community 71"
Cohesion: 1.0
Nodes (1): HasFeatureScope

### Community 72 - "Community 72"
Cohesion: 1.0
Nodes (1): HasFeatureDispatcher

### Community 73 - "Community 73"
Cohesion: 1.0
Nodes (1): DefaultFeatureScope

### Community 74 - "Community 74"
Cohesion: 1.0
Nodes (0): 

### Community 75 - "Community 75"
Cohesion: 1.0
Nodes (1): YamvLogLevel

### Community 76 - "Community 76"
Cohesion: 1.0
Nodes (0): 

### Community 77 - "Community 77"
Cohesion: 1.0
Nodes (0): 

### Community 78 - "Community 78"
Cohesion: 1.0
Nodes (0): 

### Community 79 - "Community 79"
Cohesion: 1.0
Nodes (0): 

### Community 80 - "Community 80"
Cohesion: 1.0
Nodes (0): 

### Community 81 - "Community 81"
Cohesion: 1.0
Nodes (0): 

### Community 82 - "Community 82"
Cohesion: 1.0
Nodes (0): 

### Community 83 - "Community 83"
Cohesion: 1.0
Nodes (0): 

## Knowledge Gaps
- **58 isolated node(s):** `HiltClassNames`, `YamvClassNames`, `FeatureFqns`, `MviDispatcherConfig`, `State` (+53 more)
  These have ≤1 connection - possible missing edges or undocumented components.
- **Thin community `Community 61`** (2 nodes): `HiltMviStore.kt`, `hiltMviStore()`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Community 62`** (2 nodes): `MviDispatcherConfig.kt`, `MviDispatcherConfig`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Community 63`** (2 nodes): `State.kt`, `State`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Community 64`** (2 nodes): `AutoDispatcherConfig.kt`, `AutoDispatcherConfig`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Community 65`** (2 nodes): `CounterScreen.kt`, `CounterScreen()`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Community 66`** (2 nodes): `CounterState.kt`, `CounterState`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Community 67`** (2 nodes): `FlowExtensions.kt`, `takeUntilSignal()`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Community 68`** (2 nodes): `Theme.kt`, `YamvTheme()`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Community 69`** (2 nodes): `CounterContent.kt`, `CounterContent()`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Community 70`** (2 nodes): `TypedFeatureWrapper.kt`, `wrap()`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Community 71`** (2 nodes): `HasFeatureScope.kt`, `HasFeatureScope`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Community 72`** (2 nodes): `HasFeatureDispatcher.kt`, `HasFeatureDispatcher`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Community 73`** (2 nodes): `DefaultFeatureScope.kt`, `DefaultFeatureScope`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Community 74`** (2 nodes): `ActionTypedFeatureWrapper.kt`, `wrap()`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Community 75`** (2 nodes): `YamvLogLevel.kt`, `YamvLogLevel`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Community 76`** (1 nodes): `build.gradle.kts`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Community 77`** (1 nodes): `OpenForTesting.kt`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Community 78`** (1 nodes): `AutoFeature.kt`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Community 79`** (1 nodes): `CounterFeaturesModule.kt`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Community 80`** (1 nodes): `IncreaseFeature.kt`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Community 81`** (1 nodes): `Shape.kt`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Community 82`** (1 nodes): `Color.kt`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Community 83`** (1 nodes): `Type.kt`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **What connects `HiltClassNames`, `YamvClassNames`, `FeatureFqns` to the rest of the system?**
  _58 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `Community 0` be split into smaller, more focused modules?**
  _Cohesion score 0.11 - nodes in this community are weakly interconnected._
- **Should `Community 1` be split into smaller, more focused modules?**
  _Cohesion score 0.13 - nodes in this community are weakly interconnected._
- **Should `Community 2` be split into smaller, more focused modules?**
  _Cohesion score 0.13 - nodes in this community are weakly interconnected._
- **Should `Community 3` be split into smaller, more focused modules?**
  _Cohesion score 0.14 - nodes in this community are weakly interconnected._
- **Should `Community 4` be split into smaller, more focused modules?**
  _Cohesion score 0.14 - nodes in this community are weakly interconnected._
- **Should `Community 5` be split into smaller, more focused modules?**
  _Cohesion score 0.14 - nodes in this community are weakly interconnected._