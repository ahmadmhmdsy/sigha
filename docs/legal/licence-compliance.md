# Licence compliance record — Sigha 0.2.0 (build 2)

**Created:** 2026-11-17 · **Owner:** policy-guardian (WS4) · **Status:** working compliance record, not a legal opinion.
**Scope:** the APK distributed with package `com.sigha.converter`, versionName 0.2.0 / versionCode 2, all ABIs (arm64-v8a, x86_64, universal). It records what is satisfied today, what becomes satisfied only after a publisher action, and what is unresolved. Counsel sign-off is blocker **B7** in [docs/legal/release-blockers.md](release-blockers.md).

**Companion files**
- `NOTICE.md` — published notices; shipped byte-identical as `app/src/main/assets/licenses/THIRD-PARTY-NOTICES.txt` and shown in **Settings → Licenses → Full third-party notices** (SHA-256 equality verified, see §7).
- `docs/legal/privacy-policy.md` — privacy policy (see §8 here for the one privacy/licensing overlap: the optional OCR pack download).
- `docs/legal/clearance-memo.md` — legal clearance including conditions **C4** (no GPL/AGPL in the APK) and **C5** (FFmpegKit conditional: notices, texts, source offer, relink information, counsel sign-off).
- `docs/plan/production-readiness.md` — WS4 definition of done.
- `docs/legal/sources/` — provenance copies of licence texts and policy snapshots.

---

## 1. What is actually distributed

Source of truth for the dependency set: `app/build.gradle.kts` (read 2026-11-17). The JVM/Android
libraries below are merged into the APK; the native libraries are the `.so` files those artefacts
contain. A release AAB is split by ABI (arm64-v8a, x86_64) plus a universal APK, so every licence
duty below attaches to each distributed artefact.

### 1.1 JVM/Android artefacts

| Artefact | Licence (as stated by the artefact) | Notice / text shipped |
|---|---|---|
| `dev.ffmpegkit-maintained:ffmpeg-kit-full:8.1.9` | LGPL-3.0 (POM); wrapper of FFmpeg (LGPL-2.1-or-later) | `licenses/ffmpeg-kit/FFmpegKit-LGPL-3.0.txt`, `licenses/LGPL-3.0.txt`, `licenses/GPL-3.0.txt`, `licenses/LGPL-2.1.txt` |
| `com.tom-roush:pdfbox-android:2.0.27.0` (+ FontBox, Bouncy Castle, LiberationSans) | Apache-2.0; MIT (Bouncy Castle); SIL OFL 1.1 (font) | `licenses/Apache-2.0.txt`, `licenses/MIT-BouncyCastle.txt`, `licenses/SIL-OFL-1.1.txt` |
| `org.apache.commons:commons-compress:1.28.0` | Apache-2.0 | `licenses/Apache-2.0.txt` + NOTICE reproduced in NOTICE.md §2.1 |
| `commons-io:commons-io:2.20.0`, `commons-codec:commons-codec:1.19.0`, `org.apache.commons:commons-lang3:3.18.0` (transitive) | Apache-2.0 | same + NOTICE reproduced in NOTICE.md §2.1 |
| `org.tukaani:xz:1.12` | 0BSD | `licenses/BSD-0-Clause-XZ-for-Java.txt` |
| `cz.adaptech.tesseract4android:tesseract4android:4.9.0` | Apache-2.0 (wrapper); native Tesseract Apache-2.0; Leptonica BSD-2-Clause; libjpeg-turbo IJG/BSD/zlib; libpng libpng | `licenses/Apache-2.0.txt`, `licenses/BSD-2-Clause-leptonica.txt`, `licenses/IJG-libjpeg-turbo-LICENSE.md`, `licenses/ffmpeg-kit/license_libpng.txt` |
| `io.coil-kt:coil-compose:2.7.0` (+ OkHttp/Okio) | Apache-2.0 | `licenses/Apache-2.0.txt` |
| AndroidX, Jetpack Compose, Material 3, Navigation, DataStore, DocumentFile, ExifInterface, ProfileInstaller, Startup | Apache-2.0 | `licenses/Apache-2.0.txt` |
| Kotlin stdlib, kotlinx.coroutines, kotlinx.serialization | Apache-2.0 | `licenses/Apache-2.0.txt` |
| `smart-exception-java:0.2.1` / `smart-exception-common:0.2.1` (transitive via FFmpegKit) | **UNRESOLVED (O1)** — no licence text published in the Maven artefacts | none — cannot be shipped until resolved |

