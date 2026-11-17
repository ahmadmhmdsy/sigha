# Android Stack Feasibility — Verified Dependencies for the Filee-Inspired Converter

> **Owner:** engineer (teammate `android-stack`) · **Task:** task-3
> **Fetched:** 2026-11-16 (Asia/Baghdad, UTC+03:00) · **Machine:** Windows, JDK 21.0.13 (Temurin), Android SDK at `C:\Android\Sdk\` (platforms 35 + 36; build-tools 34/35/36), Gradle 8.11.1 at `C:\gradle\gradle-8.11.1`, Gradle 8.9 at `E:\android_apps\digital_converter\tools\gradle-8.9`.
> **Method:** every version below was fetched live, not remembered. Maven Central via `https://repo1.maven.org/maven2/<group-path>/<artifact>/maven-metadata.xml`; Google Maven via `https://dl.google.com/android/maven2/<group-path>/group-index.xml`; JitPack via `https://jitpack.io/<group>/<artifact>/maven-metadata.xml`; licenses via each artifact's `.pom` `<licenses>` block **and** the upstream LICENSE file; sizes via HTTP download measured with PowerShell (AAR/JAR bytes), and by unzipping AARs to measure raw `.so` bytes per ABI; native 16 KB page support verified by parsing ELF program headers (`p_align`) of the actual shipped `.so` files.
> **Rule applied:** anything not fetched/measured is marked **UNVERIFIED** with the reason. Quotes are short (≤3 lines) with their source.
> **Not legal advice.** Halal/legal conclusions live in the separate clearance deliverable; this file states licenses and compliance obligations only.

---

## 0. TL;DR — the decisions this file supports

| Area | Decision | Why (evidence in this file) |
|---|---|---|
| Build config | **AGP 8.10.1 + Gradle 8.11.1 + Kotlin 2.0.21 + KSP 2.0.21-1.0.28, compileSdk 36, targetSdk 36, minSdk 26, JVM 17** — the sibling-proven config in `E:\android_apps\steps_app\` | Sibling produced `app-debug.apk` = **10,971,650 bytes** (2026-11-16 22:57) and its build files now declare AGP **8.10.1** (re-read 2026-11-16 23:5x). AGP 8.10 requires Gradle ≥ 8.11.1 and supports max API 36; Play requires target API 36 from 2026-08-31. |
| Video/audio engine | **`dev.ffmpegkit-maintained:ffmpeg-kit-full:8.1.9`** (LGPL-3.0) as primary. **Never** the `-gpl` artifacts. | Latest published 8.1.9; minSdk 24; arm64-v8a + x86_64 only; every `.so` verified 16 KB-aligned; package remains `com.arthenica.ffmpegkit` (drop-in). |
| Simple video path | **`androidx.media3:media3-transformer:1.11.1`** is viable only as an optional hardware-only path (trim/transcode/crop via MediaCodec) — **not** a replacement for FFmpeg coverage | Media3 docs: Transformer "relies on MediaCodec for encoding, which limits the output formats supported" and has no ExoPlayer software decoders. |
| PDF | **`com.tom-roush:pdfbox-android:2.0.27.0`** (Apache-2.0) for text extraction/manipulation + framework `PdfRenderer` (API 21) for rasterizing + framework `PdfDocument` (API 19) for simple PDF creation. **Never** iText (AGPL) or MuPDF (AGPL). `androidx.pdf:pdf-viewer:1.0.0-beta01` is beta, API 28+, needs compileSdk 36 + SDK extension 19 — **defer**. | AAR metadata and LICENSE files verified; iText/MuPDF licenses verified AGPL-3.0. |
| OCR | **`cz.adaptech.tesseract4android:tesseract4android:4.9.0`** from **JitPack** (Apache-2.0; not on Maven Central) with **`tessdata_best`** language packs downloaded on device. Alternative: `dev.ffmpegkit-maintained:tesseract-android:5.5.0` (Maven Central, arm64-only, bundled eng model). ML Kit is the Google-proprietary fallback (own ToS). | JitPack release 4.9.0 verified (12,848,454 B, SHA-256 recorded in §2.3); tessdata_best Apache-2.0; sizes measured. |
| On-device models (ONNX) | **`com.microsoft.onnxruntime:onnxruntime-android:1.32.0`** (MIT) is **not recommended for v1** — 36.4 MB arm64 `.so` per ABI, no model chosen. Keep only if a specific model is cleared; put it in a Play feature module. | AAR measured & unzipped; no per-ABI Maven artifact exists (Maven Central directory listing, §2.4). |
| Archives | `org.apache.commons:commons-compress:1.28.0` + `org.tukaani:xz:1.12` (0BSD) + `net.lingala.zip4j:zip4j:2.11.6` + `com.github.junrar:junrar:8.1.2` (read-only RAR, UnRAR license caveat). | licenses + sizes verified; commons-compress docs quote 7z capability. |
| UI | **Compose BOM 2024.12.01** (Compose 1.7.6 / Material3 1.3.1), activity-compose 1.9.3, navigation-compose 2.8.5, Coil 2.7.0 — the sibling-proven set. **Do not jump to BOM 2026.09.00 with Kotlin 2.0.21** (untested). **Avoid `material-icons-extended`** (AAR 35.7 MB) — use `material-icons-core`. | BOM POM mapping + sibling build verified; latest available versions listed for the future. |
| Data | Room 2.6.1 + KSP 2.0.21-1.0.28 (if needed), DataStore 1.1.1, WorkManager 2.10.0, coroutines 1.9.0 — sibling-proven. Current project uses DataStore + WorkManager only. | sibling `app/build.gradle.kts` + successful APK. |
| Images/GIF | framework `ImageDecoder`/`AnimatedImageDrawable` (API 28+), `Bitmap.compress` (WebP lossy/lossless API 30+), `androidx.exifinterface`, `androidx.heifwriter` (API 28+), `pl.droidsonroids.gif:android-gif-drawable:1.2.32` (MIT) for GIF decoding/display. GIF encoding via FFmpeg — **UNVERIFIED** until probed on device. | API levels fetched from reference docs; AAR manifest/ELF verified. |
| Ship vs download | APK ships the engines; **language/model packs download in-app** (tessdata_best eng 15.4 MB, ara 12.6 MB; ONNX models). Play base-module limit is 500 MB compressed, so size is a UX problem, not a hard blocker. | Play size-limit page fetched; model sizes measured from GitHub API. |

**Two concrete build-file flags** (found while verifying, §8.7): (1) `android.packaging.jniLibs.useLegacyPackaging = true` in the current project should be `false` (16 KB/uncompressed guidance); (2) `material-icons-extended` is a 35.7 MB AAR that bloats debug APKs.

---

## 1. Verified build configuration (exact snippets)

### 1.1 What is proven on this machine

`E:\android_apps\steps_app\` declares and **successfully built an APK** with:

| Item | Value (re-read 2026-11-16 23:5x) | Evidence |
|---|---|---|
| AGP | **8.10.1** | `E:\android_apps\steps_app\build.gradle.kts` |
| Kotlin | **2.0.21** (android + compose plugins) | same file |
| KSP | **2.0.21-1.0.28** | same file |
| compileSdk / targetSdk | **36 / 36** | `app/build.gradle.kts` |
| minSdk | **26** | same |
| Java/JVM target | **17** | same |
| Compose BOM | **2024.12.01** | same |
| Gradle that ran | **8.11.1** (`.gradle\8.11.1` exists; `C:\gradle\gradle-8.11.1`) | directory listing |
| Output | `app-debug.apk` **10,971,650 bytes**, LastWriteTime 2026-11-16 22:57 | file listing |
| SDK | platforms `android-35`, `android-36`; build-tools 34.0.0/35.0.0/36.0.0; JDK 21.0.13 | directory listings |

Official compatibility (developer.android.com release notes, fetched 2026-11-16):
- AGP **8.10** line → "The maximum API level that Android Gradle Plugin 8.10 supports is API level 36. … Gradle **8.11.1** … SDK Build Tools 35.0.0 … JDK 17". AGP **8.10.1** is the patch release the sibling uses (release notes carry a "Fixed Issues in Android Gradle Plugin 8.10.1" section; `gradle-8.10.1.pom` present on Google Maven).
- AGP **8.7** → "Maximum API level … 35. Minimum version … Gradle **8.9** … Build Tools 34.0.0 … JDK 17".
- Google Play target API (fetched https://developer.android.com/google/play/requirements/target-sdk): "Starting August 31 2026: New apps and app updates must target Android 16 (API level 36) or higher to be submitted to Google Play".
- Android 16 setup (fetched …/versions/16/setup-sdk): apps need "Android Gradle plugin 8.9.0-rc01 or higher" to compile against API 36.

→ **targetSdk 36 is mandatory today; compileSdk 36 follows** (targetSdk cannot exceed compileSdk; media3 1.11.1 and `androidx.pdf` also require compileSdk 36 — see §2.11).

### 1.2 `settings.gradle.kts`

```kotlin
pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // ONLY because Tesseract4Android 4.9.0 is published on JitPack, not Maven Central:
        maven("https://jitpack.io") {
            content { includeGroup("cz.adaptech.tesseract4android") }
        }
    }
}

rootProject.name = "DigitalConverter"
include(":app")
```

### 1.3 Root `build.gradle.kts`

```kotlin
plugins {
    id("com.android.application") version "8.10.1" apply false
    id("org.jetbrains.kotlin.android") version "2.0.21" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.21" apply false
    id("org.jetbrains.kotlin.plugin.serialization") version "2.0.21" apply false // only if kotlinx.serialization is used
    id("com.google.devtools.ksp") version "2.0.21-1.0.28" apply false
}
```

### 1.4 `app/build.gradle.kts` (android block + dependency set)

```kotlin
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization") // optional
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.example.digitalconverter"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.digitalconverter"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables { useSupportLibrary = true }
        // ffmpeg-kit 8.x publishes ONLY arm64-v8a + x86_64; 32-bit devices are not supported.
        ndk { abiFilters += listOf("arm64-v8a", "x86_64") }
    }

    buildTypes {
        release {
            isMinifyEnabled = true          // R8: AARs ship their own consumer rules (verified)
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }

    buildFeatures { compose = true }

    packaging {
        resources {
            excludes += setOf(
                "/META-INF/{AL2.0,LGPL2.1}",
                "/META-INF/DEPENDENCIES",
                "/META-INF/LICENSE*",
                "/META-INF/NOTICE*"
            )
        }
        jniLibs {
            useLegacyPackaging = false      // REQUIRED for 16 KB / Play: .so compressed => not acceptable
            // Uncomment ONLY if two AARs both ship libc++_shared.so
            // (e.g. ffmpeg-kit-full + dev.ffmpegkit-maintained:tesseract-android):
            // pickFirsts += "**/libc++_shared.so"
        }
    }

    // Local APK builds only — for Play use an AAB and let Play split ABIs (never ship a universal APK).
    splits {
        abi {
            isEnable = true
            reset()
            include("arm64-v8a", "x86_64")
            isUniversalApk = false
        }
    }
}

