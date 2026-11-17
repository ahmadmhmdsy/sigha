# ADR-0002: Execution model - foreground service over WorkManager

- **Status:** accepted for 0.2
- **Date:** 2026-11-17
- **Deciders:** Lead, app-auditor, build-verifier

## Context

Conversions ran in an app-scoped coroutine (data/AppCoroutines.kt). If the process died, the batch was lost silently. androidx.work was declared (2.10.0) but had zero usages while injecting WAKE_LOCK, RECEIVE_BOOT_COMPLETED and FOREGROUND_SERVICE permissions into the manifest. The council rated this High (H1) and flagged the declared-but-unused dependency as a trust defect.

## Decision

Use a foreground service (work/ConversionService.kt) started when a batch begins and stopped when the queue is idle. The existing in-process queue remains the execution engine; the service keeps the process alive and shows progress. Persist a batch_active flag so an interrupted batch is announced on the next start. Remove the WorkManager dependency and the permissions it injected.

WorkManager was rejected for this pass: its Worker/constraint model would require re-architecting the queue (progress, per-item cancellation, output publishing) with high regression risk and no benefit for a user-initiated, screen-on task.

## Consequences

- Positive: batch survives backgrounding; progress is visible outside the app; permissions now match behaviour.
- Negative: a foreground service is not resumable after process death (the batch is lost, but no longer silently); notification permission must be requested on API 33+.
- Future: resumable queues are a deliberate 1.x project (persisted job spec + per-item state machine), not a patch.
