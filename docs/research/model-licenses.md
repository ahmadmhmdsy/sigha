# On-Device Model & AI-Pack Licence Audit — commercial-use clearance for the Android app

**Role:** Model Licensing Auditor (teammate model-auditor) · **Task:** task-4
**Project:** offline-first Android file converter inspired by the idea of Filee (new app, original code)
**User constraint being audited:** models must be usable **offline** or **downloadable on demand**, and must be **free for commercial use**.
**Evidence date:** all fetches performed **2026-11-16 (Asia/Baghdad)**, unless a different date is stated. Licence facts are what upstream published on that date.
**Status of this document:** factual technical/licence research, not legal advice. A lawyer/mufti review is a separate deliverable (task-1).

**Revision 2 (2026-11-16 23:56, after cross-check with android-stack/task-3):** Tesseract4Android 4.9.0 AAR size and SHA-256 are now **verified** (12,848,454 B); the earlier JitPack HEAD HTTP 500 was transient. Exact bundled upstream versions (Tesseract/Leptonica/libjpeg/libpng) are downgraded to **UNVERIFIED** (not readable from the AAR), and the AAR is confirmed to ship **no licence/notice file**, so the Apache-2.0 + BSD-2-Clause + IJG/libpng texts must be added by us. See §2.1 A4, §5.2, §7, Appendix C.

**Revision 2.1 (doc-sync with android-stack):** added the docs/research/android-stack.md §2.12 cross-reference for shared artifact pins, and the fact that **no per-ABI ONNX Runtime artifact exists on Maven Central**; verdicts and recommended packs unchanged.

> Evidence rule used here: every licence claim below was checked by fetching the upstream licence file or the Hugging Face model-card metadata on 2026-11-16. Where a fetch was impossible (gated repo, 401, missing file) the claim is marked **UNVERIFIED**. Model-quality claims were **not** tested in this audit.

---

## 0. Executive summary (read this first)

1. **The v1 "model" story can be licence-clean with only permissive licences.** OCR: Tesseract engines + traineddata are **Apache-2.0** (verified). Upscaling: Real-ESRGAN weights are **BSD-3-Clause** (verified) and ONNX Runtime is **MIT** (verified). No non-commercial weight is needed for v1.
2. **Recommended v1 packs:**
   * **Pack A (OCR, offline-first):** bundle tessdata_fast **eng + ara = 5,545,144 bytes ≈ 5.29 MiB** in the APK; offer tessdata_best **eng + ara = 28,004,325 bytes ≈ 26.71 MiB** as an on-demand "high-accuracy OCR" download. Engines: Tesseract4Android (Apache-2.0) + Tesseract 5.5.1 + Leptonica (BSD-2-Clause per upstream README).
   * **Pack B (Upscale, on-demand):** Real-ESRGAN General x4v3 **4,885,111 B ≈ 4.66 MiB**, x4plus **67,040,989 B ≈ 63.94 MiB**, anime 6B **17,938,799 B ≈ 17.11 MiB** (official .pth sizes verified; converted to ONNX at build time, ONNX Byte size not yet measured → **UNVERIFIED exact ONNX size**). Runtime: ONNX Runtime Android **MIT**, AAR **59,335,382 B ≈ 56.59 MiB** for **all ABIs** (per-ABI split size **UNVERIFIED**).
3. **Roadmap packs are also clean if chosen correctly:** Whisper weights **MIT** (OpenAI README says "code and model weights ... MIT"), whisper.cpp **MIT**; OPUS-MT en↔ar **Apache-2.0** (verified model cards); Vosk **Apache-2.0** code and several Arabic Vosk models **Apache-2.0**; Piper code **MIT** — but **per-voice Piper licences vary and several are unclear** → do not ship TTS voices until each MODEL_CARD is cleared.
4. **Forbidden in a commercial app:** NLLB-200 (**CC-BY-NC-4.0**), BRIA RMBG-1.4/2.0 (**non-commercial**), FLUX.1 [dev] (**non-commercial licence**), Upscayl app (**AGPL-3.0**), Vosk's 1.3 GB Arabic AGPL model, **Upscayl community model packs (no licence file at all)**, and any SD/OpenRAIL weight **if** you are not prepared to propagate its use-restrictions (not needed here).
5. **ML Kit is a legitimate but non-redistributable dependency:** on-device, free-of-charge **not found in the terms we fetched** (pricing = **UNVERIFIED**), models remain Google's, and ML Kit translation imposes **Google Translate attribution** and is limited to phones/tablets/laptops. ML Kit **Text Recognition does not support Arabic** (verified: Latin/Chinese/Devanagari/Japanese/Korean only), so it cannot replace Tesseract for the Arabic OCR use case.
6. **Distribution model:** keep the base download small (Play limits verified: base module **500 MB**, per asset pack **1.5 GB**, on-demand/fast-follow packs **30 GB** cumulative, total **34 GB**; APK-only legacy limit **100 MB**). Download model data only (never .so/.dex) from our own HTTPS host — Play policy forbids downloading executable code, and model weights are data, not code (policy text quoted in §3.4).
7. **Privacy is a hard requirement and is also good licensing optics:** no user file uploads; only model-pack downloads; explicit user action; size shown before download; Wi-Fi preference; **no silent network**; SHA-256 pinned; resumable; offline licence texts bundled.
8. **Build-stack flags handed to android-stack (revised after cross-check):** (a) Tesseract4Android 4.9.0 AAR is **verified**: 12,848,454 B ≈ 12.25 MiB, SHA-256 pinned in Appendix A (the earlier JitPack HEAD HTTP 500 was transient; the artifact downloads fine and the project build resolved it) — but the AAR contains **no licence/notice file**, so we must add the Apache-2.0 + Leptonica BSD-2-Clause + libjpeg/libpng texts ourselves; (b) ONNX Runtime onnxruntime-mobile is stale (Maven latest **1.18.0**, and 1.32.0 AAR path 404) — use onnxruntime-android **1.32.0**.
9. **Decisions the Lead should make** are listed in §3.6 (bundle-fast-OCR vs download-only; include ONNX Runtime in base; ML Kit vs self-hosted translation; background-removal route; TTS route).
10. **Notice duties are modest but real:** Apache-2.0 (Tesseract data/engine, Tesseract4Android, OPUS-MT, sherpa-onnx, Vosk), BSD-3-Clause (Real-ESRGAN: keep copyright + 3-clause text + no endorsement), MIT (ONNX Runtime, Whisper, whisper.cpp, Piper code), plus **Google Translate attribution** if ML Kit translation is used. Templates and placement are in §5.

---

## 1. Method, evidence classes, and licence terminology

**Fetched evidence.** Every row below cites the upstream URL. Fetch status is recorded in Appendix C. For sizes I used, in order of preference: (a) exact Content-Length from an HTTP HEAD/GET, (b) GitHub Contents API size, (c) Hugging Face tree API size. Exact Byte counts are authoritative; MiB figures are rounded to 2 dp (1 MiB = 1,048,576 B).

**Commercial-use verdict vocabulary** (applies to **shipping the weight in/with the app**, including on-demand download from our own server):

| Verdict | Meaning |
|---|---|
| **ALLOWED** | Permissive licence; commercial use and redistribution permitted; keep the licence text/attribution the licence requires. |
| **ALLOWED-WITH-ATTRIBUTION** | Commercial use permitted, but an exact notice/attribution is required (Apache-2.0, BSD-3-Clause, MIT, CC-BY-4.0, MPL-2.0 source-availability for covered files). Exact notices in §5. |
| **ALLOWED-WITH-RESTRICTIONS** | Commercial use permitted only while we propagate a use-restriction licence to users (CreativeML OpenRAIL-M / OpenRAIL++). Not recommended for this app. |
| **PROPRIETARY-USE-ONLY** | Usable as a service/dependency, but weights are Google's; cannot be extracted or redistributed. Commercial use governed by Google terms. |
| **UNVERIFIED** | No authoritative licence statement found for the **weights** (even if the code repo is licensed); do not ship until cleared in writing. |
| **FORBIDDEN** | Cannot be shipped/downloaded in a commercial app under its current licence (non-commercial, AGPL, or no licence). |

**Two warnings that shaped the table:**
* A permissive **code** repository does not automatically license separately-hosted **weights**. Each row states how the weights are distributed and on what basis.
* Hugging Face re-uploads of famous weights carry **inconsistent licence metadata**. Example: search for "Real-ESRGAN" returned re-uploads tagged bsd-3-clause, openrail, mit, and creativeml-openrail-m (fetched 2026-11-16). **Only the official upstream source is accepted as licence evidence.** Downloaded artifacts must be pinned to official URLs + SHA-256.

---

## 2. Candidate matrix (role | files & size | runtime | SPDX + licence link | commercial | redistribution | source | SHA-256 plan | notes)

Legend for **SHA-256 plan**: **PIN** = hash pinned in app model manifest and verified after download; **PIN+** = additionally published in the repo/legal docs; **BUNDLE** = verified as a build-time artifact before packaging.

### 2.1 OCR (Tesseract family) — v1 Pack A

