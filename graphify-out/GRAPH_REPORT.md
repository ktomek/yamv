# Graph Report - .  (2026-04-11)

## Corpus Check
- Corpus is ~27,363 words - fits in a single context window. You may not need a graph.

## Summary
- 481 nodes · 398 edges · 93 communities detected
- Extraction: 99% EXTRACTED · 1% INFERRED · 0% AMBIGUOUS · INFERRED: 5 edges (avg confidence: 0.8)
- Token cost: 0 input · 0 output

## God Nodes (most connected - your core abstractions)
1. `FeaturesModuleGenerator` - 13 edges
2. `FeatureRouterTest` - 11 edges
3. `YAMV Framework` - 10 edges
4. `MviRuntime` - 10 edges
5. `YamvLoggerTest` - 9 edges
6. `FeatureRouter` - 8 edges
7. `KoinMviRetainedStoreDispatcherConfigTest` - 8 edges
8. `FeatureFilter` - 7 edges
9. `ViewModelGenerator` - 7 edges
10. `MviRuntimeTest` - 6 edges

## Surprising Connections (you probably didn't know these)
- `Site Documentation Home` --semantically_similar_to--> `YAMV Framework`  [INFERRED] [semantically similar]
  site-docs/index.md → README.md
- `Outcomes as Standalone Classes Pattern` --semantically_similar_to--> `Distributed Reducers Design`  [INFERRED] [semantically similar]
  site-docs/getting-started.md → README.md
- `typedFeatureWithScope Builder` --semantically_similar_to--> `FlowFeature (low-level)`  [INFERRED] [semantically similar]
  docs/superpowers/plans/2026-04-09-framework-logging-feature-scope-concurrency.md → site-docs/features.md
- `README Documentation Plan` --references--> `YAMV Framework`  [EXTRACTED]
  docs/superpowers/plans/2026-04-09-documentation-readme-pages.md → README.md
- `Unidirectional Data Flow` --references--> `MviRuntime`  [EXTRACTED]
  site-docs/architecture.md → CLAUDE.md

## Hyperedges (group relationships)
- **MVI Data Flow Pipeline** — claude_featurerouter, claude_mviruntime, claude_feature, claude_stateoutcome, claude_effectoutcome, claude_intentionoutcome [EXTRACTED 1.00]
- **Feature Abstraction Hierarchy** — claude_feature, claude_typedfeature, claude_functiontypedfeature, features_flowfeature, features_wrap_method [EXTRACTED 1.00]
- **KSP Code Generation Annotation Set** — claude_autostate, claude_autofeature, architecture_autodispatcherconfig, claude_ksp, readme_module_processor_hilt [EXTRACTED 1.00]

## Communities

### Community 0 - "Architecture & Annotations"
Cohesion: 0.1
Nodes (28): Store Lifecycle (init-dispatch-clear), Rationale: CompletableDeferred Subscription Safety, Unidirectional Data Flow, @AutoFeature Annotation, @AutoState Annotation, CompletableDeferred Subscription Safety, EffectOutcome, Feature (sealed) (+20 more)

### Community 1 - "MviRuntime Stress Tests"
Cohesion: 0.13
Nodes (6): FastAction, Increment, MviRuntimeStressTest, SlowAction, StressIntention, StressState

### Community 2 - "Koin Dispatcher Config Tests"
Cohesion: 0.13
Nodes (4): InspectableMviRetainedStore, KoinMviRetainedStoreDispatcherConfigTest, TestState, TrackingCoroutineDispatcherConfig

### Community 3 - "Documentation & Getting Started"
Cohesion: 0.13
Nodes (15): Hilt Setup Guide, Koin Setup Guide, Site Documentation Home, iOS App Example, Kotlin Multiplatform Support, README Documentation Plan, Module: core, Module: processor-core (+7 more)

### Community 4 - "Features Module Generator"
Cohesion: 0.14
Nodes (1): FeaturesModuleGenerator

