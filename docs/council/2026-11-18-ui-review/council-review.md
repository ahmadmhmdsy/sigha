# Sigha UI/UX Design Critique — Impeccable × Qiyas Council (0.2.1 delta re-run)

**Method: design review only (detector not applicable, native platform)**

Target: `app/src/main/java/com/sigha/converter/ui` — 22 Kotlin files, plus the strings, manifest and the screens they render.
Product: Sigha / صيغة 0.2.1 (`com.sigha.converter`, versionCode 3) — offline file converter, Arabic-first MENA audience, no account, no uploads.
Mode: **Operate** (task-completion utility; all ten Nielsen heuristics apply).
Date: 2026-11-18. Assessment is read-only and covers tree `90d81a9` (fix pass `ed3ee6c`); the follow-up regression closure is reported separately at the end and is not part of the score.

Evidence base, and its limits:

- Static review of the UI package, `ui/theme/*`, `res/values*/strings*.xml`, `AndroidManifest.xml`, `themes.xml`, the engines' note/failure producers and the 16 test classes.
- 0.2.1 emulator evidence: 23 PNGs in `docs/qa/evidence/ui-021/` (API 36, 1080×2400 @ 420 dpi, Arabic RTL except `home-empty-en.png`) and `docs/qa/verification.md` §7–§8. Two of them were captured for this re-run from the final tree: `home-empty-ar-landscape-recheck.png` (2400×1080, fresh install, 3-button nav) and `ime-pages-ar-recheck.png` (keyboard open on the pages field). Copies are archived in `docs/council/2026-11-18-ui-review/evidence/`.
- Contrast ratios are **computed** from authored tokens in sRGB with M3 tonal compositing stated, then cross-checked against sampled screenshot pixels; chrome heights are arithmetic from declared dp plus M3 defaults. Neither is a probe measurement.
- **Not run:** a TalkBack session, a native Arabic copy review, an OEM-skin device (One UI/MIUI/EMUI), a tablet/foldable, a physical device. Where a finding depends on those it is marked *inferred*.

---

## Council seated

Same Qiyas mode C council as the 0.2.0 critique, re-run as a delta. The five dynamic seats were replayed by the same five teammates; the Lead held Engineer, Devil's Advocate and Maestro. The 8-member session limit is why the 8 baseline roles map onto 5 seat reports.

| Baseline role | 0.2.1 seat | Mandate carried into this run |
|---|---|---|
| The Expert | design-director | Visual language, specificity, second-order effects |
| Platform Guardian | material-conformance | Material 3, window contract, platform behaviour |
| Accessibility Auditor | a11y-auditor | TalkBack, low vision, motor, contrast |
| Arabic-First Editor | app-auditor | Arabic copy, RTL, bidi, typography |
| Naive User / UX flows | product-strategist | Task completion, error prevention, recovery |
| Engineer, Devil's Advocate, Maestro | Lead | Synthesis, adjudication, verdict |

---

## Step 0 — Frame and invariants

**Goal.** A closed-test build an Arabic-first, non-technical user can trust for real paperwork: pick a file, convert it, find it, and understand what happened.

**Hard constraints.** Android 26–36; Material 3 is the rulebook; offline by design; no account; primary audience Arabic-speaking MENA (Iraq first), often on metered data, often on 3-button navigation, often at font scale 1.3–2.0.

**Invariants carried from 0.2.0 (unchanged, now testable):**

1. **Trust is the product.** Every moment the app speaks — a note, a failure, a policy, a permission prompt — pays the promise back or spends it.
2. **The app must answer "what happened?" after any interruption.**
3. **Arabic is an architecture, not a translation layer.** Grammar is a function of count; text is chosen where the locale is known.
4. **The window is one surface.** Insets and bar appearance are properties of the window, consumed once.
5. **A capability is never hidden, only gated, and the gate is actionable in place.**

---

## What changed since 0.2.0

