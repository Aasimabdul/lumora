# Lumora — Your Night. Your Time.

A lightweight, open-source Android standby clock with customizable AMOLED themes, photo backgrounds, battery status, and optional privacy-conscious notification previews.

> **Project status: early.** The Android app is **not built yet**. There is no APK and no release. See [docs/DEVELOPMENT_STATUS.md](docs/DEVELOPMENT_STATUS.md) for exactly what exists and what has been verified.

## What exists today

- `core/` — Android-free Kotlin: the 40-theme catalog (ids, names, categories only), time/date formatting, battery mapping, charging-event dedup, and the notification-preview state machine with 10/15/20/25/30 s auto-hide. **50 tests pass.**
- `rust-core/` — WCAG contrast maths and theme-configuration validation/sanitising. **15 tests pass; rustfmt and clippy clean.** Not yet linked into Android.

## Planned (not implemented)

Compose UI and 40 rendered themes, standby mode, photo backgrounds and custom themes, DataStore persistence, `NotificationListenerService` integration, permissions & privacy screen, optional charging automation, CI-built APKs.

## Platform note

Lumora is an app-level standby clock. It does not replace the Android lock screen or a manufacturer Always-On Display.

## Build and test

See [docs/BUILDING.md](docs/BUILDING.md). Architecture: [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md). Privacy: [PRIVACY_POLICY.md](PRIVACY_POLICY.md). Permissions: [docs/PERMISSIONS.md](docs/PERMISSIONS.md).

## Security

Report vulnerabilities privately — see [SECURITY.md](SECURITY.md).

## License

MIT — see [LICENSE](LICENSE). Third-party components: [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md). Contributions: [CONTRIBUTING.md](CONTRIBUTING.md).
