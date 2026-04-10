# YAMV — Yet Another MVI Framework

A Kotlin-first **MVI (Model-View-Intent)** framework for Android & Kotlin Multiplatform with compile-time code generation via KSP.

## Why YAMV?

MVI architecture brings predictability to UI state. YAMV makes it production-ready:

- **Compile-time safety** — KSP generates your ViewModels and DI modules
- **Zero runtime reflection** — all wiring happens at build time
- **Coroutine-native** — built on Kotlin Coroutines and Flow, no RxJava
- **Multiplatform** — same framework for Android (Hilt/Koin) and iOS (Koin + Compose Multiplatform)
- **Testable** — pure Kotlin core with no Android dependencies; test with JUnit 5 + Turbine

## Core Concepts

```
UI dispatches Intention
    → FeatureRouter routes to matching Features
        → Features emit Outcomes
            → StateOutcome  → reduces state (pure function)
            → EffectOutcome → side effect (navigation, toast, etc.)
            → IntentionOutcome → dispatches another intention
    → StateFlow<S> updates UI
```

## Modules at a Glance

| Module | What it provides |
|--------|-----------------|
| `core` | `State`, `Outcome`, `@AutoState`, `@AutoFeature` |
| `yamv` | `MviRuntime`, `MviStore`, `FeatureRouter`, feature builders |
| `yamv-viewmodel` | `MviViewModel` (Android + iOS) |
| `yamv-hilt` | `hiltMviStore()` Compose helper |
| `yamv-koin` | `koinMviStore()` Compose helper |
| `processor-hilt` | KSP: generates `*Store` ViewModel + `*FeaturesModule` |

## Quick Install

See [Getting Started](getting-started.md) for full setup instructions.
