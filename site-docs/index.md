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

<p align="center">
  <img src="resources/yamv-mvi-flow.gif" alt="YAMV MVI Data Flow" width="700"/>
</p>

## Modules at a Glance

| Module | What it provides |
|--------|-----------------|
| `yamv-core` | `State`, `Outcome`, `@AutoState`, `@AutoFeature` |
| `yamv` | `MviRuntime`, `MviStore`, `FeatureRouter`, feature builders |
| `yamv-retainer` | `MviRetainedStore` (Android ViewModel + iOS) |
| `yamv-hilt` | `hiltMviStore()` Compose helper |
| `yamv-koin` | `koinMviStore<S, I>()` / `mviStore {}` DSL |
| `yamv-processor-core` | DI-agnostic KSP utilities (`AutoStateDiscovery`, `AutoFeatureDiscovery`) |
| `yamv-processor-hilt` | Hilt KSP: generates `*Store` retained store + `*FeaturesModule` |

## Quick Install

See [Getting Started](getting-started.md) for full setup instructions.
