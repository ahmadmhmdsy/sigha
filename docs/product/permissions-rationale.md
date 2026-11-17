# Permissions rationale — Sigha 0.2.1

Every permission Sigha requests, why it exists, what happens when the user or the OS withholds it,
and what the app deliberately does **not** request. Checked against
`app/src/main/AndroidManifest.xml` on 2026-11-17 (the foreground-service permissions and service
element landed with the execution-model workstream; the manifest is the source of truth).

**Play store rule:** the permission set in the merged release manifest must match this document,
the privacy policy and the Data safety form. If a permission is added, this file is updated in the
same change — otherwise the listing review can truthfully be called deceptive.

## 1. Declared permissions

| Permission | Protection level | Requested at runtime? | Used for |
|---|---|---|---|
| `android.permission.INTERNET` | normal (install-time) | no | Optional OCR pack download only |
| `android.permission.ACCESS_NETWORK_STATE` | normal (install-time) | no | Metered/offline check before the 28 MB download |
| `android.permission.POST_NOTIFICATIONS` | dangerous, Android 13+ | yes | Progress notification for a running batch |
| `android.permission.FOREGROUND_SERVICE` | normal (install-time) | no | Keep a user-started batch alive when backgrounded |
| `android.permission.FOREGROUND_SERVICE_DATA_SYNC` | normal (install-time) | no | Service type for the same batch (Android 14+) |
| `android.permission.WRITE_EXTERNAL_STORAGE` (`maxSdkVersion="28"`) | dangerous, Android 6–9 only | yes | Legacy save to Download/Sigha on Android 8–9 |

### 1.1 `INTERNET`

- **Reason:** the OCR pack is not bundled (28,004,325 bytes: English 15,400,601 + Arabic
  12,603,724). It is downloaded only when the user taps Download on the Model packs screen.
- **What is sent:** an HTTPS GET for two pinned files on `raw.githubusercontent.com`
  (tessdata_best, commit `e12c65a915945e4c28e237a9b52bc4a8f39a0cec`). No file, filename, device
  identifier or usage data is sent. The host sees the request and the IP address, as with any
  HTTPS request; the privacy policy must say so.
- **What it is never used for:** conversion, history, settings, thumbnails, telemetry. There is no
  analytics, crash-reporting or ads SDK in the build.
- **If unavailable/blocked (e.g. firewall, VPN, device policy):** the download fails with a
  localized message naming the network as the cause; every conversion still works.
- **If the app had no permission:** not possible for normal permissions; if a future build adds a
  network-dependent feature, it must reuse this permission and this paragraph must change.

### 1.2 `ACCESS_NETWORK_STATE`

- **Reason:** before starting a 28 MB download, the app checks whether the connection is metered
  or absent so it can warn ("this may use mobile data") and avoid a pointless attempt offline.
- **What it does not do:** it does not read traffic content, SSIDs or identifiers.
- **If it fails or is unavailable:** the warning is skipped and the download is still offered; the
  user can cancel.

### 1.3 `POST_NOTIFICATIONS` (Android 13+)

- **Reason:** a running batch is executed in a foreground service, and Android requires that
  service to show a notification. The notification shows batch progress and a cancel action.
- **When requested:** contextually, the first time the user starts a conversion batch, with an
  in-app explanation; not on first launch.
- **If denied:** conversions still run and still survive backgrounding; the notification is
  suppressed (Android still lists the foreground service in the task manager), and progress remains
  visible on the app's Queue screen. The app does not nag; the user can grant it later in system
  settings.
- **Privacy:** the notification contains only file names the user chose and progress; it does not
  appear on the lock screen with details unless the user's system settings allow it.

### 1.4 `FOREGROUND_SERVICE` and `FOREGROUND_SERVICE_DATA_SYNC`

- **Reason:** conversions are CPU/IO-heavy and can take minutes (video, large PDFs). Without a
  foreground service Android would kill the batch shortly after the app is backgrounded. The
  service is user-started, short-lived (it stops when the queue is idle) and shows progress.
- **Type:** `dataSync` in `0.2.x` (short, user-initiated file processing with a visible
  notification). Android 15 limits `dataSync` runs to 6 hours per 24 hours; a batch is expected to
  finish far sooner, and the service stops when the queue empties.
- **Policy risk to resolve before production:** Google Play and Android 15/16 define a
  `mediaProcessing` type for "converting media files", which may be the type reviewers expect for
  this app, and it requires a Play declaration and a demonstration video. The choice between
  `dataSync` (used now) and `mediaProcessing` is owned by the policy workstream; if it changes,
  the manifest, this document, the store declarations and the demonstration video change together.
  Do not submit to production before that decision is recorded.