### 1.2 Native libraries

| Origin | Native libraries | Evidence |
|---|---|---|
| FFmpegKit Full 8.1.9 AAR | per ABI: `libavcodec.so`, `libavdevice.so`, `libavfilter.so`, `libavformat.so`, `libavutil.so`, `libc++_shared.so`, `libffmpegkit.so`, `libffmpegkit_abidetect.so`, `libswresample.so`, `libswscale.so` | `unzip -l` on the cached AAR (path recorded in §7) — 10 `.so` per ABI |
| Tesseract4Android 4.9.0 AAR | `libtesseract.so`, `libleptonica.so`, `libjpeg.so`, `libpngx.so` | `unzip -l` on the cached AAR |

The FFmpeg libraries statically contain the FFmpegKit-vendored third-party libraries listed in
NOTICE.md §3 (32 licence texts shipped). No GPL and no non-free FFmpeg module is configured: the
FFmpegKit POM describes the artefact as "no GPL", and binary string probes found no
`--enable-gpl`, `--enable-nonfree`, `--enable-libx264` or `--enable-libiconv` configure flags in
`libavutil.so` / `libavcodec.so`. `x264`-looking strings are H.264 SEI parser strings (`x264_build`,
`x264 - core`), not a linked encoder. The OpenH264 wrapper encoder is present (O3).

### 1.3 App assets and downloaded content

Added in this pass: 45 licence/notice files under `app/src/main/assets/licenses/` and three legal
documents under `app/src/main/assets/legal/` (see §4 for hashes). They are read offline by
`LicensesScreen` / `PrivacyScreen`; nothing is fetched from the internet to display them.

Not shipped in the APK: any GPL/AGPL engine (LibreOffice, Pandoc, Ghostscript, calibre, unRAR
writer), ONNX Runtime (removed — see §6), Real-ESRGAN weights (not offered in this build), Whisper
weights. The only network content is the optional OCR `tessdata_best` pack (Apache-2.0, pinned
commit, size and SHA-256 checked before use), downloaded only after an explicit user tap and
disclosed in the privacy policy. No model or weight ships inside the APK.

---

## 2. LGPL obligations — clause by clause

Two LGPL layers ship: the **FFmpegKit wrapper (LGPL-3.0)** and the **FFmpeg libraries + the LGPL
third-party libraries inside them (LGPL-2.1-or-later; LAME and Shine are LGPL-2.0)**. Android loads
the libraries as native shared objects (`System.loadLibrary`), which means the app is a "work that
uses the Library" / "Combined Work" rather than a static single binary; that helps with relinking
but does not remove the source and notice duties.

