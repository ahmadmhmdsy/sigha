# Seat: UX flows, information architecture and states

Scope: the six primary jobs (share/in → target → convert → find; batch; scanned PDF → OCR text; cancel;
return after a kill; revoke the folder; clear history), plus the IA and naming of Convert / Queue /
History / Settings / Model packs. All citations were read in the working tree on 2026-11-17.
Paths are relative to the repo root.

## Verdict

The workbench's happy path is calm and well built — one page, one primary action, honest empty
states — but the app has no memory of what it did. Selection, queue and results live in memory and
in a four-second snackbar, so every interruption, revocation or partial failure ends in the same
place: the user cannot tell what happened, where it went, or what is left, and the only recovery the
UI offers is to rebuild the batch and run it again. The OCR path the product is built around is a
dead end in the shipped build: installing the 28 MB pack does not change the screen that told you to
install it.

## Strengths

1. **States are written down, not implied.** Every list has an empty state, failures get a localized
   headline with the English diagnostic behind "Show technical details", and the Privacy screen even
   has its own loading and load-failure states — the pattern to copy elsewhere
   (ui/QueueScreen.kt:236-264, ui/FailureUi.kt:12-28, ui/PrivacyScreen.kt:143-159).
2. **Conversion is not owned by a screen.** The queue is app-scoped, the ViewModel mirrors it, and a
   foreground service keeps the batch alive across tab switches and backgrounding
   (ui/WorkbenchViewModel.kt:63-94, work/ConversionQueue.kt:97-124, work/ConversionService.kt:26-51).
   Leaving Convert while a batch runs is genuinely safe.
3. **The genuinely destructive actions are confirmed and explained.** History clear names the exact
   consequence ("This only clears the list. Converted files stay in your storage."), the folder picker
   refuses an unwritable tree up front, and the pack download states size, licence and metered risk
   before spending data (ui/HistoryScreen.kt:142-157, ui/SettingsScreen.kt:97-105,
   ui/ModelPacksScreen.kt:114-153).

## Findings

