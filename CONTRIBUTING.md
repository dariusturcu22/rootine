# Contributing

How we work. Short version: issue, branch, commits, PR, review, merge.

## 1. Pick an issue

- Work is tracked as GitHub issues on the project board. Pick one from **Todo** in your team's column and move it to **In progress**.
- If there is no issue for your work, create one from a template (feature, bug or task). Do not start work without an issue.
- If the work changes an API, the contract in [docs/api/](docs/api/README.md) is agreed first, in a PR reviewed by both sides.

## 2. Branch

Create a branch from an up-to-date `main`. Name: `<type>/<issue-number>-<short-name>`.

| Type | Use for | Example |
|---|---|---|
| `feat` | New feature | `feat/12-parcel-crud` |
| `fix` | Bug fix | `fix/31-heatmap-empty` |
| `docs` | Documentation only | `docs/40-api-backend-auth` |
| `chore` | Tooling, config, dependencies | `chore/45-bump-vite` |
| `test` | Tests only | `test/50-risk-job-tests` |

```bash
git switch main
git pull
git switch -c feat/12-parcel-crud
```

Never push directly to `main`.

## 3. Commit

Use [Conventional Commits](https://www.conventionalcommits.org/) with the folder as the scope: `type(scope): short description`. Scopes: `frontend`, `backend`, `ai-service`, `docs`, `ci`, and `root` for changes that touch many parts or only root files.

```
feat(backend): add parcel CRUD
fix(frontend): show empty state on the heatmap
docs(api): describe the diagnoses endpoints
test(ai-service): add golden image tests
chore(ci): update setup-node
```

Allowed types: `feat`, `fix`, `docs`, `test`, `chore`, `refactor`, `ci`, `build`, `perf`, `style`. `feature` is not a type.

Rules for commit messages and PR text:

- English, present tense, short first line (about 70 characters).
- **No attribution.** No `Co-Authored-By` lines, no "Generated with ..." lines, no emoji robot lines, no mention of AI tools. CI fails the PR if it finds them.
- No secrets, datasets or model files in commits.

## 4. Check locally

Run the checks of the part you changed before you push. Commands are in [SETUP.md](SETUP.md): tests, lint and format for [frontend](SETUP.md#frontend), [backend](SETUP.md#backend) and [AI](SETUP.md#ai-service).

Every PR includes tests for what it changes.

### Lint baseline

Stricter lint rules (see the Code standards in [AGENTS.md](AGENTS.md#code-standards)) apply to every new file. Files that broke them when the rules were added are listed in one place per tool:

| Tool | Baseline list |
|---|---|
| ruff (`ai-service`) | `[tool.ruff.lint.per-file-ignores]` in `ai-service/pyproject.toml` (except the `tests/**` line, which is a permanent rule) |
| ESLint (`frontend`) | `lintBaselineFiles` in `frontend/eslint.config.js` |
| Prettier (`frontend`) | the last block of `frontend/.prettierignore` |

Never add a file to a baseline. When you clean a file, remove it from the list in the same PR.

## 5. Open a pull request

```bash
git push -u origin feat/12-parcel-crud
```

Open a PR to `main` and fill in the template. Requirements:

- Title in Conventional Commit style, for example `feat(backend): add parcel CRUD`.
- `Closes #12` in the description, so the issue closes when the PR merges.
- Tests added, docs updated, API contract updated if an endpoint changed.
- CI is green. The only required check is `ci-result`.
- The `pr-title` check tells you if the title follows `type(scope): description`. It is advisory until Demo 1 (Friday 13 November 2026). After the demo we make it blocking by adding it to the `needs` list of `ci-result`.

## 6. Review

- At least **1 approval** is required, from a code owner of the folders you changed (see `.github/CODEOWNERS`).
- Answer every comment. When you push new commits, old approvals are dismissed and the reviewer looks again.
- Reviewers: check that the tests fit the change, the API docs match the code, and nothing forbidden is in the diff.

## 7. Merge

Use a **normal merge commit**. Squash and rebase merges are disabled. The branch is deleted after the merge. Move the issue to **Done** (the board does it automatically when the PR merges).

## Releases

Tags are created at demos: `v0.1.0-demo1`, `v0.2.0-demo2`, `v1.0.0`. A tag builds and publishes the Docker images.
