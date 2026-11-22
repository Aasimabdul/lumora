# Threat model

Lumora is an app-level standby clock. This is a short, honest model of what it protects and how. It is not a formal audit, and none of it has been verified on a device yet.

## Assets

1. **Notification content** (sender, message text) while previews are enabled.
2. **User photos** chosen as theme backgrounds.
3. **Settings and saved themes** (preferences only).
4. **Release signing key** (held by the owner, in repository secrets).

## Trust boundaries

- Android delivers notifications to `LumoraNotificationListener` only after the user grants Notification access.
- The photo picker hands the app a single user-chosen image; Lumora requests no storage permission.
- The app requests no network permission and ships no analytics, ads or crash-reporting SDKs.

## Threats and mitigations

| Threat | Mitigation |
| --- | --- |
| Message text written to disk, logs or crash reports | Held in memory only; `toString()` of notification types is redacted; no persistence path exists in the model. |
| Hidden text still reaching UI state | Privacy filtering happens when the preview card is built, so hidden fields never enter state. |
| Preview shown after being disabled or access revoked | The preview is hidden when previews are disabled, privacy-relevant settings change, or the app stops being allowed; generation counters make stale timers no-ops. |
| Previews from unwanted apps | Optional per-app allow-list. |
| Photo location metadata leaking | Photos are re-encoded as JPEG on import (EXIF dropped). |
| Path traversal through stored file names | Stored names must match a strict pattern and resolve inside the backgrounds directory. |
| Corrupt or hostile stored theme JSON | Tolerant, bounded decoding with validation and clamping; unknown or newer formats fall back to defaults. |
| Data exfiltration by backup | `allowBackup="false"`. |
| Malicious dependency or workflow change | Dependabot, CodeQL, dependency review, wrapper validation, least-privilege workflow permissions. |
| Leaked signing key | Never in the repository; secret scanning in CI; owner-only repository secrets. |

## Known limits

- Anyone with the unlocked phone can see a preview while it is on screen (use privacy mode).
- A rooted or compromised device is out of scope.
- Debug-signed pre-releases are not production-signed; compare the published SHA-256 before installing.
- Runtime behaviour has not been verified on a physical device.

Report problems privately: see [SECURITY.md](../SECURITY.md).
