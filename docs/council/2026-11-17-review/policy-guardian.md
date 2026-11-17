# Policy Guardian / Model Auditor — Sigha (صيغة) council review

- **Seat:** Policy Guardian (halal / legal / Play policy / security) + Model Auditor (qiyas: *fāsid al-iʿtibār* — invalid premise in the legal/licence claims)
- **Date:** 2026-11-17 (Asia/Baghdad)
- **Scope read:** `NOTICE.md`, `README.md`, `docs/legal/clearance-memo.md` (conclusions + open items), `app/build.gradle.kts`, `app/src/main/AndroidManifest.xml`, `model/PackInstaller.kt`, `model/PackCatalog.kt`, `model/PackLayout.kt`, `model/Digest.kt`, `ui/LicensesScreen.kt`, `ui/AboutScreen.kt`, `ui/Intents.kt`, `data/IncomingFilesBus.kt`, `res/values/strings.xml`, `res/values-ar/strings.xml`, `docs/qa/verification.md`, `docs/research/android-stack.md`
- **Method:** read files; dumped the built APK with `System.IO.Compression` + `aapt2 dump xmltree/badging`; recomputed the APK SHA-256; ran `zipalign -c -P 16 4`; inspected the resolved Gradle metadata (`dev.ffmpegkit-maintained` POM in the Gradle cache, lint dependency model); fetched licence pages and the four Play policy pages over HTTPS with `Invoke-WebRequest`. **Not run:** `:app:testDebugUnitTest` (131-test claim taken from `docs/qa/verification.md`), any device/emulator test, any conversion, ELF `readelf` check (no NDK/readelf on this host).

---

## Verdict

Sigha is honestly *engineered* for privacy — no analytics, no crash SDK, no logging calls, `allowBackup=false`, SAF instead of All-files access, and a model-pack downloader that pins SHA-256, enforces size, installs atomically into app-private storage and never logs a path. That is unusually good and should be said plainly. But the project is **not licence- or policy-clean as a distributable artefact**, and two of its own claims are false *today*: `README.md:59` says "notices and relinking information are published" when no relinking/installation information exists anywhere in the repo or the APK, and the shipped OSS list names ONNX Runtime (removed) while omitting at least ten components that are actually inside the 72 MB APK — including an LGPL-2.1 `libsndfile` and a Cisco OpenH264 build whose patent licence does not travel with a recompiled binary. There is no privacy policy anywhere (Play requires one on the listing *and* inside the app even for data-free apps), no Data-safety answer sheet, and the clearance memo's own blocking conditions C4/C9/C10 are unmet while the README presents the app as release-ready apart from signing. The halal/legal red lines are, however, respected: no GPL/AGPL text or engine is in the APK, no All-files access, no model weights shipped, no Filee text/asset in shipped UI. Verdict: **ship-blocked on compliance paperwork and one false claim; the security posture itself is sound.**

---

## Findings

### F1 — **Critical** — LGPL-3.0 §4 duties are not met in the shipped artefact, and the README says they are
**Evidence.** `README.md:59`: "FFmpegKit is LGPL-3.0 — notices and relinking information are published." Reality:
- `NOTICE.md:6-21` is a bare table with GitHub homepages — no Corresponding Source for the exact 8.1.9 binaries, no build flags/scripts, no written offer, no relink/installation instructions.
- `ui/LicensesScreen.kt:29-43` lists name+licence only; there is **no URL field and no clickable source link**, and `COMPONENTS` never mentions FFmpeg source or relinking.
- `res/values/strings.xml:178-181`: "Full license texts are available from each project" and "…see the project pages for the full texts and source links" — in an app whose licences screen has no links at all.
- The only source offer physically shipped is inherited from the AAR: `res/raw/source.txt` inside the APK says the source "can be downloaded from https://github.com/arthenica/ffmpeg-kit/wiki/Source page" and to mail `open-source@arthenica.com` — a **retired project** (the POM of the actual dependency points to `github.com/ffmpegkit-maintained/ffmpeg`). `docs/research/android-stack.md:299` even flagged "⚠ The AAR's `res/raw/source.txt` still points to the retired Arthenica wiki — use the fork URL"; the warning was never acted on.
- No code references `R.raw.license*` or `R.raw.source` (grep across `app/src/main/java`: 0 hits), so the LGPL-3.0 text that *is* in the resource table is unreachable to users.
- `docs/legal/clearance-memo.md:335` (condition C5) already required "notices + licence texts + FFmpeg source offer + relink/installation information; counsel sign-off on LGPL-3.0 §4(d)/(e)".

