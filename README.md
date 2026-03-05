<p align="center">
  <img src="assets/logo.png" alt="Melo Logo" width="400">
</p>

<h1 align="center">Melo</h1>

<p align="center">
  <strong>A modern, powerful, and cross-platform music player.</strong>
</p>

<p align="center">
  <img src="https://img.shields.io/github/actions/workflow/status/Adriianh/Melo/release.yml?style=for-the-badge" alt="Build Status">
  <img src="https://img.shields.io/badge/Java-21-orange?style=for-the-badge&logo=openjdk" alt="Java Version">
  <img src="https://img.shields.io/badge/Gradle-9.1.0-blue?style=for-the-badge&logo=gradle" alt="Gradle Version">
</p>

---

## 🎶 About Melo

Melo is a music player designed for speed and versatility. While it currently offers a powerful and intuitive Command-Line Interface (CLI), it is architected with a future-proof mindset to support graphical interfaces across multiple platforms. Melo integrates with Spotify, iTunes, Piped, and Last.fm to provide a unified listening and discovery experience.

## ✨ Features

- 🚀 **Intuitive TUI**: Full control from your terminal with a rich, keyboard-driven interface.
- 🔍 **Multi-source Search**: Discover tracks across iTunes and Piped simultaneously, with deduplication.
- 🎵 **Queue System**: Build a playback queue, shuffle, and cycle through repeat modes (off / all / one).
- 📻 **Radio Auto-play**: When your queue runs out, Melo automatically fetches similar tracks via Last.fm and keeps playing — just like Spotify Radio.
- ⏩ **Seek**: Jump forward or backward within a track using keyboard shortcuts.
- 🎧 **Direct Streaming**: High-quality audio streaming powered by Piped + yt-dlp + ffplay.
- 🖼️ **Artwork & Lyrics**: Album art rendered inline in the terminal; lyrics fetched on demand.
- ❤️ **Library**: Save your favourite tracks and access them from the Library screen.
- 📦 **Cross-platform**: Runs on any system with Java 21+ (**Windows**, **macOS**, **Linux**).

## 🛠️ Tech Stack

- **Language**: [Kotlin](https://kotlinlang.org/) (JVM)
- **Build System**: [Gradle 9.0.0](https://gradle.org/) with Kotlin DSL
- **Architecture**: Clean Architecture (modules: `cli`, `core`, `data`)
- **CLI Framework**: [Clikt](https://ajalt.github.io/clikt/) · [TamboUI](https://tamboui.dev/)
- **Dependency Injection**: [Koin](https://insert-koin.io/)
- **Audio**: [yt-dlp](https://github.com/yt-dlp/yt-dlp) + [ffplay](https://ffmpeg.org/)
- **Data sources**: Spotify API · iTunes Search API · Piped · Last.fm

## 🎹 Keyboard Shortcuts

| Key | Action |
|-----|--------|
| `Enter` | Play selected track |
| `Space` | Play / Pause |
| `←` / `→` | Seek ±5% within track |
| `p` / `n` | Previous / Next track in queue |
| `q` | Add track to queue / Toggle queue panel |
| `s` | Toggle shuffle |
| `r` | Cycle repeat mode (off → all → one) |
| `f` | Toggle favourite |
| `+` / `-` | Volume up / down |
| `Tab` | Cycle focus between panels |
| `1` / `2` / `3` | Switch detail tab (Info / Lyrics / Similar) |
| `Del` / `d` | Remove track from queue |
| `c` | Clear queue |

## 🚀 Getting Started

### Prerequisites
- **Java 21** or higher — [Download Temurin](https://adoptium.net/)
- **yt-dlp** — [Install guide](https://github.com/yt-dlp/yt-dlp#installation)
- **ffmpeg** (includes ffplay) — [Download](https://ffmpeg.org/download.html)

### Installation from release

Download the archive for your platform from the [latest release](https://github.com/Adriianh/Melo/releases/latest) and run the installer:

**Linux / macOS**
```bash
tar -xzf melo-*-linux.tar.gz   # or macos
cd melo-*/
chmod +x install.sh
./install.sh
# Then add ~/.local/bin to your PATH if it isn't already:
# export PATH="$HOME/.local/bin:$PATH"   # add this to ~/.bashrc or ~/.zshrc
```

**Windows** (PowerShell)
```powershell
Expand-Archive melo-*-windows.zip
cd melo-*\
.\install.ps1
# Open a new terminal — melo is now on your PATH
```

After installation, run:
```
melo
```

### Uninstall

**Linux / macOS**
```bash
cd melo-*/
./uninstall.sh
```

**Windows** (PowerShell)
```powershell
cd melo-*\
.\uninstall.ps1
```

---

### Building from source

1. Clone the repository:
   ```bash
   git clone https://github.com/Adriianh/Melo.git
   cd Melo
   ```
2. Build the fat-JAR:
   ```bash
   ./gradlew :cli:shadowJar
   # Output: cli/build/libs/melo.jar
   ```
3. Run directly:
   ```bash
   java -jar cli/build/libs/melo.jar
   ```
4. Or build a distributable archive for your current OS:
   ```bash
   ./gradlew :cli:dist
   # Output: cli/build/dist/melo-*-<os>.tar.gz (or .zip on Windows)
   ```

## 📈 Roadmap

- [x] Core structure and Clean Architecture setup.
- [x] API integration with Spotify, iTunes, Piped and Last.fm.
- [x] Automated cross-platform distribution (fat-JAR + shell wrappers).
- [x] Full TUI with search, library, home screen and detail panels.
- [x] Audio playback with queue, shuffle, repeat and seek.
- [x] Spotify-style radio auto-play via Last.fm similar tracks.
- [ ] GUI implementation using Compose for Desktop.
- [ ] Mobile support via Compose Multiplatform.
- [ ] Local library and playlist management.

## 📄 License

This project is licensed under the MIT License. See the [LICENSE](LICENSE) file for details.

---
<p align="center">Handcrafted with ❤️ by <a href="https://github.com/Adriianh">Adriianh</a></p>