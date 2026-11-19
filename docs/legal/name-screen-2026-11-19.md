# Name screen — "Sigha / صيغة" (builder-side, public sources)

**Date:** 2026-11-19 (Asia/Baghdad)
**Block:** `docs/legal/release-blockers.md` B5, publisher-guide Step 2
**Status:** research only — **decision OPEN** (the decision and any filing are the publisher's or counsel's)
**Scope:** what is publicly visible from this machine. **Not legal advice.** Registry searches were
**not** run (see §5).

The package id (`com.sigha.converter`) and the listing title are effectively permanent after the first
upload. This record exists so the decision is made against visible evidence, before the first upload.

## 1. Method

| Source | Query | Date |
|---|---|---|
| Google Play (US and IQ storefronts) | `Sigha`, `صيغة`, `Siyagha`, `Sighat`, `Sigha PDF`, `Sigha converter` | 2026-11-19 |
| Apple App Store (iTunes Search API, US storefront) | `Sigha`, `صيغة`, `Siyagha` | 2026-11-19 |
| General web search | `"Sigha" trademark`, `"Siyagha" trademark app`, `"Sigha Sistemas"`, `"صيغة" علامة تجارية` | 2026-11-19 |

Play results were read with `google-play-scraper` 10.1.4 (search + app detail against
play.google.com). App Store results were read with the public iTunes Search API
(`https://itunes.apple.com/search?term=…&entity=software&country=us`), then filtered for
Sigha/صيغة/Siyagha-like titles. Search APIs are fuzzy: a zero match is evidence of absence from the
returned result set, not proof that no app exists.

## 2. Google Play

- **No app titled "Sigha"** was returned on either storefront. The "Sigha" query returned only
  fuzzy neighbours (`Sigatul-Haj`, `Sighisoara Transport Local`, `SigzenSFA`, `Singpass`).
- **No app titled "صيغة"** was returned. The word appears descriptively in converter/player titles
  ("فيديو لاعب كل صيغة" = "video player, all formats"), which is expected for an Arabic word meaning
  "format".
- **Closest name on Play: "Siyagha"** — `app.siyagha.ai`, developer Osama Ayesh, genre lifestyle,
  "+100" installs, Arabic summary "إضفاء الطابع الإنساني على نصوص الذكاء الاصطناعي…" (humanising AI
  text). Its site `siyagha.ai` is "صياغة — إعادة صياغة النصوص وتحويل اللهجات العربية", an Arabic
  rewriting SaaS. This is the strongest name-similarity candidate found.
- `dz.bijouterie.gestion` — "Siagha — Gestion de bijouterie" (jewellery management, Algeria).
- `com.maktabatulishaat.SharhIlmusSigha` — "Ilm us Sigha علم الصیغۃ" (Arabic morphology book app);
  uses the word in its grammar sense, different category, low confusion risk but noted.
- No file-conversion app titled Sigha was found.

## 3. Apple App Store (US storefront)

- `Sigha`: 40 results, **none titled Sigha** (fuzzy: Singha Logistics, Picsart, …).
- `صيغة`: 188 results, none titled صيغة; nearest by spelling is **"Soghah - صوغة"** (Tocaan LLC,
  `id6451238147`) — a different Arabic word (صوغة vs صيغة), one letter apart.
- `Siyagha`: 142 results, **no app titled Siyagha**.
- Other storefronts (Iraq, MENA, EU) were not checked.

## 4. General web (software businesses using the name)

| Name | What it is | URL |
|---|---|---|
| SIGHA | Colombian HR-management software platform ("Sistema Integrado para la Gestión Humana"), part of GIGHA | https://sigha.com.co/ |
| Grupo Sigha / Sigha Sistemas | Brazilian accounting/ERP software group | https://www.sigha.com.br/ |
| صياغة (Siyagha) | Arabic text-rewriting SaaS, also the Play app above | https://siyagha.ai/ |
| **Sigha (sigha.app)** | **A live developer tool: "Sigha — Salesforce Formula Debugger" (client-side, browser). Another software product already owns the `sigha.app` domain.** | https://sigha.app/ |
| sigha.com | A different, unrelated site (TLS handshake fails from this machine); the domain is registered | — |
| "Bot Tahwil al-Sigha" | Telegram media/file converter — generic descriptive phrase ("تحويل الصيغة" = format conversion), not a brand | — |

## 5. Registries NOT queried here

These need a browser session or a filing agent/counsel; they are the searches that actually decide
B5, and a web search is not a substitute:

- **WIPO Global Brand Database** — word mark "Sigha" / "صيغة", classes 9 and 42:
  https://branddb.wipo.int/
- **Iraqi trademark registry** (Law No. 21 of 1957, Ministry of Trade) — classes 9 and 42. No reliable
  public search portal was confirmed from this machine; use the Ministry/agent route.
- **EUIPO eSearch** — https://euipo.europa.eu/eSearch/
- **USPTO trademark search** — https://tmsearch.uspto.gov
- The clearance memo §5.8 conflicts ("Filee", "Filey", "Filee Pro", the Filee donut logo) were not
  re-checked in this pass.

## 6. Reading (for the decision, not a legal opinion)

- "صيغة" is an ordinary Arabic word ("form/format") and appears descriptively in many app titles;
  that usually makes a word mark weaker, and it is for counsel to say whether it is registrable or
  protectable in classes 9/42.
- At least **three software businesses** use "Sigha"/"Siyagha": two management-software companies
  (Colombia, Brazil) and one Arabic text-rewriting SaaS whose name is the same word with one letter
  added. Confusion risk in software classes is plausible and needs a real registry search.
- The Play/App Store titles are not the blocker; the registry search and the decision are.
- **The domain the legal texts assume does not exist for us.** `privacy@sigha.app`, `opensource@sigha.app`
  and the LGPL source URL in `assets/legal/*` and `NOTICE.md` all point at `sigha.app`, which is
  live and belongs to another software product ("Sigha — Salesforce Formula Debugger"). Any mailbox
  or source host must be on a domain this publisher actually controls; if the name changes under
  B5, the domain changes with it. This is a new, concrete reason to finish B5 before B2/B6.

## 7. Decision (publisher / counsel)

- [ ] Keep "Sigha / صيغة" — counsel note: ______________________
- [ ] Change the name to: ______________________ (then package id, label, listing, icon, splash and
      every legal text change **before** the first upload, in one change)
- Decision date: ____________  Signed/approved by: ______________________

Registry search reports and filing receipts attach here when they exist. Until this block is filled,
B5 stays **OPEN** and the first upload does not happen.
