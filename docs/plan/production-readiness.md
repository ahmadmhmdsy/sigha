# Production-readiness plan — Sigha (صيغة)

**Created:** 2026-11-17 · **Owner:** Lead · **Input:** docs/council/2026-11-17-review/council-review.md
**Goal:** turn the reviewed debug-only prototype into a build that (a) tells the truth, (b) is legally distributable, (c) survives a device, and (d) can be signed and uploaded.

## Definition of done for this pass

1. `:app:clean :app:testDebugUnitTest :app:assembleDebug :app:lintDebug :app:assembleRelease :app:bundleRelease` all green from a clean tree.
2. Packaging assertions pass: **32 stored (uncompressed) .so**, 16 KB-aligned ELF, zipalign pass, release APK/AAB produced.
3. No user-visible English error path: failures show a localized headline + optional details.
4. Scanned PDF -> TXT works (OCR fallback), notes reach the UI, media conversions strip metadata, FFmpeg has a timeout.
5. Privacy policy exists in-app and in docs; NOTICE/licences list only what ships and include the LGPL source offer.
6. README / PRODUCT.md / docs/qa/verification.md contain only numbers reproduced by `scripts/verify.ps1` in this tree.
7. Emulator smoke run recorded (install, launch, monkey, crash scan).

## Workstreams and file ownership

| WS | Scope | Owner | Files (write scope) | Depends on |
|---|---|---|---|---|
| **WS0** | Baseline, core graph/planner, docs honesty, verification script, integration | Lead | `core/`, `scripts/`, `README.md`, `docs/qa/verification.md`, `docs/plan/`, `docs/adr/` | — |
| **WS1** | Engine correctness: OCR fallback, page ranges, FFmpeg metadata/timeout/progress, archive caps, EPUB DRM, text decoding, multi-output flags, lint error | engine-auditor | `engine/**`, `app/src/test/**/engine/**` | WS0 core props |
| **WS2** | App layer: notes end-to-end, localized failures, OCR gating, SAF release, DataStore corruption, output folder validation, queue cap, pack UX, foreground service + notification, interrupted-batch marker | app-auditor | `data/**`, `work/**`, `ui/**`, `MainActivity.kt`, `model/**`, `res/values*/strings_ui.xml` | WS0 core props |
| **WS3** | Release mechanics: signing config, R8, ABI splits, lint gate, manifest (service, permissions, extractNativeLibs), Gradle cleanup, packaging assertions, emulator QA | build-verifier | `app/build.gradle.kts`, `build.gradle.kts`, `settings.gradle.kts`, `gradle.properties`, `app/proguard-rules.pro`, `app/src/main/AndroidManifest.xml`, `keystore/` (gitignored) | WS2 service class |
| **WS4** | Legal/policy: licence assets + screen, privacy policy + screen, NOTICE rewrite, compliance doc, release-blocker checklist | policy-guardian | `app/src/main/assets/licenses/**`, `ui/LicensesScreen.kt`, `ui/PrivacyScreen.kt`, `docs/legal/**`, `NOTICE.md`, `res/values*/strings_legal.xml` | WS3 BuildConfig/strings |
| **WS5** | Product docs: honest PRODUCT.md, store listing copy, permissions rationale, changelog | product-strategist | `docs/product/**`, `PRODUCT.md`, `CHANGELOG.md` | WS0 numbers |

**Rules:** one writer per file; new string resources go in per-workstream files (`strings_ui.xml`, `strings_legal.xml`) to avoid merge conflicts; no new formats/features in this pass; every claim needs a command output.

## Order of execution

1. WS0: git baseline (done), core props (done), plan (this file).
2. WS1 + WS2 + WS3 + WS4 + WS5 start in parallel on disjoint files.
3. Lead integrates: build, fix compile errors, run tests, packaging checks.
4. WS0 closes docs against measured numbers; verification script becomes the single source of truth.
5. Emulator smoke; record evidence in docs/qa/verification.md.

## Verification gate (scripts/verify.ps1)

- clean build + unit tests (parse XML results: expect 131+ tests, 0 failures)
- APK facts: size, SHA-256, stored/deflated .so count, ELF `PT_LOAD p_align`
- zipalign `-c -P 16`
- lint report summary
- release APK/AAB existence and signing check
- claim extraction: grep README/PRODUCT/verification for numbers and compare with measured values

## Human-only blockers (cannot be done from this machine)

