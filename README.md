<h1 align="center">Melo</h1>

<p align="center">
  <strong>Modern, fast, and beautiful music player for Android, Desktop, and Terminal</strong><br>
  <em>Built with Kotlin Multiplatform & Compose Multiplatform</em>
</p>

<p align="center">
  <a href="https://github.com/Adriianh/Melo/releases"><img src="https://img.shields.io/github/v/release/Adriianh/Melo?color=7C3AED&style=for-the-badge&logo=github" alt="Latest Release"></a>
  <img src="https://img.shields.io/badge/Platform-Android%20%7C%20Linux%20%7C%20Windows%20%7C%20macOS-blue?style=for-the-badge" alt="Platforms">
  <img src="https://img.shields.io/badge/Kotlin-Multiplatform-purple?style=for-the-badge&logo=kotlin" alt="Kotlin Multiplatform">
  <img src="https://img.shields.io/badge/Compose-Multiplatform-4285F4?style=for-the-badge&logo=jetpackcompose" alt="Compose Multiplatform">
  <img src="https://img.shields.io/github/license/Adriianh/Melo?style=for-the-badge" alt="License">
</p>

<p align="center">
  <a href="#features">Features</a> •
  <a href="#downloads">Downloads</a> •
  <a href="#installation">Installation</a> •
  <a href="#terminal-tui--cli">Terminal TUI & CLI</a> •
  <a href="#gui-experience">GUI Experience</a> •
  <a href="#building-from-source">Building from Source</a> •
  <a href="#tech-stack">Tech Stack</a> •
  <a href="#roadmap">Roadmap</a>
</p>

---

## Overview

**Melo** is a cross-platform music streaming and local audio player that combines visual elegance with high-performance audio engineering. It gives you the best of both worlds:

- **Melo GUI (Compose Multiplatform)**: a native, fluid graphical interface for **Android** and **Desktop (Linux, Windows, macOS)** with dynamic ambient theming, synchronized LRC lyrics with live translation, and offline downloads.
- **Melo TUI & CLI**: a fast, keyboard-centric terminal interface and headless daemon for lightweight background listening and shell integration. Both share the same unified core and library.

---

## Features

- **Multi-client streaming** – InnerTube engine plays from YouTube Music with automatic client fallback (Android, Web, iOS).
- **Age-gate bypass** – embedded `yt-dlp` + QuickJS runtime resolves restricted and geo-blocked tracks in parallel.
- **Zero-gap playback** – stream URL caching and prefetching eliminate latency between tracks.
- **Live lyrics** – synchronized LRC lyric tracking with automatic centering, verse jumping, and on-the-fly translation.
- **Ambient theming** – real-time accent extraction from album artwork drives a hardware-accelerated, dark-first **Material 3** + glassmorphism UI.
- **Native integration** – Jetpack Media3 `ExoPlayer` with lock screen controls on Android; `MPRIS`, `SMTC`, and system media keys on desktop.
- **Offline mode** – download manager with persistent metadata and a local library scanner that parses tags and embedded covers (MP3, FLAC, M4A, and more).
- **Scrobbling & RPC** – Last.fm scrobbling (`melo scrobble`) and Discord Rich Presence (`melo rpc`) from the TUI.

---

## Downloads

