# GitHub publishing checklist (manual repository settings)

These settings live in the github.com web interface. They are not part of the source tree, most have
no REST endpoint, and they are what turns a pushed repository into an understandable public project.
Work top to bottom as the repository owner (`@ahmadmhmdsy`).

Menu paths differ slightly between GitHub UI versions; where a path recently changed, the older
location is given in parentheses.

## 1. Confirm the About panel

Repository home → **About** (right sidebar) → pencil icon, or **Settings → General**.

- [ ] **Description** present. Current:
      "Sigha (صيغة) - offline, privacy-first file converter for Android: 48 formats, 32 converters,
      everything converted on device. No account, no ads, no analytics. Kotlin + Jetpack Compose,
      English + Arabic (RTL)."
- [ ] **Website** set to <https://ahmadmhmdsy.github.io/sigha/> (it will only load once Pages is
      deployed — see §5).
- [ ] **Topics** present and accurate. Twenty are set today: `android`, `android-app`, `arabic`,
      `audio-conversion`, `document-conversion`, `ffmpeg`, `file-converter`,
      `image-conversion`, `jetpack-compose`, `kotlin`, `local-first`, `material-design`,
      `no-ads`, `ocr`, `offline`, `pdf`, `privacy`, `rtl`, `tesseract`,
      `video-conversion`.

## 2. Social preview image

**Settings → General → Social preview → Upload an image** (there is no REST API for this).

- [ ] Render `site/assets/social-card.html` to a PNG at **1280 × 640 px** (GitHub's recommended and
      maximum preview size; that page is authored at exactly 1280 × 640 — open it in a browser and
      capture it, or use the export if the website team ships one) and upload that PNG. If the site
      team later adds `site/assets/social-preview.png`, upload that file instead.
- [ ] Open the repository link in a chat/social app once to confirm the card renders (cache may
      lag; GitHub does not re-fetch immediately).

## 3. Features

**Settings → General → Features**.

- [ ] **Issues** on (the issue templates in `.github/ISSUE_TEMPLATE/` require it).
- [ ] **Discussions**: currently **off**. Either keep questions on Issues, or turn Discussions on
      and update `CONTRIBUTING.md`/`SECURITY.md`, which currently say Discussions are disabled.
- [ ] **Wiki** off (the docs tree is the wiki).
- [ ] **Projects** off unless a public roadmap board is wanted.

## 4. Licence and community files

- [ ] `LICENSE` (Apache-2.0) pushed. The repository home sidebar should then show
      "Apache-2.0 license". GitHub's licence detection can take a minute after the first push.
- [ ] **Insights → Community Standards** ("Community profile") now lists: README, LICENSE,
      CONTRIBUTING, CODE_OF_CONDUCT, issue templates and the pull-request template. Before this
      round the profile was incomplete (no licence, no contribution docs, no templates); the target
      is a complete profile.
- [ ] **Security → Policy** shows `SECURITY.md` (the file must be in the repository root or
      `.github/`; this repo keeps it at the root).

## 5. GitHub Pages (the website)

The site is deployed by `.github/workflows/pages.yml` from the `site/` directory. At the time of
writing, the Pages API returned 404 and <https://ahmadmhmdsy.github.io/sigha/> returned 404, so
Pages is not enabled yet.

- [ ] **Settings → Pages → Build and deployment → Source: GitHub Actions** (older UI:
      **Settings → Pages → Source → GitHub Actions**).
- [ ] `.github/workflows/pages.yml` exists on `main` and its run completed successfully
      (**Actions** tab → *Pages* workflow → green).
- [ ] <https://ahmadmhmdsy.github.io/sigha/> returns 200 and matches the About-panel Website field.
- [ ] Optional custom domain later: Settings → Pages → Custom domain. Do **not** point the
      `sigha.app` domain at this project — it belongs to another product (see
      `docs/legal/name-screen-2026-11-19.md`).

## 6. Security settings

**Settings → Code security** (older UI: **Security** tab → **Advisories** → **Settings**).

