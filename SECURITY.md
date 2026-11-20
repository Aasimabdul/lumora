# Security Policy

## Supported versions

Lumora has no released version yet. Once releases exist, only the latest release receives security fixes.

## Reporting a vulnerability

Please **do not open a public issue** for security problems.

Report privately using GitHub's **"Report a vulnerability"** button (Security tab → Advisories) on the Lumora repository. Include:

- what the problem is and which component it affects,
- steps to reproduce,
- the impact you expect (e.g. exposure of notification content),
- your Android version and device, if relevant.

You can expect an acknowledgement within 7 days. Fixes are coordinated with the reporter before public disclosure.

## Scope and design commitments

Issues of particular interest:

- notification content (sender, message text) leaking to storage, logs, crash reports or the network,
- previews shown when disabled, when privacy mode is on, or after access is revoked,
- any use of permissions beyond those listed in [docs/PERMISSIONS.md](docs/PERMISSIONS.md),
- unsafe handling of user-supplied photo URIs or stored theme configuration.

Out of scope: behaviour that requires a rooted or already-compromised device, and Android's own lock-screen or Always-On Display (Lumora does not replace them).

## Handling secrets

Signing keys, keystores and tokens must never be committed. `.gitignore` excludes common key and credential files; release signing, when added, will use repository secrets.