| # | Sev | Finding | Evidence | Why it matters | Fix |
|---|-----|---------|----------|----------------|-----|
| 1 | **P0** | Installing the OCR pack does not refresh the Workbench. Targets and the OCR hint are computed only when the selection or the target changes, so a user who obeys the on-screen instruction installs 28 MB, comes back, and still sees no TXT target and the same hint. | ui/WorkbenchViewModel.kt:239-249 (`withDerived`, called only from :119, :132, :140, :149), ui/WorkbenchViewModel.kt:277-286, res/values/strings_ui.xml:25 | The one OCR path the product is built around fails on its own instruction; the only fix is to disturb the selection or tap an unrelated target. On metered Iraqi data that is 28 MB spent for a feature that appears not to exist. | Observe pack install state in the VM and recompute on change; turn the hint into an inline "Install OCR pack (28 MB)" action that opens the existing confirmation in place. |
| 2 | **P1** | Results appear at the end of a scrolling list; the fixed bar only ever says "Converting…". | ui/WorkbenchScreen.kt:255-297 (order: files → target → options → progress), :650-659, :663 | The action is pinned at the bottom and its outcome is appended below the fold; a 5-file batch leaves the progress card two screens away with no auto-scroll, and nothing persistent says "file 3 of 12". | Put n-of-m, current file and cancel in the bar; swap the file list for the progress list on start; make the completed bar a "Show results" button. |
| 3 | **P1** | No per-item retry. A failed row is a dead end, and the only retry the UI offers re-runs the entire selection. | ui/QueueScreen.kt:236-264, :272-310, ui/WorkbenchViewModel.kt:166-187, docs/product/PRODUCT.md:192 ("retry") | After 8 of 10 succeed, pressing Convert again re-converts all 10 and writes 8 duplicates (`UniqueNames` → " (1)"), which the user must find and delete. PRODUCT.md promises retry; no retry string or control exists in the app. | Per-row Retry that re-enqueues input + options; or "Retry 2 failed" that only re-runs FAILED rows and drops completed files from the selection. |
| 4 | **P1** | The target picker blames the set, not the file. One dead format kills every target, with the message "These files have no target in common. Try files of the same kind."; the only error vocabulary is "This conversion isn't supported yet." | ui/WorkbenchViewModel.kt:277-286, ui/WorkbenchScreen.kt:367-374, res/values/strings.xml:49, ui/FailureUi.kt:14, res/values/strings.xml:184, core/FormatRegistry.kt:73-83, docs/product/PRODUCT.md:17-18 | Nine recognised formats (TIFF, RTF, TAR, BZ2, XZ, YAML, XML, MOBI, FB2) and any unknown-but-MIME-bearing file are selectable and produce a false explanation; "yet" implies future support, which PRODUCT §1 explicitly forbids. | Show per-file reachability on the card ("No conversion available for TIFF") and name the blocking file; a dedicated dead-format message with a remove action; drop "yet". |
| 5 | **P1** | The Arabic PDF-pages hint teaches an input the parser rejects, and the field is never validated. | res/values-ar/strings.xml:57 ("مثال: 1-3، 5", U+060C), engine/pdf/PdfPageSelection.kt:24 (`split(',')` only), ui/WorkbenchViewModel.kt:162-164 (no validation), engine/pdf/PdfToImageConverter.kt:131-139, engine/pdf/PdfToTextConverter.kt:104-112, res/values/strings_ui.xml:8 / :4 | An Arabic keyboard's comma is U+060C, so the example in the app fails; the failure surfaces only after the batch runs, as the generic headline "Conversion failed." with an English detail behind a disclosure. | Accept U+060C (and Arabic-Indic digits), normalize before parsing, validate inline with supporting-text error and a disabled Convert. |
| 6 | **P1** | In folder mode the app never names the destination, and a revoked folder is reported only as a per-row footnote after the batch has run. | data/OutputPublisher.kt:252-258 (`displayPath = null`), ui/QueueScreen.kt:272-278 ("Saved"), ui/HistoryScreen.kt:253-262 (note only, never the path), data/ConversionRunner.kt:130-142, ui/SettingsScreen.kt:97-105 | Rows say "Saved" with no location, the opposite of "find the result"; after revocation the summary still reads "All files converted" (ui/WorkbenchViewModel.kt:226-232) and the destination appears only as a small note under each row (`ui_note_output_folder_fallback`), while Settings keeps offering the dead folder because the grant is checked only when it is picked. | Show the folder name per row; re-validate the grant when Settings opens and before a batch starts, with a banner; name the destination in the completion summary. |
| 7 | **P1** | A process kill mid-batch leaves one transient snackbar and no state. | ui/WorkbenchViewModel.kt:77-78 (`MutableSharedFlow`, replay 0), :95-99 (flag consumed in `init`), ui/WorkbenchScreen.kt:105-106 (collector), work/ConversionQueue.kt:75-76 (in-memory rows), data/SettingsStore.kt:78-88 (single boolean) | The queue and selection are gone, History holds only completed files, and the message names nothing. "Files that were not converted can be added again" = re-pick 12 files through SAF from memory. | Persist the batch (URIs, target, options, per-row status); restore it on launch with "Retry N unfinished", or at minimum a dismissible banner with a count linking to History. |
| 8 | **P1** | Sharing a file while the app sits on another tab does nothing visible. | MainActivity.kt:53-57, :74-79, data/IncomingFilesBus.kt:22-32, ui/WorkbenchViewModel.kt:83-91, ui/SighaNavHost.kt:90-130 | The file is appended to a background tab's selection; if a batch is running, the append also invalidates the chosen target (ui/WorkbenchViewModel.kt:239-241) and shifts the layout under the user. | Navigate to Convert on incoming-file events, confirm with "1 file added", and guard `addFiles` while converting. |
| 9 | **P2** | The queue is an icon-only destination with four names and no presence outside one tab; the OS notification cannot be tapped or used to cancel. | ui/WorkbenchScreen.kt:152-159, res/values/strings.xml:27, :62, :63, :74 (View queue / Progress / View results / Queue), ui/SighaNavHost.kt:37, :45, work/ConversionService.kt:74-91 | Progress is only visible if you remember an unlabelled list icon on the Convert tab; the one always-visible surface (the notification) neither opens the app nor offers Cancel. | One name ("Results"), a tab or a badge on the Convert tab, plus `setContentIntent` and a Cancel action on the notification. |
| 10 | **P2** | Four "clear" actions with three scopes, one of them unconfirmed; history deletion is all-or-nothing. | ui/WorkbenchScreen.kt:244-252 (no confirm), :529-531, ui/QueueScreen.kt:87-91, ui/HistoryScreen.kt:74-83, :142-157, data/HistoryStore.kt:39-44 (`remove()` has no caller) | The X sits next to "Add more"; one tap destroys a 30-file selection with no undo, while deleting one sensitive history row requires wiping all 50. | One vocabulary by scope (Remove / Clear results / Delete history), undo for selection removal, per-row swipe-to-delete in History. |
| 11 | **P2** | An installed model pack cannot be removed; a "Not available yet" AI card advertises a feature PRODUCT.md says must not be advertised. | ui/ModelPacksScreen.kt:288-294, model/ModelPacks.kt:43-116 (no removal API), docs/product/PRODUCT.md:195 ("install/remove"), res/values/strings.xml:159-161, ui/ModelPacksScreen.kt:234-239 | 28 MB is permanent once installed, and the Settings-adjacent surface promises image enhancement that does not exist — the exact overclaiming the product doc forbids. | Add Remove (delete `installDir`, confirm); drop the upscale section from the shipped build. |
| 12 | **P2** | Engine notes — the only warning that pages, sheets or lines were dropped — are hard-coded English inside a localized "Note: %1$s". | engine/pdf/PdfToTextConverter.kt:100, engine/office/OfficeConverters.kt:295-299, engine/text/TextConverters.kt:44-48, engine/media/VideoConverter.kt:46, :60, ui/WorkbenchScreen.kt:606-613, res/values/strings_ui.xml:18 | In the Arabic build the most consequential messages ("Only the first sheet was exported", "No text layer found; used OCR") are English, and they are not behind the technical-details disclosure. | Type the notes (kind + int args), localize the headline, keep the engine string in details. |
| 13 | **P2** | The Convert button's disabled state has no explanation, and an invalidated target deselects with no message. | ui/WorkbenchScreen.kt:663, ui/WorkbenchViewModel.kt:239-241, res/values/strings.xml:48 (`workbench_target_hint` defined, zero usages) | The bar is disabled with the reason possibly off-screen, and adding a file silently clears the user's format choice. | Render the hint under the bar, scroll/flash the target card, and announce "Target reset". |
| 14 | **P2** | Batches over 60 files silently lose rows; the picker has no cap and no warning. | work/ConversionQueue.kt:96, :285, ui/WorkbenchViewModel.kt:203, ui/WorkbenchScreen.kt:189 | A 70-photo batch converts all 70 but shows 60; the completion summary under-reports and 10 results exist only in History. | Cap selection at 60 with a message, or page the queue instead of truncating. |
| 15 | **P3** | "Image quality" is shown for PNG targets, where it does nothing. | ui/WorkbenchViewModel.kt:242-244, ui/WorkbenchScreen.kt:432-453, engine/image/ImageToImageConverter.kt:72-88, :77 | PNG is lossless: the one control a user reaches for to shrink a file is inert exactly where they try it. | Hide quality for PNG targets or say "quality applies to JPG/WebP only". |