| # | Obligation | Clause | Where it is satisfied | Status |
|---|---|---|---|---|
| L1 | Prominent notice with each copy of the Combined Work that the Library is used in it and that the Library and its use are covered by the licence | LGPL-3.0 §4(a) | `NOTICE.md` §1; in-app **Licenses → LGPL components and your rights** | **met** |
| L2 | Accompany the Combined Work with a copy of the GNU GPL and the LGPL | LGPL-3.0 §4(b) | `licenses/GPL-3.0.txt`, `licenses/LGPL-3.0.txt`, `licenses/LGPL-2.1.txt`, `licenses/ffmpeg-kit/FFmpegKit-LGPL-3.0.txt`, all shown in-app | **met** |
| L3 | Convey under terms that permit modification of the Library portion and reverse engineering for debugging such modifications | LGPL-3.0 §4 chapeau, §2.2 | No EULA, terms of service or click-through restricts use, modification or debugging; the privacy policy covers data only | **met** (nothing restricts it) |
| L4 | Written offer, valid at least three years, for the Corresponding Source | LGPL-3.0 §4(d)(0); LGPL-2.1 §6(a) | `assets/legal/source-offer.txt` shown in-app; `NOTICE.md` §1.2 | **open — B6**: the offer text is complete but the mailbox `opensource@sigha.app` is a placeholder and no source archive is hosted yet |
| L5 | Alternative route: Corresponding Source available from a network server at no further cost | LGPL-3.0 §4(d)(1) | Same text names the future network route; no URL exists yet | **open — B6** (not the chosen route until hosting exists) |
| L6 | Installation Information (methods, procedures, authorisation keys, information) required to install and run a modified library in the app | LGPL-3.0 §4(e); LGPL-2.1 §6(d) | `assets/legal/relink-instructions.txt` + NOTICE.md §1.3: the native libraries are separate `.so` files under `lib/<abi>/`; rebuild the APK with replacement libraries and a user key | **partial — B7**: counsel must confirm the app-side relinkable material/route for a signed APK |
| L7 | No additional restrictions on the licensed material | LGPL-3.0 §(none added); GPL-3.0 §10 via LGPL §4 | No additional terms imposed anywhere | **met** |
| L8 | "AS IS" disclaimer for the libraries | LGPL-3.0 §§15–16; LGPL-2.1 §§15–16 | `NOTICE.md` §1.4; in-app **Licenses** no-warranty block | **met** |
| L9 | For the LGPL-2.1-only/2.0 libraries: source of the library itself, notice, and the §6 alternative (own terms for the work, with source for the Library and relinking materials) | LGPL-2.1 §6; LGPL-2.0 §6 | Same offer and relink texts cover the FFmpeg bundle as one source archive (S only — the static-only libs ride inside the FFmpeg artefacts) | **open — B6/B7** until the archive is published; the texts are in place |
| L10 | Keep the source offer valid for the full three years and for every later version | LGPL-3.0 §4(d)(0) | Not yet operational: the 3-year clock starts at first distribution | **open — B6** (publisher calendar duty) |

**What is explicitly not claimed.** The task draft asked to state that "notices and relinking
information are published" — they are **not published**, only packaged in the app and repository.
The record says so rather than asserting compliance that does not exist today.

---

## 3. Apache-2.0 and other permissive licences

| # | Obligation | Clause | Where | Status |
|---|---|---|---|---|
| A1 | Give recipients a copy of the Apache-2.0 licence | Apache-2.0 §4(a) | `licenses/Apache-2.0.txt` (11,360 B), shown in-app | **met** |
| A2 | Reproduce the attribution notices of every dependency NOTICE file | Apache-2.0 §4(d) | NOTICE.md §2.1 reproduces commons-compress 1.28.0, commons-lang3 3.18.0, commons-io 2.20.0 and commons-codec 1.19.0 NOTICE texts verbatim. The Gradle packaging excludes `META-INF/NOTICE*`, so the repository/app copy is the readable copy; no other dependency in the cache scanning had a NOTICE file. | **met** (with the packaging exclude documented) |
| A3 | Mark modified files | Apache-2.0 §4(b) | No dependency is modified; the app only consumes published artefacts | **n/a** |
| A4 | Retain copyright, patent, trademark and attribution notices | Apache-2.0 §4(c) | NOTICE §2/§3 list every shipped component and every bundled FFmpeg library, texts packaged. Final merged APK must still be inspected (Gradle not run in this pass). | **met in tree; APK check pending** |
| A5 | MIT, BSD-2, BSD-3, ISC, 0BSD, zlib, WTFPL, IJG: keep copyright + permission + disclaimer with copies | each licence | Each component's text is shipped under `licenses/`: `MIT-BouncyCastle.txt`, `BSD-2-Clause-leptonica.txt`, `BSD-0-Clause-XZ-for-Java.txt`, `BSD-3-Clause.txt`, `IJG-libjpeg-turbo-LICENSE.md`, and per-library `licenses/ffmpeg-kit/license_*.txt` | **met** |
| A6 | SIL OFL 1.1: copyright notice + licence travel with the font; Reserved Font Names not used in a modified font | OFL 1.1 §1, §3 | `licenses/SIL-OFL-1.1.txt` carries the Liberation/Arimo/Tinos/Cousine copyright lines; PDFBox ships the font unmodified and the app does not rename it. The embedded font's `name` table contains "Copyright (c) 2012 Red Hat, Inc." (probe in §7). | **met** |
| A7 | Bouncy Castle MIT-style licence | BC licence | `licenses/MIT-BouncyCastle.txt` | **met** |
| A8 | smart-exception-java | **unknown** | No licence file in the Maven artefact; presumed GitHub repo 404s | **open — O1** |

