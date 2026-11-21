# UX and retention research — 2026-11-21

Scope: offline file conversion on Android, evaluated for Sigha 0.2.1 (Arabic-first, Iraq/MENA).
This note records the research that drove the 2026-11-21 production-hardening pass and the
UX roadmap that remains. It is a decision record, not a marketing document.

**Evidence standard.** Sigha ships no analytics, so none of the numbers below are Sigha's own.
Magnitudes come from adjacent categories (utility-app benchmarks, a language-learning widget
study, permission-prompt trials, a localization abandonment survey). They are directional
principles only. Platform claims that only a device can settle are marked **[verify]**.

## 1. Six findings

1. **The completion moment is the weakest part of the app.** The only "N files converted ·
   saved to …" signal is a snackbar pushed through a non-replaying SharedFlow, so it is lost when
   the user switches tabs or backgrounds the app. Before this pass the workbench result row offered
   Open but not Share; the progress notification had no tap target; History never showed where a
   file went. "Did it work, and where is it?" is the one question a converter must never leave open.
2. **For images, "where did my file go" is structurally broken on Android 10+.** Everything is
   published to the Downloads collection (`Download/Sigha`), which gallery apps and the system
   photo picker do not list. Most WhatsApp-first users judge success by whether the picture is in
   their gallery. On API 26–28 the legacy file + MediaScanner path usually is gallery-visible
   **[verify]**.
3. **Permission friction is inverted.** `POST_NOTIFICATIONS` was requested unconditionally in
   `onCreate` — the anti-pattern Android documents against, and the opposite of what the repo's own
   permissions rationale claims. A 2024 JMIR trial found contextual prompts materially more
   effective than setup prompts at earning authorisation. Storage denial on API 26–28 had no
   recovery path: no Settings link, no automatic folder fallback. (The 2026-11-21 pass fixed the
   denial recovery; the priming still asks on launch and is a follow-up.)
4. **There is no repeat-use scaffolding.** Zero launcher shortcuts, zero widgets, no last-target
   memory, no re-run from History, no recent-file surface on the home screen. The only repeat
   surface is a 50-row History three taps away.
5. **Failure UX is half-built.** Eighteen typed, language-independent failure kinds with localized
   headlines is better than most competitors. But none carries an action button, "retry" did not
   exist anywhere, and an input-URI permission loss surfaced as generic `UNKNOWN`. The 2026-11-21
   pass added typed no-audio / no-video kinds, a partial-publish note, a Settings action on storage
   denial, and graceful handling of missing files; per-kind actions remain.
6. **Trust assets are real but buried.** No ads, analytics or account dependencies; a precise
   on-device claim; in-app privacy and licence screens; SHA-256-pinned model packs. Trust has to be
   said in-product at the moment of doubt — right after the first conversion — not only on a store
   label.

## 2. Retention mechanics that fit this app

| Mechanic | Why it fits | Current gap (after 2026-11-21) | Effort |
|---|---|---|---|
| Share-after-convert on the result row | Converted files usually exist to be sent | Added to the workbench row this pass (Queue/History already had it); "Share all" still missing | S |
| Persistent result card (counts, destination, Share, Open, "Where is it?") | Replaces the lossy snackbar; survives tab switches and backgrounding | Not built; highest-leverage remaining UX change | M |
| Remember last target per category | Removes the only real decision; second run becomes one tap | Not built | S |
| Recent conversions on home + "Convert again" | Turns recall into a repeat | Not built | M |
| Launcher shortcuts (2 static + 1 dynamic) | Closest thing to a widget without a dependency | Not built | S–M |
| Completion notification with tap-through | Closes the loop when the user leaves a long batch | Progress notification now opens the app; completion notification not built | M |
| Batch retry-failed | Partial batches currently require re-picking everything | Not built | S–M |
| "Try a sample" in the empty state | Removes the need to have a file in the right place | Not built | S |

**What not to do:** streaks, badges, generic "come back" notifications, onboarding carousels.
They contradict the no-tracking, no-nag position and Play's notification policy.

## 3. The completion contract

One persistent result surface in the workbench until dismissed or a new batch starts:

1. Headline: "5 files converted · saved to Download/Sigha", plural-aware, failed count when non-zero.
2. Primary action: Share all (multiple) / Share (single).
3. Secondary: Open, Open folder (best effort), "Where is it?" sheet, Copy path.
4. Each row keeps Open, adds Share and Retry on failure; the location line becomes tappable.
5. The card is ViewModel state, not an event; on process death it can be rebuilt from History.

