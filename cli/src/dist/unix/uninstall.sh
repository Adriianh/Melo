#!/usr/bin/env sh
set -e

INSTALL_DIR="${MELO_INSTALL_DIR:-$HOME/.local/share/melo}"
BIN_DIR="${MELO_BIN_DIR:-$HOME/.local/bin}"

rm -rf "$INSTALL_DIR"
rm -f  "$BIN_DIR/melo"

echo "✓ Melo uninstalled."

