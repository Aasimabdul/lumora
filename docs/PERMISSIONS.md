# Permissions (design — app module not yet implemented)

The manifest does not exist yet. This is the intended, minimal set.

| Access | Needed for | Notes |
| --- | --- | --- |
| Notification Listener access | Optional notification previews | Granted by the user in Android system settings, separate from any runtime permission. Off by default. Revoke in Settings → Notifications → Notification access. |
| `POST_NOTIFICATIONS` (Android 13+) | Lumora's *own* notifications, only if a feature needs them | Does **not** grant access to other apps' notifications. |
| Photo access | Custom photo backgrounds | Use the system Photo Picker (no broad storage permission). |

Explicitly avoided: contacts, SMS, call log, microphone, location, unrestricted storage, overlay, accessibility, root.

Apps may hide notification content; Lumora can only show what Android provides to a listener.
