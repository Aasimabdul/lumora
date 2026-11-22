## What changed

## How it was tested

- [ ] `./gradlew :core:test :app:testDebugUnitTest :app:lintDebug`
- [ ] Rust changes: `cargo fmt --check && cargo test && cargo clippy --all-targets --all-features -- -D warnings`

## Privacy and security checklist

- [ ] No notification content, secrets, keys or personal data in code, logs, tests or commits
- [ ] No new permissions (or they are justified and listed in `docs/PERMISSIONS.md`)
- [ ] No new network access or third-party SDKs (or they are justified and listed in `THIRD_PARTY_NOTICES.md`)
- [ ] Artwork and fonts are original or properly licensed