- [ ] **Private vulnerability reporting** enabled. Then `SECURITY.md` links and the
      **Security → Advisories → Report a vulnerability** button work; reports arrive privately.
      This setting is what `SECURITY.md` and `.github/ISSUE_TEMPLATE/config.yml` point at.
- [ ] **Dependabot alerts** enabled (they are a repository setting; `.github/dependabot.yml` only
      opens version-update pull requests, it does not enable alerts).
- [ ] **Dependabot security updates** enabled.
- [ ] **Secret scanning** / **push protection** enabled where GitHub offers them for public repos.
- [ ] Confirm all three workflows (`Android CI`, `Pages`, and any later release workflow) run and
      that Actions is not restricted: **Settings → Actions → General**.

## 7. Branch protection (recommended)

**Settings → Branches → Branch protection rules** → rule for `main`.

- [ ] Require the **Android CI** status check before merging.
- [ ] Optionally require pull requests and a linear history once there is more than one contributor.
      A solo-owner repository can keep this light, but the CI check should not be bypassed.

## 8. Publish the v0.2.1 release

The release body is ready at [release-notes-v0.2.1.md](release-notes-v0.2.1.md).

- [ ] Push the commit with this round's files; wait for **Android CI** to pass on `main`.
- [ ] Create an annotated tag `v0.2.1` on the tested commit and push it (or let GitHub create the
      tag when drafting the release).
- [ ] **Releases → Draft a new release**: tag `v0.2.1`, title `Sigha 0.2.1 — صيغة`, body pasted
      from `docs/release-notes-v0.2.1.md`.
- [ ] Attach `dist/Sigha-0.2.1-universal-debug.apk`, `dist/Sigha-0.2.1-arm64-debug.apk` and
      `dist/SHA256SUMS.txt`. Verify the sizes and hashes in the notes against the files one last
      time.
- [ ] Tick **Set as a pre-release** — these are debug-key QA builds, not a store release, and the
      release notes say so in the first paragraph.
- [ ] Publish, then open the release page and check every link in the notes resolves.

## 9. Release process for the next version

For each future version (example: 0.2.2):

1. **Bump the version** in `app/build.gradle.kts`: `versionCode` +1 and `versionName`.
2. **Write the changelog entry** in `CHANGELOG.md` under a new `## X.Y.Z — <date> — <theme>`
   heading; keep past sections untouched.
3. **Run the gate** on the exact tree to be released: `pwsh -File scripts/verify.ps1` (and
   `-ReleaseCandidate` once the publisher's legal values exist). Commit the new evidence file in
   `docs/qa/evidence/`.
4. **Run the runtime checks** for user-visible changes: `scripts/device-smoke.ps1`,
   `scripts/feature-smoke.ps1`, `scripts/retention-smoke.ps1`; commit their evidence.
5. **Write the release-notes file** `docs/release-notes-vX.Y.Z.md` from the new evidence JSON.
6. **Commit, push to `main`, wait for Android CI** to pass.
7. **Tag and release:** annotated tag `vX.Y.Z` on that commit; draft release with the notes file as
   the body; attach the APKs and `SHA256SUMS.txt`; mark pre-release while the build is still a
   debug-key QA build.
8. **Verify the published release:** open each link, download one APK and check its SHA-256 against
   the notes, and confirm the release is visible on the Releases page.

Never move or re-create a published tag; if something is wrong, release a new patch version.

## 10. State snapshot (2026-11-23)

Recorded from the public repository API before this documentation round, so the checklist above can
be read in context:

| Setting | State |
|---|---|
| Visibility | public |
| Default branch | `main` |
| Description | set (quoted in §1) |
| Website | set — the URL returns 404 until Pages is deployed |
| Topics | 20 set |
| Issues | enabled |
| Discussions | disabled |
| Wiki | disabled |
| Licence detected | none (no `LICENSE` file before this round) |
| Community profile | incomplete — no licence, contributing, code of conduct or issue templates |
| GitHub Pages | not enabled (API 404) |

This round adds `LICENSE`, `CONTRIBUTING.md`, `CODE_OF_CONDUCT.md`, `SECURITY.md`, the
`.github/` templates and CI workflow, so items in §4 should complete after the next push; §2, §5,
§6 and §7 are still manual steps for the owner.
