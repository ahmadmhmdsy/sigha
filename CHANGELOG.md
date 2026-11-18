# Changelog

All notable changes to Sigha, grouped per release. This file records **what changed**, not measured
results: APK sizes, hashes, test counts and format statistics change with every build and live in
`docs/qa/verification.md` and the machine-readable evidence written by `scripts/verify.ps1`.
Store-facing release notes live in [`docs/product/store-listing.md`](docs/product/store-listing.md).

The first tracked state of this repository is commit `11bccbb` ("baseline snapshot before
production-readiness pass"). Earlier prototype builds were not tagged and are not reconstructed
here.

## 0.2.1 — 2026-11-17 — UI/UX fix pass

Follow-up to the design critique of the 0.2.0 UI (`.impeccable/critique/` and
`docs/council/2026-11-17-ui-review/`): the two P0 findings and the three P1 workstreams chosen for
this release. No converter, routing or capability change; the format catalogue is the 0.2.0 one.

### Fixed

- **The OCR pack is no longer a dead end.** The workbench collects the pack download state, so
  installing the pack unlocks the OCR targets on the same screen without touching the selection.
  A missing pack is now an inline "Install OCR pack (size)" card with the existing confirmation
  dialog, and the card shows live download state with cancel. `PRODUCT.md` was corrected to match:
  targets are hidden while the pack is missing, not merely annotated.
- **Landscape is usable.** The empty state is a scrollable `LazyColumn` with a compact variant for
  viewports under 480 dp height (smaller icon, tightened rhythm), so the title, body, "Choose
  files" action and privacy footnote are all reachable in a 2400×1080 window. Verified in portrait,
  landscape, landscape at font scale 2.0 and dark mode.
- **Convert is never disabled without a reason.** When a target is required and none is selected,
  the bar shows `workbench_target_hint` above the disabled action instead of a silent dead button.
- **Folder mode names the destination.** The published path is the folder's display name plus the
  file name instead of a bare file name, and the saved-to line says where the file went.
- **Share-in navigates to Convert.** Files arriving from another app now open the workbench and
  announce "1 file added" instead of being added silently behind another tab.
- **Arabic page selection is accepted and validated.** The pages field normalises Arabic-Indic and
  Extended Arabic-Indic digits, U+060C/U+066C/U+061B commas and dashes to the ASCII form the parser
  understands, and reports an invalid entry inline (`option_pages_error`) instead of failing the
  conversion later.
- **History metadata no longer breaks mid-word in Arabic.** Date and size are atomic items in a
  `FlowRow`, so "10 كيلوبايت" wraps as a unit under the row actions.

### Arabic completeness

- **Lossy-conversion notes are typed and localised.** `core.Note` is a serialisable sealed interface
  with 29 kinds (page limits, truncation, archive duplicates/symlinks, office extraction, CSV/JSON
  shaping, EPUB/MOBI limits, OCR fallback, composite), each rendered by `NoteUi` from resources and
  shown in the queue, history and workbench rows. The engine no longer hands user-facing English
  text to the UI; the free-text `note` field is gone from the model.
- **Counted strings are `<plurals>`** with the six Arabic categories (zero/one/two/few/many/other)
  for files added, files selected, convert action, batch summary and history empty state; the
  invalid passive "نُجحت" is gone.
- **The OCR pack is named in Arabic.** The catalogue resolves title and description from resources
  (`pack_ocr_title`, `pack_ocr_description`) instead of English literals, and the confirmation
  dialog quotes the same strings.
- **Arabic privacy policy ships.** `assets/legal/privacy-policy.ar.md` is a nine-section
  translation, selected when the app locale is Arabic. Release is still gated on a native Arabic
  review of that text.
- **The first OCR language follows the device locale** (`OcrLanguages.defaultForLanguage`), so an
  Arabic phone starts with Arabic instead of returning Latin noise.
- **Arabic typography.** A separate type scale with 0 sp letter spacing and line height ×1.15 is
  selected when the app locale is Arabic; the Latin scale is unchanged.

### Window and theme ownership

- **One chrome owner.** `SighaNavHost` owns the app `Scaffold`, the navigation bar, `imePadding`,
  the horizontal safe-drawing inset and the bottom inset when no navigation bar is shown; the nine
  per-screen zero-inset overrides were removed. The workbench pages field can no longer sit under
  the keyboard.
- **System bars follow the resolved theme.** `enableEdgeToEdge()` no longer runs with defaults
  before settings load; `SystemBarStyle` is re-applied whenever the resolved `darkTheme` changes,
  so forced dark on a light phone no longer paints dark icons on a dark surface.
- **The Material 3 role ramp is authored in the Sigha palette** (onSurface/onBackground,
  onSurfaceVariant, the surfaceContainer family, inverse roles, outline, surfaceTint), so body text
  is ink, cards group against the background, and the navigation bar is no longer baseline lavender.
- **Shapes are configured** (`SighaShapes`: 6/10/14/20/28 dp) and cards use `shapes.large` with a
  1 dp outline instead of a 20 dp one-off radius.
- **`values-night` window background** removes the white flash on a dark cold start.

### Accessibility

- **State is announced, not only painted.** `StatusPill` exposes `stateDescription`, section
  headers are headings, the quality slider has a name and a state description, progress rows merge
  their descendants (and a queued row no longer announces "0 percent"), radio rows are one
  selectable group with `Role.RadioButton`, and a source→target pair is one announcement
  ("TXT to PDF") in the queue and history.
- **Contrast and target fixes.** Format badges use `onSurface` on a 12% accent tint (7 of 13 were
  3.76–4.37:1), status pills use tonal containers (3.99:1), the "Choose folder" action is at least
  48 dp tall, and the top bar is one line so the title survives font scale 2.0.

### Docs

- `PRODUCT.md` / `docs/product/PRODUCT.md`: OCR inline install and live target refresh, resumable
  pack download, no per-row retry in this release, and tablets/foldables/desktop declared out of
  scope for the Arabic-first phone path.
- `docs/qa/verification.md`: 0.2.1 facts, the emulator-5556 visual/RTL evidence, and the corrected
  statement about engine notes (now localised; failure diagnostics stay English).
- `docs/plan/production-readiness.md`: the "engine note text stays English" claim corrected.

### Verification status

| Item | Status at time of writing |
|---|---|
| JVM tests | 137 tests, 16 classes, 0 failures/errors/skips |
| Debug APK, release APKs/AAB, signing, 16 KB, ABI split | green; sizes, SHA-256 and version re-recorded in `docs/qa/verification.md` |
| Emulator smoke (install, cold start, resumed activity, 120-event monkey, no crash) | pass on emulator-5554 |
| Emulator visual pass (AR, portrait/landscape, font scale 2.0, dark) | pass on emulator-5556; screenshots in `docs/qa/evidence/ui-021/` |
| OCR inline install | exercised end to end on the emulator: card → dialog → 28 MB download → TXT target appears live |
| Arabic policy native review | **not run** — release gate |
| Device QA (physical phones, EN+AR) | **not run** |

### Follow-up — critique re-run regressions (2026-11-18)

The 0.2.1 UI was re-critiqued after the fix pass (`docs/council/2026-11-18-ui-review/`). The design
score moved 19 → 24/40 and no P0 remained, but the re-run found debts created by the pass itself;
they are fixed here, without changing the 0.2.1 scope or the format catalogue.

- **The pages validation now gates Convert.** An invalid pages range disables the action, the
  ViewModel refuses it with the field message, the field exposes `error()` semantics and its helper
  is a polite live region, and the disabled button carries the reason as a `stateDescription`.
- **A share-in cannot mutate a running batch.** `addFiles` refuses incoming files while converting
  and says so; the UI-only guard is now enforced in the ViewModel too.
- **The target hint no longer asks for the impossible.** It renders only when a reachable target
  exists, so it cannot contradict the "no reachable target" card.
- **Arabic plural quantity is correct.** `PagesLimited` pluralises on the pages actually read, not
  the document length (a 101-page scan no longer renders the "one" form); the `two` item keeps the
  dual; U+066C is dropped instead of becoming a page separator.
- **One card grammar.** History rows use the outlined `SighaCard` (and lose a duplicate open
  control); Queue rows use the same card. "Not available yet" uses a neutral container instead of
  the sand selection colour.
- **The batch outcome is visible in place.** Done rows in the workbench and the queue render the
  saved path, two lines and bidi-wrapped, instead of holding it in the model only.
- **Announcement hygiene.** Status pills no longer announce their label twice, route rows use
  `clearAndSetSemantics`, exclusive chip rows are selectable groups, a queued row no longer
  announces "0 percent", and a cancelled summary keeps its done/failed counts.
- **Platform polish.** The missing `tertiaryContainer`/`surfaceBright`/`surfaceDim`/`scrim` roles
  are authored, the Queue empty state is scrollable in landscape and at font scale 2.0, and privacy
  headings are exposed as headings.

Re-verified with `scripts/verify.ps1` and `scripts/device-smoke.ps1` on 2026-11-18; measured
numbers live in `docs/qa/verification.md` and the evidence JSON files.

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