| # | Role | Model file(s) + exact size | Runtime | Licence (SPDX) + licence link | Commercial | Redistribution | Source URL | SHA-256 plan | Notes |
|---|---|---|---|---|---|---|---|---|---|
| A1 | Arabic + English OCR language data, **best accuracy** | **ara.traineddata** 12,603,724 B ≈ 12.02 MiB; **eng.traineddata** 15,400,601 B ≈ 14.69 MiB; **both = 28,004,325 B ≈ 26.71 MiB**. Optional **osd.traineddata** 10,562,727 B ≈ 10.07 MiB | Tesseract (native, via Tesseract4Android) | **Apache-2.0** — https://github.com/tesseract-ocr/tessdata_best/blob/main/LICENSE (fetched raw, 10,173 B) | **ALLOWED-WITH-ATTRIBUTION** | Yes | https://github.com/tesseract-ocr/tessdata_best | **PIN+** (hashes in Appendix A) | README: "These models only work with the LSTM OCR engine of Tesseract 4." Tesseract 5.5.1 (inside Tesseract4Android 4.9.0) has the LSTM engine, so format-compatible. Arabic uses the Arabic script model ara; no separate Arabic model needed. No NOTICE file in repo root at fetch date. |
| A2 | OCR language data, **small/fast** | **ara.traineddata** 1,432,056 B ≈ 1.37 MiB; **eng.traineddata** 4,113,088 B ≈ 3.92 MiB; **both = 5,545,144 B ≈ 5.29 MiB** | Tesseract (native) | **Apache-2.0** — https://github.com/tesseract-ocr/tessdata_fast/blob/main/LICENSE (fetched raw) | **ALLOWED-WITH-ATTRIBUTION** | Yes | https://github.com/tesseract-ocr/tessdata_fast | **BUNDLE** | Smallest licence-clean Arabic OCR pack; ideal for bundling in the APK (offline out-of-box). Same Apache-2.0 obligations as A1. |
| A3 | OCR language data, "standard" | **eng** 23,466,654 B ≈ 22.38 MiB; **ara** 2,494,806 B ≈ 2.38 MiB | Tesseract (native) | **Apache-2.0** — https://github.com/tesseract-ocr/tessdata/blob/main/LICENSE (fetched raw) | **ALLOWED-WITH-ATTRIBUTION** | Yes | https://github.com/tesseract-ocr/tessdata | not planned | Standard repo mixes legacy+LSTM data; larger English file than "best" — no reason to prefer it over A1/A2. Listed for completeness. |
| A4 | OCR engine wrapper | AAR **12,848,454 B ≈ 12.25 MiB** (verified; SHA-256 in Appendix A) | Native .so + Java (upstream versions per README: Tesseract 5.5.1, Leptonica 1.85.0, libjpeg v9f, libpng 1.6.48 — **not verifiable from the AAR itself → UNVERIFIED**) | **Apache-2.0** — https://github.com/adaptech-cz/Tesseract4Android/blob/master/LICENSE (fetched raw) | **ALLOWED-WITH-ATTRIBUTION** | Yes (link/vendor) | https://github.com/adaptech-cz/Tesseract4Android (latest tag **4.9.0**, GitHub API) | n/a (code dependency) | Maven: cz.adaptech.tesseract4android:tesseract4android:4.9.0 per README. Supports Android 5.0 (API 21)+. README lists bundled third-party libs "with their own specific licenses": Tesseract (Apache-2.0), Leptonica (**BSD-2-Clause per README**; direct licence file 404 → **UNVERIFIED text**), libjpeg, libpng. Root listing shows **no NOTICE** file; AAR contains only classes.jar + annotations.zip and **no licence/notice file** (android-stack, 2026-11-16) → notices must be added by the app. |
| A5 | Tesseract engine (inside A4) | n/a | Native | **Apache-2.0** — https://github.com/tesseract-ocr/tesseract/blob/main/LICENSE (fetched raw) | **ALLOWED-WITH-ATTRIBUTION** | Yes | https://github.com/tesseract-ocr/tesseract | n/a | Include Tesseract attribution when shipping A4. |
| A6 | Google ML Kit Text Recognition v2 (alternative OCR) | Model downloaded/bundled by ML Kit; exact size per script documented by Google (≈4 MB bundled / ≈260 KB unbundled per script per architecture) | Google Play services / bundled native | Proprietary; **ML Kit Terms of Service** — https://developers.google.com/ml-kit/terms (fetched 2026-11-16) | **PROPRIETARY-USE-ONLY** (commercial use allowed under Google terms; see caveat) | **No** — weights are Google's, may not be extracted/redistributed | https://developers.google.com/ml-kit/vision/text-recognition/v2 | n/a | **Does not support Arabic** (verified supported scripts: Chinese, Devanagari, Japanese, Korean, Latin). Cannot serve the Arabic OCR requirement. Metrics/telemetry disclosure duties per ML Kit terms. |
| A7 | PaddleOCR / Paddle-Lite (alternative OCR engine) | model sizes not audited | native (C++) | **Apache-2.0** — https://github.com/PaddlePaddle/PaddleOCR/blob/main/LICENSE (fetched raw) | **ALLOWED-WITH-ATTRIBUTION** | Yes | https://github.com/PaddlePaddle/PaddleOCR | not planned | Licence-clean fallback engine; Arabic accuracy and Android integration **UNVERIFIED**; not needed for v1 because Tesseract covers Arabic. |

### 2.2 Speech-to-text / audio (roadmap Pack C)

| # | Role | Model file(s) + exact size | Runtime | Licence (SPDX) + licence link | Commercial | Redistribution | Source URL | SHA-256 plan | Notes |
|---|---|---|---|---|---|---|---|---|---|
| C1 | Whisper weights (tiny/base/small), ggml | **ggml-tiny.bin** 77,691,713 B ≈ 74.09 MiB; **ggml-tiny-q5_1.bin** 32,152,673 B ≈ 30.66 MiB; **ggml-base.bin** 147,951,465 B ≈ 141.10 MiB; **ggml-base-q5_1.bin** 59,707,625 B ≈ 56.94 MiB; **ggml-small.bin** 487,601,967 B ≈ 465.01 MiB; **ggml-small-q5_1.bin** 190,085,487 B ≈ 181.28 MiB | whisper.cpp (native JNI) or sherpa-onnx | **MIT** for code **and weights** — https://github.com/openai/whisper/blob/main/LICENSE (fetched raw); openai/whisper README: "Whisper's code and model weights are released under the MIT License." | **ALLOWED-WITH-ATTRIBUTION** | Yes | https://huggingface.co/ggerganov/whisper.cpp (HF metadata license: mit, fetched 2026-11-16) | **PIN+** | Official ggml conversions, MIT. tiny q5_1 (30.66 MiB) is the realistic mobile pack; base q5_1 (56.94 MiB) is "quality" option. Model files are data. |
| C2 | whisper.cpp runtime | n/a | Native .so (C++) | **MIT** — https://github.com/ggml-org/whisper.cpp/blob/master/LICENSE (fetched raw; "Copyright (c) 2023-2026 The ggml authors") | **ALLOWED-WITH-ATTRIBUTION** | Yes | https://github.com/ggml-org/whisper.cpp | n/a | MIT permits commercial shipping; include MIT text + copyright line. |
| C3 | Whisper ONNX (alternative runtime path) | Tiny int8: encoder 10,124,977 B + decoder_merged_int8 30,719,241 B ≈ **40,844,218 B ≈ 38.95 MiB**; base int8: 23,201,297 + 53,693,315 = **76,894,612 B ≈ 73.33 MiB** (HF tree sizes, onnx-community/whisper-tiny and -base, fetched 2026-11-16) | ONNX Runtime Android | Underlying weights **MIT** (openai/whisper). HF conversion repo metadata **inconsistent**: onnx-community/whisper-tiny has **no licence metadata**; onnx-community/whisper-base is tagged **apache-2.0** — not authoritative for the weights. | **ALLOWED-WITH-ATTRIBUTION** only if we export from MIT weights ourselves or pin a repo whose licence we accept; **UNVERIFIED** for the third-party ONNX repo as-is | conversion repo adds files but cannot relicense OpenAI weights; safest is our own export | https://huggingface.co/onnx-community/whisper-tiny ; https://huggingface.co/onnx-community/whisper-base | **PIN** | Prefer ggml route (C1/C2) for licence certainty and simplicity; ONNX route needs encoder+decoder orchestration and is larger in fp32. |
| C4 | sherpa-onnx runtime (ASR + TTS + Android) | n/a | Native AAR/JNI | **Apache-2.0** — https://github.com/k2-fsa/sherpa-onnx/blob/master/LICENSE (fetched raw) | **ALLOWED-WITH-ATTRIBUTION** | Yes | https://github.com/k2-fsa/sherpa-onnx | n/a | Supports Whisper models, VITS/Piper-style TTS, Android; one runtime could cover speech + TTS later. Uses ONNX Runtime underneath. |
| C5 | Vosk runtime + Arabic models (alternative ASR) | Arabic: **vosk-model-ar-mgb2-0.4** **318 MB** (Apache-2.0), **vosk-model-small-ar-tn-0.1-linto** **158 MB** (Apache-2.0), **vosk-model-ar-0.22-linto-1.1.0** **1.3 GB — AGPL (do not ship)**; English small 40 MB Apache-2.0 | Native JNI | Code: **Apache-2.0** — https://github.com/alphacep/vosk-api/blob/master/COPYING (fetched raw). Model licences vary per model and are listed on https://alphacephei.com/vosk/models (fetched 2026-11-16). | **ALLOWED-WITH-ATTRIBUTION** for Apache-2.0 models only | Yes for Apache-2.0 models | https://alphacephei.com/vosk/models | **PIN+** | Vosk models page lists licences per model: Apache-2.0, MIT, CC-BY-NC-SA, LGPL-3.0, GPLv3, AGPL. **Never use the AGPL/NC models.** Arabic Apache-2.0 models are large (158–318 MB) → Whisper tiny is smaller. |
| C6 | ML Kit language identification | model managed by ML Kit | Google Play services | Proprietary, ML Kit ToS (link above) | **PROPRIETARY-USE-ONLY** | No | https://developers.google.com/ml-kit/language/identification | n/a | Optional; keep out of v1. |

### 2.3 Image upscaling — v1 Pack B

