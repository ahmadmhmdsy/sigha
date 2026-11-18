# Devil's Advocate seat — Qiyas Council, Round 1 (2026-11-18)

## 1. What remains in my dimension (evidence and release mechanics)

- **The gate certifies a shape, not the thing shipped.** `docs/qa/evidence/verify-20261118-193810.json` is green: 137 tests / 16 classes, 32/32 `.so` stored, `minPAlign=16384`, zipalign exit 0, lint 0 errors. Every one of those is a fact about a zip; none is a fact about a conversion.
- **No arm64 code has ever run.** `device-smoke-20261118-195244.json` facts: `abi: "x86_64"`, `monkeyEvents: 120`, `crashBufferLines: 0`. The release build ships arm64-v8a and x86_64; only the x86_64 branch has executed. The default `app-arm64-v8a-release.apk` (49.8 MB) has never been installed by any process.
- **The "signed release" is signed with the QA key.** `apksigner verify --print-certs` → `CN=Sigha Local Verification, OU=QA` (`keystore/README.md`: never for Play; `docs/legal/release-blockers.md`:31). `scripts/verify.ps1`:212 passes on any signature and records no signer identity; its success text is the literal `"unsigned: "`.
- **The claim-drift check is a 10-string scan of 3 files** (`scripts/verify.ps1`:237-259: README, PRODUCT, verification.md). It cannot see `CHANGELOG.md`, `store-listing.md`, `AndroidManifest.xml` or `docs/legal/`. "claim-drift clean" certifies almost nothing.
- **Council cautions.** *Theater:* this council cannot move the date — the date is fixed by an unrun device checklist (`docs/qa/device-checklist.md`:100-103; `store-listing.md`:247) and four OPEN human items, so a vote between "ship 0.2.1" and "polish 0.2.2" is a vote on the wrong variable, and seat-file consensus will look like a decision while changing nothing. *Unverified assumption:* "gate green" is read as "the app works", but the tree itself records `Device QA (physical phones, EN+AR) — not run` (`CHANGELOG.md`:109) and instrumentation `none in 0.2.x` (`PRODUCT.md`:121,123), so the closed test will not measure it either.

## 2. Recommended next sequence (max 5) and what I would not do now

1. Run `device-checklist.md` rows A–F on one physical arm64 phone (EN) plus one Arabic RTL pass; record results in `docs/qa/verification.md`. Cost: days. It falsifies the product claim before 14 days are at stake.
2. In parallel, the human lane in this order: B5 name → B1 upload key → B2 live privacy URL → B6 source tarball + real mailbox → B7 counsel, because a closed-test AAB is distribution (`release-blockers.md`:20-22).
3. Re-cut the upload artifact under the publisher key and assert in `verify.ps1` that the signer is not the QA key.
4. Make the gate fail when the device record is absent — else the new rule is also decoration (Card 4, below).
5. Only then start the 14-day clock, uploading 0.2.1 as it is; design was ruled not blocking (`council-review.md`:255).

**Would not do now:** a third design critique or a push past 24/40; the identity layer, per-app language and bidi sweep before first testers; treat the 120-event emulator monkey as device evidence; treat 58 lint warnings as a release gate.

## 3. Qiyas card

- **Al-Far'** — ruling: the release gate must consume the evidence of the claim it certifies. Before the clock starts, `verify.ps1` must fail when a recorded physical-device run of the conversion rows is absent, and the upload artifact's signer must be the publisher key.
- **Al-Asl** — the project's own Card 4: "a validation that does not gate is decoration" (`docs/council/2026-11-18-ui-review/council-review.md`:236-242), where the folder destination had to refuse up front rather than fail per file after the work. Verifiable in-repo; no external precedent needed.
- **Al-'Illa** — a check adjacent to, but narrower than, the promise it is believed to certify spends trust twice: once when it passes while the real condition is unmet, and again when the failure arrives after the docs said "verified". The device checklist and the folder picker share this: both discover failure only after the user's work.
- **Tanqih** — incidental: the 24/40 score, the 6763 ms cold start, the 120 monkey events, whichever seat proposed the rule. The 'illa survives without any of them.
- **Conditions** — (i) one physical arm64 phone, rows A–F executed, `adb logcat -b crash` clean; (ii) an Arabic RTL pass; (iii) the record is machine-readable by `verify.ps1`, not prose; (iv) a named owner and date, since human tasks otherwise float. If (iii) fails, the rule repeats the defect it corrects.

