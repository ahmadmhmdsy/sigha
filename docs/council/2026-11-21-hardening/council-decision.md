# Council decision — production hardening review, 2026-11-21

Status: accepted. Owner: Lead. Applies to the unreleased 0.2.1 build (versionCode 3).
Inputs: the engine/data audit, the UI/UX audit, the first upload-tree runs (2026-11-19), and the
two external research notes in `docs/research/2026-11-21-*.md`.

## 1. Question put to the council

The user asked for a build that "will not fail" on a real phone: edge cases, fallbacks, try/catch,
error bounding, higher UX, higher retention, and no crash path — working only against x86_64
emulators until an arm64 phone is available. Also: explain the silent-video finding, apply the fix if
it helps users, add batch processing and any competitor feature that is genuinely worth it, and find
gaps competitors leave unfixed that Sigha could own.

## 2. Verdict

**Sigha is not production-ready, and this pass does not change that.** It is a materially hardened
QA build: the known crash and data-loss paths found by three audits are closed and covered by tests
and emulator runs, while the release blockers are unchanged and are not code problems:

| Blocker | State |
|---|---|
| 21 placeholder values in shipped legal assets (14 reachable) | unchanged; `verify.ps1 -ReleaseCandidate` fails until the publisher supplies real values |
| QA debug key signature | unchanged |
| No arm64 run on a physical phone | unchanged (user deferred the phone) |
| No upload-key-signed AAB installed once on arm64 | unchanged |
| B1–B7 legal/publisher items | unchanged |
| Play account, owned domain, mailboxes | not created |
| Foreground-service declaration + demo video | pending |
| Closed-test clock | not started |

What did change: an emulator-verified, crash-bounded build with typed failure paths, per-output
publishing, bounded resource use and a fixed silent-video UX. That is the correct scope for the
deferred-device phase.

## 3. Findings and dispositions

### Fixed in this pass

| # | Finding (severity) | Fix |
|---|---|---|
| 1 | Silent video offered audio targets that always failed (P0 UX) | Media probe hides audio targets only for silent video; typed `NO_AUDIO_TRACK` / `NO_VIDEO_TRACK`; hint string EN+AR |
| 2 | A single failed output page discarded a whole successful conversion (P0 data loss) | Per-output publish with `PartialPublish(failed)`; total failure only raises `SAVE_FAILED` |
| 3 | Notification built without a tap target (P1 UX) | `contentIntent()` with guarded PendingIntent |
| 4 | No Share on the workbench result row (P1 UX) | Share `IconButton` on DONE rows with a `queue_share_failed` fallback |
| 5 | Unknown format id could `error()` at startup (P0 crash) | `FormatRegistry.detect` returns null; per-family `runCatching`; defensive OCR family; `FormatRegistryTest` |
| 6 | Corrupt history row dropped the entire history (P1) | Per-entry decode with skip; test `oneCorruptEntryDoesNotDiscardTheRest` |
| 7 | Legacy string notes leaked into the typed model (P2) | Stripped in `HistoryCodec`; test |
| 8 | Deep JSON/CSV input could overflow the recursive parser (P1 crash) | Pre-parse string-aware depth guard at 256, `StackOverflowError` → `FILE_TOO_LARGE`; two tests |
| 9 | PDF→image/text had no upper bound (P1 OOM) | Caps 500 / 2,000 pages with actionable messages; text→PDF capped at 400 pages |
| 10 | `.part` resume could resume onto a corrupt partial file (P1 data corruption) | Full-length verification before the range request; restart from zero; 4xx final except 408/429/416 |
| 11 | Settings writes could throw out of a click handler (P1 crash) | `saveSettings` helper + `ui_settings_save_failed` |
| 12 | Non-`Exception` throwables escaped the queue (P2) | `Throwable` catch; OOM → `FILE_TOO_LARGE`; rethrow fatal VM errors |
| 13 | Folder revoked mid-run failed a whole batch (P1 UX) | `FolderFallback` to Downloads with a typed note |
| 14 | Storage denial on API 26–28 had no recovery (P1 UX) | Settings action + SAF folder escape hatch + copy fix |
| 15 | MediaStore pending row could leak on failure (P2) | Single insert transaction, delete-on-failure, read-back display name, legacy scan URI by path |
| 16 | Foreground service start/stop races could notify after stop (P2) | `stopService` fallback, explicit cancel, `update()` no-op after stop, start-failure cancel |
| 17 | Duplicate Compose keys crash in History (P1) | Stable keys |
| 18 | OCR/engine note text still in English in one path (P2) | Typed note sealed interface already in 0.2.1; OCR converters defensive |
| 19 | 137-test suite did not cover any of the above | +12 tests, 149 total |
| 20 | No written UX/retention or competitive record | Two research notes + this decision |

