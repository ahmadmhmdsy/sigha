# Tester instrument — closed test (12 tasks × 14 days)

**For:** the publisher, to send to the 15 testers recruited for the Play closed test.
**Companion:** `docs/product/store-listing.md` §8 (cohort and pass criteria), `docs/plan/publisher-guide.md` Step 9.

The 14-day test is a **compliance gate plus a crash/claim census**, not a desire test. Its job is to
show the build survives real phones and that no listing claim is false. The exit interview is the only
product signal — it feeds 0.2.2, not this upload.

## 1. What testers receive

One message with: the Play opt-in link, the task list below, the report format, and the exit questions.
Ask for one line per task (PASS / FAIL / SKIP + what happened) and one screenshot only when something
is broken.

## 2. Task list (send verbatim)

1. Convert a photo to JPG and to PDF; check the result in Downloads.
2. Convert a DOCX to PDF and to TXT; check the text output and the note about layout.
3. Convert an XLSX with two sheets to CSV; confirm the "first sheet only" note.
4. Convert a PDF to JPG with page range 1; then a scanned PDF (no text layer) to TXT **without** the
   OCR pack — expect a clear install instruction, not a crash.
5. Install the OCR pack on Wi-Fi, then repeat task 4 — expect text and the OCR note.
6. Start a 5-file batch, switch apps for 2 minutes — expect the notification/progress to continue, and
   the final rows to be complete on return. Note the bar's progress text (n of m) and the destination
   shown when it finishes.
7. Turn on airplane mode and convert an image — expect success (no network needed).
8. Deny the notification permission (Android 13+) and start a batch — expect conversion to continue, no
   crash, progress still visible in Queue.
9. Share a file from Files/Photos into Sigha; convert it; share the result back out.
10. Switch the app to Arabic and repeat one conversion; report any untranslated or broken RTL text.
11. Deny legacy storage permission on Android 8–9 (if applicable) and save — expect a message and an
    alternative folder path.
12. Fill the report form with: device model, Android version, task number, result, screenshot if broken.

## 3. Exit interview (send on day 13–14, 10 minutes)

Ask these in this order, one answer each:

1. Which task was the hardest to understand, and what did you expect the app to do?
2. Did anything say it worked when it did not? (Any false claim is a P0.)
3. Did you lose a conversion or a batch at any point? What did you see?
4. If you converted many photos at once, what did you want next? (Free text — this is the 0.2.2/0.3
   signal.)
5. Would you keep the app installed after the test? Why / why not?

Record the answers verbatim; do not paraphrase into pass/fail.

## 4. Report form

| Field | Example |
|---|---|
| Tester ID | T03 |
| Device / Android / ABI | Pixel 7 / 15 / arm64-v8a |
| Task | 4 |
| Result | FAIL — showed "conversion failed" instead of the install instruction |
| Screenshot | filename or none |
| Notes | ... |

## 5. Pass criteria (do not start production access before all hold)

- No open P0/P1 defect; no crash in a core flow.
- Crash-free sessions ≥ 99.5% in Play vitals for the cohort.
- ≥ 95% conversion success across expected-success tasks.
- Zero false claims reported.
- Notification and denial behaviour verified on at least one Android 13+ device.
- Every tester completes ≥ 8 of 12 tasks; written exit answers recorded.

## 6. Kill signals (stop and fix, do not explain away)

- OCR installs that never convert a scan (the install instruction path is broken).
- A batch that loses completed files or shows the wrong destination.
- Any tester seeing "converted" for an empty or wrong output.
- Crash on share-in or on a 5-file batch.

## 7. Where the results go

One table per tester in `docs/qa/closed-test-results.md` (create it at test start), plus the exit
answers verbatim. The publisher records the 14-day opt-in dates there. The council decision
(`docs/council/2026-11-18-next/council-decision.md`) names the metrics this instrument feeds.