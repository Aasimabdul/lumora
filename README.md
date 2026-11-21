# Lumora — Your Night. Your Time.

A lightweight, open-source Android standby clock with customizable AMOLED themes, photo backgrounds, battery status, and optional privacy-conscious notification previews.

> **Project status: feature-complete preview, not device-tested.** The app compiles and its unit tests and lint pass in CI, but it has **not been run on a real device or emulator**. Pre-release APKs on the Releases page are debug-signed unless the owner has added signing secrets. See [docs/DEVELOPMENT_STATUS.md](docs/DEVELOPMENT_STATUS.md).

## What exists

- **40 themes**, every one with its own renderer (digital, analog, futuristic, nature, creative, templates). No "Coming soon" entries.
- **Manual clock customization** with a live preview: clock size, date/battery text size, position, font family (default, sans, serif, mono, cursive), weight, text colour (swatches or any `#RRGGBB`), show/hide date and battery.
- **My themes**: save a look as a named theme with a colour or photo background (zoom, pan, darken, blur on Android 12+). Photos are copied into private app storage (EXIF/GPS removed) and never uploaded. Favourites, search and category filters in the gallery.
- **Standby clock** with live time/date/battery, optional notification previews (Notification access, off by default, 10/15/20/25/30 s auto-hide, privacy mode, per-app allow-list, memory only).
- **Charging automation** (off by default): starts standby when a charger is connected **while Lumora is open on screen**; optional exit on unplug. Android does not let apps open themselves from the background, so it cannot start while Lumora is closed.
- Onboarding, About/licence info, privacy & permissions centre.
- **`core/`** — Android-free Kotlin: catalog, validated settings, clock look, custom-theme model and JSON codec, moon phase, notification-preview state machine, charging dedup. Unit-tested.
- **`rust-core/`** — WCAG contrast maths and theme-config validation (15 tests; rustfmt and clippy clean). **Not linked into the app** — Lumora does not use Rust at runtime.

## Not done

Device/emulator testing, Compose/instrumented UI tests, Rust JNI integration, vibration option, signed release (needs the owner's keystore secrets).

## Platform note

Lumora is an app-level standby clock. It does not replace the Android lock screen or a manufacturer Always-On Display.

## Build and test

See [docs/BUILDING.md](docs/BUILDING.md). Architecture: [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md). Privacy: [PRIVACY_POLICY.md](PRIVACY_POLICY.md). Permissions: [docs/PERMISSIONS.md](docs/PERMISSIONS.md).

## Security

Report vulnerabilities privately — see [SECURITY.md](SECURITY.md).

## License

MIT — see [LICENSE](LICENSE). Third-party components: [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md). Contributions: [CONTRIBUTING.md](CONTRIBUTING.md).
