# Building

## Requirements

- JDK 17+ (developed with 21)
- Rust stable with `rustfmt` and `clippy`
- Android SDK + build-tools (needed only once the `app` module exists)
- Android NDK (needed only for Rust-on-Android integration, not yet implemented)

## Rust core

```bash
cd rust-core
cargo fmt --check
cargo test
cargo clippy --all-targets --all-features -- -D warnings
```

## Kotlin core (no Android required)

```bash
./gradlew :core:test
```

If Maven repositories are unreachable, `scripts/verify-core.sh` compiles and runs the same tests using the Kotlin compiler, coroutines and JUnit jars bundled with a local Gradle installation (`gradle` must be on `PATH`).

## Android app

Not yet implemented. Planned: `./gradlew lint assembleDebug assembleRelease`. Release signing will use secrets supplied by the repository owner; no keys are ever committed.
