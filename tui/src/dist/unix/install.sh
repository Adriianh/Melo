#!/usr/bin/env sh
# =====================================================================
# Melo TUI & CLI Installer for Unix (Linux & macOS)
#
# Optional: set MELO_CREATE_DESKTOP=1 or pass --desktop to create a
# Linux desktop menu entry (Linux only).
# =====================================================================
set -e

INSTALL_DIR="${MELO_INSTALL_DIR:-$HOME/.local/share/melo-tui}"
BIN_DIR="${MELO_BIN_DIR:-$HOME/.local/bin}"
CONFIG_DIR="${MELO_CONFIG_DIR:-$HOME/.config/melo}"

CREATE_DESKTOP=false
case "${MELO_CREATE_DESKTOP:-}" in
    1|true|yes|on) CREATE_DESKTOP=true ;;
esac
for arg in "$@"; do
    case "$arg" in
        --desktop) CREATE_DESKTOP=true ;;
        --no-desktop) CREATE_DESKTOP=false ;;
        *) ;;
    esac
done

if [ -t 1 ]; then
    GREEN='\033[38;5;84m'
    YELLOW='\033[38;5;221m'
    CYAN='\033[38;5;81m'
    NC='\033[0m'
else
    GREEN=''
    YELLOW=''
    CYAN=''
    NC=''
fi

mkdir -p "$INSTALL_DIR" "$BIN_DIR" "$CONFIG_DIR"

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"

echo "Installing Melo TUI binary to $INSTALL_DIR..."
cp -f "$SCRIPT_DIR/melo" "$INSTALL_DIR/melo"
chmod +x "$INSTALL_DIR/melo"

# Copy any shared libraries (e.g., AWT .so or .dylib files) if they exist
find "$SCRIPT_DIR" -maxdepth 1 \( -name "*.so" -o -name "*.dylib" \) -exec cp -f {} "$INSTALL_DIR/" \; 2>/dev/null || true

# Copy uninstaller script if present
if [ -f "$SCRIPT_DIR/uninstall.sh" ]; then
    cp -f "$SCRIPT_DIR/uninstall.sh" "$INSTALL_DIR/uninstall.sh"
    chmod +x "$INSTALL_DIR/uninstall.sh"
fi

cat << 'EOF' > "$BIN_DIR/melo-tui"
#!/usr/bin/env sh
INSTALL_DIR="%INSTALL_DIR%"
if [ "$(uname)" = "Darwin" ]; then
  export DYLD_LIBRARY_PATH="$INSTALL_DIR:$DYLD_LIBRARY_PATH"
else
  export LD_LIBRARY_PATH="$INSTALL_DIR:$LD_LIBRARY_PATH"
fi
exec "$INSTALL_DIR/melo" "$@"
EOF
sed -i "s|%INSTALL_DIR%|$INSTALL_DIR|g" "$BIN_DIR/melo-tui" 2>/dev/null || sed -i '' "s|%INSTALL_DIR%|$INSTALL_DIR|g" "$BIN_DIR/melo-tui"
chmod +x "$BIN_DIR/melo-tui"
ln -sf "melo-tui" "$BIN_DIR/melo-cli"

cat << 'EOF' > "$BIN_DIR/melo"
#!/usr/bin/env sh
# Melo Unified Smart Launcher
export MALLOC_ARENA_MAX=2

# Find GUI executable
GUI_BIN=""
if [ -x "$HOME/.local/bin/melo-gui" ]; then
    GUI_BIN="$HOME/.local/bin/melo-gui"
elif [ -x "$HOME/.local/share/melo-gui/bin/Melo" ]; then
    GUI_BIN="$HOME/.local/share/melo-gui/bin/Melo"
elif [ -x "/opt/melo-gui/bin/Melo" ]; then
    GUI_BIN="/opt/melo-gui/bin/Melo"
elif [ -x "/usr/local/bin/melo-gui" ]; then
    GUI_BIN="/usr/local/bin/melo-gui"
elif [ -x "/usr/bin/melo-gui" ]; then
    GUI_BIN="/usr/bin/melo-gui"
fi

# Find TUI executable
TUI_BIN=""
if [ -x "$HOME/.local/bin/melo-tui" ]; then
    TUI_BIN="$HOME/.local/bin/melo-tui"
elif [ -x "$HOME/.local/share/melo-tui/melo" ]; then
    TUI_BIN="$HOME/.local/share/melo-tui/melo"
elif [ -x "/opt/melo-tui/melo" ]; then
    TUI_BIN="/opt/melo-tui/melo"
elif [ -x "/usr/local/bin/melo-tui" ]; then
    TUI_BIN="/usr/local/bin/melo-tui"
elif [ -x "/usr/bin/melo-tui" ]; then
    TUI_BIN="/usr/bin/melo-tui"
fi

