# Material conformance — Android platform / Material 3 delta re-audit

**Seat:** material-conformance · **Round:** 2026-11-18 · **Tree:** HEAD `90d81a9` (fix pass `ed3ee6c`) · **Baseline:** `docs/council/2026-11-17-ui-review/seats/material-conformance.md` · **Dimension:** platform/window conformance, theming, adaptivity, motion/state restoration. Read-only audit: no app-source, script or QA-evidence edits; device work owned by the Lead.

## Verdict

0.2.1 closes the baseline's fatal flaw at the structural level: one owner now supplies the window contract (`SighaNavHost.kt:75-134`), and system-bar appearance follows the *resolved* app theme rather than `onCreate` (`MainActivity.kt:56-67`). Of the 15 baseline findings in this dimension, **4 are resolved, 5 partial, 6 open**; no P0/P1 regression. Two P2 residuals: landscape/IME behaviour of HEAD is **not covered by the evidence set** (every landscape capture predates the final inset line and the empty state is still centred on the physical screen), and the single-choice chip-semantics fix stopped half-way. Nothing in this dimension blocks shipping 0.2.1, but the conformance claim is not yet evidence-complete.

## Status of every baseline finding

| # | Baseline finding | Status | Evidence |
|---|---|---|---|
| 1 | System-bar appearance frozen at `onCreate` | **Resolved** | `MainActivity.kt:56-67` re-applies `enableEdgeToEdge` from the resolved `darkTheme` (icon appearance derives from the same predicate); `home-empty-ar-dark.png` (forced dark over a light system) shows a dark status region over `#0F1513` and a `#1A2022` nav strip; light captures show status icons over `#F4F8F6`. |
| 2 | IME / safe-drawing insets unconsumed | **Partially resolved** | Root consumes IME + horizontal `safeDrawing` and the idle bottom safe area (`SighaNavHost.kt:76,122-134`); Queue's old nav-bar padding is gone. But there is no IME-open capture and the landscape evidence predates `SighaNavHost.kt:125` — see N1, N2. |
| 3 | Queue missing bottom inset when idle | **Resolved** | Cancel bar only when work is active (`QueueScreen.kt:96-117`); root adds bottom safe area when the bottom bar is hidden (`SighaNavHost.kt:126-134`); `queue-empty-ar.png` (captured 2026-11-18, after the fix) clears the 3-button strip. |
| 4 | Nine nested Scaffolds / four inset strategies | **Partially resolved** | One root owner (`SighaNavHost.kt:75-134`), screens use zero-inset Scaffolds; but 9 `Scaffold(` and 9 `contentWindowInsets = WindowInsets(0,0,0,0)` remain, plus 5 per-screen `SnackbarHost`s (`HistoryScreen.kt:92`, `ModelPacksScreen.kt:168`, `QueueScreen.kt:118`, `SettingsScreen.kt:128`, `WorkbenchScreen.kt:182`) and 2 dead `navigationBarsPadding()` calls (`AboutScreen.kt:65`, `LicensesScreen.kt:204`). |
| 5 | No Material You / dynamic colour | **Open** | Zero `dynamicColor` / `dynamic*ColorScheme` under `app/src/main/java`; `PRODUCT.md` records no decision for or against. |
| 6 | No adaptive layout / rail | **Open (scoped out)** | `PRODUCT.md:109-110`: phones only, landscape must stay usable at 200% font, multi-pane/rail out of scope; still no `WindowSizeClass` / `BoxWithConstraints`; only a height-based `EmptyState` variant (`Common.kt:119`). |
| 7 | Non-M3 XML theme, no night resources/splash | **Partially resolved** | `values-night/themes.xml:7` now sets `#0F1513` and the light window hex equals `SighaMist` (`themes.xml:6`); still `android:Theme.Material.NoActionBar` (`themes.xml:3`), no splashscreen API, and the window still follows system `uiMode` — see N6. |
| 8 | Notification permission on cold start | **Open** | `MainActivity.kt:44`, called before `setContent` (`:46`). |
| 9 | Shared file never navigates to Workbench | **Resolved** | `WorkbenchViewModel.kt:90-91,123` emits on add; `SighaNavHost.kt:64-73` collects and navigates `WORKBENCH` with `launchSingleTop`. |
| 10 | Hand-rolled card/pills/bottom bar, no `Shapes` | **Partially resolved** | `Shape.kt:12-17` + `SighaCard.kt:24-31` tokenised now; still hand-rolled 20dp/`tonalElevation` rows (`QueueScreen.kt:183-185`, `HistoryScreen.kt:183-185`), 12dp-shadow bars (`WorkbenchScreen.kt:808-811`, `QueueScreen.kt:98-102`), 28dp hero (`AboutScreen.kt:76`) and 12dp code block (`LicensesScreen.kt:419`). |
| 11 | Radio/selectable semantics; FilterChip single-choice; small licences header | **Partially resolved** | Radio rows and dialog groups fixed (`SettingsScreen.kt:155,278,347-378`); licences header ≈60dp; theme and target/bitrate chips are still `FilterChip` (checkbox role) — see N3. |
| 12 | Two-line top bar clipping at 200% font | **Resolved** | `Common.kt:59-61`: one line, `titleLarge`, ellipsis; tagline moved to `AboutScreen.kt:98-103`; portrait, English and landscape font-200 captures show no clipped title. |
| 13 | Zero motion | **Open** | No animation API in `ui/` (grep for `animate`, `AnimatedVisibility`, `Crossfade` finds none). |
| 14 | No `<monochrome>` icon, no `localeConfig` | **Open** | No `monochrome` or `localeConfig` anywhere in `app/src/main`; no `res/xml/locales_config.xml`. |
| 15 | Near-zero state restoration | **Open** | 1 `rememberSaveable` (`LicensesScreen.kt:309`), 0 `SavedStateHandle`; OCR dialog state uses plain `remember` (`WorkbenchScreen.kt:683`); `configChanges` still masks recreation (`AndroidManifest.xml:22`). |