The 0.2.1 pass (`ed3ee6c`, 83 files, +3,044/−398) did what the council asked, and it did it at cause level:

- **OCR dead end removed.** The Workbench observes pack state (`WorkbenchViewModel.kt:108-111`), re-derives targets live, and the hint became an inline install card with size, confirmation, progress and cancel (`WorkbenchScreen.kt:677-800`). Verified end to end: a 28,004,325-byte download made the TXT target appear without touching the selection (`ocr-pack-card-ar.png` → `ocr-unlocked-ar.png`).
- **Landscape unblocked.** The empty state is a scrollable, compact-aware `LazyColumn` (`WorkbenchScreen.kt:186-230`, `Common.kt:110-173`); 2400×1080 at font 1.0 and 2.0 both keep the CTA reachable.
- **Window and theme got one owner.** Root `Scaffold` + one `NavHost` inset strategy (`SighaNavHost.kt:75-134`), system bars re-derived from the resolved theme (`MainActivity.kt:56-67`), authored light/dark role ramps (`Theme.kt:21-101`), authored shapes (`Shape.kt`), night window background (`values-night/themes.xml`).
- **Arabic became resources.** Typed `Note` (29 kinds) → `NoteUi.kt` with no literals, 16 plural sets × six categories (96 forms), localised pack catalogue, Arabic privacy asset, Arabic type rhythm.
- **Accessibility gained state, not just labels.** Headings (14 sites), radio groups, named slider, tonal pills, merged progress rows, 48 dp floors, AA contrast.

**What the pass did not do:** it authored the system, not the identity; it fixed the card token where it was used but not the two list screens; and it introduced a small set of new consistency debts (below).

---

## Design Health Score — 0.2.1

| # | Heuristic | 0.2.0 | 0.2.1 | Key issue |
|---|---|---|---|---|
| 1 | Visibility of System Status | 2 | **3** | OCR install now reports and reveals itself live, per-row states are localised and openable, notes speak Arabic. Still no batch position (n-of-m) in the pinned bar and no destination in the summary. |
| 2 | Match System / Real World | 2 | **3** | Notes, packs, policy and counts now exist in Arabic, with six plural categories. Residual: bare "OCR", numeral policy written but unenforced, bidi still raw interpolation. |
| 3 | User Control and Freedom | 2 | **2** | Cancel exists in the converting bar and on the OCR card; still no per-row retry, no per-row history delete, no undo for a cleared selection, four "clear" scopes. |
| 4 | Consistency and Standards | 2 | **3** | One window owner, one role ramp, one shape scale, tonal pills. But two card grammars now coexist (outlined `SighaCard` vs borderless History/Queue rows) and sand means both "selected" and "not available yet". |
| 5 | Error Prevention | 2 | **2** | Pages input is normalised and visibly validated — but Convert is not gated on it, so a knowingly-invalid batch still runs and fails per file (N1). Dead formats remain selectable; a target reset is still silent. |
| 6 | Recognition Rather Than Recall | 2 | **2** | The disabled CTA now explains itself and the OCR route is on the page. The required target still sits below the file list, the queue is still an icon-only destination with four names. |
| 7 | Flexibility and Efficiency | 2 | **2** | Batch, add-more and share-in work; still no per-item retry, no convert-again from history, no multi-select, eight bitrate chips in one row. |
| 8 | Aesthetic and Minimalist Design | 2 | **3** | The palette is a scheme now (mist page, ink text, emerald action, sand selection), cards read as cards, dark is authored rather than inverted. Composition still stacks two bottom bars; 13 category accents still compete with the role ramp. |
| 9 | Error Recovery | 1 | **2** | The OCR dead end is gone and notes are localised; failures are still one generic headline with English details, failed rows are dead ends, a process kill still leaves one transient snackbar. |
| 10 | Help and Documentation | 2 | **2** | Arabic privacy policy and in-context OCR help exist; licence bodies stay English, no format/limit help exists in context, and a "Not available yet" AI card still advertises an unbuilt feature. |
| **Total** | | **19 / 40** | **24 / 40** | **Acceptable (20–27) — targeted improvements needed, no longer "major overhaul"** |

