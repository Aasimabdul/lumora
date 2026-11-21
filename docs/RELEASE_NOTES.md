# Lumora v1.1.0

App-level standby clock (does not replace the lock screen or Always-On Display).

**New**
- All 40 themes now have their own renderer; nothing is "Coming soon".
- Manual customization with live preview: clock size, date/battery text size, position, font, weight, text colour (swatches or hex), show/hide date and battery.
- My themes: colour or photo backgrounds (private storage, zoom/pan/darken/blur), favourites, search, category filters.
- Optional charging automation (off by default, works while Lumora is open), onboarding, About, per-app notification allow-list, clear-preview button, launcher icon.

**Unchanged**
- Notification previews are opt-in, memory-only, 10/15/20/25/30 s auto-hide.

**Not included:** Rust runtime integration (the Rust library exists but the app does not use it), UI/instrumented tests, vibration option.

**Verification:** Kotlin unit tests, Android lint, debug APK build and Rust fmt/test/clippy are run by GitHub Actions. **Not verified:** behaviour on a real device or emulator, and how each theme looks on screen.
