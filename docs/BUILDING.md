# Building

## Requirements

- JDK 17+
- Android SDK (platform 35 and build-tools); set `ANDROID_HOME` or `local.properties` (`sdk.dir=...`)
- Rust stable with `rustfmt` and `clippy` (for `rust-core`)
- Android NDK — only for future Rust-on-Android integration (not implemented)

## Android app

```bash
./gradlew :core:test :app:testStandardDebugUnitTest :app:testNotificationsDebugUnitTest
./gradlew :app:lintStandardDebug :app:lintNotificationsDebug
./gradlew :app:assembleStandardDebug         # app/build/outputs/apk/standard/debug/
./gradlew :app:assembleNotificationsDebug    # app/build/outputs/apk/notifications/debug/
./gradlew :app:assembleStandardRelease :app:assembleNotificationsRelease   # signed only if signing env vars are set

# Two flavors: "standard" (no notification previews, installable past Play Protect) and
# "notifications" (adds the notification listener). See docs/INSTALL.md.
```

Release signing reads these environment variables (never commit keys): `LUMORA_KEYSTORE_PATH`, `LUMORA_KEYSTORE_PASSWORD`, `LUMORA_KEY_ALIAS`, `LUMORA_KEY_PASSWORD`. Without them the release APK is unsigned and not installable.

## Rust core

```bash
cd rust-core
cargo fmt --check
cargo test
cargo clippy --all-targets --all-features -- -D warnings
```

## Kotlin core without the Android SDK

`./gradlew` needs the SDK because `:app` is part of the build. To test only the Android-free logic, `scripts/verify-core.sh` compiles and runs `core` tests using the Kotlin compiler, coroutines and JUnit jars bundled with a local Gradle install (`gradle` on `PATH`).

## Releasing

Update `versionName`, push, then push a tag `vX.Y.Z` equal to it. `.github/workflows/release.yml` builds, verifies, and publishes only when signing secrets are configured.