## 4. One fatal flaw in each obvious plan

- **"Ship 0.2.1 to the closed test now" — tool: Fasad i'tibar.** The plan takes a sound principle (a green gate precedes upload) and substitutes an invalid instance of its subject: the gate's subject is the archive's shape, while the release's subject is device behaviour plus key custody. Grounded breaks: arm64 never executed, signer is the QA key, and `device-checklist.md`:103 makes the device run a precondition of the upload. One afternoon of testing is traded for a possible 14-day reset and for spending the word "verified" on a build nobody ran.
- **"Finish 0.2.2 before any testers" — tool: 'Adam ta'thir.** The stated kill signal is OCR-install-then-no-conversion (`council-review.md`:266) and the release gate is device behaviour; the 0.2.2 list touches neither, and the authority already ruled 0.2.1 not blocked by design (`:255`). The alleged cause does not reach the alleged effect.

## 5. Red line before the 12-tester closed test begins

B1, B2, B6 and B7 closed (or a dated counsel note that closed testing this binary is lawful); device-checklist rows A–F passed on at least one physical arm64 device with a clean crash buffer and one Arabic RTL pass; the upload artifact signed by a key that is not the QA key; the record committed. Until then the clock stays stopped: it is 14 consecutive days that restart if the cohort changes materially (`store-listing.md`:299-301), and it must not be spent to learn what one afternoon on a phone would have said.

## Round 2 — revision

**Keep:** the gate must consume the evidence of what it certifies (Card 4); the signer assertion (publisher key, not `CN=Sigha Local Verification`); no clock before B1/B2/B6/B7; no scored-critique chase; no identity layer or per-app language before the first Arabic QA.

**Drop:** 0.2.1 as the candidate build. Policy is right and it is verifiable inside the shipped assets: `assets/legal/privacy-policy.md`:4-5,65 ("placeholder address … before the first public release"), `source-offer.txt`:28-29,34-37 ("PLACEHOLDER … TODO"), `NOTICE.md`:48-51, `relink-instructions.txt`:23 — checklist F5/F6 would pass that text. I also drop my "pass-then-rebuild" ordering: a full A–F pass is a day of human time that would then be paid twice.

**Downgrade:** bidi/string polish leaves my Round-1 exclusion. The Arabic editor is right that my red line was silent on Arabic, and `res/values-ar/strings.xml` proves coverage, not correctness: it enters the pre-pass set for the screens the checklist exercises; the full bidi sweep stays 0.2.2.

**(a) Policy changes the object, not the logic.** The pass must run on the artifact that can be uploaded, so: legal text → freeze → rebuild+sign → pass → clock. The legal edits cannot finish without B2's live URL and B6's mailbox, so the human lane becomes gating, not parallel; and the rebuild must be signed with the publisher key, so B1 precedes the freeze.

**(b) Rebuild-before-pass is the lesser evil (Engineer), with two conditions they omitted.** (i) The rebuild must stop at the last behavioural change — "land the P1s" as open-ended permission is how the window gets voided, and 0.2.2 item 6 (per-item retry, durable batches, notification intent) is behavioural. (ii) A 30–60 minute arm64 smoke on today's APK precedes the rebuild: not a pass, insurance that the thing being rebuilt can convert a file at all.

**(c)** Rebuild-before-pass, frozen after the last behavioural commit; smoke first; Arabic native read enters the red line (A5, F1, F6 plus one conversion), so the 14-day testers are not the first Arabic reader.

**(d) First action today:** freeze the candidate as "the next build, not 0.2.1", and run the 30–60 minute arm64 smoke (install, cold start, one JPG→PNG, one PDF→TXT, the OCR gate) on a physical phone — its result decides whether the rebuild is a fix or a rescue.

**New fact for the council:** the gate has no placeholder check (`verify.ps1`:237-259 scans 3 docs for 10 strings), so "claim-drift clean" is green on a tree whose own Licences screen says TODO; `store-listing.md`:292-293 makes any false claim a P0, and the written offer currently names a mailbox that does not exist.