#!/usr/bin/env sh
set -e

INSTALL_DIR="${MELO_INSTALL_DIR:-$HOME/.local/share/melo}"
BIN_DIR="${MELO_BIN_DIR:-$HOME/.local/bin}"
CONFIG_DIR="${MELO_CONFIG_DIR:-$HOME/.config/melo}"

rm -rf "$INSTALL_DIR"
rm -f  "$BIN_DIR/melo"

# Ask before removing config so users don't lose their API keys
if [ -d "$CONFIG_DIR" ]; then
    printf "Remove config directory %s? [y/N] " "$CONFIG_DIR"
    read -r answer
    case "$answer" in
        [yY]*) rm -rf "$CONFIG_DIR" && echo "✓ Config removed." ;;
        *)     echo "  Config kept at $CONFIG_DIR" ;;
    esac
fi

echo "✓ Melo native binary uninstalled."
