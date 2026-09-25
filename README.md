# PressStress

PressStress is an Android-first, system-wide pause button. It floats above apps and
the launcher, giving the user a deliberate hold interaction before an impulse turns
into automatic scrolling.

## Why Android and Kotlin

Android exposes the system overlay and foreground-service APIs needed for this
product. Native Kotlin keeps the binary, memory footprint, and permission lifecycle
smaller than a cross-platform runtime. iOS does not offer a general-purpose window
that can remain above other apps, so an equivalent iOS version would need a different
product design (Shortcuts, Screen Time shields, or an app-specific experience).

## MVP features

- System-wide floating button over apps and the home screen
- Explicit overlay-permission onboarding
- Draggable vertical position, fixed to the left edge
- 5–30 second configurable hold interaction
- Gentle, neutral, strict, and minimal message tones
- Local daily completion counter
- Persistent notification with a one-tap stop action
- No accounts, analytics, network access, Accessibility Service, or usage-history access

See [docs/PRODUCT_PLAN.md](docs/PRODUCT_PLAN.md) for architecture, policy notes,
design decisions, and the staged roadmap.

## Build

Requirements:

- JDK 17
- Android SDK 36
- Android Studio Quail 4 (2026.1.4) or newer

Open the project in Android Studio, allow Gradle sync to finish, and run the `app`
configuration on an Android 8.0+ device or emulator.

Command line:

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug
```

The debug APK is written to `app/build/outputs/apk/debug/app-debug.apk`.
GitHub Actions also builds the APK on every push to `main` and makes it available as
the `pressstress-debug` artifact on the workflow run.