| # | Role | Model file(s) + exact size | Runtime | Licence (SPDX) + licence link | Commercial | Redistribution | Source URL | SHA-256 plan | Notes |
|---|---|---|---|---|---|---|---|---|---|
| B1 | Real-ESRGAN **General x4 v3** (small/fast) | **realesr-general-x4v3.pth** **4,885,111 B ≈ 4.66 MiB** (GitHub release asset size; downloaded + hashed) | ONNX Runtime Android after build-time export (or Qualcomm AI Hub ONNX/TFLite export) | **BSD-3-Clause** — https://github.com/xinntao/Real-ESRGAN/blob/master/LICENSE (fetched raw; "Copyright (c) 2021, Xintao Wang") | **ALLOWED-WITH-ATTRIBUTION** | Yes | https://github.com/xinntao/Real-ESRGAN/releases/tag/v0.2.5.0 | **PIN+** (hash in Appendix A) | Fastest licence-clean option; quality below x4plus. BSD-3 requires copyright + conditions + disclaimer; no endorsement. ONNX exact Byte size **UNVERIFIED** until we export; expected ≈ .pth size in fp32. Qualcomm AI Hub ONNX w8a8 bundle (zip) is **24,952,432 B ≈ 23.80 MiB** (HEAD, fetched) — reference for quantized size, licence file points back to Real-ESRGAN BSD-3. |
| B2 | Real-ESRGAN **x4plus** (quality) | **RealESRGAN_x4plus.pth** **67,040,989 B ≈ 63.94 MiB** (downloaded + hashed) | ONNX Runtime Android (export) | **BSD-3-Clause** (same licence file) | **ALLOWED-WITH-ATTRIBUTION** | Yes | https://github.com/xinntao/Real-ESRGAN/releases/tag/v0.1.0 | **PIN+** (hash in Appendix A) | Reference model; needs tiled inference on mobile (memory). Qualcomm AI Hub float ONNX zip **62,174,027 B ≈ 59.29 MiB** (HEAD) ≈ fp32 ONNX size sanity check. |
| B3 | Real-ESRGAN **anime 6B** (digital art) | **RealESRGAN_x4plus_anime_6B.pth** **17,938,799 B ≈ 17.11 MiB** | ONNX Runtime Android | **BSD-3-Clause** (same) | **ALLOWED-WITH-ATTRIBUTION** | Yes | https://github.com/xinntao/Real-ESRGAN/releases/tag/v0.2.2.4 | **PIN+** | Smaller quality model for anime/illustration; optional pack. |
| B4 | Real-ESRGAN **x2plus** | **RealESRGAN_x2plus.pth** **67,061,725 B ≈ 63.95 MiB** (release asset size, not downloaded/hashed) | ONNX Runtime Android | **BSD-3-Clause** | **ALLOWED-WITH-ATTRIBUTION** | Yes | https://github.com/xinntao/Real-ESRGAN/releases/tag/v0.2.1 | PIN | Needed only for true 2× upscaling; otherwise x4 + downscale. |
| B5 | ONNX Runtime Android (runtime, base APK) | AAR **59,335,382 B ≈ 56.59 MiB** (all ABIs) | ONNX Runtime | **MIT** — https://github.com/microsoft/onnxruntime/blob/main/LICENSE (fetched raw) | **ALLOWED-WITH-ATTRIBUTION** | Yes | https://repo1.maven.org/maven2/com/microsoft/onnxruntime/onnxruntime-android/maven-metadata.xml (latest **1.32.0**) | n/a | com.microsoft.onnxruntime:onnxruntime-android:1.32.0. Play ABI splits reduce the per-device download (**per-ABI size UNVERIFIED**). onnxruntime-mobile alternative is stale (Maven latest 1.18.0; 1.32.0 AAR path 404) → do not plan on it. |
| B6 | ESRGAN (original, 4×) | **ESRGAN_SRx4_DF2KOST_official-ff704c30.pth** **66,922,040 B** (release asset size) | ONNX Runtime | Code repo **Apache-2.0** — https://github.com/xinntao/ESRGAN/blob/master/LICENSE (fetched raw); no separate weights licence found | **ALLOWED-WITH-ATTRIBUTION** (repo licence covers distribution; re-check weights position) | Yes | https://github.com/xinntao/Real-ESRGAN/releases/tag/v0.1.1 (asset) / https://github.com/xinntao/ESRGAN | PIN | Older architecture; no advantage over B1–B3. Listed as a fallback. |
| B7 | SwinIR | model sizes not audited | ONNX Runtime (export) | **Apache-2.0** — https://github.com/JingyunLiang/SwinIR/blob/main/LICENSE (fetched raw) | **ALLOWED-WITH-ATTRIBUTION** | Yes | https://github.com/JingyunLiang/SwinIR | PIN | Heavy (transformer) → poor mobile candidate; listed for completeness. |
| B8 | waifu2x | model sizes not audited; weights hosted separately from repo | native / ONNX export | Code **MIT** — https://github.com/nagadomi/waifu2x/blob/master/LICENSE (fetched raw); **weights licence UNVERIFIED** | **UNVERIFIED** for weights | Do not ship until cleared | https://github.com/nagadomi/waifu2x | — | Code MIT is clear; the waifu2x model weights are distributed from the project's model site with no licence statement found in this audit. Not recommended. |
| B9 | Upscayl **app** and **custom-models** repo | ncnn .bin/.param community models (e.g. 4xLSDIR 33,424,520 B; 4x_NMKD-Siax_200k 66,793,352 B; RealESRGAN_General_x4_v3 2,435,272 B) | ncnn (not ONNX) | App: **AGPL-3.0** — https://github.com/upscayl/upscayl/blob/main/LICENSE (fetched raw). Custom-models repo: **no LICENSE file** at root on 2026-11-16 (root listing: README.md + models/ only); README credits authors but states no per-model licence. | **FORBIDDEN** to link/copy app code (AGPL-3.0). Community models: **UNVERIFIED → treat as NOT CLEARED** | No (unless each model's own licence is obtained) | https://github.com/upscayl/custom-models | — | Do not use this repo as a model source. If a specific community model is ever wanted (e.g. LSDIR family), obtain the author's written licence from the original source and record it. |

### 2.4 Background removal / segmentation

| # | Role | Model file(s) + exact size | Runtime | Licence (SPDX) + licence link | Commercial | Redistribution | Source URL | SHA-256 plan | Notes |
|---|---|---|---|---|---|---|---|---|---|
| D1 | U²-Net (full) | **u2net.onnx** **175,997,641 B ≈ 167.85 MiB** (rembg release asset, HEAD) | ONNX Runtime Android | Code/weights repo **Apache-2.0** — https://github.com/xuebinqin/U-2-Net/blob/master/LICENSE (fetched raw) | **ALLOWED-WITH-ATTRIBUTION** | Yes | https://github.com/xuebinqin/U-2-Net ; ONNX conversion hosted at https://github.com/danielgatis/rembg/releases/tag/v0.0.0 | PIN+ | Large for mobile. ONNX conversion hosted by rembg (MIT code); conversion adds no new rights, but the exact ONNX file's provenance/licence is not separately stated → record Apache-2.0 attribution for U²-Net and pin the file. |
| D2 | U²-Net**p** (portable) | **u2netp.onnx** **4,574,861 B ≈ 4.36 MiB** (rembg release asset, HEAD) | ONNX Runtime Android | **Apache-2.0** (U²-Net repo, as D1) | **ALLOWED-WITH-ATTRIBUTION** | Yes | as D1 | PIN+ | The only realistically mobile-sized licence-clean background remover found. Quality lower than full U²-Net; not benchmarked here. |
| D3 | IS-Net / DIS (general use) | **isnet-general-use.onnx** **178,648,008 B ≈ 170.37 MiB** (rembg release asset, HEAD) | ONNX Runtime Android | **Apache-2.0** — https://github.com/xuebinqin/DIS/blob/main/LICENSE.md (fetched raw; Apache-2.0 text) | **ALLOWED-WITH-ATTRIBUTION** | Yes | https://github.com/xuebinqin/DIS ; ONNX at rembg v0.0.0 | PIN+ | Very large; DIS5K dataset has separate Terms of Use (PDF in repo) — those are **dataset** terms, not fetched; weight licence in repo is Apache-2.0. Residual question for legal: dataset-terms effect on trained weights → **UNVERIFIED**, low risk but note it. |
| D4 | BiRefNet (full) | **model.onnx** **972,666,916 B ≈ 927.61 MiB** fp32; **model_fp16.onnx** **489,666,272 B ≈ 466.98 MiB** (HF tree) | ONNX Runtime Android | **MIT** — https://github.com/ZhengPeng7/BiRefNet/blob/main/LICENSE (fetched raw; "Copyright (c) 2024 ZhengPeng"); HF metadata license: mit | **ALLOWED-WITH-ATTRIBUTION** | Yes | https://huggingface.co/onnx-community/BiRefNet-ONNX | PIN | Too large for phones as a default pack. |
| D5 | BiRefNet **lite** | **model.onnx** **224,005,088 B ≈ 213.63 MiB** fp32; **model_fp16.onnx** **114,538,221 B ≈ 109.23 MiB** | ONNX Runtime Android | **MIT** (as D4) | **ALLOWED-WITH-ATTRIBUTION** | Yes | https://huggingface.co/onnx-community/BiRefNet_lite-ONNX | PIN | Still heavy; candidate for high-end devices only. |
| D6 | BRIA RMBG-1.4 / RMBG-2.0 | n/a | — | **Non-commercial** ("other" — bria-rmbg-1.4): model card says "**source-available model for non-commercial use**" — https://huggingface.co/briaai/RMBG-1.4 (fetched 2026-11-16) | **FORBIDDEN** | **No** | https://huggingface.co/briaai/RMBG-1.4 | — | Non-commercial licence; needs a paid BRIA agreement (not priced/verified here). Do not ship. |
| D7 | Google ML Kit Subject Segmentation | model managed/downloaded by Google Play services; size not stated in fetched docs | Google Play services | Proprietary, ML Kit ToS (fetched) | **PROPRIETARY-USE-ONLY** | No | https://developers.google.com/ml-kit/vision/subject-segmentation/android (fetched; requires API 24+, unbundled library) | n/a | Licence-clean dependency route for background removal without shipping weights; requires GMS; model download at/after install (user-facing) → not fully offline-first until downloaded. |
| D8 | LaMa (inpainting / object eraser) | checkpoint not audited (big-lama distributed via Google Drive + smartywu/big-lama HF mirror per README) | ONNX Runtime (export; heavy) | Code repo **Apache-2.0** — https://github.com/advimman/lama/blob/main/LICENSE (fetched raw). **Weights licence not stated** in repo/README (README only gives download links) | **UNVERIFIED** for weights | Do not ship until cleared | https://github.com/advimman/lama | — | Code is clean; weights need written clarification. Roadmap only. |
| D9 | MI-GAN (inpainting) | weights not audited | ONNX Runtime | Code **MIT** — https://github.com/Picsart-AI-Research/MI-GAN/blob/main/LICENSE (fetched raw); weights/ONNX not audited | **UNVERIFIED** for weights | Do not ship until cleared | https://github.com/Picsart-AI-Research/MI-GAN | — | Potential lighter eraser; licence audit incomplete. |

### 2.5 Translation (roadmap Pack D)

