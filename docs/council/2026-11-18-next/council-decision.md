# Council decision — what remains, what next (2026-11-18)

**Method:** Qiyas Council, Mode C (merged) — causal-analogical reasoning + closed council, ending with a
silent Maestro. **Question:** for Sigha 0.2.1 (code-complete, unreleased), what remains, and what next —
ship path vs 0.2.2 design work? **Seats:** the same council that ran the 2026-11-18 review, re-convened on
the new question. **Rounds:** 2 (positions, then forced revision under attack). **Evidence:** repository at
`93355e2`, gate green (`docs/qa/evidence/verify-20261118-193810.json`), device smoke x86_64
(`docs/qa/evidence/device-smoke-20261118-195244.json`).

| Seat | Role | Qiyas tool |
|---|---|---|
| design-director | The Expert / الخبير المتخصص | Takhrij al-Manat + Athar thani |
| engine-auditor | The Engineer / المهندس | Al-Far' |
| build-verifier | Devil's Advocate / محامي الشيطان | Al-Qawadih |
| product-strategist | The Naive User / الزبون الغبي | Tahqiq al-Manat |
| policy-guardian | Policy Guardian / حارس السياسات (dynamic) | Fasad al-I'tibar |
| app-auditor | Arabic-First Editor / المحرر العربي (dynamic) | Tahqiq al-Manat (language) |
| lead | Maestro / المايسترو | rules what survived |

Seat files: `docs/council/2026-11-18-next/seats/`.

---

## Step 0 — frame and ground

**Goal.** Decide the next move for an unreleased, code-complete 0.2.1: whether to start the Play closed
test, what must change before it, and what waits. Done means a dated sequence with one build, one evidence
set, and a stop rule.

**Hard constraints (grounded in the tree).**

- The upload cannot be today's artefact: the shipped legal text names itself a placeholder —
  `app/src/main/assets/legal/privacy-policy.md:5,65` ("legal name to be completed", "placeholder address"),
  `privacy-policy.ar.md:5,65`, `source-offer.txt:28-29,34`, `relink-instructions.txt:23`, `NOTICE.md:48-50`.
- No arm64 code has ever run: the only device evidence is debug x86_64 under the QA key
  (`device-smoke-20261118-195244.json:64`; `docs/qa/verification.md:103,112-113,117,163-164`).
- The device checklist (A–H, ~60 rows) is unrun (`docs/qa/device-checklist.md:98-103`); the only recorded
  conversion is one manual TXT→PDF (`docs/qa/verification.md:140-141`).
- Human-only blockers B1–B7 are all OPEN (`docs/legal/release-blockers.md:10-22`); B6/B7 attach at the first
  conveyance, i.e. the first tester install, not at production.
- The gate certifies archive shape, not device behaviour: 137 JVM tests (no Android/SAF/MediaStore/FFmpeg/
  Tesseract code), lint, zip checks. Its claim-drift step scans 10 strings in 3 docs (`scripts/verify.ps1:237-259`);
  its signature step records no signer identity (`scripts/verify.ps1:213-218`).
- No telemetry, no accounts, no crash SDK (`PRODUCT.md:121-124`) — the 14-day closed test is the only
  user-contact channel, and it is a compliance gate (12 testers × 14 continuous days for personal accounts
  created after 2023-11-13) before production access (`docs/product/store-listing.md:239-265`).
- No CI, no remote; the builder's time is limited; a physical arm64 phone is required and its availability is
  unconfirmed (the one unknown that can stop everything).

### What remains — three lanes

**Lane A — upload-tree code (builder, ~2 days).**

