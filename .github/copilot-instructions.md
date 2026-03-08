# GitHub Guidelines

## Branches
- `main` — stable, production-ready code only
- `feat/description` — new features (e.g. `feat/wrapped-screen`)
- `fix/description` — bug fixes (e.g. `fix/lyrics-scroll`)
- `refactor/description` — code restructuring (e.g. `refactor/repository-providers`)
- `chore/description` — build, dependencies, tooling (e.g. `chore/update-ktor`)

Delete branches after merging. Never reuse old branches.

## Commits
Follow Conventional Commits. Each commit must:
- Represent one logical change
- Compile and pass all tests
- Have a clear, descriptive message

Prefixes:
- `feat:` new feature
- `fix:` bug fix
- `refactor:` restructuring without behavior change
- `test:` adding or modifying tests
- `chore:` build, dependencies, tooling
- `docs:` documentation

Good examples:
```
feat: add GetSimilarTracksUseCase with blank input validation
fix: strip French prefix from lyrics.ovh responses
refactor: extract SpotifyMusicProvider from MusicRepositoryImpl
test: add SpotifyMapperTest for missing artist and artwork cases
chore: replace YoutubeApiClient with PipedApiClient
```

Bad examples:
```
wip
fixed
stuff
feat: lots of changes
```

## Pull Requests
- One PR per feature, fix, or refactor
- Title follows the same Conventional Commits format
- Description must include:
  - What changed and why
  - How to test it manually
  - Screenshots or recordings for UI changes

Template:
```markdown
## What
Brief description of the change.

## Why
Motivation or context.

## How to test
Steps to verify the change works correctly.

## Screenshots
If applicable.
```

## Code Reviews
Even working solo, review your own PR before merging:
- Read the full diff in GitHub before merging
- Verify tests pass in CI
- Check for leftover debug logs or commented code
- Confirm the change matches the PR description

## Merging
- Always merge from GitHub, not from the terminal
- Use **Squash and merge** for small fixes with noisy commit history
- Use **Merge commit** for features where individual commits are meaningful
- Never force push to `main`

## CI — GitHub Actions
Runs on every PR targeting `main`:
- Build: `./gradlew build`
- Tests: `./gradlew test`
- Packaging: `./gradlew :cli:packageApp` on ubuntu, windows, macos runners

Release installers are generated automatically on tags matching `v*`.