# Lumora v1.2.1

**Play Protect fix.** Google Play Protect blocks sideloaded apps that declare a notification listener, so Lumora is now built in two variants:

- `Lumora-v1.2.1.apk` — **standard**: everything except notification previews. Does not declare a notification listener.
- `Lumora-v1.2.1-notifications.apk` — adds optional notification previews. Install with `adb` (see [docs/INSTALL.md](docs/INSTALL.md)).

This is not a guarantee: Play Protect may still show an unknown-app prompt for a new app. Not tested on a device.

---

# Lumora v1.2.0

**New in 1.2.0**
- 11 bundled fancy fonts (Audiowide, Michroma, Bebas Neue, Righteous, Anton, Poiret One, Pacifico, Lobster, Share Tech Mono, VT323, Press Start 2P; SIL OFL) on top of the 5 system fonts.
- Font picker that previews the time in every font, in both Customize and a new Settings > Clock font section.
- UI refresh: display typeface for titles, navigation icons, redesigned Home screen and grouped Settings cards.

Everything from 1.1.0 below still applies. Not device-tested.

---

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