### Community 5 - "FeatureRouter Tests"
Cohesion: 0.14
Nodes (2): FeatureRouterTest, TestState

### Community 6 - "MviRegistry"
Cohesion: 0.14
Nodes (3): DefaultMviRegistry, MutableMviRegistry, MviRegistry

### Community 7 - "StateHandle"
Cohesion: 0.18
Nodes (2): EmptyStateHandle, StateHandle

### Community 8 - "ViewModel Generator"
Cohesion: 0.2
Nodes (1): ViewModelGenerator

### Community 9 - "MviRuntime Tests"
Cohesion: 0.2
Nodes (3): MviRuntimeTest, TestState, TestStateImpl

### Community 10 - "ActionTypedFeature Tests"
Cohesion: 0.2
Nodes (4): ActionTestState, ActionTypedFeatureWrapperTest, DoAction, OtherAction

### Community 11 - "Logger Tests"
Cohesion: 0.2
Nodes (1): YamvLoggerTest

### Community 12 - "FeatureRouter"
Cohesion: 0.22
Nodes (1): FeatureRouter

### Community 13 - "CoroutineDispatcherConfig"
Cohesion: 0.22
Nodes (2): CoroutineDispatcherConfig, DefaultCoroutineDispatcherConfig

### Community 14 - "MviRetainedStore Tests"
Cohesion: 0.22
Nodes (4): MviRetainedStoreTest, TestMviRetainedStore, TestState, TestStateImpl

### Community 15 - "FeatureFilter"
Cohesion: 0.25
Nodes (1): FeatureFilter

### Community 16 - "MviRegistry Tests"
Cohesion: 0.25
Nodes (3): MviRegistryTest, TestState1, TestState1Impl

### Community 17 - "MviRuntime Logging Tests"
Cohesion: 0.25
Nodes (2): LogTestState, MviRuntimeLoggingTest

### Community 18 - "DefaultFeatureScope Tests"
Cohesion: 0.25
Nodes (2): DefaultFeatureScopeTest, ScopedFeatureState

### Community 19 - "MviRuntime Config Builder"
Cohesion: 0.25
Nodes (2): MviRuntimeBuilder, MviRuntimeConfig

### Community 20 - "Outcome Hierarchy"
Cohesion: 0.29
Nodes (5): EffectOutcome, IntentionOutcome, Outcome, Reducer, StateOutcome

### Community 21 - "Counter Outcomes"
Cohesion: 0.29
Nodes (3): AutoDecreaseCounterOutcome, AutoIncreaseOutcome, ChangeCounterOutcome

### Community 22 - "Counter Intentions"
Cohesion: 0.29
Nodes (6): AutoDecreaseCounterIntention, AutoIncreaseCounterIntention, DecreaseCounterIntention, IncreaseCounterIntention, StopAutoDecreaseCounterIntention, StopAutoIncreaseCounterIntention

### Community 23 - "iOS ContentView"
Cohesion: 0.29
Nodes (4): ComposeView, ContentView, UIViewControllerRepresentable, View

### Community 24 - "FeatureRouter Dispatcher Tests"
Cohesion: 0.29
Nodes (2): DispatcherTestState, FeatureRouterDispatcherTest

### Community 25 - "FeatureRouter Logging Tests"
Cohesion: 0.29
Nodes (2): FeatureRouterLoggingTest, LoggingTestState

### Community 26 - "DispatcherConfig Tests"
Cohesion: 0.29
Nodes (1): CoroutineDispatcherConfigTest

### Community 27 - "Dispatcher Architecture"
Cohesion: 0.29
Nodes (7): @AutoDispatcherConfig Annotation, Dispatcher Architecture, HasFeatureScope / HasFeatureDispatcher, Rationale: Default Dispatcher Assignment, CoroutineDispatcherConfig, @AutoDispatcherConfig Code Generation, Rationale: Multithreading by Convention

