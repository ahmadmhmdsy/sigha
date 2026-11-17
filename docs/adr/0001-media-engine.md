# ADR-0001: Native media engine - ffmpeg-kit fork vs Media3-only

- **Status:** accepted for the 0.2 production-readiness pass; revisit at day 45
- **Date:** 2026-11-17
- **Deciders:** Lead, policy-guardian, engine-auditor

## Context

The app converts video and audio formats that Android's platform decoders and Media3's transformers cannot cover (MKV/AVI/FLV/WebM containers, FLAC/OGG/OPUS/AMR audio, arbitrary codec pairs). The chosen dependency is dev.ffmpegkit-maintained:ffmpeg-kit-full:8.1.9, a republish of the retired Arthenica ffmpeg-kit line by a single maintainer (POM inceptionYear 2026). It is LGPL-3.0 per the POM and contains LGPL-2.1-or-later FFmpeg plus LGPL/BSD externals.

## Decision

Keep ffmpeg-kit for the 0.2 closed-test build, under three conditions:

1. The LGPL compliance pack ships (licence texts, written offer for Corresponding Source, relink information) and counsel signs off on the clearance memo.
2. The dependency is pinned and verified (Gradle dependency verification metadata), and a migration plan exists.
3. At day 45, decide: stay (with counsel sign-off) or replace the media edges with a Media3-only engine and drop unsupported formats honestly.

## Consequences

- Positive: real format coverage; engine already integrated and tested; no rewrite.
- Negative: legal duty, supply-chain risk (one maintainer, young fork), ~24 MB of native code per ABI, no upstream support.
- If the fork is abandoned: no security updates for the native decoders; fallback is a narrower Media3-only build with a reduced catalogue.

## Alternatives considered

- **Media3-only:** lawful and maintained, but loses MKV/AVI/FLV and most audio codec pairs; the catalogue would shrink.
- **Server-side conversion:** rejected - violates the offline/privacy promise and adds cost.
- **Self-built FFmpeg:** the most defensible long-term (own the source, control licences) but weeks of NDK work; keep as the day-90 option.
