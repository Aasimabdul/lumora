# Architecture

Lumora is an application-level standby clock. It does **not** replace the Android lock screen, privileged system UI, or a manufacturer's Always-On Display.

## Modules

| Module | State | Purpose |
| --- | --- | --- |
| `core/` (Kotlin/JVM, no Android deps) | Implemented, tested | Theme catalog, time/date formatting, battery mapping, charging-event dedup, notification-preview state machine |
| `rust-core/` (Rust, no deps) | Implemented, tested, **not linked into Android** | WCAG contrast maths, `readable_foreground`, theme-config validation and sanitising |
| `app/` (Android, Compose) | Compiles and passes unit tests + lint in CI; **not device-tested** | Standby activity, 4-tab main UI, DataStore repository, listener service, 6 theme renderers. Manual DI via `LumoraApp`; no ViewModels yet |

Keeping logic in Android-free modules makes it testable on a plain JVM.

## Notification preview policy (implemented in `NotificationPreviewController`)

- One card at a time; the newest eligible notification replaces the current card.
- Eligible only when previews are enabled **and** notification access is granted **and** standby is visible. Events at other times are discarded, never queued.
- Ongoing notifications and group summaries are ignored. Optional package allowlist (empty = all apps).
- Identical repeat events are ignored and do not restart the timer. A same-key event with changed content updates the card and restarts the timer.
- Removal of the source notification hides its card. The system notification is never cancelled or modified.
- Display duration is 10/15/20/25/30 s (default 15 s). Changing it re-times the active card measured from when it was first shown (hidden immediately if that time has passed).
- Disabling the feature, revoking access, leaving standby, or `release()` clears the card and cancels the timer immediately.
- Changing show-text / show-sender / privacy-mode clears the visible card.
- Privacy filtering happens when the card is built, so hidden text never enters UI state. `toString()` on notification/card types is redacted.
- Each timer carries a generation number; a callback from a superseded timer is a no-op.
- Content is held in memory only; the model has no persistence path.

## Rust core and Android

`rust-core` has no JNI layer yet. To integrate: add the NDK, build `cdylib` for the target ABIs with `cargo-ndk`, expose a small JNI surface, and verify the `.so` files are packaged. Until that is done and tested, the app must not claim to use it.

## Theme catalog vs. rendering

`ThemeCatalog` defines 40 ids/names/categories. `ui/clock/ThemeRegistry.kt` merges the per-category renderer files (`Themes*.kt`) into `themeRegistry`; a unit test requires one renderer per catalog id. Renderers draw through the shared `Stage` in `ThemeKit.kt`, which applies the user's size/position/font/colour from `ClockFrame`. A saved custom theme (`CustomTheme`, stored as versioned JSON in DataStore) wraps a base renderer with a colour or photo background (`ThemeSurface.kt`); photos live in private storage via `PhotoStore`.
