# Clearance memo — Filee 2.5.0 → new Android offline file converter (working title: TBD)

**Memo status:** draft for the Team Lead and for professional review. It is an internal decision record, not a legal opinion and not a fatwa.
**Date and place:** 2026-11-16 (Asia/Baghdad), prepared by the "legal-clearance" teammate of the build team.
**Applies to:** the public KnifeLemon/Filee repository at the snapshot taken 2026-11-16 (release v2.5.0, repo pushed 2026-11-16T03:43:28Z) and to the planned Android application described in the shared brief.
**Disposition:** **CLEARED WITH CONDITIONS** for the clean-room path (read MIT-licensed docs for facts, write original Kotlin, ship permissive-licensed components only). **BLOCKED** for reusing Filee expression, media, name, logo or trade dress, and for bundling GPL/AGPL engines. No breach occurred, so there is no SEALED or DESTROY corpus and no tawbah/remediation programme.
**Not a lawyer, not a mufti.** Recommend an Iraqi lawyer and a qualified scholar review this before any commercial launch. See section 7.

---

## 0. Plan under review (clearance-skill workflow step 0)

We will read public, MIT-licensed documentation and metadata from the Filee repository (docs, README, changelog, third-party notices, LICENSE; source code only if a specific fact cannot be learned otherwise), extract **facts and ideas** into a written specification, and then write our **own original Kotlin/Compose Android application** that performs offline file conversion with an optional, user-initiated download of on-device models. We will not copy Filee code, documentation prose, images, icons, screenshots, the "Filee" name or logo, or its donut trade dress. We will not bundle GPL/AGPL engines in the APK; we will use permissive-licensed components and, only if strictly needed, an LGPL-3.0 FFmpeg build under a compliance plan. Material is stored under docs/research/filee (8 text files from the public repository) and docs/legal/sources (licence, law and policy snapshots fetched 2026-11-16); it is read by the build team and the Lead, and is not published as a product. Nothing is executed, decompiled or installed.

---

## 1. Scope and status

### 1.1 Target and how it was obtained

| Item | Verified fact |
|---|---|
| Project | KnifeLemon/Filee — "Free, open-source file converter" desktop app for Windows/Linux/macOS, .NET/Avalonia, C#. Homepage https://filee.sh |
| Owner / licensor | GitHub user KnifeLemon; copyright line in LICENSE: "Copyright (c) 2026 Myeonggu Kim (KnifeLemon) and Filee contributors" |
| Repository status | public, not archived, not disabled; created 2026-09-29T20:05:15Z; last push 2026-11-16T03:43:28Z; 28,025 stars, 1,599 forks at fetch |
| Latest release | v2.5.0, published 2026-11-16T08:52:44Z (GitHub API) |
| Licence | MIT (GitHub licence API: spdx_id MIT, blob sha bc70a7a942baa9a8f57d918bd882134a66aaa809); local LICENSE text identical after normalisation |
| Format count | README: "270 formats"; repo description: "Convert 263 formats offline". Minor inconsistency in the source, not legally material |
| How obtained | Public HTTPS downloads from github.com / raw.githubusercontent.com by the research teammate on 2026-11-16 between 23:04 and 23:05 (Baghdad). No GitHub account, token, private access or click-through EULA was involved. |
| Material held | 8 text files only (≈530 KB): LICENSE, README.md, CHANGELOG.md, THIRD-PARTY-NOTICES.md, docs ARCHITECTURE/ENGINES/FORMAT-RULES/ADDING-A-CONVERTER, global.json, plus today's LICENSE/README/LOGO.md/API-metadata snapshots. No .cs source, no binaries, no installer, no media, no screenshots. |
| Material NOT held | Filee source code, compiled binaries, installers, engine downloads, images, icons, fonts, screenshots, website assets, user data. Nothing from Filee was executed or decompiled. |

### 1.2 Licence and EULA status (the contract layer)

- The repository is licensed under **MIT**. The GitHub root-contents listing (fetched 2026-11-16) contains LICENSE, README.md, THIRD-PARTY-NOTICES.md, CHANGELOG.md, CODE_OF_CONDUCT.md, CONTRIBUTING.md, SECURITY.md, global.json, and the src, docs, build, installer, tests, skills directories — **there is no EULA, terms-of-use, CLA or trademark-policy file**, and docs/LOGO.md contains no separate brand licence statement.
- **No EULA was accepted by us.** We never downloaded or ran the Filee installer or portable build. The MIT licence is a bare copyright licence and requires no acceptance click. The installer distributed through Filee releases may present its own notices to a user who runs it; that is irrelevant to us because we did not run it.
- GitHub's own Terms of Service apply to using github.com: section 5 grants a licence to view and fork public repositories **through the Service**, and says "You may grant additional rights by adopting a license"; section 8 says the Terms "do not restrict lawful access to or use of the contents of public repositories by third parties". The repository's MIT LICENSE is exactly that additional grant, and it — not mere public visibility — is the basis of our rights.
- What MIT permits (verified from LICENSE): use, copy, modify, merge, publish, distribute, sublicense, sell, subject to one condition — the copyright notice and permission notice must be included in all copies or substantial portions of the Software. There is no warranty. MIT does **not** grant trademark rights, and it does not relicense third-party components that Filee itself bundles or downloads.
- Third-party components: Filee's own MIT licence does not extend to the engines and libraries listed in THIRD-PARTY-NOTICES.md (e.g. 7-Zip, LibreOffice, Pandoc, Ghostscript, calibre, FFmpeg, upscayl-bin). Those are facts we may learn, but each remains under its own licence; we will not copy them from Filee.

### 1.3 Jurisdictions

| Jurisdiction | Basis and status |
|---|---|
| **Iraq (primary)** | Copyright Law No. 3 of 1971 "with its Amendments", English translation by the Ministry of Justice, Official Gazette Department, Translation Division (2020), WIPO Lex record 10345. Trademarks and Trade Names Law No. 21 of 1957 as amended up to Law No. 9 of 2010, WIPO Lex record 21933. Iraq is not a WTO member: its WTO accession Working Party was established on 13 December 2004 and Iraq does not appear in the WTO members list fetched 2026-11-16. Berne membership could not be verified from WIPO's official parties page (404); WIPO Lex shows no Berne notification for Iraq and secondary sources state Iraq is not a Berne member — see Appendix B. This is **not decisive** because Article 49 of Law 3/1971 extends protection to foreign authors' works. |
| **Google Play (contractual)** | Developer Distribution Agreement: Google entities include Google LLC (Delaware), Google Ireland Limited and Google Commerce Limited (Ireland) and Google Asia Pacific Pte. Ltd. Section 16.8: claims are "governed by the laws of the State of California, excluding California's conflict of laws provisions" with exclusive jurisdiction in Santa Clara County, California. Play Developer Program Policies and the Data safety/User Data rules apply in addition. |
| **Other distribution markets** | **UNKNOWN / Lead decision.** No market-by-market clearance has been done. If the app is offered in the EU/UK, GCC states, US or elsewhere, consumer, privacy, trademark and product-safety rules add obligations. Counsel review per market is required; see conditions C11. |
| **Halal frame** | Reasoning aid only (section 5.7), not a fatwa. A scholar should rule before sale. |

### 1.4 Clean-case statement — what was NOT done

No decompilation, disassembly or tracing; no installer, portable build or engine execution; no crack, keygen, dongle or licence bypass; no scraping; no download of images, icons or media; no server contact beyond ordinary page/file fetches of publicly documented material. No EULA was accepted. Because no protected expression was copied and no accepted condition was breached, **there is no SEALED material to quarantine and no DESTROY corpus to hash and delete**, and no tawbah/remediation sequence is triggered (contrast the Al-Ameen V9 case in the skill reference). If any teammate later decompiles Filee, copies its code, or downloads its media, they must stop and this gate must be re-run.

---

## 2. Sources

All URLs were fetched by the team on **2026-11-16** (Asia/Baghdad) with PowerShell Invoke-WebRequest or curl, and saved in docs/legal/sources as plain text/HTML/JSON/PDF. "Verified" means the quoted fact was read in the fetched copy, not inferred.

