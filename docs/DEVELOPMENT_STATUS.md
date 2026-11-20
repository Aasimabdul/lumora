# Lumora — Development Status

_Last updated: 2026-11-20. Statuses: PASS, FAIL, BLOCKED, NOT RUN, NOT STARTED._

## Baseline (Milestone A)

| Item | Finding |
| --- | --- |
| Working directory | Empty; not a Git repository. No existing Lumora / "Standby Mode" project, no remotes. A fresh repo was initialised. |
| JDK | OpenJDK 21.0.13 |
| Gradle | 8.14.3 (local install); wrapper generated for 8.14.3 |
| Kotlin | No standalone `kotlinc`; Kotlin 2.0.21 compiler bundled inside Gradle is used by `scripts/verify-core.sh` |
| Rust | rustc / cargo 1.97.0, rustfmt, clippy; host target only |
| Android SDK / build-tools / NDK / adb / emulator | **Not installed** |
| Devices / emulators | None |
| GitHub CLI | Installed (2.89.0); `gh auth status` reports the configured token as invalid |
| Network | crates.io index/static host reachable. Maven Central, Google Maven, `dl.google.com` and `services.gradle.org` are denied (HTTP 403 from the egress proxy) |

## Blockers

1. **Android SDK and Android Gradle Plugin cannot be obtained here** (Google/Maven hosts denied). Consequently the `app` module (Compose UI, listener service, manifest), Android lint, Compose/instrumented tests, APK build, signing and device testing cannot be done in this environment.
2. **No GitHub access** (invalid token) — nothing has been pushed; no tag or release exists.
3. **No NDK** — Rust cannot be cross-compiled for Android ABIs or linked via JNI here.

## Milestones

| Milestone | Status | Notes |
| --- | --- | --- |
| A. Inspect / baseline | PASS | See above. No baseline Android build was possible. |
| B. Architecture & core UI | PARTIAL | Pure-Kotlin `core` module done and tested. Navigation, dashboard, DataStore, standby screen: NOT STARTED (need Android SDK). |
| C. 40 themes | PARTIAL | Catalog (ids, names, categories) done and tested. **No Compose rendering of any theme exists.** 0 of 40 themes render. |
| D. Photos & custom themes | NOT STARTED | Rust config validation/sanitising (for corrupted custom themes) exists and is tested. |
| E. Permissions & notifications | PARTIAL | Preview state machine and auto-hide logic done and tested. `NotificationListenerService`, manifest, settings UI, permissions screen: NOT STARTED. |
| F. Battery & charging | PARTIAL | Battery mapping and charging-event dedup done and tested. Broadcast receiver / launch logic: NOT STARTED. |
| G. Rust | PARTIAL | Library done, tested, linted. **Not integrated into Android** (no NDK, no app module). |
| H. Test & build repair | PARTIAL | Rust and Kotlin-core checks pass. Android lint, debug/release builds: BLOCKED. |
| I. GitHub & release | BLOCKED | Local commits only. |

## Verified results (executed in this environment)

| Check | Result |
| --- | --- |
| `cargo fmt --check` (rust-core) | PASS |
| `cargo test` (rust-core) | PASS — 15 tests |
| `cargo clippy --all-targets --all-features -- -D warnings` | PASS |
| Kotlin `core` module via `scripts/verify-core.sh` (Kotlin 2.0.21, JUnit 4.13.2, coroutines 1.6.4) | PASS — 50 tests |
| `./gradlew :core:test` | NOT RUN (Gradle cannot resolve plugins/dependencies offline) |
| Android lint / `assembleDebug` / `assembleRelease` | BLOCKED |
| CI workflow | NOT RUN |

Tests *authored but not executed*: none. Tests *not authored*: Compose UI, Android integration, DataStore persistence/migration, photo-URI recovery (all depend on the Android layer).

## Next concrete steps

1. Run in an environment with Android SDK and Maven access (a developer machine or GitHub Actions): `./gradlew :core:test`.
2. Create the `app` module (Compose, DataStore, manifest, `NotificationListenerService`) on top of `core`.
3. Implement the 40 theme renderers and previews, then screenshot tests.
4. Add NDK + JNI binding for `rust-core` and verify ABI packaging.
5. Fix `gh` authentication, create the `Lumora` repository, push, and let CI verify.
