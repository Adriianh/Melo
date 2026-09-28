# Known Issues

## TUI — Focus & Key Event Handling

### Unfocused panels respond to key events

**Status:** Resolved (in v2.2.2 / PR #79)  
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

**Status:** Partially fixed  
**Affects:** Home screen, Library screen (fixed); other future screens may be affected

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