## Dimension scores

| Dimension | Score | One-line justification |
|---|---|---|
| Visibility of status | **2** | Per-row progress, status pills and a notification exist, but batch position is never in the bar you are looking at, results are below the fold, and folder-mode output has no visible location. |
| User control and freedom | **2** | Cancel exists in two places and History clear is confirmed, but there is no per-row cancel or retry, no undo for selection loss, and stop/cancel is unreachable from Settings or History. |
| Error prevention and recovery | **1** | Pages input is unvalidated and taught wrong, dead formats are selectable and mis-explained, every failed row is a dead end, and the remedy for a missing pack is a prose navigation path. |
| Recognition vs recall | **1** | The OCR fix, the retry, the output location and the queue itself all require remembering where something is; the queue has four names and no label. |
| Cognitive load | **2** | One action, few options and scannable chips; but a six-section single scroll, a duplicated results surface (progress card + Queue + History) and four different "clear"s tax a low-literacy, non-English-first audience. |

## Persona red flags

**"Office phone" user (Arabic, Iraq, converts a bank PDF and receipts for WhatsApp).**
She opens a scanned PDF → no TXT target → reads "OCR targets appear after the OCR pack is installed"
→ Settings → Model packs → 28 MB confirm (metered warning) → back → still no TXT, same hint. She
types "1-3، 5" with her Arabic keyboard into "PDF pages" and gets "فشل التحويل." Later she picks a
folder for output, converts, sees only "تم الحفظ" with no folder named, and when the SD card's grant
dies the batch lands in Downloads while the summary still says "اكتمل تحويل جميع الملفات" — the
destination is only a footnote under each row.

