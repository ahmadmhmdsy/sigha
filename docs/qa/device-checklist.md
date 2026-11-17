# Sigha — physical-device QA checklist (0.2.1)

The automated gate (`scripts/verify.ps1` + `scripts/device-smoke.ps1`) proves the build, the
tests, packaging, signing and a crash-free cold start on an emulator. It cannot prove that real
conversions, SAF grants, notifications and RTL rendering work on hardware. This checklist is that
remaining gate: run it on at least one physical phone before uploading to Play, and paste the
results into `docs/qa/verification.md` (or a new dated report).

**Test device(s):** model, Android version, ABI, language. Minimum: one arm64 phone on Android 14+
in English, one in Arabic (RTL). If an API 26–28 device is available, run the legacy publishing
rows too.

## A. Install and first run

| # | Step | Expected |
|---|---|---|
| A1 | Install the release APK (`app-arm64-v8a-release.apk`) with `adb install` | installs; icon labelled Sigha / صيغة |
| A2 | Launch cold | workbench appears < 3 s, no crash, tagline "Convert on your phone. Files are not uploaded." |
| A3 | Notification permission prompt | appears once; denying it must not block conversions |
| A4 | Rotate the screen on an empty workbench | no crash, layout intact |
| A5 | Switch system language to Arabic | whole UI RTL, no clipped text, no English-only labels |

## B. Conversions (one row per engine family)

For each: select the input, choose the target, convert, confirm the output opens and the queue
shows success. Record output size and any note shown.

| # | Input → target | What to check |
|---|---|---|
| B1 | JPG → PNG | output in Downloads, opens in Gallery |
| B2 | PNG → JPG | EXIF orientation respected, no metadata carried over |
| B3 | HEIC → JPG (Android 9+) | converts; on older devices a clear limit message |
| B4 | multi-page PDF → PNG | one file per page, no overwrite |
| B5 | text PDF → TXT | text matches the visible page |
| B6 | **scanned PDF → TXT** | either OCR text, or the "install OCR pack" message — never an empty file |
| B7 | DOCX → PDF | text present, note about extraction shown |
| B8 | XLSX → CSV | first worksheet, note about row cap if hit |
| B9 | MP4 → MP3 | plays; no metadata (GPS/device) in output |
| B10 | MP4 → GIF | completes; timeout message if a very long file |
| B11 | ZIP → unzip | files extracted to a folder, entry/size caps enforced |
| B12 | 7z → ZIP | archive valid |
| B13 | EPUB → TXT | chapters concatenated; DRM book refused with a clear message |
| B14 | SRT → VTT | timestamps correct |
| B15 | JSON → CSV | nested values handled, note if flattened |
| B16 | JPG → TXT **without** OCR pack | target hidden or a clear "pack required" message |
| B17 | Start a conversion, press Cancel | stops promptly, no partial output left in Downloads |
| B18 | Convert 20 files at once | queue processes all, UI stays responsive |

## C. Model pack (OCR)

| # | Step | Expected |
|---|---|---|
| C1 | Open Model packs → OCR | size (≈28 MB) and licence shown |
| C2 | Tap Download on mobile data | confirmation dialog warns about metered connection |
| C3 | Start download, press Cancel | stops; reopening shows partial state, not "installed" |
| C4 | Download again | resumes instead of restarting from 0 (watch the progress) |
| C5 | Finish download | "Installed"; B6/B16 now work without a network |
| C6 | Turn on airplane mode and run B6 | OCR works offline |
| C7 | Delete app data / reinstall | pack is gone, catalogue still lists it |

## D. Output destinations

| # | Step | Expected |
|---|---|---|
| D1 | Settings → Output → Downloads, convert | file appears in Downloads/Sigha |
| D2 | Settings → Output → choose folder (SD card or Drive), convert | file appears in that folder |
| D3 | Revoke/remove the folder grant, convert again | conversion still succeeds, falls back to Downloads with a note |
| D4 | Choose a read-only folder in the picker | refused with "cannot be written to" message |
| D5 | Convert with a full/near-full storage | clear failure, no zero-byte file |

## E. Background execution and notifications

| # | Step | Expected |
|---|---|---|
| E1 | Start a batch, press Home | conversion continues, progress notification visible |
| E2 | Open the notification and tap Cancel | batch stops, notification clears |
| E3 | Force-stop the app mid-batch, relaunch | "previous batch was interrupted" notice, no phantom success |
| E4 | Convert in split-screen / picture-in-picture | no crash, progress stays accurate |

## F. History, queue and settings

| # | Step | Expected |
|---|---|---|
| F1 | Open History | past conversions listed with localized failure headlines; details expandable |
| F2 | Tap a history output | opens in the right app or shows a clear message |
| F3 | Clear history | list empties, survives restart |
| F4 | Change theme (system/light/dark) | applied everywhere, survives restart |
| F5 | Settings → Privacy | policy renders, links tappable |
| F6 | Settings → Licences | grouped licences, long texts expand, Arabic locale shows Arabic titles |

## G. Legacy paths (only if an API 26–28 device is available)

| # | Step | Expected |
|---|---|---|
| G1 | First conversion to Downloads on API 26–28 | storage permission asked once, then file appears |
| G2 | Deny storage permission, convert | localized explanation, no crash |

## H. Sign-off

- [ ] All rows above pass, or every failure has a filed issue with device/Android version.
- [ ] No ANR and no crash in `adb logcat -b crash` during the run.
- [ ] Results recorded in `docs/qa/verification.md` with device details and date.
- [ ] Only then: Play closed test upload (human tasks B1–B3 in `docs/legal/release-blockers.md`).