## New findings / regressions (P0–P3)

No P0 or P1 regressions were found in this dimension.

**N1 — P2, verification gap: the landscape inset of HEAD is unproven.** Every landscape capture was written at 22:53, ~33 min before fix commit `ed3ee6c` (23:26), so it may predate the final root inset line. In `home-empty-ar-landscape.png` and `home-empty-ar-landscape-font200-scrolled.png` the empty-state CTA is centred at x≈1200 of 2400 (measured green band 1008–1393 and 927–1474), i.e. the physical centre, while the 3-button strip starts at x=2274 (126 px = 48 dp); an applied horizontal `safeDrawing` inset moves the centre to ≈1137. Top-bar title (ends x≈2225) and tab labels do sit clear of the strip, so the *chrome* is inset correctly. Why it matters: the landscape half of #2 cannot be closed from this evidence set — re-capture from the HEAD build before the window contract is called verified.

**N2 — P2, inferred IME/chrome stacking (new risk from the fix).** Root `imePadding()` (`SighaNavHost.kt:76`) lifts the whole scaffold, bottom `NavigationBar` included, and `NavigationBar` also applies its own bottom system-bar inset: verified against the material3 1.3.1 AAR that `NavigationBarDefaults.windowInsets = systemBarsForVisualComponents.only(Horizontal + Bottom)` and that `NavigationBar` consumes that parameter (`javap`). With the keyboard open the bar should therefore float one nav-bar inset above the IME (a visible gap), with the Workbench/Queue bottom bars stacking below it. No IME capture exists, so this is inferred. Why it matters: the field being typed into is exactly what baseline #2 targeted; one keyboard-open screenshot settles it.

**N3 — P3, single-choice semantics still incoherent.** `ThemeSelector` now wraps its `FlowRow` in `selectableGroup()` but keeps `FilterChip` children (`SettingsScreen.kt:319-329`); Workbench target (`WorkbenchScreen.kt:419-424`) and bitrate (`:531-543`) chips have neither a group nor radio semantics. `FilterChip` sets `Role.Checkbox` directly (verified bytecode in `ChipKt$SelectableChip$1`), so TalkBack announces checked/unchecked for a one-of-N choice — the unfixed half of #11, now inconsistent within one screen. Why it matters: a screen-reader user cannot tell a radio choice from a multi-select one.