### 2.1 Filee itself

| # | Source | Date fetched | What it says (verified) |
|---|---|---|---|
| F1 | https://github.com/KnifeLemon/Filee (page) | 2026-11-16 | Public MIT project; owner KnifeLemon; description "Free, open-source file converter. Convert 263 formats offline..."; homepage https://filee.sh |
| F2 | https://api.github.com/repos/KnifeLemon/Filee | 2026-11-16 | license.spdx_id = MIT; created 2026-09-29T20:05:15Z; pushed 2026-11-16T03:43:28Z; archived false; 28,025 stars; 1,599 forks; open issues 164 |
| F3 | https://api.github.com/repos/KnifeLemon/Filee/license | 2026-11-16 | MIT License; blob sha bc70a7a942baa9a8f57d918bd882134a66aaa809; size 1105 |
| F4 | https://raw.githubusercontent.com/KnifeLemon/Filee/main/LICENSE | 2026-11-16 | MIT text; copyright "2026 Myeonggu Kim (KnifeLemon) and Filee contributors"; permission sentence and notice condition (quoted below) |
| F5 | https://raw.githubusercontent.com/KnifeLemon/Filee/main/README.md | 2026-11-16 | "270 formats, no other software"; "Nothing is uploaded. Filee works offline"; sponsor section; MIT badge; no trademark or brand-policy statement |
| F6 | https://api.github.com/repos/KnifeLemon/Filee/releases/latest | 2026-11-16 | Latest release v2.5.0, published 2026-11-16T08:52:44Z, Windows/Linux/macOS assets |
| F7 | https://api.github.com/repos/KnifeLemon/Filee/contents/ | 2026-11-16 | Root: LICENSE, README.md, THIRD-PARTY-NOTICES.md, CHANGELOG.md, docs, src, build, installer, skills, tests; no EULA or terms file |
| F8 | https://raw.githubusercontent.com/KnifeLemon/Filee/main/docs/LOGO.md | 2026-11-16 | Logo is hand-made; source artwork at src/Filee.App/Assets/Icons/filee.svg; no separate brand/licence statement |
| F9 | Local: docs/research/filee/ (8 files) | 2026-11-16 23:04–23:05 | Facts on engines, architecture (FormatRegistry, RoutePlanner/Dijkstra, donut UI), format rules, third-party notices, changelog; MIT repository documentation |

### 2.2 GitHub terms

| # | Source | Date fetched | What it says (verified) |
|---|---|---|---|
| G1 | https://docs.github.com/en/site-policy/github-terms/github-terms-of-service (content fetched from https://raw.githubusercontent.com/github/docs/main/content/site-policy/github-terms/github-terms-of-service.md) | 2026-11-16 | §5: public repositories may be viewed and forked through the Service; "You may grant additional rights by adopting a license." §8: Terms do not restrict lawful access to or use of public-repository contents by third parties |

### 2.3 Iraq law (primary statutes)

| # | Source | Date fetched | What it says (verified) |
|---|---|---|---|
| L1 | https://www.wipo.int/wipolex/en/legislation/details/10345 | 2026-11-16 | WIPO Lex record: Law No. 3 of 1971 on Copyright, Iraq; entry into force 21 January 1971; English and Arabic PDFs available |
| L2 | Iraq Copyright Protection Law No. 3 of 1971, English PDF (signed WIPO Lex URL for iq008en_1.pdf; saved as sources/iraq-law-3-1971-en.pdf, 21 pages, text extracted) | 2026-11-16 | Translation by Ministry of Justice (2020), "with its Amendments". Articles 1, 2(2), 3, 4, 8, 13, 14, 44, 45, 49 read and quoted below |
| L3 | https://www.wipo.int/wipolex/en/legislation/details/21933 | 2026-11-16 | Law No. 21 of 1957 on Trademarks and Trade Names (as amended up to Law No. 9 of 2010); amended up to 15 February 2010; subject matter trademarks, GIs, trade names |
| L4 | https://www.wipo.int/wipolex/en/treaties/ShowResults?code=IQ&search_what=N | 2026-11-16 | Iraq's WIPO treaty notifications include WIPO Convention (1975), Paris/Stockholm (1975), Singapore (2014), PCT (2022), Marrakesh (2024); **no Berne notification listed** |
| L5 | https://www.wto.org/english/thewto_e/acc_e/a1_iraq_e.htm | 2026-11-16 | Iraq accession: Working Party established 13 December 2004; accession process ongoing |
| L6 | https://www.wto.org/english/thewto_e/whatis_e/tif_e/org6_e.htm | 2026-11-16 | WTO members list; Iraq is not present (corroborates non-membership) |

### 2.4 Sharīʿah sources

| # | Source | Date fetched | What it says (verified) |
|---|---|---|---|
| S1 | https://api.alquran.cloud/v1/ayah/5:1/editions/quran-uthmani,en.sahih | 2026-11-16 | Qur'an 5:1, Uthmani text and Saheeh International translation: "O you who have believed, fulfill [all] contracts." |
| S2 | https://sunnah.com/abudawud:3594 | 2026-11-16 | Sunan Abi Dawud 3594: "Muslims are on (i.e. stick to) their conditions." Grade shown: Hasan Sahih (Al-Albani) |
| S3 | https://iifa-aifi.org/en/54157.html | 2026-11-16 | International Islamic Fiqh Academy, Resolution No. 43 (5/5) on Moral Rights, 15 December 1988: business names, trade marks, literary production and invention are rights of their holders with tradable financial value, recognised by Sharīʿah and not permissible to violate; copyrights and patent rights are Sharīʿah-protected rights |
| S4 | Skill references: legal-framework.md, sharīʿah-framework.md, artifact-classification.md, case-al-ameen-v9.md | Read 2026-11-16 | Baseline reasoning and the Al-Ameen V9 failure case; the reference itself says it is not legal advice and that local counsel is needed. One Iraq article attribution could not be verified — Appendix B |

### 2.5 Third-party licences (the components our own build may use)

| # | Source | Date fetched | What it says (verified) |
|---|---|---|---|
| T1 | https://www.rarlab.com/license.htm | 2026-11-16 | WinRAR EULA includes the unRAR restriction sentence quoted below |
| T2 | https://raw.githubusercontent.com/ip7z/7zip/main/DOC/License.txt | 2026-11-16 | 7-Zip RAR code is "GNU LGPL with unRAR license restriction"; full restriction text: unRAR sources cannot be used to re-create the RAR compression algorithm; distribution permitted if the no-archiver statement is kept |
| T3 | https://ffmpeg.org/legal.html | 2026-11-16 | FFmpeg is LGPL-2.1-or-later, GPL if GPL parts are enabled; compliance checklist: compile without --enable-gpl and --enable-nonfree, use dynamic linking, distribute corresponding source |
| T4 | https://github.com/arthenica/ffmpeg-kit ; https://raw.githubusercontent.com/arthenica/ffmpeg-kit/master/LICENSE ; .../README.md | 2026-11-16 | Repository **archived** (last push 2026-07-02), licence LGPL-3.0; "Licensed under LGPL 3.0 by default, GPL v3.0 if GPL licensed libraries are enabled"; -gpl bundles are GPL-3.0 |
| T5 | https://www.gnu.org/licenses/lgpl-3.0.txt | 2026-11-16 | LGPL-3.0 section 4 Combined Works: notice, licence copy, relink options 4(d)(0)/(1), Installation Information 4(e); section 5 Combined Libraries |
| T6 | https://www.gnu.org/licenses/agpl-3.0.txt | 2026-11-16 | AGPL-3.0 section 13: network-interaction users must be offered Corresponding Source |
| T7 | https://www.gnu.org/licenses/gpl-3.0.txt | 2026-11-16 | GPL-3.0 full text (reference for excluded components) |
| T8 | https://www.apache.org/licenses/LICENSE-2.0.txt | 2026-11-16 | Apache-2.0; section 4(d): NOTICE-file attribution must be included in derivative distributions |
| T9 | https://www.mozilla.org/en-US/MPL/2.0/ | 2026-11-16 | MPL-2.0 file-level copyleft (LibreOffice's licence) |
| T10 | https://creativecommons.org/licenses/by-nc/4.0/legalcode.txt | 2026-11-16 | "NonCommercial means not primarily intended for or directed towards commercial advantage or monetary compensation." |
| T11 | https://github.com/tesseract-ocr/tessdata_fast (README) | 2026-11-16 | "All data in the repository are licensed under the Apache-2.0 License" |
| T12 | https://github.com/tesseract-ocr/tesseract (LICENSE) | 2026-11-16 | Apache-2.0 text |
| T13 | https://github.com/openai/whisper (LICENSE) | 2026-11-16 | MIT (OpenAI) |
| T14 | https://github.com/ggerganov/whisper.cpp (LICENSE) | 2026-11-16 | MIT (ggml authors) |
| T15 | https://github.com/xinntao/Real-ESRGAN (LICENSE) | 2026-11-16 | BSD-3-Clause; clause 3: names may not be used to endorse or promote products without permission |

