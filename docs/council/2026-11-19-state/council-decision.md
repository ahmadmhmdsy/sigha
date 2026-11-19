# Council 2026-11-19 — app state, production readiness, and ASO

**Mode:** Qiyas Council, Mode C (merged: method header + role cards + global invariants + transcript +
Maestro verdict). **Seats:** 6 (4 core + 2 dynamic). **Rounds:** 2 + Maestro.
**Question (Q1–Q5):** Is the app usable today and production-ready? What remains and in what order?
Is the ASO/listing plan sound — title, short and full description, metadata, category, tags,
screenshots, web SEO? What is being missed? What must not be done now?

**Evidence base:** `docs/qa/verification.md` (§5–§10), `docs/qa/evidence/verify-20261119-004710.json`,
`docs/legal/release-blockers.md` (B1–B7), `docs/plan/publisher-guide.md`, `docs/product/
aso-plan-2026-11-19.md` (v1 draft under review), `docs/product/store-listing.md`, `PRODUCT.md`,
`docs/council/2026-11-18-next/council-decision.md`, and live Play/App-Store/web searches run for this
review (Iraq storefront). No claim below rests on a number that cannot be re-run locally.

## Method header

The council reasons by **naql** (transfer of the ruling from a prior, evidenced case) and **takhrij**
(deriving the cause, *'illa*, that makes the transfer valid). The **qawadih** (objection tools) used
this round: *naqd* (contradiction), *farq mu'aththir* (a difference that changes the ruling),
*fasad i'tibar* (invalid premise), *'adam ta'thir* (the objection does not move the verdict), and
*athar thani* (the second-order effect). Two rounds of forced evolution were run; a position could
only survive if it answered the strongest objection against it. The Maestro rules last and may split
a seat's recommendation (survive part, drop part).

## Role cards

| Seat | Core/dynamic | Lens | Round-1 headline |
|---|---|---|---|
| **The Expert / الخبير المتخصص** | core | domain knowledge, lead time, market evidence | buyer-unusable QA build; exact long tails, not head terms |
| **The Engineer / المهندس** | core | build, signing, runtime evidence | green gate certifies the wrong artefact; arm64 has never run |
| **The Devil's Advocate / محامي الشيطان** | core | attacks the plan and the evidence | ASO v1 fails; the PDF title buys mismatched one-star installs |
| **The Naive User / الزبون الغبي** | core | the user's actual job | the app answers a format question, not the job typed into search |
| **The Policy Guardian / حارس السياسات** | dynamic | Play policy, LGPL, accuracy of claims | title/description are claim channels; placeholder offer is an LGPL §4 failure |
| **The Arabic-First Editor / المراجع العربي** | dynamic | Arabic queries, RTL, parity | the listing forfeits the Arabic title slot; parity must be a gate |

## Global invariants

1. A claim in the listing is a promise the build must keep; an unshipped outcome is a policy defect
   (Play metadata accuracy), not a marketing choice.
2. Evidence must be re-runnable locally; no figure, size or hash enters a doc without its command.
3. The upload artefact must be the tagged, upload-key-signed build; the QA key and its evidence are
   for Step Zero only.
4. LGPL duties attach at the first conveyance of a copy — the first tester install, not the first
   production install.
5. The 12-testers × 14-continuous-days clock is a compliance gate, not a retention experiment; it
   cannot start until the legal and signing preconditions are in place.
6. Arabic is a first-class locale: parity is enforced by a gate, not by review effort.

---

## Round 1 (positions, objections, proposals)

**Expert.** Not usable by a buyer; QA-signed sideload only. Order: Step Zero arm64 → B6/B7 before any
tester install → B2 → B1 upload key/App Signing → B3 declarations/assets → B5 in parallel. ASO: v1
sells "صور إلى PDF" (plural) and "PDF converter" while the build ships one image → one PDF and no
merge/compress, so unservable ranking buys one-star reviews; owns only true long tails (صورة إلى PDF,
PDF إلى نص, DOCX إلى PDF, فيديو إلى MP3, HEIC إلى JPG, بدون إعلانات); "48 formats" style counts are
out; blind spots: QA-key AAB under App Signing plants a key you do not control, the continuous-day
clock, legal values inside the tester build, `raw.githubusercontent.com` from Iraqi networks unverified,
large-video OOM on low-RAM devices.

**Engineer.** Usable only as an emulator build; not production-ready. The gate is green on an artefact
that cannot be uploaded (21 placeholders, QA-key signature); the pre-launch report confirms, it does
not falsify — it is downstream of account, key, forms and listing. Seven days: arm64 AVD or borrowed
arm64 phone (30 min), open the account and generate the upload key, supply real legal values. Drop
"File & PDF" from the title: it imports merge/split/edit intent the app does not serve.

**Devil's Advocate.** Not usable, not production-ready; ASO v1 fails. The fatal flaw is the PDF title:
IQ PDF searches resolve to edit/merge/compress tools Sigha lacks. A crawl cannot exercise SAF,
MediaStore, OCR download, background or API 26–28; 137 JVM tests certify archive shape and EN↔AR
tokens, not claims or device behaviour. Title "Sigha: File Converter" + Arabic localized
"صيغة: محوّل الملفات"; PDF qualified in the short description; purge "offline", bare "no upload",
format counts, Filee's name, unsupported "secure/private". Blind spots: placeholders, no arm64
evidence, first-tester duties, tester mechanics, missing Play foreground-service declaration + demo
video (already flagged by the 2026-11-18 council and absent from B1–B7).

**Naive User.** Fatal flaw by *tahqiq al-manat*: I type outcomes (محول الصور الى pdf, تحويل الفيديو الى
MP3, docx pdf); the store shows PDF tools; the honest-limits block admits the four things I want
(many images → one PDF, scanned pages → one PDF, merge/split/compress, target-size compress) are not
shipped; iLovePDF already does them. The 12×14 test proves crashes, not desire. Screenshots show a
picker and a slider, never the result. Title "Sigha: Image to PDF Converter" (29) — outcome-first.

**Policy Guardian.** Usable: no; production-ready: no. Green gate ≠ uploadable. "Offline/on-device"
is an absolute contradicted by the 28 MB OCR download; "PDF" in the title imports merge/split/
compress intent (Help 9898842); HEIC→JPG needs an inline Android-9 qualifier. Answer the Play
foreground-service declaration (targetSdk 36, `dataSync`; description, user impact, use case, demo
video) — mandatory and absent from the blockers register. Arabic PDF head terms are PDF-tool-owned;
target shipped long tails. Add Arabic localized title.

**Arabic-First Editor.** Not production-ready; x86_64 only. The ASO plan's claim that Play indexes the
title locale-blind is false — console translations include the app name, and the Arabic label is
already صيغة (`values-ar/strings.xml:11`). Keep EN title, add AR "صيغة: محول صور وملفات إلى PDF"
(29/30); the EN short at 79 drops the typed "free"; the AR short (76) misses the typed "تحويل" and
the PDF intents its own evidence calls the volume. Extend the existing claim-drift gate
(`scripts/verify.ps1`) to listing fields so claim/locale drift is caught by a check, not by review.
Blind spots: arm64/14+ native crash (16 KB pages, FFmpeg ABI), 14-day reset, distribution duties at
first install, Data-safety/listing mismatch, late B5 rename.

**Round-1 convergence:** all six seats: *not usable for a buyer; not production-ready*; all six put
arm64 evidence and the legal/signing lane before any tester install. **Split:** the PDF token in the
title, the Arabic title, and the short-description strategy.

## Round 2 (forced evolution — ratification votes)

The Lead put six items to four seats with the revised plan; each had to ratify or kill with a cause.

| # | Item | Votes (R = ratify, K = kill/replace) | Ruling |
|---|---|---|---|
| 1 | EN title `Sigha: File Converter` (PDF out) | Devil R, Expert R, Policy R, Arabic-First K | **Survived** |
| 2 | AR title: A PDF/صور (29), B ملفات+PDF (23), C ملفات على هاتفك (27) | Devil C, Policy C, Expert B, Arabic-First A/K | **C survived** |
| 3 | EN short: A head-term 79, B outcome pairs 71, C no-upload 71 | Devil B, Expert B, Policy B, Arabic-First A | **B survived** |
| 4 | AR short: keep v2 generic vs outcome pairs | Policy keep (substitute الصور والمستندات), Arabic-First keep, Devil replace, Expert replace | **Tie → Maestro: outcome pairs, singular صورة, typed gerund first** |
| 5 | Screenshot order: outcome-first vs picker-first | Devil (shot 1 only), Expert (shot 1 only), Policy reorder, Arabic-First change shot 1 | **Outcome-first survived; shot 1 = one image → PDF; OCR card at 5** |
| 6 | Explicit "does not edit/merge/split/compress PDFs" line | All R | **Survived, moved before the read-more fold** |

**Final round-2 fatal flaws accepted into the plan:**

1. *Placeholder greening:* passing `-ReleaseCandidate` by deleting TODO lines instead of supplying
   real B2/B6 values would ship a source offer naming no source — the exact LGPL §4 failure. The gate
   must pass with real values, and B2/B6 must be live.
2. *Uninstalled signing identity:* no upload-key-signed AAB has ever been installed anywhere; the
   QA-signed APK is a different identity. Install the upload-key build once on the borrowed arm64
   phone and walk the script before the clock starts.
3. *OCR precondition:* "PDF to text" hides that scanned PDFs need the 28 MB optional pack; the OCR
   sentence must sit beside the PDF bullets, or the claim reads false to the users most likely to
   install.
4. *FGS demo video:* record it in the same one device pass, on the tagged arm64 build; a later
   re-shoot costs a second pass and an inconsistent artefact.
5. *Plural "صور إلى PDF":* the listing must not ride the many-images-to-one-PDF intent (0.2.2/0.3,
   not shipped); singular "صورة" in the AR short and one image in shot 1 kill the implication.

## Maestro verdict

| Item | Verdict | Cause |
|---|---|---|
| "Usable today" | **Downgraded**: usable only as a sideloaded QA build on the tested x86_64 emulator; never on arm64, never as the upload-signed artefact | ABI-specific native loading and SAF/MediaStore paths differ; the only install evidence is an emulator |
| "Production ready" | **Dropped** — no | 21 shipped placeholders, QA-key signature, B1–B7 open, 0 arm64 runs, 0 upload-key installs, no Play account, no owned domain/mailboxes, FGS declaration pending, clock not started |
| EN title "Sigha: File & PDF Converter" (v1) | **Dropped** | Unserved merge/edit intent; the title is a claim channel |
| EN title "Sigha: File Converter" | **Survived** | Provable; head term; PDF qualified in the short description |
| AR localized title (PDF-first) | **Dropped**; "صيغة: محوّل ملفات على هاتفك" (27/30) survived | "محوّل PDF" is iLovePDF/WPS intent; "على هاتفك" is the honest differentiator. *Dissent recorded.* |
| EN short v1 (head term + families, 79) | **Dropped**; "Image to PDF, PDF to text, DOCX to PDF, video to MP3 — free, no upload." (71) survived | Exact-direction long tails are winnable; head terms are a funded brand war |
| AR short v2 generic (71) | **Downgraded** to the outcome-pair line (79) | Carries the PDF/صور intents without the plural overclaim. *Dissent recorded (Policy: keep with "الصور والمستندات").* |
| "Offline"/"on-device"/"no network" absolute | **Dropped** from every listing field | The 28 MB OCR download is a network request; only the qualified sentence survives |
| Format counts ("48 formats", "recognises 48, converts 39") | **Dropped** from store copy | Dates badly, invites "where is X?", exceeds what users need; counts stay in technical docs |
| "Free" in the EN short | **Survived** | Typed autocomplete term ("file converter app free") and true of the build |
| Category Tools | **Survived** | Utility, not an office suite; Product category must not imply fidelity |
| Picker-first screenshots (v1 set) | **Dropped**; outcome-first order survived | Users must see the result and the saved file; top uninstall answer |
| OCR card at position 7 | **Downgraded** to 5 | The OCR pack is the answer for scans; keep it before the batch/quality shots |
| No-edit/merge/split/compress line | **Survived**, moved to the first ~300 chars | Cheapest place to kill mismatch is before install |
| "Play pre-launch report closes the arm64 gap" | **Dropped** | It is a crawl/startup check, downstream of account, key, forms and listing; it cannot exercise SAF, MediaStore, OCR, background or API 26–28 |
| ASO experiments / web SEO before the upload build | **Suspended** until the first upload-key build and the closed test | The text moves nothing while installs are zero; the site needs an owned domain |
| 0.2.2 design/feature work, dependency upgrades, R8 changes, paid traffic | **Suspended** | Not on the critical path; each adds risk before the first conveyance |

## Step Zero (the first action)

Borrow an arm64, Android 14+ phone for 30–60 minutes. Install the **QA-signed release APK** (Step Zero
only; the upload-key AAB is installed later, once, before the clock). Walk: cold start → JPG→PNG →
DOCX→PDF → text-PDF→TXT → scanned-PDF OCR pack download **on an Iraqi network** → share-in →
background conversion with the notification → Arabic RTL walk → record build SHA, device model, Android
version, logcat for native crashes. This is the only evidence that can falsify the arm64 gap; the
pre-launch report is the automated second net, not a substitute. If no phone is available within
48 hours, the fallback is one instrumentation smoke test on Samsung Remote Test Lab or Firebase Test
Lab (not Robo), and the gap stays explicitly open.

## Metrics that would change the verdict

| Metric | Now | Target to re-rule |
|---|---|---|
| arm64 release runs | 0 | ≥1 on Android 14+ with the script above |
| upload-key AAB installs | 0 | ≥1 on the borrowed phone before the clock |
| `verify.ps1 -ReleaseCandidate` | red by design (21 placeholders) | green **with real B2/B6 values** |
| release signers | QA key `CN=Sigha Local Verification` | upload key recorded, App Signing enabled |
| testers opted in | 0 | 12 continuously for 14 days (15–18 recruited, dated log) |
| B1–B7 | all OPEN | closed in order B5 → B1 → B2 → B6/B7 → B3 |

## Blind spots (merged, ranked)

1. **arm64 has never run** — native FFmpeg ABI, 16 KB pages, SAF/MediaStore; Step Zero, and install
   the upload-key build once pre-clock.
2. **Signing identity** — uploading the QA-key AAB would bind a key you do not control under App
   Signing; generate the upload key, enable App Signing, re-sign.
3. **The 14-day clock is fragile** — 12 testers opted in continuously; a dropout, a build change or a
   track change restarts it; recruit 15–18, keep a dated opt-in log, freeze the build.
4. **Legal duties attach at first conveyance** — B6 source offer hosted, B7 written, B2 URL live, zero
   placeholders before the first tester install; never green the gate by deleting TODO lines.
5. **Domain and mailboxes** — `sigha.app` serves another product ("Sigha — Salesforce Formula
   Debugger" on GitHub Pages); `sigha.com` is taken; every `privacy@/opensource@sigha.app` placeholder
   is unusable. Buy an owned domain, create the mailboxes, then B2/B6 and web SEO.
6. **Foreground-service declaration + demo video** — mandatory for targetSdk 36 `dataSync`; record in
   the same arm64 pass on the tagged build.
7. **Data safety vs claims** — "no upload" must coexist with the OCR pack's host/IP disclosure; HEIC
   (Android 9+) and AVIF (Android 12+) need inline qualifiers against minSdk 26.
8. **OCR host is a single unmirrored dependency** — `raw.githubusercontent.com` answered 200 / 262 KB
   range in ~0.6 s from Baghdad on 2026-11-19 (checked for this council), but GitHub blocking, rate
   limits or a dead commit would break the pack; keep a mirror, pin the commit, test on-device in Iraq.
9. **Low-end device memory** — large-video FFmpeg may OOM on 3–4 GB devices (assumption); read Play's
   pre-launch device list and test one low-RAM phone.
10. **ASO after launch** — no in-app rating prompt, no web presence yet, experiments need a live
    listing; the title/description are frozen until B5 answers.

## Readiness verdict (the one-line answer)

**Usable today: only as a sideloaded QA build on the tested x86_64 emulator — not for a buyer.**
**Production-ready: no.** The shortest honest path is Step Zero (borrowed arm64 phone) + the
account/key/domain lane in parallel, then B5 → B1 → B2 → B6/B7 → tag → declarations → upload-key AAB →
12×14 → production access.

## Files this council changed

- `docs/product/aso-plan-2026-11-19.md` — v2/v3: localized title, outcome-pair shorts, shot order,
  banned wording, OCR/no-edit placement, parity rule.
- `docs/product/store-listing.md` — metadata table (localized title), short/full descriptions,
  screenshot shot list.
- `docs/legal/release-blockers.md` — B2/B6 domain conflict; B3 foreground-service declaration,
  upload-key/precondition steps.
- `docs/plan/publisher-guide.md` — owned-domain placeholders in Steps 4–5, Step 2 pointer.
- `docs/legal/name-screen-2026-11-19.md` — `sigha.app` finding.

*Dissent register:* Arabic-First Editor (AR title should carry PDF/صور; Arabic market is PDF-framed;
revisit with Play Console search terms after the closed test); Policy Guardian (AR short should keep
the generic line with "الصور والمستندات"); Expert (EN title "Offline File Converter"; counts
"recognises 48, converts 39" in the full description). Each dissent is recorded above rather than
silently absorbed.