dependencies {
    // --- UI (sibling-proven set) ---
    implementation(platform("androidx.compose:compose-bom:2024.12.01")) // Compose 1.7.6 / Material3 1.3.1
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-core")   // NOT icons-extended (35.7 MB AAR)
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.navigation:navigation-compose:2.8.5")

    // --- Storage / background (sibling-proven set) ---
    implementation("androidx.room:room-runtime:2.6.1")          // optional; project uses DataStore only
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")
    implementation("androidx.datastore:datastore-preferences:1.1.1")
    implementation("androidx.work:work-runtime-ktx:2.10.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3") // Kotlin-2.0.21-compatible line
    implementation("androidx.documentfile:documentfile:1.1.0")
    implementation("androidx.exifinterface:exifinterface:1.4.2")
    implementation("io.coil-kt:coil-compose:2.7.0")
    // implementation("io.coil-kt:coil-gif:2.7.0") // only if animated GIF thumbnails are needed

    // --- Conversion engines (LGPL/Apache/MIT, verified) ---
    implementation("dev.ffmpegkit-maintained:ffmpeg-kit-full:8.1.9") // LGPL-3.0, arm64-v8a + x86_64
    implementation("com.tom-roush:pdfbox-android:2.0.27.0")        // Apache-2.0
    implementation("org.apache.commons:commons-compress:1.28.0")  // Apache-2.0
    implementation("org.tukaani:xz:1.12")                         // 0BSD
    implementation("net.lingala.zip4j:zip4j:2.11.6")              // Apache-2.0 (AES zip)
    implementation("com.github.junrar:junrar:8.1.2")              // UnRAR license — read-only RAR
    implementation("pl.droidsonroids.gif:android-gif-drawable:1.2.32") // MIT

    // --- OCR (choose ONE strategy) ---
    implementation("cz.adaptech.tesseract4android:tesseract4android:4.9.0") // JitPack, Apache-2.0
    // OR (Maven Central, arm64-only, bundled eng model):
    // implementation("dev.ffmpegkit-maintained:tesseract-android:5.5.0")

    // --- Optional, later phase: on-device ONNX models (adds ~36 MB arm64) ---
    // implementation("com.microsoft.onnxruntime:onnxruntime-android:1.32.0") // MIT
    // implementation("androidx.heifwriter:heifwriter:1.1.0")                // HEIF/AVIF encode, API 28+

    debugImplementation("androidx.compose.ui:ui-tooling")
}
```

### 1.5 `gradle.properties` (minimum)

```properties
org.gradle.jvmargs=-Xmx4096m -Dfile.encoding=UTF-8
org.gradle.parallel=true
android.useAndroidX=true
android.nonTransitiveRClass=true
kotlin.code.style=official
```

`local.properties` must contain `sdk.dir=C\:\\Android\\Sdk` (sibling uses exactly that escaped form).

### 1.6 Build command

```powershell
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-21.0.13.9-hotspot"
C:\gradle\gradle-8.11.1\bin\gradle.bat :app:assembleDebug --no-daemon
# optional: generate a wrapper (sibling has none):  ... gradle.bat wrapper --gradle-version 8.11.1
```

### 1.7 16 KB page-size compliance (mandatory for a Play submission with native code)

Android docs (fetched https://developer.android.com/guide/practices/page-sizes):
> "all apps targeting Android 15 (API level 35) and higher must support 16 KB memory page sizes on 64-bit devices on Google Play. Starting February 1, 2027, if your app updates don't support 16 KB memory page sizes, you won't be able to release these updates."
> "Upgrade to AGP version 8.5.1 or higher and use uncompressed shared libraries."

Because this app bundles native code it **is** in scope. I parsed the ELF `PT_LOAD` `p_align` of every `.so` we plan to ship (2026-11-16):

| AAR / lib | `.so` files checked | `p_align` result |
|---|---|---|
| ffmpeg-kit-full 8.1.9 (arm64-v8a) | 10/10 (`libavcodec`, `libavfilter`, `libavformat`, `libavutil`, `libavdevice`, `libswresample`, `libswscale`, `libffmpegkit`, `libffmpegkit_abidetect`, `libc++_shared`) | all `0x4000`, **16 KB OK** |
| onnxruntime-android 1.32.0 (arm64-v8a) | `libonnxruntime.so`, `libonnxruntime4j_jni.so` | `0x4000`, **16 KB OK** |
| tesseract4android 4.9.0 (arm64-v8a) | `libtesseract.so`, `libleptonica.so`, `libjpeg.so`, `libpngx.so` | `0x4000`, **16 KB OK** |
| dev.ffmpegkit-maintained:tesseract-android 5.5.0 (arm64-v8a) | `libtesseract.so`, `libleptonica.so`, `libtesseract_jni.so`, `libc++_shared.so` | `0x4000`, **16 KB OK** |
| android-gif-drawable 1.2.32 (arm64-v8a) | `libpl_droidsonroids_gif.so` | `0x4000`, **16 KB OK** |

Final-build gate (build-tools 35.0.0 installed here):
```powershell
# ZIP alignment of stored .so entries (last line must read "Verification successful")
C:\Android\Sdk\build-tools\35.0.0\zipalign.exe -v -c -P 16 4 app-release.apk
# AAB config check
bundletool dump config --bundle=app-release.aab | Select-String alignment   # expect PAGE_ALIGNMENT_16K
```
Also: keep `useLegacyPackaging = false` (uncompressed), never ship a universal APK, and run one internal test on a 16 KB device image.

---

## 2. Master dependency table (every version fetched 2026-11-16)

Legend: **Latest** = newest *stable* version found in live metadata (pre-releases excluded); **Proven** = version already proven by the sibling APK; **APK cost** = measured artifact bytes / raw arm64 `.so` bytes where native; "AAR x B" = compressed download size of the AAR (all ABIs inside).

### 2.1 Video / audio

| Coordinate | Latest verified | License (SPDX) | minSdk | APK size cost (measured) | Commercial OK? | Risk / notes |
|---|---|---|---|---|---|---|
| `dev.ffmpegkit-maintained:ffmpeg-kit-full:8.1.9` | **8.1.9** (`<release>8.1.9</release>`, lastUpdated 20260926); 8.1.7/8.1.8 also present | **LGPL-3.0** (POM + repo) | **24** (AAR manifest) | AAR **31,057,330 B** (2 ABIs); raw arm64 `.so` **31,527,400 B (~30.06 MiB)**, x86_64 **38,200,216 B** | **Yes**, if LGPL obligations met (below) | FFmpeg n8.1.2; NDK r27c; 16 KB enforced in CI. Only **arm64-v8a + x86_64**. Single-maintainer fork; original retired 2025-01-06 and binaries pulled 2025-04-01. |
| `…:ffmpeg-kit-audio:8.1.9` | 8.1.9 | LGPL-3.0 | 24 | AAR **21,537,944 B**; arm64 **21,341,704 B (20.35 MiB)** | Yes | lame, opus, libvorbis, speex, opencore-amr, shine, soxr, twolame, vo-amrwbenc, libilbc |
| `…:ffmpeg-kit-video:8.1.9` | 8.1.9 | LGPL-3.0 | 24 | AAR **25,207,612 B**; arm64 **25,024,320 B (23.87 MiB)** | Yes | dav1d, libvpx, kvazaar, libass, libtheora, libwebp, zimg, freetype, fontconfig, fribidi, snappy |
| `…:ffmpeg-kit-https:8.1.9` | 8.1.9 | LGPL-3.0 | 24 | AAR **21,880,804 B**; arm64 **21,416,208 B** | Yes | gmp + gnutls (network only; offline app does not need it) |
| `…:ffmpeg-kit-min:8.1.9` | 8.1.9 | LGPL-3.0 | 24 | AAR **17,140,030 B**; arm64 **17,591,568 B (16.78 MiB)** | Yes | bare FFmpeg; no external codec libs |
| `…:ffmpeg-kit-*-gpl:8.1.9` | 8.1.9 | **GPL-3.0** | 24 | same order | **NO** for a closed-source app | x264/x265/xvid/vidstab → copyleft over the whole app. **Do not use.** |
| `androidx.media3:media3-transformer:1.11.1` | **1.11.1** (`1.12.0-beta01` newer but beta) | **Apache-2.0** (POM) | **23** (manifest) | AAR **506,251 B** (+ media3-common **644,502 B**, media3-muxer **109,910 B**, effect **402,882 B**) | Yes | **Requires compileSdk 36** (`minCompileSdk=36`). Hardware/MediaCodec only. |
| `androidx.media3:media3-effect:1.11.1` | 1.11.1 | Apache-2.0 | 23 | AAR 402,882 B | Yes | video effects/GL; optional |
| `androidx.media3:media3-common:1.11.1` | 1.11.1 | Apache-2.0 | 23 | AAR 644,502 B | Yes | required by transformer |

**FFmpegKit codec reality check** (fork README, fetched 2026-11-16): decode is always available (H.264/H.265/AV1/VP8-9), and what changes between tiers is the **encoder** set. `full` = audio + video + https (LGPL). The `-gpl` variants add x264/x265/xvid/vidstab and are GPL-3.0. MediaCodec hardware encode is included in **every** variant.

Consequences for the format matrix:
- **H.264 encode**: no x264 (GPL). Options: MediaCodec via FFmpeg (`h264_mediacodec`), FFmpeg-native MPEG-4/MPEG-2/FLV codecs. **`openh264` presence in `full` is UNVERIFIED** — the AAR ships `license_openh264.txt` (§8.1), so likely included; probe `-encoders` and cache the answer.
- **HEVC encode**: `kvazaar` (LGPL) software + MediaCodec HEVC hardware.
- **H.264/HEVC/AV1/VP8/VP9 decode, MP3/AAC/Opus/Vorbis/FLAC/AMR/WAV encoders** are covered by FFmpeg built-ins + the LGPL libs above.
- Containers/muxers: FFmpeg built-in muxers (MP4/MKV/WebM/AVI/MOV/FLV/TS/MP3/OGG/WAV/FLAC, …) are present; the fork publishes **no itemized `-formats` list** → per-format capability is **UNVERIFIED** until an on-device probe. GIF encode is FFmpeg-native and likely present but **UNVERIFIED**.

**LGPL-3.0 compliance obligations for APK distribution** (fork `LICENSE` + `THIRD-PARTY-NOTICES.txt`, fetched 2026-11-16):
1. Your app code is **not** affected: "Your application code is NOT affected by the LGPL. You may keep your source code proprietary and sell your application commercially."
2. Include the notices in an "Open Source Licenses"/About screen or bundle them as assets.
3. Provide the corresponding source of the LGPL libraries: `https://github.com/ffmpegkit-maintained/ffmpeg`. ⚠ The AAR's `res/raw/source.txt` still points to the retired Arthenica wiki — use the fork URL.
4. Allow replacement/relinking: the libs are dynamically linked `.so` files inside the AAR.
5. Keep copyright notices; do not misrepresent the license; never use the `-gpl` variants.
6. Practical UI: an About screen listing FFmpeg (LGPL-2.1+ with LGPL-3.0 portions), FFmpegKit (LGPL-3.0), smart-exception (BSD-3), plus the bundled external libs. The AAR's 34 `res/raw/license_*.txt` resources merge into the app and can be enumerated at runtime by name.
- **Packaging / ProGuard**: the AAR ships consumer rules keeping `com.arthenica.ffmpegkit.FFmpegKitConfig` (native methods, `log`, `statistics`, `safOpen`, `safClose`) and `AbiDetect`; with `isMinifyEnabled = true` no manual rules are needed. Java package stays `com.arthenica.ffmpegkit` — only the groupId changed.
- **No NDK** is needed (prebuilt AARs). Only arm64-v8a + x86_64 → detect `Build.SUPPORTED_ABIS` and fail gracefully on 32-bit devices. `libc++_shared.so` is bundled; duplicate only if combined with another AAR that also ships it (Tesseract4Android does **not**; `dev.ffmpegkit-maintained:tesseract-android` **does**).