Band note: the +5 is concentrated exactly where the pass aimed — status, real-world language, consistency, aesthetics, recovery. Control, prevention, recognition and efficiency did not move, and every one of those four is now about the *batch outcome surface* rather than the chrome.

---

## Design Specificity Verdict

**2 / 4 — a recognisable system, not yet an identity.** (was 1.5 / 4)

The palette is a scheme rather than an accent: mist page, ink text, emerald action, sand selection, a green-grey container ramp, and a dark theme authored independently (`Theme.kt:21-101`; `home-empty-ar-dark.png`). Shape is authored (6→28 dp), cards have a hairline instead of an invisible tonal tint, and the empty state is composed for this product (product title, on-device promise as body, supported-format footnote, compact landscape). Arabic finally has its own leading and tracking, and a real selection accent exists. A designer would recognise a Sigha skin; this is no longer stock M3 with a green button.

It is still category-interchangeable where it matters: the mark is a stock `SwapHoriz` glyph used six times, the Arabic voice belongs to the OEM font, the launcher is off-palette gold, the privacy/offline differentiator has no persistent visual token while `offline_badge` stays unrendered, and the new rules are unwritten — one commit after the card fix, History and Queue already use a different card (R1). The third point needs an authored mark and wordmark, a bundled Arabic face, a privacy token, and one documented card/row language.

**Deterministic scan / visual overlays:** not applicable (native platform); screenshots are archived instead.

---

## Status of the 0.2.0 verdict

| 0.2.0 item | 0.2.1 status | Basis |
|---|---|---|
| **P0** OCR pack dead end | **Resolved** | Live pack observation, inline install, target appears with no selection change (`ocr-*.png`) |
| **P0** Landscape primary action disappears | **Resolved** | Fresh 2400×1080 capture at HEAD; 200 % font scale scrolls instead of clipping |
| **P1** Arabic-first stops at the chrome | **Partially resolved** | Seat: 4 resolved / 6 partial / 4 open. Copy blockers closed; bidi (1/4), per-app language, unmirrored icons and English engine detail remain |
| **P1** Window contract and palette half-owned | **Largely resolved** | Seat: 4 resolved / 5 partial / 6 open. Single owner, themed bars, full ramp, fresh landscape + IME evidence; residual: nested Scaffolds, notification permission, state restoration |
| **P1** Accessibility: labels without state, AA failures | **Largely resolved** | Seat: 8 resolved / 5 partial / 1 open; 13/20 → 18/20; zero P1 left |
| Secondary P1 — target below files, silent disabled CTA | **Partial** | Hint renders and the disabled button now names the reason; target still below the file list, CTA still says "Convert" |
| Secondary P1 — no per-item retry | **Open** | 0.2.2 scope (council-deferred) |
| Secondary P1 — folder mode never names destination | **Partial** | Queue row says "Saved to %1$s"; workbench done rows now carry the path; summary and History still silent |
| Secondary P1 — Arabic pages hint vs parser | **Partial** | Input accepted and visibly validated; Convert not gated (N1) |
| Secondary P1 — share-in while on another tab | **Resolved** | `openWorkbench` bus navigates to Convert (`SighaNavHost.kt:61-73`) |
| Secondary P1 — process kill mid-batch | **Open** | In-memory queue; 0.2.2 scope |
| Seat P1 — batch outcome below the fold | **Open** | No n-of-m in the pinned bar; results still appended below progress |

Counts across the five seat reports: **20 resolved · 28 partially resolved · 23 open · 0 new P0**.

---

## Remaining Priority Issues

### [P1] The required target is still below the file list, and the CTA never names it

