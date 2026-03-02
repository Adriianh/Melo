#!/usr/bin/env sh
set -e

INSTALL_DIR="${MELO_INSTALL_DIR:-$HOME/.local/share/melo}"
BIN_DIR="${MELO_BIN_DIR:-$HOME/.local/bin}"
CONFIG_DIR="${MELO_CONFIG_DIR:-$HOME/.config/melo}"

mkdir -p "$INSTALL_DIR" "$BIN_DIR" "$CONFIG_DIR"

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
cp "$SCRIPT_DIR/melo.jar" "$INSTALL_DIR/melo.jar"

printf '#!/usr/bin/env sh\nexec java --enable-native-access=ALL-UNNAMED -jar "%s/melo.jar" "$@"\n' "$INSTALL_DIR" > "$BIN_DIR/melo"
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

echo ""
echo "✓ Melo installed to $INSTALL_DIR"
echo "✓ Launcher placed at $BIN_DIR/melo"
echo "✓ Config directory created at $CONFIG_DIR"
echo ""
echo "Add your API keys to $CONFIG_DIR/.env before running Melo."
echo ""
echo "Make sure $BIN_DIR is in your PATH."
echo "  Add to ~/.bashrc or ~/.zshrc:"
echo "    export PATH=\"\$HOME/.local/bin:\$PATH\""
echo ""