---

## 4. Licence-text inventory (shipped bytes and hashes)

All files live under `app/src/main/assets/`. SHA-256 is taken of the file as packaged (marked
`…` = truncated display; full hashes can be re-generated with the command in §7). This table is the
manifest the build-verifier can assert against.

| Asset | Bytes | SHA-256 (first 16 hex) |
|---|---|---|
| `legal/privacy-policy.md` | 5559 | `6EBFAD14A594C789…` |
| `legal/relink-instructions.txt` | 2038 | `B0696DA9B286D771…` |
| `legal/source-offer.txt` | 2660 | `3ADF87969C8E2491…` |
| `licenses/Apache-2.0.txt` | 11360 | `B8676920A9B5DD9F…` |
| `licenses/BSD-0-Clause-XZ-for-Java.txt` | 663 | `1E77EF26CEE5216D…` |
| `licenses/BSD-2-Clause-leptonica.txt` | 1521 | `87829ABB5BBB00B5…` |
| `licenses/BSD-3-Clause.txt` | 1460 | `5A93D5831E1297AB…` |
| `licenses/ffmpeg-kit/FFmpegKit-LGPL-3.0.txt` | 7652 | `E3A994D82E644B03…` |
| `licenses/ffmpeg-kit/license_cpu_features.txt` | 12876 | `AF272C1B4DD08DD6…` |
| `licenses/ffmpeg-kit/license_dav1d.txt` | 1317 | `B327887DE263238D…` |
| `licenses/ffmpeg-kit/license_expat.txt` | 1144 | `122F2C27000472A2…` |
| `licenses/ffmpeg-kit/license_fontconfig.txt` | 8616 | `51A51AA9823704FD…` |
| `licenses/ffmpeg-kit/license_freetype.txt` | 2074 | `2E3BBB7D7C5C3963…` |
| `licenses/ffmpeg-kit/license_fribidi.txt` | 26440 | `32434AFCC8666BA0…` |
| `licenses/ffmpeg-kit/license_harfbuzz.txt` | 1971 | `BA8F810F2455C2F0…` |
| `licenses/ffmpeg-kit/license_kvazaar.txt` | 1549 | `3C1DC3D7F8A3D08C…` |
| `licenses/ffmpeg-kit/license_lame.txt` | 25284 | `BFE4A52DC4645385…` |
| `licenses/ffmpeg-kit/license_libaom.txt` | 1316 | `4764A286D8B2FAEA…` |
| `licenses/ffmpeg-kit/license_libass.txt` | 755 | `F7E30699D0279835…` |
| `licenses/ffmpeg-kit/license_libiconv.txt` | 35147 | `8CEB4B9EE5ADEDDE…` |
| `licenses/ffmpeg-kit/license_libilbc.txt` | 1511 | `3082DA3B4B80A135…` |
| `licenses/ffmpeg-kit/license_libogg.txt` | 1466 | `D2AB5758336489DA…` |
| `licenses/ffmpeg-kit/license_libpng.txt` | 5345 | `5C0BB4B05B1354AE…` |
| `licenses/ffmpeg-kit/license_libsndfile.txt` | 26518 | `AD01EA5CD2755F60…` |
| `licenses/ffmpeg-kit/license_libtheora.txt` | 1470 | `8417FAD7DA775735…` |
| `licenses/ffmpeg-kit/license_libuuid.txt` | 214 | `18A4A3931B5B7032…` |
| `licenses/ffmpeg-kit/license_libvorbis.txt` | 1470 | `EC1815DB59FCD302…` |
| `licenses/ffmpeg-kit/license_libvpx.txt` | 1537 | `8267348D5AF1262C…` |
| `licenses/ffmpeg-kit/license_libxml2.txt` | 1289 | `C5C63674F8A83C4D…` |
| `licenses/ffmpeg-kit/license_opencore_amr.txt` | 10271 | `8B3F1762349248D4…` |
| `licenses/ffmpeg-kit/license_openh264.txt` | 1295 | `DD5C1C9668512530…` |
| `licenses/ffmpeg-kit/license_opus.txt` | 1928 | `8338CE8D922BB441…` |
| `licenses/ffmpeg-kit/license_sdl.txt` | 930 | `61E627031E1160BF…` |
| `licenses/ffmpeg-kit/license_shine.txt` | 25767 | `517C8BBEA53E7257…` |
| `licenses/ffmpeg-kit/license_snappy.txt` | 2645 | `55172044F7E24120…` |
| `licenses/ffmpeg-kit/license_soxr.txt` | 26432 | `F2F118B9029EC187…` |
| `licenses/ffmpeg-kit/license_speex.txt` | 1773 | `2654A4264B2BFE29…` |
| `licenses/ffmpeg-kit/license_twolame.txt` | 26428 | `257A842724705950…` |
| `licenses/ffmpeg-kit/license_vo_amrwbenc.txt` | 10271 | `8B3F1762349248D4…` |
| `licenses/ffmpeg-kit/license_zimg.txt` | 484 | `7637386B5F81E8A7…` |
| `licenses/FFmpegKit-fork-LICENSE.txt` | 7652 | `E3A994D82E644B03…` |
| `licenses/GPL-3.0.txt` | 35151 | `728BE057DE55DA6A…` |
| `licenses/IJG-libjpeg-turbo-LICENSE.md` | 6077 | `BA6BCEEBCBA0FDD3…` |
| `licenses/LGPL-2.1.txt` | 26419 | `20E50FE7AAE3E563…` |
| `licenses/LGPL-3.0.txt` | 7654 | `60E73D73CC792C82…` |
| `licenses/MIT-BouncyCastle.txt` | 1171 | `0E01F1549C9022F4…` |
| `licenses/SIL-OFL-1.1.txt` | 4414 | `93FED46019C38BBE…` |
| `licenses/THIRD-PARTY-NOTICES.txt` | 12970 | `00EB713D457CB301…` |

