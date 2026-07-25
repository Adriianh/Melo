<h1 align="center">Melo</h1>

<p align="center">
  <strong>Modern, fast, and cross-platform music player</strong>
</p>

<p align="center">
  <img src="https://img.shields.io/github/actions/workflow/status/Adriianh/Melo/release.yml?style=for-the-badge" alt="Build Status">
  <img src="https://img.shields.io/github/v/release/Adriianh/Melo?style=for-the-badge" alt="Latest Release">
  <img src="https://img.shields.io/badge/Java-21+-orange?style=for-the-badge&logo=openjdk" alt="Java Version">
  <img src="https://img.shields.io/badge/Gradle-9.x-blue?style=for-the-badge&logo=gradle" alt="Gradle Version">
  <img src="https://img.shields.io/github/license/Adriianh/Melo?style=for-the-badge" alt="License">
</p>

---

Melo is a versatile music player designed for efficiency and aesthetics. It provides a seamless experience by combining a rich **Terminal User Interface (TUI)** for immersive listening with a robust **Command Line Interface (CLI)** and background **Daemon** for lightweight control.

## 🚀 Dual Experience

Melo adapts to your workflow with two primary modes of operation:

- **Interactive TUI**: A beautiful, keyboard-driven interface to browse, search, and manage your library. Best for discovery and focused listening. Simply run `melo`.
- **Powerful CLI**: Control playback, manage your queue, or fetch track info directly from your shell without opening the full interface. Run `melo <command>` (e.g., `melo play`, `melo next`).

---

## 🕹️ Command Cheat Sheet

### Playback & Daemon
- `melo daemon start` — Start the background engine.
- `melo play "query"` — Search and play a track immediately.
- `melo pause` / `melo resume` — Control the active stream.
- `melo status` — Check what's playing and daemon health.

### Library & Queue
- `melo queue list` — View upcoming tracks.
- `melo queue add "query"` — Add a specific track to the queue remotely.
- `melo local scan` — Update your local music library.
- `melo share` — Get the YouTube Music link for the current track.
- `melo tag` — Interactively edit metadata for local files.

---

## 🖼️ Demo

<p align="center">
  <img src="assets/preview/home_screen.png" alt="Melo Home Screen" width="350">
  <img src="assets/preview/search_screen.png" alt="Melo Search Screen" width="350">
  <img src="assets/preview/song_screen.png" alt="Melo Song Screen" width="350">
  <img src="assets/preview/stats_screen.png" alt="Melo Stats Screen" width="350">
</p>
<p align="center">
  <em>Home</em> &nbsp; | &nbsp; <em>Search</em> &nbsp; | &nbsp; <em>Song</em> &nbsp; | &nbsp; <em>Stats</em>
</p>

---

## ✨ Features

- **Background Daemon**: Play music persistently in the background using `melo daemon start`.
- **Unified Search**: Access multiple streaming sources and your local library in a single view.
- **Remote Control**: Manage playback from any terminal with commands like `melo pause`, `next`, or `queue list`.
- **Social Integration**: Real-time Discord Rich Presence and Last.fm scrobbling support.
- **Offline & Library**: Download tracks for offline use and manage local file metadata (`melo tag`).
- **Smart Discovery**: Automatic radio mode and intelligent recommendations based on your taste.
- **Aesthetic TUI**: Modern design with inline artwork previews and synced lyrics.

---

## 🛠️ Quick Start

1. **Download** the latest release for your platform from [GitHub Releases](https://github.com/Adriianh/Melo/releases/latest).
2. **Install** using the provided script (e.g., `./install.sh` on Linux/macOS).
3. **Configure** your API keys in the generated `.env` file or via `melo config`.
4. **Run** `melo` to enter the TUI or `melo daemon start` to play in the background.

---

## ⚙️ Configuration

Configuration files are located in your user config directory (e.g., `~/.config/melo`). You can manage settings via the `.env` file or the CLI:

```bash
melo config set <key> <value>      # Update a setting
melo config list                  # View all settings
melo config auth <provider>       # Authenticate with services (e.g., Spotify, Last.fm)
```

For more details, run `melo --help`.

---

## 📦 Installation

### Prerequisites
- **Java 21+** (for building or running the JAR)
- **[yt-dlp](https://github.com/yt-dlp/yt-dlp)** and **[ffmpeg](https://ffmpeg.org/)** (required for audio streaming)

### From Source
1. Clone the repository: `git clone https://github.com/Adriianh/Melo.git`
2. Build the native binary: `./gradlew :tui:nativeCompile`
   - *Output: `tui/build/native/nativeCompile/melo`*

---

## 🗺️ Roadmap & Vision

Melo is evolving beyond the terminal. Our goal is to provide a unified music experience across all your devices while maintaining the speed and simplicity you love.

- [ ] **Melo Desktop**: A native cross-platform GUI using Compose Multiplatform.
- [ ] **Melo Mobile**: Android and iOS support to take your library anywhere.
- [ ] **Listen Together**: Real-time synchronized playback with friends.
- [ ] **Cloud Sync**: Sync your history, favorites, and settings across all platforms.

---

## 🤝 Contributing

Contributions are welcome! Please follow the [Conventional Commits](https://www.conventionalcommits.org/) style and ensure your code is clean and tested. Check out our [Contribution Guidelines](.github/git-commit-instructions.md) for more details.

---

## 📄 License

Melo is licensed under the **GNU General Public License v3.0 (GPLv3)**. See [LICENSE](LICENSE) for details.
