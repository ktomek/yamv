# Changelog

## [Unreleased]

### Added
- `TypedUnitFeature<S, I>` — typed counterpart of `Feature.FlowUnitFeature` for streaming side effects with no state contribution; `.wrap()` produces a real `FlowUnitFeature` (#50, #52).
- Hilt KSP processor now supports `@AutoFeature` on all typed feature shapes — `TypedFeature`, `ActionTypedFeature`, `TypedUnitFeature` were previously dropped at discovery (#51, #56).
- Typed `koinMviStore<S, I>()` with cross-navigation sharing via `ProvideAppMviStoreOwner` and `koinMviAppStore<S, I>()` (#41, #44).
- `:yamv-processor-hilt-fixtures` — KSP coverage fixtures asserting the binding decision (`@Binds` vs `@Provides … = it.wrap()`) for every recognized feature shape (#55, #58).
- `MviExceptionHandler` — customizable exception handling for the MVI pipeline with fail-fast default.
- `MviErrorContext` / `ErrorSource` — structured error context (source, intention, feature).
- Lifecycle and concurrency stress tests for `MviRuntime`.

### Changed
- Publishing migrated from JitPack to Maven Central; coordinates are now `io.github.ktomek:<artifact>:VERSION` (#35, #36).
- Modules renamed with `yamv-` prefix: `core` → `yamv-core`, `processor-core` → `yamv-processor-core`, `processor-hilt` → `yamv-processor-hilt` (#37, #38, #48, #49).

### Fixed
- Eager `FlowFeature` emissions were dropped due to an `MviRuntime` startup race; `FeatureRouter` now blocks the first dispatch until every feature has subscribed (#46, #47).

## [0.1.0] — Initial Release

### Added
- `MviRuntime` — core MVI runtime with coroutine-based state management
- `MviStore` — interface for state store (state + effects + dispatch)
- `FeatureRouter` — intention routing with subscription ordering guarantee
- `Feature` sealed interface: `FlowFeature`, `FlowUnitFeature`
- `TypedFeature` — typed intention flow transformer
- `FunctionTypedFeature` — single-intention suspend handler with concurrent processing
- `MviViewModel` — Android/iOS ViewModel base class
- `@AutoState` / `@AutoFeature` — KSP annotations for compile-time code generation
- `processor-hilt` — Hilt KSP processor generating `*Store` and `*FeaturesModule`
- `yamv-hilt` — `hiltMviStore()` Compose helper
- `yamv-koin` — `koinMviStore()` multiplatform Compose helper
- iOS support via Compose Multiplatform + Koin
