# Release blockers that only a human can close

**Created:** 2026-11-17 · **Owner:** policy-guardian (WS4) · **Status:** all items **OPEN**; nothing in this file can be executed from the build machine.
**Companion:** [`docs/legal/licence-compliance.md`](licence-compliance.md) (what is satisfied today), [`docs/plan/production-readiness.md`](../plan/production-readiness.md) §"Human-only blockers".

Every row below is a **distribution blocker**, not a code task. The repository already contains the
texts, screens and offer prose that these steps publish or confirm; what is missing is human
credentials, human judgement or a human-hosted URL.

| ID | Blocker | Blocks | Status |
|---|---|---|---|
| **B1** | Play upload key ownership | first upload | OPEN |
| **B2** | Privacy policy public HTTPS URL | listing + closed test | OPEN |
| **B3** | Data safety form + listing + closed test (12 testers × 14 days) | production release | OPEN |
| **B4** | Merchant/entity decision for paid distribution | monetisation only | OPEN (decision) |
| **B5** | Trademark search/registration for the name "Sigha (صيغة)" | first upload (package/name are effectively permanent) | OPEN |
| **B6** | LGPL "Corresponding Source" hosting + `opensource@sigha.app` | any public distribution | OPEN |
| **B7** | Counsel sign-off on FFmpegKit LGPL obligations and open licence questions | any public distribution | OPEN |

**Order to close them:** B5 → B1 → B2 → B3 for the path to production; B6 and B7 must be closed
before the first public artefact (sandbox build is not exempt: any distribution of the APK triggers
the LGPL duties). B4 only if the app is ever sold or takes in-app payments.

---

## B1 — Play upload key ownership

**Why human:** the upload key is the publisher's cryptographic identity. It must be generated on
hardware the publisher controls, its secrets must never be in the repository, and losing it without
Play App Signing recovery is an account problem. A local verification keystore is created in this
tree (`keystore/`, gitignored) and **must never be used for Play** — the plan says so explicitly.

**Steps (publisher only):**
1. Decide the publishing identity (personal account vs organisation) and create/verify the Play
   Console developer account with 2-step verification.
2. Generate the upload key on the publisher's machine:
   `keytool -genkeypair -v -keystore sigha-upload.jks -alias upload -keyalg RSA -keysize 2048 -validity 10000`
   (use a strong password; record the alias and password in the publisher's password manager, not in
   the repo).
3. Enrol the app in **Play App Signing**: Play Console → the app → **Setup → App signing**. Play
   keeps the app-signing key; the key from step 2 becomes the upload key.
4. Back up `sigha-upload.jks` + password to **two** offline media plus a password manager; record the
   certificate SHA-256 fingerprint (e.g. `keytool -list -v -keystore sigha-upload.jks`) in the
   publisher's release notes (not necessarily in the public repo).
5. Put the Play upload keystore path/passwords in `keystore/keystore.properties` **on the publisher's
   machine only** for the signed release build; the repo file must stay local.
6. If the upload key is lost: Play Console → **Setup → App signing → Request upload key reset**; this
   is a support flow and takes days — do not learn it during a launch.

**Evidence to close:** upload certificate fingerprint recorded, keystore backups verified, a signed
release APK/AAB built with it and accepted by Play Console.

---

## B2 — Privacy policy public HTTPS URL

**Why human:** Play requires a hosted, publicly reachable HTTPS URL; a local file or an in-app screen
does not satisfy the listing requirement. The repository copy and the in-app screen (Added in WS4)
exist so the published page has a single source of truth.

**Steps:**
1. Publish `docs/legal/privacy-policy.md` at a URL the publisher controls, for example
   `https://sigha.app/privacy` (static host or custom domain). Requirements: HTTPS, no login, no
   redirect to an app-store page, stable URL, plain HTML readable on mobile.