Provenance rules used:
- `licenses/ffmpeg-kit/license_*.txt` and `licenses/ffmpeg-kit/FFmpegKit-LGPL-3.0.txt` were extracted **unchanged** from the FFmpegKit AAR's `res/raw/` (the AAR also carries `res/raw/license.txt` = LGPL-3.0, and `res/raw/source.txt` = a stale Arthenica pointer; `source.txt` is deliberately **not** shipped because it names a retired wiki, a retired e-mail and "GPL v3.0", none of which describe this build — see NOTICE.md §3).
- `licenses/LGPL-3.0.txt`, `GPL-3.0.txt`, `Apache-2.0.txt` come from `docs/legal/sources/` (provenance copies in the repository). `LGPL-2.1.txt` has a provenance copy at `docs/legal/sources/lgpl-2.1.txt`.
- `licenses/BSD-3-Clause.txt` is the canonical BSD-3-Clause text used for libuuid, whose extracted AAR text is only a 214-byte pointer to a documentation path that is not in the APK; source: SPDX licence-list-data `text/BSD-3-Clause.txt` (fetched 2026-11-17).
- `licenses/BSD-0-Clause-XZ-for-Java.txt` was fetched from the XZ for Java repository's `COPYING` (2026-11-17) after `tukaani-project/xz-java` GitHub raw URLs 404'd; the XZ for Java project page states 1.12 is 0BSD ("Version 1.9 and older are in the public domain").
- `licenses/THIRD-PARTY-NOTICES.txt` is a byte-identical copy of `NOTICE.md` (hash equality checked).
- No licence text in this tree was written or guessed by hand; every file is a copy of a published text. The only texts written for this project are `assets/legal/*` (our own offer, relink and policy prose).