### 2.6 Google Play / Android policy

| # | Source | Date fetched | What it says (verified) |
|---|---|---|---|
| P1 | https://play.google/developer-distribution-agreement.html | 2026-11-16 | §16.8: governed by California law, exclusive jurisdiction Santa Clara County; Google entities include Google LLC, Google Ireland Ltd, Google Commerce Ltd, Google Asia Pacific |
| P2 | https://support.google.com/googleplay/android-developer/answer/9888072 | 2026-11-16 | Intellectual Property policy: prohibits infringing copyright, trademark, patents; app and store-listing content must be our own or licensed |
| P3 | https://support.google.com/googleplay/android-developer/answer/17006354 | 2026-11-16 | Deceptive Behavior + Misrepresentation: metadata must precisely reflect functionality; asset downloads must be prompted with size disclosed first; no false affiliation or "official" claims |
| P4 | https://support.google.com/googleplay/android-developer/answer/10467955 | 2026-11-16 | All files access (MANAGE_EXTERNAL_STORAGE): only when SAF/MediaStore cannot work; permitted uses list file managers, backup/restore, antivirus, document management; Permissions Declaration Form required |
| P5 | https://support.google.com/googleplay/android-developer/answer/10144311 | 2026-11-16 | User Data policy: transparency about access, collection, use, handling and sharing of user data |
| P6 | https://support.google.com/googleplay/android-developer/answer/10787469 | 2026-11-16 | Data safety form mandatory for every app, including apps that collect nothing; privacy-policy link required |
| P7 | https://developer.android.com/google/play/requirements/target-sdk | 2026-11-16 | From 31 August 2026 new apps and updates must target Android 16 (API level 36) or higher; existing apps must target API 35+ to stay available to new users |
| P8 | https://support.google.com/googleplay/android-developer/answer/9888170 | 2026-11-16 | Permissions and APIs that access sensitive information: policy summary, minimum-scope expectations |

### 2.7 Name screen (not a legal search)

| # | Source | Date fetched | Finding |
|---|---|---|---|
| N1 | Web search of Google Play/public web | 2026-11-16 | "Tabdeel - Exchange Goods" exists on Google Play (Alphatech Solutions) and the App Store; "Tahweel" exists on Google Play (Tahweel Financial Services; app.tahweel.io); a quick screen found no direct Play app named "Sigha", but that is not proof of availability |

---

## 3. Acts assessed

Legend: KEEP/NEVER verdicts follow the clearance-skill vocabulary. "Ḥalāl basis" is reasoning, not a fatwa.

