# Security policy

## Supported versions

| Version | Supported |
|---|---|
| 0.2.x (current: **0.2.1**, versionCode 3) | Yes — security fixes land on the current development line |
| 0.1.x and earlier | No |

The project currently distributes **QA builds signed with the standard Android debug key**, attached
to GitHub Releases. They are not Play Store builds and have not been through a store review; treat
them as test builds. The production signing key and the Play distribution path are tracked in
[`docs/legal/release-blockers.md`](docs/legal/release-blockers.md) (blocker B1) and
[`docs/plan/publisher-guide.md`](docs/plan/publisher-guide.md).

## Report a vulnerability privately

Please **do not open a public issue, pull request or discussion** for a security problem, and do not
post exploit details before a fix is available.

Use GitHub's private vulnerability reporting for this repository:

1. Open <https://github.com/ahmadmhmdsy/sigha/security/advisories/new> — or use the repository's
   **Security** tab → **Advisories** → **Report a vulnerability**. (This requires GitHub's private
   vulnerability reporting to be enabled for the repository; it is on the repository-settings
   checklist in [`docs/publishing-checklist.md`](docs/publishing-checklist.md).)
2. Describe the issue: the affected version (`0.2.1`, versionCode 3) and how it was installed,
   Android version plus device/ABI, what an attacker can do, exact reproduction steps, and any logs
   or proof of concept.
3. Remove file names, folder paths and personal data from logs and screenshots.

GitHub advisories provide a private channel and a place for coordinated disclosure; the maintainer
handles reports there. No security email address or PGP key is published for this project — please do
not rely on one.

If private reporting is temporarily unavailable, contact the maintainer through GitHub at
<https://github.com/ahmadmhmdsy> and say only that you have a security report; keep the details for
the private channel.

## What to expect

Sigha is maintained by one person on a best-effort basis. The times below are targets, not a
contract:

| Stage | Target |
|---|---|
| Acknowledge the report | within 7 days |
| Initial assessment (in scope? severity, affected versions) | within 14 days |
| Fix or mitigation for a confirmed high-severity issue | as soon as practical, typically within 30 days |
| Public advisory, with your approval and credit if you want it | after a fix is released |

There is no bug bounty. Good-faith research that follows this policy is welcome, and the maintainer
will not pursue or support legal action against it.

## Scope

In scope — the app's own code and this repository's distribution:

- `app/src/main/java/com/sigha/converter/**` — conversion engines, routing, queue and foreground
  service, settings, output publishing (MediaStore and SAF), model-pack download and verification.
- Exported Android components and intent handling: share-in, "open with", pending intents, the
  foreground-service notification and its tap target.
- The privacy claims: anything that sends data off the device without the user's explicit action, or
  any telemetry, analytics or crash reporting found in a build.
- The OCR model-pack verification path (pinned URL, size and SHA-256 validation, resume handling).
- Build and release configuration in this repository (secrets handling, signing wiring, CI).
- Dependency vulnerabilities that are reachable in a shipped APK.

Out of scope:

- Vulnerabilities in third-party libraries and engines (FFmpegKit/FFmpeg, PDFBox-Android,
  commons-compress, xz, Tesseract4Android, AndroidX, kotlinx.serialization). Report those upstream —
  but do tell us when a shipped version is affected, so the dependency can be updated.
- Problems that require a rooted device, physical access, or a malicious OS/ROM.
- Social engineering, spam, phishing, or denial of service against GitHub.
- Play Protect warnings or installation friction caused by the debug-key signature of the current QA
  builds (a documented limitation, not a vulnerability).
- The placeholder legal text in the shipped assets (a documented release blocker, not a bug).

## Network surface and data handling

Sigha has **no server**. It has no account, no ads, no analytics, no crash reporting and no remote
logging. Files are converted on the device; only the system picker and `Share` move a result out,
and both are user actions.

The **only** network request the app can make is the optional OCR language-pack download:

- It starts only after an explicit tap on the Model packs screen and a confirmation dialog that shows
  the size (~28 MB) and warns on metered connections. Nothing is downloaded at first launch, during a
  conversion, or in the background.
- It downloads Tesseract trained-data files from `raw.githubusercontent.com` over HTTPS, pinned to a
  specific commit and verified against a SHA-256 digest that ships inside the app; a corrupted or
  substituted file is rejected before it is installed.
- The host sees the usual HTTPS request data (IP address, time, requested file, user agent). No file
  names, file contents or conversion metadata are sent. The user-facing statement is the
  [privacy policy](docs/legal/privacy-policy.md).

A build that makes any other network request is a security/privacy report — please use the private
channel above.

## Secure-by-default notes

- No broad storage permission is requested on Android 10+; output uses MediaStore or a
  user-granted SAF folder. The "All files access" permission is not used.
- Android cloud backup is disabled; app state stays local.
- No signing key material is in the repository. Release signing comes from a git-ignored
  `keystore/keystore.properties` on the publisher's machine (see
  [`docs/legal/release-blockers.md`](docs/legal/release-blockers.md)).
- Dependencies are pinned directly in `app/build.gradle.kts` (plugin versions in
  `build.gradle.kts`), and the exact shipped set is recorded in `NOTICE.md` and
  [`docs/legal/licence-compliance.md`](docs/legal/licence-compliance.md). JitPack is reachable only
  for the Tesseract4Android group by a content filter in `settings.gradle.kts`.
