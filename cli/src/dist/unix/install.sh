#!/usr/bin/env sh
set -e

INSTALL_DIR="${MELO_INSTALL_DIR:-$HOME/.local/share/melo}"
BIN_DIR="${MELO_BIN_DIR:-$HOME/.local/bin}"

mkdir -p "$INSTALL_DIR" "$BIN_DIR"

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
cp "$SCRIPT_DIR/melo.jar" "$INSTALL_DIR/melo.jar"

printf '#!/usr/bin/env sh\nexec java -jar "%s/melo.jar" "$@"\n' "$INSTALL_DIR" > "$BIN_DIR/melo"
chmod +x "$BIN_DIR/melo"

echo ""
echo "✓ Melo installed to $INSTALL_DIR"
echo "✓ Launcher placed at $BIN_DIR/melo"
echo ""
echo "Make sure $BIN_DIR is in your PATH."
echo "  Add to ~/.bashrc or ~/.zshrc:"
echo "    export PATH=\"\$HOME/.local/bin:\$PATH\""
echo ""