| # | Item | Evidence |
|---|---|---|
| A1 | Final legal shipped text (EN+AR policy, NOTICE, source offer, relink text) — needs B2 URL, B6 mailbox, publisher legal name | assets above |
| A2 | `ConversionService.onTimeout` — Android 15+ stops a `dataSync` batch past 6 h/24 h; no handler exists | `ConversionService.kt:26-56`; manifest `:49-53` |
| A3 | Batch outcome in the pinned bar: n-of-m + current file; destination in the batch result | `WorkbenchScreen.kt:874-883` |
| A4 | CTA names its target ("Convert to PDF") | `WorkbenchScreen.kt:906-916` |
| A5 | OCR failure kind: `NO_TEXT_LAYER`/`MODEL_PACK_MISSING` are declared and mapped but never thrown, so a scanned PDF without the pack shows the generic "conversion failed" instead of the install instruction | `PdfToTextConverter.kt:93-97`; `OcrSupport.kt:27,34,35`; `FailureUi.kt:18-19,27` |
| A6 | Five AR strings + doc corrections (details label says English, OCR title Arabic-led, one privacy date, destination label, D1/task-1 wording, F1 row) | `values-ar/strings_ui.xml:12-13`; `strings.xml:165,215`; `strings_legal.xml:25`; `device-checklist.md:65,84`; `store-listing.md:269` |
| A7 | Remove the AI-upscaling card from the upload build (shipped "not available yet") | `ModelPacksScreen.kt:238-243`; `values-ar/strings.xml:218` |
| A8 | Gate hardening: shipped-legal placeholder scan; EN↔AR key/plural parity (~15 lines); record signer cert in evidence | `scripts/verify.ps1:213-218,237-259` |

**Lane B — human / legal / distribution (publisher; the critical path).** B5 name screen → B1 upload key and
Play App Signing → B2 public HTTPS policy (final text, both languages) → B6 LGPL source bundle hosted or a
valid written offer with a monitored mailbox → B7 counsel opinion or a recorded risk acceptance → B3
declarations (Data safety, foreground-service declaration + demo video, content rating, app access, ads,
target audience), listing assets (icon, feature graphic, 8 EN + 8 AR screenshots from the upload build),
15 testers recruited for a 12-tester cohort, then the 14-day clock and production access.

**Lane C — after the test (0.2.2 / 0.3).** Structural composition (target sheet opened from the bar, root
NavigationBar yields), bidi sweep with one wrap helper, per-app language (`localeConfig` + one locale source),
identity layer (write DESIGN.md now; decide the mark on a date), per-item retry + durable batches,
notification `setContentIntent`/cancel, motion/previews, dynamic-colour decision, numerals policy, string
hygiene, history per-row delete, queue truncation, and the 0.3 outcomes (many→one PDF, target-size compress).

---

## Transcript

**Expert (round 1).** The referral artifact is the converted file and one sentence — "my phone did this,
nothing was uploaded" — not the shell. Identity is recall at production discovery, after testers. Composition
splits: legibility (CTA names target, destination in the batch result) before the first production build; the
structural one-surface rewrite after device evidence. *Takhrij al-Manat:* the 0.2.0→0.2.1 cycle worked because
every fix was named by a measurement on a frozen artifact and re-measured — copy that, not the score.

**Engineer (round 1).** Engine code has nothing left in scope; what remains is device evidence. The two P1s are
two changes, one upload: batch outcome is render-only (~0.5 dev-day), composition is behavioural (~1 dev-day)
and its size hinges on one sentence — whether the root NavigationBar yields to the contextual bar
(`SighaNavHost.kt:59`). *Fasad al-I'tibar* against "ship now, build 0.2.2 during the window": the window measures
one build; vitals, the ≥95% reports, the checklist and the screenshots all name one artefact, so a mid-window
change voids the evidence the test exists to produce.

**Devil's Advocate (round 1).** Neither plan moves the binding constraint. The gate is a zip-shape fact: 137
tests, 32/32 `.so` stored, 16 KB ELF, lint 0 — but no arm64 code has ever run, and the release is signed with
the QA key (`CN=Sigha Local Verification`), not a publisher key. Claim-drift scans three docs. *Fasad al-I'tibar*
against "ship 0.2.1 now": the gate's subject is archive shape; the release's subject is device behaviour plus
key custody. *'Adam ta'thir* against "finish 0.2.2 first": its list does not touch the stated kill signal
(`council-review.md:266`) and design was already ruled non-blocking (`:255`).