Pre-built binaries and packages for **v2.1.5** are available on the [GitHub Releases](https://github.com/Adriianh/Melo/releases/latest) page:

| Platform | Format | Package |
|---|---|---|
| **Android** | APK | `composeApp-release.apk` |
| **Linux — GUI** | AppImage / Debian | `Melo-2.1.5-x86_64.AppImage` • `melo_2.1.5_amd64.deb` |
| **Linux — TUI** | Tarball | `melo-2.1.5-linux-x64.tar.gz` |
| **Windows — GUI** | Setup / MSI | `Melo-Setup.exe` • `Melo-2.1.5.msi` |
| **Windows — TUI** | Zip | `melo-2.1.5-windows.zip` |
| **macOS — GUI** | DMG | `Melo-2.1.5.dmg` |
| **macOS — TUI** | Tarball | `melo-2.1.5-macos.tar.gz` |

Nightly development builds of the TUI are also published for every push to `master` — see [Installation](#installation) for how to install them.

> The TUI and CLI use the same binary. The tarball ships a `melo` native binary (GraalVM), shared-library bindings, and the installer scripts.

---

## Installation

### Terminal TUI & CLI (One-Line Installer)

**Linux & macOS**:

```bash
curl -fsSL https://raw.githubusercontent.com/Adriianh/Melo/master/scripts/install.sh | bash
```

**Windows (PowerShell)**:

```powershell
irm https://raw.githubusercontent.com/Adriianh/Melo/master/scripts/install.ps1 | iex
```

The installer places:

- `~/.local/share/melo-tui/` — native binary and shared libraries
- `~/.local/bin/melo` — unified launcher (see [launcher behavior](#terminal-tui--cli))
- `~/.local/bin/melo-tui` and `~/.local/bin/melo-cli` — explicit TUI launchers
- `~/.config/melo/` — configuration directory with a `.env` template

#### Installer Options

| Option | Description |
|---|---|
| `--nightly` | Install the latest nightly development build from `master` |
| `--desktop` | Create a Linux desktop menu entry (`~/.local/share/applications/melo-tui.desktop`) |
| `--no-desktop` | Skip the menu entry even when `MELO_CREATE_DESKTOP=1` |
| `-v, --version <VERSION>` | Install a specific version (e.g. `2.1.0`) |
| `MELO_VERSION` | Environment variable equivalent to `--version` |
| `MELO_INSTALL_DIR` | Custom install directory (default: `~/.local/share/melo-tui`) |
| `MELO_BIN_DIR` | Custom bin directory (default: `~/.local/bin`) |
| `MELO_CONFIG_DIR` | Custom config directory (default: `~/.config/melo`) |
| `MELO_CREATE_DESKTOP=1` | Environment variable equivalent to `--desktop` |

**Nightly (Linux & macOS)**:

```bash
curl -fsSL https://raw.githubusercontent.com/Adriianh/Melo/master/scripts/install.sh | bash -s -- --nightly
```

**Nightly (Windows)**:

```powershell
$env:MELO_VERSION="nightly"; irm https://raw.githubusercontent.com/Adriianh/Melo/master/scripts/install.ps1 | iex
```

#### Updating & Uninstalling

- **Update**: re-run the install command at any time — it upgrades to the latest release in place.
- **Uninstall**:
  - Linux & macOS: `curl -fsSL https://raw.githubusercontent.com/Adriianh/Melo/master/scripts/uninstall.sh | bash`
  - Windows: `irm https://raw.githubusercontent.com/Adriianh/Melo/master/scripts/uninstall.ps1 | iex`

#### Configuration (`.env`)

After installing, add your API keys to `~/.config/melo/.env` and restart the terminal:

```env
# Last.fm API key (used for music discovery and scrobbling)
# Get yours at: https://www.last.fm/api/account/create
LASTFM_API_KEY=

# Spotify credentials (optional, improves search results)
# Get yours at: https://developer.spotify.com/dashboard
SPOTIFY_CLIENT_ID=
SPOTIFY_CLIENT_SECRET=
```

### GUI (Android & Desktop)

- **Android**: install `composeApp-release.apk` from [Releases](https://github.com/Adriianh/Melo/releases/latest).
- **Desktop**:
  - **Linux**: AppImage (portable, no install needed) or Debian package (`sudo apt install ./melo_2.1.5_amd64.deb`).
  - **Windows**: run `Melo-Setup.exe` or deploy the MSI (GPO-friendly).
  - **macOS**: open the DMG and drag Melo to Applications.
  - **Arch Linux (AUR)**: `yay -S melo-bin`.

The GUI's `.desktop` entry includes an "Open Terminal UI (TUI)" action, so installing the GUI also gives you quick access to the TUI.

---

## Terminal TUI & CLI

### The Unified `melo` Launcher

The installer provides a single `melo` command that dispatches based on context:

| Invocation | Behavior |
|---|---|
| `melo` | No arguments: launches the **TUI** from an interactive terminal; if the GUI is also installed and you launch it from a desktop menu, it opens the **GUI** |
| `melo --tui` / `melo -t` | Force the TUI |
| `melo --gui` / `melo -g` | Force the GUI (requires the Desktop app) |
| `melo <subcommand>` | Any CLI subcommand is delegated to the TUI binary |

`melo-tui` and `melo-cli` are direct launchers for the terminal app if you want to bypass the unified dispatcher.

### Basic Usage

```bash
# Launch the interactive terminal UI
melo

# Headless daemon playback
melo daemon start
melo play "Never Gonna Give You Up"
melo pause
melo resume
melo next
melo stop

# Queue & library management
melo queue list
melo queue add "Bohemian Rhapsody"
melo queue clear
melo local scan
melo local list
```

### Command Reference

| Group | Command | Description |
|---|---|---|
| **Playback** | `play` | Play a track or search and play on the fly |
| | `pause` / `resume` | Pause or resume playback (daemon) |
| | `next` / `prev` | Skip to the next or previous track |
| | `stop` | Stop playback |
| | `queue` | Manage the queue (`add`, `list`, `remove`, `clear`) |
| | `radio` | Start a radio station from a track |
| | `playlist` | Manage playlists |
| | `status` | Show current playback and daemon status |
| **Search & Discovery** | `search` | Unified search for tracks, artists, albums, and playlists |
| | `discover` | Home, trending, and explore feeds (`discover home`, `discover trending`, `discover explore`) |
| | `history` | Show listening history |
| **Local & Offline** | `local` | Scan and manage your local library (`local scan`, `local list`) |
| | `download` | Download tracks for offline playback |
| | `tag` | Read or edit track metadata |
| | `lyrics` | Show synchronized LRC lyrics for the current track |
| **Daemon & Session** | `daemon` | Background playback daemon (`start`, `stop`, `status`, `run`) |
| | `stats` | Playback statistics |
| **Auth & Services** | `auth` | Authenticate external services (`youtube`, `lastfm`) |
| | `scrobble` | Manage Last.fm scrobbling |
| | `rpc` | Toggle Discord Rich Presence |
| | `share` | Share the current track |
| **Configuration** | `config` | View or set configuration options |

Run `melo <command> --help` for details on any command.

### TUI Previews

<p align="center">
  <img src="assets/preview/home_screen.png" alt="Melo Home Screen" width="370">
  <img src="assets/preview/song_screen.png" alt="Melo Song Screen" width="370">
</p>
<p align="center">
  <img src="assets/preview/search_screen.png" alt="Melo Search Screen" width="370">
  <img src="assets/preview/stats_screen.png" alt="Melo Stats Screen" width="370">
</p>

---

## GUI Experience

Melo's graphical client is built from the ground up using **Compose Multiplatform**:

- **Home Feed**: Instant frame-1 load from offline cache with algorithmic sections (Charts, Trending, Playlists, Albums, Artists, and Speed Dial).
- **Now Playing**: Full-screen and docked modes with album art, interactive queue, lyric view, and audio controls.
- **Search**: Instant suggestions, search history, and unified queries for tracks, artists, albums, and playlists.
- **Library**: Custom playlists (create, rename, reorder, delete), favorites ("Me Gusta"), and play history.

---

## Building from Source

### Prerequisites
- **JDK 21+** (e.g., Eclipse Temurin 21)
- Android SDK (for building the Android APK)

```bash
# Clone the repository
git clone https://github.com/Adriianh/Melo.git
cd Melo

# Run the Desktop client
./gradlew :composeApp:run

# Build the Android release APK
./gradlew :composeApp:assembleRelease

# Package Desktop distribution for your current OS
./gradlew :composeApp:packageReleaseDistributionForCurrentOS

# Build the Terminal native binary
./gradlew :tui:nativeCompile
```

Prefer pre-built development builds? Install the [nightly TUI](#installation) instead of building from source.

---

## Tech Stack

- **UI Framework**: [Compose Multiplatform](https://www.jetbrains.com/lp/compose-multiplatform/) (Android & Desktop)
- **Core Engine**: [Kotlin Multiplatform (KMP)](https://kotlinlang.org/docs/multiplatform.html)
- **Dependency Injection**: [Koin](https://insert-koin.io/)
- **Networking**: [Ktor Client 3.x](https://ktor.io/) & Kotlinx Serialization
- **Image Pipeline**: [Coil 3](https://coil-kt.github.io/coil/) (memory & disk caching, hardware crossfade)
- **Audio Players**:
  - Android: [AndroidX Media3 ExoPlayer](https://developer.android.com/media/media3)
  - Desktop: Native / Java Sound with [JMTC](https://github.com/selemba1000/JavaMediaTransportControls)
- **Terminal UI**: [TamboUI](https://github.com/tamboui/tamboui), [Mordant](https://github.com/ajalt/mordant) & [Clikt](https://github.com/ajalt/clikt)

---

## Roadmap

### Released

| Version | Highlights |
|---|---|
| **v2.1.0** | TUI redesign, unified core & modular architecture |
| **v2.0.0** | Compose Multiplatform client (Android, Linux, Windows, macOS), synchronized LRC lyrics & live translation, offline downloads & local media scanner |

### Up Next

- [ ] ListenBrainz scrobbling
- [ ] Parametric DSP equalizer
- [ ] Listen Together (synchronized room playback)
- [ ] Cloud synchronization of playlists and history

---

## Contributing

Contributions are welcome! Please follow the [Conventional Commits](https://www.conventionalcommits.org/) style and ensure your code is tested:

```bash
# Run all unit tests
./gradlew test :composeApp:jvmTest
```

---

## License

Melo is licensed under the **GNU General Public License v3.0 (GPLv3)**. See [LICENSE](LICENSE) for details.
