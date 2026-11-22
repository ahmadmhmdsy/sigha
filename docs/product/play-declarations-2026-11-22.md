# Play Console declarations — paste-ready packet (builder draft, 2026-11-22)

**Status:** builder preparation only. **Nothing here has been submitted to Play Console.** The
publisher pastes these answers; several depend on values only the publisher has (privacy URL, legal
name, testers). This packet exists so Step 7 of `docs/plan/publisher-guide.md` is a copy job, not a
research job.

**Rule zero still applies:** do not upload until `scripts/verify.ps1 -ReleaseCandidate` passes (no
placeholder in shipped legal text) and the physical arm64 checklist is recorded against the tagged
build.

Sources for the policy rules are in `docs/research/market.md` §2.4 and §2.12 (Play foreground-service
declaration, data-safety, target-audience pages, fetched 2026-11-16). Field labels in Play Console
change over time — read the live form and map these answers onto it; if a label does not match, the
rule underneath still applies.

## 1. Data safety

The app has no analytics, ads, crash-reporting or account SDK (verified in `app/build.gradle.kts`
and the manifest: six permissions, no third-party network SDK). The only outbound request is the
optional, explicitly user-initiated OCR model-pack download over HTTPS
(`model/PackInstaller.kt`, pinned `raw.githubusercontent.com`/GitHub release URLs).

| Play question | Answer | Why |
|---|---|---|
| Does your app collect or share any of the required user data types? | **No** | User files are read and written on-device; nothing is transmitted. The pack download sends no user data. |
| Is all of the user data encrypted in transit? | **Not applicable** (if forced: encryption yes for the pack HTTPS) | No user data leaves the device. The pack download is HTTPS. |
| Do you provide a way for users to request that their data be deleted? | **Not applicable** | No server-side account or stored user data. |
| Data types (location, contacts, files, etc.) | **None selected** | Local file access is user-initiated via the system picker/SAF and never leaves the device. |
| Is your app's data collection independent / third-party? | **No third parties** | No SDK collects data. |

Consistency notes for the reviewer-facing texts: the privacy policy must say the same — on-device
processing, optional pack download, no analytics, no ads, no account.

## 2. Content rating questionnaire (IARC)

App category: **Utility / Productivity** (a file converter). Answer draft, to be completed in the
form truthfully:

| Question area | Answer |
|---|---|
| Violence, blood, sexual content, nudity, language, controlled substances, gambling | **No to all** |
| User-generated content shared with other users | **No** — files never leave the device, there is no feed, chat or sharing surface inside the app |
| Users can interact or exchange content with other users | **No** |
| Shares user's location | **No** (no location permission) |
| Purchases / ads | **No in-app purchases, no ads** |
| Does the app contain a browser or link to external content? | The privacy policy/licences are in-app screens; the app itself does not open a browser (the OCR pack downloads in the background) |
| Is the app primarily directed at children? | **No** — general utility, target audience 13+ |

## 3. Target audience, ads, access

| Section | Answer |
|---|---|
| Target audience | **13+ / general** (not primarily children) |
| Ads | **No ads** |
| App access | **All functionality available without restrictions** — no login, no account, no subscription |
| News app / COVID / government | No / No / No |
| In-app purchases | **None** (no billing library, no Play Billing) |
| Financial features / health / VPN / photo-video permissions declarations | None apply |

## 4. Foreground-service declaration (required, targetSdk 36)

The shipped manifest declares **`ConversionService` with `android:foregroundServiceType="dataSync"`**
(`app/src/main/AndroidManifest.xml`), plus `FOREGROUND_SERVICE` and
`FOREGROUND_SERVICE_DATA_SYNC`. Android 15+ also enforces the `dataSync` 6 h/24 h budget; the service
overrides `onTimeout` and stops cleanly (`work/ConversionService.kt`).

**What to fill in the form**

- **Service type:** `dataSync` (must match the uploaded manifest; the alternative
  `mediaProcessing` is a policy-workstream decision — if it ever changes, manifest, declaration and
  demo video change together).