Fetch failures recorded rather than filled in: `XZ-for-Java-PublicDomain.txt` (404), smart-exception-java licence (absent from Maven; GitHub 404), `tukaani-project/xz-java/main/LICENSE` (404).

---

## 5. Unresolved questions

| ID | Question | Why it matters | Owner |
|---|---|---|---|
| **O1** | What is the licence of `smart-exception-java:0.2.1` / `smart-exception-common:0.2.1`? | It is a transitive FFmpegKit dependency; it ships in the APK, but publishes no licence file and its presumed repo returns 404. Shipping code with no licence is a compliance gap even if it turns out to be permissive. | build-verifier (resolve/replace) + Lead; recorded here |
| **O2** | Is the shipped libiconv under GPL-3.0 or LGPL-2.1? | The bundled text is the libiconv package's GPL-3.0 `COPYING`; the library is normally LGPL-2.1. If the binary really is GPL-3.0, the "LGPL-only" story for the FFmpeg bundle is wrong. Binary string probe found `(libiconv_t)-1` in `libavcodec.so`, so libiconv is linked; the configure flags in the artefacts do not show `--enable-libiconv`. | B7 counsel, with upstream fork provenance |
| **O3** | Does Cisco's royalty-free AVC/H.264 patent licence extend to OpenH264 inside this third-party FFmpeg build? | The OpenH264 wrapper encoder is present; Cisco's grant covers "this binary" as distributed by Cisco. Either obtain Cisco's binary licence confirmation or remove/ignore the H.264 encoder path. | B7 counsel |
| **O4** | Which exact upstream FFmpeg revision and configure flags does `ffmpeg-kit-full:8.1.9` contain? | The Corresponding Source must be the exact source used. FFmpegKit's version scheme only pins the FFmpeg major.minor (8.1), not the revision; the fork's build configuration is not in this repository. | B6 (publish with source) + B7 |

---

## 6. What was deliberately removed from the notices

`ONNX Runtime` was present in the previous `NOTICE.md` draft and in earlier build plans
("ONNX Runtime — MIT"). It is **not** in the build: no dependency, no `libonnxruntime` native
library, no ONNX model. It was removed from `NOTICE.md`, from the in-app list and from the plan
references this record covers. Listing a component that does not ship is a false attribution and is
exactly what council finding H13 flagged. If ONNX is added later, a new notice row and licence text
(and an audit of the model licence) are required.

