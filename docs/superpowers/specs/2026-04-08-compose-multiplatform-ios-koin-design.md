# Compose Multiplatform iOS Example with Koin — Design Spec

**Date:** 2026-04-08
**PR:** #8 — iOS: SwiftUI example app consuming Yamv KMP framework
**Issue:** #5

---

## Goal

Replace the SwiftUI `iosApp/` counter example with Compose Multiplatform. Share counter logic, UI, and Koin configuration between Android (`app/koin`) and iOS by converting three modules to KMP. The iOS entry point becomes a thin Xcode shell that hosts a `ComposeUIViewController` produced from Kotlin.

---

## Module Changes

| Module | Before | After | Notes |
|---|---|---|---|
| `app/common` | Android library | KMP library | CMP UI, shared intentions, theme |
| `app/koin` | Android app | KMP app | Android APK + iOS framework |
| `yamv-koin` | Android library | KMP library | DSL works on both platforms |
| `app/hilt` | Android app | unchanged | Android-only, not affected |
| `yamv`, `core`, `yamv-viewmodel` | already KMP | unchanged | no changes needed |
| `iosApp/` | SwiftUI shell | CMP Xcode shell | remove SwiftUI counter logic |

---

## `app/common` — KMP Library

**Plugin:** `kotlin("multiplatform")` + `com.android.library`

**Source sets:**
- `commonMain`: `CounterContent.kt` (CMP), `Intentions.kt`, theme (`Color`, `Type`, `Shape`, `Theme`)
- No `androidMain` or `iosMain` needed — all code is platform-agnostic CMP

**Compose:** Migrate from `androidx.compose.material` (Material 1) to `org.jetbrains.compose.material3` (Material 3). This only affects `app/common` and `app/koin`; `app/hilt` is untouched.

**Dependencies:**
```kotlin
commonMain {
    api(project(":core"))
    api(project(":yamv"))
    implementation(compose.material3)
    implementation(compose.runtime)
    implementation(compose.foundation)
    implementation(libs.kotlinx.coroutines.core)
}
```

---

## `app/koin` — KMP Application

**Plugin:** `kotlin("multiplatform")` + `com.android.application`

**Source sets:**
- `commonMain`: `CounterState`, feature classes, outcomes, `CounterFeaturesModule` (Koin), `CounterScreen`
- `androidMain`: `MainActivity`, `KoinApp` — unchanged from today
- `iosMain`: `MainViewController.kt` — Koin init + `ComposeUIViewController`

**`iosMain/MainViewController.kt`:**
```kotlin
fun initKoin() {
    startKoin { modules(counterModule) }
}

fun MainViewController() = ComposeUIViewController { CounterScreen() }
```

**iOS framework output:**
```kotlin
listOf(iosX64(), iosArm64(), iosSimulatorArm64()).forEach {
    it.binaries.framework {
        baseName = "Counter"
        isStatic = true
    }
}
```

**Dependencies:**
```kotlin
commonMain {
    implementation(project(":app:common"))
    implementation(project(":yamv-koin"))
    implementation(libs.koin.core)
}
androidMain {
    implementation(libs.koin.android)
    implementation(libs.androidx.core.ktx)
    implementation(libs.bundles.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.timber)
}
```

---

## `yamv-koin` — KMP Library

**Plugin:** `kotlin("multiplatform")` + `com.android.library`

**Source sets:**
- `commonMain`: entire `KoinMviStore.kt` DSL (`KoinMviStore<S>`, `stateQualifier`, `mviStore`, `koinMviStore`)
- `koinViewModel()` in `koinMviStore()` sourced from `io.insert-koin:koin-compose-viewmodel` (KMP)

**Replace** `koin-androidx-compose` with `koin-compose` + `koin-compose-viewmodel`.

**Dependencies:**
```kotlin
commonMain {
    api(project(":yamv-viewmodel"))
    implementation(project(":core"))
    implementation(libs.koin.core)
    implementation(libs.koin.compose)
    implementation(libs.koin.compose.viewmodel)
}
androidMain {
    implementation(libs.koin.android)
}
```

---

## `iosApp/` — Xcode Shell

**Remove:**
- `counter/CounterViewModel.swift`
- `counter/CounterView.swift`

**Update `ContentView.swift`:**
```swift
import SwiftUI
import Counter

struct ContentView: View {
    var body: some View {
        ComposeView()
            .ignoresSafeArea(.keyboard)
    }
}

struct ComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        MainViewControllerKt.MainViewController()
    }
    func updateUIViewController(_ vc: UIViewController, context: Context) {}
}
```

**Update `iOSApp.swift`:**
```swift
import SwiftUI
import Counter

@main
struct iOSApp: App {
    init() {
        MainViewControllerKt.initKoin()
    }
    var body: some Scene {
        WindowGroup { ContentView() }
    }
}
```

**Update build phase:**
- `./gradlew :app:koin:linkDebugFrameworkIosSimulatorArm64` (was `:yamv:link…`)

**Update framework search path:**
- `$(SRCROOT)/../app/koin/build/bin/iosSimulatorArm64/debugFramework`

---

## Gradle / Version Catalog

**`gradle/libs.versions.toml` additions:**
```toml
[versions]
compose-multiplatform = "1.8.0"

[plugins]
compose-multiplatform = { id = "org.jetbrains.compose", version.ref = "compose-multiplatform" }

[libraries]
koin-compose = { module = "io.insert-koin:koin-compose", version.ref = "koin" }
koin-compose-viewmodel = { module = "io.insert-koin:koin-compose-viewmodel", version.ref = "koin" }
```

`koin-compose` and `koin-compose-viewmodel` are available at Koin 4.0.0 — no version bump needed.

---

## Data Flow

The MVI flow is identical on both platforms:

```
ComposeUI (CounterScreen)
  → koinMviStore<CounterState>()
  → koinViewModel(stateQualifier<CounterState>())   [koin-compose-viewmodel, KMP]
  → KoinMviStore<CounterState>                       [yamv-koin, KMP]
  → MviRuntime<CounterState>                         [yamv, KMP]
  → Feature set (via counterModule)                  [app/koin commonMain]
  → StateFlow<CounterState>
  → CounterScreen re-renders
```

Koin ViewModel lifecycle on iOS is managed by `androidx.lifecycle:lifecycle-viewmodel` (already KMP via `yamv-viewmodel`).

---

## What Does Not Change

- `app/hilt` — Android-only, untouched
- `processor-hilt`, `processor-core` — untouched
- `yamv`, `core`, `yamv-viewmodel` — already KMP, no changes
- MVI logic — identical on both platforms, no platform forks

---

## Verification

```bash
# Android still works
./gradlew :app:koin:assembleDebug
./gradlew :app:hilt:assembleDebug

# iOS framework builds
./gradlew :app:koin:linkDebugFrameworkIosSimulatorArm64

# Open Xcode, run on simulator
open iosApp/iosApp.xcodeproj
```
