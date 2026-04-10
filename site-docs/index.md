# YAMV — Yet Another MVI Framework

A Kotlin-first **MVI (Model-View-Intent)** framework for Android & Kotlin Multiplatform with compile-time code generation via KSP.

## Why YAMV?

MVI architecture brings predictability to UI state. YAMV makes it production-ready:

- **Compile-time safety** — KSP generates your retained stores and DI modules
- **Zero runtime reflection** — all wiring happens at build time
- **Coroutine-native** — built on Kotlin Coroutines and Flow, no RxJava
- **Multiplatform** — same framework for Android (Hilt/Koin) and iOS (Koin + Compose Multiplatform)
- **Testable** — pure Kotlin core with no Android dependencies; reducers are standalone classes testable without coroutines

## Core Concepts

``` mermaid
flowchart LR
    UI["🖥️ UI\ndispatch()"] -->|Intention| Router["🚦 FeatureRouter"]
    Router --> Features["🧩 Features"]
    Features -->|StateOutcome| State["🔄 reduces state"]
    Features -->|EffectOutcome| Effect["⚡ side effect"]
    Features -->|IntentionOutcome| Router
    State -->|"StateFlow‹S›"| UI

    style UI fill:#7c4dff,color:#fff,stroke:#7c4dff
    style Router fill:#aa00ff,color:#fff,stroke:#aa00ff
    style Features fill:#d500f9,color:#fff,stroke:#d500f9
    style State fill:#651fff,color:#fff,stroke:#651fff
    style Effect fill:#6200ea,color:#fff,stroke:#6200ea
```

## Modules at a Glance

| Module | What it provides |
|--------|-----------------|
| `core` | `State`, `Outcome`, `@AutoState`, `@AutoFeature` |
| `yamv` | `MviRuntime`, `MviStore`, `FeatureRouter`, feature builders |
| `yamv-retainer` | `MviRetainedStore` (Android ViewModel + iOS) |
| `yamv-hilt` | `hiltMviStore()` Compose helper |
| `yamv-koin` | `koinMviStore()` / `mviStore {}` DSL |
| `processor-hilt` | KSP: generates `*Store` retained store + `*FeaturesModule` |

## Quick Install

See [Getting Started](getting-started.md) for full setup instructions.
