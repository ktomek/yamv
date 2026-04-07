# YAMV iOS Example

## Setup

1. Build the Kotlin framework:
   ```bash
   ./gradlew :yamv:linkDebugFrameworkIosSimulatorArm64
   ```

2. Open `iosApp.xcodeproj` in Xcode.

3. Build and run on the iOS Simulator.

The framework is built from `:yamv` and linked automatically via a Run Script build phase.
