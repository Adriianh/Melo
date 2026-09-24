#!/usr/bin/env sh
# =====================================================================
# Melo TUI & CLI Uninstaller for Unix (Linux & macOS)
# =====================================================================
set -e

INSTALL_DIR="${MELO_INSTALL_DIR:-$HOME/.local/share/melo-tui}"
BIN_DIR="${MELO_BIN_DIR:-$HOME/.local/bin}"
CONFIG_DIR="${MELO_CONFIG_DIR:-$HOME/.config/melo}"

echo "Removing Melo TUI binaries from $INSTALL_DIR..."
rm -rf "$INSTALL_DIR"
rm -f "$BIN_DIR/melo-tui" "$BIN_DIR/melo-cli"

# Remove the optional TUI desktop menu entry (if present)
DESKTOP_FILE="${XDG_DATA_HOME:-$HOME/.local/share}/applications/melo-tui.desktop"
if [ -f "$DESKTOP_FILE" ]; then
    rm -f "$DESKTOP_FILE"
    echo "✓ Desktop menu entry removed."
    if command -v update-desktop-database >/dev/null 2>&1; then
        update-desktop-database "$(dirname "$DESKTOP_FILE")" >/dev/null 2>&1 || true
    fi
fi

if [ -x "$BIN_DIR/melo-gui" ]; then
    echo "Melo GUI is still installed. Keeping unified 'melo' launcher pointing to GUI."
    ln -sf "melo-gui" "$BIN_DIR/melo"
else
    if [ ! -d "$HOME/.local/share/melo-gui" ] && [ ! -d "$HOME/.local/share/melo" ]; then
        rm -f "$BIN_DIR/melo"
    fi
fi

if [ -d "$CONFIG_DIR" ]; then
    printf "Remove TUI config directory %s? [y/N] " "$CONFIG_DIR"
    read -r answer
    case "$answer" in
        [yY]*) rm -rf "$CONFIG_DIR" && echo "✓ Config removed." ;;
        *)     echo "  Config kept at $CONFIG_DIR" ;;
    esac
fi

echo "✓ Melo TUI uninstalled successfully."
