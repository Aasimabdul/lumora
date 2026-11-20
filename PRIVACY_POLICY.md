# Privacy Policy (draft)

Status: **draft** — the Android app does not exist yet, so this describes binding design commitments, not yet-verified behaviour. It will be re-checked against the shipped app and its dependency list before any release.

- **No network use.** The code in this repository contains no networking. Dependencies will be audited before release.
- **Photos** stay on the device; they are referenced via the system picker, not uploaded.
- **Notification access** is optional, off by default, and granted by the user in Android settings. Previews are held in memory only, shown for 10–30 s (default 15 s), then cleared. Message text is never written to storage, logs or crash reports. Lumora never replies to, dismisses or alters notifications.
- **Controls:** disable previews at any time; hide message text or sender; privacy mode; revoke access in Android settings.