- **What:** The hint under the bar now says "اختر صيغة الهدف" (`WorkbenchScreen.kt:813-822`), but the target card still follows every file card (`:277-290`), no default is chosen (`WorkbenchViewModel.kt:205-209,297-300`), and the button says only "Convert" / "Convert N files" (`:853-863`). In `workbench-file-ar.png` the target section is below the fold while the bar asks for it.
- **Why it matters:** The one decision the app cannot make for the user is also the only one placed after variable-length content, and the primary action never confirms what will be produced. On a 640 dp phone with two files, the user scrolls to find a choice the bar is already demanding.
- **Fix (0.2.2):** Put the target control in a bottom sheet opened from the bar, or give the bar a "Choose target" primary state that opens it; once chosen, the button label becomes "Convert to TXT". Do not default silently.

### [P1] Two stacked bottom bars still cost a third of a small phone

- **What:** `ConvertBar` is a nested Scaffold `bottomBar` above the root `NavigationBar` (`WorkbenchScreen.kt:173-181,802-817`; `SighaNavHost.kt:78-116`). Computed bottom chrome is 176 dp, 202 dp while the hint shows — 27.5–32 % of a 640 dp phone, 41–45 % once the 88 dp top chrome is added.
- **Why it matters:** This is the same finding the 0.2.0 council ranked second, and 0.2.1 made it 26 dp taller in exactly the state that needs the viewport (R3). It is the single largest remaining composition defect.
- **Fix (0.2.2):** One contextual bottom surface: the nav bar is replaced by the convert bar while files are selected, or the convert action moves into the target sheet.

### [P1] The batch outcome still is not visible in place

- **What:** The pinned bar shows spinner + "Converting…" + Cancel with no n-of-m or current file (`WorkbenchScreen.kt:830-839`); "View results" is below the progress card (`:580-584`). In the assessed tree the done row held `displayPath` but did not render it (`WorkbenchViewModel.kt:48,262`), and the completion summary names counts, not location (`:267-289`).
- **Why it matters:** "What happened to my 12 files, and where did they go?" is the question the product exists to answer, and answering it still requires leaving Convert.
- **Fix (0.2.2):** n-of-m + current file in the bar, destination in the done row and summary; the regression closure already renders the saved path on done rows.

### [P1] (regression, closed in the follow-up) The new pages validation does not gate Convert

- **What:** With `pagesError = true` the field is red and the helper is replaced, but the button stays enabled (`WorkbenchScreen.kt:843`) and `convert()` has no check (`WorkbenchViewModel.kt:202-223`), so the batch runs and every PDF item fails with the generic UNKNOWN headline.
- **Why it matters:** The pass taught the app that the input is invalid, then let the primary action spend minutes proving it. One boolean.
- **Disposition:** Closed in the 0.2.1 follow-up commit (gate + `error()`/`liveRegion` semantics + message), see "Regression closure".

### [P2] The remaining debt (all confirmed, none new to this run)

