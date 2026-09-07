#!/usr/bin/env bash
# =====================================================================
# Melo GUI Desktop Installer for Linux
# Supports both user-level (~/.local) and system-wide (/opt) installs
# =====================================================================

set -e

GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

echo -e "${BLUE}=== Melo Music Player (GUI) — Linux Installer ===${NC}"

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
APP_SOURCE=""

if [ -d "$SCRIPT_DIR/app/Melo" ]; then
    APP_SOURCE="$SCRIPT_DIR/app/Melo"
elif [ -d "$SCRIPT_DIR/bin" ] && [ -f "$SCRIPT_DIR/bin/Melo" ]; then
    APP_SOURCE="$SCRIPT_DIR"
elif [ -d "$SCRIPT_DIR/../../composeApp/build/compose/binaries/main/app/Melo" ]; then
    APP_SOURCE="$(cd "$SCRIPT_DIR/../../composeApp/build/compose/binaries/main/app/Melo" && pwd)"
fi

if [ -z "$APP_SOURCE" ] || [ ! -f "$APP_SOURCE/bin/Melo" ]; then
    echo -e "${RED}Error: Melo GUI payload not found in $SCRIPT_DIR.${NC}"
    echo "Make sure you have built the application (./gradlew :composeApp:createDistributable)"
    echo "or run this script from inside the extracted release archive."
    exit 1
fi

ICON_SOURCE=""
DESKTOP_SOURCE=""

if [ -f "$SCRIPT_DIR/melo.desktop" ]; then
    DESKTOP_SOURCE="$SCRIPT_DIR/melo.desktop"
elif [ -f "$SCRIPT_DIR/packaging/linux/melo.desktop" ]; then
    DESKTOP_SOURCE="$SCRIPT_DIR/packaging/linux/melo.desktop"
fi

if [ -f "$SCRIPT_DIR/../windows/assets/wizard_small.bmp" ] && [ -f "$SCRIPT_DIR/../../composeApp/src/jvmMain/resources/icons/icon.png" ]; then
    ICON_SOURCE="$SCRIPT_DIR/../../composeApp/src/jvmMain/resources/icons/icon.png"
elif [ -f "$SCRIPT_DIR/icon.png" ]; then
    ICON_SOURCE="$SCRIPT_DIR/icon.png"
elif [ -f "$APP_SOURCE/lib/Melo.png" ]; then
    ICON_SOURCE="$APP_SOURCE/lib/Melo.png"
fi

if [ "$EUID" -eq 0 ]; then
    INSTALL_DIR="/opt/melo-gui"
    BIN_DIR="/usr/local/bin"
    DESKTOP_DIR="/usr/share/applications"
    ICON_DIR="/usr/share/icons/hicolor/512x512/apps"
    echo -e "${YELLOW}Installation scope: System-wide (${INSTALL_DIR})${NC}"
else
    INSTALL_DIR="${XDG_DATA_HOME:-$HOME/.local/share}/melo-gui"
    BIN_DIR="$HOME/.local/bin"
    DESKTOP_DIR="${XDG_DATA_HOME:-$HOME/.local/share}/applications"
    ICON_DIR="${XDG_DATA_HOME:-$HOME/.local/share}/icons/hicolor/512x512/apps"
    echo -e "${YELLOW}Installation scope: User (${INSTALL_DIR})${NC}"
fi

mkdir -p "$INSTALL_DIR"
mkdir -p "$BIN_DIR"
mkdir -p "$DESKTOP_DIR"
mkdir -p "$ICON_DIR"

echo "Copying Melo GUI files to $INSTALL_DIR..."
cp -rf "$APP_SOURCE"/* "$INSTALL_DIR/"
chmod +x "$INSTALL_DIR/bin/Melo"
if [ -f "$INSTALL_DIR/melo.sh" ]; then
    chmod +x "$INSTALL_DIR/melo.sh"
fi

echo "Creating dedicated GUI launcher at $BIN_DIR/melo-gui..."
cat << 'EOF' > "$BIN_DIR/melo-gui"
#!/bin/sh
export MALLOC_ARENA_MAX=2
EXEC_TARGET="%INSTALL_DIR%/bin/Melo"
if [ -f "%INSTALL_DIR%/melo.sh" ]; then
    EXEC_TARGET="%INSTALL_DIR%/melo.sh"
fi
exec "$EXEC_TARGET" "$@"
EOF
sed -i "s|%INSTALL_DIR%|$INSTALL_DIR|g" "$BIN_DIR/melo-gui"
chmod +x "$BIN_DIR/melo-gui"

echo "Setting up unified smart launcher at $BIN_DIR/melo..."
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
elif [ -x "/opt/melo/bin/Melo" ]; then
    GUI_BIN="/opt/melo/bin/Melo"
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

if [ -n "$ICON_SOURCE" ] && [ -f "$ICON_SOURCE" ]; then
    echo "Installing application icon to $ICON_DIR/melo.png..."
    cp -f "$ICON_SOURCE" "$ICON_DIR/melo.png"
fi

if [ -n "$DESKTOP_SOURCE" ] && [ -f "$DESKTOP_SOURCE" ]; then
    echo "Installing desktop launcher in $DESKTOP_DIR/melo.desktop..."
    cp -f "$DESKTOP_SOURCE" "$DESKTOP_DIR/melo.desktop"
    sed -i "s|^Exec=.*|Exec=$BIN_DIR/melo-gui %U|g" "$DESKTOP_DIR/melo.desktop"
    if [ -f "$ICON_DIR/melo.png" ]; then
        sed -i "s|^Icon=.*|Icon=melo|g" "$DESKTOP_DIR/melo.desktop"
    fi
fi

if command -v update-desktop-database >/dev/null 2>&1; then
    update-desktop-database "$DESKTOP_DIR" 2>/dev/null || true
fi
if command -v gtk-update-icon-cache >/dev/null 2>&1; then
    gtk-update-icon-cache -f -t "$(dirname "$ICON_DIR/../../")" 2>/dev/null || true
fi

if ! ldconfig -p 2>/dev/null | grep -q "libvlc.so"; then
    if [ ! -f "/usr/lib/libvlc.so" ] && [ ! -f "/usr/lib64/libvlc.so" ] && [ ! -f "/usr/lib/x86_64-linux-gnu/libvlc.so" ]; then
        echo -e "${YELLOW}Notice: 'libvlc.so' was not detected on this system.${NC}"
        echo "For audio playback on Linux, please ensure VLC is installed:"
        echo "  - Arch Linux: sudo pacman -S vlc"
        echo "  - Ubuntu/Debian: sudo apt install vlc"
        echo "  - Fedora: sudo dnf install vlc"
    fi
fi

echo -e "${GREEN}Melo GUI installed successfully!${NC}"
echo -e "You can launch the GUI via '${BLUE}melo-gui${NC}', through your app launcher, or run '${BLUE}melo${NC}'."
if [ "$EUID" -ne 0 ]; then
    case ":$PATH:" in
        *":$BIN_DIR:"*) ;;
        *) echo -e "${YELLOW}Note:${NC} $BIN_DIR is not in your \$PATH. Add it to your ~/.bashrc or ~/.zshrc." ;;
    esac
fi