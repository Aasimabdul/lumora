# Third-party notices

Lumora itself is MIT-licensed (see [LICENSE](LICENSE)). The dependencies currently declared are:

| Component | Used in | License |
| --- | --- | --- |
| Kotlin standard library | `core` (runtime) | Apache License 2.0 |
| kotlinx.coroutines | `core` (runtime) | Apache License 2.0 |
| JUnit 4 | `core` (tests only, not shipped) | Eclipse Public License 1.0 |
| Rust standard library | `rust-core` | MIT OR Apache-2.0 |

`rust-core` has no third-party crate dependencies.

This list covers only what exists today. When the Android app module is added (Jetpack Compose, Material 3, AndroidX, DataStore, etc.), this file and the in-app open-source licenses screen must be updated, and the dependency licenses re-checked from the resolved dependency tree rather than from memory.