**Naive User (round 1).** *Tahqiq al-Manat:* the plan's biggest item reports a batch whose reward is not shipped —
20 photos become 20 PDFs (`PRODUCT.md:47`), and "make it small enough for WhatsApp" is a quality slider, not a
target size (`:49`). The 14 days can produce a crash/claim census, not retention. The app's own OCR inline fix
is the precedent: put the answer where the user stands.

**Policy Guardian (round 1).** Green gate ≠ uploadable. The artefact ships legal text that names itself a
placeholder, so B2 is a content fix + rebuild, not a hosting chore; and LGPL duties attach per copy conveyed
(`LGPL-3.0.txt` §4(a)/(d)(0); `LGPL-2.1.txt` §6), so B6 belongs before the first tester install. New gap: Play's
foreground-service declaration (description, user impact, use case, demo-video link) is mandatory for a
targetSdk-36 build declaring `dataSync`, and it is absent from B1–B7 and the listing gate. The 12×14 rule and
the "no collection" Data-safety answers are stated correctly.

**Arabic-First Editor (round 1).** Not blocked by the bulk: plurals and the saved-path wrap are closed. Blocked
by three red-line instances — raw English diagnostics under an Arabic-only label, the destination label, the
double privacy date — and by a gate that cannot see Arabic: the N6 bug passed 137 green tests. Precedent: the
typed-Note migration moved a class of defects behind one enforceable boundary; do the same with one wrap helper
and a parity test, not five hand edits.

**Round 2 — forced evolution.**

**Devil's Advocate** (attacked by Policy, Engineer, Arabic): drops 0.2.1 as candidate and its pass-then-rebuild
order. *"Policy is right and it is verifiable inside the shipped assets … checklist F5/F6 would pass that text."*
Keeps: rebuild-before-pass, but the rebuild must stop at the last behavioural change, and a 30–60 minute arm64
smoke precedes it as insurance. Concedes Arabic coverage ≠ correctness; native read enters the red line.

**Engineer** (attacked by Devil, Expert, Policy): drops structural composition from the upload tree — it rewrites
the only writer of `state.target` (`WorkbenchViewModel.kt:181-189`) and cannot share the single pass. Downgrades
to "legibility only", one tree, two commits; FGS switch is not one manifest line (`mediaProcessing` is API 35
against minSdk 26, typed start, dual declaration ≈0.5 dev-day).

**Expert** (attacked by Devil, Engineer, Naive): *"my round-1 sequence is dropped. The shipped legal text names
itself a placeholder, so no honest upload exists without a rebuild … one rebuild, one upload artifact."* Folds
the legibility half into the forced rebuild, with two added checklist rows; no 0.3 before the test — add one
script question instead.

**Naive User** (attacked by Policy, Expert, Engineer): accepts that the closed test is a production-access gate,
not a retention experiment; withdraws the objection but keeps the exit-wording caution. Re-scopes the pre-upload
cut to what the tester script actually grades, and contributes the verified OCR failure-kind defect: a scanned
PDF without the pack shows "conversion failed" instead of the install instruction, so tester task 4's expected
result is missing from the build.

**Policy Guardian** (attacked by Devil, Engineer, Expert): drops the `mediaProcessing` switch (keep `dataSync`,
console answer + demo video); keeps `onTimeout` as a required code fix; produces the one-final-build order:
freeze inputs → one commit (final legal text, `onTimeout`, the ruled P1s) → verify + tag → device checklist on
that tag → screenshots → publisher builds the AAB from the same tag with the upload key.

**Arabic-First Editor** (attacked by Expert, Devil, Engineer): drops the pre-test six-moment review to a 15-minute
human read, drops the `formatSavedPath()` test to 0.2.2, downgrades the destination label to one string + two doc
rows, and proves no EN↔AR parity or plural check exists anywhere today.

