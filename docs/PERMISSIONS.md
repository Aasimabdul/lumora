# Permissions

The manifest declares **no `<uses-permission>` entries**. The built APK contains one permission added automatically by AndroidX (`com.simbuild.lumora.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION`, an app-internal permission that protects Lumora's own non-exported receivers on older Android versions); it grants no access to user data.

| Access | Needed for | Notes |
| --- | --- | --- |
| Notification Listener access | Optional notification previews | A special access the user grants in Android settings, separate from runtime permissions. Off by default. The service is declared with `BIND_NOTIFICATION_LISTENER_SERVICE`. Revoke in Android Settings → Notifications → Notification access. |
| `POST_NOTIFICATIONS` (Android 13+) | Lumora's *own* notifications | Not requested; Lumora posts none. It would **not** grant access to other apps' notifications. |
| Photo access | Custom photo backgrounds | Feature not implemented. Plan: system Photo Picker, no storage permission. |

Explicitly avoided: contacts, SMS, call log, microphone, location, unrestricted storage, overlay, accessibility, root.

Apps may hide notification content; Lumora can only show what Android provides to a listener. The manifest `<queries>` entry (launcher apps) only helps show app names; it is not a permission.

_Verified with `aapt2 dump badging` on the CI-built debug APK (2026-11-20)._