| # | Finding | Evidence | Owner |
|---|---|---|---|
| 1 | Bidi is untouched: five raw interpolations and a one-line ellipsized LTR saved path; `BidiFormatter`/RLM still absent | `QueueScreen.kt:280-288`; `values-ar/strings_ui.xml:23` | Arabic |
| 2 | Card grammar split: `SighaCard` outlined vs borderless History/Queue rows; the History row is tappable with ~1.02:1 surface | `SighaCard.kt:24-31` vs `HistoryScreen.kt:181-186` | Design |
| 3 | Sand overloaded: same amber means selected (nav pill, chips) and "not available yet" | `SighaNavHost.kt:81-91` vs `ModelPacksScreen.kt:378-384` | Design |
| 4 | Identity layer missing: stock mark ×6, OEM typeface, off-palette launcher, no privacy token | `SighaNavHost.kt:86`; `ic_launcher_foreground.xml`; `Type.kt:86-107` | Design |
| 5 | No per-app language (`localeConfig`, in-app switch); Arabic unreachable without changing the phone | `AndroidManifest.xml:11-18` | Platform |
| 6 | Three locale sources disagree (config, `Locale.getDefault()`, config-once OCR default) | `Theme.kt:92-97`, `PrivacyScreen.kt:60-61`, `SettingsStore.kt:64-68` | Arabic/Platform |
| 7 | Plural-quantity bug: `PagesLimited` passes `total` as quantity, so 101-page scans read "the first page was read of one page" | `NoteUi.kt:19-24`; `values-ar/strings_ui.xml:36` | Arabic |
| 8 | Share-in bypasses the converting guard | `WorkbenchViewModel.kt:96-104,122-159` | UX |
| 9 | Hint contradicts the empty-targets card | `WorkbenchScreen.kt:813-821` vs `:382-389` | UX |
| 10 | Dead formats still selectable; target silently resets on add/remove | `WorkbenchViewModel.kt:299-301` | UX |
| 11 | Queue named four ways, icon-only, notification cannot be opened/cancelled | `strings.xml:27,69,70,96`; `ConversionService.kt:74-91` | UX |
| 12 | Process kill mid-batch leaves one transient snackbar; queue in memory; >60 files truncate the row list | `ConversionQueue.kt:76-97` | UX |
| 13 | "Not available yet" AI card advertises an unshipped feature | `ModelPacksScreen.kt:239-242,369-394` | Product |
| 14 | 0 motion, 0 `@Preview`, no DESIGN.md/spec — the system lives in code and already drifted | grep/glob | Design |
| 15 | Unmirrored product icons (`SwapHoriz` on the CTA/tab) | `WorkbenchScreen.kt:848`; `SighaNavHost.kt:86` | Arabic |
| 16 | Cold-start window follows system `uiMode` rather than a forced app theme; no `tertiaryContainer`/`surfaceBright`/`surfaceDim`/`scrim` (now authored in the closure) | `values/themes.xml`; `Theme.kt` | Platform |

---

## New findings and regressions from the fix pass

