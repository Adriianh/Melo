# Known Issues

## TUI — Focus & Key Event Handling

### Unfocused panels respond to key events

**Status:** Open  
**Affects:** All screens (Home, Search, Library, Sidebar)

#### Description

Pressing `↑`/`↓`/`Enter` while a panel is **not** the active focused element can
still trigger actions in other panels (e.g. navigating the sidebar list while the
search bar has focus, or scrolling the results list while the sidebar is selected).

#### Root cause

tamboui's `EventRouter` broadcasts any key event that is **not consumed** by the
focused element to **all registered elements** as a "global hotkey" pass. Each
panel's `onKeyEvent` lambda is invoked with `isFocused = false`, but the lambda
itself has no reliable way to distinguish a genuine global hotkey from a navigation
key that should be scoped to the focused panel only.

Guards based on `runner()?.focusManager()?.focusedId()` partially mitigate the
issue but do not fully prevent it in all focus transition states (e.g. during the
first render frame after switching sections, or when `focusedId` is `null`).

#### Workaround

Use **Tab** to explicitly move focus to the desired panel before using arrow keys
or Enter. The focused panel is indicated by a colored border (`PRIMARY_COLOR`).

#### Potential fix

Waiting on upstream tamboui support for a scoped `onKeyEvent` that receives an
`isFocused: Boolean` parameter (already present in the internal `handleKeyEvent`
API but not exposed to the toolkit lambda API).  
Alternatively, a custom `FocusAwarePanel` wrapper that intercepts the `EventRouter`
dispatch and short-circuits navigation keys when not focused could be implemented
once the tamboui API is better understood.

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

Home and Library screens wrap their root element in a `stack()` with
`ClearGraphicsElement` as the base layer, which erases terminal graphics on every
render cycle. Any new screen added in the future **must** follow this same pattern.