| # | Role | Model file(s) + exact size | Runtime | Licence (SPDX) + licence link | Commercial | Redistribution | Source URL | SHA-256 plan | Notes |
|---|---|---|---|---|---|---|---|---|---|
| T1 | OPUS-MT **en→ar** | **pytorch_model.bin** **307,574,661 B ≈ 293.33 MiB**; tokenizer **source.spm** 801,074 B, **target.spm** 916,890 B, **vocab.json** 2,117,975 B; ONNX int8 reference: encoder 51,600,664 B + decoder_merged_int8 58,878,333 B ≈ **110,478,997 B ≈ 105.36 MiB** (Xenova/opus-mt-en-ar tree) | ONNX Runtime Android (export with Optimum; SentencePiece tokenizer in app) | **Apache-2.0** — model card licence field verified: https://huggingface.co/Helsinki-NLP/opus-mt-en-ar (fetched 2026-11-16, license: apache-2.0) | **ALLOWED-WITH-ATTRIBUTION** | Yes | https://huggingface.co/Helsinki-NLP/opus-mt-en-ar | PIN+ | Card: OPUS readme eng-ara, transformer, SentencePiece spm32k. Model quality not benchmarked here; news-domain training data may suit document text better than chat. |
| T2 | OPUS-MT **ar→en** | **pytorch_model.bin** **307,640,325 B ≈ 293.39 MiB**; ONNX int8: encoder 51,617,048 + decoder_merged_int8 58,894,845 ≈ **110,511,893 B ≈ 105.39 MiB** (Xenova/opus-mt-ar-en tree) | ONNX Runtime Android | **Apache-2.0** — verified card: https://huggingface.co/Helsinki-NLP/opus-mt-ar-en | **ALLOWED-WITH-ATTRIBUTION** | Yes | https://huggingface.co/Helsinki-NLP/opus-mt-ar-en | PIN+ | Both directions ≈ **221 MB** int8 on disk — a chunky pack; download on demand only. If ONNX sizes must shrink, quantize further (q4) but re-validate quality/licence (quantization of Apache-2.0 weights keeps licence). |
| T3 | ML Kit on-device Translation (no weights shipped) | dynamic language packs; size not stated in fetched docs (**UNVERIFIED**) | Google Play services / on-device | Proprietary, **ML Kit ToS** + **Usage guidelines for on-device translation** (fetched 2026-11-16): app must comply with **Google Cloud Translation API attribution requirements** (layout, Google attribution, branding); API is restricted to **smartphones, tablets, laptops/desktops** — not embedded devices (cars/TVs) without written permission | **PROPRIETARY-USE-ONLY** | No | https://developers.google.com/ml-kit/language/translation/translation-terms ; supported languages: https://developers.google.com/ml-kit/language/translation/translation-language-support (includes ar Arabic and en English) | n/a | **No model-licence burden**; requires Google Play services; not fully offline on first use (model download). Attribution obligations are real (see §5.4). Pair-level ar↔en availability **UNVERIFIED** (both languages are listed; per-pair listing not fetched). |
| T4 | Argos Translate | **.argosmodel** packages (en_ar / ar_en exist; sizes not audited) | CTranslate2 (Python) — **not an Android runtime** without porting | Code **MIT or CC0** (dual) — https://github.com/argosopentech/argos-translate/blob/master/LICENSE (fetched raw; README confirms dual MIT/CC0). Model packages: **argospm-index has no licence field** (index.json fetched 2026-11-16: only package_version, argos_version, codes, names, links) → model licences **UNVERIFIED** | **UNVERIFIED** for models (code is fine) | No, until per-model licence is established | https://github.com/argosopentech/argos-translate ; https://github.com/argosopentech/argospm-index | — | Also Python-centric (CTranslate2 Python bindings); Android port is non-trivial. Not recommended. |
| T5 | Mozilla Bergamot / Firefox Translations | ar↔en models exist; ar-en model file uncompressed **42,992,955 B ≈ 41.00 MiB** (model registry JSON, fetched 2026-11-16; .gz on GCS) | bergamot-translator (native; MPL-2.0) | Runtime **MPL-2.0** — https://github.com/browsermt/bergamot-translator/blob/main/LICENSE (fetched raw). Model repo LICENSE **MPL-2.0** — https://github.com/mozilla/firefox-translations-models/blob/main/LICENSE (fetched raw). Per-model licence is **not stated** in the model registry JSON → **UNVERIFIED** for weights | **UNVERIFIED** for the weights (MPL-2.0 "source" obligations for model binaries are also legally unclear) | Needs legal review | https://github.com/mozilla/firefox-translations-models ; registry https://storage.googleapis.com/moz-fx-translations-data--303e-prod-translations-data/db/models.json | PIN after clearance | Interesting (small, fast), but MPL-2.0 source-availability on model binaries is an open question → not a v1 choice. |
| T6 | NLLB-200 (Meta) | e.g. 600M distilled (~2.4 GB fp32) — not needed | — | **CC-BY-NC-4.0** — https://huggingface.co/facebook/nllb-200-distilled-600M (metadata license: "cc-by-nc-4.0", fetched 2026-11-16) | **FORBIDDEN** | No | https://huggingface.co/facebook/nllb-200-distilled-600M | — | Non-commercial only. Also enormous. |

### 2.6 Text-to-speech (roadmap; only after per-voice clearance)

| # | Role | Model file(s) + exact size | Runtime | Licence (SPDX) + licence link | Commercial | Redistribution | Source URL | SHA-256 plan | Notes |
|---|---|---|---|---|---|---|---|---|---|
| V1 | Piper runtime | n/a | Native (piper.cpp / sherpa-onnx) | **MIT** — https://github.com/rhasspy/piper/blob/master/LICENSE.md (fetched raw; "Copyright (c) 2022 Michael Hansen") | **ALLOWED-WITH-ATTRIBUTION** | Yes | https://github.com/rhasspy/piper | n/a | Code clean. Voice licences are separate — see V2. |
| V2 | Piper voices (Arabic + English) | e.g. **ar_JO-kareem-medium.onnx** **63,201,294 B ≈ 60.27 MiB**; **ar_JO-kareem-low.onnx** **63,201,294 B**; **en_US-lessac-medium.onnx** **63,201,294 B** (HF tree sizes) | Native / sherpa-onnx | Voices repo metadata license: mit (HF card) **but each voice's MODEL_CARD cites its own dataset licence**: en_GB-alba-medium → **CC-BY-4.0**; en_US-lessac-* → Blizzard 2013 licence URL (**text not fetched**); ar_JO-kareem-* → "**See URL**" (Arabic TTS dataset repo; **UNVERIFIED**); en_US-amy-medium → "See URL" | **UNVERIFIED per voice** (repo-level MIT is not enough evidence for a profit-making ship) | Not until per-voice clearance | https://huggingface.co/rhasspy/piper-voices (MODEL_CARDs fetched 2026-11-16) | PIN after clearance | Voice files are all ~63.20 MB at medium/low for the sampled voices. For v1 use the **Android platform TextToSpeech** engine instead (no model redistribution, no voice-licence issue; offline voices depend on device/OEM). |
| V3 | Android platform TTS | n/a | OS service | Device/OEM-provided; app redistributes nothing | ALLOWED (no third-party weights shipped) | n/a | https://developer.android.com/reference/android/speech/tts/TextToSpeech | n/a | Dependency-free route for v1; quality/offline availability per device — verify on target devices. |

### 2.7 Generative-image models (only to document what to avoid)

| # | Role | Licence | Commercial | Redistribution | Source | Verdict |
|---|---|---|---|---|---|---|
| G1 | Stable Diffusion 1.4/1.5 family | **CreativeML OpenRAIL-M** (licence text fetched: "Copyright (c) 2022 Robin Rombach and Patrick Esser and contributors … CreativeML Open RAIL-M dated August 22, 2022") | Permitted **with use-based restrictions**; users must be made to comply (Attachment A); must pass licence on | Yes with restrictions | https://huggingface.co/CompVis/stable-diffusion-v1-4 ; licence text https://huggingface.co/spaces/CompVis/stable-diffusion-license (license.txt fetched) | **ALLOWED-WITH-RESTRICTIONS** — not needed for a file converter; do not add. |
| G2 | Stable Diffusion XL | **OpenRAIL++-M** (HF metadata openrail++, fetched) | Similar restrictions | Yes with restrictions | https://huggingface.co/stabilityai/stable-diffusion-xl-base-1.0 | **ALLOWED-WITH-RESTRICTIONS** — not needed. |
| G3 | FLUX.1 [dev] | **FLUX.1 [dev] Non-Commercial License v1.1.1** — "freely available for your non-commercial and non-production use" (licence file fetched from BFL GitHub model_licenses/LICENSE-FLUX1-dev; HF repo is gated, raw licence 401) | **NO** | No | https://github.com/black-forest-labs/flux/blob/main/model_licenses/LICENSE-FLUX1-dev | **FORBIDDEN** for a commercial app. |
| G4 | FLUX.1 [schnell] | **Apache-2.0** (HF metadata license: apache-2.0, fetched) | Yes | Yes | https://huggingface.co/black-forest-labs/FLUX.1-schnell | ALLOWED but **not needed**; huge and irrelevant to file conversion. |

### 2.8 ML Kit platform facts (for the feature-decision record, not a model pack)

| Fact | Evidence (fetched 2026-11-16) |
|---|---|
| ML Kit terms incorporate **Google APIs Terms of Service**; "machine learning models will be considered related software"; APIs run on-device; **may not reverse engineer or extract**; APIs may contact Google servers for fixes/updated models and send **usage metrics** (app must disclose per law) | https://developers.google.com/ml-kit/terms |
| Text Recognition v2 scripts: **Chinese, Devanagari, Japanese, Korean, Latin** — **no Arabic** | https://developers.google.com/ml-kit/vision/text-recognition/v2 |
| Translation: 50+ languages, dynamic model downloads, same models as Google Translate offline mode; **Arabic (ar) in supported list** | https://developers.google.com/ml-kit/language/translation ; https://developers.google.com/ml-kit/language/translation/translation-language-support |
| Translation usage guidelines: must follow **Cloud Translation API attribution requirements** (layout, Google attribution, branding); limited to smartphones/tablets/laptops/desktops; no embedded devices without written permission | https://developers.google.com/ml-kit/language/translation/translation-terms ; https://cloud.google.com/translate/attribution |
| Subject segmentation: API 24+, **unbundled** library downloaded via Google Play services | https://developers.google.com/ml-kit/vision/subject-segmentation/android |
| Pricing: **no fee/no-charge clause found** in the ML Kit terms fetched → **UNVERIFIED (pricing)**. Treat "free" as a business assumption to be confirmed with Google/legal, not a licence fact. | same terms page |

---

## 3. Recommended v1 model packs and distribution strategy

### 3.1 Pack A — OCR (v1, ships with app + optional high-accuracy download)

