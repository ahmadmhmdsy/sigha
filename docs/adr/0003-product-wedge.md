# ADR-0003: Catalogue-first vs outcome-first product

- **Status:** accepted for 0.2 (outcome-first), catalogue breadth kept as capability
- **Date:** 2026-11-17
- **Deciders:** Lead, product-strategist, Maestro

## Context

The store hook was "48 formats". The graph shows 48 recognised, 39 convertible, 25 producible, and 9 formats with no edge at all. Market evidence (Filee-derived apps, WhatsApp compression behaviour, scanner apps) shows users open a converter for an outcome: scan to PDF, combine images into one PDF, merge/split/compress a PDF, or shrink a photo/video to a shareable size.

## Decision

Lead the product with four outcomes, while keeping the format graph as a capability and an advanced surface. Do not market the count as "every format works". Implement the outcomes before the store push (Phase 3 of the 90-day plan); until then the listing describes only what ships.

## Consequences

- Positive: claims become true; the store hook matches observed demand; the engine graph remains an asset.
- Negative: the app needs new UI flows (multi-image -> one PDF, PDF merge/split/compress, compress-to-size) that are not in 0.2; the format matrix moves to a secondary screen.
- Guardrail: no outcome feature ships without device QA and a passing verify.ps1 run.