### Community 28 - "DispatcherConfig Module Gen"
Cohesion: 0.33
Nodes (1): DispatcherConfigModuleGenerator

### Community 29 - "FunctionTypedFeature Concurrency"
Cohesion: 0.33
Nodes (2): ConcurrencyTestState, FunctionTypedFeatureConcurrencyTest

### Community 30 - "Feature Base Types"
Cohesion: 0.33
Nodes (3): Feature, FlowFeature, FlowUnitFeature

### Community 31 - "iOS StateHandle"
Cohesion: 0.33
Nodes (1): IosStateHandle

### Community 32 - "Android StateHandle"
Cohesion: 0.33
Nodes (1): AndroidStateHandle

### Community 33 - "YAMV KSP Processor"
Cohesion: 0.4
Nodes (2): YamvProcessor, YamvProcessorProvider

### Community 34 - "IntentionRouter"
Cohesion: 0.4
Nodes (1): IntentionRouter

### Community 35 - "TypedFeature"
Cohesion: 0.4
Nodes (3): TypedFeature, TypedFeatureHolder, TypedUnitFeatureHolder

### Community 36 - "MviRetainedStore"
Cohesion: 0.4
Nodes (1): MviRetainedStore

### Community 37 - "FeatureRegistrar (Koin)"
Cohesion: 0.4
Nodes (1): FeatureRegistrar

### Community 38 - "KoinMviRetainedStore"
Cohesion: 0.4
Nodes (1): KoinMviRetainedStore

### Community 39 - "Hilt Class Names"
Cohesion: 0.5
Nodes (3): FeatureFqns, HiltClassNames, YamvClassNames

### Community 40 - "AutoDecrease Feature"
Cohesion: 0.5
Nodes (1): AutoDecreaseFeature

### Community 41 - "AutoIncrease Feature"
Cohesion: 0.5
Nodes (1): AutoIncreaseFeature

### Community 42 - "Koin App Entry"
Cohesion: 0.5
Nodes (1): KoinApp

### Community 43 - "Hilt App Entry"
Cohesion: 0.5
Nodes (1): YamvApp

### Community 44 - "iOS App Entry"
Cohesion: 0.5
Nodes (2): App, iOSApp

### Community 45 - "HasFeatureDispatcher Tests"
Cohesion: 0.5
Nodes (1): HasFeatureDispatcherTest

### Community 46 - "MviRuntime Core"
Cohesion: 0.5
Nodes (1): MviRuntime

### Community 47 - "MviStore"
Cohesion: 0.5
Nodes (1): MviStore

### Community 48 - "YAMV Logger Core"
Cohesion: 0.5
Nodes (1): Yamv

### Community 49 - "AutoFeature Discovery"
Cohesion: 0.5
Nodes (1): AutoFeatureDiscovery

### Community 50 - "Design Philosophy"
Cohesion: 0.5
Nodes (4): Outcomes as Standalone Classes Pattern, Distributed Reducers Design, Forced Modularity Design, Rationale: No Central Reducer

### Community 51 - "StateHandle Hilt Module"
Cohesion: 0.67
Nodes (1): YamvStateHandleModule

### Community 52 - "Dispatcher Hilt Module"
Cohesion: 0.67
Nodes (1): YamvDispatcherModule

### Community 53 - "AutoState Annotation"
Cohesion: 0.67
Nodes (2): AutoState, NoDefaultState

### Community 54 - "iOS Main ViewController"
Cohesion: 0.67
Nodes (0): 

### Community 55 - "Decrease Feature"
Cohesion: 0.67
Nodes (1): DecreaseFeature

### Community 56 - "MainActivity"
Cohesion: 0.67
Nodes (1): MainActivity

### Community 57 - "FunctionTypedFeature"
Cohesion: 0.67
Nodes (1): FunctionTypedFeature

### Community 58 - "ActionTypedFeature"
Cohesion: 0.67
Nodes (1): ActionTypedFeature

