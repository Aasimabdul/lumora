# Lumora — Development Status

_Last updated: 2026-11-20. Statuses: PASS, FAIL, BLOCKED, NOT RUN, NOT STARTED._

## Environment (authoring sandbox)

JDK 21, Gradle 8.14.3, Rust 1.97 (rustfmt, clippy). **No Android SDK, NDK, adb or emulator.** Google Maven, Maven Central, Gradle plugin portal, `dl.google.com` and `services.gradle.org` return HTTP 403; `gh` token is invalid. Re-checked after a long pause: unchanged.

## What was actually verified

| Check | Result |
| --- | --- |
| `cargo fmt --check`, `cargo test` (15 tests), `cargo clippy -D warnings` | PASS |
| Kotlin `core` unit tests via `scripts/verify-core.sh` | PASS — 55 tests |
| `app/` Kotlin parse check (Kotlin 2.0.21 compiler, no Android classpath) | No syntax errors in 14 files. **This is not a compile**: ~1,000 unresolved-reference errors are expected without the Android/Compose libraries |
| Manifest / resource XML well-formed | PASS |
| `./gradlew :core:test` | BLOCKED here (needs AGP + SDK because `:app` is in the build) |
| Android lint, `assembleDebug`, `assembleRelease` | BLOCKED |
| Install/launch on device or emulator | NOT RUN |
| CI and release workflows | NOT RUN |
| APK | **None exists** |
| GitHub push / tag / release | BLOCKED (invalid token) — nothing published |

## Implemented, but UNCOMPILED (written without an Android toolchain; expect first-build fixes)

- `app/`: Application, DataStore settings repository, `NotificationListenerService`, notification-access helper, `MainActivity` (Home / Themes / Privacy / Settings tabs), `StandbyActivity` (immersive, keep-screen-on while visible, tap for Exit), live clock + battery state, preview card, 6 theme renderers.
- Manifest declares **no permissions**; listener service is protected by `BIND_NOTIFICATION_LISTENER_SERVICE`.
- Release signing reads secrets from the environment only.

## Not started

34 of 40 theme renderers; photo backgrounds and custom themes; charging automation; onboarding, about and licenses screens; Compose/instrumented tests; DataStore persistence tests; JNI/NDK integration of `rust-core` (the library is **not used by the app**).

## Theme count

Catalog: 40. Renderers written: **6** (uncompiled). Verified rendering: **0**.

## Next steps

1. Push to GitHub with a valid token; let CI (`ci.yml`) run and fix the first-build errors.
2. Add release secrets (see `.github/workflows/release.yml`), then tag `v0.1.0` — the tag must equal `versionName`.
3. Implement the remaining themes and features, then NDK/JNI for Rust.
