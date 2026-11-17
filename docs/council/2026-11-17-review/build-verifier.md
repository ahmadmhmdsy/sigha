# Seat: The Devil's Advocate on evidence (al-Qawadih — invalid premise)
**Project:** Sigha (صيغة) — `com.sigha.converter` · **Date:** 2026-11-17 (Asia/Baghdad) · **Mode:** qiyas-council, merged
**Scope of this seat:** reproduce or refute the project's own claims by running them. This is the only seat permitted to build. No source or review files were modified; only `app/build/`, `.tmp_verify/` (scratch copies) and my three build logs were written.

## Verdict (one paragraph)

Most of the project's headline numbers are **real and I reproduced them**: a cache-disabled clean build from source succeeds (44/44 tasks executed, 3m45s), all 131 JVM tests pass in 15 classes with zero failures, the 48-format catalogue and 32 converter edges match the source exactly, the ABI set and package identity are as documented, and the OCR pack's 28,004,325-byte figure is the exact sum of its two pinned files. But the *method* that produced the published verification was a no-op build (all tasks up-to-date), and the evidence document presents that as a clean reproduction. That single flaw cascades into three false or invalid claims: the 72,522,602-byte / 8922…8266 APK is a stale, internally-fragmented artifact (a clean build is 69,569,713 B, hash 75C0…108F, with 2.95 MB of zip dead space and a byte-identical payload); the claim that native libs ship "uncompressed [and] aligned" is false in both APKs (all 32 `.so` are DEFLATED, because `AndroidManifest.xml:12` sets `extractNativeLibs="true"` and silently overrides `useLegacyPackaging = false`); and the cited 16 KB proof (`zipalign -P 16`) is **vacuous** for compressed libs — though I independently parsed all 32 ELF headers and the 16 KB property itself does hold. The "12 engine families" figure is wrong (11). Release is not merely unsigned: no signing config exists at all, R8 is off, and lint is configured never to be able to fail. The engineering core is genuinely better than the packaging/QA paperwork around it.

---

## Findings

### F1 — Critical — The published "verification" was a no-op build presented as a clean one
**Evidence.**
- `.gradlew.bat :app:testDebugUnitTest :app:assembleDebug --no-daemon --console=plain` → `> Task :app:testDebugUnitTest UP-TO-DATE`, `> Task :app:assembleDebug UP-TO-DATE`, `BUILD SUCCESSFUL in 41s`, `43 actionable tasks: 43 up-to-date`, `EXITCODE=0`. Nothing was compiled, packaged or tested; the command merely re-reported prior outputs.
- Forced real run: `.gradlew.bat :app:clean :app:testDebugUnitTest :app:assembleDebug --no-daemon --no-build-cache --console=plain` → `BUILD SUCCESSFUL in 3m 45s`, `44 actionable tasks: 44 executed`, `EXITCODE=0` (log: `build-verify-clean.log`).
- `docs/qa/verification.md:28-30` cites `build-final.log` as the evidence and `verification.md:150` states these results pass "from a clean invocation of the commands above". `build-final.log:59-60` itself says `BUILD SUCCESSFUL in 49s` / `43 actionable tasks: 11 executed, 32 up-to-date` — i.e. the cited log is *not* a clean build either.
**Why it matters.** Every number in `verification.md` inherits its authority from "we ran it". The document claims a clean-run methodology it never used. This is the invalid premise (`al-qawadih`) at the root of F2–F4: a stale artifact and a stale configuration were validated as if fresh. It is also exactly the failure mode that lets a contradiction like F3 survive a "green" build.
**Fix.** Make the documented reproducible command the only one used for evidence: `:app:clean :app:testDebugUnitTest :app:assembleDebug --no-daemon --no-build-cache`; record the Gradle "N executed" line verbatim; delete or regenerate `build-final.log`; add a CI job that runs the same command on a clean checkout.