**Why it matters.** LGPL-3.0 §4 requires, on conveyance of a Combined Work: prominent notice, a copy of the licence, and either Minimal Corresponding Source + relinkable application code (§4(d)(0)) or a "suitable shared library mechanism" (§4(d)(1)), plus Installation Information for a User Product (§4(e)). A signed APK gives no way for a user to replace the `.so` without re-signing, so the Android-specific argument has to be *written down and defended*, not assumed. Right now a recipient in the EU (or an OSS compliance scanner) can point to a redistributed LGPL library with no source offer: that is a licence violation, and the repo's own claim makes it look deliberate.
**Fix.** (a) Correct `README.md:59` immediately. (b) Add `app/src/main/assets/licenses/` containing the LGPL-3.0 and GPL-3.0 texts, the fork's `LICENSE`/`THIRD-PARTY-NOTICES`, all 32 `license_*.txt` from the AAR, and a written FFmpeg/FFmpegKit source offer naming commit/tag `8.1.9` and the fork repo URL + how the APK's exact build can be reproduced. (c) Turn `LicensesScreen` into a real document reader (assets → screen), with a "Replace this library" paragraph. (d) Obtain the counsel sign-off C5 demands, or drop FFmpegKit for Media3 (memo Q3 already recommended Media3-first).

