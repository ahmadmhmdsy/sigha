# Publisher guide — from this tree to the closed test

**Written:** 2026-11-19 · **For:** the publisher (the only person who can do these steps) · **Companion:**
`docs/council/2026-11-18-next/council-decision.md`, `docs/legal/release-blockers.md`.

Nothing in this guide can be executed from the build machine. Each step lists what to do, the exact
values to supply, and the evidence that closes it. Work top to bottom: the order is the critical path.

> **Rule zero:** do not upload anything until `scripts/verify.ps1 -ReleaseCandidate` passes (no
> placeholder left in shipped legal text) **and** the physical-device checklist is recorded against the
> tagged build. The 14-day clock cannot be paused.

## 0. The one-screen version

| # | Step | Blocks | Typical time |
|---|---|---|---|
| 0 | Get an arm64 Android 14+ phone (or remote real-device access) | everything | 1 day |
| 1 | Play developer account (personal) | upload | 1–3 days (verification) |
| 2 | Name screen (B5) | upload (package name is permanent) | 1–2 days |
| 3 | Upload key + Play App Signing (B1) | upload | 1 hour |
| 4 | Privacy policy at a public HTTPS URL (B2) | upload | 1 day |
| 5 | LGPL source + mailbox (B6) | first tester install | 1 day |
| 6 | Counsel packet (B7) | first tester install | 1–3 weeks |
| 7 | Play Console declarations (data safety, content rating, FGS + demo video) | upload | 1 day |
| 8 | Listing assets (icon, feature graphic, 8×2 screenshots) | upload | 1 day |
| 9 | Build the AAB from the tagged commit, upload, 15 testers, 14 days | production | 15 days |
| 10 | Production access application | public release | days |

## Status board (updated 2026-11-22)

The build lane is done and verified; everything below is either the publisher's or blocked on the
publisher. Step numbers match the table above.