Snackbars remain only for transient messages (file added, path copied, permission denied with an
action).

## 4. "Where did my file go?" — platform truth

| Output type | API 26–28 (legacy + MediaScanner) | API 29+ (MediaStore) |
|---|---|---|
| Written where | Real file in public Download, then scanned | MediaStore.Downloads row with `RELATIVE_PATH Download/Sigha`, `IS_PENDING` while writing |
| Visible in Files app | Yes | Yes |
| Visible in gallery / photo picker | Usually yes, indexed by MIME **[verify]** | **No** — the row is in the downloads collection, not the images collection |
| Permission | `WRITE_EXTERNAL_STORAGE` (maxSdk 28) | None for the app's own inserts |
| SAF folder mode | Folder display name + file name; absolute path unknowable | Same |

**Recommendation (not yet shipped): route by media type.** Images → `MediaStore.Images` with
`Pictures/Sigha`; video → `MediaStore.Video` with `Movies/Sigha`; audio → `MediaStore.Audio`
with `Music/Sigha`; everything else → `MediaStore.Downloads` with `Download/Sigha`; the same rule
on 26–28 through the legacy path + scan. Ship it with a "keep one folder" toggle for people who
prefer the current promise. This is the real fix for the most common post-conversion doubt; until
then the honest copy is: "Files apps list it; gallery and Photos apps usually don't."

## 5. Failure UX

Principles (NN/g error-message guidance, 2023): keep the message close to the source, say what
happened and what to do next, never say only "an error occurred", protect effort with retry, match
severity to the format, never blame the user.

Mapping for Sigha:

- Inline action at the row, one primary next step per kind: Install pack, Free space, Choose folder,
  Allow access, Retry, Add files.
- Partial success is first-class: "4 of 5 converted · 1 failed" + "Retry failed (1)"; the saved rows
  stay shareable.
- Cancel is destructive with work in it: "Stop converting? Files already converted stay saved." /
  "إيقاف التحويل؟ الملفات التي اكتملت ستبقى محفوظة."
- Retry semantics: re-enqueue only failed entries; check readability first so a lost grant becomes
  `FILE_ACCESS_LOST`, not `UNKNOWN`; never auto-retry.

## 6. Permissions and onboarding

- Ask for notifications at the first Convert that is likely to outlive the screen (2+ files, or
  audio/video target), not on launch. Prime in-app, remember "Not now", never re-prompt after a
  system denial; add a Settings row that opens notification settings.
- Storage denial must never block conversion: offer SAF folder mode and a Settings action. The
  2026-11-21 pass added the action and the automatic Downloads fallback for a revoked folder.
- Empty-state CTAs: History's "No conversions yet" and Queue's "Nothing in the queue" are dead ends;
  add "Choose files" / "Go to Convert".

## 7. How to validate without analytics

1. 12-tester closed test (Play requirement) with a structured exit interview: first-task time,
   "where is the file?" answer, failure interpretation.
2. A 2–4 week diary study with 5–8 users: when do they convert, do they find outputs, do they
   repeat without the icon?
3. Instrument only what can be counted locally and shared voluntarily (tester-instrument file);
   do not add an SDK, do not add network analytics.

## 8. Sources

- Airship Q3 2025 activation benchmarks (via eMarketer): https://www.emarketer.com/content/industry-kpis--utility-driven-apps-outperform-entertainment-on-day-30-engagement
- Duolingo widget retention (archived): https://web.archive.org/web/20240504045720/https:/blog.duolingo.com/widget-feature/
- Android launcher shortcuts: https://developer.android.com/develop/ui/views/launch/shortcuts
- NN/g usability heuristics: https://www.nngroup.com/articles/ten-usability-heuristics/
- NN/g error-message guidelines: https://www.nngroup.com/articles/error-message-guidelines/
- JMIR 2024 contextual permission prompts: https://www.jmir.org/2024/1/e55694
- Applause 2024 localization survey: https://www.applause.com/blog/global-apps-need-local-flavor-to-win-customers/
- Android notification permission guidance: https://developer.android.com/develop/ui/compose/notifications/notification-permission
- MediaStore shared-media guidance: https://developer.android.com/training/data-storage/shared/media

**Caveat.** The 2024 JMIR trial's sample size is not cited here: an earlier draft claimed n=210 and
the source did not support that number on re-check. The direction (contextual beats setup prompt)
stands on the paper's own reporting; the magnitude should be read from the paper.