* **Engine:** Tesseract4Android 4.9.0 (Apache-2.0), AAR **12,848,454 B ≈ 12.25 MiB** (SHA-256 in Appendix A). README says it bundles Tesseract 5.5.1 (Apache-2.0) + Leptonica (BSD-2-Clause) + libjpeg/libpng; those exact versions are **README claims — not verifiable inside the AAR (UNVERIFIED)**. The AAR ships **no licence/notice file**, so we add the Apache-2.0 text + Leptonica BSD-2-Clause text + IJG/libpng notices ourselves.
* **Bundled in the APK/AAB (out-of-box offline):** tessdata_fast **eng + ara** = **5,545,144 B ≈ 5.29 MiB** (verified sizes).
  * Sources: https://raw.githubusercontent.com/tesseract-ocr/tessdata_fast/main/eng.traineddata (4,113,088 B) and .../ara.traineddata (1,432,056 B).
* **On-demand download ("High-accuracy OCR pack"):** tessdata_best **eng + ara** = **28,004,325 B ≈ 26.71 MiB**.
  * Sources: https://raw.githubusercontent.com/tesseract-ocr/tessdata_best/main/eng.traineddata (15,400,601 B) and .../ara.traineddata (12,603,724 B).
  * Exact hashes (downloaded and computed by this audit, 2026-11-16) are in Appendix A — use them as the initial pinned values.
* **Licence notices:** Apache-2.0 for tessdata + Tesseract + Tesseract4Android; BSD-2-Clause notice for Leptonica (confirm text at build); libjpeg/libpng notices (see §5).
* **Why fast bundled + best downloadable:** the user gets a genuinely offline OCR immediately (5.3 MiB), and accuracy can be upgraded without an app update; best is 5× larger and worse for first-run experience.

### 3.2 Pack B — Upscale (v1, on-demand; runtime in base)

* **Runtime:** ONNX Runtime Android 1.32.0 (MIT), AAR 59,335,382 B ≈ 56.59 MiB all ABIs. Keep it in the base app only if upscaling is a launch feature; ABI splits should be enabled (per-ABI size **UNVERIFIED**). There is **no per-ABI Maven artifact** on Maven Central (android-stack, 2026-11-16) — the only dependency is the all-ABI AAR, so per-device size reduction comes from ABI splits at packaging, not from a smaller artifact. Pin duplicated in docs/research/android-stack.md §2.12; update both docs together.
* **Weights (BSD-3-Clause, official Real-ESRGAN releases), converted to ONNX at build time:**
  * "Fast" pack: realesr-general-x4v3.pth **4,885,111 B**; hash pinned in Appendix A. ONNX w8a8 export ≈ 23–25 MB (Qualcomm AI Hub w8a8 zip 24,952,432 B) — exact ONNX size **UNVERIFIED**.
  * "Quality" pack: RealESRGAN_x4plus.pth **67,040,989 B**; Qualcomm float ONNX zip 62,174,027 B as a size sanity check.
  * Optional "Anime" pack: RealESRGAN_x4plus_anime_6B.pth **17,938,799 B**.
* **Export pipeline (our build, not shipped source):** PyTorch checkpoint → ONNX (opset compatible with ONNX Runtime Android version) → optional int8 quantization → measure actual bytes → record SHA-256 in the manifest → publish under our HTTPS host. **Licence does not change when we convert** (BSD-3-Clause weights); keep the Real-ESRGAN copyright/licence text in the notices.
* **Inference design:** tile-based inference (e.g. 128–256 px tiles with overlap) to bound memory; downscale/upscale ratio from the requested output size; no server calls.

### 3.3 Roadmap packs (C and D)

* **C — Whisper transcription (on-demand, ~1 pack per quality level):**
  * Recommended: **whisper.cpp (MIT) + official ggml quantized model**: tiny q5_1 **32,152,673 B ≈ 30.66 MiB** (fast) or base q5_1 **59,707,625 B ≈ 56.94 MiB** (better). Use the official OpenAI MIT weights; do not take third-party ONNX conversions without pinning their provenance.
  * Alternative: **sherpa-onnx (Apache-2.0)** if we later want one runtime for ASR/TTS; it supports Whisper models.
  * Requires 16 kHz mono WAV extraction (FFmpegKit/MediaCodec — see android-stack); licence of the converter stack is separate (LGPL/GPL handling is task-1/task-3).
  * **Arabic:** Whisper supports Arabic; model accuracy not tested here.
* **D — Translation (en↔ar):**
  * **Licence-cleanest self-hosted:** OPUS-MT en→ar + ar→en, **Apache-2.0** (verified model cards). Build ONNX int8 exports; budget **≈ 221 MB total** (105.36 + 105.39 MiB) + tokenizer files — a large optional pack; download on demand, never bundle.
  * **Lowest-licence-burden (but GMS-dependent):** **ML Kit Translation** — 50+ languages incl. ar, dynamic downloads, no weights shipped. Costs: Google Play services requirement; **Google Translate attribution** in UI/help/Store description; smartphone/tablet-only restriction. Pricing clause **UNVERIFIED**.
  * **Do not use:** NLLB (CC-BY-NC), Argos model packages (no model licence metadata), Bergamot (MPL source question) for v1.
* **E — Text-to-speech:** use Android platform TextToSpeech in v1 (no weights). If a downloadable TTS is later required, pick Piper voices **only after each voice's MODEL_CARD licence is cleared in writing**; the repo-level MIT card is not sufficient evidence for Blizzard/"See URL" voices.
* **F — Background removal:** cleanest self-hosted = **u2netp.onnx 4,574,861 B ≈ 4.36 MiB (U²-Net, Apache-2.0)**; cleanest zero-weights = **ML Kit Subject Segmentation** (proprietary, GMS, model download). BiRefNet lite (MIT) is a high-end option at ≈109 MiB fp16.
* **G — Object eraser/inpainting:** **no cleared pack yet** (LaMa weights unverified; MI-GAN weights unverified). Keep out of v1.

### 3.4 APK vs on-demand strategy (with verified Play limits)