### Community 59 - "FunctionTypedFeature Wrapper"
Cohesion: 0.67
Nodes (0): 

### Community 60 - "YamvLogger"
Cohesion: 0.67
Nodes (1): YamvLogger

### Community 61 - "AutoState Discovery"
Cohesion: 0.67
Nodes (1): AutoStateDiscovery

### Community 62 - "AutoDispatcherConfig Discovery"
Cohesion: 0.67
Nodes (1): AutoDispatcherConfigDiscovery

### Community 63 - "HiltMviStore"
Cohesion: 1.0
Nodes (0): 

### Community 64 - "MviDispatcherConfig"
Cohesion: 1.0
Nodes (1): MviDispatcherConfig

### Community 65 - "State Interface"
Cohesion: 1.0
Nodes (1): State

### Community 66 - "AutoDispatcherConfig Annotation"
Cohesion: 1.0
Nodes (1): AutoDispatcherConfig

### Community 67 - "Counter Screen"
Cohesion: 1.0
Nodes (0): 

### Community 68 - "Counter State"
Cohesion: 1.0
Nodes (1): CounterState

### Community 69 - "Flow Extensions"
Cohesion: 1.0
Nodes (0): 

### Community 70 - "UI Theme"
Cohesion: 1.0
Nodes (0): 

### Community 71 - "Counter Content"
Cohesion: 1.0
Nodes (0): 

### Community 72 - "TypedFeature Wrapper"
Cohesion: 1.0
Nodes (0): 

### Community 73 - "HasFeatureScope"
Cohesion: 1.0
Nodes (1): HasFeatureScope

### Community 74 - "HasFeatureDispatcher"
Cohesion: 1.0
Nodes (1): HasFeatureDispatcher

### Community 75 - "DefaultFeatureScope"
Cohesion: 1.0
Nodes (1): DefaultFeatureScope

### Community 76 - "ActionTypedFeature Wrapper"
Cohesion: 1.0
Nodes (0): 

### Community 77 - "YamvLogLevel"
Cohesion: 1.0
Nodes (1): YamvLogLevel

### Community 78 - "CI/CD Infrastructure"
Cohesion: 1.0
Nodes (2): CI/CD GitHub Actions Pipeline, Semantic Versioning Setup

### Community 79 - "JitPack & Release"
Cohesion: 1.0
Nodes (2): JitPack Publishing, GitHub Release Workflow

### Community 80 - "Documentation Site"
Cohesion: 1.0
Nodes (2): GitHub Pages Deployment, MkDocs Material Site

### Community 81 - "Root Build Config"
Cohesion: 1.0
Nodes (0): 

### Community 82 - "OpenForTesting"
Cohesion: 1.0
Nodes (0): 

### Community 83 - "AutoFeature Annotation"
Cohesion: 1.0
Nodes (0): 

### Community 84 - "Counter Features Module"
Cohesion: 1.0
Nodes (0): 

### Community 85 - "Increase Feature"
Cohesion: 1.0
Nodes (0): 

### Community 86 - "UI Shape"
Cohesion: 1.0
Nodes (0): 

### Community 87 - "UI Color"
Cohesion: 1.0
Nodes (0): 

### Community 88 - "UI Typography"
Cohesion: 1.0
Nodes (0): 

### Community 89 - "MviStore Docs"
Cohesion: 1.0
Nodes (1): MviStore

### Community 90 - "Changelog"
Cohesion: 1.0
Nodes (1): Changelog v0.1.0

### Community 91 - "iOS StateHandle Docs"
Cohesion: 1.0
Nodes (1): IosStateHandle (iOS saved state)

### Community 92 - "Features Guide"
Cohesion: 1.0
Nodes (1): Features Guide

## Knowledge Gaps
- **84 isolated node(s):** `HiltClassNames`, `YamvClassNames`, `FeatureFqns`, `MviDispatcherConfig`, `State` (+79 more)
  These have ≤1 connection - possible missing edges or undocumented components.
