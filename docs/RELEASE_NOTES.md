# Lumora v0.1.0 (early preview)

An early, incomplete preview — not the full 1.0.

**Included**
- Full-screen standby clock (app-level; does not replace the lock screen or Always-On Display) with live time, date, battery level and charging status.
- 6 of the planned 40 clock themes: Minimal Digital, Neon Cyan, Flip Clock, Vertical Clock, Analog Classic, Minimal Analog. The other 34 appear in the gallery as "Coming soon" and cannot be selected.
- Optional notification previews (off by default) through Android's Notification access, with sender/text visibility, privacy mode and 10/15/20/25/30 s auto-hide (default 15 s). Content is kept in memory only.
- Settings (12/24-hour, date format, date/battery visibility) saved with DataStore.

**Not included yet**
- 34 themes, photo backgrounds, custom themes, charging automation, Rust integration (the Rust library exists but the app does not use it), UI/instrumented tests, onboarding and about screens.

**What was verified (GitHub Actions):** Kotlin unit tests, Android lint, debug APK build, and Rust fmt/test/clippy all passed on the tagged commit. **Not verified:** behaviour on a real device or emulator (standby mode, notification previews, battery updates are untested at runtime).
