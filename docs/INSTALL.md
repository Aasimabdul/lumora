# Installing Lumora (and Google Play Protect)

Lumora is not on Google Play, so you install the APK yourself ("sideloading"). Android and Google Play Protect treat that more strictly than Play Store installs.

## Which APK?

| File | Notification previews | Play Protect |
| --- | --- | --- |
| `Lumora-vX.Y.Z.apk` (**standard**) | Not included | Does **not** declare a notification listener, so it is not blocked for that reason |
| `Lumora-vX.Y.Z-notifications.apk` | Included (optional, off by default) | Declares a notification listener, which Play Protect blocks when an app is installed from a browser, messaging app or file manager |

Both use the same app id, so you can switch between them as long as they are signed with the same key. Files ending in `-debug.apk` are signed with the Android debug key; switching between a debug-signed and a release-signed build needs an uninstall first.

Why: Google's developer guidance says apps installed from the internet that declare `BIND_NOTIFICATION_LISTENER_SERVICE` are blocked at install ([source](https://developers.google.com/android/play-protect/warning-dev-guidance)). That is the only feature in Lumora that needs it, so the standard build leaves it out.

## Installing the standard build

1. Download the APK from the GitHub Release and compare its checksum with `SHA256SUMS.txt` (`sha256sum Lumora-*.apk`).
2. Open it and allow your browser/file manager to install apps when Android asks.
3. If Play Protect says it has not seen the app before, choose **Scan app** (sends it to Google for a check) or **More details → Install anyway**. Google says the unknown-app prompt goes away once the app has been checked; it cannot be appealed.

Lumora cannot turn Play Protect off or hide from it, and you should not disable Play Protect permanently.

## Installing the notifications build

Because of the block above, install it from a computer with `adb` (wireless or USB debugging), which is not an "internet-sourced" install:

```
adb install -r Lumora-vX.Y.Z-notifications.apk
```

Then, on Android 13 and newer, sideloaded apps may have **restricted settings**: open Settings → Apps → Lumora → ⋮ (top right) → **Allow restricted settings**, then turn on Notification access for Lumora. These steps vary slightly by device.

## For maintainers: avoiding the "debug key" problem

Debug-signed pre-releases are what the release workflow publishes when no signing secrets exist. To publish properly signed releases:

```
pkg install -y openjdk-17 gh      # Termux
gh auth login
bash scripts/make-keystore.sh
```

The script creates the keystore on your device and stores `LUMORA_KEYSTORE_B64`, `LUMORA_KEYSTORE_PASSWORD`, `LUMORA_KEY_ALIAS` and `LUMORA_KEY_PASSWORD` as GitHub repository secrets. You can then publish a release without a computer: GitHub → Actions → **Release** → *Run workflow* (branch `main`). It builds the `versionName` in `app/build.gradle.kts`, creates the tag `v<versionName>` on that commit and publishes the release. Back up the keystore file; losing it means future updates cannot install over existing installs.

If Play Protect still blocks a build that follows Google's guidance, the app developer can file a Play Protect appeal: <https://support.google.com/googleplay/android-developer/contact/protectappeals>. No signing or flavor choice can guarantee Play Protect will not warn about a new, unknown app.