- **Thin community `HiltMviStore`** (2 nodes): `HiltMviStore.kt`, `hiltMviStore()`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `MviDispatcherConfig`** (2 nodes): `MviDispatcherConfig.kt`, `MviDispatcherConfig`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `State Interface`** (2 nodes): `State.kt`, `State`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `AutoDispatcherConfig Annotation`** (2 nodes): `AutoDispatcherConfig.kt`, `AutoDispatcherConfig`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Counter Screen`** (2 nodes): `CounterScreen.kt`, `CounterScreen()`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Counter State`** (2 nodes): `CounterState.kt`, `CounterState`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Flow Extensions`** (2 nodes): `FlowExtensions.kt`, `takeUntilSignal()`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `UI Theme`** (2 nodes): `Theme.kt`, `YamvTheme()`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Counter Content`** (2 nodes): `CounterContent.kt`, `CounterContent()`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `TypedFeature Wrapper`** (2 nodes): `TypedFeatureWrapper.kt`, `wrap()`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `HasFeatureScope`** (2 nodes): `HasFeatureScope.kt`, `HasFeatureScope`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `HasFeatureDispatcher`** (2 nodes): `HasFeatureDispatcher.kt`, `HasFeatureDispatcher`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `DefaultFeatureScope`** (2 nodes): `DefaultFeatureScope.kt`, `DefaultFeatureScope`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `ActionTypedFeature Wrapper`** (2 nodes): `ActionTypedFeatureWrapper.kt`, `wrap()`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `YamvLogLevel`** (2 nodes): `YamvLogLevel.kt`, `YamvLogLevel`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `CI/CD Infrastructure`** (2 nodes): `CI/CD GitHub Actions Pipeline`, `Semantic Versioning Setup`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `JitPack & Release`** (2 nodes): `JitPack Publishing`, `GitHub Release Workflow`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Documentation Site`** (2 nodes): `GitHub Pages Deployment`, `MkDocs Material Site`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Root Build Config`** (1 nodes): `build.gradle.kts`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `OpenForTesting`** (1 nodes): `OpenForTesting.kt`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `AutoFeature Annotation`** (1 nodes): `AutoFeature.kt`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Counter Features Module`** (1 nodes): `CounterFeaturesModule.kt`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Increase Feature`** (1 nodes): `IncreaseFeature.kt`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `UI Shape`** (1 nodes): `Shape.kt`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `UI Color`** (1 nodes): `Color.kt`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `UI Typography`** (1 nodes): `Type.kt`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `MviStore Docs`** (1 nodes): `MviStore`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Changelog`** (1 nodes): `Changelog v0.1.0`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `iOS StateHandle Docs`** (1 nodes): `IosStateHandle (iOS saved state)`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Features Guide`** (1 nodes): `Features Guide`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `MviRuntime` connect `Architecture & Annotations` to `Dispatcher Architecture`?**
  _High betweenness centrality (0.003) - this node is a cross-community bridge._
- **What connects `HiltClassNames`, `YamvClassNames`, `FeatureFqns` to the rest of the system?**
  _84 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `Architecture & Annotations` be split into smaller, more focused modules?**
  _Cohesion score 0.1 - nodes in this community are weakly interconnected._
- **Should `MviRuntime Stress Tests` be split into smaller, more focused modules?**
  _Cohesion score 0.13 - nodes in this community are weakly interconnected._
- **Should `Koin Dispatcher Config Tests` be split into smaller, more focused modules?**
  _Cohesion score 0.13 - nodes in this community are weakly interconnected._
- **Should `Documentation & Getting Started` be split into smaller, more focused modules?**
  _Cohesion score 0.13 - nodes in this community are weakly interconnected._
- **Should `Features Module Generator` be split into smaller, more focused modules?**
  _Cohesion score 0.14 - nodes in this community are weakly interconnected._