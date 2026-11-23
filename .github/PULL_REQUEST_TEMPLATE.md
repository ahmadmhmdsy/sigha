## Summary

<!-- What does this change do, and why? -->

## Scope

<!-- Which files or areas are touched? What does this pull request deliberately NOT change? -->

## Evidence

<!-- Required for any behaviour, number or claim change. Evidence files live in docs/qa/evidence/. -->

- [ ] Full gate on this commit: `pwsh -File scripts/verify.ps1` — evidence
      `docs/qa/evidence/verify-<stamp>.json`
- [ ] Runtime check, if user-visible: `scripts/device-smoke.ps1`, `scripts/feature-smoke.ps1` or
      `scripts/retention-smoke.ps1` — evidence `docs/qa/evidence/...`
- [ ] Not applicable (documentation-only change)

## Test commands run

```text
# paste the commands and their exit status here
```

## Checklist

- [ ] I read [CONTRIBUTING.md](https://github.com/ahmadmhmdsy/sigha/blob/main/CONTRIBUTING.md), and this change follows the evidence rule.
- [ ] `CHANGELOG.md` has an entry under the current unreleased version (documentation-only changes may skip it).
- [ ] Docs that state measured facts are updated from the evidence JSON, not from memory.
- [ ] No telemetry, analytics, ads, cloud upload or new network calls — the optional OCR pack download is the only one.
- [ ] No new dependency without a licence review in `NOTICE.md` and `docs/legal/licence-compliance.md`.
- [ ] No secrets, keystores, `local.properties`, build output or personal data are committed.
- [ ] User-visible strings exist in both `values/strings.xml` and `values-ar/strings.xml`; RTL and dark mode checked.
- [ ] Placeholder markers in shipped legal text were not deleted to make the release gate pass.
- [ ] No claim or number is presented without evidence.
