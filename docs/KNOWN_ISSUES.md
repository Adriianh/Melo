# Known Issues

## Desktop (Linux) — Sign-In Failures

### YouTube Music sign-in fails with `posix_spawn` permission error

**Status:** Resolved (next release)  
**Affects:** Desktop builds installed from the Linux `tar.gz` bundle (including the Arch package)

#### Description

Signing in to YouTube Music through the system browser failed on some Linux devices with:

```
java.io.IOException: Cannot run program "/usr/bin/firefox": posix_spawn failed, error: 13 (Permission denied)
```

#### Root cause

The Linux `tar.gz` is assembled with Gradle's `Tar` task, which packs files as `0644`
unless a spec inside `eachFile` marks them executable. The bundled JVM runtime's
`lib/runtime/lib/jspawnhelper` and `jexec` were not part of that allowlist, so they lost
their execute bit. The JVM implements `ProcessBuilder` on Linux with `posix_spawn`,
which it performs through `jspawnhelper`; without the bit every child process —
including the browser opened for sign-in — fails with `EACCES`. The `.deb` and AppImage
bundles were unaffected because they inherit the permissions of the jpackage app image.

#### Fix applied

1. **Packaging allowlist:** `packageLinuxTarGz` and the AppImage task now share a single
   predicate that marks `bin/`, native libraries, scripts, `jspawnhelper`, and `jexec`
   as executable.
2. **Installer hardening:** the Linux installer and Arch `PKGBUILD` re-apply the execute
   bit to the JVM spawn helpers after copying, so a stale archive cannot regress.
3. **Browser fallback:** when a detected browser cannot be spawned, the auth flow now
   tries the remaining installed browsers and logs an actionable permission hint instead
   of aborting on the first candidate.

---

## TUI — Focus & Key Event Handling

### Unfocused panels respond to key events

**Status:** Resolved (in v2.2.2 / PR #80, Fixes #79)  
**Affects:** All screens (Home, Search, Library, Sidebar)

#### Description

Pressing `↑`/`↓`/`Enter` while a panel was **not** the active focused element could
trigger actions in other panels (e.g. navigating the sidebar list while the
search bar had focus, or scrolling the results list while the sidebar was selected).

#### Root cause

TamboUI's `EventRouter` broadcasts any key event that is **not consumed** by the
focused element to **all registered elements** as a fallback "global hotkey" pass.
`StyledElement` invokes each panel's `onKeyEvent` lambda regardless of whether the
element is focused. Without explicit focus guards, panels responded to unconsumed
navigation keys as if they were focused. Additionally, `focusedId` was `null` on startup
and could become `null` across certain transitions.

#### Fix applied

1. **Strict focus guards on all panel handlers:** Every panel key handler (`sidebar`, `home`, `results`, `library`, `now-playing`, `stats`, `offline`, `detail`) strictly checks that its element ID matches `appRunner()?.focusManager()?.focusedId()` before handling scoped keys, returning `EventResult.UNHANDLED` otherwise.
2. **Self-healing focus management:** Guaranteed initial focus on startup (`home-panel`) and automatic focus recovery in `renderRoot()`, ensuring that `focusedId` is never `null` during render or transition frames.
3. **Smooth search handoff:** Pressing `↓` from `search-bar` smoothly moves focus to `results-panel`, while pressing `↑` at index 0 or `/` from `results-panel` returns focus to `search-bar`. Arrow keys in `search-bar` are consumed to prevent leakage.
4. **Bidirectional navigation:** Left arrow (`MOVE_LEFT`) seamlessly returns focus from main panels to `sidebar-panel` when on the leftmost list edge.

---

## TUI — Terminal Graphics Persistence

### Artwork pixels bleed into non-image screens

**Status:** Resolved (in TamboUI 0.4.0)  
**Affects:** Terminal graphics rendering

#### Description

When a track's artwork is loaded (pixel/sixel rendering), switching to a screen
that does not display an image may leave the artwork visible as a ghost behind the
new content.

#### Fix applied

TamboUI 0.4.0 natively handles raw output cleanup via `Terminal.cleanupRawOutput()` when an `Image`
widget stops rendering (issuing terminal escape codes and clearing the previous image rectangle).
The previous workaround of using `ClearGraphicsElement` / `ClearGraphicsWidget` was removed because
registering dummy `RawOutputCapable` areas across screens caused terminal text to be wiped with
spaces and created buffer desynchronization artifacts.