| # | Act | Legal basis | Ḥalāl basis | Verdict |
|---|---|---|---|---|
| A1 | Reading the public MIT repository (LICENSE, README, docs, metadata) | MIT grant plus GitHub ToS §5/§8; no EULA condition exists for repository content | Reading what one is licensed to read; seeking facts is permissible | **ALLOWED** |
| A2 | Reading Filee source code to learn facts (not done yet; only if a fact is unobtainable from docs) | MIT permits reading and copying with notice; facts learned are not copyrightable | Studying a work to learn unprotected facts is permissible; copying expression is not | **ALLOWED for facts only**, provided no code or comments are transcribed |
| A3 | Reimplementing the idea, feature set and workflow in original Kotlin from our own spec | Copyright protects expression, not ideas, methods or functionality (general principle; also implied by Art 1's "original works" and Art 2's closed list; see Appendix B on the reference's Article 3 claim). No decompilation is used. | Competing on ideas, standards and better service is lawful competition; intention is to serve customers, not to free-ride on expression | **ALLOWED** |
| A4 | Copying Filee code, comments, doc prose, UI text or format tables and shipping it | MIT permits it if the copyright and permission notices are included; a translated/ported copy is a derivative work that also needs the notice | The accepted licence condition binds ("المسلمون على شروطهم"); honouring it would make the act permissible — but project policy is originality | **NOT USED (policy: NO).** If ever done: **ALLOWED ONLY WITH FULL MIT NOTICE** |
| A5 | Reusing facts from Filee's format/engine metadata (extension sets, engine capability lists, route examples) with attribution | Facts and methods are not owned; if a substantial table or doc passage is reproduced, it is MIT-licensed expression and the notice is required | Honesty and attribution; no deception about origin | **ALLOWED with MIT notice if expression is copied; otherwise factual citation recommended** |
| A6 | Porting Filee code line-by-line to Kotlin ("rewrite in Kotlin") | Derivative of MIT code; MIT notice required; possibly also a translation right under Iraqi Art 8(2) if it were not licensed — it is licensed, so the MIT terms control | Licence condition binds; allowed only if honoured | **NEVER under project policy** (we write original code) |
| A7 | Using the "Filee" name, logo, icons, screenshots, donut artwork or trade dress in the app, store listing or marketing | Trademark/trade-dress rights are not granted by MIT; Play IP policy prohibits infringement and requires listing content to be ours or licensed; Play Deceptive Behavior prohibits misrepresentation and false affiliation | Passing off another's brand or implying endorsement is deception (ghish); the name and effort behind it are protected wealth (IIFA 43/5) | **NEVER** |
| A8 | Describing the app as "Filee-compatible", "Filee for Android", "the Filee alternative" or otherwise trading on the name | Same as A7; risk of confusion and of an IP complaint | Deception about origin; forbidden | **NEVER** (a neutral factual sentence in internal docs — "inspired by the MIT-licensed Filee project" — is permissible if true and non-prominent, but keep it out of listing metadata) |
| A9 | Bundling GPL/AGPL engines in the APK (Pandoc GPL-2.0+, Ghostscript AGPL-3.0, calibre GPL-3.0) | GPL/AGPL require the whole combined work to be conveyed under the same licence and source; incompatible with a closed-source APK; AGPL §13 adds a network-source obligation | A licence condition that is not itself unlawful binds; using it while refusing its condition is a breach | **NEVER** |
| A10 | Bundling LibreOffice (MPL-2.0) plus H2Orestart (GPL-3.0) and a JRE in an Android app | MPL is file-level copyleft and the H2Orestart plugin is GPL; also technically infeasible on Android | Condition binds | **NEVER** for Android v1 |
| A11 | Optional, user-initiated download of a GPL/AGPL **separate program** from its official site (not linked, not bundled, no shared address space) | Mere aggregation of separate programs; GPL does not reach our app; if we host a modified AGPL program, §13 source offer applies | Allowed if the licence is honoured and disclosed; not planned | **ALLOWED ONLY AS SEPARATE, UNMODIFIED, OFFICIAL DOWNLOAD; not planned** |
| A12 | Bundling an LGPL-3.0 FFmpegKit/FFmpeg build | FFmpegKit LGPL-3.0 (GPL if GPL libraries enabled); FFmpeg LGPL-2.1+ (GPL if --enable-gpl); LGPL §4 duties: notice, licence copy, relink option, Installation Information; dynamic linking keeps our Kotlin proprietary | Licence condition binds; obligation is to comply, not to avoid the library | **CONDITIONAL ALLOWED** — LGPL build only, with the compliance plan in section 5.3 |
| A13 | Bundling a "-gpl" FFmpegKit build or enabling x264/x265/GPL filters | GPL-3.0 would cover the whole app, including our Kotlin | Condition binds | **NEVER** |
| A14 | Extracting RAR archives (read-only) with an unRAR-derived reader | unRAR restriction permits unpacking RAR archives and forbids using the code to create a RAR-compatible archiver or re-create the compression algorithm | Allowed use within the licence; no deception | **CONDITIONAL ALLOWED** — read-only, no RAR writer, unRAR restriction notice included |
| A15 | Implementing a RAR compressor / WinRAR-compatible writer | unRAR restriction; RAR compression algorithm is proprietary | Condition binds | **NEVER** |
| A16 | Bundling on-device model weights (OCR, upscaling, ASR, translation) | Each weight set has its own licence; Apache-2.0/MIT/BSD-3/CC-BY-4.0 are usable with notice; CC-BY-NC, AGPL and research-only terms are incompatible with a commercial app; exact matrix is docs/research/model-licenses.md | Licence condition binds; respecting authors' terms is part of honesty | **CONDITIONAL** — only after licence verification; NC/AGPL/non-commercial **NEVER** |
| A17 | Downloading model packs only on explicit user action, after showing size and source | Play Deceptive Behavior: asset downloads must be prompted and the download size disclosed first; Data safety must reflect any collection | Transparency and consent; no surprise network use | **ALLOWED WITH CONDITIONS** |
| A18 | Stating the app is "offline" | Play Deceptive Behavior: all metadata must precisely reflect functionality; a false offline claim is misrepresentation | Deception about function is forbidden; the claim must be literally true | **ALLOWED ONLY IF TRUE**: conversion works with no network; optional downloads are labelled as optional and network-requiring |
| A19 | Uploading user files to any server, or collecting content/analytics | Play User Data and Data safety; privacy expectations; Iraqi law has no comprehensive data-protection statute verified, so apply best practice; secret recording/transfer would also be a breach of trust | Amanah; privacy; no deception | **NEVER for user-file upload**; no analytics or ad SDKs in v1 |
| A20 | Providing DRM circumvention, password cracking or signature/identity-document forgery features | Anti-circumvention and fraud rules vary by market and Play IP policy; forgery is criminal fraud; converting the user's own unprotected/password-known files is ordinary utility use | Circumventing protection or forging documents is haram; neutral conversion is permitted | **NEVER for circumvention/forgery**; support user-owned files and user-supplied passwords only |
| A21 | Consulting Filee's THIRD-PARTY-NOTICES.md to shortlist libraries for our stack | Facts about third-party components; each component's own licence governs our use | Learning facts is permissible; we then honour each licence | **ALLOWED** (with independent licence verification of each chosen library — docs/research/android-stack.md) |
| A22 | Publishing the app on Google Play | DDA (California law), Play IP, Deceptive Behavior, User Data, Data safety, All files access, target API 36 (from 31 Aug 2026), permissions rules | Contracts bind; honest metadata and privacy are required | **CONDITIONAL** on the conditions in section 6 |
| A23 | Publishing in other stores or markets | UNKNOWN; per-market law and store terms | Contracts bind; no deception | **PENDING** — counsel decision |
| A24 | Using Filee's donation/sponsor names or logos | MIT does not grant trademark rights; third-party payment logos are their owners' marks | Deception/borrowed endorsement | **NEVER** |
| A25 | Accepting voluntary donations or selling the app | Permissible trade; no riba if no interest-bearing financing is used; taxes/registration are separate | Honest earning is permitted | **ALLOWED** (subject to company/tax set-up and, for paid apps, Play payments policy — open question Q2) |

---

## 4. Artefact table

Class definitions (from the skill): **KEEP** = facts, public documents, own observations/spec, metadata; **SEALED** = notes produced through a breach, internal only; **SANITISE** = mixed content, remove expression and keep facts; **DESTROY** = copies of protected expression; **NEVER** = acts/uses that must not happen. There is no SEALED or DESTROY item in this project because no breach occurred and no protected expression was copied.

| # | Artefact | Class | Reason | Action |
|---|---|---|---|---|
| B1 | Filee source code (.cs, .csproj, BAML, assets) | **KEEP (reference only)** | Public MIT material; reading for facts is lawful; we have not downloaded it | Do not vendor. If a specific fact forces a read, read for facts only; never paste code into our repo; code-review gate |
| B2 | Filee repository text docs (LICENSE, README, CHANGELOG, THIRD-PARTY-NOTICES, ARCHITECTURE, ENGINES, FORMAT-RULES, ADDING-A-CONVERTER, global.json) | **KEEP** | Public MIT documentation; stored as facts under docs/research/filee; not a build input | Keep with URL and date; do not copy prose into the app; if any passage is reused, add the MIT notice (Appendix A) |
| B3 | Filee source artwork, icons, screenshots, docs/media images, website images | **NEVER** (DESTROY any copy) | Copyright expression plus trade dress; using them would also be misrepresentation | Never download or copy; delete any accidental copy; design our own icon and screenshots |
| B4 | "Filee" name, word mark, logo, donut trade dress, filee.sh branding | **NEVER** | MIT grants no trademark rights; Play IP and Deceptive Behavior policies prohibit confusion and false affiliation | No use in app name, package name, icon, screenshots, listing, metadata, in-app text or code identifiers |
| B5 | Our Kotlin/Compose source code, resources, original UI | **KEEP** | Our own expression; clean-room from facts and spec | Standard copyright header; contributor attestation that no Filee code was copied; review before commit |
| B6 | Our format matrix / conversion-graph / engine map derived from Filee facts | **KEEP** | Facts and our own schema; if a substantial Filee table or doc passage is reproduced verbatim, that part is MIT expression | Facts only by default; if expression is copied, add MIT notice; cite Filee in docs/research/filee-matrix.md |
| B7 | Our research notes containing short quotations with attribution | **KEEP** | Iraqi Art 14 permits analyses and short quotations with attribution; keep each quote short (≤5 lines) | Cite source URL and date; audit quotes before any public release; do not paste whole sections or code |
| B8 | Release APK/AAB | **KEEP** | Our code plus permissive/conditional components; must satisfy every component licence and Play policy | Ship an open-source notices screen and licence texts; no GPL/AGPL binaries; signed release; SHA-256 recorded; privacy-policy link |
| B9 | Third-party permissive libraries actually used (Apache-2.0, MIT, BSD-3, e.g. Compose, Media3, ONNX Runtime, Tesseract, tessdata, Whisper, Real-ESRGAN) | **KEEP with notices** | Apache-2.0 §4 requires licence + NOTICE attribution; MIT requires copyright + permission notice; BSD-3 requires notice and forbids endorsement claims | Generate a complete dependency/licence list; bundle licence texts and NOTICE; attribution screen in app |
| B10 | FFmpegKit/FFmpeg LGPL-3.0 libraries (if used) | **KEEP (conditional)** | LGPL-3.0 §4 duties: notice, licence copies, relink option, Installation Information; upstream FFmpegKit is archived and must be replaced by a maintained, verified LGPL build | Prefer Media3 (Apache-2.0). If FFmpegKit is required: LGPL build only, dynamic linking, no --enable-gpl, source of FFmpeg + build instructions + relink/installation information, written notice; counsel signs off |
| B11 | FFmpegKit "-gpl" artifacts, x264/x265/GPL filters | **NEVER** | GPL-3.0 would extend to the whole app | Build-time deny-list; verify shipped .so files and licence manifest |
| B12 | GPL/AGPL engine bundles (Pandoc, Ghostscript, calibre, H2Orestart, upscayl-bin) | **NEVER** | Copyleft incompatible with a closed APK; also infeasible on Android | Never bundle; if ever needed, user-initiated download of an unmodified separate program from its official page only, with disclosure and legal review |
| B13 | LibreOffice (MPL-2.0) binaries and JRE | **NEVER** | MPL file-level obligations, GPL plugin, size and platform infeasibility | Do not ship; document the format families as unsupported |
| B14 | RAR read-only extraction component (e.g. a junrar/UnRAR-derived reader) | **KEEP (conditional)** | unRAR restriction permits unpacking, forbids RAR compression/archiver creation; a port inherits the restriction | Read-only only; include the unRAR restriction notice in About/notices; no RAR writer; verify the library licence and provenance (docs/research/android-stack.md) |
| B15 | Desktop binaries such as 7z.exe, rhwp, ExifTool, FFmpeg.exe | **NEVER** | Not Android-compatible; separate licences (LGPL/unRAR/Artistic/GPL) | Use Android-native libraries (e.g. Apache Commons Compress for zip/7z reading); never copy Filee's engine binaries |
| B16 | OCR language pack (Tesseract tessdata_fast/tessdata_best) | **KEEP (conditional)** | Apache-2.0 verified ("All data in the repository are licensed under the Apache-2.0 License") | Verify chosen repo/version; ship or download with Apache-2.0 notice; record SHA-256 |
| B17 | Whisper / whisper.cpp model pack | **KEEP (conditional)** | Code repos MIT; the weight file's own statement must be checked in docs/research/model-licenses.md before shipping | Verify weights licence; ship MIT notice; SHA-256; user-initiated download with size disclosure |
| B18 | Real-ESRGAN and other upscaler weights | **KEEP (conditional)** | Real-ESRGAN repo BSD-3-Clause; other models vary (CC-BY-4.0, WTFPL, CC-BY-NC, AGPL tools) | Only models cleared in docs/research/model-licenses.md; BSD-3 notice and no-endorsement rule; NC/AGPL blocked |
| B19 | Any model with CC-BY-NC, AGPL, research-only or unknown terms | **NEVER** | NonCommercial restriction or copyleft; cannot be used in a commercial app | Maintain a blocklist; reject in the model-download manifest unless terms are verified permissive |
| B20 | On-device inference runtime (ONNX Runtime Apache-2.0 / TFLite Apache-2.0) | **KEEP with notices** | Apache-2.0 obligations | Include licence and NOTICE; verify version licence in docs/research/android-stack.md |
| B21 | App name candidates and our own brand assets | **KEEP (conditional)** | Our own mark only after a trademark search; no similarity to Filee | Human trademark/Play name search before adoption; register in Iraq if commercial; original icon artwork |
| B22 | Store listing, screenshots, promo copy | **KEEP (our own)** | Must precisely reflect functionality (Play Deceptive Behavior); no Filee imagery or name | Write original copy; state offline scope precisely; disclose optional model download sizes; no "Filee" or "official" claims |
| B23 | Privacy policy and Data safety declaration | **KEEP** | Mandatory for every app, even with no collection; must match actual behaviour | Write before submission; declare no collection only if true; disclose download hosts and any logging |
| B24 | User files processed by the app | **KEEP (local/transient)** | User's own data; must not be uploaded; delete temporary copies | SAF-based access; process locally; clean temp files; no network during conversion |
| B25 | Filee installer, portable build, engine downloads, delta patches | **NEVER** | Not needed; binaries; would also carry extra licences | Do not download, run or redistribute |
| B26 | Filee donation/sponsor names and logos | **NEVER** | Third-party trademarks and brand | Use only our own funding presentation |

---

## 5. Analysis

### 5.1 Copyright: facts vs expression, and what MIT actually permits

The governing rule is the one the clearance skill states: facts and ideas are not owned; expression and copies are. Filee's MIT licence is unusually permissive, so the legal risk in this project is **not** the reading of the repository; it is (a) accidentally importing expression (code, comments, doc prose, UI text, images) and failing to attach the MIT notice, and (b) treating the MIT software licence as if it also granted trademark or third-party rights.

Verified MIT text (LICENSE, copyright "2026 Myeonggu Kim (KnifeLemon) and Filee contributors"):

> "Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software"

> "The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software."

Consequences:

1. **Reading** the repository and learning that a feature exists is permitted and creates no obligation. The MIT condition triggers only on copying the Software or substantial portions of it.
2. **Our original Kotlin implementation** from a written spec is our own expression and is outside Filee's copyright. No notice is required for ideas, workflows, feature lists, extension-to-engine mappings or algorithms we implement independently.
3. **If** a teammate ever copies a table, a paragraph of documentation, a UI string, or code, the MIT notice (copyright line + full permission text) must travel with it. The safest policy, adopted here, is: do not copy; if a substantial factual table is reproduced from Filee docs, add the notice anyway (Appendix A).
4. **A translation or port** of Filee code would be a derivative work. MIT allows it with the notice, but the project chooses an independent implementation, so this path is closed by policy (B5, A6).
5. **"Public on GitHub" is not itself the licence.** GitHub ToS §5 gives other users only a licence to view and fork through the Service, and §8 preserves lawful access; the right to reuse comes from the repository's MIT LICENSE ("You may grant additional rights by adopting a license"). Here that additional grant exists and is broad.
6. **Iraqi law** independently protects Filee's expression: computer programs are protected as literary works (Art 2(2)); reproduction, translation, adaptation and modification need the author's written permission (Art 8); the single personal copy of Art 13 and the analysis/short-quotation rule of Art 14 do not authorise cloning a product or copying its documentation into a commercial app. Because all our use is either licensed by MIT or limited to facts and short attributed quotations, we do not need to rely on those exceptions — but the internal research notes should stay within Art 14 (short quotes, attribution) so they remain defensible as study material.

### 5.2 Trademark, name and trade dress — the clear NEVER

MIT is a copyright licence. It does not grant rights in the "Filee" name, the hand-made logo (docs/LOGO.md shows the source artwork is Filee's own), the donut menu look, the screenshots or the site branding. Those are protected by trademark/trade-dress and unfair-competition rules, by Play's Intellectual Property policy and by Play's Deceptive Behavior policy:

> "This policy prohibits apps and developer accounts from infringing on the intellectual property rights of others, including copyright, trademark, and patents. You must ensure that all content in your app and store listing is either your own original work or that you have the necessary licenses and/or permissions to use it." (Play IP policy, P2)

> "All metadata, including the store listing, screenshots, and title, must precisely reflect the app's functionality." (Play Deceptive Behavior, P3)

Using the Filee name or imagery would create a likelihood of confusion, invite an IP complaint and contradict the honest-origin requirement. The donut drag-and-drop **idea** is a workflow and is not protected; copying the exact look, icons or artwork is. We design an original Android interaction (share-sheet/picker + our own radial picker if any) and our own icon. No "Filee-compatible", "official", "Filee for Android" or similar strings anywhere.

The Iraqi trademark statute to check before adopting our own name is Law No. 21 of 1957 on Trademarks and Trade Names, as amended up to Law No. 9 of 2010 (WIPO Lex record 21933). Registration and clearance are human/legal tasks (Q5). Play name availability is a separate check.

### 5.3 Third-party licences: what may ship and what may not

**Never bundle in the APK:** GPL- and AGPL-licensed engines (Pandoc GPL-2.0+, Ghostscript AGPL-3.0, calibre GPL-3.0, H2Orestart GPL-3.0). Their copyleft would require the whole combined work to be conveyed under the same licence with source, which is incompatible with a proprietary app, and the AGPL adds a network-source obligation (AGPL §13). LibreOffice is MPL-2.0 (file-level copyleft) and is in any case a desktop program that cannot run inside an Android APK. These components are also impractical on Android, so the "optional user-download" fallback is not planned; if it ever were, it would have to be an unmodified separate program downloaded from its official source, with its own notices and no linking (A11, B12).

**FFmpegKit (LGPL-3.0) — conditional.** FFmpegKit's own README says the library and its default bundles are LGPL 3.0, but "GPL v3.0 if GPL licensed libraries are enabled", and the -gpl bundles "make the whole bundle effectively subject to the GPL v3.0". FFmpeg's legal page adds: "Compile FFmpeg without --enable-gpl and without --enable-nonfree. Use dynamic linking ... Distribute the source code of FFmpeg ... Make sure the source code corresponds exactly to the library binaries you are distributing." LGPL-3.0 §4 then requires, for a Combined Work:

> "Give prominent notice with each copy of the Combined Work that the Library is used in it and that the Library and its use are covered by this License."
> "Accompany the Combined Work with a copy of the GNU GPL and this license document."
> (d) either convey Minimal Corresponding Source and Corresponding Application Code suitable for recombining/relinking, or use a suitable shared library mechanism; (e) provide Installation Information where GPL §6 would require it.

Practical plan: prefer **Media3 Transformer (Apache-2.0)** plus platform MediaCodec to avoid the copyleft question entirely where it covers the formats. If FFmpegKit is unavoidable: use only a **maintained LGPL build** (the arthenica repository is archived since 2026; a maintained fork must be verified for provenance and build flags), dynamic linking, no GPL modules, and ship notice + LGPL/GPL texts + FFmpeg corresponding source (or a written offer) + relink/installation instructions. Because Android apps are "User Products" and an APK is signed, the exact way to satisfy §4(d)/(e) is a lawyer question; do not ship FFmpegKit without that sign-off (Q3, C5).

**unRAR — conditional, read-only.** The restriction is narrow and clear:

> "The unRAR sources cannot be used to re-create the RAR compression algorithm, which is proprietary. Distribution of modified unRAR sources in separate form or as a part of other software is permitted, provided that it is clearly stated in the documentation and source comments that the code may not be used to develop a RAR (WinRAR) compatible archiver." (7-Zip License.txt, section "unRAR license restriction")

and the WinRAR EULA adds:

> "Neither RAR binary code, WinRAR binary code, UnRAR source or UnRAR binary code may be used or reverse engineered to re-create the RAR compression algorithm, which is proprietary, without written permission."

Therefore extracting an existing RAR archive is permitted, but we must not ship a RAR writer/compressor, must keep the restriction statement in our notices/documentation, and must check whether the chosen Android reader (e.g. junrar) is an unRAR derivative and therefore inherits the restriction (docs/research/android-stack.md). Dropping RAR support entirely is the safest option if schedule allows (Q7).

**Model weights — conditional, per-licence.** Verified permissive cases: Tesseract language data is Apache-2.0; OpenAI Whisper and whisper.cpp are MIT; Real-ESRGAN is BSD-3-Clause (with the no-endorsement clause: "Neither the name of the copyright holder nor the names of its contributors may be used to endorse or promote products derived from this software without specific prior written permission."). Apache-2.0 additionally requires the NOTICE-file attribution in section 4(d). Non-commercial terms are incompatible with a commercial app: CC BY-NC's definition is "not primarily intended for or directed towards commercial advantage or monetary compensation" — so CC-BY-NC weights are **NEVER**. AGPL-licensed tools/weights are **NEVER** to bundle. The exact per-model decision belongs to docs/research/model-licenses.md; this memo only fixes the classification rule (A16, B16–B19).

**Notices we must ship** for whatever we do use: MIT copyright + permission notice; Apache-2.0 licence + NOTICE; BSD-3 notice + disclaimer and no endorsement; the unRAR restriction statement; FFmpeg/LGPL notices and source offer if used. Templates are in Appendix A.

### 5.4 Google Play and the offline claim

- **Contract:** the Developer Distribution Agreement is governed by California law with exclusive jurisdiction in Santa Clara County (§16.8); Google may contract through Google LLC (Delaware), Google Ireland Ltd or other entities depending on the developer's location. Play's program policies sit on top.
- **Intellectual property:** the IP policy (P2) and Deceptive Behavior policy (P3) require that everything in the app and listing be ours or licensed, and that metadata precisely reflect functionality. This is the legal basis for A7/A8: no Filee name or imagery, no false affiliation, no "official" claim.
- **Offline claim must be literally true.** If the app's editor says offline conversion, conversion must complete with the network off. The only network use is an explicit, user-initiated model download; that download must be prompted and its **size disclosed before it starts** (Play requires this for additional assets), and the listing must not claim "works fully offline" without qualification. Store copy should read, for example, "Converts files on your device; optional OCR/AI packs need internet once to download."
- **Permissions:** a converter can work through the Storage Access Framework and MediaStore, and Play's All files access policy says the permission may be used only when those "cannot effectively make use" of the best practices (P4). A file converter does not qualify for the permitted uses (file managers, backup/restore, antivirus, document-management apps) and would need the Permissions Declaration Form and approval. **Do not request MANAGE_EXTERNAL_STORAGE**; use SAF/picker + share intents. Do not request permissions the app does not use.
- **Data safety:** the form is mandatory for every app, "even developers with apps that do not collect any user data must complete this form and provide a link to their privacy policy" (P6). If v1 ships with no analytics, no ads, no upload and no server, declare **no data collected or shared**, and write the privacy policy to match. If model packs are hosted on our own server, server logs (IP, timestamps) are data processing and must be declared; hosting them on GitHub Releases reduces our collection but the privacy policy should still name the download host.
- **User data:** the User Data policy requires transparency about access, collection, use, handling and sharing (P5). No file content, names or metadata may be transmitted (A19).
- **Target API:** from 31 August 2026 (already in force on the memo date) new apps and updates must target Android 16 (API level 36) or higher (P7). The sibling project uses compileSdk 35; the new app must be built against SDK 36 to submit. Technical checks (16 KB page size, ABI, FGS) are owned by the stack/Play dossier tasks, but they are submission blockers and are listed as conditions.
- **Payments, ads, families, content rating:** not assessed here because the monetisation model is undecided (Q2). If ads or in-app purchases are added, the clearance must be re-run: Play payments policy, ad-content/content-rating rules, and the ḥalāl screen for ad content (no riba, gambling, immorality).

### 5.5 Privacy and security of user files

The product promise is local processing. Conditions: conversion performs no network I/O; temporary files live in app-private storage and are deleted after completion; no file names/content in logs; no analytics or crash SDKs in v1; model downloads are the only network path, initiated by the user, with size/source disclosure and SHA-256 verification before use (the integrity-check-before-use pattern is a generic engineering idea, not copied code). If a share/export uses a content: URI, no broad storage permission is needed. This supports the Play Data safety "no collection" declaration and the ḥalāl requirements of honesty and trust (amanah).

### 5.6 Jurisdiction notes, including corrections to the skill reference

- **Iraq — copyright.** Law No. 3 of 1971 (with amendments; Ministry of Justice 2020 translation). Verified articles: Art 1 (original works of literature, arts and sciences protected); Art 2(2) ("Computer programs, whether by source or machine code, that must be protected as literary works."); Art 4 (translations/adaptations protected, without prejudice to the original author); Art 8 (reproduction in any medium, translation/adaptation/modification, distribution etc. need the author's **written permission**); Art 13 (one copy for personal use may not be prevented); Art 14(1) (analyses and short quotations permitted for conveying, controversy, education, edification or information, with attribution to author and source); Art 44 (compensation); Art 45 (piracy: fine of IQD 5,000,000–10,000,000; on a second conviction imprisonment 5–10 years and/or a fine of IQD 100,000,000–200,000,000, plus possible closure and confiscation/destruction); Art 49 (the law applies to works of Iraqi and foreign authors first published/presented in Iraq or in a foreign country; foreigners receive protection no less than Iraqis).
- **Iraq — the reference's Article 3 claim.** The skill's legal-framework.md says "Art. 3: protection covers expression, not ideas, procedures, methods or mathematical concepts as such." The Ministry of Justice translation we fetched has **Article 3 about protection of the work's title**, and no express idea/expression clause anywhere in the translation. The idea/expression distinction remains a general copyright principle and it is also true that MIT/contract do not reach ideas, but the memo does **not** cite an Iraqi article for it. Counsel should confirm whether an amendment or another statute states it (Appendix B).
- **Iraq — trademarks.** Law No. 21 of 1957 on Trademarks and Trade Names, amended up to Law No. 9 of 2010 (WIPO Lex 21933). Name clearance and registration are human/legal steps (Q5). Unregistered-name protection and unfair-competition remedies were not verified.
- **Iraq — treaties.** Iraq is not a WTO member (accession ongoing since the 2004 Working Party). Berne membership was not verified: WIPO's old parties URL returned 404, WIPO Lex's Iraq treaty notifications list contains no Berne entry, and secondary sources describe Iraq as a non-member. Even so, Art 49 protects foreign authors' works in Iraq, so the Korean author's expression is protected here regardless of Berne; the MIT licence is the controlling permission in any event. **Marked UNVERIFIED for counsel.**
- **GCC and other markets.** The skill reference warns that GCC states differ and that UAE is stricter for software; no GCC text was read for this memo. If the app is distributed in the GCC, EU, UK or US, counsel must review per market. **UNKNOWN pending Lead decision (Q1).**
- **Google Play contract.** California law and Santa Clara County courts (§16.8), with the Google entity depending on the developer's location. This is a contract with Google, not a law that governs our IP ownership.

### 5.7 Sharīʿah reasoning (a reasoning aid, not a fatwa)

**The condition is binding.** Qur'an 5:1 commands: "O you who have believed, fulfill [all] contracts." The Prophet (ﷺ) said: "Muslims are on (i.e. stick to) their conditions." (Sunan Abi Dawud 3594; graded Hasan Sahih by al-Albani). An accepted licence — including an open-source licence and a Play developer agreement — is a condition that must be honoured unless it commands something unlawful. That is the ḥalāl basis for taking third-party licence compliance seriously, including the MIT notice, LGPL duties, unRAR restriction and model terms.

**Intellectual property is protected wealth.** The International Islamic Fiqh Academy resolved:

> "Business name, corporate name, trade mark, literary production, invention or discovery, are rights belonging to their holders and have, in contemporary times, financial value which can be traded. These rights are recognized by Shariah; therefore, not permissible to violate." (Resolution No. 43 (5/5), 15 December 1988)

> "Copyrights and patent rights are Shariah protected rights. Their holders are entitled to freely exploit them. It is not permissible to violate such rights." (same resolution, Third)

Under this reasoning: copying Filee's name, logo or expression without permission would be a transgression; using MIT-licensed material within its terms is permissible; independent competition on the idea is permissible.

**Neutral utility is permitted.** File conversion is a neutral tool serving lawful needs (documents, photos, media, archives). The default in things is permissibility; the sin, if any, attaches to unlawful use. The Al-Ameen case applies necessity rigorously — "the licence is expensive" or "we need to catch up" did not excuse decompilation when the vendor was reachable and the product was for sale. Here necessity is not even in play: the material is MIT-licensed, so we act within permission rather than relying on any exception.

**Conditions we adopt as ḥalāl requirements.** (1) Honour every contract and licence condition (Qur'an 5:1; the hadith). (2) No deception (ghish): truthful origin, truthful "offline" claim, disclosed download sizes, no Filee name or implied endorsement. (3) No haram use cases: no DRM circumvention, no password cracking, no document/identity forgery, no malware, no non-consensual privacy violation. (4) Respect model authors' terms; never use non-commercial weights in a commercial product. (5) No riba: no interest-bearing financing and no interest-based monetisation; ordinary sale or voluntary donation is permitted. (6) Amanah over user files: process locally, do not upload, do not retain. (7) Honest attribution where attribution is due. (8) If a later feature or monetisation changes the analysis (ads, purchases, cloud processing), stop and re-run this gate.

**No breach to repair.** Unlike Al-Ameen V9, no accepted condition was broken and no protected expression was copied. Tawbah conditions (stop, remove copies, regret, resolve the right, resolve not to return) are therefore not triggered. If a breach is later discovered: stop, hash, destroy, sanitise, seek permission, record — do not keep copies "for reference".

### 5.8 Name recommendation

**Do not use "Filee", any confusingly similar variant ("Filey", "Filee Pro", "Filee Android"), or the donut logo/brand.** Reasons: MIT grants no trademark rights; Play IP and Deceptive Behavior policies; Iraqi trademark law; and the ḥalāl prohibition on deception about origin.

Short list (all need a human trademark and Play-availability check; a quick screen on 2026-11-16 already found conflicts, so do not treat any of these as available):

| Candidate | Meaning / rationale | Quick-screen finding (2026-11-16) | Status |
|---|---|---|---|
| **Sigha** (صيغة, "format") | Direct, memorable, Arabic-first; describes a file's form | No direct Play app named "Sigha" found in a quick screen; not proof of availability, and it is a common word so collisions in other classes are likely | **Preferred candidate, pending search** |
| **Tahweel Files** (تحويل, "conversion") | Strong semantic fit; only as a distinctive compound, never bare "Tahweel" | "Tahweel" exists on Google Play (Tahweel Financial Services; app.tahweel.io) | **Risky; avoid bare form, compound needs counsel** |
| **Tabdeel** (تبديل, "replacement/change") | Common Arabic word, descriptive | "Tabdeel - Exchange Goods" exists on Google Play (Alphatech Solutions) and the App Store | **Avoid bare form** |

Recommendation: prefer a coined or compound mark rather than a bare common Arabic word, run a proper search in the Iraqi trademark registry (Law 21/1957) and Korea/EU/US if distributed there, check Play name availability (including the 30-character listing title and the package name), and register before spending on branding. Final name approval is a human/legal decision (Q5).

---

## 6. Conditions and open questions

### 6.1 Mandatory conditions before the first public release

- **C1 — Clean-room discipline.** No Filee code, comments, documentation prose, UI strings, tables or assets may be vendored. Every contributor signs a "no copied code" attestation; code review checks against the Filee snapshot. If any expression is ever reused, add the MIT notice in Appendix A to the repository and the in-app notices, and record it in this memo.
- **C2 — Brand separation.** Original app name (after clearance), original icon, original screenshots, no "Filee" in metadata or in-app text; no claim of affiliation, compatibility or endorsement.
- **C3 — Attribution hygiene.** In docs, cite Filee with URL and date; keep quotes ≤5 lines; provide the MIT notice if any Filee expression or substantial factual table is reproduced (recommended even then).
- **C4 — No GPL/AGPL in the APK.** Add a build-time and release check that fails if the app contains GPL/AGPL components (x264/x265, --enable-gpl FFmpeg, Ghostscript, Pandoc, calibre, H2Orestart, upscayl-bin) or any GPL-configured artifact. Keep the licence manifest in the repo.
- **C5 — FFmpegKit decision.** Prefer Media3 Transformer (Apache-2.0). If FFmpegKit is unavoidable: only a maintained LGPL build with verified provenance/build flags; dynamic linking; notices + licence texts + FFmpeg source offer + relink/installation information; counsel sign-off on LGPL-3.0 §4(d)/(e) for a signed Android APK before release.
- **C6 — RAR read-only.** If RAR is supported: extraction only, never a RAR writer; ship the unRAR restriction notice; verify the reader's provenance and licence. Otherwise drop RAR (cleaner).
- **C7 — Model gate.** No model pack ships or is offered in the app manifest until docs/research/model-licenses.md verifies its licence, notice text, source URL and SHA-256. Block CC-BY-NC, AGPL, research-only and unknown-licence models by allow-list.
- **C8 — Play compliance.** Data safety form + privacy policy before submission; no analytics/ad SDKs in v1; SAF/picker, no MANAGE_EXTERNAL_STORAGE; target SDK 36 (submission requirement since 31 Aug 2026); 16 KB page-size and other technical submission checks signed off by the stack/Play dossier tasks; asset-download disclosure with sizes; metadata exactly matches behaviour.
- **C9 — Offline truth.** Network off must still convert. Store copy must distinguish "conversion is offline" from "optional AI/OCR packs download once, size disclosed". Test this before release.
- **C10 — Privacy by design.** No user-file upload; app-private temp with cleanup; no content logging; model downloads only after explicit user action with integrity check; disclose the download host in the privacy policy.
- **C11 — Professional review.** Before commercial launch: Iraqi lawyer (copyright, trademark, consumer/e-commerce, company/tax), a qualified scholar for a fatwa on the monetisation and use-case limits, and Play policy review. Re-run this gate if the monetisation model, distribution countries or stack change.
- **C12 — Record keeping.** Keep this memo, the snapshots in docs/legal/sources and the dependency-licence manifest with the release. Re-fetch sources and re-clear if Filee facts are reused after a material repository change (Filee is under active development; facts are frozen at v2.5.0, 2026-11-16).

### 6.2 Open questions (decisions needed)

- **Q1 — Distribution markets:** Iraq only, MENA/GCC, or global via Play? Drives per-market counsel. UNKNOWN.
- **Q2 — Monetisation:** free, paid, ads, in-app purchases, donations? Ads/IAP would require Play payments/ad policy review and a fresh ḥalāl review of ad content and data collection. Also: is paid-app distribution even available in Iraq through Google Play? UNVERIFIED.
- **Q3 — FFmpegKit vs Media3-only?** LGPL compliance burden vs format coverage. Recommendation: start Media3-only and re-open if a must-have codec is missing.
- **Q4 — Which model packs in v1?** OCR (Tesseract/tessdata) and upscaling (Real-ESRGAN BSD-3) look feasible; Whisper/translation later. Pending docs/research/model-licenses.md.
- **Q5 — Name and trademark:** choose after Iraqi trademark search + Play availability + domain; register; check similarity to "Filee" is avoided.
- **Q6 — Model hosting:** GitHub Releases vs own server? Affects Data safety/privacy declarations and download-size disclosure.
- **Q7 — RAR:** support read-only or drop? Dropping avoids the unRAR restriction and a licensing dependency.
- **Q8 — Entity and taxes:** who publishes (individual vs Iraqi company) and how revenue is declared? Needs local accountant/lawyer.

---

## 7. Decision

**We may proceed** with the clean-room plan: read the public MIT-licensed Filee repository for facts, write an original Kotlin/Compose Android converter, and ship only components whose licences we can satisfy (Apache-2.0, MIT, BSD-3, and conditionally LGPL-3.0 under a written compliance plan). **We must not** copy Filee code, documentation prose, images, icons or screenshots; must not use the Filee name, logo or trade dress in any form, including "compatible" or "official" claims; must not bundle GPL/AGPL engines; must not ship a RAR compressor; must not use non-commercial or AGPL model weights; must not upload user files or misstate the offline behaviour. No SEALED or DESTROY material exists and no remediation is required. This decision is conditional on C1–C12 and on the open questions being answered; the "offline" claim and the model-download disclosure are release blockers for Play. **Not a lawyer, not a mufti; recommend professional legal review (Iraqi counsel) and a qualified scholar's fatwa before commercial launch.**

---

## Appendix A — Notice texts to ship if/when applicable

**A.1 MIT notice (Filee, if any expression or substantial factual table is reused).** Include the copyright line and the full MIT permission text. Suggested repository notice:

> Portions of this project's format documentation are derived from Filee (https://github.com/KnifeLemon/Filee), Copyright (c) 2026 Myeonggu Kim (KnifeLemon) and Filee contributors, licensed under the MIT License. The MIT permission text is reproduced in LICENSES/Filee-MIT.txt.

Add the full MIT text verbatim, including the copyright line and the notice condition quoted in section 5.1.

**A.2 LGPL-3.0 notice (only if FFmpegKit/FFmpeg is bundled).** In About/OSS notices and in the repository:
"This application uses FFmpegKit and FFmpeg libraries, licensed under the GNU Lesser General Public License version 3. The licence texts are available here. The Corresponding Source for FFmpeg and FFmpegKit, together with build instructions, is available at [URL or written offer]. You may replace the library with a modified, interface-compatible version; instructions are at [URL]." Keep the FFmpeg source offer current for every shipped build; include the GPL-3.0 and LGPL-3.0 texts.

**A.3 unRAR restriction notice (if RAR reading is supported).** "This app can extract RAR archives using code derived from the unRAR sources. The unRAR sources cannot be used to re-create the RAR compression algorithm, which is proprietary; this app does not create RAR archives and may not be used to develop a RAR (WinRAR) compatible archiver."

**A.4 Apache-2.0 / MIT / BSD-3 (all shipped permissive components).** Bundle the licence text and, for Apache-2.0 components with a NOTICE file, the NOTICE attribution (Apache-2.0 §4(d)); for MIT components, the copyright and permission notice; for BSD-3 components, the copyright notice, conditions and disclaimer, and no endorsement claim using the authors' names.

---

## Appendix B — Discrepancies and corrections to assumptions

1. **Skill reference vs fetched Iraqi text (Art 3).** legal-framework.md attributes the idea/expression rule to Art 3 of Law 3/1971. The Ministry of Justice translation fetched today has Art 3 on titles, not on ideas. The memo relies on the general copyright principle and MIT, not on that article. Counsel should confirm whether the reference is describing an amendment or another text.
2. **Iraq Berne status.** Not verified. WIPO's old Berne parties URL returned 404; WIPO Lex's Iraq notification list shows no Berne notification; secondary sources say Iraq is not a Berne member. Art 49 of Law 3/1971 protects foreign authors' works in Iraq regardless. Marked **UNVERIFIED**.
3. **Iraq WTO status.** Verified non-member-in-progress: Working Party established 13 Dec 2004; Iraq absent from the WTO members list fetched today. TRIPS is therefore not directly binding, but the domestic statute and the MIT licence control.
4. **Play contract law.** The brief said "Ireland/US contractual". Verified: DDA §16.8 chooses California law and Santa Clara County courts; the Google contracting entity can be Google LLC, Google Ireland Ltd or others depending on region. Both facts are recorded.
5. **Filee format count.** The repository description says 263 formats; the README says 270. Use the README figure with the description caveat; not legally material.
6. **FFmpegKit upstream.** arthenica/ffmpeg-kit is archived (last push 2026-07-02) and LGPL-3.0. Any build must use a maintained fork with verified provenance; the licence duties do not disappear because the project is archived.
7. **Hadith grading.** The skill reference says "ṣaḥīḥ by al-Albani"; the sunnah.com record fetched today shows "Hasan Sahih (Al-Albani)". The memo uses the fetched grade.
8. **Play target API.** The sibling project's compileSdk 35 is below the submission requirement in force on the memo date (new apps must target API 36 since 31 Aug 2026). Flagged to the Lead as a technical blocker.
9. **Name screen is not a trademark search.** The "Sigha/Tahweel/Tabdeel" findings are a web/Play screen only; no registry search was possible here.

---

## Appendix C — Evidence snapshot inventory

Saved under docs/legal/sources (all plain text/HTML/JSON/PDF, no binaries): Filee repo metadata, licence API, latest release, LICENSE, README, LOGO.md, repo root and docs listings; GitHub Terms of Service; Iraq Copyright Law No. 3/1971 English and Arabic PDFs plus extracted English text; Iraq trademark law record page; WIPO Lex Iraq treaty notifications; WTO Iraq accession and members pages; IIFA Resolution 43 page; Qur'an 5:1 JSON; Sunan Abi Dawud 3594 page; unRAR/WinRAR licence; 7-Zip licence; FFmpeg legal page; FFmpegKit LICENSE/README/repo metadata; LGPL-3.0, AGPL-3.0, GPL-3.0, Apache-2.0, MPL-2.0 and CC-BY-NC texts; Tesseract, tessdata, Whisper, whisper.cpp, Real-ESRGAN licences; Play DDA and the Play policy pages (IP, Deceptive Behavior, All files access, User Data, Data safety, target API, sensitive permissions). The workspace copy of Filee's LICENSE (docs/research/filee/LICENSE, SHA-256 34209D059D78056E9A68C861AEBEE328C0651C909467479C49B3E2DD1BD9CECE) is textually identical to today's fetched copy after normalisation (BOM/line endings differ).

---

*Prepared 2026-11-16 (Asia/Baghdad) by the legal-clearance teammate. This is a diligence record to support the project's decision, not legal advice and not a fatwa. **Not a lawyer, not a mufti; recommend professional review before commercial launch.***