---

## Maestro's verdict

### 1. Verdict table

| Branch | Status | Cause / constraint it carries |
|---|---|---|
| 0.2.1 as the upload candidate | **Dropped** | Placeholders inside shipped assets; QA-key signing; no arm64 evidence |
| Rebuild before the device pass | **Survived** | One tree, two commits; the pass must see the artefact that ships |
| Batch outcome + CTA legibility in the upload build | **Survived** | P1, render-only over existing state (~4–6 h); makes failure legible in the script's batch task |
| OCR failure-kind fix | **Survived** | New verified defect; one `kind` argument makes the scan instruction real |
| `ConversionService.onTimeout` | **Survived** | Android 15+ `dataSync` 6 h/24 h; required regardless of FGS type |
| Five AR strings + 15-min native read | **Survived** | Small, in an already-rebuilt build; native read is human-only |
| Gate hardening (placeholder scan, parity check, signer record) | **Survived** | A validation that does not gate is decoration (Card 4) |
| Remove AI-upscaling card | **Survived** | Shipped "not available yet"; Expert red line; one block |
| Structural composition (target sheet + nav yield) | **Suspended** | Rewrites the target writer; own evidence after the test |
| Bidi sweep / per-app language / identity layer | **Suspended** | Post-access; DESIGN.md rule can be written now |
| 0.3 outcomes (many→one PDF, target-size compress) | **Suspended** | Own experiment after production access; one script question now |
| `dataSync → mediaProcessing` switch | **Dropped** | `dataSync` + "Local processing: Other" is defensible; no manifest change |
| Device pass on 0.2.1 before the rebuild | **Dropped** | It would bless placeholder text; one pass only |
| 14-day test as desire validation | **Dropped** | It is a compliance gate + claim/crash census |
| Signer fail-condition in the local gate | **Downgraded** | Record the cert; Play enforces the upload key |

### 2. Step Zero — the falsification test

**30–60 minutes, on a physical arm64 Android 14+ phone, before any rebuild:** install the existing
`app-arm64-v8a-release.apk` (QA-signed, R8-minified — the shipped shape), cold start, one JPG→PNG, one
PDF→TXT, one scanned PDF→TXT without the pack, one share-in, one batch backgrounded for two minutes.

| Result | Decision |
|---|---|
| Passes | Open the upload tree (Phase 1) and rebuild |
| Crashes or produces nothing | Fix first; the rebuild becomes a rescue; re-run Step Zero |
| No arm64 phone available | **Stop.** The plan is blocked; acquire or borrow a device before any code work |

### 3. The upload build — exact contents (one tree, two commits)

1. **Plumbing commit:** legal slots for A1 (real values land when B2/B6 close), `onTimeout` (A2), OCR failure
   kind (A5), five AR strings + doc corrections (A6), AI-card removal (A7), gate checks (A8).
2. **Legibility commit:** batch position + destination in the bar/result (A3), CTA names its target (A4).
3. `scripts/verify.ps1` green on the final tree; tag it.
4. Freeze rule: **no placeholder string may remain in shipped assets** when the tag is cut; every later change
   waits for the next release.

### 4. Sequence to the clock

| Phase | Who | Work | Gate |
|---|---|---|---|
| 0 (today) | builder + publisher | Step Zero; B6 source bundle; legal text with slots; FGS/Data-safety answer sheet; trademark screen | Step Zero passes |
| 1 (≈2 days) | builder | Upload tree (two commits), gate hardening, emulator pre-check | `verify.ps1` green |
| 2 (inputs) | publisher | B5 name decision, B1 upload key, B2 URL + legal name/contact, B6 hosting/mailbox, B7 opinion or risk acceptance | All values in the tree; no placeholder remains |
| 3 (evidence) | builder + publisher | Tag → device checklist A–H on the tagged QA-signed arm64 release; 8 EN + 8 AR screenshots; 15-min native Arabic read; tester script + exit interview | Checklist recorded against the tag |
| 4 (upload) | publisher | AAB from the same tag with the upload key; declarations; listing; Closed testing with 15 testers | Upload accepted |
| 5 (14 days) | — | Freeze the tested tree; 0.2.2 on a branch, not uploaded; collect script + exit answers | ≥8/12 tasks per tester; zero false claims |
| 6 (access) | publisher | Production-access application | — |

