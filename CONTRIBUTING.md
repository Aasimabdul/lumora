# Contributing

1. Keep logic that doesn't need Android in `core/` or `rust-core/` and cover it with tests.
2. Before opening a PR: `cargo fmt --check && cargo test && cargo clippy --all-targets --all-features -- -D warnings` in `rust-core/`, and `./gradlew :core:test`.
3. Don't weaken tests or suppress warnings to get green.
4. Never include message content, secrets or signing material in code, logs or commits.
5. Theme artwork must be original or properly licensed — no copyrighted characters or brand assets.