- **If the service cannot start** (e.g. background-start restriction): the batch runs while the app
  is visible and is marked interrupted otherwise; on the next launch the app says a batch was
  interrupted so the user can retry. Nothing is silently lost from the user's perspective — failed
  rows show a reason.
- **If the user force-stops the app:** the batch stops; the app reports the interruption next
  launch.

### 1.5 `WRITE_EXTERNAL_STORAGE` (maxSdkVersion 28)

- **Reason:** on Android 8.0–9.0 (API 26–28) writing to the public `Download/Sigha` folder still
  requires the legacy storage permission. On Android 10+ the app publishes through MediaStore and
  **does not** request this permission at all (`maxSdkVersion="28"`).
- **When requested:** when the user first saves a conversion on those Android versions, with an
  explanation; the system dialog is the only prompt.
- **If denied:** the result cannot be written to Downloads. The app keeps the file in its own
  storage and offers to save it through the system file picker (SAF) to a folder the user chooses;
  it does not crash or silently discard the result.
- **No broad storage access is requested on Android 10+:** inputs arrive through SAF
  (`OpenDocument` / `OpenDocumentTree`) or a share intent; `MANAGE_EXTERNAL_STORAGE` is a
  restricted Play permission and is not requested.

### 1.6 Permissions added by libraries (not written by the app)

| Permission | Origin | Behaviour |
|---|---|---|
| `android.permission.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION` | AndroidX (signature-level, auto-merged) | No runtime prompt, no user-visible effect; it hardens dynamically registered broadcast receivers. |

The merged manifest is what Play sees. Verify it with:

```
# after a release build; prints the permission list Play will see
aapt2 dump permissions app/build/outputs/apk/release/app-universal-release.apk
```

If the APK is ABI-split, run it on the arm64 APK. **`scripts/verify.ps1` does not currently assert
the permission list** — either extend it or record this command's output in the verification
notes for the release commit (open item for the build workstream).

## 2. Permissions deliberately NOT requested

| Not requested | Why not |
|---|---|
| `READ_EXTERNAL_STORAGE` / `READ_MEDIA_IMAGES` / `READ_MEDIA_VIDEO` / `READ_MEDIA_AUDIO` | Files are chosen by the user through SAF or shared in; the app reads only what the user selected, through the returned content URI grant. |
| `CAMERA` | No camera capture in `0.2.x`. Scanning is a roadmap outcome (see `PRODUCT.md`); if it ships, the camera permission and its Play declaration are a separate, visible product decision. |
| `RECORD_AUDIO`, `ACCESS_FINE_LOCATION`, `ACCESS_COARSE_LOCATION` | No feature needs them. |
| `MANAGE_EXTERNAL_STORAGE` | Restricted Play permission; unnecessary for a SAF app. |
| `QUERY_ALL_PACKAGES` | No package enumeration; share/Open-with intents use standard `<intent-filter>` matching. |
| `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` | The foreground service is the supported mechanism; asking for battery exemption would be unnecessary privilege. |
| `SYSTEM_ALERT_WINDOW`, `PACKAGE_USAGE_STATS`, `BLUETOOTH`, contacts, calendar, SMS | No feature needs them. |
| `com.google.android.gms.permission.AD_ID` | No advertising SDK is present. |

Also relevant to privacy: `android:allowBackup="false"` — the app's DataStore (settings, history)
and cache are excluded from Android backup, so conversion history does not ride along in a cloud
backup.

## 3. Denial matrix (quick answers for reviews and QA)

| Permission withheld | Conversions | OCR download | Notification | Output |
|---|---|---|---|---|
| `POST_NOTIFICATIONS` denied | run normally | unaffected | not shown | normal |
| Notification channel disabled | run normally | unaffected | not shown | normal |
| `WRITE_EXTERNAL_STORAGE` denied (Android 8–9) | run; save step needs the picker | unaffected | unaffected | user picks a folder via SAF |
| Network unavailable / blocked | run normally | fails with a message | unaffected | normal |
| Foreground-service start refused | run while app visible; interrupted batch is announced | unaffected | unaffected | normal |

## 4. Verification checklist for the release commit

1. Build the release APK/AAB on the commit being shipped.
2. Run `aapt2 dump permissions` and compare the list with §1 (including the library-added row).
3. Confirm the merged manifest has `<service android:name=".work.ConversionService"`,
   `android:exported="false"`, `android:foregroundServiceType="dataSync"`.
4. Confirm no `READ_*`, `CAMERA`, location or `MANAGE_EXTERNAL_STORAGE` appears.
5. Confirm `android:allowBackup="false"` is still in the application element.
6. Confirm the in-app privacy statement and the store Data safety answers match §1.
7. Record the output in `docs/qa/verification.md` (owned by the build workstream, not edited here).
