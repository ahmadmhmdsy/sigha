# Sigha documentation

This is the index of the documentation tree. The root [README](../README.md) is the project
overview; the product promise is in [PRODUCT.md](../PRODUCT.md). Build, test and format numbers in
these docs are taken from the machine-readable evidence in [qa/evidence/](qa/evidence/) and
`scripts/verify.ps1` — not from memory.

## Product

- [Product definition](../PRODUCT.md) — the one-page product definition, the wedge and the non-goals.
- [Capability specification](product/PRODUCT.md) — format-by-format truth: what is recognised,
  convertible, producible, and what is deliberately missing.
- [Store listing draft](product/store-listing.md) — Play Console copy and screenshot plan (draft).
- [ASO plan 2026-11-19](product/aso-plan-2026-11-19.md) — discovery strategy, titles, keywords.
- [Permissions rationale](product/permissions-rationale.md) — every permission, why it exists, what
  happens when it is denied.
- [Play declarations 2026-11-22](product/play-declarations-2026-11-22.md) — paste-ready Data safety,
  content-rating and foreground-service answers (builder draft, nothing submitted).

## Legal and licences

- [Privacy policy](legal/privacy-policy.md) — the policy text (repository copy; the app renders the
  matching asset).
- [Licence compliance record](legal/licence-compliance.md) — what is shipped, which licence duties
  are satisfied, and what is unresolved.
- [Release blockers](legal/release-blockers.md) — the human-only items (B1–B7) that block Play.
- [Clearance memo](legal/clearance-memo.md) — the Filee-inspired idea clearance record.
- [Name screen 2026-11-19](legal/name-screen-2026-11-19.md) — public-source name search.
- [Third-party notices](../NOTICE.md) — the notices shipped byte-identical inside the app.
- [Legal sources](legal/sources/) — provenance copies of licence texts and policy snapshots
  (read-only inputs, not product docs).

## Quality and evidence

- [Verification record](qa/verification.md) — what was built, tested and observed, including what
  was not.
- [Device checklist](qa/device-checklist.md) — the physical-device QA checklist that closes the
  runtime gap.
- [Tester instrument](qa/tester-instrument.md) — the closed-test script for testers.
- [Evidence folder](qa/evidence/) — machine-readable gate, device-smoke, feature-smoke and retention
  runs, plus screenshots.

## Architecture decisions

- [ADR-0001: media engine](adr/0001-media-engine.md) — FFmpegKit fork vs Media3-only.
- [ADR-0002: execution model](adr/0002-execution-model.md) — foreground service over WorkManager.
- [ADR-0003: product wedge](adr/0003-product-wedge.md) — catalogue-first vs outcome-first.

## Research

- [Android stack](research/android-stack.md) — verified dependency feasibility.
- [Filee matrix](research/filee-matrix.md) — feature/engine/format takhrij of the inspiration, and
  what an Android rebuild can and cannot do.
- [Market dossier](research/market.md) — market and Google Play constraints.
- [Model licences](research/model-licenses.md) — on-device model and AI-pack licence audit.
- [Competitive gap 2026-11-21](research/2026-11-21-competitive-gap.md) — build-vs-buy research.
- [UX and retention 2026-11-21](research/2026-11-21-ux-retention.md) — completion and repeat-use
  research behind the hardening pass.
- [Filee reference copies](research/filee/) — public Filee docs kept for provenance only; no Filee
  code or assets are used in Sigha.

## Council reviews

- [Qiyas council — founding review](council/qiyas-council.md) — can the idea be rebuilt honestly?
- [2026-11-17 review](council/2026-11-17-review/council-review.md) — the review that drove the
  production-readiness pass.
- [2026-11-17 UI review](council/2026-11-17-ui-review/council-review.md) — design critique of 0.2.0.
- [2026-11-18 UI review](council/2026-11-18-ui-review/council-review.md) — delta re-run of the 0.2.1
  fix pass.
- [2026-11-18 next](council/2026-11-18-next/council-decision.md) — what remained after 0.2.1.
- [2026-11-19 state](council/2026-11-19-state/council-decision.md) — readiness and ASO decision.
- [2026-11-21 hardening](council/2026-11-21-hardening/council-decision.md) — hardening review
  decision.

## Planning and release

- [Production-readiness plan](plan/production-readiness.md) — the workstream plan behind 0.2.x.
- [Publisher guide](plan/publisher-guide.md) — the critical path from this tree to a closed test,
  including the human-only blockers.
- [Publishing checklist](publishing-checklist.md) — the GitHub settings a maintainer must set by hand.
- [Release notes v0.2.1](release-notes-v0.2.1.md) — the GitHub Release notes for the current release.

## Models

- [Model packs and OCR](models/integration.md) — how the optional OCR pack is downloaded, verified
  and used; the upscaling family is intentionally not enabled in this build.