- **Use case:** choose the closest listed option for local, user-initiated file processing. The exact
  wording of Play's list changes; at the time of writing the guide recommended the
  "local file processing / local processing" option. Read the live list and pick the one that says
  the work happens locally for the user — do not pick "data transfer" if a local-processing option
  exists.
- **Description (draft, ~350 characters):**
  > Sigha converts the files the user explicitly selected into another format, entirely on the
  > device. A batch can take minutes, so a foreground service with a progress notification keeps the
  > conversion alive when the user switches apps. The service starts only when the user taps Convert
  > and stops when the queue is idle or the user cancels.
- **User impact if the service is deferred or interrupted (draft):**
  > Without the foreground service, a conversion the user started would be paused or killed when they
  > leave the app, and the user would lose the time spent. With it, progress is visible and the batch
  > completes in the background; the user can cancel from the notification or the app.
- **Demo video:** public or unlisted HTTPS link (unlisted YouTube is fine), 30–60 s, recorded on the
  **tagged upload build** on the **borrowed arm64 phone** (see §5 script).

## 5. Demo-video script (30–60 s, record once on the tagged build)

Record at 9:16 or 16:9, app icon at the start, English captions (reviewer language) — Arabic UI is
fine to show, but add English captions so the flow is unambiguous. No personal files, no notifications
from other apps; airplaneless, Wi-Fi off is fine because conversion is local.

1. **0–5 s** — App icon and name on the launcher, then open Sigha.
2. **5–15 s** — Tap Choose files → DocumentsUI → select **3 files** (e.g. 2 images + 1 PDF) → Open.
3. **15–20 s** — Select target PDF, tap "Convert 3 files".
4. **20–35 s** — Press Home: the progress notification appears with the app name, the current file
   and progress; the queue continues.
5. **35–45 s** — Pull down the notification shade to show the progress notification, tap it: the app
   reopens on the running batch.
6. **45–55 s** — Completion state: "N files converted", saved location, Share/Open actions; open one
   output.
7. **55–60 s** — Optional: cancel a second batch from the notification to show the stop path.

Keep the video and the declaration text consistent: if the copy says "cancel from the notification",
show the cancel action or remove that clause.

**Capture commands (device or emulator):**

```powershell
# rehearse on any API 36 device/emulator; the submission video must be the tagged build on the arm64 phone
adb shell screenrecord --size 1080x1920 --bit-rate 8000000 --time-limit 60 /sdcard/fgs-demo.mp4
# perform the script, then:
adb pull /sdcard/fgs-demo.mp4 fgs-demo.mp4
```

A rehearsal recorded on the x86_64 emulator is fine for timing and captions; do **not** submit it —
the declaration video must show the uploaded build (and the phone run is the first time the arm64
artefact executes at all).

## 6. Pre-submission consistency checklist

- [ ] `pwsh -File scripts/verify.ps1 -ReleaseCandidate` passes (21 placeholders gone: legal name,
      privacy contact, effective date, LGPL mailbox/source URL).
- [ ] Privacy policy live at the HTTPS URL, page text identical to the in-app asset (EN + AR).
- [ ] AAB built from the tagged commit with the **upload key**, installed once on the arm64 phone.
- [ ] The demo video was recorded on that same tagged build.
- [ ] Listing texts and screenshots show no unimplemented feature; format counts match the build.
- [ ] The Data-safety answers above still match the uploaded APK's dependencies (re-check before
      every upload: any new SDK can change the answer).
- [ ] Target audience, content rating and ads answers match the listing and the policy.
- [ ] The closed-test track is prepared with 15 tester emails (12+ needed for 14 days).

## 7. Still owned by the publisher (not the builder)

Legal name, privacy contact email and effective date; the domain + HTTPS hosting for the policy;
`opensource@` mailbox and the LGPL source tarball URL/SHA-256; the Play account and upload key;
counsel packet or a signed risk acceptance; the arm64 device; the app listing assets; testers.
See `docs/plan/publisher-guide.md` Appendix A for the value-to-place mapping.
