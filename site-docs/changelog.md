# Changelog

## [Unreleased]

### Added
- `MviExceptionHandler` — customizable exception handling for the MVI pipeline with fail-fast default
- `MviErrorContext` / `ErrorSource` — structured error context (source, intention, feature)
- Lifecycle and concurrency stress tests for `MviRuntime`

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