2. Keep the published page's policy body **identical** to
   `app/src/main/assets/legal/privacy-policy.md` (the Markdown file is the source; the in-app screen
   renders it). When one changes, the other must change in the same commit.
3. Replace the placeholder contact in both privacy documents if the publisher creates a different
   address (currently `privacy@sigha.app` is a placeholder — the publisher must create and monitor it
   before publishing).
4. In Play Console: **App content → Privacy policy** → paste the URL. Also fill it in the store
   listing's "Privacy Policy" field if the console asks.
5. Confirm from a clean browser profile with no login that the URL loads and shows the effective date
   (2026-11-17) and the OCR-pack download disclosure.

**Evidence to close:** live URL, screenshot of the page, console screenshot of the saved field, and a
note in the release checklist that URL and asset are identical (hash the rendered text or diff the
Markdown).

---

## B3 — Data safety form, store listing, closed test (12 testers × 14 days)

**Why human:** all of it is in Play Console and requires the publisher's account and real testers.

**Steps:**
1. **Data safety** (Play Console → **App content → Data safety**). Answer for this build:
   - Data collected: **none**. Data shared: **none**. No analytics, ads, crash or attribution SDK.
   - "Is all of the user data encrypted in transit?" — the app transmits nothing; the only network
     call is the user-initiated OCR pack download over HTTPS. The policy names the host and the IP
     disclosure; the form itself stays "no data collected" because the app itself does not collect
     it (confirm this reading against the current form and, if in doubt, disclose the pack download
     under "Data collected: none" plus the policy link).
   - Confirm the privacy policy URL (B2) is entered when the form asks for it.
