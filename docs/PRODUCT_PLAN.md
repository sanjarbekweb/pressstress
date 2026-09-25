# PressStress product and engineering plan

## Product promise

PressStress creates a small pause between an impulse and an automatic action. It is
not a blocker and it does not shame the user. The primary success metric is the share
of completed holds followed by the user leaving or shortening the unwanted session.

## Platform decision

### Android: ship first

Native Kotlin is the most efficient implementation for this product because the core
feature is an Android system window (`TYPE_APPLICATION_OVERLAY`) owned by a foreground
service. A Flutter or React Native implementation would still need a native Android
service, permission flow, notification channel, and custom window integration.

The minimum supported version is Android 8.0 (API 26), where
`TYPE_APPLICATION_OVERLAY` became the supported non-system overlay type.

### iOS: separate product, not a port

iOS does not permit an arbitrary interactive view to remain above the launcher or
other apps. An iOS edition must be designed around supported mechanisms such as
FamilyControls/ManagedSettings shields, App Intents, widgets, or Shortcuts. It cannot
honestly promise feature parity with the Android floating button.

## MVP experience

1. The app explains why the overlay permission is needed.
2. The user selects a hold duration and message tone.
3. Android opens the system "display over other apps" approval screen.
4. The user explicitly starts the overlay.
5. A 64 dp translucent blue button sits 5 dp from the left safe edge.
6. The user can drag it vertically. Horizontal drift is prevented.
7. Holding fills a restrained progress ring. Releasing early resets it.
8. Completion gives light haptic feedback, rotates a short one-line message, and
   increments a local daily count.
9. The overlay can always be stopped from the main screen or its notification.

## Visual system

- Near-black navy surfaces (`#070B16` and `#0E1528`)
- Cobalt/periwinkle accent (`#6D7CFF`) with a restrained cyan highlight
- Simulated mirror glass: translucent radial fill, crisp rim, small internal specular
  highlight; no debris, particle cloud, or heavy shadow
- One-line reactions set in the system sans typeface, 16 sp, beside the button
- Tone is supportive by default. Strict reactions are opt-in.

True cross-app background blur is device- and compositor-dependent. The MVP uses a
reliable translucent glass treatment rather than requesting screen capture or drawing
false blur from another app's pixels.

## Architecture

```text
MainActivity
  -> Overlay permission + notification permission
  -> SharedPreferences (duration, tone, position, count)
  -> explicit start/stop intents

OverlayService (foreground, specialUse)
  -> persistent notification + stop action
  -> WindowManager
       -> MirrorHoldView (touchable 64 dp button)
       -> TextView (non-touchable message, alpha <= 0.8)
```

Two small windows avoid turning the message text into a large invisible touch blocker.
The label window is non-touchable and stays at or below Android's documented maximum
obscuring opacity, allowing interaction with the app below it.

## Permissions and privacy

Required:

- `SYSTEM_ALERT_WINDOW`: granted manually in Android settings
- `FOREGROUND_SERVICE` and `FOREGROUND_SERVICE_SPECIAL_USE`: keep the visible overlay
  alive while the app is not foregrounded
- `POST_NOTIFICATIONS` on Android 13+: show the persistent service notification

Deliberately excluded from MVP:

- Accessibility Service
- Usage access / app history
- screen capture
- contacts, location, microphone, camera
- internet permission

All preferences and counts remain on-device. The app does not determine which app is
underneath the overlay. App-aware reactions can be evaluated later only after policy
review and explicit user research proves they add enough value to justify access.

## Play release checklist

- Explain overlay use before opening system settings.
- Record the required Play Console foreground-service demo video.
- Declare the `specialUse` subtype and explain the user-perceptible wellness overlay.
- Ensure the persistent notification accurately describes the active feature.
- Publish a concise privacy policy even though MVP data is local-only.
- Test overlay touch pass-through, permission revocation, process death, rotation,
  split screen, foldables, and OEM battery-management behavior.

## Roadmap

### Validation build (implemented here)

- Global overlay, hold flow, settings, local completion count, stop controls

### Pilot

- Before/after urge score
- Session history stored locally
- Export/delete controls
- Copy and tone experiments
- Battery and OEM compatibility telemetry only with explicit consent

### Evidence-driven expansion

- Optional schedules and focus modes
- App-specific triggers only if users demand them and a policy-safe implementation is
  validated
- Android Digital Wellbeing integrations if public APIs become suitable
- iOS companion built around supported Screen Time APIs, without promising an overlay

## Primary references

- Android overlay settings and `Settings.canDrawOverlays`:
  https://developer.android.com/reference/android/provider/Settings
- `TYPE_APPLICATION_OVERLAY`, touch flags, and obscuring opacity:
  https://developer.android.com/reference/android/view/WindowManager.LayoutParams
- Foreground-service types and `specialUse` declarations:
  https://developer.android.com/develop/background-work/services/fgs/service-types
- Foreground-service overview:
  https://developer.android.com/develop/background-work/services/fgs
- Android 16 SDK setup:
  https://developer.android.com/about/versions/16/setup-sdk
- Built-in Kotlin in Android Gradle Plugin 9+:
  https://developer.android.com/build/migrate-to-built-in-kotlin
- Google Play device and network abuse policy:
  https://support.google.com/googleplay/android-developer/answer/16559646
