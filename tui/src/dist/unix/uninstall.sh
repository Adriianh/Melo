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