### F2 — High — The 72,522,602-byte / `8922…8266` APK does not reproduce; the real clean APK is 2.95 MB smaller
**Evidence.**
- Pre-existing artifact hashed before rebuild: `app/build/outputs/apk/debug/app-debug.apk` = 72,522,602 B, SHA-256 `8922614FD9F06FD1C3E30F0D957D533E7CBCC8EFAA26064C744EF5A8BD9E8266` — so README:20 and `verification.md:37-38` are **true of that file**.
- Clean rebuild: 69,569,713 B, SHA-256 `75C0FC7BCDABF445EE3982533013D0339187C8CB89F6115587AC35438D29108F` (delta −2,952,889 B).
- The delta is **not** content: both APKs have 496 entries, `ONLY_IN_CLAIMED=0`/`ONLY_IN_NEW=0`, and identical compressed payload (sum of compressed entry sizes = **69,479,796 B in both**; sum raw = 171,436,965 B in both). The only entry-level differences are `classesN.dex` **permutations** (e.g. claimed `classes16.dex`=1,123,504 → rebuilt `classes18.dex`=1,123,504) whose deltas sum to exactly 0.
- The missing 2,952,891 B is **internal zip dead space in the claimed APK**: a 2,815,380-byte hole before `AndroidManifest.xml`, plus 77,095 B before `classes6.dex`, 51,082 B before `classes9.dex`, 9,334 B before `classes4.dex`. The rebuilt APK has `totalInternalGap: 0`.
**Why it matters.** The published size is an artifact of an incrementally patched APK that was never compacted; it overstates the shippable artifact by 4.1% and, worse, means the documented hash cannot be reproduced by anybody, including the authors, from the current tree. Any release note, store listing or comparison quoting 72.5 MB is quoting a ghost. The internal holes also show the artifact was assembled, not built, end-to-end.
**Fix.** Delete both stale figures; rewrite `verification.md` §3 from the clean build (`69,569,713 B` / `75C0FC7B…108F`) and state that debug APK hashes are build-local (debug signing + D8 multi-dex ordering are non-deterministic), so a reproducible-release pipeline (fixed D8 ordering, v2-only signing, `--no-build-cache`) is needed before any hash is published as a verification artifact.