**Media3 Transformer capabilities** (fetched …/media/media3/transformer): converts formats (transcoding), trims, crops, applies effects; "Transformer also relies on MediaCodec for encoding, which limits the output formats supported"; default output "standard MP4 files using InAppMp4Muxer". → FFmpegKit is the conversion engine; Media3 only if a fast hardware MP4 path is later needed.

### 2.2 PDF (and the AGPL traps)

| Coordinate / API | Latest verified | License | minSdk | APK size cost | Commercial OK? | Risk / notes |
|---|---|---|---|---|---|---|
| `com.tom-roush:pdfbox-android:2.0.27.0` | **2.0.27.0** | **Apache-2.0** (POM) | **14** manifest; README: "Requires API 19 or greater for full functionality" | AAR **3,254,019 B** (pure Java + assets: CMaps, AFM, LiberationSans TTF) | **Yes** | Best offline PDF text extraction (PDF→TXT), split/merge/rotate, encryption, metadata, basic PDF creation. **Memory:** whole-document model in Java heap; large PDFs need heap budget/streaming; run conversions in WorkManager. Consumer ProGuard rules ship in the AAR. |
| `android.graphics.pdf.PdfRenderer` | framework — **API 21** | Android platform | 21 | 0 | Yes | PDF **page rasterization** to `Bitmap` only (PDF→PNG/JPEG/thumbnails); no text extraction/editing. Close pages/descriptor promptly. |
| `android.graphics.pdf.PdfDocument` | framework — **API 19** | Android platform | 19 | 0 | Yes | Simple **PDF creation** by drawing on `Canvas` (image→PDF, bitmap→PDF). No text layout engine (Arabic shaping via Paint/StaticLayout). |
| `androidx.pdf:pdf-viewer` | **1.0.0-beta01** (no stable) | Apache-2.0 | **28** | AAR 1,290,390 B | Yes (license-wise) | **Defer/don't use now:** `minCompileSdk=36`, `minCompileSdkExtension=19`, `minAndroidGradlePluginVersion=8.9.1`. This machine has no `sdk-extensions` directory → build may fail; viewer-only, not a converter. |
| PDFium (native engine) | no official, maintained Android Maven artifact found; only Central hit `io.github.rhariskumar3:pdfium:1.0.5` (third-party viewer SDK) | PDFium itself **BSD-3-Clause-style** (fetched LICENSE) | — | unknown | BSD-3 allows commercial use | **UNVERIFIED**: no maintained binding; would need NDK build. Not needed for MVP (PdfRenderer + PDFBox cover render/extract). Don't adopt the random 1.0.5 AAR without source review. |
| iText (`com.itextpdf:*`) | — | **AGPL-3.0 or commercial** (fetched `LICENSE.md`) | — | — | **NO** (without a paid commercial license) | **Do not ship.** AGPL copyleft over the whole app. |
| MuPDF | — | **AGPL-3.0** (fetched `COPYING`) | — | — | **NO** | **Do not ship.** |

Note: PDF→Word/Excel (DOCX/XLSX) with layout is **not** a solved offline Android problem with these libs — PDFBox extracts text only (layout lost). Declare it as text extraction, not "PDF→DOCX conversion".

### 2.3 OCR

| Coordinate | Latest verified | License | minSdk | APK size cost | Commercial OK? | Risk / notes |
|---|---|---|---|---|---|---|
| `cz.adaptech.tesseract4android:tesseract4android:4.9.0` (**JitPack only**) | JitPack `<release>4.9.0</release>`; **not on Maven Central** (Central search numFound: 0) | **Apache-2.0** (GitHub repo) | **21** (manifest) | AAR **12,848,454 B** (4 ABIs), SHA-256 `BCE5D6413A1A5AE3D7240033FBBC851BA3217D0A08D9769400E17A077F42CB2A` — **PIN shared with docs/research/model-licenses.md rev 2** (§2.12); arm64 `.so` **7,514,368 B (~7.17 MiB)** (`libtesseract` 4,164,360; `libleptonica` 2,878,240; `libjpeg` 251,888; `libpngx` 219,880); **no traineddata bundled** | **Yes** | Tesseract 5.x build (exact upstream versions not re-verified). Requires the JitPack repo; **JitPack returned HTTP 500 to model-auditor on 2026-11-16 but my download and the project build resolved fine** → keep Gradle cache, pin SHA-256, vendor fallback AAR (§8.6). No `libc++_shared` → no duplicate-.so conflict with ffmpeg. |
| `dev.ffmpegkit-maintained:tesseract-android:5.5.0` | **5.5.0** | **Apache-2.0** (POM) | **24** (manifest) | AAR **5,014,159 B**; arm64 `.so` **7,073,088 B (~6.75 MiB)** + bundled `assets/tessdata/eng.traineddata` **4,113,088 B**; x86_64 **not** shipped | **Yes** | Tesseract 5.5.0 + Leptonica 1.84.1, 16 KB aligned, Kotlin `suspend` API; free tier arm64-v8a only, single-thread; other languages via `addLanguage()`; Pro tier (paid) adds models/OpenMP/x86_64. Bundles `libc++_shared.so` (duplicate caveat with ffmpeg). |
| `com.google.mlkit:text-recognition:16.0.1` (bundled) | **16.0.1** | **Proprietary — Google ML Kit Terms of Service** | 21 (docs: "Android devices running API level 21 or higher") | bundled = larger app; exact MB **UNVERIFIED** | Yes under ML Kit ToS; **review terms with legal/halal clearance** | Bundled model, faster init, no Play Services dependency; closed-source model. |
| `com.google.android.gms:play-services-mlkit-text-recognition:19.0.2` (unbundled) | **19.0.2** | **Proprietary — ML Kit ToS** | 21 | small APK, model downloaded dynamically | Yes under ML Kit ToS | Requires Google Play services (not guaranteed on Iraq-market devices) → **bad fit for offline-first**; docs: "unbundled (smaller app size, dynamic download)". |
| `tesseract-ocr/tessdata_best` (models) | repo pushed 2024-03-09; `eng` **15,400,601 B**, `ara` **12,603,724 B**, `osd` **10,562,727 B** | **Apache-2.0** (GitHub API) | — | per-language download | **Yes** | Download on demand; verify SHA-256; store in app files dir. `tessdata_fast` for comparison: `eng` **4,113,088 B**, `ara` **1,432,056 B**. |
| PaddleOCR / other ONNX OCR | — | models Apache-2.0 (verify per model card) | — | model + ORT 36 MB arm64 | **UNVERIFIED** | Only if ONNX Runtime is added anyway; not an MVP dependency. |

OCR verdict: **Tesseract4Android 4.9.0 (JitPack) + downloadable tessdata_best** is the best offline, open-source, commercially-safe OCR path. Prefer it over ML Kit to avoid proprietary model terms and Play-services dependency.

### 2.4 On-device ML runtime

| Coordinate | Latest verified | License | minSdk | APK size cost | Commercial OK? | Risk / notes |
|---|---|---|---|---|---|---|
| `com.microsoft.onnxruntime:onnxruntime-android:1.32.0` | **1.32.0** (`<release>1.32.0</release>`, lastUpdated 20261113) | **MIT** (POM) | **24** (manifest) | AAR **59,335,382 B**; raw arm64 `libonnxruntime.so` **36,307,784 B** + `libonnxruntime4j_jni` 122,192; x86_64 **45,079,616 B**; armeabi-v7a 25,791,836 B; x86 43,328,156 B | **Yes** (MIT) | EPs per ORT docs: CPU (default), **Android NNAPI**, **XNNPACK**, Qualcomm **QNN**. 16 KB aligned (verified). **No per-ABI Maven artifact** exists (§8.5) — only this all-ABI AAR. Models have **separate licenses**. `onnxruntime-mobile` is stale (latest 1.18.0) — do not use. |
| `com.microsoft.onnxruntime:onnxruntime-android-qnn` | **1.29.0** (older than 1.32.0) | MIT (same project) | — | AAR **9,419,194 B** | Yes | Qualcomm QNN-targeted package; contents/ABIs **UNVERIFIED**; not a drop-in replacement for non-Qualcomm devices. |

Recommendation: keep ONNX Runtime **out of the v1 APK** (or in a separate Play feature module) until a concrete model is selected — 34.74 MiB per ABI is the single biggest optional cost.

### 2.5 Archives / compression