**Verified Play constraints (fetch date 2026-11-16):**
* Base module **500 MB**; individual feature modules **500 MB**; individual asset packs **1.5 GB**; cumulative total for all modules + install-time asset packs **4 GB**; cumulative for on-demand/fast-follow asset packs **30 GB**; total max compressed download **34 GB**; apps >1 GB must have minSdk 21+; **legacy APK publishing limit 100 MB** (https://support.google.com/googleplay/android-developer/answer/9859372).
* Asset Delivery modes: install-time, fast-follow (auto-download after install), on-demand (https://developer.android.com/guide/app-bundle/asset-delivery).

**Recommendation:** ship a **small base** and download **model data only**:
1. Base AAB: app + Tesseract4Android + bundled tessdata_fast eng/ara (5.3 MiB) + ONNX Runtime only if upscaling is in the base feature.
2. Model packs hosted by us (GitHub Releases for dev/QA; production on a CDN like Cloudflare R2/S3/Bunny with immutable URLs) as zips: model.onnx/.traineddata + LICENSE.txt + manifest.json (version, bytes, sha256, licence SPDX, source URL).
3. Do **not** use Play Asset Delivery for v1: verified limits are generous, but each model update would require an app release (asset packs are versioned with the app). Self-hosted packs decouple model updates and also work for sideloaded APKs — relevant for the Iraqi market.
4. Never download native code: Play's Device and Network Abuse policy says *"an app may not download executable code (such as dex, JAR, .so files) from a source other than Google Play"* (https://support.google.com/googleplay/android-developer/answer/16559646). Model weights (.onnx, .traineddata, .bin) are data; do not package or download .so/.dex at runtime. ONNX Runtime/Tesseract native libs must be in the APK/AAB.
5. Play size dialog: apps above 200 MB trigger a non-blocking mobile-data warning (same size-limits page) — another reason to keep the base under ~100 MB and download models.
6. **User privacy (explicit requirement):** the app must never upload user files. Only *downloads* of model packs are network operations. Implement: explicit "Download" tap with size + licence shown; Wi-Fi-only default with per-pack override; no silent background downloads; cancel/resume; deletion control; offline banner when a pack is missing.
7. **Integrity & resume plan (concrete):**
   * Manifest per pack: {id, version, url, bytes, sha256, spdx, sourceUrl, minAppVersion}; ship a copy in the app (for bundled packs) and fetch the same manifest over HTTPS from our host.
   * Download to a .tmp file in app-private storage, verify **byte count + SHA-256** before atomic rename; resume via HTTP Range and re-verify the full file at the end; reject on mismatch and retry once.
   * Keep previous pack version until the new one verifies; allow rollback.
   * Bundle licence texts in-app per pack; also include LICENSE.txt inside each pack zip.
   * Publish hashes in repo docs and in the release notes so the build and the store listing can be audited.
8. **16 KB page-size readiness** for the native libs (Tesseract4Android, ONNX Runtime) must be verified by android-stack against Play requirements for 2026 — flagged here, not audited.

### 3.5 Estimated pack economics (exact Byte counts; no invented store sizes)

| Pack | Contents | Bytes | ≈ MiB | Delivery |
|---|---|---|---|---|
| OCR bundled | tessdata_fast eng+ara | 5,545,144 | 5.29 | in APK/AAB |
| OCR high-accuracy | tessdata_best eng+ara | 28,004,325 | 26.71 | on demand |
| Upscale fast | realesr-general-x4v3 (.pth → ONNX) | 4,885,111 (.pth) | 4.66 (.pth) | on demand |
| Upscale quality | RealESRGAN_x4plus (.pth → ONNX) | 67,040,989 (.pth) | 63.94 (.pth) | on demand |
| Upscale anime | RealESRGAN_x4plus_anime_6B (.pth → ONNX) | 17,938,799 (.pth) | 17.11 (.pth) | on demand |
| Whisper tiny (q5_1) | ggml-tiny-q5_1.bin | 32,152,673 | 30.66 | on demand |
| Whisper base (q5_1) | ggml-base-q5_1.bin | 59,707,625 | 56.94 | on demand |
| Translate en→ar int8 | encoder+decoder_merged (reference) | 110,478,997 | 105.36 | on demand |
| Translate ar→en int8 | encoder+decoder_merged (reference) | 110,511,893 | 105.39 | on demand |
| Background remove (u2netp) | u2netp.onnx | 4,574,861 | 4.36 | on demand |
| Runtime (OCR) | Tesseract4Android AAR (all ABIs) | 12,848,454 | 12.25 | in APK/AAB |
| Runtime (base) | onnxruntime-android AAR (all ABIs) | 59,335,382 | 56.59 | in APK/AAB |

* ONNX Runtime AAR includes all ABIs; Play ABI splits reduce the installed download, but exact per-ABI values were not measured (**UNVERIFIED**); **no per-ABI Maven artifact exists** (android-stack, 2026-11-16), so a smaller per-ABI dependency is not an option. Runtime pins are duplicated in docs/research/android-stack.md §2.12 — update both documents together if either artifact is bumped.
* ONNX export of Real-ESRGAN will differ from the .pth byte size; measure and record at build time (**UNVERIFIED** today).

### 3.6 Decisions for the Lead (blocking or near-blocking)

1. **Bundle tessdata_fast (5.3 MiB) or make all OCR data downloadable?** Recommendation: bundle fast (true out-of-box offline, tiny cost).
2. **Is upscaling a launch feature?** If yes, ONNX Runtime (~56.59 MiB AAR all ABIs) must be in the base; if roadmap, keep it out of v1 to protect install size.
3. **Translation route:** ML Kit (no weights, GMS, Google Translate attribution, pricing unverified) vs OPUS-MT (Apache-2.0, ~221 MB for both directions, needs ONNX pipeline). Recommendation: ML Kit for roadmap first, OPUS-MT as GMS-free fallback; do not ship either in v1.
4. **Background removal:** u2netp (4.36 MiB, Apache-2.0, fully offline) vs ML Kit Subject Segmentation (GMS, proprietary). Recommendation: u2netp when the feature is built.
5. **TTS:** platform TextToSpeech for v1; Piper voice licences are not clear enough to ship today.
6. **Tesseract4Android AAR:** size/SHA-256 now verified (12,848,454 B); ensure the build resolves/vendors JitPack reliably and add the Apache-2.0 + BSD-2-Clause + IJG/libpng notice texts (not bundled in the AAR).

---

## 4. Commercial-use verdict table (condensed)

| Candidate | Verdict | Required action |
|---|---|---|
| tessdata_best / tessdata_fast / tessdata eng+ara (Apache-2.0) | **ALLOWED-WITH-ATTRIBUTION** | Ship Apache-2.0 text; keep upstream source URL/version; no NOTICE file existed at fetch date. |
| Tesseract engine + Tesseract4Android (Apache-2.0) | **ALLOWED-WITH-ATTRIBUTION** | Apache-2.0 text + attribution; include Leptonica (BSD-2-Clause) and libjpeg/libpng notices. The AAR ships no licence file, so all texts must be added by us. |
| Whisper weights + whisper.cpp (MIT) | **ALLOWED-WITH-ATTRIBUTION** | MIT text + "Copyright (c) 2022 OpenAI" and "Copyright (c) 2023-2026 The ggml authors". |
| ONNX Runtime Android (MIT) | **ALLOWED-WITH-ATTRIBUTION** | MIT text + "Copyright (c) Microsoft Corporation". |
| Real-ESRGAN (BSD-3-Clause) | **ALLOWED-WITH-ATTRIBUTION** | Keep "Copyright (c) 2021, Xintao Wang", the 3 clauses + disclaimer; do not use names to endorse. |
| ESRGAN / SwinIR (Apache-2.0) | **ALLOWED-WITH-ATTRIBUTION** | Apache-2.0 text; record source. |
| OPUS-MT en↔ar (Apache-2.0) | **ALLOWED-WITH-ATTRIBUTION** | Apache-2.0 text + model card attribution; record weights provenance. |
| sherpa-onnx (Apache-2.0) | **ALLOWED-WITH-ATTRIBUTION** | Apache-2.0 text. |
| Vosk code + Apache-2.0 models only | **ALLOWED-WITH-ATTRIBUTION** | Apache-2.0 text; per-model licence check before download; never AGPL/NC models. |
| Piper code (MIT) | **ALLOWED-WITH-ATTRIBUTION** | MIT text + "Copyright (c) 2022 Michael Hansen". |
| Piper voices (per-voice) | **UNVERIFIED** | Resolve each voice's dataset licence in writing first; prefer platform TTS. |
| U²-Net / IS-Net (Apache-2.0) | **ALLOWED-WITH-ATTRIBUTION** | Apache-2.0 text; note IS-Net/DIS5K dataset terms → legal review of weights position. |
| BiRefNet (MIT) | **ALLOWED-WITH-ATTRIBUTION** | MIT text; size makes it niche. |
| ML Kit modules (text recog., translation, subject segmentation) | **PROPRIETARY-USE-ONLY** | Follow ML Kit ToS + Google APIs ToS; disclose metrics in Data safety; translation: Google Translate attribution + phone/tablet restriction; never extract/redistribute models; pricing UNVERIFIED. |
| Argos models | **UNVERIFIED** | No licence metadata in argospm index; do not ship. |
| Bergamot models | **UNVERIFIED** | MPL-2.0 repo but per-model licence/source-form question unresolved; do not ship in v1. |
| LaMa weights / MI-GAN weights | **UNVERIFIED** | Code licences are clean (Apache-2.0 / MIT) but weights/checkpoints lack licence statements in this audit; do not ship. |
| waifu2x weights | **UNVERIFIED** | Code MIT; weights licence not found. |
| Upscayl app code | **FORBIDDEN** | AGPL-3.0 — do not link, copy, or embed. |
| Upscayl custom-models pack | **FORBIDDEN/UNCLEARED** | Repo has no licence file; per-model clearance required (not done). |
| NLLB-200 | **FORBIDDEN** | CC-BY-NC-4.0 (non-commercial). |
| BRIA RMBG-1.4 / 2.0 | **FORBIDDEN** | Non-commercial model licence. |
| FLUX.1 [dev] | **FORBIDDEN** | Non-commercial/non-production licence. |
| Vosk AGPL Arabic model (1.3 GB) | **FORBIDDEN** | AGPL model — do not ship. |
| SD 1.x (CreativeML OpenRAIL-M) / SDXL (OpenRAIL++-M) | **ALLOWED-WITH-RESTRICTIONS** (not needed) | Propagate licence + use restrictions; better not to add. |

---

## 5. Licence-notice requirements and where they must appear

### 5.1 In-app: "Open-source & model licences" screen

* Reachable from **Settings → About → Licences** (and linked from the model-download card for each pack).
* Contents: one row per component **and per model pack**, each expanding to the full licence text + source URL + version.
* Because packs can be downloaded later, the licence text for **all** packs must be **bundled in the app** (not only inside the downloaded zip) — otherwise a user who downloads a pack later may never see the notice.
* Keep the notices for models even if the model is deleted from the device.
* If an OSS licence screen is generated at build time (e.g. from Maven POMs), it will **not** cover model weights (they are not Maven dependencies) — add them manually.

### 5.2 Repository: docs/legal/third-party-notices.md

*Owner: legal teammate (task-1). Do not write that file from this task; the rows below are designed to be copied there.*

Minimum rows for v1 (copy verbatim, filling versions/hashes):

| Component | Version/ID | Licence | Source | Notice |
|---|---|---|---|---|
| Tesseract OCR language data (tessdata_fast) | eng 4,113,088 B; ara 1,432,056 B | Apache-2.0 | https://github.com/tesseract-ocr/tessdata_fast | Apache-2.0 text; attribution to Tesseract OCR project |
| Tesseract OCR language data (tessdata_best, on-demand) | eng 15,400,601 B; ara 12,603,724 B; sha256 pinned | Apache-2.0 | https://github.com/tesseract-ocr/tessdata_best | Apache-2.0 text |
| Tesseract4Android | 4.9.0; AAR 12,848,454 B ≈ 12.25 MiB; sha256 BCE5D6413A1A5AE3D7240033FBBC851BA3217D0A08D9769400E17A077F42CB2A | Apache-2.0 | https://github.com/adaptech-cz/Tesseract4Android | Apache-2.0 text **must be added by us — AAR ships no licence file** |
| Tesseract OCR engine | 5.5.1 per README (**UNVERIFIED** from AAR) | Apache-2.0 | https://github.com/tesseract-ocr/tesseract | Apache-2.0 text |
| Leptonica | 1.85.0 per README (**UNVERIFIED** from AAR) | BSD-2-Clause (per upstream README) | https://github.com/DanBloomberg/leptonica | BSD-2 text **must be sourced and added by us** — no licence file in AAR; direct upstream fetch 404 on 2026-11-16 |
| libjpeg / libpng | per Tesseract4Android README | IJG / libpng licence | Tesseract4Android README | reproduce upstream licence texts — **not bundled in AAR** |
| ONNX Runtime Android | 1.32.0 | MIT | https://github.com/microsoft/onnxruntime | MIT text + "Copyright (c) Microsoft Corporation" |
| Real-ESRGAN weights (Pack B) | x4plus / general-x4v3 / anime 6B; sha256 pinned | BSD-3-Clause | https://github.com/xinntao/Real-ESRGAN | "Copyright (c) 2021, Xintao Wang" + 3-clause text + disclaimer |

### 5.3 Exact notice content by licence class

**Apache-2.0 (tessdata, Tesseract, Tesseract4Android, OPUS-MT, sherpa-onnx, Vosk):**
* Include a **copy of the Apache License 2.0** (full text) with the app/pack.
* Retain the upstream copyright/attribution notices; if upstream ships a **NOTICE** file, reproduce its contents. *Checked 2026-11-16: no NOTICE file in the root of tessdata_best, tessdata_fast, Tesseract4Android at fetch date.* The Tesseract4Android AAR itself contains no licence/notice file either (android-stack, 2026-11-16), so the Apache-2.0 text ships from us. Re-check at build.
* If we modify any covered files (we should not modify weights), mark the modified files; packaging into a zip is not a modification.
* Suggested line: *"This product includes Tesseract OCR language data from the tessdata project (https://github.com/tesseract-ocr/tessdata_best), licensed under the Apache License, Version 2.0."*

**BSD-3-Clause (Real-ESRGAN):**
* Reproduce **verbatim**: "Copyright (c) 2021, Xintao Wang / All rights reserved." + the three conditions + warranty disclaimer (from https://github.com/xinntao/Real-ESRGAN/blob/master/LICENSE).
* Do not use the author's name/names to endorse the app.
* Applies to **converted ONNX files** too — the weights remain BSD-3-Clause.

**MIT (ONNX Runtime, Whisper, whisper.cpp, Piper code, Argos code):**
* Include the MIT text + the respective copyright line:
  * ONNX Runtime: Copyright (c) Microsoft Corporation
  * Whisper: Copyright (c) 2022 OpenAI
  * whisper.cpp: Copyright (c) 2023-2026 The ggml authors
  * Piper: Copyright (c) 2022 Michael Hansen
* The MIT text itself requires: "The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software."

**CC-BY-4.0 (only if a CC-BY voice/dataset-derived model is ever shipped, e.g. Piper en_GB-alba):** attribution + licence link + indicate changes. Not planned for v1.

**MPL-2.0 (Bergamot, if ever used):** provide MPL text; make source of covered files available; model-binary "source form" question must be resolved by legal — reason it is not in v1.

### 5.4 Google ML Kit / Google Translate attribution (if ML Kit translation is used)

Required by Google's **Usage guidelines for ML Kit on-device translation** (fetched 2026-11-16):
* Comply with **Google Cloud Translation API attribution requirements** — layout, **Google attribution**, and **branding** (https://cloud.google.com/translate/attribution).
* State in the **app description and help/documentation** that **Google Translate is used to power translation** and link to the Cloud Translation site.
* When showing translated text, make clear it is **automatic translation from Google Translate**.
* Restriction: the on-device Translation API may only be used in apps for **smartphones, tablets, laptops, desktops** — not embedded devices, cars, TVs, appliances, speakers without Google's written permission.
* The exact UI wording/markup must be taken from the Cloud Translation attribution page at implementation time; Google may change it without notice. Keep the URL in the notice file.
* Also disclose ML Kit metrics per ML Kit ToS in the Play Data safety form.

### 5.5 Where else notices must appear

* **Model download screen:** pack name, size, licence (SPDX), source, "Licence" link; the download button is an explicit consent action.
* **Each downloaded pack zip:** LICENSE.txt + manifest.json with SPDX + source URL.
* **Play Store listing/help:** if ML Kit translation is used — Google Translate statement + link; if a model pack is user-downloadable — describe that only model data is downloaded (no user files uploaded).
* **Privacy policy / Data safety:** no user file uploads; ML Kit metrics disclosure if ML Kit is used; model download traffic is not user data.
* **NOTICE file:** if any bundled OSS has a NOTICE, ship a combined NOTICE in the APK assets; otherwise keep the licence screen + third-party-notices.md authoritative.

---

## 6. Explicitly FORBIDDEN / not-cleared models for a commercial app

| Model / asset | Why forbidden / not cleared | Licence evidence (fetched 2026-11-16) |
|---|---|---|
| **NLLB-200** (all sizes) | **Non-commercial** (CC-BY-NC-4.0) | HF metadata: https://huggingface.co/facebook/nllb-200-distilled-600M (license: "cc-by-nc-4.0") |
| **BRIA RMBG-1.4 / RMBG-2.0** | **Non-commercial** model licence; gated | https://huggingface.co/briaai/RMBG-1.4 — "source-available model for non-commercial use"; licence name bria-rmbg-1.4 |
| **FLUX.1 [dev]** | **Non-commercial and non-production** licence | https://github.com/black-forest-labs/flux/blob/main/model_licenses/LICENSE-FLUX1-dev — "freely available for your non-commercial and non-production use" |
| **Upscayl app source** | **AGPL-3.0** — linking/copying would impose AGPL on our app | https://github.com/upscayl/upscayl/blob/main/LICENSE |
| **Upscayl custom-models model pack** | **No licence file in repo**; per-model licences unknown; several community ESRGAN models are licensed individually by their authors | Root listing 2026-11-16: README.md + models/ only; no LICENSE; README credits authors, states no model licence |
| **Vosk vosk-model-ar-0.22-linto-1.1.0** (1.3 GB Arabic) | **AGPL** per Vosk model page | https://alphacephei.com/vosk/models |
| **Vosk models tagged GPL-3.0 / LGPL-3.0 / CC-BY-NC-SA** (various languages) | Copyleft or non-commercial | same page (licence column per model) |
| **waifu2x weights** | Code MIT but **weights licence not found** — do not ship until cleared | https://github.com/nagadomi/waifu2x |
| **LaMa / MI-GAN checkpoints** | Code clean (Apache-2.0 / MIT) but **checkpoint licences not stated** | https://github.com/advimman/lama ; https://github.com/Picsart-AI-Research/MI-GAN |
| **Argos Translate model packages** | **No licence metadata** in argospm index | https://github.com/argosopentech/argospm-index (index.json fetched) |
| **Bergamot/Firefox Translation models** | Repo MPL-2.0 but **per-model licence not stated**; MPL source-form question for binaries | https://github.com/mozilla/firefox-translations-models |
| **Piper voices with Blizzard/"See URL" dataset licences** | Voice-level licence unclear; repo-level MIT card insufficient | e.g. en_US-lessac-medium MODEL_CARD → Blizzard 2013 licence URL; ar_JO-kareem → "See URL" |
| **SD 1.x / SDXL** | Not forbidden, but **use-restriction licences** (CreativeML OpenRAIL-M / OpenRAIL++-M) must be propagated to users; unnecessary for this app | licence text fetched (OpenRAIL-M) |
| **ML Kit models themselves** | Proprietary; extracting/redistributing prohibited by ML Kit ToS ("may not reverse engineer or attempt to extract the source code or any related software"; models are "related software") | https://developers.google.com/ml-kit/terms |

---

## 7. Open questions / UNVERIFIED list (do not silently convert these into claims)

1. **Tesseract4Android artifact — size resolved, version/notice questions open.** AAR size is **verified: 12,848,454 B** (SHA-256 BCE5D6413A1A5AE3D7240033FBBC851BA3217D0A08D9769400E17A077F42CB2A, android-stack 2026-11-16); the HEAD HTTP 500 I saw was **transient** and the artifact downloads fine. Still **UNVERIFIED**: exact bundled upstream versions (Tesseract 5.5.1 / Leptonica 1.85.0 / libjpeg v9f / libpng 1.6.48 are README claims; no version strings or notice file inside the AAR), and the Leptonica BSD-2-Clause licence text (must be obtained and shipped by us).
2. **ONNX Runtime per-ABI download size** — only the all-ABI AAR 59,335,382 B was measured, and **no per-ABI Maven artifact exists** (android-stack, 2026-11-16); per-device savings depend on ABI splits at packaging, not on the dependency.
3. **Real-ESRGAN ONNX export byte sizes** — .pth sizes are verified; ONNX sizes are not exported/measured yet. Qualcomm AI Hub zip sizes are only a sanity reference.
4. **Leptonica licence text** — BSD-2-Clause is stated in Tesseract4Android README; raw licence file 404 at DanBloomberg/leptonica/master/LICENSE, and the AAR ships no licence file (android-stack, 2026-11-16). We must obtain the BSD-2-Clause text (correct upstream path or Tesseract4Android source tree) and ship it ourselves; also reproduce IJG/libpng texts.
5. **ML Kit pricing** — no fee clause found in the terms fetched; "free" is **UNVERIFIED** as a contractual fact.
6. **ML Kit translation ar↔en pair availability** — Arabic and English are both in the supported language list; per-pair support not fetched.
7. **ML Kit translation model download sizes** — not documented in fetched pages.
8. **Piper voice licences** — repo card MIT; per-voice dataset licences vary and several are unresolved ("See URL", Blizzard). Do not ship voices.
9. **DIS5K dataset Terms of Use effect on IS-Net weights** — dataset terms PDF exists in the repo but was not read; weight repo licence is Apache-2.0.
10. **U²-Net / rembg ONNX conversion provenance** — weights upstream Apache-2.0; the ONNX files are hosted as assets of a third-party (rembg, MIT code) repo; record source and pin hash.
11. **Whisper ONNX conversion repo licences** — metadata inconsistent (tiny: none; base: apache-2.0). Prefer own export or ggml.
12. **waifu2x / LaMa / MI-GAN weight licences** — not found.
13. **HF re-upload contamination** — many re-uploads of Real-ESRGAN/others with different licence tags; only official sources count.
14. **16 KB page-size support** of Tesseract4Android/ONNX Runtime native libs — build-level verification for android-stack, not audited here.
15. **Tesseract OCR accuracy for Arabic** — not benchmarked in this audit (licence-only audit).

---

## Appendix A — Artifacts actually downloaded and SHA-256 computed on 2026-11-16

Method: PowerShell Invoke-WebRequest -OutFile then Get-FileHash -Algorithm SHA256 (rows marked android-stack were verified by that teammate, task-3, same day). Downloads went to %TEMP%; no files were written into the repo by this audit.

| Artifact | Official URL | Bytes | SHA-256 |
|---|---|---|---|
| tesseract4android-4.9.0.aar | https://jitpack.io/cz/adaptech/tesseract4android/tesseract4android/4.9.0/tesseract4android-4.9.0.aar | 12,848,454 | BCE5D6413A1A5AE3D7240033FBBC851BA3217D0A08D9769400E17A077F42CB2A (android-stack) |
| eng.traineddata (tessdata_best) | https://raw.githubusercontent.com/tesseract-ocr/tessdata_best/main/eng.traineddata | 15,400,601 | 8280aed0782fe27257a68ea10fe7ef324ca0f8d85bd2fd145d1c2b560bcb66ba |
| ara.traineddata (tessdata_best) | https://raw.githubusercontent.com/tesseract-ocr/tessdata_best/main/ara.traineddata | 12,603,724 | ab9d157d8e38ca00e7e39c7d5363a5239e053f5b0dbdb3167dde9d8124335896 |
| RealESRGAN_x4plus.pth | https://github.com/xinntao/Real-ESRGAN/releases/download/v0.1.0/RealESRGAN_x4plus.pth | 67,040,989 | 4fa0d38905f75ac06eb49a7951b426670021be3018265fd191d2125df9d682f1 |
| realesr-general-x4v3.pth | https://github.com/xinntao/Real-ESRGAN/releases/download/v0.2.5.0/realesr-general-x4v3.pth | 4,885,111 | 8dc7edb9ac80ccdc30c3a5dca6616509367f05fbc184ad95b731f05bece96292 |
| RealESRGAN_x4plus_anime_6B.pth | https://github.com/xinntao/Real-ESRGAN/releases/download/v0.2.2.4/RealESRGAN_x4plus_anime_6B.pth | 17,938,799 | f872d837d3c90ed2e05227bed711af5671a6fd1c9f7d7e91c911a61f155e99da |

**Note on pinning:** GitHub release assets and raw.githubusercontent content are mutable in principle. In production, mirror the exact verified files to our own immutable host and pin the SHA-256 above. Also pin the upstream commit/tag in the manifest for provenance (record the tessdata_best main commit at build time; not fetched in this audit). Artifact pins shared with the stack memo: **docs/research/android-stack.md §2.12** duplicates the tesseract4android 4.9.0 and onnxruntime-android 1.32.0 pins — **if either artifact is bumped, both documents must change together**.

---

## Appendix B — Model pack manifest schema (recommended)

Indented code block (JSON):

    {
      "schema": 1,
      "packs": [
        {
          "id": "ocr-best",
          "version": "2026.11.1",
          "title": "High-accuracy OCR (English + Arabic)",
          "files": [
            {"name": "eng.traineddata", "bytes": 15400601, "sha256": "8280aed0782fe27257a68ea10fe7ef324ca0f8d85bd2fd145d1c2b560bcb66ba"},
            {"name": "ara.traineddata", "bytes": 12603724, "sha256": "ab9d157d8e38ca00e7e39c7d5363a5239e053f5b0dbdb3167dde9d8124335896"}
          ],
          "spdx": "Apache-2.0",
          "licenseUrl": "https://github.com/tesseract-ocr/tessdata_best/blob/main/LICENSE",
          "sourceUrl": "https://github.com/tesseract-ocr/tessdata_best",
          "minAppVersion": 1
        }
      ]
    }

Rules: the app verifies every file's bytes and sha256 after download; a pack is used only if all files verify; the manifest itself is delivered over HTTPS from the same host as the files; no manifest-driven code execution is possible because packs contain only data files (allow-list extensions: .onnx, .traineddata, .bin, .json, .spm, .txt, .model).

---

## Appendix C — Fetch log (evidence)

All fetched **2026-11-16** with PowerShell Invoke-WebRequest/Invoke-RestMethod; HTTP 200 unless noted. (GitHub API used unauthenticated, within rate limits.)

**Licence files (raw):** tessdata_best/LICENSE OK · tessdata/LICENSE OK · tessdata_fast/LICENSE OK · tesseract/LICENSE OK · Tesseract4Android/LICENSE OK · openai/whisper/LICENSE OK · whisper.cpp/LICENSE OK · Real-ESRGAN/LICENSE OK · ESRGAN/LICENSE OK · SwinIR/LICENSE OK · waifu2x/LICENSE OK · U-2-Net/LICENSE OK · DIS/LICENSE.md OK · BiRefNet/LICENSE OK · lama/LICENSE OK · upscayl/LICENSE OK (AGPL-3.0) · MI-GAN/LICENSE OK · sherpa-onnx/LICENSE OK · vosk-api/COPYING OK · piper/LICENSE.md OK · argos-translate/LICENSE OK · bergamot-translator/LICENSE OK (MPL-2.0) · firefox-translations-models/LICENSE OK (MPL-2.0) · PaddleOCR/LICENSE OK · onnxruntime/LICENSE OK (MIT) · FLUX1-dev model licence (github BFL) OK · CreativeML OpenRAIL-M license.txt (HF space) OK · Leptonica LICENSE FAIL 404.

**Model metadata / sizes:** GitHub API tessdata_best root OK (sizes, LICENSE 10,173 B) · GitHub API Tesseract4Android latest release OK (4.9.0) · GitHub API Tesseract4Android root OK (no NOTICE) · GitHub API Real-ESRGAN releases OK (all .pth sizes) · GitHub API upscayl/custom-models root + models OK (no LICENSE; model sizes) · GitHub API rembg releases OK (installer files; model HEADs: u2net.onnx 175,997,641; u2netp.onnx 4,574,861; isnet-general-use.onnx 178,648,008; birefnet-general 404) · HF tree ggerganov/whisper.cpp OK (ggml sizes) · HF tree + README onnx-community/whisper-tiny OK, whisper-base OK · HF tree Helsinki-NLP/opus-mt-en-ar OK, ar-en OK; model card licence apache-2.0 OK · HF tree Xenova/opus-mt-en-ar, ar-en ONNX int8 sizes OK · HF tree qualcomm/Real-ESRGAN-x4plus OK (release_assets.json; LICENSE references original) · HEAD Qualcomm AI Hub zips OK (onnx float 62,174,027; onnx w8a8 24,952,432) · HF tree rhasspy/piper-voices OK (voice ONNX 63,201,294 B; MODEL_CARDs) · HF voices.json OK (183 voices) · HF API briaai/RMBG-1.4 OK (license other; non-commercial) · HF API facebook/nllb-200-distilled-600M OK (cc-by-nc-4.0) · HF API CompVis SD1.4 OK (creativeml-openrail-m) + space tree/license.txt OK · HF API stabilityai SDXL OK (openrail++) · HF API FLUX.1-dev OK (other, gated; raw licence 401) · HF API FLUX.1-schnell OK (apache-2.0) · HF API onnx-community/BiRefNet-ONNX + _lite OK · moz-fx-translations-data models.json OK (118 pairs; ar-en/en-ar; no licence field) · argospm-index/index.json OK (no licence field; en_ar/ar_en entries) · alphacephei.com/vosk/models OK (per-model licences incl. Arabic) · repo1.maven.org onnxruntime-android maven-metadata.xml OK (1.32.0) + AAR HEAD OK (59,335,382) · onnxruntime-mobile metadata OK (1.18.0) + 1.32.0 AAR 404 · JitPack API cz.adaptech/tesseract4android/4.9.0 OK (status ok, modules tesseract4android, tesseract4android-openmp) · JitPack AAR HEAD FAIL 500 (transient; artifact download verified same day by android-stack: 12,848,454 B, SHA-256 pinned in Appendix A).

**Policy/docs pages:** https://developers.google.com/ml-kit/terms OK · .../language/translation OK · .../translation/translation-terms OK · .../translation/translation-language-support OK · .../vision/text-recognition/v2 OK · .../vision/subject-segmentation/android OK · https://cloud.google.com/translate/attribution OK · https://support.google.com/googleplay/android-developer/answer/16559646 OK (Device and Network Abuse; executable-code clause) · https://support.google.com/googleplay/android-developer/answer/9859372 OK (size limits) · https://developer.android.com/guide/app-bundle/asset-delivery OK.

---

## Appendix D — Short evidence quotes (≤3 lines each)

| Claim | Quote | Source |
|---|---|---|
| Whisper weights are MIT | "Whisper's code and model weights are released under the MIT License." | https://github.com/openai/whisper/blob/main/README.md |
| Real-ESRGAN is BSD-3 | "BSD 3-Clause License / Copyright (c) 2021, Xintao Wang" | https://github.com/xinntao/Real-ESRGAN/blob/master/LICENSE |
| tessdata_best format | "These models only work with the LSTM OCR engine of Tesseract 4." | https://github.com/tesseract-ocr/tessdata_best/blob/main/README.md |
| RMBG non-commercial | "RMBG v1.4 is available as a source-available model for non-commercial use" | https://huggingface.co/briaai/RMBG-1.4 |
| NLLB non-commercial | license: "cc-by-nc-4.0" | https://huggingface.co/facebook/nllb-200-distilled-600M/raw/main/README.md |
| FLUX dev non-commercial | "freely available for your non-commercial and non-production use" | https://github.com/black-forest-labs/flux/blob/main/model_licenses/LICENSE-FLUX1-dev |
| Play executable-code rule | "an app may not download executable code (such as dex, JAR, .so files) from a source other than Google Play" | https://support.google.com/googleplay/android-developer/answer/16559646 |
| ML Kit models are proprietary | "machine learning models will be considered related software. ML Kit APIs run on-device. When using the ML Kit APIs, you may not reverse engineer or attempt to extract the source code or any related software" | https://developers.google.com/ml-kit/terms |
| ML Kit translation attribution | "An app that uses the ML Kit on-device Translation API must comply with the Google Cloud Translation API attribution requirements" + restricted to smartphones/tablets/laptops/desktops | https://developers.google.com/ml-kit/language/translation/translation-terms |
| Argos dual licence | "Argos Translate is dual licensed under either the MIT License or Creative Commons CC0." | https://github.com/argosopentech/argos-translate/blob/master/README.md |
| Upscayl AGPL | "GNU AFFERO GENERAL PUBLIC LICENSE / Version 3, 19 November 2007" | https://github.com/upscayl/upscayl/blob/main/LICENSE |
| Vosk model licences vary | Arabic Vosk model rows: "vosk-model-ar-mgb2-0.4 … Apache 2.0"; "vosk-model-ar-0.22-linto-1.1.0 … AGPL" | https://alphacephei.com/vosk/models |
| CreativeML restrictions | "You shall require all of Your users who use the Model or a Derivative of the Model to comply with the terms of this paragraph (paragraph 5)." | https://huggingface.co/spaces/CompVis/stable-diffusion-license (license.txt) |

---

## Appendix E — Pack recommendation summary (for the app spec)

| Feature | v1 status | Pack | Bytes (exact/approx) | Licence | Download host |
|---|---|---|---|---|---|
| OCR English+Arabic | **Ship (bundled fast + optional best)** | tessdata_fast eng/ara; tessdata_best eng/ara | 5,545,144; 28,004,325 | Apache-2.0 | bundled + our CDN (mirror of upstream) |
| Image upscale ×2/×4 | **Ship (on-demand)** | Real-ESRGAN general-x4v3; x4plus; anime_6B (ONNX export) | 4,885,111 / 67,040,989 / 17,938,799 (.pth) | BSD-3-Clause | our CDN, pinned SHA-256 |
| Transcription | Roadmap | Whisper tiny/base q5_1 via whisper.cpp | 32,152,673 / 59,707,625 | MIT | our CDN |
| Translation en↔ar | Roadmap (decide route) | ML Kit (no weights) or OPUS-MT ONNX int8 | ≈110.5 MiB/direction | Proprietary (ML Kit) / Apache-2.0 (OPUS) | Play Services or our CDN |
| Background removal | Roadmap | u2netp.onnx / ML Kit subject segmentation | 4,574,861 | Apache-2.0 / proprietary | our CDN / Play Services |
| TTS | Not planned | Android platform TTS | 0 | none shipped | — |
| Object eraser | **Blocked** | LaMa/MI-GAN weights unclear | — | UNVERIFIED | — |