**N4 — P3, Queue empty state still non-scrollable.** `QueueScreen.kt:122-134` centres a plain `Column`, while Workbench's empty branch is now a scrollable `LazyColumn` (`WorkbenchScreen.kt:188-230`) and `EmptyState`'s compact variant keys off `screenHeightDp < 480` only (`Common.kt:119`). `PRODUCT.md:109-110` promises landscape usability at 2400×1080/200% font; at that size the compact state is borderline and cannot scroll, unlike Workbench. Inferred, not captured — no queue-at-200% shot exists.

**N5 — P3, theming debt left by the palette pass.** `Theme.kt:21-85` authors no `tertiaryContainer` / `onTertiary` / `onTertiaryContainer`, `surfaceBright`, `surfaceDim` or `scrim` (only `tertiary`); any future date/time picker, bottom sheet or dialog that reads them falls back to baseline M3 purple. Hard-coded radii survive on four surfaces and two elevation bars (see #10), so `SighaShapes` is read by exactly one component (`SighaCard.kt:26`). No functional failure today; brand-drift risk only.

**N6 — P3, cold-start window still follows the system, not the app theme.** `themes.xml:3` keeps the platform `Material` parent and `values-night/themes.xml:7` is selected by system `uiMode`, while the app theme is resolved only inside Compose (`MainActivity.kt:48-52`). A user who forces Dark over a light system still gets a light window background for the first frames, and platform-styled permission dialogs; the inverse applies for forced Light. Residual half of #7; cold start is not in the evidence set.

## Dimension judgement (0.2.1)

| Dimension | Baseline | Now | Why |
|---|---|---|---|
| Platform / window conformance | 2/4 | **3/4** | One window owner; system bars follow the resolved theme including forced dark (`home-empty-ar-dark.png`); insets consumed once; Queue idle inset fixed. Held back by stale landscape/IME evidence (N1/N2), 9 nested Scaffolds, dead inset calls, stale `adjustResize` (`AndroidManifest.xml:25`). |
| Theming / token discipline | 2/4 | **3/4** | Full light/dark role ramp, `Shapes` wired, Arabic typography line-height fix, `SighaCard` tokenised, window hexes aligned with `Color.kt`. Held back by missing roles (N5), no dynamic colour (#5), hand-rolled radii/bars (#10), system-bound cold-start window (N6). |
| Adaptivity | 1/4 | **2/4** | Compact and scrollable Workbench empty state, landscape font-200 evidence, product scope declared. Still no width-based adaptation at all by decision (#6), and the Queue empty state is non-scrollable (N4). |
| Motion / state restoration | 1/4 | **1/4** | Unchanged: no motion, one saved-state key, no `SavedStateHandle` (#13/#15). |

Overall: 0.2.1 moves the dimension from “Material 3 inside the content box” to “Material 3 owns the window boundary”. Two dimensions still sit at 3/4 and none reaches 4/4. This dimension does not block the release; close the P2s with evidence (N1/N2) and a small follow-up (N3) before repeating the conformance claim in release notes.

## Limits

- Evidence is the Android 16 (API 36) emulator only — 1080×2400 @420 dpi, 3-button navigation, per-app Arabic locale. There is no IME-open, landscape-3-button, landscape-cutout, TalkBack, OEM or real-device capture; anything depending on those is marked inferred.
- All landscape captures (22:53) predate fix commit `ed3ee6c` (23:26); I could not confirm they came from the final tree. #2's landscape half, N1 and N2 need a re-capture from HEAD — the Lead owns the device.
- The per-app locale was set via shell `cmd locale set-app-locales`; the Settings UI affordance for per-app language still depends on `localeConfig` (#14).
- I did not read the other seats' 2026-11-18 reports; this is a delta inside my dimension only.
- No Gradle or emulator run this round. Component semantics (`FilterChip` → `Role.Checkbox`, `NavigationBarDefaults` insets) were verified with `javap` against the cached material3 1.3.1 AAR, not on a device.
- Colour/contrast statements use `Theme.kt` constants and screenshot pixels; no WCAG sweep was repeated here (accessibility seat owns it).
- API 26–34 first-frame edge-to-edge timing is unverified: `enableEdgeToEdge` now runs from a `LaunchedEffect` (`MainActivity.kt:56-67`) instead of `onCreate`; on API 36 edge-to-edge is enforced anyway, so the evidence cannot show a flash.