### F2 — **High** — The attribution list is both wrong and incomplete (phantom ONNX; ≥10 shipped components missing; Apache NOTICE files stripped)
**Evidence.**
- `NOTICE.md:17` and `ui/LicensesScreen.kt:35` list **ONNX Runtime (MIT)**. It is not in `app/build.gradle.kts`, not in the APK (`lib/*` has no `libonnxruntime*`; zip-name scan for `onnx` → 0 entries), and `grep onnx|ai.onnxruntime app/src` → 0 hits. `docs/qa/verification.md:51-54,128` correctly says it was removed — the two user-facing lists were never updated.
- The APK ships `lib/arm64-v8a/libleptonica.so` (BSD-2-Clause), `libjpeg.so` (IJG terms), `libpngx.so` (libpng licence), plus the ffmpeg-kit statically-linked set evidenced by 32 `res/raw/license_*.txt`: `license_openh264.txt`, `license_libsndfile.txt` (**LGPL-2.1** text, 26 KB), `license_libaom.txt`, `license_dav1d.txt`, `license_kvazaar.txt`, `license_libvpx.txt`, `license_libass.txt`, `license_fontconfig.txt`, `license_libxml2.txt`, `license_sdl.txt` … **none of these appear in NOTICE.md**.
- Resolved dependency set (from Gradle's lint model) includes components absent from NOTICE: `com.squareup.okhttp3:okhttp:4.12.0`, `okio`, `org.bouncycastle:bcprov/bcutil/bcpkix-jdk15to18:1.72` (Bouncy Castle licence — a notice-bearing licence), `commons-codec`, `commons-io`, `commons-lang3`, `com.arthenica:smart-exception-java/common:0.2.1` (licence **unknown**: its GitHub LICENSE URL returns HTTP 404), Room/WorkManager/navigation/DataStore/profileinstaller/emoji2, and PDFBox's `assets/…/ttf/LiberationSans-Regular.ttf` (likely SIL OFL-1.1 — **unverified**).
- `app/build.gradle.kts:42` excludes `/META-INF/{AL2.0,LGPL2.1,LICENSE.txt,NOTICE.txt,LICENSE.md,NOTICE.md}` and `/META-INF/DEPENDENCIES` from **every** dependency. Apache-2.0 §4(d) requires the NOTICE contents to be retained; the APK contains no `META-INF /*NOTICE*` entries at all (only `okhttp3/internal/publicsuffix/NOTICE`).
- In-app licence text contradicts the repo: `NOTICE.md:11` says "FFmpeg | LGPL-2.1-or-later", while `res/values/strings.xml:181` says "FFmpeg is licensed under LGPL-3.0".

**Why it matters.** Wrong attribution is worse than none: it is a false statement about a third party's rights (a *fāsid iʿtibār* in the licensing qiyās — the whole "permissive and correctly noticed" premise fails at the first component that is missing), it breaks the BSD-2/BSD-3/IJG/OFL notice conditions, and Play's IP policy requires that all content be "either your own original work or that you have the necessary licenses". Bouncy Castle and libsndfile are the two most likely to be flagged by a real OSS audit.
**Fix.** Generate `NOTICE.md` and the Licenses screen from a single machine-readable source (JSON/YAML) built from the resolved dependency graph + the AAR's `res/raw/license_*.txt`; drop ONNX; add leptonica/libjpeg/libpng/ffmpeg-kit externals/Bouncy Castle/okhttp/okio/commons-*/smart-exception/fontbox; stop excluding `META-INF/NOTICE.txt`; add a unit test that fails when a resolved dependency is missing from the notice data.

### F3 — **High** — No privacy policy exists at all, though Play requires one on the listing *and inside the app*; the memo's C10 disclosure is unmet
**Evidence.**
- `glob docs/**/*privacy*` → **0 files**; no privacy screen in `ui/`; `SighaNavHost.kt` has no such route; the string `settings_privacy` ("Privacy") exists at `res/values/strings.xml:124` and `res/values-ar/strings.xml:124` but is referenced by **no** code (grep: only the two resources) — a planned surface that was never built.
- Play (verified 2026-11-17, <https://support.google.com/googleplay/android-developer/answer/9859455>): *"You must link to a privacy policy on your store listing and within your app, even if your app doesn't access personal or sensitive user data or target children."* Data safety (verified, <https://support.google.com/googleplay/android-developer/answer/10787469>): *"Even developers with apps that do not collect any user data must complete this form and provide a link to their privacy policy."*
- `docs/legal/clearance-memo.md:338` (C8) and `:340` (C10) — "Data safety form + privacy policy before submission"; "…disclose the download host in the privacy policy" — neither exists. Q6 ("model hosting: GitHub Releases vs own server? Affects Data safety/privacy declarations") is still open, yet the code already hard-codes GitHub raw content (`PackCatalog.kt:13-14`).

**Why it matters.** This is a hard Play Console blocker (the listing cannot be completed without a policy URL) and a trust failure for a privacy-first product: the app says "Your files stay yours" (`strings.xml:170-171`) but has no document stating what is and is not done with data.
**Fix.** Write and host a short, specific privacy policy: files never leave the device; no analytics/ads/crash SDKs; the only network request is a user-initiated HTTPS GET to `raw.githubusercontent.com` (Tesseract tessdata, 28,004,325 B, commit-pinned, SHA-256 verified) which exposes the user's IP to GitHub; state retention/deletion (packs in app-private storage, uninstalled with the app; no history upload). Link it from Settings (reuse `settings_privacy`) and from the About screen; put the URL in Play Console; then answer Data safety as "no data collected/shared" and be ready to justify the download exception.

### F4 — **High** — "Fully offline / nothing leaves your phone" as shipped copy + Play's Deceptive Behavior policy = listing risk; the offline-truth test was never run
**Evidence.** `res/values/strings.xml:6` "Convert offline. Nothing leaves your phone."; `:168-169` "Works fully offline"; `:41` "Nothing is uploaded"; `README.md:3,5` "privacy-first, fully offline". Meanwhile `AndroidManifest.xml:4-6` declares INTERNET + ACCESS_NETWORK_STATE and `PackInstaller.kt` performs real HTTPS downloads. Play's Deceptive Behavior policy (verified 2026-11-17, <https://support.google.com/googleplay/android-developer/answer/9888077>): *"All metadata, including the store listing, screenshots, and title, must precisely reflect the app's functionality."* The memo already made this a release blocker: `docs/legal/clearance-memo.md:339` (C9) "Offline truth. Network off must still convert… Test this before release." That test has **not** been run (`docs/qa/verification.md:151-154`: "Device verification: not done").
**Why it matters.** A one-star "it asks for internet" or a reviewer reading "fully offline" next to an INTERNET permission is exactly the metadata-accuracy trap; the fix is cheap text, the failure is a policy strike on the whole account.
**Fix.** Keep "conversion happens on your device" as the claim; never "fully offline" unqualified. Disclose the download in the first screen where it matters (already partly done: `strings.xml:147-148`) and in the store description ("one optional 28 MB OCR pack downloads once"). Run the network-off test on a device and record it in `docs/qa/verification.md`.

### F5 — **Medium-High** — Model-pack supply chain: strong integrity, weak provenance for the *engine* and no dependency verification
**Evidence (strengths first).** `PackCatalog.kt:13-14,19,26`: URLs pinned to tessdata_best commit `e12c65a9…`, SHA-256 per file; `PackInstaller.kt:157-164` verifies length + SHA-256 before install; `:166-182` replaces atomically (rename, fallback copy), deletes partials in `finally` (`:53-55`); `PackLayout.kt:18` writes under `context.filesDir/models`; redirects capped at 5 and only 3xx followed (`:97-125`). No `Log`/`println` anywhere in `app/src/main/java` (grep: 0 hits) → no file path/URI leakage. A MITM or compromised `raw.githubusercontent.com` cannot deliver arbitrary content because the digest is in the APK. That is genuinely solid.
Gaps: (a) the redirect handler never checks the scheme/host of `Location` (`PackInstaller.kt:112-119`); it is only saved by the platform's targetSdk-28+ cleartext default and TLS validation — make it explicit (`https` only, host allow-list). (b) `PackLayout.isInstalled/isFileValid` (`:40-58`) validates **size only** after install, so post-install corruption/tampering is not re-detected (low risk in app-private storage, but a re-hash on first use per session is cheap). (c) Nothing pins the **engine** artefact: `app/build.gradle.kts:87` uses `dev.ffmpegkit-maintained:ffmpeg-kit-full:8.1.9`, whose POM declares a single maintainer ("Jokobee", `jokobee.com`, `inceptionYear 2026`) — a community republish of the retired Arthenica line, not a foundation-backed project; there is **no** Gradle dependency-verification metadata in the repo, and `settings.gradle.kts:19` adds JitPack globally (unused by any current coordinate — the Tesseract artifact resolves from Maven Central despite the "JitPack build" comment at `app/build.gradle.kts:92`). (d) `res/raw/license_openh264.txt` indicates OpenH264 is compiled into the build; Cisco's royalty-free AVC patent licence covers *"this binary"* from Cisco (verified: <https://www.openh264.org/BINARY_LICENSE.txt>), not a third-party recompilation — an unresolved **patent** question (presence inferred, not probed: **UNVERIFIED**). (e) `com.arthenica:smart-exception-java:0.2.1` is a transitive dependency with an unknown licence (404 on its LICENSE).
**Why it matters.** The download path is audited but the *code that does the converting* is a third-party rebuild with unknown build flags and one maintainer; a licence/patch compromise there is invisible to the code review, and "no GPL" is only the POM's word (no `license_x264/x265` texts are shipped, which is consistent, but build flags cannot be verified from the APK).
**Fix.** Add `gradle/verification-metadata.xml` (`--write-verification-metadata sha256,pgp`) and fail on dynamic versions; delete the JitPack repository; vendor the fork's `LICENSE`/`THIRD-PARTY-NOTICES`/source commit into `docs/legal/sources` with its SHA-256; ask the fork maintainer for build flags/provenance or build FFmpeg yourself for release; probe `-encoders` once on a device and record `openh264/h264_mediacodec` results; resolve the smart-exception licence in NOTICE.

### F6 — **Medium** — Name/trademark clearance is still open, but the repo and UI treat "Sigha" as final
**Evidence.** `docs/legal/clearance-memo.md:317-323`: "Sigha (صيغة…) — No direct Play app named 'Sigha' found in a quick screen; **not proof of availability**, and it is a common word so collisions in other classes are likely | **Preferred candidate, pending search**"; `:346-350` Q5 "choose after Iraqi trademark search + Play availability"; `:390` "Name screen is not a trademark search"; `:201` B21 "our own mark only after a trademark search". Yet `README.md:1`, `strings.xml:5,125`, `settings.gradle.kts:22`, the package id and the icon all ship the un-cleared name.
**Why it matters.** Play IP/impersonation review plus Iraqi Law 21/1957 (§5.8 of the memo) — and a rebrand after release (package id, store listing, screenshots) is expensive. Naming Filee in *code comments* only (`core/RoutePlanner.kt:7`, `ui/theme/Color.kt:5`) is acceptable (not shipped), and no Filee string/asset is in the APK (verified: grep hits are comments only, and nothing under `res/` or `assets/` references Filee) — memo C2 is respected in the shipped artefact.
**Fix.** Before any public artefact beyond a private build: Iraqi registry + Play name search, decide on a coined/compound mark or register the word mark, and add a `docs/legal/name-clearance.md` recording the search. Keep "Filee" out of the store listing, icon, screenshots and in-app text (a factual "inspired by Filee" sentence belongs in the repo README only, if at all).

### F7 — **Medium** — Manifest/packaging contradictions and dead background stack: 4 permissions for work the app does not do; `extractNativeLibs` contradicts the packaging config
**Evidence.**
- `AndroidManifest.xml:12` sets `android:extractNativeLibs="true"`, and the built APK confirms `extractNativeLibs=true` (`aapt2 dump xmltree`), while `app/build.gradle.kts:47` sets `jniLibs { useLegacyPackaging = false }` and `docs/qa/verification.md:46` claims "uncompressed, aligned native libs". The two mechanisms disagree; `extractNativeLibs=true` means libs are extracted at install and the in-APK alignment is not what loads them.
- The merged manifest carries `WAKE_LOCK`, `RECEIVE_BOOT_COMPLETED`, `FOREGROUND_SERVICE` and `POST_NOTIFICATIONS`, all inherited from `androidx.work:work-runtime:2.10.0` (`app/build.gradle.kts:74`), but no `Worker`/`CoroutineWorker`/`setForeground`/`NotificationManager` use exists anywhere in `app/src/main/java` (grep: 0 hits). Conversions run in `work/ConversionQueue.kt:83` on an `AppScope` coroutine, so a long conversion dies with the process and the user is never notified.
- No `android:enableOnBackInvokedCallback`; for targetSdk 36 on Android 16 predictive back animations are **on by default** and *"onBackPressed is not called and KeyEvent.KEYCODE_BACK is not dispatched anymore"* (verified 2026-11-17, <https://developer.android.com/about/versions/16/behavior-changes-16>) — system-back behaviour in the Compose app is therefore unverified (device QA not done).
**Why it matters.** Permissions shown on the store listing must be justified; background conversion without a foreground service is a real user-facing failure for large media; and the 16 KB claim is being read as stronger than what was actually tested (zipalign checks zip page alignment, not ELF `PT_LOAD` alignment — I re-ran `zipalign -c -P 16 4` myself: exit 0; the ELF check is **not run**).
**Fix.** Either implement WorkManager + `setForeground` with `foregroundServiceType="dataSync"` + `POST_NOTIFICATIONS` runtime request (and declare the FGS type in the manifest), or remove work-runtime and its inherited permissions. Set `android:extractNativeLibs="false"` (or remove the attribute and let AGP decide) and re-assert 16 KB with `readelf -l`/`objdump -p` on both ABIs. Add `enableOnBackInvokedCallback="true"` after testing back navigation on API 33–36.

### F8 — **Low** — The release gate the memo demanded does not exist; lint is switched off
**Evidence.** `app/build.gradle.kts:50-53`: `lint { abortOnError = false; checkReleaseBuilds = false }`. `docs/legal/clearance-memo.md:334` (C4) requires "a build-time and release check that fails if the app contains GPL/AGPL components… Keep the licence manifest in the repo". No such task/test/script exists (no `LICENSES/`, nothing in `tools/` except a stray full Gradle **8.9** distribution and tessdata binaries while the wrapper is 8.11.1), and `README.md:51-52` merely asserts the result.
**Why it matters.** Compliance that depends on a human remembering is not compliance; the next dependency bump can silently pull GPL or a new licence.
**Fix.** Add a Gradle task + a JVM test that (i) fails on any artifact in a deny-list of GPL/AGPL groups, (ii) diffs the resolved dependency set against `NOTICE` data, (iii) fails on a permission not in an allow-list; enable `lint.abortOnError = true` for release (or run `lintVitalRelease` in CI); delete `tools/gradle-8.9*`.

### F9 — **Low** — Distribution metadata is not release-ready (expected, but must be on the checklist)
**Evidence.** `app/build.gradle.kts:16-17` versionCode 1 / 0.1.0; `:22-26` release type has no `signingConfig` and `isMinifyEnabled = false`; the built APK is `android:debuggable=true`; only an APK is produced (Play requires an AAB for new apps); `abiFilters` ships arm64-v8a + x86_64 (72.5 MB debug APK). Play also requires the export-compliance/encryption self-classification for an app that uses HTTPS.
**Fix.** Add a release signing config from environment/keystore (never in repo), `bundleRelease` with an ABI/asset split, R8 on, and record the resulting AAB size/hash; complete the Play "App content" declarations incl. the US export self-classification.

### F10 — **Low** — Halal/legal red lines: all kept (verified) — one open item is model-weights provenance for the "coming soon" upscaler
**Evidence (kept).** No GPL/AGPL engine or licence text in the APK (no x264/x265/ghostscript/pandoc/calibre; the ffmpeg-kit build ships no GPL texts and the POM says "no GPL"); no `MANAGE_EXTERNAL_STORAGE`/All-files access (only `WRITE_EXTERNAL_STORAGE maxSdkVersion=28`, requested at runtime for the legacy Downloads path only — `WorkbenchScreen.kt:114,131`); no model weights in the APK (`res/`+assets listing: none); `allowBackup="false"`; no account, ads, analytics or crash SDK (resolved dependency set has none); no Filee expression in shipped UI.
**Open.** `NOTICE.md:28` promises a future Real-ESRGAN (BSD-3) pack; `docs/research/model-licenses.md` must gate it before any download is offered (memo C7). The upscale entry is correctly disabled (`model_packs_upscale_coming_soon`), so nothing is violated today — keep the allow-list gate.

---

## Licence matrix — "component | NOTICE says | actual | ok?"

| Component (shipped) | NOTICE.md says | Actual (verified where stated) | OK? |
|---|---|---|---|
| FFmpegKit (fork) | LGPL-3.0, fork URL | POM `dev.ffmpegkit-maintained:ffmpeg-kit-full:8.1.9` declares LGPL-3.0; real project, single maintainer; AAR embeds Arthenica notices + **stale** `source.txt` | ⚠ licence right, source/relink not provided |
| FFmpeg | LGPL-2.1-or-later, "without x264/x265" | No GPL licence texts in APK; POM says "no GPL"; build flags **not verifiable** from APK | ⚠ unverified build flags |
| ffmpeg-kit bundled externals (openh264, libsndfile, libaom, dav1d, kvazaar, libvpx, libass, freetype, fontconfig, harfbuzz, libxml2, sdl, soxr, speex, opus, lame, twolame, theora, vorbis, snappy, zimg, shine, ilbc, opencore-amr, wavpack…) | **not listed** | 32 `res/raw/license_*.txt` present; **libsndfile is LGPL-2.1**; OpenH264 BSD + Cisco patent licence for Cisco binaries only | ✗ |
| PDFBox-Android / FontBox | Apache-2.0 | Apache-2.0 (POM); ships `LiberationSans-Regular.ttf` (likely SIL OFL-1.1 — **unverified**) | ⚠ add font notice |
| Commons Compress | Apache-2.0 | Apache-2.0 | ✓ (but its NOTICE is stripped) |
| XZ for Java | Public domain | Public domain | ✓ |
| Tesseract4Android | Apache-2.0 | Apache-2.0 (LICENSE fetched, HTTP 200, <https://github.com/adaptech-cz/Tesseract4Android>) | ✓ |
| Tesseract native / Leptonica / libjpeg / libpng | Tesseract only (Apache-2.0) | `libtesseract.so` Apache-2.0; `libleptonica.so` **BSD-2**, `libjpeg.so` **IJG**, `libpngx.so` **libpng** — not listed | ✗ |
| ONNX Runtime | MIT (shipped) | **Absent** from APK and dependency set | ✗ phantom |
| AndroidX/Compose/Material3, Kotlin, Coil, DataStore, DocumentFile, ExifInterface | Apache-2.0 | Apache-2.0 | ✓ (but okhttp/okio, Room, WorkManager, navigation, bouncycastle, commons-io/codec/lang3, smart-exception are shipped and unlisted) |
| Bouncy Castle 1.72 (bcprov/bcutil/bcpkix) | **not listed** | Bouncy Castle Licence (MIT-style, notice required) | ✗ |
| smart-exception-java/common 0.2.1 | **not listed** | Licence **unknown** (LICENSE URL 404) | ✗ resolve |
| tessdata_best eng+ara pack | Apache-2.0, download-only | Apache-2.0, commit-pinned + SHA-256 (28,004,325 B = 15,400,601 + 12,603,724 ✓) | ✓ |
| Real-ESRGAN (future) | BSD-3 | Not shipped | ✓ (gate before shipping) |

---

## Claims checked vs actual

| Claim (source) | Actual | Verdict |
|---|---|---|
| APK 72,522,602 B, SHA-256 `8922614F…8266` (`README.md:20`, `docs/qa/verification.md:37-38`) | Re-computed: **72,522,602 B**, **8922614FD9F06FD1C3E30F0D957D533E7CBCC8EFAA26064C744EF5A8BD9E8266** | **VERIFIED** |
| targetSdk 36 / minSdk 26 / `com.sigha.converter` / vc 1 | `aapt2 dump badging/xmltree`: target 36, min 26, package + version match | **VERIFIED** |
| ABIs arm64-v8a + x86_64 | APK: 16 `.so` under `lib/arm64-v8a`, 16 under `lib/x86_64` (32) | **VERIFIED** |
| 16 KB aligned (`docs/qa/verification.md:45`) | `zipalign -c -P 16 4` → exit 0. This proves **zip** page alignment, not ELF 16 KB segment alignment; ELF check not run (no NDK/readelf here) | **PARTLY VERIFIED / overstated scope** |
| "ONNX Runtime removed; no ONNX code remains" (`docs/qa/verification.md:51-54`) | True in code/APK; but `NOTICE.md:17` and `LicensesScreen.kt:35` still list it | **TRUE in build, FALSE in user-facing lists** |
| "notices and relinking information are published" (`README.md:59`) | No relink/installation info anywhere; no source offer for the shipped build; shipped `source.txt` points to a retired project | **FALSE** |
| "FFmpeg is licensed under LGPL-3.0" (`strings.xml:181`) | FFmpeg core is LGPL-2.1-or-later (`NOTICE.md:11`); FFmpegKit wrapper is LGPL-3.0 | **FALSE (imprecise)** |
| "Full license texts are available from each project" / "see the project pages" (`strings.xml:179-181`) | Licences screen has **no links** and no texts; offline app | **MISLEADING** |
| "Fully offline" / "Nothing leaves your phone" (`README.md:3,5`; `strings.xml:6,168`) | Files are never uploaded (no upload code) ✓, but INTERNET is declared and used for pack downloads; Play metadata must "precisely reflect functionality" | **RISKY METADATA** — fix listing copy |
| "131 JVM tests green" | **Not run** by me; documented in `docs/qa/verification.md:58` | not run |
| "Device QA not done" | Matches `docs/qa/verification.md:151-154` | **VERIFIED (honest)** |
| "No analytics / no account / no ads" | Resolved dependency set has no analytics/ads/crash SDK; no `Log` calls; `allowBackup=false` | **VERIFIED** |
| Model pack "SHA-256 pinned" (`README.md:19`) | `PackCatalog.kt:19,26` pins 64-hex digests; `PackInstaller.kt:157-164` enforces; `PackCatalogTest` asserts https + commit pin | **VERIFIED** |
| "No Filee code, brand, logo, screenshots or trade dress" (`README.md:58-59`, `NOTICE.md:41-43`) | No Filee strings/assets in `app/src/main/res` or APK; only 2 source **comments** mention it | **VERIFIED (comments only)** |

---

## Play submission readiness — tool-verifiable vs human-only

**Tool-verifiable (can be automated before upload)**
1. targetSdk ≥ 36 ✔ (verified) — Play requires API 36 for new apps/updates since **31 Aug 2026** (verified, <https://developer.android.com/google/play/requirements/target-sdk>).
2. Release AAB with release signing + R8 + ABI split — **missing** (no signingConfig, `isMinifyEnabled=false`).
3. Manifest minimality: drop unused WorkManager permissions or implement FGS; justify INTERNET; `extractNativeLibs` fixed; no All-files access ✔ (verified absent).
4. 16 KB ELF check on each ABI (`readelf -l` `LOAD … 0x4000`) — **not run**.
5. GPL/AGPL + licence-inventory gate (memo C4) — **missing**; `lint.abortOnError=false` — fix.
6. NOTICE ↔ resolved-dependency diff test; Apache NOTICE retention — **failing** (F2).
7. Privacy policy asset + in-app route (`settigs_privacy` target) and Data-safety mapping — **missing** (F3).
8. Dependency verification metadata; remove JitPack; pin fork provenance — **missing** (F5).
9. Exported `MainActivity` SEND/VIEW `*/*` filters: they are intended (share/open-with) and `IncomingFilesBus.kt:44-63` handles `clipData` and `ACTION_VIEW`; no `<queries>` needed because the app never calls `resolveActivity` (verified: `Intents.kt:36-43` uses `startActivity` in try/catch) ✔.

**Human-only**
1. **Closed testing**: personal accounts created after 13 Nov 2023 need "a minimum of **12 testers** … opted in continuously for at least **14 days**" before production access (verified, <https://support.google.com/googleplay/android-developer/answer/14151465>) — not started.
2. Privacy policy hosted at a public URL + entered in Play Console; Data-safety form (mandatory even with no data).
3. App-content declarations: ads (no), target audience, sensitive-permission justification (notifications/storage), export-compliance self-classification.
4. Store assets: 512×512 icon, 1024×500 feature graphic, screenshots, short/full description **without** "fully offline" and **without** the Filee name.
5. Trademark/name search (Iraqi registry + Play availability) and go/no-go on "Sigha"; register if commercial.
6. LGPL-3.0 §4(d)/(e) counsel sign-off, or the Media3 decision (memo Q3/C5).
7. Device QA on API 26–36 including network-off conversion, SAF folder grant, MediaStore saves on API 26–28, pack download on a real network.
8. Model-pack hosting decision (Q6) and, for any future pack, licence+hash review (C7).
9. Developer-account identity/verification and organization vs individual decision (memo Q8).

---

## False / misleading claims (verbatim)

1. `README.md:59` — **"FFmpegKit is LGPL-3.0 — notices and relinking information are published."** Published where? Nothing in `NOTICE.md`, no URL in `LicensesScreen`, and the only shipped source text (`res/raw/source.txt`) points at the retired Arthenica wiki. **False.**
2. `NOTICE.md:17` — **"| ONNX Runtime (Android) | MIT | …"** under the heading **"Runtime libraries (shipped in the APK)"**. It is not in the APK. **False.**
3. `ui/LicensesScreen.kt:35` — **`LicenseLine("ONNX Runtime", "MIT")`** shown in-app. **False.**
4. `res/values/strings.xml:181` — **"This app includes FFmpegKit (LGPL-3.0). FFmpeg is licensed under LGPL-3.0; see the project pages for the full texts and source links."** FFmpeg core is LGPL-2.1-or-later, and there are no links. **False/imprecise.**
5. `res/values/strings.xml:179` — **"Full license texts are available from each project."** Not from the app, which is offline and lists no URLs. **Misleading.**
6. `res/values/strings.xml:6` — **"Convert offline. Nothing leaves your phone."** together with a declared INTERNET permission and a 28 MB download. The sentence is about files (true) but reads as an absolute offline claim; Play requires metadata to reflect functionality precisely. **Risky.**
7. `docs/qa/verification.md:45` — **"16 KB page alignment | `zipalign -c -P 16 4` → exit 0 (pass)"** is presented as the 16 KB requirement; zipalign only checks zip alignment, and `extractNativeLibs="true"` in the shipped manifest contradicts the "uncompressed native libs" framing at `:46`. **Overstated.**

---

## Top-5 prioritized actions

1. **Fix the LGPL claim and build the compliance pack (F1 + F2).** Ship `assets/licenses/` (LGPL-3.0+GPL-3.0 texts, all 32 AAR licence texts, fork LICENSE/NOTICES, written FFmpeg/FFmpegKit source offer naming build 8.1.9 and how to reproduce it, plus a "replace the library" paragraph), read it from `LicensesScreen`, regenerate `NOTICE.md` from the resolved dependency graph, delete the ONNX rows, correct `README.md:59` and `strings.xml:181`. Without this the app is not distributable in the EU and the repo statement is false.
2. **Write the privacy policy and the Data-safety answers, then align the copy (F3 + F4).** Host it, link it in Settings/About (wire the unused `settings_privacy` string), state the GitHub download + IP exposure, declare "no data collected/shared", and change "fully offline / nothing leaves your phone" to "conversion runs on your device".
3. **Automate the licence/permission gate and kill the supply-chain soft spots (F5 + F8).** Deny-list GPL/AGPL, NOTICE-vs-dependency test, permission allow-list, `lint.abortOnError=true`, Gradle verification metadata, remove JitPack, vendor fork provenance, resolve smart-exception's licence, probe the codec set on a device.
4. **Resolve the manifest/background contradictions (F7).** Either WorkManager + `dataSync` foreground service + notification permission, or drop work-runtime and its three inherited permissions; set `extractNativeLibs` consistently; verify back navigation on API 36; re-do the 16 KB claim with `readelf`.
5. **Close the human gates before any listing (F6 + F9).** Name/trademark search, release AAB signing/R8/split, closed test with 12 testers × 14 days, device QA on API 26–36 with network off, and the counsel sign-off the memo's C5 already requires.

*Not a lawyer and not a mufti: this is a diligence review against the licences and Play policies cited above, not legal advice. Unverified items are marked as such (ELF 16 KB, OpenH264 presence/patent scope, LiberationSans licence, smart-exception licence, 131-test run).*
