# Lumora — Development Status

_Last updated: 2026-11-22. Statuses: PASS, FAIL, BLOCKED, NOT RUN, NOT STARTED._

Repository: https://github.com/Aasimabdul/lumora (branch `main`).

## Verified in GitHub Actions (CI run on commit b300484)

| Check | Result |
| --- | --- |
| `cargo fmt --check`, `cargo test` (15 tests), `cargo clippy -D warnings` | PASS |
| `./gradlew :core:test :app:testDebugUnitTest` (core: 55 tests) | PASS |
| `./gradlew :app:lintDebug` | PASS |
| `./gradlew :app:assembleDebug` | PASS |
| APK inspection (`aapt2 dump badging`) | `com.simbuild.lumora`, versionName 0.1.0, versionCode 1 (1.1.0 / 2 in current source), compileSdk 35 |

The app was first compiled in CI. Fixes needed on the way: `android.useAndroidX` in `gradle.properties`, an API-28 guard for display-cutout mode, and replacing `android-actions/setup-android` (license-acceptance failure) with a direct license step.

## Not verified / not done

| Item | Status |
| --- | --- |
| Install and launch on a device or emulator | NOT RUN |
| Runtime behaviour: standby, notification previews, battery updates | NOT VERIFIED |
| Compose UI / instrumented tests | NOT STARTED |
| Signed release build (needs owner's keystore secrets) | NOT DONE |
| Rust used by the app (JNI/NDK) | NOT STARTED — library is not linked into the app |

## Themes

Catalog: 40. Renderers: **40** (`ThemeRegistryTest` asserts the registry keys equal the catalog ids). The renderers are verified only to compile and to be registered; how each one looks on a real screen is NOT VERIFIED.

## Added in 1.2.0

11 bundled fancy fonts (Open Font License) with a font picker in Customize and Settings; refreshed UI (display typeface, tab icons, redesigned Home and Settings with grouped cards). Visual result NOT VERIFIED on a device.

## Added in 1.1.0

All 40 themes; manual customization (size, position, font, weight, colour); saved custom themes with colour/photo backgrounds; favourites/search/filters; charging automation (foreground only); onboarding; About; notification app allow-list and clear button; launcher icon.

## Not started

DataStore persistence tests; Compose/instrumented tests; Rust JNI; vibration option.

## Release

See GitHub Releases. A tag `vX.Y.Z` equal to `versionName` triggers `.github/workflows/release.yml`. Without signing secrets it publishes only a debug-signed pre-release.
