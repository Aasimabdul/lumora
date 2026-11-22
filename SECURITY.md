# Security Policy

## Supported versions

Only the latest release receives security fixes.

| Version | Supported |
| --- | --- |
| 1.2.x (latest) | Yes |
| Older | No — please update |

## Reporting a vulnerability

Please **do not open a public issue** for security problems.

Report privately through GitHub: **Security tab → "Report a vulnerability"**
(<https://github.com/Aasimabdul/lumora/security/advisories/new>). Include:

- what the problem is and which component it affects,
- steps to reproduce,
- the impact you expect (for example exposure of notification content),
- your Android version and device, if relevant.

What to expect:

- an acknowledgement within **7 days**,
- a status update at least every **14 days** until it is resolved,
- coordinated disclosure: please give us a reasonable time to ship a fix (target: 90 days) before publishing details,
- credit in the release notes if you want it.

Lumora is maintained by one person without a bug bounty; reports are appreciated and handled in good faith.

## Scope

In scope:

- notification content (sender, message text) leaking to storage, logs, crash reports or the network,
- previews shown when disabled, when privacy mode is on, or after access is revoked,
- any use of permissions beyond those listed in [docs/PERMISSIONS.md](docs/PERMISSIONS.md),
- unsafe handling of user-chosen photos, stored theme configuration or file names (for example path traversal),
- weaknesses in the build, signing or release workflows.

Out of scope: behaviour that requires a rooted or already-compromised device, social engineering, denial of service by a user against their own device, and Android's own lock screen or Always-On Display (Lumora does not replace them).

## Security measures in this repository

- Notification text is held in memory only and filtered before it reaches UI state (see [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) and [docs/THREAT_MODEL.md](docs/THREAT_MODEL.md)).
- No network permission is requested; no analytics or crash-reporting SDKs are included.
- Backups are disabled (`allowBackup="false"`).
- Photos are copied to private app storage, re-encoded (location metadata dropped), and read back only through a name-validated path.
- CI: unit tests, Android lint, CodeQL, Dependabot (Gradle, Cargo, GitHub Actions), Gradle wrapper validation, secret scanning, dependency review on pull requests.
- Workflows use least-privilege `permissions`; releases publish a SHA-256 checksum next to the APK.

## Handling secrets

Signing keys, keystores and tokens must never be committed. `.gitignore` excludes common key and credential files. Release signing uses repository secrets (`LUMORA_KEYSTORE_B64`, `LUMORA_KEYSTORE_PASSWORD`, `LUMORA_KEY_ALIAS`, `LUMORA_KEY_PASSWORD`) that only the owner sets. If you find a secret in the repository or its history, report it privately as above.

Verify a download: compare `sha256sum Lumora-*.apk` with the published `.sha256` file.
