## Agent skills

### Issue tracker

Issues live in this repo's GitHub Issues (`qwerkilo/SpektraLab-Android`), managed with the `gh` CLI. See `docs/agents/issue-tracker.md`.

### Triage labels

Five canonical roles, default label strings. See `docs/agents/triage-labels.md`.

### Domain docs

Single-context: `GLOSSARY.md` and `docs/adr/` at the repo root. See `docs/agents/domain.md`.

## Pre-commit hook

Enable once per clone: `git config core.hooksPath .githooks`.
The hook syntax-checks staged `.sh` files with `bash -n` and staged `.py` files with `ast.parse`.
It does not run the Android build.