2. **App content**: content rating questionnaire (IARC), target audience (not primarily for
   children), ads declaration (**no ads**), news app (**no**), COVID-19 contact tracing (**no**),
   data safety, privacy policy, **App access** ("all functionality is available without
   restrictions") and government-app declaration (no).
3. **Store listing**: title (≤30 chars, "Sigha" — Arabic "صيغة" is acceptable in the title), short
   description (≤80), full description (≤4000) — every claim must match behaviour: local processing
   is true; do **not** say "never uses the internet" — say "conversion runs on your device; optional
   OCR packs download once, size shown before download". Icon, feature graphic, phone screenshots.
   Do not use Filee's name, logo or screenshots.
4. **Closed testing**: Play Console → **Testing → Closed testing** → create a track, add the 12+
   testers (email list or Google Group), upload the signed AAB, release, and keep 12 testers opted in
   for **14 continuous days** (personal accounts created after 13 Nov 2023 must meet this before
   applying for production access). Feed testers the same build the production track will use.
5. **Production access**: after the 14 days, apply in Play Console → **Dashboard → Production
   access**; answer the questions truthfully and describe the testing feedback loop.

**Evidence to close:** Data safety submission confirmation, listing screenshots, closed-test track
with 12 opted-in testers and dates, production-access approval.

---

## B4 — Merchant/entity decision for paid distribution

**Why human:** it is a legal/accounting decision (who sells, where the money is paid, how tax is
declared). The plan notes Play merchant registration is not available for Iraq; that constrains
"paid app" distribution.

**Steps:**
1. Decide the monetisation model: free (recommended for v1), voluntary donation outside Play,
   paid app, or in-app purchases. Ads would additionally need ad-content review and a fresh
   ḥalāl review (clearance memo Q2).
2. If paid/IAP: Play Console → **Setup → Payments profile** → create merchant/developer account for a
   supported country/entity, provide legal entity, address, tax and bank details, link it to the
   developer account, and complete the payments-profile verification.
3. Record the decision and the reasoning in this file (or a decision record under `docs/adr/`), since
   it changes the Data safety answers, the listing and possibly the clearance memo.

**Evidence to close:** signed-off decision record; if monetised, an approved payments profile linked
to the account.

---

## B5 — Trademark search and registration for the name

**Why human:** name clearance is a legal search plus a registry filing; a quick Play search is not
proof (the clearance memo already found conflicts for alternatives and calls "Sigha" only a
preferred candidate).

**Steps:**
1. Search before the first upload — the package name and listing title are effectively permanent after
   release even though code can change.
2. Search: (a) Google Play and App Store for "Sigha"/"صيغة"; (b) Iraqi trademark registry (Law No. 21
   of 1957, as amended) for word mark in classes 9 (software) and 42 (SaaS/software services);
   (c) WIPO Global Brand Database; (d) EUIPO and USPTO if distributing outside Iraq/MENA.
3. Confirm no confusing similarity with "Filee", "Filey", "Filee Pro" or the Filee donut logo
   (clearance memo §5.8).
4. File the application in the chosen class(es); keep the search reports and filing receipts.
5. Apply the same name in: application label, `applicationId`, Play listing, icon, splash, privacy
   policy header and `docs/`. If the final name differs from "Sigha", the compliance texts that name
   the app must be updated in the same change.

**Builder-side screen (2026-11-19):** `docs/legal/name-screen-2026-11-19.md` records what public
sources show from this machine: no Google Play or US App Store app titled "Sigha", but three software
businesses use the name — SIGHA (Colombian HR software), Grupo Sigha (Brazilian ERP) and صياغة /
Siyagha (Arabic text-rewriting SaaS, also a Play app with +100 installs). The registry searches
(WIPO, Iraqi, EUIPO, USPTO) and the keep/change decision are still open; see §5 and §7 of that
record. This does not close B5.

**Evidence to close:** search reports with dates, filing numbers/status, IP counsel note if any
conflict is found.

---

## B6 — LGPL "Corresponding Source" hosting (the written offer made real)

**Why human:** the source archive must be published by the publisher at a URL the publisher controls,
and the mailbox in the offer must exist. The repository already contains the offer text
(`assets/legal/source-offer.txt`, shown in-app) and `NOTICE.md` §1.2, but both currently point to a
**placeholder** address and no archive exists. Until this is done, the LGPL-3.0 §4(d) written offer is
text-only and the LGPL-2.1 §6 source duty is unmet for a distributed binary.

**Steps:**
1. Create the mailbox `opensource@sigha.app` (or a different address the publisher will monitor) and
   have a human own it. Placeholders currently used: `opensource@sigha.app` in NOTICE.md §1.2 and
   `assets/legal/source-offer.txt`; `privacy@sigha.app` in the privacy policy. Every placeholder
   must be replaced with a real, monitored address or the offer is not usable.
2. Assemble the Corresponding Source for **exactly** the shipped build:
   - FFmpegKit fork source at the revision that produced `ffmpeg-kit-full-8.1.9.aar` (the fork's tag
     or commit — capture it, do not guess);
   - the FFmpeg source revision the fork built (FFmpeg 8.1 line; the exact commit must be read from the
     fork's build configuration — O4 in licence-compliance.md);
   - the build scripts and the configure flags actually used (no `--enable-gpl`, no
     `--enable-nonfree`; the LGPL build configuration);
   - a diff/note describing any fork modification (if unattested, a "no modifications by Sigha"
     statement);
   - a short `README` saying how to rebuild the `.so` files and how they install (`lib/<abi>/` in the
     APK), pointing at the in-app relink text.
3. Publish the tarball at a stable HTTPS URL the publisher controls (e.g. a release asset on a
   Sigha-controlled repository or `https://sigha.app/source/ffmpeg-kit-8.1.9.tar.xz`), and publish its
   SHA-256 next to it.
4. Update in the same change: `NOTICE.md` §1.2 (replace placeholder/TODO with the URL and the real
   mailbox), `assets/legal/source-offer.txt`, `docs/legal/licence-compliance.md` (§2 L4/L5/L9) and
   regenerate `assets/licenses/THIRD-PARTY-NOTICES.txt` from `NOTICE.md` (hash equality must hold).
5. Keep the offer valid for **three years** from each distribution date, including after the app is
   no longer sold; keep the archive reachable for that whole period. A note in the publisher calendar
   for every version bump: "refresh source URL + offer if FFmpegKit/FFmpeg revision changed".
6. Answer requests at the mailbox: deliver the archive for no more than the cost of distribution; for
   a download URL that cost is zero.

**Evidence to close:** live URL with tarball, published SHA-256, mailbox that receives and answers a
test message, copies of the updated NOTICE/offer text, and a dated screenshot of the offer as shown
in the app.

---

## B7 — Counsel sign-off on FFmpegKit LGPL obligations and the open licence questions

**Why human:** it is a legal judgement about a signed Android APK, and two questions (libiconv variant,
OpenH264 patent scope) cannot be answered from the artefact metadata.

**Questions to put to counsel (with the files to read):**
1. **LGPL-3.0 §4(d)/(e) for a signed APK.** The app is a Combined Work with the LGPL-3.0 FFmpegKit
   wrapper and LGPL-2.1+ FFmpeg/lib libraries, all loaded as separate `.so` files. Is the published
   Corresponding Source plus the written offer (B6) enough to satisfy §4(d)(0)? Does §4(e) require
   more than the relink instructions in `assets/legal/relink-instructions.txt` (Android signature
   requirement, no Installation Information keys of our own)? Do we need to publish the app-side
   relinkable material (e.g. the wrapper build config) or is the interface information sufficient?
2. **libiconv (O2).** The bundled text is the libiconv package's GPL-3.0 COPYING while the library is
   usually LGPL-2.1; `(libiconv_t)-1` appears in `libavcodec.so`. Is the shipped binary GPL-3.0 (which
   would change the analysis for the whole FFmpeg bundle) or LGPL-2.1? What evidence from the fork is
   needed?
3. **OpenH264 (O3).** The FFmpeg build includes the OpenH264 wrapper encoder. Does Cisco's
   royalty-free binary licence cover this third-party build, or must the H.264 encoder path be
   disabled/removed (or a Cisco binary licence obtained)?
4. **smart-exception-java (O1).** No licence file exists in the Maven artefacts and the presumed
   repository 404s. Counsel should record the position and, if the licence cannot be established,
   require removal/replacement.
5. **Notice set sufficiency:** is shipping `assets/licenses/` + `assets/legal/` + `NOTICE.md` (mirrored
   as an asset) enough "prominent notice" for §4(a), and does the in-app Licenses screen satisfy the
   "with each copy" requirement for a store-distributed app?
6. **App-side relink material:** confirm whether Android's signed-package model means a user cannot
   install a modified-library build of our app without uninstalling/re-signing, and whether the
   instructions must say so explicitly (they currently do).

**Deliverables:** a written opinion (paragraph per question, with the answer recorded in
`docs/legal/licence-compliance.md` §2/§5, statuses updated), and a decision on whether the shipped
stack is distributable as-is or must switch to the Media3-only path the clearance memo preferred
(memo Q3/C5).

**Evidence to close:** signed/datable counsel memo stored in the repository (`docs/legal/`), statuses
in licence-compliance.md flipped from "open — B7" to "met", and no placeholder left in the offer.

---

## How to close an item in this file

1. Do the human action, keep the evidence (URL, screenshot, receipt, memo).
2. Change the row in the table at the top to **CLOSED** with the date, and update the corresponding
   status in `docs/legal/licence-compliance.md`.
3. If the action changed any shipped text (offer, notice, policy), rebuild/regenerate the mirrored
   asset (NOTICE.md → `assets/licenses/THIRD-PARTY-NOTICES.txt`; policy → `assets/legal/privacy-policy.md`)
   and re-run the hash check.
4. If the action changed the distributed artefact, bump the version and re-run WS4 checks before the
   upload.