| # | Blocker | Why human |
|---|---|---|
| B1 | Play upload key ownership | the upload key is the publisher's identity; must be generated and stored by the account owner (a local verification keystore is created, never used for Play) |
| B2 | Privacy policy public URL | Play requires a hosted HTTPS URL; repo copy + in-app screen are provided |
| B3 | Data safety form + listing + closed test (12 testers x 14 days) | Play Console only |
| B4 | Merchant/entity decision for paid apps | Play merchant registration unavailable for Iraq |
| B5 | Trademark search/registration for the working name | registry/IP counsel |
| B6 | LGPL "Corresponding Source" hosting | the source/build tarball must be published at a stable URL the publisher controls |
| B7 | Counsel sign-off on FFmpegKit LGPL obligations | legal judgement |

## Execution status (2026-11-17, this tree)

| WS | State | What landed | Evidence |
|---|---|---|---|
| WS0 | done | honest README/verification, verification gate, ADRs, plan | `scripts/verify.ps1`, `docs/qa/evidence/` |
| WS1 | done | scanned-PDF OCR fallback, page ranges, FFmpeg metadata strip + 20-min timeout + free-space preflight, archive write budget, EPUB DRM/char cap, multi-output routing guard, OOM containment | `app/src/main/java/.../engine/**`, unit tests |
| WS2 | done | localized failure headlines (16 kinds), notes to UI, OCR gating, app-scoped pack downloads with resume/cancel, foreground service + queue, SAF grant release, folder writability check, privacy screen + nav, SAF picker guard | `data/**`, `ui/**`, `work/**` |
| WS3 | done | signing config (git-ignored keystore), R8 + resource shrinking, ABI splits + universal, AAB (splits off for bundle), lint gate, stored/aligned native libs, `device-smoke.ps1` | `app/build.gradle.kts`, `scripts/verify.ps1`, `scripts/device-smoke.ps1` |
| WS4 | done | 45 licence texts, Licences screen, privacy policy + in-app screen, NOTICE rewrite, compliance + release-blocker docs, Arabic legal strings | `assets/licenses/**`, `assets/legal/**`, `docs/legal/**`, `NOTICE.md` |
| WS5 | done | one-page PRODUCT.md, full spec, store listing, permissions rationale, CHANGELOG 0.2.0 | `PRODUCT.md`, `docs/product/**`, `CHANGELOG.md` |
| WS6 | done | 0.2.1 UI/UX fix pass: OCR pack inline install + live target refresh, landscape empty state, typed localized notes, six-category plurals, Arabic privacy policy, single window/chrome owner + theme role ramp, accessibility semantics and contrast | `.impeccable/critique/`, `docs/council/2026-11-17-ui-review/`, `docs/qa/verification.md`, `CHANGELOG.md` |

### Definition-of-done check

1. Clean build/test/assemble/lint/release/bundle — **green**, see `docs/qa/evidence/verify-*.json`.
2. Packaging: **32 stored `.so`**, arm64 `PT_LOAD` alignment asserted, zipalign pass — same JSON.
3. No user-visible English-only failure path: localized failure headline + expandable English details — code + strings; lossy-conversion notes are typed (`core.Note`) and resolved from resources by `NoteUi` in the queue, history and workbench. Only raw engine failure diagnostics stay English, behind a details disclosure.
4. Scanned PDF → TXT via OCR fallback; notes reach UI; metadata stripped; FFmpeg timeout — engine tests + code.
5. Privacy policy in-app and in docs; NOTICE lists what ships incl. LGPL source offer — `docs/legal/`, `NOTICE.md`.
6. README / PRODUCT / verification carry only measured numbers — `verify.ps1` claim-drift step.
7. Emulator smoke recorded — `scripts/device-smoke.ps1` (install, cold start, resumed, 120-event monkey, no crash). **Real conversions and visual UI still need a physical device.**

### Human-only blockers still open

B1 upload key · B2 privacy-policy URL · B3 Play Console data-safety/listing/closed test · B4 merchant entity ·
B5 trademark · B6 LGPL source hosting · B7 counsel sign-off. See `docs/legal/release-blockers.md`.

**Council decision (2026-11-18):** the path to the closed test — one upload build, the human lane,
and the 0.2.2 deferrals — is `docs/council/2026-11-18-next/council-decision.md`. It adds the Play
foreground-service declaration to the console work and folds the batch-outcome/CTA legibility items
into the upload build.
