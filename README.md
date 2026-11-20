# Lumora — Your Night. Your Time.

A lightweight, open-source Android standby clock with customizable AMOLED themes, photo backgrounds, battery status, and optional privacy-conscious notification previews.

> **Project status: early, not yet built.** The Android app source is written but has **never been compiled**. There is no APK and no release. See [docs/DEVELOPMENT_STATUS.md](docs/DEVELOPMENT_STATUS.md) for exactly what has and hasn't been verified.

## What exists

- **`app/`** (uncompiled) — Compose standby clock with live time/date/battery, Home / Themes / Privacy / Settings screens, DataStore settings, opt-in notification previews via `NotificationListenerService`. **6 of 40 themes** have renderers; the rest show as "Coming soon".
- **`core/`** — Android-free Kotlin: theme catalog (40 ids), time/date formatting, battery mapping, charging-event dedup, settings validation, and the notification-preview state machine with 10/15/20/25/30 s auto-hide. **55 tests pass.**
- **`rust-core/`** — WCAG contrast maths and theme-config validation. **15 tests pass; rustfmt and clippy clean.** Not yet linked into the app.

## Not implemented yet

34 themes, photo backgrounds, custom themes, charging automation, Rust integration, Compose/instrumented tests, onboarding and about screens.

## Platform note

Lumora is an app-level standby clock. It does not replace the Android lock screen or a manufacturer Always-On Display.

## Build and test

See [docs/BUILDING.md](docs/BUILDING.md). Architecture: [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md). Privacy: [PRIVACY_POLICY.md](PRIVACY_POLICY.md). Permissions: [docs/PERMISSIONS.md](docs/PERMISSIONS.md).

## Security

Report vulnerabilities privately — see [SECURITY.md](SECURITY.md).

## License

MIT — see [LICENSE](LICENSE). Third-party components: [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md). Contributions: [CONTRIBUTING.md](CONTRIBUTING.md).
