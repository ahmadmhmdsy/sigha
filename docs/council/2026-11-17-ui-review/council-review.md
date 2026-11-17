# Sigha UI/UX Design Critique — Impeccable × Qiyas Council

**Method: design review only (detector not applicable, native platform)**

Target: \`app/src/main/java/com/sigha/converter/ui\` — 20 files, ~2,500 lines, plus the theme, strings, manifest and the screens they render.
Product: Sigha / صيغة 0.2.0 (\`com.sigha.converter\`) — offline file converter, Arabic-first MENA audience, no account, no uploads.
Mode: **Operate** (task-completion utility; all ten Nielsen heuristics apply).
Date: 2026-11-17. Review is read-only: no app-source edits were made.

Evidence base, and its limits:

- Static review of every UI file, \`ui/theme/*\`, \`res/values*/strings*.xml\`, \`AndroidManifest.xml\`, \`themes.xml\`.
- Emulator run: Android 16 ATD image, 1080×2400 @ 420 dpi. Fresh debug install of the reviewed commit. UI hierarchy dumps (\`uiautomator\`) plus real screenshots (PNG) — four are archived in \`docs/council/2026-11-17-ui-review/evidence/\`.
- Contrast ratios are **computed** from the actual hex values after Material 3 tonal-elevation compositing (card surface = #EFF6F4 light, #1A2521 dark), not measured with a probe.
- **Not run:** TalkBack session, an Arabic-native copy review, an OEM-skin device (One UI/MIUI/EMUI), a tablet/foldable. Where a finding depends on those, it is marked *inferred*.

---

## Council seated

Qiyas-council mode C (merged). Core roles plus three dynamic roles, each owning a dimension that could kill the product.

| Role | Mandate | Signature question | Qiyas tool | Red line |
|---|---|---|---|---|
| **The Expert** — composite mobile-utility designer | Extract the true cause from precedents; forecast second-order effects | Why did the precedent *really* work, and what breaks in 6–24 months? | Takhrij al-Manat | Accepting a surface fix for a cause-level defect |
| **The Engineer** — Compose/Android | Build the solution; every proposal carries a precedent and a cause | What is the precedent, and what is the cause? | Al-Far' | Proposing anything untethered |
| **Devil's Advocate** | Attack causes, not wording | Where does the cause break? | Al-Qawadih | Accepting the first draft |
| **Naive User** — "Abu Ali", sarcastic phone-shop customer | Test whether the cause exists in the actual user | What's in it for me, and why would I bother? | Tahqiq al-Manat | Complexity he cannot see the point of |
| **The Maestro** | Silent judge; rules what survived | — | — | — |
| **Arabic-First Editor** (dynamic) | Arabic copy, bidi, typography | Does this sentence exist in Arabic? | Fasad al-i'tibar | English inside a user-facing Arabic sentence |
| **Accessibility Auditor** (dynamic) | TalkBack, low vision, motor | Can a blind user confirm this state? | Tahqiq al-Manat | State encoded only visually |
| **Platform Guardian** (dynamic) | Material 3, window contract, Play rules | Whose job is this inset or flag? | Al-Farq al-mu'aththir | Per-screen reimplementation of window behaviour |

---

## Step 0 — Frame and invariants

**Goal.** A production-ready 0.2.0 that an Arabic-first, non-technical user can trust for real paperwork: pick a file, convert it, find it, and understand what happened.

**Hard constraints.** Android 26–36; Material 3 is the rulebook; offline by design; no account; primary audience Arabic-speaking MENA (Iraq first), often on metered data, often on 3-button navigation, often at font scale 1.3–2.0.

**Global invariants (Expert, stated before debate):**

1. **Trust is the product.** The pitch is "your files never leave the phone". Every moment the app *speaks* — a note, a failure, a privacy screen, a permission prompt — either pays that trust back or spends it.
2. **The app must answer "what happened?" after any interruption.** A utility that loses work converts its audience back to WhatsApp file forwarding.
3. **Arabic is an architecture, not a translation layer.** Text chosen where the locale is unknown can only be in the developer's language; Arabic grammar is a function of count, not of \`n == 1\`.
4. **The window is one surface.** Insets and system-bar appearance are properties of the window, consumed once; per-screen ownership gives N chances to get it wrong.
5. **A capability is never hidden, only gated.** Hiding a target because its pack is missing turns "needs a download" into "the app is broken".

---

## Design Health Score

| # | Heuristic | Score | Key Issue |
|---|---|---|---|
| 1 | Visibility of System Status | 2 | Per-row progress and status pills exist, but batch position never appears in the pinned bar, results land below the fold, folder mode never names the destination, and installing the OCR pack changes nothing on screen. |
| 2 | Match System / Real World | 2 | Arabic prose is natural, but "OCR" is untranslated jargon at the decision point, engine notes are English, numerals mix ٠١٢٣ with 0123, and "Saved" appears without a location. |
| 3 | User Control and Freedom | 2 | Cancel works in two places and history-clear is confirmed, but clearing a 30-file selection has no undo, there is no per-row cancel/retry, and history deletion is all-or-nothing. |
| 4 | Consistency and Standards | 2 | Stock M3 components and one icon family, but M3 baseline purple paints the navigation bar and every label, three pill variants and four "clear" scopes coexist, and the queue has four names. |
| 5 | Error Prevention | 2 | Folder writability is validated up front and destructive clears are confirmed, but the pages field is unvalidated, nine dead formats are selectable, the target silently resets when a file is added, and Convert is disabled with no reason. |
| 6 | Recognition Rather Than Recall | 2 | Format chips and the empty-state category line aid recognition, but the OCR remedy is a prose navigation path, the queue is an unlabelled icon, retry does not exist, and the required target sits below the file list. |
| 7 | Flexibility and Efficiency | 2 | Batch conversion, "add more" and share-in are good; no per-item retry, no "convert again" from history, no multi-select, and eight bitrate chips in one row break the working-memory rule. |
| 8 | Aesthetic and Minimalist Design | 2 | The palette is calm and the layout uncluttered, but the nav bar is Material baseline lavender, ink and sand never reach text/surface, 26 hard-coded accents compete, and the trust line is duplicated in two places. |
| 9 | Error Recovery | 1 | Failures collapse to one generic headline with English details behind a disclosure; failed rows are dead ends; a process kill mid-batch leaves one snackbar; a revoked folder is reported only as a per-row footnote after the run. |
| 10 | Help and Documentation | 2 | About/Licenses/Privacy exist and the OCR hint explains a missing capability, but the privacy-policy body is English-only, no format/limit help exists in context, and a "Not available yet" AI card advertises an unbuilt feature. |
| **Total** | | **19 / 40** | **Poor (12–19) — "major UX overhaul required"** |

Band note: the English/portrait happy path alone reads closer to 26–28; the deficit is concentrated in exactly the three places the product differentiates itself — Arabic-first content, recovery, and adaptivity. That concentration is the finding.

---

## Design Specificity Verdict

**1.5 / 4 — a branded skin on a category-interchangeable skeleton.**

The composition is the generic converter template: centred empty state → file cards → category-labelled format chips → options card → pinned convert bar. An unrelated file utility could use the whole structure unchanged. What is branded is one emerald, one sand, one ink and a swap-arrows glyph — and the ink never reaches text (all body copy is M3 default \`onSurface\` #1D1B20), the sand survives only in icon circles and the selected nav indicator, and the swap-arrows glyph is used as the app identity, the Convert tab icon, the Convert action and the empty-state illustration at once.

The strongest product-specific idea in the codebase — a typed, localised failure taxonomy (\`ConversionFailureKind\` → Arabic headline) — is not carried into the notes, the packs or the policy. "Arabic-first" is therefore visible in the *layout direction* but not in the *typographic voice* (no Arabic family, Latin tracking on a connected script) and not at the *moments of truth* (English notes, English pack, English policy).

**Deterministic scan:** not applicable (native platform).

**Visual overlays:** not applicable (native platform). Screenshots are archived instead.

---

## Overall Impression

This is a disciplined Material 3 app with an unusually honest product idea, and the calm happy path is real: one primary action, no upload, no dark patterns, confirmations that explain consequences. But the app is systematically assembled rather than authored, and the assembly stops exactly where Sigha has something to say. The single biggest opportunity is not a redesign — it is to **make the moments where the app speaks (notes, packs, policy, failures, chrome) as Arabic and as trustworthy as the moments where it is silent.**

---

## What's Working

1. **A calm, honest happy path.** One page, one primary action, a pinned Convert bar, and copy that explains consequences instead of asking for faith — "This only clears the list. Converted files stay in your storage." (\`HistoryScreen.kt:142-157\`). No onboarding wall, no account, no upsell.
2. **Arabic/RTL structure is genuinely correct at the layout level.** 218/218 string keys with identical format specifiers, \`supportsRtl="true"\`, zero \`left/right\` padding, auto-mirrored navigation. The Arabic screenshot shows mirrored chrome, reversed tab order and right-aligned copy — better than most Arabic Android apps in the region.
3. **A typed failure taxonomy and redundant status.** \`ConversionFailureKind\` → localised headline (\`FailureUi.kt:12-28\`), status carried by text + colour + icon shape, stock M3 controls giving 48 dp targets, ripple and state layers for free. This is the pattern the notes and packs should copy.

---

## Priority Issues

### [P0] The OCR pack is a dead end: obey the app's instruction and nothing changes

- **What:** Targets and the OCR hint are computed only when the selection or target changes (\`WorkbenchViewModel.kt:239-249\`, called from \`:119, :132, :140, :149\`). The pack's installed state is read inside that computation (\`:246\`, \`:277-286\`) but never observed, so installing the 28 MB pack and returning to Convert leaves the same screen: no TXT target, same hint.
- **Why it matters:** The OCR path is the product's Arabic-defining feature. The user spends 28 MB on metered data, obeys the on-screen instruction, and concludes the app cannot do Arabic. The only workaround is to disturb the selection or tap an unrelated target — undiscoverable.
- **Fix:** Collect \`Repo.packDownloads.states\` in the ViewModel and recompute derived state on change; turn the hint into an inline "Install OCR pack (28 MB)" action that opens the existing confirmation in place; when the install finishes, show the TXT target and remove the hint without navigation. Reconcile \`PRODUCT.md:135\` ("targets visible without the pack") with the code, which hides them.
- **Suggested command:** \`$impeccable harden\`

### [P0] In landscape the primary action disappears

- **What:** The empty state is a non-scrollable \`Column(fillMaxSize, Arrangement.Center)\` wrapping \`EmptyState\` (\`WorkbenchScreen.kt:174-212\`; \`Common.kt:106-166\`). On a 2400×1080 landscape screen the content area is 639 px tall while the empty state needs 1,029 px. Device evidence: only the icon and title render; the body copy, the "Choose files" button and the footnote are absent, no scrollable node exists, and the remaining node bounds overlap (title y 693–765, button y 702–828) — \`evidence/english-landscape-empty.png\`.
- **Why it matters:** A user who rotates the phone (or uses a tablet/keyboard) cannot add files at all. First-run in landscape looks broken, and there is no in-app way out except a share intent from another app.
- **Fix:** Make the empty state scrollable and compact for short viewports (icon 112→72 dp under ~480 dp height, reduced vertical rhythm), or use a \`LazyColumn\` with \`fillParentMaxHeight\`. Verify 2400×1080 and 1080×2400 at font scale 1.0 / 1.3 / 2.0.
- **Suggested command:** \`$impeccable adapt\`

### [P1] Arabic-first stops at the chrome

- **What:** Every surface where the app *reports what happened* is produced outside the resource system:
  - Notes: Arabic label "ملاحظة: %1$s" wraps English engine text (\`strings_ui.xml:14\`; producers \`OcrPageRange.kt:32,43,49,55\`, \`VideoConverter.kt:70\`, \`PdfToTextConverter.kt:100\`). The success path prints "ملاحظة: Only the first 50 of 120 pages were read."
  - OCR pack: title and description are English literals (\`PackCatalog.kt:32-34\`), rendered on the packs screen and spliced into an Arabic dialog sentence (\`ModelPacksScreen.kt:122,277-285\`).
  - Privacy policy: one English asset (\`PrivacyScreen.kt:55\` + \`assets/legal/privacy-policy.md\`) behind Arabic chrome — the trust document of a privacy-first app.
  - Counts: **0 \`<plurals>\`** in the project; "تحويل %1$d ملفات", "%1$d نُجحت · %2$d فشلت" (\`values-ar/strings.xml:43,60,68\`) are ungrammatical at 2 and at 11; \`نُجحت\` is not a valid passive.
  - OCR language defaults to \`eng\` regardless of locale (\`SettingsStore.kt:38\`), so an Arabic user's first scan returns Latin noise rather than an error.
  - Typography applies Latin tracking to Arabic: \`headlineSmall\` −0.2 sp, \`labelMedium\` +0.4 sp, \`bodySmall\` +0.2 sp (\`Type.kt:17-75\`) — at 12 sp this visibly disconnects a connected script; no Arabic family is bundled.
- **Why it matters:** The user is fluent in the app right up to the moment the app has something to tell them, then the language changes under them. That is the exact "machine-translated menu" experience \`PRODUCT.md:21-22\` says Sigha exists to beat.
- **Fix:** Replace the free-text note with \`NoteKind\` + typed args resolved in the composable (copy the failure pattern); convert every counted string to \`<plurals>\` with the six Arabic categories and use \`pluralStringResource\`; key the pack catalogue by id and resolve title/description from resources; ship \`privacy-policy.ar.md\` selected by locale; derive the OCR default from \`configuration.locales[0].language\`; branch the type scale on language (0 sp tracking, ×1.15 line height) and bundle one Arabic family.
- **Suggested command:** \`$impeccable clarify\` (then \`$impeccable typeset\` for the Arabic scale)

### [P1] The window contract and the palette are half-owned

- **What:** \`enableEdgeToEdge()\` runs with defaults before the persisted theme is read (\`MainActivity.kt:37\` vs \`:41-46\`) and is never re-applied; \`uiMode\` is in \`configChanges\` (\`AndroidManifest.xml:22\`), so system-theme flips do not recreate the activity. Forced Dark on a light phone therefore paints **dark system icons on #0F1513** — measured #060808 on #0F1513, ≈1.07:1, with a light #E9E9EA system navigation strip under a dark app (\`evidence/arabic-forced-dark-system-light.png\`). Cold start flashes a hard-coded light window background (#F7FAF8, \`themes.xml:6\`; no \`values-night\`). Nine nested \`Scaffold\`s each set \`contentWindowInsets = WindowInsets(0,0,0,0)\`, and there are 0 hits for \`imePadding\`, \`WindowInsets.ime\` or \`safeDrawing\`: the only text field can sit under the keyboard, and the Queue list slides under the 3-button nav bar because its inset lives only on a conditional bottom bar (\`QueueScreen.kt:95-118\`).
- **Why it matters:** The theme selector is a shipped feature that breaks the app's own chrome; the nav bar is Material baseline lavender #F3EDF7 (light) / #211F26 (dark) rather than SighaMist/SighaNight, because \`surfaceContainer\` and the \`on*\` roles are never set (\`Theme.kt:15-55\`); body text is M3 baseline #1D1B20, not SighaInk; cards measure ~1.02:1 against the background with no border or shadow, so grouping is invisible in daylight.
- **Fix:** One chrome owner at the NavHost consuming \`safeDrawing\` + IME and passing resolved padding down; \`SystemBarStyle\` derived from the resolved \`darkTheme\` and re-applied in a \`SideEffect\`; author every M3 role the components read (onSurface/onBackground/onSurfaceVariant, surfaceContainer family) in Sigha ink/mist/night; set \`Shapes\`; add \`values-night\` window background; delete the nine zero-inset overrides.
- **Suggested command:** \`$impeccable colorize\` (then \`$impeccable harden\` for insets)

### [P1] Accessibility: labels without state, and light-theme AA failures

- **What:** Zero \`Modifier.semantics\`, zero \`stateDescription\`, zero \`heading()\`, one \`Role.\` in ~2,500 lines of Compose. Radio rows are \`Row.clickable\` + nested \`RadioButton\` (\`SettingsScreen.kt:272-296, 340-364\`) — two focus stops, and the row never says "selected"; the quality Slider is unnamed (\`WorkbenchScreen.kt:446-451\`); progress rows never say which file a percentage belongs to and a QUEUED row announces "0 percent" (\`:591-597\`); source→target reads as two unrelated codes (\`QueueScreen.kt:214-226\`); failure reasons are 12 sp and \`maxLines = 2\` (\`HistoryScreen.kt:245-251\`). In light theme, 7 of 13 format badges are 3.76–4.37:1 and the "Done"/"Installed" pills are 3.99:1 after tonal compositing; "Choose folder" measures 43.8 dp; the two-line top bar loses the privacy tagline at font scale 2.0 (\`evidence/arabic-font200-portrait.png\`).
- **Why it matters:** TalkBack users can operate the app but cannot confirm any decision the visual design encodes; low-vision users lose the state labels first; the product's central promise is the first thing clipped at the font size they run.
- **Fix:** One semantics pass over five shared components (\`SectionHeader\` heading, \`StatusPill\` stateDescription, radio \`selectableGroup\`/\`selectable(Role.RadioButton)\`, slider name + state, progress row merged state); neutral text on tinted badges and \`primaryContainer\` for pills; pad the folder button to 48 dp; one-line top bar (or \`MediumTopAppBar\`).
- **Suggested command:** \`$impeccable audit\`

### Secondary P1s (from the seat reports; each has a concrete fix)

| Finding | Evidence | Fix |
|---|---|---|
| The required target choice is buried below the file list; Convert is disabled with no default and no reason (\`workbench_target_hint\` is defined, 0 usages) | \`WorkbenchScreen.kt:255-268, 661-679\`; \`strings.xml:48\` | Render the hint under the bar, flash/scroll to the target card on first add; decide whether a default target is allowed |
| No per-item retry; retrying 2 failed of 10 re-converts all 10 and writes 8 duplicates | \`QueueScreen.kt:236-264\`; \`WorkbenchViewModel.kt:166-187\` | Per-row Retry / "Retry failed (2)" that re-enqueues only FAILED rows |
| Folder mode never names the destination ("Saved"), and a revoked grant is a per-row footnote after the batch | \`OutputPublisher.kt:256\` (\`displayPath = null\`); \`QueueScreen.kt:272-278\` | Name the folder per row and in the summary; re-validate the grant before a run |
| The Arabic pages hint teaches an input the parser rejects | \`values-ar/strings.xml:57\` (U+060C) vs \`PdfPageSelection.kt:24\` (\`split(',')\` only); no inline validation | Accept U+060C and Arabic-Indic digits, normalize, validate inline with a disabled Convert |
| Sharing a file while the app sits on another tab does nothing visible | \`MainActivity.kt:74-79\`; \`SighaNavHost.kt:90-130\` (no bus observer) | Navigate to Convert on incoming files + "1 file added" |
| A process kill mid-batch leaves one transient snackbar and no state | \`WorkbenchViewModel.kt:96-98\`; in-memory queue | Persist the batch and offer "Retry N unfinished" (second pass) |

---

## Persona Red Flags

**Umm Ali — 34, Baghdad, Arabic-only, phone-as-office (maps to "Casey", distracted mobile).**
She opens a scanned PDF, sees no TXT target, reads "تظهر أهداف OCR بعد تثبيت حزمة OCR", installs 28 MB on metered data, comes back — nothing changed. She types the app's own example "1-3، 5" with her Arabic keyboard and gets "فشل التحويل." She converts to a folder, sees "تم حفظ الملف" with no location, and when the grant dies the batch lands in Downloads while the summary still says all files converted. Her conclusion is not "a setting is wrong"; it is "this app does not do Arabic."

**Kareem — 29, Baghdad, blind since birth, TalkBack at 2× (maps to "Sam", accessibility-dependent).**
He can pick files and tap Convert; he cannot hear his own decisions: the quality slider says "0.85", not "image quality, 85 percent"; the OCR dialog costs two swipes per language and never says which is current; a queued file announces "0 percent" while the pill says "Queued"; his result row reads "JPG, PDF" with no direction; the reason his batch failed is 12 sp, two lines, ellipsized; nothing is a heading, so he cannot skim the six-section workbench.

**Abu Ahmad — 41, Baghdad, Samsung A-series, 3-button navigation, system light + app forced Dark (maps to "Riley", deliberate stress tester).**
Cold start flashes a near-white window, then asks for notification permission before he has converted anything. With system light and app Dark, the clock and battery become dark icons on #0F1513 — unreadable while he converts — and the bottom system strip is white under a dark app. He rotates to landscape to see the file list: the "Choose files" button is gone. He shares an invoice from WhatsApp while the app is on Settings: the file is added silently behind another tab. He kills the app mid-batch: the only trace is a snackbar he may have missed.

---

## Minor Observations

1. **No motion at all** — 0 \`animate*\` calls; licence rows and queue details snap open, tab switches use the library-default fade. P3, but a decision to document.
2. **No Material You** — dynamic colour is absent and undiscussed; 26 category accents are fixed hex, so they cannot follow dynamic surfaces. P2 (decision, not defect).
3. **No adaptive layout** — \`NavigationBar\` at every width, no WindowSizeClass, no rail, no content max-width; tablets get 1000 dp-wide cards. P2.
4. **No per-app language** — no \`localeConfig\`, no in-app switch; Arabic appears only if the whole phone is Arabic. P2 for an Arabic-first launch.
5. **Notification permission on first cold start** without rationale, using raw \`requestPermissions\`, no recovery path on permanent denial. P2.
6. **Eight bitrate chips in one row** ("Auto", 64…320) — working-memory rule says ≤4 visible options; the "Auto" default is not explained. P2.
7. **String hygiene** — 218 keys with full parity, but unused keys (\`workbench_target_hint\`, \`offline_badge\`, \`settings_theme\`, \`workbench_storage_permission_message\`, \`licenses_*\`, many \`action_*\`), the tagline duplicated in top bar and empty state, and four "clear" labels for three scopes. P2.
8. **Shape and pill drift** — three pill implementations, \`Shapes\` never configured, 20 dp cards against M3's 4/8/12/16/28 token set. P2.
9. **History** — rows have three focus stops (row + open + share) with duplicate open action; empty history has no CTA; deletion is all-or-nothing. P2.
10. **Queue** — named four ways (View queue / Progress / View results / Queue), icon-only, no badge, notification cannot be tapped or cancelled. P2.
11. **Queue truncates display at 60 rows** (\`MAX_ENTRIES\`) while converting all — a 70-file batch shows 60. P2.
12. **Splash and icon** — light window background in dark mode (white flash); launcher icon has no monochrome layer, so Android 13+ themed icons won't tint it. P2/P3.
13. **Numerals and bidi** — ٨٠% beside 64/96 chips, hand-typed "50" against a code constant; "التنزيلات/Sigha" reorders to "Sigha/التنزيلات"; paths/filenames are spliced without \`BidiFormatter\`, so the ellipsis eats the filename; \`SwapHoriz\` is not auto-mirrored in RTL. P2.
14. **No DESIGN.md, no tokens doc, no \`@Preview\`** — the design system lives only in \`Color.kt\`/the composables. P3.

---

## Questions to Consider

- If Arabic is the promise, why is the only document a user is asked to *trust* — the privacy policy — English-only?
- Should a capability ever be invisible because its pack is missing, or should every supported conversion be visible with an explicit "needs pack" state?
- What would "your files never leave the phone" look like as a *design element* rather than a tagline — and why is it only text?
- Is Sigha's identity really a swap-arrows glyph borrowed from the Convert action?
- Does 0.2.0 need landscape, tablets and Material You at all, or should those be explicitly out of scope until the Arabic-first path is whole?

---

## Qiyas Cards

### Card 1 — A capability is never hidden, only gated, and the gate is actionable in place

- **Al-Far' (proposal):** Always show OCR targets, marked "needs pack"; tapping one opens the existing 28 MB confirmation as a sheet from the Workbench; when the install finishes the list and hint update in place, with no navigation and no selection change.
- **Al-Asl (precedent):** Google Translate's offline languages and Gboard's language list — the language stays visible with a download affordance and size, and the row updates in place when the download completes. The app's own failure taxonomy is the same shape: the engine reports a typed fact, the UI decides how to say it.
- **Al-'Illa (causal invariant):** Availability is an attribute of an artifact the user can see, not a filter applied to the list. Hiding a target converts "needs a download" into "the app is broken", and any view computed once from that hidden state goes stale the moment the state it depends on changes — which is exactly what ships today.
- **Tanqih al-Manat (stripped noise):** The precedent's download UI (progress bars, Wi-Fi heuristics) is incidental; the transferable part is *visible artifact + explicit state + in-place acquisition*.
- **Conditions:** the Workbench must observe pack state as a Flow; a partially installed pack must be handled; the target must still fail gracefully if the pack disappears between selection and run; \`PRODUCT.md:135\` must become the single truth.

### Card 2 — One owner for the window and the theme

- **Al-Far' (proposal):** A single chrome owner at the NavHost consumes \`safeDrawing\` + IME once and passes resolved padding to screens; \`SystemBarStyle\` is derived from the resolved \`darkTheme\` and re-applied on change; the M3 scheme is authored for every role the components read, and \`Shapes\` is configured.
- **Al-Asl (precedent):** Google's Now in Android — one \`NiaApp\` Scaffold owns navigation chrome and window insets, a chrome-free NavHost receives \`PaddingValues\`, feature screens are layout-only; the Material 3 edge-to-edge guidance makes bar appearance a function of the theme composable, not of \`onCreate\`.
- **Al-'Illa (causal invariant):** A window inset and the system-bar appearance are properties of the window, not of a screen; they must be consumed exactly once at the highest composable that owns the system bar, and they must be a pure function of the same state that drives content colour. Duplicating the decision across N owners gives N chances to zero it — the Queue screen is the proof, not a prediction.
- **Tanqih al-Manat:** Now in Android's module structure and Hilt setup are incidental; the transferable part is *single ownership + derivation from theme state*.
- **Conditions:** verify 3-button landscape (side insets), cutout, gesture nav, IME open on the pages field; status/nav icon contrast ≥3:1 in both theme-mismatch directions; the 26 fixed accents re-tested against authored surfaces.

### Card 3 — Localise where the locale is known; make plurality a type, not a string

- **Al-Far' (proposal):** Replace free-text notes with \`NoteKind\` + typed args resolved in the UI; every counted sentence becomes \`<plurals>\` with the six Arabic categories; the OCR default derives from the locale; the pack catalogue is keyed by id.
- **Al-Asl (precedent):** The app's own failure path — \`ConversionFailureKind\` → \`headlineRes()\` — where the engine reports an enum and the UI translates. Platform precedent: Android's \`plurals\` resource contract encodes CLDR's six Arabic categories, and \`MediaPlayer\` reports \`MEDIA_ERROR_*\` codes, never sentences, because a library has no locale.
- **Al-'Illa (causal invariant):** User-visible text must be chosen where the locale is known; text produced where no \`Context\`/\`Configuration\` exists can only ever be in the developer's language. Arabic agreement is a function of the count modulo 100, not of \`n == 1\`: one fixed form has one degree of freedom where the grammar has six.
- **Tanqih al-Manat:** The precedent's enum plumbing is incidental; the transferable part is *typed fact at the producer, sentence at the consumer*.
- **Conditions:** enumerate every note producer before touching the type; define zero and two explicitly; test 0, 1, 2, 3, 11, 100, 101; keep the numeral policy consistent inside the plural; correct \`docs/qa/verification.md:165-166\`, which currently claims notes are collapsed English details.

---

## Council Transcript

**Round 1**

**Engineer:** Proposal: one polish pass over the visual system — author the palette roles, tokenise shapes, fix the top bar, add motion. Precedent: the 0.1→0.2 UI refresh in this repo (git \`c52e185\`/\`a92f158\`) — calmer palette, fewer components, same flows; cause: consistency lowers perceived risk in a trust product.

**Devil's Advocate (Naqd):** The precedent does not transfer. 0.1→0.2 made the *chrome* consistent while the OCR path stayed broken; users did not get more conversions, they got a nicer dead end. (Fasad al-i'tibar) "Polish" assumes the visual layer is the constraint — the repo's own documents contradict that: \`PRODUCT.md:192\` promises retry, the Queue has no retry control; \`docs/qa/verification.md:165-166\` claims notes are collapsed English details, they are inline; \`PRODUCT.md:135\` says OCR targets stay visible, the code hides them. The defect is doc-vs-code drift at the trust moments, not corner radii.

**Naive User (Tahqiq al-Manat):** I installed the pack like it told me. Nothing happened. Why would I care about corner radius?

**Arabic-First Editor (fatal flaw):** The Arabic stops at the chrome. Every sentence that describes what happened to my file is English — the note, the pack card, the policy body, the technical details. The app is fluent in Arabic until it has something to say.

**Accessibility Auditor (fatal flaw):** Zero semantics state in 2,500 lines. TalkBack can navigate but cannot confirm a single decision; the quality slider announces a fraction, the radio rows never say "selected", and light-theme badges fail AA on the most repeated small text.

**Platform Guardian (fatal flaw):** The window is not owned. Forced Dark on a light phone gives dark system icons on a dark bar — device-verified at ≈1.07:1 — and the nav bar is Material baseline lavender, not Sigha.

**Expert (Takhrij al-Manat):** The cause is not "the UI is ugly"; it is "the app's trust moments are not authored". That is why the defects cluster exactly where the app speaks: notes, packs, policy, failures, chrome. Fix the moments, not the surface.

**Round 2**

**Engineer (revised):** I revise: three workstreams — (1) capability/state (OCR gated + in-place install, per-row retry, folder destination), (2) locale completeness (typed notes, plurals, pack keys, Arabic policy, locale-derived OCR), (3) window/theme ownership (one chrome owner, theme-derived bars, full role set). Plus an accessibility pass on five shared components. Qiyas cards 1–3 above.

**Devil's Advocate (Al-Athar al-Thani):** Second-order effects: showing OCR targets always invites a 28 MB download on metered data — the confirmation must state size and offer a Wi-Fi note, and it already does, so keep it. Durable batch persistence is a storage design, not a polish item — it will swallow the release.

**Arabic-First Editor:** I accept the notes/policy/plurals workstream only if plurals are type-level, not string patches, and if the numeral policy (٠١٢٣ vs 0123) is decided once and written down.

**Accessibility Auditor:** Do not bolt semantics onto two screens. Put it in the five shared components — one pass covers every screen, including the 25-section licences page.

**Platform Guardian:** Material You is a distraction now; the palette is half-authored and that is the defect. Decide dynamic colour later, in writing.

**Round 3**

**Engineer (revised again):** Dropped: full visual rebrand, Material You/dynamic colour, motion pass. Downgraded: durable batch persistence + per-row retry to the next pass (needs a storage design). Kept: both P0s and the three P1 workstreams, plus the small P1 fixes (target visibility, folder destination, pages validation, share-in navigation). Suspended on product decisions: default target auto-pick, per-app language switch, tablet rail.

**Naive User:** Will the button be there when I turn the phone sideways? Will I see TXT after I install the pack?

**Engineer:** Yes to both — those are the two P0s.

**Devil's Advocate:** Something fell: the rebrand, dynamic colour and motion are gone, and recovery is deferred. I accept, on condition the Step Zero test can kill the priority order.

**Round 4** — no new objections. The Maestro speaks.

---

## Maestro's Verdict

### Verdict table

| # | Branch | Status | Asl | 'Illa | Constraint imposed by objections |
|---|---|---|---|---|---|
| 1 | OCR capability visible-but-gated, install in place | **Survived — P0** | Google Translate offline languages; app's own typed failure path | Availability is an attribute of a visible artifact, not a filter | Metered-data confirmation; partial install; reconcile PRODUCT.md:135 |
| 2 | Scrollable/compact empty state; landscape CTA | **Survived — P0** | Material adaptive layout; Google Files scrollable empty states | Fixed non-scrollable column cannot hold content taller than the viewport | Test 3 font scales × 2 orientations |
| 3 | Arabic completeness: typed notes, plurals, pack keys, policy, OCR default, Arabic type | **Survived — P1** | App's own failure taxonomy; Android plurals contract | Text must be chosen where locale is known; Arabic agreement is count-mod-100 | Numeral policy decided once; native Arabic reviewer |
| 4 | One window owner + theme-derived chrome + full M3 role set + Shapes | **Survived — P1** | Now in Android chrome ownership; M3 edge-to-edge guidance | Window insets/appearance are window properties, pure functions of theme state | Contrast matrix both mismatch directions; 3-button/cutout/IME checks |
| 5 | Accessibility pass on 5 shared components + AA contrast + 48 dp | **Survived — P1** | M3 accessibility guidance; the app's own redundant status pattern | State must be exposed, not only painted | TalkBack run on a real device |
| 6 | Target visibility + Convert reason | **Survived — P1** | — | A required decision cannot sit below the fold with a disabled action | Decide default-target policy separately |
| 7 | Folder destination named; grant re-validated | **Survived — P1** | — | "Find the result" requires naming where it went | — |
| 8 | Pages input normalization + inline validation | **Survived — P1** | — | The app's own example must parse | Arabic-Indic digits + U+060C |
| 9 | Per-row retry + durable batch | **Downgraded — next pass (P1)** | Chrome Downloads queue | A job's identity and terminal state must outlive the surface | Storage design; reconcile with History |
| 10 | Share-in navigates to Convert | **Survived — P2, cheap** | — | A silent background append looks like failure | — |
| 11 | Full visual rebrand | **Dropped** | — | — | Scope; the brand is not the blocker |
| 12 | Material You / dynamic colour | **Dropped (revisit 1.0)** | — | — | Brand-first palette; document the decision |
| 13 | Motion pass | **Dropped (P3)** | — | — | No user-blocking impact |
| 14 | Default target auto-pick | **Suspended on product decision** | — | — | Wrong default could convert to the wrong format |
| 15 | Per-app language switch / localeConfig | **Suspended on product decision** | Android 13 per-app locales | — | Is Arabic-only-if-phone-Arabic acceptable? |
| 16 | Tablet rail / adaptive navigation | **Suspended on scope** | M3 adaptive guidance | — | Is large-screen in 0.2 scope? |

### Step Zero — the cheapest test that could kill this priority order

**Experiment (1 day, 5 Arabic-first participants, one real phone, current build):**

1. **Task A (OCR):** "Convert this scanned Arabic page to text." Observe: install pack → return → is a TXT target visible? → convert → read the result. Success = Arabic text + an Arabic note.
2. **Task B (landscape):** rotate the phone to landscape on the empty screen and ask them to add a file. Success = they find and press "Choose files".
3. **Task C (chrome):** force Dark in Settings on a light phone and ask them to read the clock/battery. Success = readable.
4. **Automated assertions in the same run:** no English text reaches \`ui_note_label\`; all counted strings are plurals; light-theme text pairs ≥4.5:1; landscape CTA present at 1.0/1.3/2.0.

**Decision table:**

| Result | Action |
|---|---|
| ≥3/5 fail Task A, or any participant says "it doesn't do Arabic" | Ship branches 1 + 3 before any visual work (today's order stands) |
| ≥1/5 cannot add a file in landscape (Task B) | Branch 2 is confirmed P0; fix in the same release |
| ≥3/5 cannot read the status bar (Task C) | Raise branch 4 to P0; otherwise keep P1 and sequence after Arabic |
| <2/5 notice Task C and Task A passes | Re-order: finish Arabic, then chrome; no rebrand |

### The solution (scope for 0.2.1)

- **Release blocker (P0):** branches 1–2.
- **Same release (P1):** branches 3–8, plus the cheap share-in fix.
- **Next pass (0.2.2):** branch 9 (durable batch + per-row retry), queue truncation, history per-row delete.
- **Explicitly out:** rebrand, dynamic colour, motion, tablet rail, default target, per-app language switch (until decided in writing).
- **Rules:** no user-facing English inside Arabic screens; every count through \`<plurals>\`; one chrome owner; every theme role authored; every text pair ≥4.5:1; every primary action reachable in both orientations at 1.0–2.0 font scale.

### Continue / kill metrics

- Task A success ≥4/5; time-to-first-conversion ≤90 s.
- 0 English strings in the Arabic note path (automated test); 100% counted strings pluralised; OCR default = device language on first run.
- Landscape CTA present and tappable at 1.0/1.3/2.0 on 2400×1080 and 1080×2400.
- TalkBack completes 3 tasks with state confirmation; light-theme text pairs 100% ≥4.5:1 (computed).
- Decision date: before the next closed-test build; re-run this critique afterwards.

### Risks and mitigations

| Risk | Mitigation |
|---|---|
| Scope: 30 findings, one release | Fix P0 + P1 only; the Maestro's table is the scope |
| 28 MB pack on metered data | Keep the existing size + metered confirmation; add a Wi-Fi note; never auto-download |
| No Arabic-native reviewer on the team | Name one; make the Arabic policy and plurals a release gate |
| TalkBack behaviour unverified | One device run with TalkBack; the semantics pass is testable by inspection |
| Per-app language and default target are product decisions | Suspend, decide in writing, do not guess |
| Only ATD emulator tested (no OEM skins) | Test one One UI device before release; Arabic font fallback is OEM-dependent |

### First action (today)

Two small commits that remove both P0s: (1) collect \`Repo.packDownloads.states\` in \`WorkbenchViewModel\` and recompute derived state on change; (2) make the Workbench empty state scrollable/compact and verify it on a 2400×1080 landscape emulator at font scale 1.0 and 2.0. Then run Step Zero.

---

## Run Notes

- **Target slug:** \`app-src-main-java-com-sigha-converter-ui\` (persisted snapshot path reported with the trend line).
- **Assessment method:** native platform → Assessment A only; detector (Assessment B) not applicable. Assessment A was split across five independent seat reports (\`seats/material-conformance.md\`, \`seats/arabic-first-ux-rtl.md\`, \`seats/accessibility.md\`, \`seats/ux-flows.md\`, \`seats/design-director.md\`) and synthesised here; the Lead also re-verified every quoted line and re-derived the headline measurements.
- **Ignore list:** \`.impeccable/critique/ignore.md\` did not exist.
- **Device evidence:** fresh debug install on Android 16 ATD, 1080×2400 @ 420 dpi, then landscape 2400×1080 via \`wm user-rotation lock 1\`; UI dumps via \`uiautomator\`; screenshots via \`adb exec-out screencap -p\` (valid PNGs — an earlier session's black captures were caused by a text-mode redirect, not the emulator). Arabic UI exercised with \`cmd locale set-app-locales com.sigha.converter --locales ar\`; forced Dark via the in-app Settings control.
- **Screenshots:** \`evidence/arabic-light-portrait.png\`, \`evidence/arabic-font200-portrait.png\`, \`evidence/arabic-forced-dark-system-light.png\`, \`evidence/english-landscape-empty.png\`.
- **Limitations:** no TalkBack session; no Arabic-native copy review; no OEM-skin device; contrast computed, not measured; the emulator's screenshot of the first frames after locale change can lag one frame (re-captured after settling).
- **Working tree:** only documentation added under \`docs/council/2026-11-17-ui-review/\`; no app-source edits.
