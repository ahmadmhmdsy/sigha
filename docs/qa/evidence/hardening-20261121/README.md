# Production-hardening evidence — 2026-11-21

All checks on `app/build/outputs/apk/debug/app-x86_64-debug.apk`
(SHA-256 `5D9EBC0F908E7A3A7AE14A766FE4662854BB3858C7B8C1B52D7982C03DC9BFFF`,
`versionCode 3` / `versionName 0.2.1`, debug key) on an API 36 x86_64 emulator.

| File | What it shows |
|---|---|
| `silent-video.png` | Silent video selected: audio-target hint visible, no audio chips, GIF still offered; the conversion finished (`Download/Sigha/clip-sage.gif`). |
| `audio-video.png` | Same flow for a video with audio: no hint, all seven audio chips, MP3 output. |
| `batch.png` | Two files (PNG + TXT) selected, "Convert 2 files to PDF" succeeded, both PDFs in `Download/Sigha/`. |
| `workbench-share-ui.xml` | UI dump of a finished workbench row: `content-desc="Share"`, `content-desc="Open file"` and the saved-path line. |

Related machine-readable evidence outside this folder:

- `docs/qa/evidence/verify-20261121-234013.json` — full `scripts/verify.ps1` run on the final tree.
- `docs/qa/evidence/device-smoke-20261122-000244.json` — install, cold start, 120 monkey events, no crash.

Not covered here: arm64/physical devices, release-artefact reinstall, archive/OCR/full conversion
matrix, API 26 legacy path. See `docs/qa/verification.md` §11.4.
