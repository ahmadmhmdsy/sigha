# Third-party notices — Sigha (صيغة)

**Applies to:** Sigha 0.2.1 (build 3), debug and release builds, package `com.sigha.converter`. The dependency set is unchanged from 0.2.0 (build 2); 0.2.1 is a UI/UX fix pass with no new libraries.
**Last updated:** 2026-11-17.

This file lists the third-party components that are **actually shipped inside the APK**. It is
shipped byte-identical inside the app as `assets/licenses/THIRD-PARTY-NOTICES.txt` and shown there
in **Settings → Licenses → Full third-party notices**, together with the full licence texts in
`assets/licenses/`. The licence
texts govern; this summary does not replace them. Nothing is listed here that is not in the build,
and nothing shipped is left out.

**What ships:** the APK (all native libraries are inside it). **What is downloaded later:** only the
optional OCR model packs, and only when the user taps Download (see §5).

---

## 1. LGPL components and your rights

Sigha includes two LGPL components:

| Component | Version / artefact | Licence |
|---|---|---|
| FFmpegKit (maintained fork) | `dev.ffmpegkit-maintained:ffmpeg-kit-full:8.1.9` (AAR, LGPL build — no GPL codecs) | **LGPL-3.0** |
| FFmpeg libraries + their statically linked third-party libraries | libavcodec, libavdevice, libavfilter, libavformat, libavutil, libswresample, libswscale, libffmpegkit, libffmpegkit_abidetect (arm64-v8a and x86_64) | **LGPL-2.1-or-later** (FFmpeg); other licences for the bundled libraries (§3) |

The wrapper (FFmpegKit) is LGPL-3.0; the FFmpeg libraries it contains are LGPL-2.1-or-later. Both
statements come from the artefact's own metadata: the AAR POM declares `GNU Lesser General Public
License v3.0`, and `res/raw/license.txt` in the AAR is the LGPL-3.0 text. FFmpeg itself is
LGPL-2.1-or-later (see <https://ffmpeg.org/legal.html>).

### 1.1 Licence texts bundled in the app

- LGPL-3.0: `assets/licenses/LGPL-3.0.txt` and `assets/licenses/ffmpeg-kit/FFmpegKit-LGPL-3.0.txt`
- LGPL-2.1: `assets/licenses/LGPL-2.1.txt`
- GPL-3.0 (included because LGPL-3.0 incorporates it by reference): `assets/licenses/GPL-3.0.txt`
- Every bundled library's own text: `assets/licenses/ffmpeg-kit/license_*.txt` (§3)

### 1.2 Written offer for Corresponding Source

The publisher offers, **valid for three years** from the date you obtained this copy of the app, to
give any third party a copy of the **Corresponding Source** for the LGPL components above, for no
more than the cost of performing the distribution (zero for network delivery). The Corresponding
Source covers the exact FFmpegKit/FFmpeg source revision used for 8.1.9, the build scripts and
configuration used to produce the native libraries, and the information needed to build and install
a modified version.

**Contact:** `opensource@sigha.app`
**Status:** ⚠️ **TODO — the publisher must create this mailbox and host the source archive at a
stable HTTPS URL before the first public release (blocker B6). This address is a placeholder.**
The full offer text is `assets/legal/source-offer.txt` and is shown in the Licenses screen.

### 1.3 Relink and installation information

You may modify the LGPL libraries and run the app with your modified version. The libraries are
loaded as native shared objects from `lib/<abi>/` in the APK. In short: obtain the Corresponding
Source, build replacement `.so` files with the same names and a compatible interface, rebuild the
APK with your libraries, sign it with your own key, and install it (Android requires matching
signatures, so a store-signed copy must be uninstalled first). The publisher has added no
restriction beyond Android's own signature requirement. The full instructions and the app-side
relinkable material to be offered are in `assets/legal/relink-instructions.txt`.
**Status:** ⚠️ **TODO — counsel must confirm, before release, how the app-side relinkable material
is delivered; until then this is incomplete for LGPL-3.0 §4(d) (blocker B7).**

### 1.4 No warranty; no publisher modifications

The LGPL components are provided **"AS IS", WITHOUT WARRANTY OF ANY KIND**, express or implied,
including merchantability and fitness for a particular purpose. The publisher redistributes the
library binaries as published upstream and has not modified their source. There is no warranty for
the app either, beyond what your local consumer law makes non-waivable.

---

## 2. Other components shipped in the APK

| Component | Licence | Licence text in app |
|---|---|---|
| PDFBox-Android 2.0.27.0 and FontBox | Apache-2.0 | `Apache-2.0.txt` |
| LiberationSans-Regular.ttf (bundled by PDFBox for text rendering) | SIL OFL 1.1 | `SIL-OFL-1.1.txt` |
| Apache Commons Compress 1.28.0 | Apache-2.0 | `Apache-2.0.txt` |
| Apache Commons IO 2.20.0 / Codec 1.19.0 / Lang3 3.18.0 (transitive) | Apache-2.0 | `Apache-2.0.txt` |
| XZ for Java 1.12 | 0BSD | `BSD-0-Clause-XZ-for-Java.txt` |
| Tesseract4Android 4.9.0 (`libtesseract.so`) | Apache-2.0 | `Apache-2.0.txt` |
| Leptonica (`libleptonica.so`) | BSD-2-Clause | `BSD-2-Clause-leptonica.txt` |
| libjpeg-turbo (`libjpeg.so`) | IJG / BSD-3-Clause / zlib (per its licence file) | `IJG-libjpeg-turbo-LICENSE.md` |
| libpng (`libpngx.so`) | libpng licence | `ffmpeg-kit/license_libpng.txt` |
| Bouncy Castle 1.72 (via PDFBox: bcprov, bcutil, bcpkix) | MIT (Bouncy Castle licence) | `MIT-BouncyCastle.txt` |
| Kotlin stdlib, kotlinx.coroutines, kotlinx.serialization | Apache-2.0 | `Apache-2.0.txt` |
| AndroidX: core, activity, lifecycle, navigation, Compose UI, Foundation, Material 3, Material Icons Extended, DataStore Preferences, DocumentFile, ExifInterface, Startup, ProfileInstaller, SavedState, Collection, Annotation | Apache-2.0 | `Apache-2.0.txt` |
| Coil 2.7.0 (with OkHttp 4.12.0 / Okio 3.9.0) | Apache-2.0 | `Apache-2.0.txt` |
| smart-exception-java 0.2.1 and smart-exception-common 0.2.1 (transitive via FFmpegKit) | **UNRESOLVED** — see open question O1 | *not bundled — see O1* |

All licence texts live in the APK at `assets/licenses/` and are readable offline in
**Settings → Licenses**. Nothing in this table is fetched from the internet.

### 2.1 Apache-2.0 NOTICE files, reproduced verbatim

Four shipped Apache Commons artefacts carry a `META-INF/NOTICE.txt`. Apache-2.0 §4(d) requires those
attribution notices to travel with the distribution, and the release packaging rules in this tree
strip `META-INF/NOTICE*` from the merged APK — so the texts are reproduced here instead. This file
ships inside the app (see the header), which keeps the readable copy requirement met even though the
original location is stripped.

```text
Apache Commons Compress
Copyright 2002-2025 The Apache Software Foundation

