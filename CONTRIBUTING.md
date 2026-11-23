# Contributing to Sigha

Thanks for wanting to improve Sigha (صيغة). Sigha is an offline, privacy-first file converter for
Android, written in Kotlin and Jetpack Compose (Material 3). This document explains how to build it,
how the project decides whether a change is done, and which changes are out of scope.

By participating you agree to the [Code of Conduct](CODE_OF_CONDUCT.md). For security issues, do
**not** open a public issue — follow [SECURITY.md](SECURITY.md).

The project is currently a **pre-release QA build** (0.2.1, versionCode 3): the APKs on the Releases
page are signed with the Android debug key and the shipped legal text still contains 21 placeholders,
so nothing here is a Play Store release yet. Read
[`docs/legal/release-blockers.md`](docs/legal/release-blockers.md) before proposing work that
assumes a store launch.

## The evidence rule

No public claim in this repository is accepted without evidence. Numbers in the README, the docs and
the release notes are copied from the machine-readable JSON written by the scripts in `scripts/`,
never from memory:

- `docs/qa/evidence/verify-<stamp>.json` — the build, test, lint and packaging gate
  (`scripts/verify.ps1`).
- `docs/qa/evidence/features-<stamp>/features.json` — the end-to-end feature smoke run.
- `docs/qa/evidence/device-smoke-<stamp>.json` and `retention-<stamp>/` — cold-start/crash and
  repeat-use checks.

If your change alters a measured fact (test count, APK size or hash, lint counts, format counts), the
pull request must include a fresh gate run and the docs must be updated from the new evidence file.
Do not invent or round numbers, and do not quote results from someone else's build.

## Build it

### Requirements

| Tool | Version used by this project |
|---|---|
| JDK | 21 (Eclipse Temurin 21.0.13 used here; the build targets JVM 17) |
| Android SDK platform | 36 (`compileSdk`/`targetSdk` 36), build-tools 36.0.0 |
| Gradle | 8.11.1 — always through the wrapper; do not bump it casually |
| Android Gradle Plugin | 8.10.1 |
| Kotlin | 2.0.21 |
| PowerShell | 7+ (`pwsh`) for the verification scripts |
| minSdk | 26 (Android 8.0) |

`local.properties` points Gradle at your SDK (`sdk.dir=...`); it is git-ignored. Release signing
credentials are never committed: `keystore/keystore.properties` is local-only, and the repository
ships no key material.

### Commands

```powershell
# JVM unit tests (this is what GitHub Actions runs)
.\gradlew.bat :app:testDebugUnitTest --no-daemon --console=plain

# Debug APK
.\gradlew.bat :app:assembleDebug --no-daemon --console=plain

# One test class
.\gradlew.bat :app:testDebugUnitTest --tests "com.sigha.converter.core.FormatRegistryTest" --no-daemon --console=plain
```

On Linux and macOS use `./gradlew` instead of `.\gradlew.bat`.

The full gate is a PowerShell 7 script; it stops stale daemons, runs a clean build, the JVM tests,
the debug APK facts (size, SHA-256, ABI splits, stored native libraries, 16 KB ELF alignment,
zipalign), the release APKs and AAB, lint, EN↔AR resource parity and the stale-claim drift check:

```powershell
pwsh -File scripts/verify.ps1                   # full gate
pwsh -File scripts/verify.ps1 -Quick            # debug build + artefact facts only
pwsh -File scripts/verify.ps1 -SkipBuild        # facts about existing artefacts only
pwsh -File scripts/verify.ps1 -ReleaseCandidate # + fail on placeholders in shipped legal text
```

`-ReleaseCandidate` is **expected to fail** while the 21 legal placeholders are unresolved. Do not
delete placeholder markers to make it pass — that would ship a source offer naming no source.

