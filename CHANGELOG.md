# Changelog

All notable changes to Sigha, grouped per release. This file records **what changed**, not measured
results: APK sizes, hashes, test counts and format statistics change with every build and live in
`docs/qa/verification.md` and the machine-readable evidence written by `scripts/verify.ps1`.
Store-facing release notes live in [`docs/product/store-listing.md`](docs/product/store-listing.md).

The first tracked state of this repository is commit `11bccbb` ("baseline snapshot before
production-readiness pass"). Earlier prototype builds were not tagged and are not reconstructed
here.

## 0.2.0 — 2026-11-17 — production-readiness (closed-test build)

First build intended for the Play **closed test**. Not published to any store. Nothing here is a
release claim until `scripts/verify.ps1`, the JVM test suite and the device QA checklist pass on the
exact commit being released.

### Fixed

- **Scanned PDFs no longer produce an empty file.** `PDF → TXT` detects that the selected pages have
  no text layer and falls back to OCR. With the OCR pack installed it returns the text and records
  the note "No text layer found; used OCR."; without the pack it fails with an instruction to
  install it instead of a generic error.
- **PDF page selection is honoured everywhere it is offered.** Both `PDF → TXT` and `PDF → image`
  parse page ranges; multi-page export writes one file per page (`name-pageN`) and never overwrites
  a page. A converter that produces several files is only allowed as the final step of a route, so
  a chain cannot silently drop outputs.
- **Lossy decisions are reported to the user instead of disappearing.** Converters now return notes
  through the conversion result (first worksheet only, spreadsheet row caps, text truncated to the
  export limit, OCR fallback) so the queue and history can show why an output is smaller than the
  input.
- **Media conversion hardened.** FFmpeg runs with a 20-minute per-file timeout and a 250 ms
  cancellation poll; a free-space guard refuses a start that the device cannot finish; a non-zero
  FFmpeg exit becomes a typed, localized failure with a bounded technical detail instead of a raw
  log dump. Partial and zero-byte outputs are deleted on failure.
- **Data loss from interrupted or corrupt state reduced.** Corrupt preference/history data falls
  back to empty defaults instead of failing; queue entries are validated on load; a batch
  interrupted by process death is detected and announced on the next launch instead of appearing
  to vanish.
- **Input guards fail loudly, not badly.** Documented caps are enforced with typed errors: images
  (100 megapixels, 200 MB input), PDFs (500 MB input), office packages (512 MB archive / 2 GB
  expanded / 50,000 entries), text inputs and exports (50,000 lines for TXT→PDF).
- **Format graph cleaned.** MIME aliases that pointed at formats with no converter were removed from
  the registry, so the app cannot advertise a dead target through an alias; route planning is
  deterministic (converter order, then format id) and rejects same-format conversions with a
  message. Archive extraction validates entry counts and sizes before writing.
- **Model-pack download failures are specific.** Corrupt or partially downloaded packs are removed
  instead of being left half-written; every failure mode (network, digest, storage) maps to its own
  message.
- **No user-visible English exception text in the Arabic UI.** User-facing failure headlines come
  from localized resources; raw exception text is kept to the bounded technical-details field.
  (Audit of the remaining strings is part of the release gate — see Known limitations.)

### Security & privacy

- **Media metadata is stripped.** Audio/video conversions pass `-map_metadata -1 -map_chapters -1`,
  so GPS coordinates, device model, author and creation timestamps in the source container do not
  travel into the output.
- **Image metadata is not carried over.** Images are decoded (EXIF orientation applied) and
  re-encoded, so EXIF blocks — including GPS — are not copied into the result.
- **No ads, no analytics, no crash-reporting SDK, no account.** The only outbound request path in
  the app is the OCR pack downloader; it is user-initiated, HTTPS, digest-pinned, and sends no file,
  filename or identifier. Conversion itself never touches the network.
- **Backup disabled.** `android:allowBackup="false"` keeps conversion history and settings out of
  Android cloud backup.
- **Error details bounded.** FFmpeg stderr is reduced to a fixed number of log lines and a truncated
  detail field instead of being surfaced wholesale.
- **Absolute claims removed from product documents.** This pass replaces "fully offline" and
  "nothing leaves your phone" with a precise statement of the single network request; the
  claim-drift check in `scripts/verify.ps1` fails the build if the old figures reappear in
  `README.md`, `PRODUCT.md` or the verification report. (In-app taglines are handled by the UI
  workstream; see Known limitations.)

### Legal

- **Full licence texts ship with the app.** Apache-2.0, LGPL-2.1, LGPL-3.0, GPL-3.0 (for the
  licence text only), BSD-2-Clause (leptonica), BSD-0-Clause (XZ for Java), MIT (Bouncy Castle),
  IJG (libjpeg-turbo), SIL OFL 1.1 and the FFmpegKit component licences under
  `app/src/main/assets/licenses/`, rendered by the in-app Licences screen.
- **LGPL-3.0 compliance plan recorded.** The clearance memo documents the §4 duties for the
  FFmpegKit/FFmpeg fork (notice, licence copy, relink option, Installation Information,
  Corresponding Source offer). Publishing the Corresponding Source is a human task (B6 in the
  readiness plan), not something the build can satisfy.
- **OCR pack attribution.** The pack catalogue records source (tessdata_best, pinned commit),
  Apache-2.0 licence and URL for the model files; the app shows size, licence and source before
  download.
- **Privacy policy drafted** under `docs/legal/`, with the Data safety answers and the closed-test
  plan prepared in [`docs/product/store-listing.md`](docs/product/store-listing.md). Hosting the
  policy URL, submitting the Data safety form and obtaining counsel sign-off remain human tasks.
- No claim of legal clearance or trademark availability is made by this release.

### Build & release

- **Version 0.2.0 (versionCode 2).**
- **Release signing from a git-ignored keystore.** `keystore/keystore.properties` is read when
  present; no key material is committed. Without it the release build is unsigned, which is the
  intended state for CI and for anyone without the publisher's key.
- **Release build rail:** R8 code shrinking + resource shrinking with explicit keep rules; AAB for
  Play plus per-ABI APKs and a universal APK for direct distribution (arm64-v8a, x86_64); lint gates
  the release (`abortOnError = true`), with one documented, scoped suppression for a transitive
  PDFBox dependency.
- **Execution model:** WorkManager was removed. A user-started batch runs in a foreground service
  with a progress notification (`dataSync` type), so backgrounding the app no longer kills the
  queue; the service stops when the queue is idle.
- **Packaging:** native libraries are stored uncompressed and the manifest no longer forces native
  library extraction; `scripts/verify.ps1` checks 16 KB page alignment (`zipalign -P 16`) and the
  stored-ELF layout. Dependency resolution no longer needs JitPack; engines resolve from Google
  Maven and Maven Central.
- **Reproducible evidence:** `scripts/verify.ps1` runs the build, tests, packaging assertions, lint
  and the claim-drift check, and writes machine-readable evidence; a git baseline makes the
  before/after diff auditable.
- **Device QA is still pending** — the release gate requires it on physical devices, including
  Android 8/9 for the legacy storage path and Android 13+ for notification behaviour.

### Docs

- New root [`PRODUCT.md`](PRODUCT.md): one-page product definition — problem, users, the four
  outcomes, what `0.2.0` actually ships, the honest catalogue statement, non-goals, metrics, claims
  discipline, roadmap, human blockers.
- [`docs/product/PRODUCT.md`](docs/product/PRODUCT.md) rewritten as a capability specification:
  per-format matrix with limits, options reference, routing rules, execution and file behaviour,
  and the explicit list of what is not in `0.2.0`.
- [`docs/product/store-listing.md`](docs/product/store-listing.md): AR and EN listing copy within
  Play limits, screenshot shot list, Data safety answers, permissions summary, closed-test plan,
  claims blocklist, release gate.
- [`docs/product/permissions-rationale.md`](docs/product/permissions-rationale.md): every
  permission with its denial behaviour, the service type, the library-added permission, and the
  permissions deliberately not requested.
- This changelog.
- ADRs 0001–0003 (media engine, execution model, product wedge) and the production-readiness plan
  with workstreams, definition of done and human blockers.
- Architecture/plan documents are the source for these bullets; where a decision and the code
  differ, the verification report and the code win.

### Known limitations at this release

1. **No device QA yet.** Performance, codec coverage, RTL rendering and notification behaviour are
   unverified on real hardware.
2. **Absolute claims were removed from the UI in this pass.** The tagline is now "Convert on your
   phone. Files are not uploaded." and About says "Runs on your device", with the OCR pack named as
   the only network request. The store copy may be used only after the same claim pass on the
   listing itself (done in `docs/product/store-listing.md`).
3. **Four outcomes are not implemented** (many images → one PDF, scanned pages → one PDF, PDF
   merge/split/compress, compress-to-shareable-size). They are roadmap, not features.
4. **Office support is extraction** (text and tables), not layout-faithful conversion; first
   worksheet only, dates as serial numbers, PDF export row cap.
5. **OCR pack is a 28 MB download**, English and Arabic only. The download is SHA-256 pinned,
   resumable (HTTP Range) and warns on metered connections before it starts.
6. **HEIC needs Android 9+, AVIF Android 12+**; older devices get a limit message.
7. **No RAR, no 7Z creation, no archive browsing UI.**
8. **Upscaling is not available yet.** The UI says so; no ONNX Runtime and no upscaling weights are
   in the APK.
9. **No Play upload key and no store submission.** A throwaway local keystore exists only so the
   release rail can be exercised (`keystore/README.md`); the publisher's key and the console work are
   human tasks B1–B7.

### Verification status

| Item | Status at time of writing |
|---|---|
| JVM tests | 135 tests, 16 classes, 0 failures on the verified tree (baseline was 131/15); re-recorded by `scripts/verify.ps1` |
| Release APK/AAB, signing rail, 16 KB, ABI split | pass on the verified tree: 3 signed APKs + AAB, 32 stored `.so`, arm64 `p_align` 16384, zipalign pass — see `docs/qa/verification.md` |
| Claim drift (stale numbers in public docs) | enforced by the same script; green |
| Emulator smoke | install, cold start, resumed activity, 120-event monkey, no crash (`scripts/device-smoke.ps1`); not a substitute for device QA |
| Device QA (physical phones, EN+AR) | **not run** |
| Store listing, Data safety, closed test | **not submitted** |