**Privacy-conscious user (clears after every session).**
First launch asks for notification permission before he has picked a file. He deletes one history row
by deleting all fifty; he cannot remove the 28 MB OCR pack he tried once. He kills the app during a
batch, reopens it, and the only trace is a snackbar he may have missed — the Queue says "Nothing in
the queue" and History shows only what finished.

## Qiyas cards

### Card 1 — give a batch a durable identity and per-file recovery

- **Al-Far' (proposal):** persist the batch (input URIs + target + options + per-row terminal state)
  and make recovery a first-class, in-place action: a "Results" surface that survives process death,
  per-row Retry, and a "Retry failed (2)" action that re-enqueues only FAILED rows.
- **Al-Asl (precedent):** Chrome for Android's Downloads page and the Play Store's download queue —
  both survive process death, list every item with a terminal state, and expose Retry per item
  instead of asking the user to start the download again.
- **Al-'Illa (causal invariant):** a job's identity and terminal state must outlive the surface that
  started it. If they live in the same memory as the screen, every interruption converts the user's
  work into manual reconstruction, and a "retry" that re-runs the whole set manufactures duplicates.
  The invariant: every async job has a durable identity, a terminal state and a one-tap recovery.
- **Conditions:** storage and queue must stay offline (DataStore/SQLite, no network); retry must
  re-run only FAILED rows and must not re-publish files that already exist; persisted SAF grants are
  capped, so the batch must re-request rather than hoard them; the row state must be reconciled with
  History on restore so a file finished just before the kill is not retried.

### Card 2 — a capability is never hidden, only gated, and the gate is actionable in place

- **Al-Far' (proposal):** always show OCR targets, marked with an "installed? / needs pack" state.
  Tapping one when the pack is absent opens the existing 28 MB confirmation as a sheet from the
  Workbench; when the install finishes the list and the pack state update in place, with no
  navigation and no selection change.
- **Al-Asl (precedent):** Google Translate's offline languages and Gboard's language list — the
  language stays visible with a download affordance and size, and the row updates in place once the
  download completes.
- **Al-'Illa (causal invariant):** availability is an attribute of the artifact the user can see, not
  a filter applied to the list. Hiding a target converts "this needs a pack" into "the app is broken",
  and any view computed once from that hidden state goes stale the moment the state it depends on
  changes. The invariant: every supported capability is visible with an explicit availability state
  and an in-place acquisition path.
- **Conditions:** the Workbench must observe pack state (today it reads it only inside
  `withDerived`, ui/WorkbenchViewModel.kt:246); a partially installed pack must be handled; the
  target must still fail gracefully if the pack disappears between selection and run; PRODUCT.md:135
  (targets visible without the pack) must be made the single truth.

## Fatal flaw

The app cannot answer "what happened?" — not after a kill, not after a revoked folder, not after a
partial failure. Selection, queue and results live only in memory, the durable residue is a single
boolean and a four-second snackbar, and the one persistent surface (the notification) does not open
the app. For an audience that treats the phone as its office and transfers files it cannot re-source,
that turns every interruption into lost work and every failure into a manual redo.

## Questions the design team should answer

1. When the workbench tells the user to install the OCR pack and they do, what is supposed to change
   on screen? Nothing does today — is that a build regression or an undesigned transition?
2. Which document is authoritative on OCR targets — `docs/product/PRODUCT.md:135` ("visible without
   the pack") or `ui/WorkbenchViewModel.kt:284` (hidden)? The two are opposite.
3. What should "retry" mean for a partial batch, given PRODUCT.md promises it and the Queue has no
   such control? Re-running everything is not retry; it is re-conversion with duplicates.
4. In folder mode, where does the app ever state the destination path? If the answer is "nowhere",
   what is the user's model of where their file went?
5. After process death, what is the user expected to do, and how do they learn which files are left?
   Is a boolean flag plus a transient snackbar the intended end state for 0.2.0?
6. Do the numeric fields accept Arabic-Indic digits and Arabic punctuation? If not, what is the
   Arabic hint at `res/values-ar/strings.xml:57` teaching?