Runtime checks need an attached emulator or device. Both smoke scripts are destructive (they clear
the app's data) and write their evidence under `docs/qa/evidence/`:

```powershell
pwsh -File scripts/device-smoke.ps1 -Serial emulator-5554
pwsh -File scripts/feature-smoke.ps1 -Serial emulator-5554
pwsh -File scripts/retention-smoke.ps1 -Serial emulator-5554
```

## Add a conversion format

Sigha routes conversions through a graph: a format is a node in `core/FormatRegistry.kt`, and each
converter is one directed edge that declares the formats it accepts and produces. Adding support is
mostly adding edges, not editing a router.

1. **Register the format** (only if the app does not know it yet) in
   `app/src/main/java/com/sigha/converter/core/FormatRegistry.kt`: add an
   `f("id", "extension", "mime", "Label", FormatCategory.X)` entry to `DATA`. Ids and extensions
   must be unique and lowercase — the catalogue tests enforce this. A format with no converter may
   exist (that is how the app can explain why it cannot help), but it must never be advertised as
   supported.
2. **Implement a converter** in the matching family package under
   `app/src/main/java/com/sigha/converter/engine/<family>/`, as a `private class` implementing
   `Converter`:

   ```kotlin
   private class TxtToPdf : Converter {
       override val id: String = "text-txt-to-pdf"
       override val inputs: Set<FileFormat> = setOf(EngineFormats.of("txt"))
       override val outputs: Set<FileFormat> = setOf(EngineFormats.of("pdf"))

       override suspend fun convert(request: ConversionRequest): ConversionResult =
           withContext(Dispatchers.IO) {
               request.onProgress(0f)
               // read request.input, write request.outputFile, check request.isCancelled()
               ConversionResult(listOf(request.outputFile), note = null)
           }
   }
   ```

   Rules for an engine:
   - Write the finished file to `request.outputFile` and report progress through
     `request.onProgress`; honour `request.isCancelled()`.
   - Never put user-facing English text in results. Report what happened with a typed `core.Note`
     (the UI localises it), and throw `ConversionException(message, cause, kind)` with a
     `ConversionFailureKind` for failures. `message` is an English diagnostic shown only as
     expandable technical details.
   - A converter that can produce several files must declare `producesMultipleOutputs = true`; the
     planner only allows such an edge as the last hop. A converter that needs an optional model pack
     sets `requiresModelPack = true`.
   - Keep one converter to one edge. Do not orchestrate routes and do not add hard-coded fallbacks
     that bypass the planner.
3. **Return it from the family list** — for example `textConverters(app)` in
   `engine/text/TextConverters.kt`. A new family also needs a `family("name") { ... }` line in
   `engine/BuiltInConverters.kt`; registration is wrapped so a broken family cannot crash app start.
4. **Add JVM tests** under `app/src/test/java/com/sigha/converter/...` (JUnit 4) for the logic you
   wrote: routing, argument building, parsers, guards. The JVM suite cannot exercise Android, SAF,
   MediaStore, FFmpegKit or Tesseract, so add the user-visible path to the smoke scripts as well.
5. **Update the docs that state facts**: `CHANGELOG.md`, the capability matrix in
   `docs/product/PRODUCT.md`, and — after running the gate — `docs/qa/verification.md` with the new
   measured numbers taken from the evidence JSON. Never change a count without the evidence file.
6. **Run the gate** and include the evidence file in the pull request.

## Code style and conventions

- Kotlin official style (`kotlin.code.style=official`), 4-space indentation, explicit imports,
  small single-purpose files. Follow the surrounding file when in doubt.
- The UI is Jetpack Compose + Material 3. Use the existing theme roles and shapes
  (`ui/theme/`), not ad-hoc colours or radii, and check light/dark, portrait/landscape and RTL.
- Every user-visible string lives in `app/src/main/res/values/strings.xml` and its Arabic twin
  `values-ar/strings.xml`. The gate checks EN↔AR key and placeholder parity and all six Arabic
  plural categories. Never concatenate sentence fragments in Kotlin.
- Comments explain *why*, not *what*. Do not leave a TODO that claims behaviour the build does not
  have.
- Respect the existing resource bounds (PDF page caps, JSON nesting depth, pack size and hash
  checks). New parsing code must bound its input and fail with a typed error.
- No new dependency without a licence review: record it in `NOTICE.md` and
  `docs/legal/licence-compliance.md`. GPL/AGPL and other copyleft engines are rejected (see below);
  the LGPL-3.0 FFmpegKit exception and its source-offer duty are documented there.

## Commits and pull requests

- Branch from `main`; one logical change per pull request; rebase or merge `main` before opening it.
- Commit subjects are short and imperative ("Fix silent-video audio targets"); the body explains
  **why** and cites the evidence JSON path.
- Fill in `.github/PULL_REQUEST_TEMPLATE.md`: summary, scope, evidence, the commands you ran, and
  the checklist.
- Update `CHANGELOG.md` with a bullet under the current unreleased version heading. Do not rewrite
  history in past sections.
- Never commit: keystores, `local.properties`, `.gradle/`, `app/build/`, tokens, personal data, or
  device logs that contain file names.
- GitHub Actions runs the unit tests and the debug build on `ubuntu-latest`; it does not run the
  Windows PowerShell gate, so the evidence attached to the pull request is what reviewers trust.

## What will be rejected

Out of scope by design — a pull request containing these will be closed:

- **Telemetry, analytics, crash reporting or attribution SDKs**, and any network call that is not the
  user-initiated OCR pack download.
- **Ads, accounts, cloud upload/sync, subscriptions or tracking identifiers.** The privacy policy and
  the product promise are the contract.
- **GPL/AGPL engines** (LibreOffice, Pandoc, Ghostscript, calibre, unRAR writer, and the like) or any
  dependency whose licence has not been reviewed into `NOTICE.md`.
- **Unevidenced claims**: invented benchmarks, ratings, user or install counts, claims that every
  format works, or absolute offline claims that ignore the OCR pack download. Numbers without an
  evidence file do not go in.
- **Debug-key or throwaway-keystore artefacts presented as release or Play builds.** The Play
  upload-key path belongs to the publisher; see `docs/plan/publisher-guide.md`.
- **Passing the release gate by deleting placeholder markers** instead of supplying the real values.
- **Loosening privacy or security defaults** (broad storage permissions, cloud backup, exported
  components, disabled signature/permission checks) without a documented threat-model discussion.
- **Format claims for the nine recognised formats with no converter** — TIFF, RTF, TAR, BZ2, XZ,
  YAML, XML, MOBI and FB2 — or UI that offers unreachable targets.

## Questions

Open an issue with the templates in `.github/ISSUE_TEMPLATE/`; bug reports and feature requests
include a required privacy checklist. For anything security-related use [SECURITY.md](SECURITY.md).
GitHub Discussions are currently disabled on the repository.