# Explicit flags
if [ "$1" = "--gui" ] || [ "$1" = "-g" ]; then
    shift
    if [ -n "$GUI_BIN" ]; then
        exec "$GUI_BIN" "$@"
    else
        echo "Error: Melo GUI is not installed." >&2
        exit 1
    fi
fi

if [ "$1" = "--tui" ] || [ "$1" = "-t" ]; then
    shift
    if [ -n "$TUI_BIN" ]; then
        exec "$TUI_BIN" "$@"
    else
        echo "Error: Melo TUI is not installed." >&2
        exit 1
    fi
fi

# Subcommands check: play, pause, next, prev, stop, resume, search, daemon, status, etc.
if [ $# -gt 0 ]; then
    if [ -n "$TUI_BIN" ]; then
        exec "$TUI_BIN" "$@"
    elif [ -n "$GUI_BIN" ]; then
        exec "$GUI_BIN" "$@"
    else
        echo "Error: Neither Melo GUI nor TUI could be found." >&2
        exit 1
    fi
fi

# No arguments:
# If both installed: interactive TTY -> TUI; non-TTY (graphical launch) -> GUI
if [ -n "$GUI_BIN" ] && [ -n "$TUI_BIN" ]; then
    if [ -t 0 ] && [ -t 1 ]; then
        exec "$TUI_BIN" "$@"
    else
        exec "$GUI_BIN" "$@"
    fi
elif [ -n "$TUI_BIN" ]; then
    exec "$TUI_BIN" "$@"
elif [ -n "$GUI_BIN" ]; then
    exec "$GUI_BIN" "$@"
else
    echo "Error: Neither Melo GUI nor TUI is installed." >&2
    exit 1
fi
EOF
chmod +x "$BIN_DIR/melo"

# Create a .env template only if one doesn't exist yet
if [ ! -f "$CONFIG_DIR/.env" ]; then
    cat > "$CONFIG_DIR/.env" <<'EOF'
# Melo configuration
# Place your API keys here and restart the terminal.

# Last.fm API key (required for music discovery)
# Get yours at: https://www.last.fm/api/account/create
LASTFM_API_KEY=

# Spotify credentials (optional, improves search results)
# Get yours at: https://developer.spotify.com/dashboard
SPOTIFY_CLIENT_ID=
SPOTIFY_CLIENT_SECRET=
EOF
fi

printf "\n"
printf "  ${GREEN}✔${NC} Melo TUI native binary installed to %s\n" "$INSTALL_DIR"
printf "  ${GREEN}✔${NC} Dedicated launcher placed at %s/melo-tui (alias: melo-cli)\n" "$BIN_DIR"
printf "  ${GREEN}✔${NC} Unified smart launcher updated at %s/melo\n" "$BIN_DIR"
printf "  ${GREEN}✔${NC} Config directory created at %s\n" "$CONFIG_DIR"
printf "\n"

# ─── Optional Linux desktop menu entry ─────────────────────────────────────
if [ "$CREATE_DESKTOP" = "true" ]; then
    if [ "$(uname -s)" = "Linux" ]; then
        DESKTOP_DIR="${XDG_DATA_HOME:-$HOME/.local/share}/applications"
        DESKTOP_FILE="$DESKTOP_DIR/melo-tui.desktop"
        EXEC_LINE="Exec=$BIN_DIR/melo-tui"

        # Idempotent: only rewrite when the Exec target changed.
        if [ -f "$DESKTOP_FILE" ] && grep -Fqx "$EXEC_LINE" "$DESKTOP_FILE"; then
            printf "  ${GREEN}✔${NC} Desktop menu entry already up to date: %s\n" "$DESKTOP_FILE"
        else
            mkdir -p "$DESKTOP_DIR"
            cat > "$DESKTOP_FILE" << EOF
[Desktop Entry]
Type=Application
Name=Melo (TUI)
GenericName=Terminal Music Player
Comment=Modern, fast terminal music player (TUI & CLI)
$EXEC_LINE
Icon=melo
Terminal=true
Categories=AudioVideo;Audio;Player;Music;
Keywords=music;player;audio;streaming;terminal;cli;
EOF
            chmod 644 "$DESKTOP_FILE"
            printf "  ${GREEN}✔${NC} Desktop menu entry created: %s\n" "$DESKTOP_FILE"
        fi

        if command -v update-desktop-database >/dev/null 2>&1; then
            update-desktop-database "$DESKTOP_DIR" >/dev/null 2>&1 || true
        fi
    else
        printf "  ${YELLOW}⚠${NC} Desktop menu entry skipped: only supported on Linux.\n"
    fi
fi

echo "Add your API keys to $CONFIG_DIR/.env before running Melo."
echo ""
echo "Make sure $BIN_DIR is in your PATH:"
echo "  export PATH=\"\$HOME/.local/bin:\$PATH\""
echo ""
