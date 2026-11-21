# Third-party notices

Lumora itself is MIT-licensed (see [LICENSE](LICENSE)). The dependencies currently declared are:

| Component | Used in | License |
| --- | --- | --- |
| Kotlin standard library | `core` (runtime) | Apache License 2.0 |
| kotlinx.coroutines | `core` (runtime) | Apache License 2.0 |
| kotlinx.serialization (JSON) | `core` (runtime) | Apache License 2.0 |
| AndroidX ExifInterface | `app` (runtime) | Apache License 2.0 |
| AndroidX (Core, Activity, Lifecycle, Navigation, DataStore) | `app` (runtime) | Apache License 2.0 |
| Jetpack Compose (UI, Foundation, Material 3) | `app` (runtime) | Apache License 2.0 |
| kotlinx.coroutines-android | `app` (runtime) | Apache License 2.0 |
| JUnit 4 | `core` (tests only, not shipped) | Eclipse Public License 1.0 |
| Rust standard library | `rust-core` | MIT OR Apache-2.0 |

`rust-core` has no third-party crate dependencies.

This list covers only what exists today. Licenses above for the Android libraries are from the declared dependencies and have **not** been checked against a resolved dependency tree. Re-check before release; an in-app licenses screen is not implemented yet.