| ID | Sev | Finding | Evidence | Why it matters |
|---|---|---|---|---|
| N1 | **P1** | Pages validation is advisory only (see above) | `WorkbenchScreen.kt:843`; `WorkbenchViewModel.kt:202-223` | Lets a knowingly-invalid batch run |
| N2 | P2 | Share-in bypasses the converting guard; the UI disables add/remove while running, the intent path does not | `WorkbenchViewModel.kt:96-104` vs `WorkbenchScreen.kt:256,268,280` | Mutates a running batch's selection; can silently drop the target |
| N3 | P2 | The new hint contradicts the empty-targets card: "Choose a target format" when no target exists | `WorkbenchScreen.kt:813-821` vs `:382-389` | Two messages, one impossible action |
| N4 | P2 | Pages error is visual-only: no `error()` semantics, no live region | `WorkbenchScreen.kt:478-498` | A TalkBack user cannot learn why Convert is disabled |
| N5 | P2 | Disabled Convert carries no reason in its own node; the hint is an unrelated sibling | `:813-822` vs `:841-864` | Focus announces "Convert, disabled" and nothing else |
| N6 | P2 | Arabic plural quantity uses `note.total`, and the `two` item loses the dual | `NoteUi.kt:19-24`; `values-ar/strings_ui.xml:36` | Wrong Arabic grammar on 101–110-page scans; EN unaffected, so it would ship unnoticed |
| N7 | P2 | Card grammar split + sand overload (P2 #2/#3 above) | see table | Workbench and History read as different builds |
| N8 | P3 | Duplicate status announcement (`stateDescription = label` over the same text); route description duplicates badges; exclusive chips still announce as checkboxes and two FlowRows lack a group; progress % announced twice; cancelled summary drops counts; U+066C normalised to a page separator | `Common.kt:213-221`; `QueueScreen.kt:216-229`; `WorkbenchScreen.kt:402-427,525-546`; `WorkbenchViewModel.kt:267-289,344-347` | Announcement hygiene and one correctness trap |
| N9 | P3 | Pack title is Latin-led ("OCR: …"), so the Arabic-defining card starts on the wrong glyph; privacy date shown two ways; "details" label wraps English engine text | `values-ar/strings.xml:215`; `values-ar/strings_legal.xml:25` vs policy line 3 | Copy polish, not reachability |

**False positives settled with fresh HEAD evidence** (both were raised as P2 platform claims):

- **Landscape content centring.** The claim was that content centred at x≈1200 of 2400 means the root horizontal inset was never applied (expected ≈1137). The fresh capture and `dumpsys` show the safe area is **128..2274** — a 128 px display-cutout inset on the left plus the 126 px 3-button strip on the right — whose centre is **1201**, exactly where the content sits. The code and the tree agree; the earlier arithmetic omitted the cutout.
- **IME/nav-bar stacking.** The claim was an inferred 48 dp gap above the keyboard from `imePadding` + `NavigationBar`'s own bottom inset. `ime-pages-ar-recheck.png` shows the navigation bar directly above the IME with no gap; the keyboard-open state is correct.

No new P0 and no new P1 beyond N1: the fix pass did not break a flow, it left three consistency debts and one un-gated validation.

---

## What's Working

1. **The OCR path is now a product, not a hint.** Card → confirmation with size and metered warning → live progress → cancel → unlocked target, all in place, no navigation, no selection change. The 0.2.0 council's first invariant is implemented and verified end to end.
2. **The window contract has one owner and it holds.** Root Scaffold, one NavHost inset strategy, theme-derived bars, night window background, and now evidence for landscape, 3-button and IME. This was the highest-risk platform item and it is code-complete.
3. **Arabic reaches the moments of truth.** Notes are typed facts rendered through resources (29 kinds, no literals), counts have all six categories, packs and policy are Arabic assets, and the Arabic type scale has its own leading and tracking. 254/254 keys, 96/96 plural forms, EN parity 173/173 + 59/59 + 22/22.
4. **Accessibility is no longer a label-only pass.** Headings, radio groups, named slider, tonal pills, merged progress, 48 dp floors, and AA contrast in both themes (floor 4.79 light, 7.08 dark; badges 12.00; pills 5.83–13.35) — 13/20 → 18/20, zero P1 left.
5. **The happy path is calm and honest.** One primary action, no account, no upload, copy that explains consequences, and a landscape empty state that now survives rotation and 200 % font scale.

---

## Persona Red Flags

**Umm Ali — 34, Baghdad, Arabic-only, phone-as-office.**
She installs the OCR pack and this time the TXT target appears where she is standing — the 0.2.0 red flag is gone. She still has to scroll past her files to choose a target, the button never tells her what it will produce, and when she converts a folder she reads "Saved to Download/Sigha" only after opening the queue; the summary says how many, not where. The saved path is still one ellipsized line around an LTR path, so the file name can be the part that disappears.

**Kareem — 29, Baghdad, blind since birth, TalkBack at 2×.**
He hears selection state, slider values, headings, per-file progress and source→target direction now — a real change. He cannot hear why Convert is disabled (N5) and cannot hear that his pages range is invalid (N4): the field turns red silently and the button stays disabled. On History he still meets the same file twice.

**Abu Ahmad — 41, Baghdad, Samsung A-series, 3-button navigation, system light + app dark.**
The cold-start flash and the unreadable clock are fixed, and landscape no longer loses the button. He still loses a third of the screen to two bottom bars, still meets a "not available yet" card in a shipped build, and still gets no motion or state restoration after a kill.

---

## Minor Observations

1. **No motion, no previews, no spec.** Still 0 `animate*`/`AnimatedVisibility`/`Crossfade`, 0 `@Preview`, no DESIGN.md. The richer the system, the more expensive the missing spec: the card rule drifted within one commit.
2. **13 category accents beside the authored role ramp.** Badges are neutral now, but the accents remain a second palette that cannot follow dynamic colour.
3. **No Material You and no adaptive layout** — both product decisions, still undocumented as such. `PRODUCT.md:109-110` now declares tablets out of scope (good); dynamic colour is still undecided.
4. **Notification permission on first cold start**, raw `requestPermissions`, no rationale and no recovery path.
5. **Numerals policy written, not enforced** — chips/date/size formatters can still flip systems independently on an Arabic-format phone.
6. **String hygiene improved but not finished** — `offline_badge`, `settings_theme` and `app_tagline` still have 0 UI uses; four names for the queue.
7. **Licence bodies and raw engine diagnostics stay English** by design; the Arabic label "إظهار التفاصيل التقنية" promises otherwise.

---

## Questions to Consider

- If the batch outcome is the product, why is the only progress surface a pinned bar with no position and no destination?
- Should a capability ever be gated behind a screen the user must leave — or is the OCR card the pattern every "needs a pack" target should follow?
- Is a stock `SwapHoriz` glyph, an OEM typeface and a gold launcher an identity, or just a palette with a logo-shaped hole?
- If Arabic is the promise, why does a screen-reader user still not hear why the primary action is disabled?
- Does 0.2.2 spend its budget on the batch outcome surface, the identity layer, or per-app language — and which one would make a closed-test user stay?

---

## Qiyas Cards (delta)

### Card 4 — A validation that does not gate is decoration

- **Al-Far':** Any validation the UI renders must also gate the action it protects, and the gate must be announced (semantics, message, or both).
- **Al-Asl:** The app's own folder validation: an unwritable destination is refused up front, not discovered as per-file failures. Platform precedent: form libraries disable submit on invalid fields and expose the error to assistive tech.
- **Al-'Illa:** A validation is a promise that the app will not spend the user's time on invalid input. Rendering the promise without enforcing it spends trust twice — once when the batch fails, once when the error was already on screen.
- **Tanqih:** The specific red colour and helper string are incidental; the transferable part is *validate → gate → announce* as one unit.
- **Conditions:** the gate must be in the ViewModel too (intent paths bypass the button); the message must be reachable by TalkBack; the rule applies to every future field.

### Card 5 — Evidence must come from the tree you ship

- **Al-Far':** A platform finding may only cite captures whose build is the reviewed tree; when in doubt, re-capture the exact state.
- **Al-Asl:** The re-run's two fresh captures: landscape centring and IME stacking both looked like defects in stale frames and both disappeared when measured against HEAD.
- **Al-'Illa:** A critique's authority is its evidence chain; a stale screenshot makes the reviewer argue with history instead of the product, and a false P2 costs the same trust as a missed one.
- **Tanqih:** The emulator/device choice is incidental; the transferable part is *capture fingerprint = reviewed commit*, and re-capture before escalating a platform claim.

---

## Maestro's Verdict

**0.2.1 is not blocked by design.** Both 0.2.0 P0s are closed with evidence, no new P0 exists, and the pass moved five heuristics by +5 (19 → 24/40) and specificity from 1.5 to 2/4. The closed-test build may proceed once the P1 regression is closed and the gate is re-run — the rest is 0.2.2 work, not a release gate.

**Order for 0.2.2 (do not reorder):**

1. **Batch outcome surface** — n-of-m + current file in the pinned bar, destination in the done row and summary. This is the product's core question and the only remaining P1 cluster.
2. **Composition** — one contextual bottom surface; target choice in a sheet opened from the bar, CTA naming the target. Removes the two stacked bars and the below-the-fold decision in one change.
3. **Arabic bidi** — `BidiFormatter.unicodeWrap` at the five interpolated sites, two-line `ContentOrLtr` saved path; the only dimension still at 1/4.
4. **Identity layer** — authored mark + monochrome launcher, bundled Arabic face, a persistent privacy/offline token, and a written card/row rule (DESIGN.md).
5. **Per-app language** — `localeConfig` + in-app switch; then revisit the three locale sources.
6. **Remove or ship the AI-upscaling card**; per-item retry and durable batches; notification `setContentIntent`/cancel.

**Continue / kill metrics for the closed test:** completion of a first conversion without leaving Convert; OCR installs that end in a TXT target; queue opens after a batch; Arabic sessions that reach a saved file; crash-free sessions. Kill signal: users who install the OCR pack but never convert a scan.

**Risks:** the regression closure is fresh code and needs the same device pass as the fix; bidi and identity are design decisions, not token edits; per-app language will expose the three-locale-source split before it fixes it.

**First action (today):** close N1 (gate + semantics) and N6 (plural quantity), re-run `scripts/verify.ps1` and the device smoke, then re-capture History/Queue for the new card grammar.

---

## Regression closure (0.2.1 follow-up, after this assessment)

The Lead closed the pass's own debts in a follow-up commit, verified by the same gate; the score above still describes the assessed tree (`90d81a9`).

- **N1 P1** — Convert is disabled on `pagesError`, `convert()` refuses with the field message, the field exposes `error()` and its helper becomes a polite live region, and the disabled button carries the reason as a `stateDescription`.
- **N2** — `addFiles` refuses incoming shares while a batch runs, with a message; the collector cannot mutate a running selection.
- **N3** — the hint only renders when a reachable target exists, so it can no longer ask for the impossible.
- **N6 / Arabic** — `PagesLimited` pluralises on `note.read`; the Arabic `two` item keeps the dual; U+066C is dropped instead of becoming a page separator.
- **Card grammar (R1)** — History rows use the outlined `SighaCard` (and lose the duplicate Open control); Queue rows use the same card.
- **Sand overload (R2)** — "not available yet" is neutral, not `secondaryContainer`.
- **Announcement hygiene (N8)** — status pills no longer double-announce, route rows use `clearAndSetSemantics`, exclusive chip rows are selectable groups, Queued progress is silent, the cancelled summary keeps its counts.
- **Outcome in place** — workbench done rows render the saved path, two lines and bidi-wrapped; Queue does the same.
- **Platform P3s** — the missing `tertiaryContainer`/`surfaceBright`/`surfaceDim`/`scrim` roles are authored; the Queue empty state is scrollable; privacy headings are headings.

**Not closed (0.2.2):** batch position in the bar, target composition, bidi everywhere, identity, per-app language, retry/durable batch, AI card.

---

## Run Notes

- **Seat mapping:** material-conformance → Platform Guardian; a11y-auditor → Accessibility Auditor; design-director → The Expert; app-auditor → Arabic-First Editor; product-strategist → Naive User/UX flows. Engineer, Devil's Advocate and Maestro were held by the Lead. Each seat wrote one file under `docs/council/2026-11-18-ui-review/seats/` and reported a ≤25-line summary; no seat edited app source, scripts or QA docs, and none read another seat's 2026-11-18 file.
- **Read-only discipline:** this review made no app-source edits; the regression closure is a separate commit, made after the assessment and reported above.
- **Evidence:** `docs/qa/evidence/ui-021/` (23 PNGs, including the two re-check captures) and `docs/qa/evidence/verify-20261118-193810.json`; the fresh captures are also in `docs/council/2026-11-18-ui-review/evidence/`.
- **Computed, not measured:** contrast from authored tokens (M3 compositing stated), chrome heights from declared dp (TopAppBar 64 + status 24; NavigationBar 80 + gesture inset; ConvertBar 72/98). Screenshots are trusted as matching `90d81a9`; the APK was not hash-verified.
- **Still unverified:** TalkBack run, native Arabic copy review, physical device, OEM skins, tablet/foldable, dark Queue/History, populated screens at font 2.0, motion/reduced-motion, high-contrast modes.
- **Assessment date:** 2026-11-18. Next critique trigger: before the next closed-test build, after the 0.2.2 batch-outcome and composition work.
