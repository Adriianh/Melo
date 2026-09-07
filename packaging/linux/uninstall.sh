#!/usr/bin/env bash
# =====================================================================
# Melo GUI Desktop Uninstaller for Linux
# =====================================================================

set -e

GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

echo -e "${BLUE}=== Melo Music Player (GUI) — Linux Uninstaller ===${NC}"

if [ "$EUID" -eq 0 ]; then
    INSTALL_DIR="/opt/melo-gui"
    BIN_LINK="/usr/local/bin/melo-gui"
    UNIFIED_BIN="/usr/local/bin/melo"
    TUI_BIN="/usr/local/bin/melo-tui"
    DESKTOP_FILE="/usr/share/applications/melo.desktop"
    ICON_FILE="/usr/share/icons/hicolor/512x512/apps/melo.png"
else
    INSTALL_DIR="${XDG_DATA_HOME:-$HOME/.local/share}/melo-gui"
    BIN_LINK="$HOME/.local/bin/melo-gui"
    UNIFIED_BIN="$HOME/.local/bin/melo"
    TUI_BIN="$HOME/.local/bin/melo-tui"
    DESKTOP_FILE="${XDG_DATA_HOME:-$HOME/.local/share}/applications/melo.desktop"
    ICON_FILE="${XDG_DATA_HOME:-$HOME/.local/share}/icons/hicolor/512x512/apps/melo.png"
fi

echo "Removing installed Melo GUI files from $INSTALL_DIR..."
rm -rf "$INSTALL_DIR"
rm -rf "$(dirname "$INSTALL_DIR")/melo" 2>/dev/null || true

rm -f "$BIN_LINK"
rm -f "$DESKTOP_FILE"
rm -f "$ICON_FILE"

if [ -x "$TUI_BIN" ]; then
    echo "Melo TUI is still installed. Pointing 'melo' directly to TUI..."
    ln -sf "$TUI_BIN" "$UNIFIED_BIN"
else
    rm -f "$UNIFIED_BIN"
fi

if command -v update-desktop-database >/dev/null 2>&1; then
    update-desktop-database "$(dirname "$DESKTOP_FILE")" 2>/dev/null || true
fi
if command -v gtk-update-icon-cache >/dev/null 2>&1; then
    gtk-update-icon-cache -f -t "$(dirname "$ICON_FILE/../../")" 2>/dev/null || true
fi

read -p "Do you also want to remove Melo database, playback history, and cache? [y/N]: " -r PURGE
if [[ "$PURGE" =~ ^[yY]$ ]]; then
    rm -rf "$HOME/.melo"
    rm -rf "${XDG_CONFIG_HOME:-$HOME/.config}/Melo" "${XDG_CONFIG_HOME:-$HOME/.config}/melo"
    rm -rf "${XDG_CACHE_HOME:-$HOME/.cache}/Melo" "${XDG_CACHE_HOME:-$HOME/.cache}/melo"
    rm -rf "${XDG_DATA_HOME:-$HOME/.local/share}/Melo"
    echo -e "${GREEN}Melo database and cache removed.${NC}"
fi

echo -e "${GREEN}Melo GUI uninstalled successfully.${NC}"