**Clean-room note kept out of the shipped notice.** The product idea was inspired by Filee
(<https://github.com/KnifeLemon/Filee>, MIT); no Filee code, assets, documentation text, brand name or
trade dress is used. Naming a competitor inside the APK's notices would itself breach the project's
brand-separation condition (clearance memo C2), so the note lives in
`docs/legal/clearance-memo.md` (§1.4, §5.8) and in two private source comments, not in the shipped
text. If Filee expression is ever reused, the MIT notice must be added to both this file and
`NOTICE.md` per memo C1/C3.

Also excluded deliberately: LibreOffice (MPL-2.0, impractical), Pandoc (GPL-2.0+), Ghostscript
(AGPL-3.0), calibre (GPL-3.0), unRAR writer (restricted licence). No RAR writer ships; if a RAR
*reader* is ever added, the unRAR restriction statement must be revisited (clearance memo C6).

---

## 7. How this was verified (and what was not)

Verified in this pass, with commands that can be re-run:
- Every asset path referenced by `LicensesScreen.kt` / `PrivacyScreen.kt` resolves to a real file under `app/src/main/assets/` (44 references, 0 missing; compared against `Get-ChildItem -Recurse -File`).
- Both `strings_legal.xml` files parse as XML (22/23 strings each), the English and Arabic key sets are identical, and no key duplicates a key in `strings.xml` / `values-ar/strings.xml`.
- `NOTICE.md` and `assets/licenses/THIRD-PARTY-NOTICES.txt` are byte-identical: both hash to `00EB713D457CB301E8DA826A53992F8CD538873E2B7183F6F8B6902F9AA5EC50` (SHA-256) at the time of writing.
- Assets hash table regenerated from the tree (§4).
- Unicode-safe: the Arabic string file is valid UTF-8 XML.
- Native binary probing was read-only, against cached AAR contents: `.so` lists, configure-flag strings, `x264`/openh264 markers.
- Font copyright probe: `LiberationSans-Regular.ttf` (410,712 B) inside the PDFBox AAR contains "Copyright (c) 2012 Red Hat, Inc." in its name table.
- META-INF NOTICE scan across the dependency cache, for every group in the dependency set: `commons-compress`, `commons-lang3`, `commons-io`, `commons-codec` are the only shipped artefacts with `META-INF/NOTICE.txt`; all four are reproduced in NOTICE.md §2.1.

**Not verified here** (and therefore not claimed):
- The final merged APK/AAB contents. `app/build.gradle.kts` still excludes `/META-INF/{AL2.0,LGPL2.1,LICENSE.txt,NOTICE.txt,LICENSE.md,NOTICE.md}` and `/META-INF/DEPENDENCIES`, and Gradle was not run in this pass (build ownership: build-verifier/Lead). The repository/app notices compensate for the Apache NOTICE strip, but the merged artefact must be unzipped and checked before release (owner: Lead at integration; WS4 cannot do it).
- 16 KB ELF alignment, `.so` compression/stored count, signing, lint — other workstreams.
- The exact resolved versions of transitive dependencies (cache contains several versions; the NOTICE texts were taken from the newest cached version of each).

---

## 8. Privacy / licensing overlap

The privacy policy must describe the only network path truthfully; the same fact is the licence
boundary for model data:
- OCR pack = Tesseract `tessdata_best` `eng` and `ara` (Apache-2.0), downloaded from
  `raw.githubusercontent.com` only after the user taps Download, with size and SHA-256 checked. The
  privacy policy discloses the host and the IP address that any HTTPS request reveals.
- No model pack ships in the APK, so no model licence accompanies the APK. If the pack list changes,
  `licence-compliance.md` (this file) and `privacy-policy.md` must both be updated before release.

---

## 9. Publisher tasks before the first public distribution

These are human-only and are expanded with exact console steps in
[`docs/legal/release-blockers.md`](release-blockers.md):

- **B6** — create the `opensource@sigha.app` mailbox, publish the Corresponding Source archive
  (FFmpegKit fork 8.1.9 build scripts + exact FFmpeg 8.1 revision + configure flags + modified file
  notice if any) at a stable HTTPS URL, replace the placeholder in `NOTICE.md` and
  `assets/legal/source-offer.txt`, and keep the offer valid three years from first distribution.
- **B7** — counsel sign-off on: LGPL-3.0 §4(d)/(e) for a signed Android APK (the relink/installation
  question), the libiconv GPL/LGPL variant (O2), the OpenH264 patent scope (O3), and whether the
  in-app notice set is sufficient. The exact FFmpeg revision (O4) must come out of B6.
- **O1** — resolve or replace smart-exception-java before the release build is signed. Until then
  the app ships an unlicensed (not merely unreviewed) third-party component.
- Keep this record current: bump the "last updated" date, re-extract NOTICE texts, re-hash assets and
  re-run the §7 checks whenever a dependency, a version or the model-pack list changes.

---

## 10. Change log

| Date | Change |
|---|---|
| 2026-11-17 | Initial WS4 record: AAR extraction of 32 FFmpeg-library licence texts + LGPL-3.0; added LGPL-2.1, GPL-3.0, Apache-2.0, BSD-2, BSD-3, 0BSD, IJG, MIT, OFL texts; rewrote NOTICE.md (ONNX removed, written offer + relink added); added Apache NOTICE reproductions; PrivacyScreen/LicensesScreen licence rendering; this document; release-blockers.md. |