### Deliberately not fixed in this pass

Each was assessed and deferred with a reason, not ignored:

- **Persistent result card / Share all / completion notification / open folder / copy path** —
  medium effort, UX-only; scheduled for the next UI cycle. The Share button added here is the
  smallest useful slice.
- **Contextual notification priming** — needs a product decision on when a batch is "likely to
  outlive the screen"; the current ask is harmless on a fresh install but wrong on a reinstall.
- **Image/video/audio collection routing** — changes the app's published-location contract and needs
  a "keep one folder" toggle; needs device QA for gallery visibility, which the emulator cannot
  fully prove.
- **Batch retry-failed, History re-run/delete, shortcuts, empty-state sample** — queued with the UX
  note; no crash/data-loss risk.
- **PDF workshop (merge/split/organise/compress/password/scan)** — the largest real competitive gap,
  but it is new capability, not hardening; the next cycle starts with merge/split/compress.
- **HEIC on API 26–27** — small FFmpeg fallback; queued because it needs its own decode test matrix.
- **Arabic searchable PDF** — the wedge, high effort/high risk; needs a corpus and a benchmark before
  code.
- **Release blockers (placeholders, keys, arm64, Play account, domain)** — human/publisher work, out
  of code scope.

## 4. Corrections to earlier claims

- Earlier stated intent to request `POST_NOTIFICATIONS` on launch is an anti-pattern; the permission
  rationale doc already says it should be contextual. Not changed yet (deferred above).
- The 2026-11-19 device-smoke notes said "video with no audio track is found, not fixed". It is fixed
  here and verified on the emulator.
- The 0.2.1 follow-up notes said "no per-row retry in this release" — still true; retry remains
  deferred and is now tracked with batch retry-failed.
- No format-count, size, or performance claim was introduced by this pass; numbers in the docs are
  the ones measured by the gate and recorded in `docs/qa/verification.md`.

## 5. Evidence required before calling 0.2.1 a release candidate

1. Full `scripts/verify.ps1` green on the final tree, with the evidence JSON committed.
2. `scripts/device-smoke.ps1` green on API 36 x86_64 (cold start, resume, monkey, no crash).
3. Silent-video, audio-video, batch and settings-output-mode flows exercised through the UI on the
   final APK.
4. `-ReleaseCandidate` still expected to fail on placeholders: that failure is the honest state until
   the publisher supplies values.
5. No claim-drift hits; EN/AR parity and plural categories green.

Current status of 1–5 is recorded in `docs/qa/verification.md` §11 (production-hardening pass).

## 6. Next cycle (unchanged priority order)

1. Persistent result card + Share all + completion notification tap-through.
2. Image/video/audio collection routing with "keep one folder" toggle.
3. PDF workshop: merge, split/extract, page organiser, target-size compression.
4. HEIC on API 26–27; save-location UX; ML Kit scanner.
5. Arabic searchable PDF MVP with a verification view.

The 90-day plan and buy thresholds are in `docs/research/2026-11-21-competitive-gap.md`.