| Step | State | What is actually done | What remains |
|---|---|---|---|
| 0 — arm64 phone | **OPEN (publisher)** | Builder prepared the Step Zero script and the device checklist; a reminder is armed | Get the phone / remote device, run §Step 0, record model + Android version + ABI |
| 1 — Play account (personal) | **OPEN (publisher)** | — | Sign-up, $25, identity verification, 2-step |
| 2 — name screen | **BUILDER PART DONE** | Public-source screen recorded 2026-11-19 in `docs/legal/name-screen-2026-11-19.md` (no exact Play/App Store app named Sigha; three software businesses use the name) | Registry searches + the keep/change decision (yours or counsel's) in release-blockers B5 |
| 3 — upload key + Play App Signing | **OPEN (publisher)** | Key commands, backup rules and build wiring are in §Step 3 | Generate/back up the key, enrol Play App Signing, build the upload-key AAB |
| 4 — privacy policy URL (B2) | **OPEN (publisher)** | Policy text exists in EN + AR with placeholders for name/contact/date; hosting requirements are in §Step 4 | Supply the legal values, host the page, paste the URL |
| 5 — LGPL source + mailbox (B6) | **OPEN (publisher)** | `license-compliance.md` and `NOTICE.md` describe exactly what to publish and how to rebuild | Own a domain, create `opensource@`, publish the source tarball + SHA-256 |
| 6 — counsel (B7) | **OPEN (publisher)** | Six questions written in `docs/legal/release-blockers.md` §B7; packet list in §Step 6 | Send to counsel, or sign a written risk acceptance |
| 7 — Play declarations | **BUILDER DRAFT DONE** | Paste-ready data-safety, content-rating, audience/ads and FGS answers + demo-video script in `docs/product/play-declarations-2026-11-22.md` | Fill the live form after Step 4's URL exists; record the demo video on the tagged build |
| 8 — listing assets | **TEXTS DONE, ASSETS OPEN** | Titles/short/full descriptions, shot list in `docs/product/store-listing.md` and `aso-plan-2026-11-19.md` | Icon, feature graphic, 8 EN + 8 AR screenshots from the upload build |
| 9 — upload + 14 days | **OPEN (publisher)** | Tester script and 15-tester mix are written | AAB upload, 15 testers, 14 unchanged days |
| 10 — production access | **OPEN (publisher)** | — | Apply after 14 days |

**Critical path:** Step 0 → Step 1 → Step 2. Nothing in the build lane needs attention until Step 4's
final values and Step 5's mailbox exist; then the builder replaces the 21 placeholders, re-runs
`-ReleaseCandidate` and hands over the tagged AAB.

**Build-lane state (why the table says "done"):** full gate green on the final 0.2.1-hardening tree
(`docs/qa/evidence/verify-20261121-234013.json`, 149 tests), silent-video fix and batch/share flows
verified on the API 36 x86_64 emulator (`docs/qa/verification.md` §11), and the UX/competitive
research recorded in `docs/research/2026-11-21-ux-retention.md` and
`docs/research/2026-11-21-competitive-gap.md`. The 21 placeholders, arm64 device, upload key and Play
account are unchanged — they are the publisher's lane, not build defects.

---

## Step 0 — a real arm64 Android 14+ phone (blocking)

The app ships `arm64-v8a`; every recorded device run so far is a debug x86_64 emulator. The release
artefact (R8-minified, arm64) has never executed. Nothing below can start without a device that runs it.

**Options, best first:**

1. **Borrow an Android 14+ phone** (any recent Samsung/Pixel/Xiaomi — check `ro.product.cpu.abi` = `arm64-v8a`).
2. **Buy a used one** (a 2023+ A-series is enough; no SIM needed — Wi-Fi is fine).
3. **Remote real-device lab:** Samsung Remote Test Lab (free, needs a Samsung account), BrowserStack
   App Live (paid), or Firebase Test Lab physical devices (Robo scripts only — it cannot judge RTL or
   read the Arabic instructions, so it is not enough for the checklist alone).

**Record:** model, Android version, ABI, date. Commands:

```
adb shell getprop ro.product.cpu.abi     # must be arm64-v8a
adb shell getprop ro.build.version.release  # must be 14+
adb install -r app-arm64-v8a-release.apk
```

**Step Zero (30–60 min, before any rebuild):** cold start; JPG→PNG; PDF→TXT; scanned PDF→TXT without
the OCR pack (expect the install instruction); share-in; one 5-file batch backgrounded for 2 minutes.
If it crashes or produces nothing, tell the builder — the rebuild becomes a rescue.

---

## Step 1 — Play developer account (personal)

1. Go to `https://play.google.com/console/signup`, sign in with the Google account that will own the app.
2. Choose **Personal** account type (the app will be published under your name).
3. Pay the one-time $25 fee; complete identity verification (government ID + address) and enable 2-step
   verification.
4. Fill the developer name and contact email; accept the Developer Distribution Agreement.
5. Record: account type, developer ID, date verified.

**Why it matters:** personal accounts created after 13 Nov 2023 must run a **closed test with at least 12
testers opted in for 14 continuous days** and then apply for production access. Plan 15 testers.
Verification can take days — start it today.

---

## Step 2 — name screen (B5)

The package name (`com.sigha.converter`) and listing title are effectively permanent after the first
upload. This is a **screen + written decision** before upload; a filing can follow, but a conflict found
later is expensive.

| Search | What to look for |
|---|---|
| Google Play + App Store | "Sigha", "صيغة", and converter apps with similar names |
| WIPO Global Brand Database | word mark "Sigha" / "صيغة" in classes 9 and 42 |
| Iraqi trademark registry (Law 21/1957, classes 9/42) | existing filings |
| EUIPO / USPTO (if distributing outside Iraq/MENA) | same |
| Clearance memo §5.8 | "Filee", "Filey", "Filee Pro" and the Filee donut logo |

Record each query: source, date, result, link. Then write the decision (keep "Sigha" / change) in
`docs/legal/release-blockers.md` B5. A builder-side public-source screen (2026-11-19) is already
recorded in `docs/legal/name-screen-2026-11-19.md`: no exact Play/App Store app named Sigha, but
three software businesses use Sigha/Siyagha, and the registry searches and decision remain yours or
counsel's. If the name changes, the package name, label, listing, icon and
every legal text change **before** the first upload, in one change.

The builder can run public web searches and record what is visible; the legal decision and any filing
are yours or counsel's. This is not legal advice.

---

## Step 3 — upload key + Play App Signing (B1)

```
keytool -genkeypair -v -keystore sigha-upload.jks -alias upload \
  -keyalg RSA -keysize 2048 -validity 10000
```

1. Use a strong password; store the keystore and password in a password manager **and** on two offline
   media. Never commit them.
2. Record the certificate fingerprint: `keytool -list -v -keystore sigha-upload.jks`.
3. In Play Console: **Setup → App signing** — enrol the app in Play App Signing; Play keeps the
   app-signing key, yours becomes the upload key.
4. On your machine, put the keystore path/passwords in `keystore/keystore.properties` (git-ignored).
   The repo's `keystore/` is the **QA** key — never use it for Play.
5. Build the AAB with the upload key: `./gradlew :app:bundleRelease` (splits are already off for the
   bundle). Verify the signer, then record versionCode + SHA-256.
6. If the key is lost: Play Console → App signing → **Request upload key reset** (takes days).

**Evidence:** fingerprint recorded, backups verified, signed AAB accepted by Play.

---

## Step 4 — privacy policy at a public HTTPS URL (B2)

1. Supply the final values: publisher legal name, privacy contact address (e.g.
   `privacy@your-domain` — note `sigha.app` is already taken by another product), effective date. The builder replaces them in `docs/legal/privacy-policy.md`, `app/src/main/assets/
   legal/privacy-policy.md` and the Arabic copy, then rebuilds.
2. Host the rendered page at a URL you control: HTTPS, no login, no redirect, stable, readable on a
   phone, and a **webpage** — not a PDF or a download. A static host (GitHub Pages, Cloudflare Pages)
   on a domain you own is enough.
3. The published text must be **identical** to the in-app asset; when one changes, the other changes in
   the same commit.
4. Paste the URL in Play Console → App content → Privacy policy, and in the listing.
5. Confirm from a clean browser profile with no login that it loads and shows the effective date and the
   OCR-pack download disclosure.

**Evidence:** live URL, screenshot, diff/hash note that page = asset, console screenshot.

---

## Step 5 — LGPL source + mailbox (B6) — before the first tester install

LGPL duties attach to **each copy conveyed**; the first tester install is a conveyance.

1. Create and monitor `opensource@` on a domain you own (not `sigha.app` — it belongs to another
   product). Replace it everywhere it appears.
2. Assemble the Corresponding Source for the **exact** shipped build:
   - FFmpegKit fork source at the revision that produced `ffmpeg-kit-full-8.1.9.aar`;
   - the FFmpeg source revision the fork built (read it from the fork's build config — O4 in
     `docs/legal/licence-compliance.md`; do not guess);
   - the build scripts and configure flags actually used (LGPL build: no `--enable-gpl`, no
     `--enable-nonfree`);
   - a note on fork modifications (or "no modifications by Sigha");
   - a README: how to rebuild the `.so` files and how they install (`lib/<abi>/`), pointing at the
     in-app relink text.
3. Publish the tarball at a stable HTTPS URL you control (release asset or `https://your-domain/source/`),
   with its SHA-256 next to it. Keep it reachable for **three years** from each distribution date.
4. Update in one change: `NOTICE.md` §1.2, `app/src/main/assets/legal/source-offer.txt`, `docs/legal/
   licence-compliance.md`; regenerate `assets/licenses/THIRD-PARTY-NOTICES.txt` (hash equality is
   checked by the gate).
5. Answer requests at the mailbox (for a download URL the cost is zero).

**Evidence:** live URL + tarball, published SHA-256, a test message answered at the mailbox, updated
texts, screenshot of the in-app offer.

---

## Step 6 — counsel packet (B7)

Send counsel: `docs/legal/licence-compliance.md`, `NOTICE.md`, `app/src/main/assets/legal/
relink-instructions.txt` and `source-offer.txt`, plus the six questions already written in
`docs/legal/release-blockers.md` §B7 (LGPL-3.0 §4(d)/(e) for a signed APK; libiconv; OpenH264;
smart-exception-java; notice sufficiency; app-side relink material).

Deliverable: a written opinion, one paragraph per question, stored in `docs/legal/`, with the statuses
in `licence-compliance.md` flipped. If counsel cannot be engaged before the test, record a **written
publisher risk acceptance** (date, scope, signature) — silence is not acceptance.

---

## Step 7 — Play Console declarations (B3 part 1)

Use these answers; they match the build and the policy.

| Section | Answer |
|---|---|
| Data safety | No data collected, no data shared; no analytics/ads/crash SDK; policy URL from Step 4 |
| Encryption in transit | Not applicable — the app transmits no user data; the only request is the user-initiated OCR pack download over HTTPS |
| Content rating (IARC) | Complete the questionnaire truthfully; no ads, no user-generated content, not primarily for children |
| Target audience | General, 13+ (not primarily children) |
| App access | All functionality is available without restrictions (no login) |
| Ads | No ads |
| News / COVID / government app | No / No / No |
| Foreground-service declaration | **Required** (targetSdk 36, `dataSync`): choose "Local processing: Other", write the description and user impact, link a 30–60 s **demo video** showing a conversion with the progress notification |
| Privacy policy | URL from Step 4 |

The demo video can be an unlisted YouTube link. Keep every answer consistent with the policy — a
mismatch is a review finding, not a warning.

**Paste-ready packet:** `docs/product/play-declarations-2026-11-22.md` contains the exact
data-safety answers, the content-rating draft, the FGS declaration text, the demo-video script and a
pre-submission consistency checklist. Use it with the live form; if a field label differs, map the
answer onto the live form.

---

## Step 8 — listing assets

Texts are final in `docs/product/aso-plan-2026-11-19.md` §4–§8 and mirrored in
`docs/product/store-listing.md` §2–§4 (title ≤30, short ≤80, full ≤4000, what's new; localized Arabic
title; outcome-first shot order). Assets to produce:

1. **App icon** 512×512 PNG (32-bit, no alpha requirement).
2. **Feature graphic** 1024×500 PNG/JPG, no text-only.
3. **8 phone screenshots per language** (EN + AR = 16), 16:9 or 9:16, min 320 px, captured from the
   **upload build** after the tag; the shot list and order are `docs/product/aso-plan-2026-11-19.md`
   §8 (shot 1 = one image → PDF result, not a picker). The Arabic set is entirely Arabic UI. No
   unimplemented feature may appear.
4. Record device model, Android version and build SHA for the captures.

---

## Step 9 — upload and the 14 days

1. Build the AAB from the **tagged** commit with the upload key; record versionCode and SHA-256.
   Before the track goes live, install this **upload-key-signed** AAB once on the borrowed arm64
   Android 14+ phone and walk the Step Zero script — the QA-signed APK is a different identity, so
   this is the first run of the shipped artefact (Expert round 2). Record the **foreground-service
   demo video** in the same pass on this tagged build (Policy round 2); a later re-shoot costs a
   second pass and an inconsistent artefact.
2. Play Console → **Testing → Closed testing** → create a track → upload the AAB → release notes
   (from `CHANGELOG.md`) → save and roll out.
3. Add testers by email list or Google Group: recruit **15** (12 needed; dropouts do not break the
   floor). Arabic-first 6, English 3, Android 8–9 2, Android 12− 2, low-storage 2 — see
   `docs/product/store-listing.md` §8.
4. Send testers the opt-in link and the tester script (`docs/qa/tester-instrument.md`); ask for one
   line per task plus the exit questions.
5. Keep the build installed and unchanged for **14 consecutive days** — the clock counts opted-in days.
6. Apply for production access in Play Console after the 14 days, describing the feedback loop.

**Do not** update the app during the window: the evidence (vitals, script results, screenshots) names
one build.

---

## Step 10 — after access

Promote the same build to production with a staged rollout (20% → 50% → 100%), watch vitals, keep the
closed track for future builds. Then start 0.2.2: structural composition, bidi sweep, identity layer,
per-app language (see the council decision).

---

## Appendix A — values you must supply

| Value | Where it lands | Placeholder today |
|---|---|---|
| Publisher legal name | privacy policy (EN+AR), NOTICE, listing | "legal name to be completed" |
| Privacy contact email | privacy policy (EN+AR) | `privacy@sigha.app` (placeholder; domain not the publisher's — see B2) |
| Privacy policy URL | Play Console, listing | — |
| LGPL mailbox | NOTICE.md §1.2, `source-offer.txt` | `opensource@sigha.app` (placeholder; domain not the publisher's — see B6) |
| LGPL source URL + SHA-256 | NOTICE.md, `source-offer.txt`, licence-compliance | — |
| Upload key fingerprint | release notes, this guide | — |
| Play account type + developer ID | release notes | — |
| Name decision (keep/change) | release-blockers B5 | — |
| Counsel opinion or risk acceptance | `docs/legal/` | — |
| Device model + Android version | `docs/qa/verification.md` | — |
| 15 tester emails | Play Console | — |

## Appendix B — evidence to keep

Per step: account verification screenshot; search log; key fingerprint + backup proof; live policy URL
+ page screenshot; source URL + SHA-256 + mailbox test; counsel memo; declarations confirmation;
listing asset files; AAB SHA-256 + versionCode; tester list with opt-in dates; production-access
decision. Paste paths into `docs/qa/verification.md` or a dated report under `docs/qa/`.

## Appendix C — what the builder has done in parallel

Done on 2026-11-19 (full gate `verify-20261119-004710.json`, legibility captures in
`docs/qa/verification.md` §7.2):

- batch position, current file and Cancel in the pinned bar; the saved destination in the completion
  summary; actions that name the target, with an empty-target fallback;
- thrown `NO_TEXT_LAYER` / `MODEL_PACK_MISSING` / password / corrupt-PDF failure kinds;
- `ConversionService.onTimeout` (the Android 15 foreground-service budget);
- five Arabic copy corrections, and the unavailable upscaling card removed;
- gate hardening: `-ReleaseCandidate` placeholder gate, EN↔AR key/placeholder/plural parity, and the
  release signer recorded in the evidence JSON.

Added 2026-11-21/22 (full gate `verify-20261121-234013.json`, emulator evidence
`docs/qa/verification.md` §11):

- silent-video fix: media probe hides audio targets, typed `NO_AUDIO_TRACK`/`NO_VIDEO_TRACK`;
- per-output publishing (a failed page no longer discards the successful ones), bounded JSON/page
  limits, pack-resume verification, foreground-service stop races, storage-denial Settings action;
- Share action on the workbench result row and a tap target on the progress notification;
- +12 JVM tests (149 total) and the 2026-11-21 UX/retention + competitive-gap research notes;
- Play declarations packet drafted: `docs/product/play-declarations-2026-11-22.md`.

Still blocked, and not the builder's to unblock: the 21 placeholder markers in the shipped legal
assets (your Step 4 values, Step 5 mailbox), the arm64 device (Step 0), and everything Play Console.
The tree is tagged only when every placeholder is gone; the device checklist runs on that tag; the
AAB is built from the same tag.