### F3 — High — "Uncompressed, aligned native libs" is false; the manifest silently overrides the Gradle packaging config
**Evidence.**
- `docs/qa/verification.md:46`: "Packaging | `jniLibs.useLegacyPackaging = false` (uncompressed, aligned native libs)".
- Zip inspection of **both** the claimed and rebuilt APKs: `STORED_ENTRIES=0 DEFLATED_ENTRIES=32` for `lib/*` — every one of the 32 `.so` files is deflate-compressed (e.g. `lib/arm64-v8a/libavcodec.so` raw 19,105,416 → compressed 9,933,216).
- Root cause: `app/src/main/AndroidManifest.xml:12` sets `android:extractNativeLibs="true"`, which overrides `app/build.gradle.kts:47` `jniLibs { useLegacyPackaging = false }`. The build says so twice: *"PackagingOptions.jniLibs.useLegacyPackaging should be set to true because android:extractNativeLibs is set to "true" in AndroidManifest.xml."* and *"android:extractNativeLibs should not be specified in this source AndroidManifest.xml file. … The AGP Upgrade Assistant can remove the attribute"* (`build-verify-clean.log`, manifest line `AndroidManifest.xml:12:9-41 Warning`).
**Why it matters.** The Gradle setting is dead configuration — a reader of `app/build.gradle.kts` would believe the APK ships uncompressed libs and plan around that (install-time extraction behaviour, APK size, 16 KB story). It does not. This is a substantive documentation error about the shipped artifact, not a nitpick.
**Fix.** Remove `android:extractNativeLibs` from the manifest (AGP's own recommendation) and keep `useLegacyPackaging = false`; rebuild; confirm `STORED_ENTRIES=32`; then correct `verification.md:46`. Also add `android:extractNativeLibs` removal to the release checklist so it cannot regress.

### F4 — High — The 16 KB-page claim rests on a check that cannot fail (right answer, invalid premise)
**Evidence.**
- `docs/qa/verification.md:45` cites `zipalign -c -P 16 4 app-debug.apk` → exit 0 as proof of 16 KB alignment. I re-ran it: `ZIPALIGN_EXIT=0` on both the claimed and the rebuilt APK — but with all 32 `.so` entries DEFLATED (F3), zipalign's `-P 16` check has no uncompressed native entry to validate. The check is **vacuous**: it would print 0 for an APK with zero native libs.
- The property *does* hold, verified independently: I extracted all 32 `.so` and parsed ELF64 program headers. Every `PT_LOAD` segment in every library has `p_align = 16384` (`TOTAL=32 OK16K=32 FAIL=0`), e.g. `lib_arm64-v8a_libavcodec.so loads=4 minAlign=16384`.
**Why it matters.** A verification document must not cite a proof technique that is structurally incapable of detecting the defect it claims to exclude. Had one dependency shipped a 4 KB-aligned `.so`, this check would still have printed exit 0 — the doc gives false assurance about a Play submission requirement.
**Fix.** Cite the real check: `zipalign` **after** making libs uncompressed (post-F3) **plus** an ELF check (`llvm-readelf -l` / `readelf -l` and assert every `LOAD` is `0x4000`, or AGP's 16 KB lint/check). Record both outputs and the failing-library list.

### F5 — Medium — "32 converter ids from 12 engine families" is wrong: there are 11 families
**Evidence.** `docs/qa/verification.md:84` says "**32 converter ids** registered from **12 engine families** (image, pdf, office, text, data, subtitle, ebook, media, ocr, archive, upscale)" — the parenthetical enumerates 11. `app/src/main/java/com/sigha/converter/engine/BuiltInConverters.kt:24-36` contains exactly 11 `addAll(...)` calls. The "32" is correct: exactly 32 `override val id` declarations exist across `engine/` (grep counted 32, e.g. `image/ImageToImageConverter.kt:21` … `pdf/PdfToImageConverter.kt:29`).
**Why it matters.** Small, but it is the same document whose numbers are used to judge readiness; an internally inconsistent count weakens the credibility of the 48/32 claims that *are* right.
**Fix.** Change 12 → 11 (or enumerate the 12th if a family is missing from the wiring). While there: two ids break the hyphen convention — `ocr/PdfTextConverter.kt:32` `"ocr.pdf-to-txt"` and `ocr/ImageToTextConverter.kt:21` `"ocr.image-to-txt"` — and `pdf/PdfToTextConverter.kt:26` `"pdf-to-text"` is unprefixed; normalise to `ocr-pdf-to-txt` etc. so ids sort and grep predictably.

### F6 — Medium — Dead MIME-alias code hides a real format gap for legacy `.doc`/`.xls`
**Evidence.** `core/FormatRegistry.kt:45-46` maps `"application/vnd.ms-excel" -> byExtension["xls"]` and `"application/msword" -> byExtension["doc"]`. Neither `xls` nor `doc` exists in `DATA` (`FormatRegistry.kt:90-148`, 48 entries: docx/odt/rtf/xlsx/ods/csv/tsv… no doc, no xls), so both aliases return `null` unconditionally. `detect()` then falls through to the ad-hoc `FileFormat(id = extension, …, category = FormatCategory.DATA)` at `FormatRegistry.kt:75-81`, so a legacy Word/Excel file silently becomes a *data-category* pseudo-format with no converter and no explanation. `docs/qa/verification.md:127` compounds it: "RTF/TIFF/**DOC** are catalogue entries without edges" — `doc` is not a catalogue entry at all.
**Why it matters.** This is dead code pretending to handle two of the most common legacy formats a converter's users will actually bring; the failure is silent rather than a clear "unsupported format" message. It is also the kind of invalid premise this seat exists to flag: the code's shape asserts a capability the catalogue does not have.
**Fix.** Either add `doc`/`xls` to the catalogue as explicitly unsupported ("no converter" state with a user-visible reason), or delete the two alias branches and make `detect()` return null for unknown MIME types; correct `verification.md:127`.

### F7 — Medium — Release is not "pending a key": there is no signing config, no shrinking, and lint can never fail
**Evidence.**
- `app/build.gradle.kts:22-27`: `buildTypes { release { isMinifyEnabled = false; proguardFiles(...) } }` — no `signingConfigs` block anywhere in `app/build.gradle.kts` or `build.gradle.kts` (grep: 0 hits for `signingConfig|keystore`); no `*.jks`/`*.keystore`/`*.p12` exists in the tree. So a `release` build produces an **unsigned** APK that Play cannot accept.
- No `splits { abi {} }`, no `bundle` config, `isShrinkResources` unset; `app/proguard-rules.pro` exists (376 B) but is inert because minify is off.
- `app/build.gradle.kts:50-53`: `lint { abortOnError = false; checkReleaseBuilds = false }` — lint is configured so it can never gate anything, in debug **or** release.
- Only a debug APK exists in `app/build/outputs/apk/`; `.gradlew :app:assembleRelease` was **not run** by me.
**Why it matters.** README:33 frames the remaining work as "needs a release key (and an ABI split)"; in fact the release *pipeline* does not exist (no signing wiring, no R8/resource shrinking, no ABI split, no lint gate). A 69–72 MB debug APK with unstripped, unshrunk code is not a submission candidate, and a project whose lint cannot fail accumulates exactly the warnings already present (9 compiler warnings, 8 of them deprecated auto-mirrored icons in RTL-aware UI — see `build-verify-clean.log` L49-L56).
**Fix.** Add `signingConfigs` fed from `keystore.properties`/env (never committed); turn on `isMinifyEnabled=true` + `isShrinkResources=true` with real keep rules for FFmpegKit/PDFBox/Tesseract; add ABI splits or ship an AAB; set `lint { abortOnError = true; checkReleaseBuilds = true }`; fix the 9 warnings (they are RTL correctness signals — `Icons.Outlined.OpenInNew` should be `Icons.AutoMirrored…` in an Arabic-capable UI).

### F8 — Medium — The documented permission set understates what the app actually requests
**Evidence.** `docs/qa/verification.md:48` lists 4 permissions. `aapt2 dump badging` on the shipped APK lists **8**: the documented `INTERNET`, `ACCESS_NETWORK_STATE`, `POST_NOTIFICATIONS`, `WRITE_EXTERNAL_STORAGE` (maxSdk 28) **plus** `WAKE_LOCK`, `RECEIVE_BOOT_COMPLETED`, `FOREGROUND_SERVICE`, `com.sigha.converter.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION` (and an implied `READ_EXTERNAL_STORAGE` maxSdk 28) — all four extra ones merged from `androidx.work`.
**Why it matters.** For a product whose entire pitch is "privacy-first, fully offline", `INTERNET` + `RECEIVE_BOOT_COMPLETED` + `FOREGROUND_SERVICE` are precisely what a privacy reviewer or a cautious Play reviewer will ask about, and the project's own evidence sheet does not mention three of them. Understating the permission surface in the QA doc is a trust defect, not a formatting one.
**Fix.** Regenerate §3's permission row from `aapt2 dump badging` verbatim, add one line per WorkManager-inherited permission explaining why it exists (queue recovery, notification-based progress), and consider `android:directBootAware`/`WorkManager` config or a documented justification for `RECEIVE_BOOT_COMPLETED` if the queue does not actually need boot rescheduling.

### F9 — Low — `build-final.log` is stale evidence that contradicts the current tree
**Evidence.** `build-final.log:50` prints the `useLegacyPackaging should be set to true` warning, while the current `app/build.gradle.kts:47` sets `useLegacyPackaging = false`; `build-final.log:59-60` shows a 49s non-clean build ("11 executed, 32 up-to-date"). Both facts are incompatible with the document that cites this log as proof of a clean build (`verification.md:28-30`).
**Fix.** Delete `build-final.log` from the evidence chain, or regenerate it with the clean command from F1 and note in `verification.md` §2 the exact invocation and the executed/up-to-date counts.

### F10 — Low — Dependency stack is 1–2 major lines behind, confirmed by the project's own lint
**Evidence (repo):** `app/build.gradle.kts:58-93` pins `core-ktx:1.13.1`, `activity-compose:1.9.3`, `compose-bom:2024.12.01`, `lifecycle-*:2.8.7`, `navigation-compose:2.8.5`, `coil-compose:2.7.0`, `kotlinx-coroutines:1.9.0`; `build.gradle.kts:3-6` pins Kotlin **2.0.21** and AGP **8.10.1**, while `compileSdk = targetSdk = 36` (`app/build.gradle.kts:10,15`).
**Evidence (web, search-result listings only — pages were NOT fetched: `web_fetch` fails in this environment with "multiple usable web providers are registered"):** `mvnrepository.com/artifact/androidx.compose/compose-bom/2026.06.01`, `developer.android.com/build/releases/agp-9-5-0-release-notes`, `developer.android.com/jetpack/androidx/releases/core` — i.e. newer Compose BOM and AGP major lines exist. **Now hard-verified locally** by the project's own tooling: the lint report's `GradleDependency`/`AndroidGradlePluginVersion` issues name the available versions — `core-ktx 1.19.1`, `lifecycle 2.11.0` (all three), `activity-compose 1.13.0`, `compose-bom 2026.09.00`, `navigation-compose 2.10.2`, `work-runtime-ktx 2.12.0`, `datastore-preferences 1.3.0`, `documentfile 1.1.0`, `exifinterface 1.4.2`, `Gradle 8.14.7` — i.e. the pinned set is 1–2 major lines behind what the toolchain itself knows about. (The web pages themselves remain unfetched: `web_fetch` fails with "multiple usable web providers are registered".)
**Why it matters.** Compiling *against* SDK 36 with 2024-era AndroidX is legal but is where edge-to-edge enforcement (targetSdk 35+/36) and window-inset APIs behave differently; older `activity`/`compose` versions are the usual source of "content under the status bar" bugs on Android 15/16. There is also no dependency-verification (`verification-metadata.xml`) or version catalog.
**Fix.** Move to a Gradle version catalog (`libs.versions.toml`), upgrade core/activity/lifecycle/compose, and record the upgrade rationale in `docs/research/android-stack.md`; add `--dependency-verification` with checksums for release builds.

### F11 — Low — Third-party native dependency has no provenance story beyond "it resolved once"
**Evidence.** `app/build.gradle.kts:87` `dev.ffmpegkit-maintained:ffmpeg-kit-full:8.1.9`. The artifact is present in the local Gradle cache (`~/.gradle/caches/modules-2/files-2.1/dev.ffmpegkit-maintained/ffmpeg-kit-full/8.1.9`) and appears in search results on `mvnrepository`/`central.sonatype.com`, so it **exists**; upstream `arthenica/ffmpeg-kit` was discontinued (source: `tanersener.medium.com/saying-goodbye-to-ffmpegkit-33ae939767e1`). The ownership/legitimacy of the "ffmpegkit-maintained" group and the integrity of the 8.1.9 binaries were **not** independently established (unverified). `settings.gradle.kts:19` also declares `jitpack.io`, broadening the supply-chain surface even though no evidence shows it is needed.
**Evidence (supply chain, positive):** the clean build made **0** download requests and reported **0** `Could not resolve` (grep of `build-verify-clean.log`: `DOWNLOAD_LINES=0`) — it resolves entirely from the warm local cache, so the build does **not** require network on this machine. A cold machine would need `google()`, `mavenCentral()` and possibly `jitpack` (`settings.gradle.kts:1-21`).
**Why it matters.** A discontinued upstream plus a community republish of 60 MB of LGPL native code is the single largest legal/availability risk in the shipping artifact (`NOTICE.md` already handles the LGPL notification side). Silent republication is also exactly how a supply-chain substitution would arrive.
**Fix.** Pin by checksum with Gradle dependency verification, vendor the exact AAR in a private repo or record the publisher identity + checksum in `docs/legal/clearance-memo.md`, drop `jitpack.io` if unused, and document the fork's maintenance contact.

### F12 — Low — Verification environment claims cannot be reproduced from the current host
**Evidence.** `docs/qa/verification.md:19` and §6 describe runtime smoke tests on `emulator-5554` (AVD `api36`, ATD x86_64). Today: `adb devices` → *"List of devices attached"* (empty); AVD `api36` **does exist** (`C:\Users\AhmadMhmoud\.android\avd\api36.avd`), the ATD system image exists (`C:\Android\Sdk\system-images\android-36\aosp_atd\x86_64`), and `emulator.exe` is present — so the prior smoke test was *possible* on this host, and a headless boot is still technically feasible, but **I did not boot it** (not run, per 3-minute cap) and no device is attached now. Device QA (interaction, conversion, SAF, MediaStore) therefore remains **not done**; the project correctly says so at `verification.md:152-154`.
**Why it matters.** The runtime evidence that does exist (install, activity resumed, empty crash buffer) is real but thin; nothing in the repo can be re-executed today without booting hardware. Claims about the app *working* rest on zero executed conversions.
**Fix.** Commit a `docs/qa/manual-checklist.md` and a scripted `smoke.sh` (install → start → one conversion of each family → assert output), boot the existing `api36` ATD headless in CI, and keep the honest "device QA not done" language until it passes on a physical device.
### F13 — Medium — Lint runs, but finds 58 issues (1 Error) that the project has disabled itself from seeing
**Evidence.** `.gradlew.bat :app:lintDebug --no-daemon --console=plain` → `BUILD SUCCESSFUL in 3m 50s`, `LINT_EXIT=0`, report at `app/build/reports/lint-results-debug.xml`: **58 issues — 1 Error, 57 Warnings** (exit code 0 *despite* the Error, because `abortOnError=false`, F7). The single Error:
- `WrongConstant` — **`engine/text/TextPdfWriter.kt:67`** `.setBreakStrategy(Layout.BREAK_STRATEGY_SIMPLE)`: `StaticLayout.Builder.setBreakStrategy` is annotated for `LineBreaker.BREAK_STRATEGY_*`, not `Layout.BREAK_STRATEGY_*`. The values are numerically identical (both `0`) so there is **no observed runtime defect today**, but the `@IntDef` contract is violated and the compiler cannot protect it — a latent bug if the constants ever diverge. (`TextPdfWriter.kt:68` `Layout.HYPHENATION_FREQUENCY_NONE` has the same shape and was not flagged.)
- `TrustAllX509TrustManager` ×3 — `checkClientTrusted`/`checkServerTrusted` empty. **Important nuance:** all three locations are inside the transitive jar `bcpkix-jdk15to18-1.72.jar` in the Gradle cache, **not in Sigha source**; it reaches the APK through `com.tom-roush:pdfbox-android:2.0.27.0`. This is not evidence that Sigha's own model-download path trusts all certificates — I did not audit that path — but it does mean a "trust-all" implementation class ships inside an app that advertises privacy.
- Warnings worth acting on: 18 `UnusedResources` (`R.string.action_convert`, `action_add_files`, `action_delete`, `action_open`, `offline_badge` … — dead strings, or strings the UI bypasses); 12 `UseKtx`; 3 `PluralsCandidate` (`%d selected` — breaks Arabic's six plural forms, a real i18n defect in an Arabic-first product); 2 `UsableSpace` (a converter that writes large outputs should use `getAllocatableBytes` before claiming failure); 2 `MonochromeLauncherIcon`; `DataExtractionRules` (deprecated `allowBackup` for Android 12+); `ObsoleteSdkInt` (`mipmap-anydpi-v26` is redundant at minSdk 26).
**Why it matters.** The project ships a lint *configuration* that guarantees this report can never block a build, so 58 findings — including one Error and an i18n bug class — accumulated silently while `verification.md` describes the build as "no errors, no warnings that block the build" (line 30, which is technically true only because nothing is allowed to block).
**Fix.** Flip `abortOnError = true` and `checkReleaseBuilds = true` (F7), baseline the 57 warnings once, then burn them down: fix `setBreakStrategy` to `LineBreaker.BREAK_STRATEGY_SIMPLE` (or keep `Layout` values and suppress with a comment), convert the three plural strings to `<plurals>` (and translate them in `values-ar/strings.xml`), delete or wire the 18 unused strings, adopt `Bitmap.scale`/`getAllocatableBytes` where lint suggests, and add a monochrome layer to the adaptive icon.

---

## Verified strengths (only what I actually ran or read)

- **Clean build from source succeeds.** `:app:clean :app:testDebugUnitTest :app:assembleDebug --no-daemon --no-build-cache` → `44 actionable tasks: 44 executed`, `BUILD SUCCESSFUL in 3m 45s`, exit 0. This is genuine: nothing was up-to-date and the build cache was disabled.
- **The test suite is real and green.** 15 JUnit XML files aggregate to **131 tests, 0 failures, 0 errors, 0 skipped**, and the per-class counts match `verification.md:60-76` exactly (`FfmpegArgsTest`=25, `JsonCsvTest`=12, `CsvTest`=11, `SubtitleFormatTest`=10, `MarkdownParserTest`=10, …). No Robolectric is present, so the doc's caveat that no Android framework code is exercised is accurate.
- **The routing/catalogue numbers are exactly right.** 48 `f(...)` entries in `core/FormatRegistry.kt:90-148`; 32 `override val id` declarations across `engine/`; 11 families wired in `BuiltInConverters.kt`.
- **Artifact identity matches the docs** (`aapt2 dump badging`): `com.sigha.converter`, versionCode 1, versionName 0.1.0, minSdk 26, targetSdk 36, label Sigha / صيغة, launchable `MainActivity`, ABIs `arm64-v8a` + `x86_64`, 16 `.so` per ABI.
- **16 KB compatibility is real**, independently of the weak proof: all 32 `.so` have ELF64 `PT_LOAD` `p_align = 16384` (0 failures).
- **Model packs are honestly pinned**: `model/PackCatalog.kt:12-28` uses a commit-pinned `tessdata_best` URL, per-file SHA-256, and sizes summing to exactly the advertised 28,004,325 B; Apache-2.0 and licence URL are recorded.
- **No ONNX residue** (0 grep hits), and `verification.md` admits upscaling is not shipped.
- **i18n hygiene is better than average**: `res/values-ar/strings.xml` exists, `supportsRtl="true"`, and I found **0** hardcoded `Text("...")` literals in `ui/` — user-facing copy goes through resources.
- **The build does not need the network on this machine** (0 download lines, 0 resolution failures) and `allowBackup="false"` is set.

---

## Claims checked vs actual

| # | Claim (source) | Verification method | Result |
|---|---|---|---|
| 1 | 48-format catalogue (`README.md` §Status; `verification.md:83`) | counted `f(...)` entries in `core/FormatRegistry.kt:90-148` | **TRUE — 48 exactly** |
| 2 | 32 converter edges (`README.md:32`; `verification.md:84`) | grep `override val id` under `engine/` | **TRUE — 32 exactly** |
| 3 | "from 12 engine families" (`verification.md:84`) | `BuiltInConverters.kt:24-36`; the doc's own list | **FALSE — 11 families (F5)** |
| 4 | 131 JVM tests green, 15 classes, 0 failures (`README.md:20`; `verification.md:58`) | clean+cache-disabled `:app:testDebugUnitTest`; summed 15 JUnit XML | **TRUE — 131 tests, 0 fail, 0 err, 0 skip; per-class counts match the doc table exactly** |
| 5 | "from a clean invocation" (`verification.md:150`) | ran the documented command, then a forced clean run | **FALSE — documented run was 43/43 up-to-date; clean run was 44/44 executed (F1)** |
| 6 | APK = 72,522,602 B, SHA-256 `8922…8266` (`README.md:20`; `verification.md:37-38`) | `Get-FileHash` on the surviving artifact; then clean rebuild | **TRUE of the stale artifact; FALSE of a clean build — clean = 69,569,713 B / `75C0FC7B…108F`; 2,952,891 B zip dead space (F2)** |
| 7 | "16 KB-page aligned" (`README.md:20`; `verification.md:45`) | `zipalign -c -P 16 4` (exit 0) **and** ELF64 `PT_LOAD p_align` parse of all 32 `.so` | **PROPERTY TRUE (32/32 `p_align=16384`) but the cited proof is VACUOUS** (all libs deflated) **(F4)** |
| 8 | `useLegacyPackaging=false` → "uncompressed, aligned native libs" (`verification.md:46`) | zip `method` per `lib/*` entry in both APKs | **FALSE — 0 stored / 32 deflated; manifest `extractNativeLibs="true"` overrides (F3)** |
| 9 | ABIs arm64-v8a + x86_64, 16 `.so` per ABI, 32 total (`verification.md:43`) | `aapt2 dump badging`; zip entry grouping | **TRUE — 16 + 16 = 32** |
| 10 | Largest libs: `libavcodec.so` 18.2 MB arm64 / 23.3 MB x86_64 (`verification.md:44`) | zip raw sizes 19,105,416 / 24,384,816 B | **TRUE (as MiB: 18.2 / 23.3)** |
| 11 | package `com.sigha.converter`, versionCode 1, versionName 0.1.0, minSdk 26, targetSdk 36, label Sigha/صيغة, launch `MainActivity` (`verification.md:39-42,49`) | `aapt2 dump badging` | **TRUE — all match** |
| 12 | Permissions = 4 listed (`verification.md:48`) | `aapt2 dump badging` | **INCOMPLETE — 8 actual (F8)** |
| 13 | OCR pack 28,004,325 B, SHA-256 pinned, Apache-2.0 (`README.md:19`; `verification.md:121`) | `model/PackCatalog.kt:16-28` sum | **TRUE — 15,400,601 + 12,603,724 = 28,004,325 exactly; both files pinned to commit `e12c65a`** |
| 14 | ONNX Runtime removed, 0 code hits (`verification.md:52,128`) | grep `onnxruntime|ai.onnxruntime` under `app/src` | **TRUE — 0 hits** |
| 15 | `upscaleConverters()` deliberately empty (`verification.md:87`) | `engine/upscale/UpscaleConverters.kt` | **TRUE (empty list; "Coming soon" matches)** |
| 16 | "no release signing, no ABI split, no R8" (`verification.md:129`) | `app/build.gradle.kts:22-27,50-53`; grep for `signingConfig|keystore`; keystore file search | **TRUE — and worse: zero signing wiring exists at all; lint cannot gate (F7)** |
| 17 | Device QA not done (`verification.md:152`) | `adb devices` empty; AVD `api36` exists | **TRUE — nothing attached; no runtime evidence reproducible today (F12)** |
| 18 | Build needs no network / deps resolve | `build-verify-clean.log` grep for `Download|Could not resolve` | **TRUE on this host — 0 downloads (warm cache). Cold-machine behaviour: unverified** |
| 19 | NDK native libs stripped | clean log `stripDebugDebugSymbols` | **NO — "Unable to strip the following libraries, packaging them as they are" × 16** (debug; size lever) |
| 20 | Lint clean / gating | `:app:lintDebug` (first attempt aborted: "Gradle build daemon has been stopped"; retry succeeded) | **FALSE — 58 issues (1 Error, 57 Warnings) yet exit 0, because `abortOnError=false` (F13)** |

**Not run (honest omissions):** `:app:assembleRelease`; `:app:bundleRelease`; `assembleAndroidTest`/`connectedAndroidTest`; any device/emulator session; `--offline` build on a cold cache; `web_fetch` of version pages (environment error, F10); independent verification of the ffmpeg-kit fork's provenance (F11).

---

## Top-5 prioritized actions

1. **Fix the packaging contradiction and rebuild (F3, F4).** Remove `android:extractNativeLibs="true"` from `AndroidManifest.xml:12`; keep `useLegacyPackaging = false`; clean-build; assert `STORED_ENTRIES=32`; then re-verify 16 KB with *both* `zipalign -c -P 16 4` (now meaningful) and an ELF `p_align` check. Correct `verification.md:46`.
2. **Make verification honest and reproducible (F1, F2, F9).** Adopt `:app:clean :app:testDebugUnitTest :app:assembleDebug --no-daemon --no-build-cache` as the only evidence command; record "44 executed"; regenerate APK size/hash (`69,569,713 B` / `75C0FC7B…108F`); delete `build-final.log` and the stale 72,522,602 B / `8922…8266` figures; add a CI job that runs exactly that.
3. **Correct the false claims before anyone quotes them again (F5, F6, F8).** `verification.md`: 12→11 families; regenerate the permission row from `aapt2`; drop "DOC … catalogue entries"; document the `.doc/.xls` gap or delete the dead aliases in `FormatRegistry.kt:45-46`.
4. **Build the release path (F7).** `signingConfigs` from untracked `keystore.properties`; `isMinifyEnabled=true` + `isShrinkResources=true` with keep rules for FFmpegKit/PDFBox/Tesseract; ABI splits or AAB; flip `lint { abortOnError = true; checkReleaseBuilds = true }`; fix the compile warnings (8 are RTL `AutoMirrored` icon deprecations — correctness in an Arabic UI) **and** the lint backlog: the `WrongConstant` Error at `TextPdfWriter.kt:67`, `<plurals>` for Arabic, and confirm or hide the transitive `bcpkix` trust-all classes (F13).
5. **Close the runtime and supply-chain holes (F10, F11, F12).** Boot the existing `api36` ATD headless and script one conversion per engine family (install → convert → assert output); pin dependencies via a version catalog + `verification-metadata` checksums; record the ffmpeg-kit fork's provenance/checksum in the clearance memo; remove unused `jitpack.io`.

---

## Commands run (for reproduction)

```powershell
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-21.0.13.9-hotspot"
Set-Location "E:\android_apps\digital_converter"

# 1. documented command (F1: proved to be a no-op)
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug --no-daemon --console=plain
#   -> 43 actionable tasks: 43 up-to-date ; BUILD SUCCESSFUL in 41s ; EXITCODE=0

# 2. forced clean, cache-disabled (the real verification)
.\gradlew.bat :app:clean :app:testDebugUnitTest :app:assembleDebug --no-daemon --no-build-cache --console=plain
#   -> 44 actionable tasks: 44 executed ; BUILD SUCCESSFUL in 3m 45s ; EXITCODE=0
#   log: build-verify-clean.log

# 3. APK facts
Get-FileHash app\build\outputs\apk\debug\app-debug.apk -Algorithm SHA256
#   claimed artifact : 72,522,602 B / 8922614FD9F06FD1C3E30F0D957D533E7CBCC8EFAA26064C744EF5A8BD9E8266
#   clean rebuild    : 69,569,713 B / 75C0FC7BCDABF445EE3982533013D0339187C8CB89F6115587AC35438D29108F
& "C:\Android\Sdk\build-tools\36.0.0\aapt2.exe" dump badging app\build\outputs\apk\debug\app-debug.apk
& "C:\Android\Sdk\build-tools\36.0.0\zipalign.exe" -c -P 16 4 app\build\outputs\apk\debug\app-debug.apk
#   -> ZIPALIGN_EXIT=0 (vacuous: 0 stored / 32 deflated lib/*.so)

# 4. test XML aggregation
#   app/build/test-results/testDebugUnitTest/*.xml -> 15 files, TOTAL_TESTS=131 FAILURES=0 ERRORS=0 SKIPPED=0

# 5. ELF 16 KB check (all 32 .so extracted from APK, ELF64 program headers parsed)
#   -> TOTAL=32 OK16K=32 FAIL=0  (every PT_LOAD p_align = 16384)

# 6. zip structure comparison (claimed vs rebuilt)
#   -> 496 entries both; sumComp 69,479,796 both; claimed has totalInternalGap=2,952,891; rebuilt 0

# 7. lint (first attempt aborted; retry completed)
.\gradlew.bat :app:lintDebug --no-daemon --console=plain
#   -> BUILD SUCCESSFUL in 3m 50s ; LINT_EXIT=0 ; 58 issues = 1 Error + 57 Warnings
#   Error: WrongConstant at engine/text/TextPdfWriter.kt:67

# 8. device feasibility
& "C:\Android\Sdk\platform-tools\adb.exe" devices      # -> empty list
& "C:\Android\Sdk\emulator\emulator.exe" -list-avds    # -> api36 (ATD x86_64 image present)
```

*Seat note: I did not modify any application source, any other seat's review file, or any existing project document. `app/build/` (build output), `.tmp_verify/` (APK copies and extracted `.so`) and logs `build-verify-run.log`, `build-verify-clean.log`, `build-verify-lint.log`, `build-verify-lint2.log` were written by me.*