### 5. Continue / kill metrics

- Every tester completes ≥ 8 of 12 script tasks; ≥ 95% conversion success; crash-free sessions ≥ 99.5%;
  zero false claims found (a false claim is a P0 for the listing, `store-listing.md:292-293`).
- Exit instrument (new): what did you convert first; did you find the file without help; would you show this to
  a friend and what would you say; **if you converted many photos at once, what did you want next?**
- Kill signal: testers who install the OCR pack but never convert a scan (`council-review.md:266`) — that is an
  OCR-flow defect, not a user verdict.
- Do **not** read retention or desire from the 14 days: the cohort is a device matrix, not a sample.

### 6. Risks and mitigations

- **No arm64 hardware** — the real critical path; Step Zero answers it today.
- **Legal values arrive late** (B2/B6/B7) — the freeze is legal-bound; the code lane fills the wait, it does not
  unblock it.
- **First run of the shipped shape** (R8, arm64) — Step Zero + the checklist catch it before the clock.
- **Mid-window changes** — freeze rule; 0.2.2 lives on a branch.
- **No telemetry** — exit interview is the instrument; never over-read it.
- **Arabic bidi remains systemic** — five strings + native read now; helper and sweep after access.
- **QA key vs upload key** — one tag, two signatures; QA evidence and upload artefact differ only in signature.

### 7. First action today

1. **Confirm a physical arm64 Android 14+ phone and run Step Zero** on the existing release APK (30–60 min).
2. **Open the upload tree** and write the plumbing commit (A1 slots, A2, A5, A6, A7, A8), then the legibility
   commit (A3, A4).
3. **Start the human lane in parallel:** B6 source bundle and legal text with slots can be done without
   publisher accounts; B5 and B1 are the publisher's.

---

## Run notes

- **Seats:** six of the existing eight council members were re-convened (design-director, engine-auditor,
  build-verifier, product-strategist, policy-guardian, app-auditor); material-conformance and a11y-auditor
  sat out — their dimensions were not the decision's edge. Each wrote one file under
  `docs/council/2026-11-18-next/seats/` and reported a ≤15-line summary; no seat edited app source, scripts,
  QA docs or another seat's file.
- **Read-only:** this decision made no app-source edits; the upload tree is the next build, not this commit.
- **Verified by the Lead before ruling:** the shipped placeholders (`privacy-policy.md:5,65`;
  `source-offer.txt:28-29,34`; `relink-instructions.txt:23`; `NOTICE.md:48-50`), the missing `onTimeout`
  (`ConversionService.kt:26-56`), the unthrown failure kinds (`PdfToTextConverter.kt:93-97`;
  `OcrSupport.kt:27,34,35`; `FailureUi.kt:18-19,27`), the x86_64-only device evidence
  (`device-smoke-20261118-195244.json:64`), and the gate's signature/claim-drift scope (`verify.ps1:213-218,237-259`).
- **External claims:** the Play rules in the Policy seat's report were checked against Play Help pages cited
  in `seats/policy-guardian.md`; everything else is in-repo. The arm64 device availability is an assumption,
  not a fact — Step Zero turns it into a test.
- **Still unverified:** physical-device behaviour, native Arabic quality of the five strings, counsel opinion,
  trademark clearance, Play Console state (account type, declarations), and the tester cohort.

**Decision date:** 2026-11-18. **Next trigger:** Step Zero result, then the upload-tree tag.