This product includes software developed at
The Apache Software Foundation (https://www.apache.org/).
```

```text
Apache Commons Lang
Copyright 2001-2025 The Apache Software Foundation

This product includes software developed at
The Apache Software Foundation (https://www.apache.org/).
```

```text
Apache Commons IO
Copyright 2002-2025 The Apache Software Foundation

This product includes software developed at
The Apache Software Foundation (https://www.apache.org/).
```

```text
Apache Commons Codec
Copyright 2002-2025 The Apache Software Foundation

This product includes software developed at
The Apache Software Foundation (https://www.apache.org/).
```

The versions quoted are those found in this build's dependency cache (compress 1.28.0, lang3 3.18.0,
io 2.20.0, codec 1.19.0); if a Gradle dependency update changes a version, re-extract each NOTICE and
update this section. No other shipped dependency was found to contain `META-INF/NOTICE*` (checked by
scanning every resolved AAR/JAR in the Gradle cache for this dependency set; PDFBox-Android, Coil,
AndroidX, kotlinx, Tesseract4Android and the FFmpegKit AAR contain none).

---

## 3. Libraries bundled inside the FFmpeg build

The FFmpeg native libraries statically include the following third-party libraries. Each ships its
own licence text inside the APK (extracted unchanged from the FFmpegKit AAR, `res/raw/license_*.txt`)
at `assets/licenses/ffmpeg-kit/<name>.txt`:

| Library | Licence as stated in its bundled text |
|---|---|
| cpu_features | Apache-2.0 |
| dav1d | BSD-2-Clause |
| expat | MIT |
| fontconfig | MIT-style (Keith Packard / fontconfig authors) |
| freetype | FreeType Licence / GPL-2.0 (dual) |
| fribidi | LGPL-2.1-or-later |
| harfbuzz | "Old" MIT |
| kvazaar | BSD-3-Clause |
| lame | LGPL-2.0 |
| libaom | BSD-3-Clause |
| libass | ISC |
| libiconv | LGPL-2.1 (library); the packaged COPYING is GPL-3.0 — see O2 |
| libilbc | BSD-3-Clause |
| libogg | BSD-3-Clause |
| libpng | libpng licence |
| libsndfile | LGPL-2.1-or-later |
| libtheora | BSD-3-Clause |
| libuuid | Modified BSD (canonical text shipped as `BSD-3-Clause.txt`; the extracted file is only a pointer) |
| libvorbis | BSD-3-Clause |
| libvpx | BSD-3-Clause |
| libxml2 | MIT |
| opencore-amr | Apache-2.0 |
| openh264 | BSD-2-Clause + Cisco AVC/H.264 patent licence — see O3 |
| opus | BSD-3-Clause |
| sdl | zlib licence |
| shine | LGPL-2.0 |
| snappy | BSD-3-Clause |
| soxr | LGPL-2.1-or-later |
| speex | BSD-3-Clause |
| twolame | LGPL-2.1-or-later |
| vo-amrwbenc | Apache-2.0 |
| zimg | WTFPL (per its text) |

One exception to "unchanged": the AAR's libuuid text is only a 214-byte pointer to a
`Documentation/licenses/COPYING.BSD-3` path that is not in the APK, so the canonical BSD-3-Clause
text (`assets/licenses/BSD-3-Clause.txt`, from the SPDX licence list) is shipped alongside it.
The licences of the bundled libsndfile, fribidi, libiconv, soxr, twolame (LGPL-2.1) and shine,
lame (LGPL-2.0) attach the same obligations described in §1: their texts are shipped, and the
written offer covers their Corresponding Source as part of the FFmpegKit artefact.
**Not shipped:** the AAR's `res/raw/source.txt`. It points at the retired Arthenica wiki and a
retired e-mail address and mentions "GPL v3.0"; it is stale, misleading for this build, and is
replaced by §1.2.

---

## 4. Components deliberately excluded from the build

LibreOffice (MPL-2.0, too heavy), Pandoc (GPL-2.0+), Ghostscript (AGPL-3.0), calibre (GPL-3.0),
unRAR (restricted licence). None is compiled into the APK, and none may be added without a separate,
user-initiated download with its own notices. **ONNX Runtime is not shipped.** It appeared in
earlier drafts of this file and in an earlier build plan; the current build contains no ONNX Runtime
code, no `libonnxruntime` library, and no model. It has been removed from this notice and from the
Licenses screen.

---

## 5. Optional model packs (downloaded on explicit user action)

| Pack | Component | Licence | Source |
|---|---|---|---|
| OCR English / Arabic | Tesseract trained data (`tessdata_best`), eng 15,400,601 B, ara 12,603,724 B | Apache-2.0 | <https://github.com/tesseract-ocr/tessdata_best> pinned to commit `e12c65a915945e4c28e237a9b52bc4a8f39a0cec` |
| Image up-scaling | Real-ESRGAN weights — **not shipped and not offered in this build** | BSD-3-Clause (planned) | <https://github.com/xinntao/Real-ESRGAN> |

Packs are downloaded only when the user taps Download, are verified by size and SHA-256 before
installation, and are stored in private app storage. The download host is
`raw.githubusercontent.com` (GitHub, Inc.); see the privacy policy
(`docs/legal/privacy-policy.md`, and Settings → Privacy in the app).

---

## 6. Open questions the publisher must close before release

- **O1 — smart-exception-java 0.2.1 / smart-exception-common 0.2.1** is a transitive dependency of
  FFmpegKit. No licence file is published in the Maven artefacts, and the presumed GitHub
  repository returns HTTP 404. Its licence is therefore **unknown**; its text is **not** shipped.
  Resolve with the maintainer or replace the dependency before release (blocker B7).
- **O2 — libiconv licence variant.** The bundled text is the GNU GPL-3.0 (the libiconv package
  COPYING); the libiconv library itself is distributed under LGPL-2.1. The LGPL-2.1 text is shipped
  as well. Confirm which variant the shipped binary is under.
- **O3 — OpenH264 patent scope.** The FFmpeg build includes OpenH264 (a built-in `libopenh264`
  encoder string is present in `libavcodec.so`). Cisco's royalty-free AVC/H.264 patent licence
  covers "this binary" as provided by Cisco; whether it extends to a third-party build must be
  confirmed by counsel.
- **O4 — upstream FFmpeg revision.** FFmpegKit's versioning scheme uses the upstream FFmpeg major
  and minor version, so 8.1.9 corresponds to an FFmpeg 8.1 build; the exact revision and configure
  flags of the fork's artefact have **not** been independently verified from this repository. They
  must be published with the Corresponding Source (§1.2) and are part of blocker B6/B7.

---

## 7. Reporting a licence problem

Write to `opensource@sigha.app` (placeholder — publisher must create it, see §1.2). For a missing
or wrong licence text in the app, that is a bug: report it and we will fix the shipped text.