| Coordinate | Latest verified | License | minSdk | APK size cost | Commercial OK? | Risk / notes |
|---|---|---|---|---|---|---|
| `org.apache.commons:commons-compress:1.28.0` | **1.28.0** (lastUpdated 20250729) | **Apache-2.0** (LICENSE file fetched; POM license block empty) | Java library (Android-safe) | JAR **1,117,221 B** | **Yes** | Docs: "7z can read most compressed and encrypted archives but only write unencrypted ones. LZMA(2) support in 7z requires XZ for Java as well." Also ar, cpio, tar, zip, gzip, XZ, bzip2, lz4, Brotli, Zstd, arj, dump. **7z write = unencrypted only; RAR write = impossible.** |
| `org.tukaani:xz:1.12` | **1.12** (lastUpdated 20260301) | **0BSD** (POM) | Java | JAR **168,792 B** | **Yes** | Needed for 7z LZMA/LZMA2 and .xz archives; pair with commons-compress. |
| `net.lingala.zip4j:zip4j:2.11.6` | **2.11.6** (lastUpdated 20260211) | **Apache-2.0** | Java | JAR **213,881 B** | **Yes** | AES-encrypted zip create/extract (`java.util.zip` cannot). |
| `com.github.junrar:junrar:8.1.2` | **8.1.2** (lastUpdated 20261009) | **UnRAR License** (POM; fetched text is RARLAB's "License for use and distribution of UnRAR") | Java | JAR **247,983 B** | **Qualified:** read-only use allowed; license restricts using the source to reverse-engineer the RAR compression algorithm. No RAR creation. | Pure-Java **RAR extraction only**; keep the license text in notices. |
| `java.util.zip` (platform) | — | Android platform | 1 | 0 | Yes | zip/deflate/gzip only. |

### 2.6 Serialization / config

| Coordinate | Latest verified | License | minSdk | APK size cost | Commercial OK? | Risk / notes |
|---|---|---|---|---|---|---|
| `org.jetbrains.kotlinx:kotlinx-serialization-json:1.13.0` (latest) / **`1.7.3` (use with Kotlin 2.0.21)** | 1.13.0 (lastUpdated 20261111); 1.7.3 verified present | **Apache-2.0** (POM) | JVM library | small (core + serialization-core, not individually measured) | **Yes** | Pin the runtime line matching the compiler: Kotlin 2.0.21 + 1.7.3 is proven; **1.13.0 with Kotlin 2.0.21 is UNVERIFIED**. |
| `org.yaml:snakeyaml:2.7` | **2.7** (lastUpdated 20260830) | **Apache-2.0** (POM) | Java | JAR **332,460 B** | **Yes** | For YAML presets/format rules; use `SafeConstructor` for untrusted YAML. |
| `com.google.code.gson:gson:2.15.0` | **2.15.0** (lastUpdated 20261024) | **Apache-2.0** (POM) | Java | JAR **316,396 B** | **Yes** | Prefer kotlinx-serialization for new code. |

### 2.7 UI / thumbnails / files

| Coordinate | Latest verified | License | minSdk | APK size cost | Commercial OK? | Risk / notes |
|---|---|---|---|---|---|---|
| `androidx.compose:compose-bom:2024.12.01` | **2024.12.01 exists**; latest BOM is **2026.09.00** | Apache-2.0 | — | BOM = versions only | Yes | POM mapping verified: `ui=1.7.6, foundation=1.7.6, runtime=1.7.6, material3=1.3.1, ui-tooling=1.7.6`. **Proven with Kotlin 2.0.21 by the sibling APK.** BOM 2026.09.00 + Kotlin 2.0.21 = **UNVERIFIED, do not mix.** |
| `androidx.compose.material3:material3` | **1.3.1** via BOM (latest stable standalone **1.4.0**; `1.5.0-rc01` newest) | Apache-2.0 | 21 | AAR (android variant) 4,965,333 B | Yes | Use BOM-managed 1.3.1. |
| `androidx.compose.material:material-icons-extended` | 1.7.6 | Apache-2.0 | 21 | **AAR 35,721,003 B; classes.jar 37,425,981 B** | Yes | **Size bomb:** debug dex includes all icon classes. Use `material-icons-core` (AAR 823,515 B) + explicit icons. |
| `androidx.activity:activity-compose:1.9.3` | **1.9.3** (sibling); latest stable **1.13.0** | Apache-2.0 | 21 | AAR **1,139,788 B** | Yes | Stick to 1.9.3; 1.13.0 with Kotlin 2.0.21 = **UNVERIFIED**. |
| `androidx.navigation:navigation-compose:2.8.5` | **2.8.5** (sibling); latest stable **2.10.2** | Apache-2.0 | 21 | AAR **1,228,033 B** | Yes | Stick to 2.8.5 for the proven build. |
| `androidx.core:core-ktx:1.13.1` | **1.13.1** (sibling); latest stable **1.19.1** | Apache-2.0 | 21 | AAR **171,581 B** | Yes | Stick to proven. |
| `androidx.lifecycle:lifecycle-runtime-ktx:2.8.7` | **2.8.7** (sibling); latest stable **2.11.0** | Apache-2.0 | 21 | small | Yes | Stick to proven. |
| `io.coil-kt:coil-compose:2.7.0` | **2.7.0** (lastUpdated 20240717; final 2.x) | **Apache-2.0** (POM) | **21** (manifest) | coil-compose AAR 17,808 B + coil 7,258 B + coil-base **428,279 B**; coil-gif **42,918 B** | Yes | Coil 3.x exists (`io.coil-kt.coil3:coil-compose:3.6.3`, KMP coordinates; plain `.aar` path 404s). Use Coil 2.7.0. |
| `androidx.documentfile:documentfile:1.1.0` | **1.1.0** | Apache-2.0 | **21** (manifest) | AAR **17,070 B** | Yes | SAF `DocumentFile` wrappers; `minCompileSdk=34`, `minAGP 8.1.1`. Project has 1.0.1 (fine). |
| `androidx.exifinterface:exifinterface:1.4.2` | **1.4.2** (newest stable; 1.5.0-beta01 exists) | Apache-2.0 | **21** | AAR **75,550 B** | Yes | EXIF read/write for image conversions; `minCompileSdk=34`. Project has 1.3.7 (fine). |

### 2.8 Persistence / background

| Coordinate | Latest verified | License | minSdk | APK size cost | Commercial OK? | Risk / notes |
|---|---|---|---|---|---|---|
| `androidx.room:room-runtime:2.6.1` + `room-ktx:2.6.1` + KSP `room-compiler:2.6.1` | **2.6.1** (sibling-proven); latest stable **2.8.5** | Apache-2.0 | 21 | room-runtime AAR **242,741 B**, room-ktx **39,256 B**; compiler is build-time | Yes | With Kotlin 2.0.21 + KSP 2.0.21-1.0.28 use **2.6.1**. Room 2.8.x with this KSP = **UNVERIFIED**. |
| `com.google.devtools.ksp` plugin | **2.0.21-1.0.28** (sibling); latest plugin line **2.3.14** | Apache-2.0 | build-time | 0 | Yes | Must match Kotlin: `2.0.21-1.0.28` is correct for Kotlin 2.0.21. |
| `androidx.datastore:datastore-preferences:1.1.1` | **1.1.1** (sibling); latest stable **1.3.0** | Apache-2.0 | 21 | small | Yes | Stick to 1.1.1; 1.3.0 = **UNVERIFIED** with this toolchain. |
| `androidx.work:work-runtime-ktx:2.10.0` | **2.10.0** (sibling); latest stable **2.12.0** | Apache-2.0 | 23 | work-runtime-ktx AAR 5,928 B + work-runtime (~few hundred KB) | Yes | WorkManager is the right place for long conversions + mediaProcessing FGS. |
| `org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0` | **1.9.0** (sibling); latest stable **1.11.0** | Apache-2.0 | JVM | small | Yes | 1.11.0 with Kotlin 2.0.21 = **UNVERIFIED**; stay on 1.9.0. |

### 2.9 Images / GIF / HEIF

| API / coordinate | Latest verified | License | minSdk | APK size cost | Commercial OK? | Risk / notes |
|---|---|---|---|---|---|---|
| `android.graphics.ImageDecoder` | framework — **API 28** | Android platform | 28 | 0 | Yes | Decode HEIF/AVIF/animated WebP/GIF on 28+; **gate by `Build.VERSION.SDK_INT >= 28`** (minSdk is 26). |
| `android.graphics.drawable.AnimatedImageDrawable` | framework — **API 28** | Android platform | 28 | 0 | Yes | Animated GIF/WebP playback without extra libs. |
| `android.graphics.Bitmap.CompressFormat` | framework | Android platform | 1 | 0 | Yes | JPEG/PNG API 1; `WEBP` API 14 (deprecated 30); `WEBP_LOSSY`/`WEBP_LOSSLESS` API 30. |
| `androidx.heifwriter:heifwriter:1.1.0` | **1.1.0** (newest stable; 1.2.0-rc02 exists) | Apache-2.0 | **28** (manifest) | AAR **53,493 B** | Yes | HEIF/AVIF **encoding** (`HeifWriter`/`AvifWriter`); needs device HEVC encoder; `minCompileSdk=34`. |
| `pl.droidsonroids.gif:android-gif-drawable:1.2.32` | **1.2.32** (lastUpdated 20260618) | **MIT** (POM) | **21** (manifest) | AAR **144,050 B**; arm64 `.so` **42,416 B** | **Yes** | GIF **decode + animated display**; 16 KB aligned; not an encoder. |
| GIF **encoding** | — | — | — | — | — | No maintained dedicated encoder dependency verified. Expected path: **FFmpeg** gif encoder/muxer (filters `palettegen`/`paletteuse` in `libavfilter`) or a custom LZW encoder. **UNVERIFIED** until probed. |
| HEIC/AVIF thumbnails | — | — | — | — | — | Coil 2.7.0 uses platform decoding → API 28+; on 26–27 either no thumbnails or decode via FFmpeg. |

### 2.10 Explicitly NOT shippable / not feasible on Android

Licenses fetched from upstream license files on 2026-11-16.

| Engine | License (verified) | Why not in the APK |
|---|---|---|
| x264 / x265 (and ffmpeg-kit `-gpl` artifacts) | **GPL-2.0**; ffmpeg-kit `-gpl` = **GPL-3.0** | Copyleft would force the whole app GPL. H.264/HEVC encoding is done via MediaCodec/kvazaar instead. |
| iText 7/8 | **AGPL-3.0** or paid commercial | AGPL over the app; PDFBox covers the need. |
| MuPDF | **AGPL-3.0** | Same. |
| Ghostscript | **AGPL-3.0** | Same; no viable Android port. |
| calibre | **GPL-3.0** | Copyleft + Python/desktop stack. |
| Pandoc | **GPL-2.0-or-later** (with exceptions) + **Haskell** | No practical Android port; GPL; huge. Markdown↔HTML must be implemented natively. |
| LibreOffice | **MPL-2.0** (not GPL, but) | **No Android port**, huge native/UNO stack; DOCX/XLSX/PPTX rendering with Arabic layout effectively impossible. Do not plan LibreOffice-based conversion. |
| UnRAR compressor / RAR creation | **UnRAR License** restricts algorithm reverse-engineering | RAR is proprietary; extract-only via junrar; **no RAR output**. |
| ExifTool | **Perl Artistic License** | Perl interpreter impractical; use `androidx.exifinterface` + FFmpeg metadata. |
| GPL/AGPL engines generally | mixed GPL/AGPL | Same copyleft/feasibility reasoning. |
| ML Kit models | Proprietary Google ToS | Not a license violation if terms followed, but a clearance decision (legal/halal), not engineering. |

**Scope consequence:** without LibreOffice/Pandoc, declare out of scope for v1: DOCX→PDF, XLSX→PDF, PPTX→PDF, EPUB→PDF/MOBI, PDF→DOCX with layout. Feasible: PDF text extraction (PDFBox), PDF rasterize (PdfRenderer), image↔PDF (PdfDocument), text conversions (TXT/CSV/JSON/XML/YAML/HTML/Markdown), media (FFmpeg), images (Bitmap/ImageDecoder/FFmpeg), archive recompress/extract.

### 2.11 Dependencies that force compileSdk 36 (AAR-metadata audit)

Inspected `META-INF/com/android/build/gradle/aar-metadata.properties` inside each cached AAR (2026-11-16). **Nothing requires > 36**; the only 36s are:

| Artifact | Version | minCompileSdk | minCompileSdkExtension | minAndroidGradlePluginVersion | In project? |
|---|---|---|---|---|---|
| `androidx.media3:media3-transformer` | 1.11.1 | **36** | 0 | 1.0.0 | no (optional path only) |
| `androidx.pdf:pdf-viewer` | 1.0.0-beta01 | **36** | **19** | **8.9.1** | no (do not add without SDK extension) |
| `androidx.documentfile:documentfile` | 1.1.0 | 34 | 0 | 8.1.1 | project has 1.0.1 (fine) |
| `androidx.exifinterface:exifinterface` | 1.4.2 | 34 | 0 | 1.0.0 | project has 1.3.7 (fine) |
| `androidx.heifwriter:heifwriter` | 1.1.0 | 34 | 0 | 8.1.1 | no |
| `dev.ffmpegkit-maintained:ffmpeg-kit-*` | 8.1.9 | 1 | 0 | 1.0.0 | **yes** |
| `com.microsoft.onnxruntime:onnxruntime-android` | 1.32.0 | 1 | 0 | 1.0.0 | **yes** (recommended to drop) |
| `cz.adaptech.tesseract4android` | 4.9.0 | 1 | 0 | 1.0.0 | **yes** |
| `com.tom-roush:pdfbox-android` | 2.0.27.0 | 1 | — | 1.0.0 | **yes** |
| `pl.droidsonroids.gif:android-gif-drawable` | 1.2.32 | 1 | 0 | 1.0.0 | no (planned) |
| `io.coil-kt:coil-compose` | 2.7.0 | 1 | 0 | 1.0.0 | **yes** |
| Compose 1.7.6 / Material3 1.3.1 / activity 1.9.3 / navigation 2.8.5 / work 2.10.0 / datastore 1.1.1 / core-ktx 1.13.1 / lifecycle 2.8.7 | as declared | ≤ 36 | 0 | ≤ 8.0 | **yes** — proven by successful resolution + compile under compileSdk 36 in this project |

**Why compileSdk 36 is mandatory anyway:** (1) `targetSdk = 36` cannot exceed `compileSdk` and Play requires target 36 since 2026-08-31; (2) AGP 8.10.x max API level = 36; (3) Android 16 setup requires AGP ≥ 8.9.0-rc01 for API 36. Full behavior-change analysis: §8.3.

### 2.12 Artifact pins shared with `docs/research/model-licenses.md` (rev 2)

| Artifact | Pinned identity | Evidence / caveats |
|---|---|---|
| `cz.adaptech.tesseract4android:tesseract4android` | **4.9.0**, AAR **12,848,454 B**, SHA-256 **`BCE5D6413A1A5AE3D7240033FBBC851BA3217D0A08D9769400E17A077F42CB2A`** | My direct JitPack download 2026-11-16; the project build resolved it; the same-day JitPack HEAD 500 seen by model-auditor is recorded as **transient**, not a version failure. The AAR contains only `classes.jar` + `annotations.zip` — **no licence/notice file**, so the app must ship Apache-2.0 + Leptonica BSD-2-Clause + IJG (libjpeg) + libpng texts itself. Upstream versions (Tesseract 5.5.1 / Leptonica 1.85.0 / libjpeg v9f / libpng 1.6.48) are upstream **README claims → UNVERIFIED from the AAR**. Leptonica BSD-2 licence text still to be sourced (upstream LICENSE 404; pull from Leptonica repo history or Tesseract4Android sources at build). |
| `com.microsoft.onnxruntime:onnxruntime-android` | **1.32.0**, AAR **59,335,382 B** (all ABIs), MIT | Maven Central metadata (lastUpdated 20261113) + measured AAR + manifest minSdk 24. No per-ABI Maven artifact exists (§8.5). Models are separate licensed downloads. |

These two pins are the values stored in `docs/research/model-licenses.md`; if either artifact is upgraded, both docs must change together (and the AAR re-hashed).

---

## 3. APK size budget (measured, not guessed)

### 3.1 Measured engine cost per ABI

| Component | arm64-v8a raw `.so` (shipped uncompressed with `useLegacyPackaging=false`) | x86_64 raw `.so` | Compressed AAR (both ABIs) |
|---|---|---|---|
| ffmpeg-kit-full 8.1.9 | **31,527,400 B ≈ 30.06 MiB** | 38,200,216 B ≈ 36.4 MiB | 31,057,330 B |
| ffmpeg-kit-video 8.1.9 | 25,024,320 B ≈ 23.87 MiB | 29,263,608 B | 25,207,612 B |
| ffmpeg-kit-audio 8.1.9 | 21,341,704 B ≈ 20.35 MiB | 24,687,032 B | 21,537,944 B |
| ffmpeg-kit-min 8.1.9 | 17,591,568 B ≈ 16.78 MiB | 20,689,440 B | 17,140,030 B |
| tesseract4android 4.9.0 (no model) | 7,514,368 B ≈ 7.17 MiB | ~7.8 MiB | 12,848,454 B (4 ABIs) |
| tesseract-android 5.5.0 (incl. eng model 4,113,088 B) | 7,073,088 B ≈ 6.75 MiB | not shipped | 5,014,159 B |
| onnxruntime-android 1.32.0 | **36,429,976 B ≈ 34.74 MiB** | 45,079,616 B ≈ 43.0 MiB | 59,335,382 B (4 ABIs) |
| android-gif-drawable 1.2.32 | 42,416 B | 41,408 B | 144,050 B (4 ABIs) |
| pdfbox-android 2.0.27.0 | pure Java, AAR 3,254,019 B | — | 3,254,019 B |
| Compose + AndroidX + Room/Work/DataStore (whole app, no media engines) | sibling `app-debug.apk` = **10,971,650 B** (debug; release with R8 ≈ 7–9 MB) | — | — |
| material-icons-extended 1.7.6 | AAR **35,721,003 B**, classes.jar 37,425,981 B (debug dex includes it) | — | — |

### 3.2 Estimated release APK / Play download

| Scenario | Estimated APK (arm64-v8a only) | Notes |
|---|---|---|
| MVP: Compose app + FFmpeg full + PDFBox + Tesseract4Android + archives | **≈ 50–56 MB** (baseline 9–11 + 30.06 + 3.3 + 7.17 + ~1) | Estimate from measured components; verify with a real build. Use ABI splits/AAB. |
| + ONNX Runtime (optional models) | **≈ 88–92 MB** arm64 | ONNX adds ~36.4 MB; put it in a Play feature module if shipped. |
| Debug build, current project dependency set, both ABIs (ffmpeg 2 ABIs + ONNX 2 ABIs + tesseract 2 ABIs + icons-extended) | **≈ 200–215 MB** | Debug APKs are unshrunk and contain both ABIs; use `ndk.abiFilters = listOf("arm64-v8a")` or splits locally. |
| Replacing icons-extended with icons-core + dropping ONNX (v1 recommendation) | **≈ 115–130 MB → ≈ 55–62 MB debug arm64**, release ≈ 45–52 MB, Play per-device download ≈ 30–40 MB | See §8.5 for the detailed estimate. |
| Universal APK (arm64 + x86_64, no splits) | **≈ 95–105 MB** (MVP) | Exceeds the legacy 100 MB APK limit on some stores — **use AAB + ABI splits**. Play base module limit is 500 MB compressed, so this is UX, not a hard blocker. |
| Smaller alternative: ffmpeg-kit-video or -audio instead of full | saves ~6 MB (video) / ~10 MB (audio) per ABI | Only if the format matrix allows; `full` recommended initially. |

Caveat: raw-`.so` numbers are what the APK stores when native libs are uncompressed (Android default, 16 KB-friendly). `useLegacyPackaging = true` compresses them (smaller APK, extraction at install) but contradicts the 16 KB guidance → **keep false**.

---

## 4. What NOT to ship in the APK / What to download instead

**Do not bundle:** GPL/AGPL engines (x264, x265, iText, MuPDF, Ghostscript), LibreOffice, Pandoc, calibre, unrar-based RAR *creation*, ExifTool, and any GPL ffmpeg-kit `-gpl` artifact. Reasons and verified licenses in §2.10.

**Do download on demand (in-app, checksums, resumable, data-only):**
- Tesseract language models from `tessdata_best` (`eng` 15,400,601 B, `ara` 12,603,724 B, `osd` 10,562,727 B; Apache-2.0) and optionally `tessdata_fast` (`eng` 4,113,088 B, `ara` 1,432,056 B) as a "fast" profile.
- Any ONNX model packs (data-only: `.onnx`/`.traineddata`/`.bin`/`.json`) — only after a model is chosen and its license cleared. **Play policy forbids downloading executable code** (`.so`/`.dex`) at runtime, so native runtimes (Tesseract, ONNX Runtime) must ship in the APK/AAB while models may download.
- Verified Play limits (fetched 2026-11-16): base module **500 MB** compressed, individual feature modules **500 MB**, asset packs **1.5 GB** each, cumulative **4 GB**; legacy APK limit **100 MB**; "New apps larger than 200 MB are now supported by either Play Feature Delivery or Play Asset Delivery."

**What is NOT feasible offline with this stack (say it in the UI):** DOCX/XLSX/PPTX→PDF (no LibreOffice), EPUB→MOBI (calibre), PDF→DOCX with layout. Provide text extraction and format-specific paths instead.

---

## 5. Risk register (engineering)

| # | Risk | Mitigation |
|---|---|---|
| 1 | **Build-config drift** (AGP/compileSdk/targetSdk) between brief and reality | Use §1 snippets (sibling-proven, AGP 8.10.1); never mix AndroidX versions across toolchains; targetSdk 36 is mandatory. |
| 2 | **FFmpegKit fork is single-maintainer** (community fork) | LGPL source is public → rebuildable; pin exact 8.1.9; keep vendored source/checksum; no `+` version ranges. |
| 3 | **APK size** (~50 MB MVP arm64; ~85 MB with ONNX) | ABI splits/AAB; drop ONNX; icons-core instead of icons-extended; download models; revisit Media3 for simple video later. |
| 4 | **270-format coverage** | FFmpeg covers hundreds, but probe on device (`-formats`, `-codecs`, `-encoders`) and hide unsupported targets in the UI. |
| 5 | **H.264/HEVC encode** depends on device MediaCodec + kvazaar; no x264 | Offer "hardware encode (fast)" vs "software (compatible)" settings; test on low-end devices (Tecno/Infinix/Samsung A-series). |
| 6 | **16 KB pages**: chosen `.so` verified; future deps may not be | CI gate `zipalign -c -P 16` + `bundletool` alignment; verify each new native lib. |
| 7 | **Duplicate `libc++_shared.so`** if ffmpeg-kit + tesseract-android both used | Use Tesseract4Android (no libc++) or add `pickFirsts`. |
| 8 | **JitPack dependency** for OCR | Gradle cache + SHA-256 pinning + vendored AAR fallback; scope JitPack repo (§8.6). |
| 9 | **PDFBox memory** on big/scanned PDFs | WorkManager + heap checks; cap preview pages; stream extraction; `largeHeap` only as last resort. |
| 10 | **ONNX Runtime size** (36 MB/ABI) | Drop from v1; feature module later. |
| 11 | **Arabic PDF text extraction** may return glyph-order garbage (PDFBox limitation) | Test Arabic PDFs early; treat PDF→text for Arabic as best-effort. |
| 12 | **No DOCX/XLSX/PPTX→PDF** | Scope out in copy/UI; only text extraction + image/PDF paths in v1. |

---

## 6. Evidence log (what was fetched, when, from where)

All requests made 2026-11-16 with PowerShell `Invoke-WebRequest -UseBasicParsing` (network proxy-safe; `web_fetch` is broken in this session).

**Maven Central metadata** (`https://repo1.maven.org/maven2/<path>/maven-metadata.xml`): `dev.ffmpegkit-maintained:ffmpeg-kit-*` → `<release>8.1.9</release>` (lastUpdated 20260926; 8.1.7/8.1.8 present); `com.microsoft.onnxruntime:onnxruntime-android` → 1.32.0 (20261113); `onnxruntime-android-qnn` → 1.29.0; `com.tom-roush:pdfbox-android` → 2.0.27.0; `org.apache.commons:commons-compress` → 1.28.0 (20250729); `org.tukaani:xz` → 1.12 (20260301); `com.github.junrar:junrar` → 8.1.2 (20261009); `net.lingala.zip4j:zip4j` → 2.11.6 (20260211); `org.jetbrains.kotlinx:kotlinx-serialization-json` → 1.13.0 (20261111); `org.yaml:snakeyaml` → 2.7; `com.google.code.gson:gson` → 2.15.0; `io.coil-kt:coil-compose` → 2.7.0; `io.coil-kt.coil3:coil-compose` → 3.6.3; `org.jetbrains.kotlinx:kotlinx-coroutines-android` → 1.11.0; `com.google.devtools.ksp:symbol-processing-gradle-plugin` → 2.3.14 (2.0.21-1.0.28 also present); `pl.droidsonroids.gif:android-gif-drawable` → 1.2.32; `org.jetbrains.kotlin:kotlin-gradle-plugin` + `compose-compiler-gradle-plugin` → 2.0.21 present; `com.arthenica:smart-exception-java` → 0.2.1 (BSD-3-Clause POM).

**Google Maven group indexes** (`https://dl.google.com/android/maven2/<group>/group-index.xml`): media3 (1.11.1 stable, 1.12.0-beta01 newest), compose-bom (2024.12.01 … 2026.09.00), material3 (1.4.0 stable, 1.5.0-rc01 newest), activity-compose (1.13.0 stable), navigation-compose (2.10.2), work (2.12.0), datastore (1.3.0), room (2.8.5), core-ktx (1.19.1), lifecycle (2.11.0 stable), documentfile (1.1.0), exifinterface (1.4.2), heifwriter (1.1.0 / 1.2.0-rc02), androidx.pdf pdf-viewer (1.0.0-beta01), mlkit text-recognition (16.0.1), play-services-mlkit-text-recognition (19.0.2), com.android.tools.build:gradle (AGP 8.7.3/8.9.2/8.10.0/**8.10.1**/8.11.1 present, 9.5.1 newest stable).

**AAR metadata / manifests / `.so`** — downloaded and unzipped: ffmpeg-kit-full 8.1.9 (manifest package `com.arthenica.ffmpegkit`, minSdk 24, 40 classes, proguard rules, 34 license resources), onnxruntime-android 1.32.0 (minSdk 24), tesseract-android 5.5.0 (minSdk 24, eng.traineddata), tesseract4android 4.9.0 (JitPack; minSdk 21; 12,848,454 B; SHA-256 recorded), pdfbox-android 2.0.27.0 (minSdk 14, assets), media3-transformer 1.11.1 (minSdk 23, `minCompileSdk=36`), androidx.pdf pdf-viewer 1.0.0-beta01 (minSdk 28, `minCompileSdk=36`, extension 19, minAGP 8.9.1), heifwriter 1.1.0 (minSdk 28, minCompileSdk 34), exifinterface 1.4.2 (minSdk 21, 34), documentfile 1.1.0 (minSdk 21, 34), coil-compose 2.7.0 (minSdk 21), android-gif-drawable 1.2.32 (minSdk 21), material-icons-extended-android 1.7.6 (AAR 35,721,003 B), compose ui-android 1.7.6 (AAR 5,031,134 B), material3-android 1.3.1 (AAR 4,965,333 B).

**Licenses**: POM `<licenses>` for every Maven Central/Google Maven artifact above; upstream LICENSE files for PDFium (BSD-3-style), MuPDF (AGPL-3.0), Ghostscript (AGPL-3.0), iText (AGPL-3.0/commercial), x264/x265 (GPL-2.0), Pandoc (GPL + exceptions), calibre (GPL-3.0), ExifTool (Perl Artistic), LibreOffice (MPL-2.0 official page); ffmpeg-kit `LICENSE` (LGPL-3.0) + `THIRD-PARTY-NOTICES.txt`; tessdata_best (Apache-2.0).

**Definitions of "verified"**: version string from live metadata; license from POM and/or upstream license file; minSdk from the AAR manifest; sizes are bytes measured after download; 16 KB result is a parsed ELF `p_align`; feature statements are quoted from vendor/Android docs. Anything else is marked UNVERIFIED.

---

## 7. UNVERIFIED list (deliberately not guessed)

1. `openh264` encoder presence in `ffmpeg-kit-full:8.1.9` (the AAR ships its license file — strong signal, but probe `-encoders`).
2. Exact itemized list of FFmpeg muxers/demuxers/filters/encoders in this build — probe `-formats` / `-codecs` / `-filters`.
3. GIF **encoding** availability via FFmpeg in this build — probe.
4. ML Kit bundled model APK size (docs do not state a number).
5. `androidx.pdf:pdf-viewer` actual rendering engine/behavior (beta; only AAR metadata inspected).
6. Media3/AndroidX 2026 versions (BOM 2026.09.00, material3 1.4.0, activity 1.13.0, navigation 2.10.2, room 2.8.5, work 2.12.0, datastore 1.3.0, coroutines 1.11.0, kotlinx-serialization 1.13.0) **with Kotlin 2.0.21** — compatibility untested; stick to the proven set.
7. Per-library license texts of FFmpeg's bundled external libs (lame/opus/vorbis/vpx/dav1d/kvazaar/webp/theora/opencore-amr/shine/soxr/zimg/libass/freetype/fontconfig/fribidi/snappy) — from general upstream knowledge, not individually re-fetched; audit before store submission.
8. `io.github.rhariskumar3:pdfium:1.0.5` quality/maintenance — do not adopt without source review.
9. ONNX model licenses (models are separate from the MIT runtime).
10. Whether `aapt2`/AGP 8.10 emits warnings for this artifact set under R8 full mode — build once with `isMinifyEnabled=true` and check.
11. `androidx.pdf` build behavior on this machine (SDK extension 19 not installed; only platforms 35/36 present) — not attempted.
12. Exact final APK/AAB byte sizes — measure with a real build (estimates in §3/§8.5).
13. `onnxruntime-android-qnn` contents/ABI support.
14. Exact Play policy wording on downloading executable code (taken from model-auditor's memo, not re-fetched here).

---

## 8. Lead follow-up (2026-11-16 23:40–23:59): exact coordinates, minCompileSdk, targetSdk 36 changes, packaging, ONNX decision

This section answers the Lead's three follow-ups using the AAR itself as evidence. Project build files were read-only inspected at 23:5x; **this teammate changed no code or build files**.

### 8.1 (a) FFmpegKit coordinates and API package — CONFIRMED from the shipped AAR

**Exact Maven coordinates** (live metadata 2026-11-16; group path `dev/ffmpegkit-maintained/`):

| Coordinate | Latest | Packaging | Notes |
|---|---|---|---|
| `dev.ffmpegkit-maintained:ffmpeg-kit-full:8.1.9` | 8.1.9 | aar | the one the project uses; audio+video+https libs, no GPL |
| `dev.ffmpegkit-maintained:ffmpeg-kit-audio:8.1.9` | 8.1.9 | aar | LGPL encoders only |
| `dev.ffmpegkit-maintained:ffmpeg-kit-video:8.1.9` | 8.1.9 | aar | video codecs/filters |
| `dev.ffmpegkit-maintained:ffmpeg-kit-https:8.1.9` | 8.1.9 | aar | gmp+gnutls, unneeded for offline |
| `dev.ffmpegkit-maintained:ffmpeg-kit-min:8.1.9` | 8.1.9 | aar | bare FFmpeg |
| `dev.ffmpegkit-maintained:ffmpeg-kit-*-gpl:8.1.9` | 8.1.9 | aar | **GPL-3.0, do not use** |

POM description (fetched from `repo1.maven.org/.../ffmpeg-kit-full-8.1.9.pom`, 2 lines):
> Drop-in replacement for com.arthenica:ffmpeg-kit-full (Arthenica's fork, retired April 2025) -- same com.arthenica.ffmpegkit API, just change the groupId.

POM `<licenses>` block: 'GNU Lesser General Public License v3.0' (the same block is duplicated twice).

**What the AAR actually ships** (unzipped 2026-11-16, `ffmpeg-kit-full-8.1.9.aar`, 31,057,330 B):
- `AndroidManifest.xml`: `<manifest package="com.arthenica.ffmpegkit">` and `<uses-sdk android:minSdkVersion="24" />` — **the API package is `com.arthenica.ffmpegkit`, unchanged**. Only the Maven group changed, so existing FFmpegKit sample code compiles after a groupId swap.
- `classes.jar` (69,961 B) contains **40 classes**, all under `com/arthenica/ffmpegkit/`: `FFmpegKit`, `FFmpegKitConfig`, `FFprobeKit`, `AbstractSession`, `FFmpegSession`, `FFprobeSession`, `MediaInformationSession`, `Session`, `ReturnCode`, `Log`, `Level`, `Statistics`, `MediaInformation`, `MediaInformationJsonParser`, `StreamInformation`, `Chapter`, `Abi`, `AbiDetect`, `Packages`, `Signal`, `SessionState`, `CameraSupport`, `WhisperKit`, `NativeLoader`, `LogCallback`, `StatisticsCallback`, `FFmpegSessionCompleteCallback`, `FFprobeSessionCompleteCallback`, `MediaInformationSessionCompleteCallback`, `AsyncFFmpegExecuteTask`, `AsyncFFprobeExecuteTask`, `AsyncGetMediaInformationTask`, `FFmpegKitConfig$SAFProtocolUrl`, `LogRedirectionStrategy`, `TranslationProvider`, `DeepLTranslationProvider`, `GoogleTranslateProvider`, `LibreTranslateProvider`, `FFmpegKitConfig$1`, `FFmpegKitConfig$2`.
  - API names the app will call (`FFmpegKit.execute`, `FFmpegKitConfig`, `ReturnCode`, `Statistics`, `AbiDetect`, `FFprobeKit.getMediaInformation`) are present exactly as in the old `com.arthenica:ffmpeg-kit` line.
- `proguard.txt` consumer rules (applied automatically even with `isMinifyEnabled=true`): keeps `com.arthenica.ffmpegkit.FFmpegKitConfig` (native methods, `log`, `statistics`, `safOpen`, `safClose`) and `AbiDetect` (native methods). No manual keep rules needed.
- `META-INF/.../aar-metadata.properties`: `minCompileSdk=1`, `minCompileSdkExtension=0`, `minAndroidGradlePluginVersion=1.0.0` → FFmpegKit imposes **no** compileSdk/AGP floor.
- Transitive dependency: `com.arthenica:smart-exception-java:0.2.1` (POM license **BSD-3-Clause**), which pulls `com.arthenica:smart-exception-common:0.2.1`. Add the BSD-3 notice to the About/licenses list.
- `res/raw/license_*.txt` — 34 bundled license texts (e.g. `license_openh264.txt`, `license_lame.txt`, `license_dav1d.txt`, `license_freetype.txt`, `license_fontconfig.txt`, `license_harfbuzz.txt`, `license_libaom.txt`, `license_libsndfile.txt`, `license_libvpx.txt`, `license_snappy.txt`), plus `res/raw/license.txt` (7,652 B) and `res/raw/source.txt` (700 B). These library resources merge into the app, so an About screen can enumerate them at runtime by resource name — a cheap way to satisfy the LGPL/third-party notice obligation.
- **`res/raw/source.txt` is stale**: it tells the user to download source from `https://github.com/arthenica/ffmpeg-kit/wiki/Source` and to mail `open-source@arthenica.com`. The fork did not update it. **The LGPL source offer must point to `https://github.com/ffmpegkit-maintained/ffmpeg`**, not the retired Arthenica URLs.
- `license_openh264.txt` being shipped is a strong signal that the full build compiles OpenH264 in, but that remains **UNVERIFIED** at runtime (probe `-encoders`).

**Verdict (a): coordinates `dev.ffmpegkit-maintained:ffmpeg-kit-full:8.1.9` and API package `com.arthenica.ffmpegkit` are confirmed from the artifact itself; the project has the correct coordinates and needs no code change.**

### 8.2 (b) minCompileSdk audit — nothing in the dependency set requires > 36

Inspected `META-INF/com/android/build/gradle/aar-metadata.properties` inside every cached AAR (2026-11-16):

| Artifact | Version | minCompileSdk | minCompileSdkExtension | minAndroidGradlePluginVersion | In project? |
|---|---|---|---|---|---|
| `androidx.media3:media3-transformer` | 1.11.1 | **36** | 0 | 1.0.0 | no (not declared) |
| `androidx.pdf:pdf-viewer` | 1.0.0-beta01 | **36** | **19** | **8.9.1** | no (must not add without SDK extension) |
| `androidx.documentfile:documentfile` | 1.1.0 | 34 | 0 | 8.1.1 | project has 1.0.1 (older, fine) |
| `androidx.exifinterface:exifinterface` | 1.4.2 | 34 | 0 | 1.0.0 | project has 1.3.7 (older, fine) |
| `androidx.heifwriter:heifwriter` | 1.1.0 | 34 | 0 | 8.1.1 | no |
| `dev.ffmpegkit-maintained:ffmpeg-kit-*` | 8.1.9 | 1 | 0 | 1.0.0 | **yes** |
| `com.microsoft.onnxruntime:onnxruntime-android` | 1.32.0 | 1 | 0 | 1.0.0 | **yes** |
| `cz.adaptech.tesseract4android` | 4.9.0 | 1 | 0 | 1.0.0 | **yes** |
| `com.tom-roush:pdfbox-android` | 2.0.27.0 | 1 | — | 1.0.0 | **yes** |
| `pl.droidsonroids.gif:android-gif-drawable` | 1.2.32 | 1 | 0 | 1.0.0 | no (planned) |
| `io.coil-kt:coil-compose` | 2.7.0 | 1 | 0 | 1.0.0 | **yes** |
| Compose 1.7.6 / Material3 1.3.1 / activity-compose 1.9.3 / navigation-compose 2.8.5 / work 2.10.0 / datastore 1.1.1 / core-ktx 1.13.1 / lifecycle 2.8.7 | as declared | ≤ 36 | 0 | ≤ 8.0 | **yes** — proven by successful dependency resolution + Kotlin compile under compileSdk 36 in this project (Lead evidence 23:40) |

**Answer:** no dependency requires `minCompileSdk > 36`. The only candidate that pulls 36 is media3-transformer 1.11.1, and it is not in the project's dependency set. `compileSdk = 36` is mandatory anyway for three independent reasons:
1. `targetSdk = 36` cannot exceed `compileSdk`, and Play requires target 36 since 2026-08-31.
2. AGP 8.10.x supports a maximum API level of 36 (release notes quote: 'The maximum API level that Android Gradle Plugin 8.10 supports is API level 36.').
3. Android 16 setup docs require 'Android Gradle plugin 8.9.0-rc01 or higher' to compile against API 36 — the project uses 8.10.1, so fine.

AGP **8.10.1** is a patch of the 8.10 line: same requirements as 8.10.0 (Gradle 8.11.1 min/default, SDK Build Tools 35.0.0, JDK 17, NDK default 27.0.12077973, max API 36). The 8.10.0 release notes carry a 'Fixed Issues in Android Gradle Plugin 8.10.1' section (issue #3906), and `gradle-8.10.1.pom` is present on Google Maven. The sibling project and this project both now declare 8.10.1 — consistent.

`documentfile 1.0.1` / `exifinterface 1.3.7` are older than the newest releases measured (1.1.0 / 1.4.2) but impose no compileSdk 36 floor and resolve fine; no action required.

### 8.3 (b cont.) targetSdk 36 / AGP 8.10 behavior changes that can affect Compose + FFmpegKit + PDFBox

Quotes fetched 2026-11-16 from developer.android.com (targeted-behavior page, FGS types page, 16 KB page page).

| # | Change | Official wording (short quote) | Effect on this app | Required action |
|---|---|---|---|---|
| 1 | **Edge-to-edge enforced, opt-out removed** | 'For apps targeting Android 16 (API level 36), R.attr#windowOptOutEdgeToEdgeEnforcement is deprecated and disabled, and your app can't opt-out of going edge-to-edge.' | Content draws under the status/navigation bars; the donut drag-and-drop canvas can be clipped or sit under the gesture bar. Layout/UX issue, not a crash. | `enableEdgeToEdge()` (activity-compose 1.9.3) + `Scaffold`/`WindowInsets.safeDrawing` padding; test on Android 15 and 16 emulators. |
| 2 | **Predictive back on by default** | 'For apps targeting Android 16 (API level 36) or higher and running on an Android 16 or higher device, the predictive back system animations ... are enabled by default. Additionally, onBackPressed is not called and KeyEvent.KEYCODE_BACK is not dispatched anymore.' | Custom back handling via `Activity.onBackPressed()` / `KEYCODE_BACK` silently stops working. Navigation Compose 2.8.5 routes back through `OnBackPressedDispatcher`, so normal navigation keeps working. | Do not override `onBackPressed`; use Navigation Compose / `BackHandler`; test back from conversion-progress screens. |
| 3 | **Orientation/resizability/aspect-ratio restrictions ignored on large screens** | 'For apps targeting Android 16 (API level 36), orientation, resizability, and aspect ratio restrictions no longer apply on displays with smallest width >= 600dp.' | A portrait-locked donut UI will rotate/resize on tablets, foldables and desktop windowing; layout must be adaptive. | Support landscape/tablet layouts; treat rotation as expected; do not rely on `screenOrientation` on large screens. |
| 4 | **Foreground service type `mediaProcessing`** | 'Service for performing time-consuming operations on media assets, like converting media to different formats. The system allows this service a limited time to run; under normal circumstances, this time limit would be 6 hours out of every 24.' (permission `FOREGROUND_SERVICE_MEDIA_PROCESSING`, constant `FOREGROUND_SERVICE_TYPE_MEDIA_PROCESSING`, runtime prerequisites: None) | Long conversion queues cannot run in a single 6h service window; a batch of large videos can exceed it. | WorkManager queue + resumable/checkpointed jobs; declare the mediaProcessing FGS type and permission so a timeout resumes instead of dying. |
| 5 | **Media/storage permissions** (inherited from target 34/33) | Android 14: 'Android 14 introduces Selected Photos Access, which allows users to grant apps access to specific images and videos in their library ... adapt your implementation to use the new READ_MEDIA_VISUAL_USER_SELECTED permission.' | Broad storage permission requests are awkward on 14+ and Play's all-files-access policy limits `MANAGE_EXTERNAL_STORAGE`. | Prefer SAF (`ACTION_OPEN_DOCUMENT`, `ACTION_OPEN_DOCUMENT_TREE`) + photo picker: **no storage runtime permission needed**. Copy `content://` input to app cache when an engine needs a real path (PDFBox). Android 13+ `READ_MEDIA_*` only if a media-library browse feature is added (exact wording not re-fetched: UNVERIFIED). |
| 6 | **16 KB page sizes (Play gate)** | 'all apps targeting Android 15 (API level 35) and higher must support 16 KB memory page sizes on 64-bit devices on Google Play. Starting February 1, 2027, if your app updates don't support 16 KB memory page sizes, you won't be able to release these updates.' | All shipped `.so` files are verified aligned (see 8.4), but packaging mode can still break the zip alignment. | `useLegacyPackaging = false`; verify with bundletool/zipalign; see checklist 8.4. |
| 7 | **AGP 8.10 native packaging defaults** | AGP 8.10 release notes: Gradle 8.11.1 min/default, Build Tools 35.0.0, JDK 17, NDK default 27.0.12077973. 16 KB docs: 'Upgrade to AGP version 8.5.1 or higher and use uncompressed shared libraries.' | `useLegacyPackaging = true` (current project setting) compresses `.so` and contradicts the 16 KB guidance — the one concrete build-file risk found. | Set `jniLibs { useLegacyPackaging = false }`; keep `abiFilters`/AAB splits; no NDK install needed (prebuilt AARs). |
| 8 | **Compose / FFmpegKit / PDFBox API behavior on API 36** | No Android-16-specific API breaking change was found in the docs for Compose 1.7.6, activity 1.9.3, Navigation 2.8.5, FFmpegKit 8.1.9 or PDFBox 2.0.27.0. | FFmpegKit loads its `.so` normally; PDFBox is pure Java + assets; Compose UI unaffected apart from insets/predictive back above. | Runtime verification: `FFmpegKit.execute("-version")`, one real transcode, one PDF text extraction, on an API 36 emulator. |
| 9 | **AGP 8.10 source-level API change** | 'Source level breaking change: finalizeDsl now requires use of parameterized types.' | Affects Gradle plugins/DSL extensions only — not app code or the listed libraries. | None for this app. |
| 10 | **16 KB compatibility mode** | 'Android 16 adds a compatibility mode, allowing some apps built for 4 KB memory pages to run on a device configured for 16 KB memory pages.' | Not an exemption from the Play requirement. | Treat as a fallback only; still align `.so` correctly. |

**Verdict (b): no dependency of ours requires compileSdk > 36; the only true build-level blocker is `useLegacyPackaging = true`, which should be `false` before any Play submission.**

### 8.4 16 KB / ABI final packaging checklist (for the build-file owner)

1. `android.packaging.jniLibs.useLegacyPackaging = false` (current project sets `true` → change).
2. Build an AAB (`bundleRelease`) and run `bundletool dump config --bundle=<app>.aab | grep alignment`; expect `PAGE_ALIGNMENT_16K` (if `PAGE_ALIGNMENT_4K`, packaging is wrong).
3. Build the release APK and run (build-tools 35.0.0 is installed on this machine): `C:\Android\Sdk\build-tools\35.0.0\zipalign.exe -v -c -P 16 4 app-release.apk` — the last line must read 'Verification successful'.
4. `unzip -v app-release.apk` must show `Stored` (not `Defl:N`) for every `lib/<abi>/*.so`; compressed `.so` means legacy packaging slipped back in.
5. Inspect every shipped `.so` with APK Analyzer (Alignment column) or `llvm-readelf -l` → `p_align = 0x4000`. Already verified for ffmpeg-kit 8.1.9 (10/10), onnxruntime 1.32.0 (2/2), tesseract4android 4.9.0 (4/4) and gif-drawable 1.2.32 (1/1).
6. Keep `abiFilters` arm64-v8a + x86_64 (project already has this) or rely on AAB ABI splits for Play; **never ship a universal APK** with these engines.
7. Add a Play internal-test run on a 16 KB page device/emulator image; watch for native `dlopen` failures (would indicate a misaligned third-party `.so`).
8. Keep the LGPL notices screen (FFmpegKit + FFmpeg + smart-exception BSD-3 + bundled external libs) reachable from About.

### 8.5 (c) APK size estimate (arm64-v8a) and ONNX decision

Measured additions (raw arm64 `.so` bytes; AAR = compressed download size):

| Component | arm64 raw `.so` | Notes |
|---|---|---|
| ffmpeg-kit-full 8.1.9 | 31,527,400 B (30.06 MiB) | AAR 31,057,330 B (2 ABIs) |
| onnxruntime-android 1.32.0 | 36,429,976 B (34.74 MiB) | AAR 59,335,382 B (4 ABIs) |
| tesseract4android 4.9.0 | 7,514,368 B (7.17 MiB) | no model bundled |
| pdfbox-android 2.0.27.0 | pure Java, AAR 3,254,019 B | assets included |
| commons-compress 1.28.0 + xz 1.12 | JAR 1,117,221 + 168,792 B | code only |
| coil-compose 2.7.0 | AAR 17,808 + base 428,279 + gif 42,918 B | code |
| material-icons-extended 1.7.6 | **AAR 35,721,003 B, classes.jar 37,425,981 B** | debug dex includes all icon classes (no shrinking) |
| material-icons-core 1.7.6 | AAR 823,515 B, classes.jar 869,838 B | use instead of extended where possible |

Estimates for **debug** builds (`.so` stored uncompressed; no R8 shrinking; AARs measured, final APK is an estimate — verify with a real build):

| Configuration | Estimated APK size |
|---|---|
| Current dependency set, **both ABIs** (project abiFilters arm64+x86_64): ffmpeg 2 ABIs (31.53+38.20 MB) + ONNX 2 ABIs (36.43+45.08 MB) + tesseract 2 ABIs (~15.3 MB) + icons-extended dex (~20–25 MB) + baseline ~11 MB + pdfbox/coil/misc ~5 MB | **≈ 200–215 MB** |
| Current set, **arm64 only** (use `ndk.abiFilters = listOf("arm64-v8a")` for local debug or ABI splits) | **≈ 115–130 MB** |
| **v1 recommendation**: drop ONNX, replace icons-extended with icons-core, keep ffmpeg full + pdfbox + tesseract, release R8 on | **≈ 55–62 MB debug / ≈ 45–52 MB release arm64**; Play per-device download roughly 30–40 MB |
| Same without tesseract (OCR deferred) | ≈ 48–55 MB debug arm64 |

**ONNX Runtime 'per-ABI' option:** there is **no per-ABI Maven artifact**. Maven Central directory `com/microsoft/onnxruntime/` lists only: `onnxruntime`, `onnxruntime-android`, `onnxruntime-android-qnn`, `onnxruntime-extensions`, `onnxruntime-extensions-android`, `onnxruntime-mobile`, `onnxruntime-training-android`, `onnxruntime_gpu`. ABI splitting is already the correct mechanism (AGP strips other ABIs; AAB delivers one ABI per device), so the 36.4 MB arm64 cost is unavoidable while the dependency stays.
- `onnxruntime-android-qnn` is 1.29.0 (older than 1.32.0) and a 9,419,194 B AAR — likely a Qualcomm QNN-targeted package; its ABI/purpose is **UNVERIFIED** and it is not a drop-in replacement for devices without QNN.

**Recommendation: drop `onnxruntime-android` for v1.** No model has been selected for the MVP; it adds 34.74 MiB per ABI (~60% of the whole app), and it cannot be moved to a later download because Play policy forbids downloading executable code (`.so`/`.dex`) at runtime — the native runtime must ship with the app. If an AI/OCR-model feature is chosen later, put it in a Play **feature module** (or a separate flavor) so the base install stays lean. If the team keeps it for experiments, at minimum restrict debug builds to arm64 and enable R8 for release.

### 8.6 JitPack / Tesseract4Android mitigation (per model-auditor + Lead)

- The project build **resolved and compiled** `cz.adaptech.tesseract4android:tesseract4android:4.9.0` from JitPack (Lead evidence).
- model-auditor saw the JitPack AAR URL return HTTP 500 on 2026-11-16; my direct download succeeded the same day: **12,848,454 B, SHA-256 BCE5D6413A1A5AE3D7240033FBBC851BA3217D0A08D9769400E17A077F42CB2A**. The artifact is fetchable, but JitPack availability is flaky — a risk, not a blocker.
- Mitigation (recommended): (1) keep the successful Gradle cache and enable dependency verification/locking so the exact bytes are pinned; (2) vendor the AAR into `app/libs/` as a fallback (`implementation(files('libs/tesseract4android-4.9.0.aar'))`) with the SHA-256 above, or use a JitPack mirror; (3) scope the JitPack repository to only this group (`content { includeGroup('cz.adaptech.tesseract4android') }`) — the project currently adds JitPack unconditionally, broadening the supply-chain surface; (4) the AAR bundles no license file, so add the Apache-2.0 text plus upstream Tesseract/Leptonica notices (Leptonica BSD-2-Clause per model-auditor's `docs/research/model-licenses.md`) to the licenses screen.
- Tesseract4Android AAR metadata: `minCompileSdk=1`, minSdk 21, contains `classes.jar` + `annotations.zip` only (no assets/models). Models stay a runtime download (data-only) per the model-licenses memo.

### 8.7 Project-state deltas seen while answering (read-only; flags for whoever owns build files)

Read `settings.gradle.kts`, `build.gradle.kts`, `app/build.gradle.kts`, `gradle.properties` at 23:5x:
- ✓ `AGP 8.10.1`, Kotlin 2.0.21, compose plugin 2.0.21, serialization plugin 2.0.21; compileSdk 36, targetSdk 36, minSdk 26, JVM 17; `gradle.properties` mirrors the sibling (Xmx4096m, parallel, caching, AndroidX, nonTransitiveRClass).
- ⚠ `android.packaging.jniLibs.useLegacyPackaging = true` → **should be `false`** (16 KB + uncompressed `.so` guidance; see 8.3/8.4).
- ⚠ `material-icons-extended` is declared; its AAR is 35.7 MB / classes.jar 37.4 MB and debug builds include all of it (no shrinking). Replace with `material-icons-core` + explicit icons, or keep only in release-with-R8 builds.
- ⚠ `maven(url = "https://jitpack.io")` is unscoped; scope it to `cz.adaptech.tesseract4android`.
- ℹ `documentfile:1.0.1` and `exifinterface:1.3.7` are older than latest (1.1.0 / 1.4.2) but resolve and impose no new floor — optional refresh later.
- ℹ No Room/KSP, no media3, no heifwriter, no zip4j/junrar: archives are commons-compress + xz only, so **AES-encrypted ZIP and RAR extraction are not available in this dependency set** (coverage gap if the format matrix needs them; zip4j 2.11.6 and junrar 8.1.2 are verified Apache-2.0 / UnRAR-read options).
- ℹ No `material-icons-core` declared; Compose foundation/material-icons not declared besides extended (material3 pulls what it needs).

### 8.8 Verification status of section 8

**Verified from artifacts:** FFmpegKit POM/manifest/classes/proguard/license assets/aar-metadata; AGP 8.10.1 POM presence and 8.10.0 requirements page (Gradle 8.11.1, Build Tools 35.0.0, JDK 17, max API 36); all `minCompileSdk` values in 8.2 from `aar-metadata.properties` except the Compose/Room/Work/DataStore/Nav rows (proven by the project's successful resolution + compile under compileSdk 36, not individually re-unzipped); the Android 16 / Android 15 / Android 14 / FGS / 16 KB quotes from developer.android.com (fetched 2026-11-16); project files as listed; Tesseract4Android AAR bytes+hash; ONNX Maven directory listing and QNN AAR size (latest 1.29.0, 9,419,194 B).
**UNVERIFIED (marked as such above):** OpenH264 compiled into `ffmpeg-kit-full` (probe `-encoders`); exact final APK/AAB sizes (measure with a build); `onnxruntime-android-qnn` content/ABI support; exact Play policy wording for runtime code downloads (taken from model-auditor's memo, not re-fetched here); Android 13 `READ_MEDIA_*` wording; navigation-compose predictive-back support levels; whether `material-icons-extended` is actually referenced by app code today (build-file presence only).

---

*Generated by teammate `android-stack` for task-3. This file is the only file this teammate wrote; all other paths were read-only inspection or temporary downloads outside the repo (`%TEMP%`).*
