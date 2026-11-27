#!/bin/sh
# Applies a macOS .app bundle update: replaces the running Melo.app bundle with
# the contents of a downloaded .zip archive once the previous instance exits.
#
# Melo writes this script next to the downloaded archive, launches it detached and
# exits immediately. The script waits for the old instance, unpacks the archive,
# swaps the .app bundle and relaunches Melo. Any failure rolls the previous bundle
# back and relaunches it, so the user is never left without Melo.
#
# Arguments:
#   1  PID of the Melo instance being replaced
#   2  Downloaded .zip archive containing Melo.app
#   3  Target .app bundle path (e.g. /Applications/Melo.app)
set -u

PARENT_PID="$1"
ARCHIVE="$2"
APP_BUNDLE="$3"

PARENT_DIR="$(dirname "$APP_BUNDLE")"
BUNDLE_NAME="$(basename "$APP_BUNDLE")"
UNPACK="$PARENT_DIR/.$BUNDLE_NAME.unpack"
PREVIOUS="$PARENT_DIR/.$BUNDLE_NAME.previous"

restart() {
    if [ -d "$APP_BUNDLE" ]; then
        open -n "$APP_BUNDLE" >/dev/null 2>&1 || open "$APP_BUNDLE" >/dev/null 2>&1 || true
    fi
}

# Detach from the dying parent and survive a closing terminal.
trap '' HUP INT TERM

WAITED=0
while kill -0 "$PARENT_PID" 2>/dev/null; do
    if [ "$WAITED" -ge 120 ]; then
        break
    fi
    sleep 0.5
    WAITED=$((WAITED + 1))
done

if [ ! -f "$ARCHIVE" ]; then
    echo "downloaded macOS update archive not found: $ARCHIVE"
    restart
    exit 1
fi

rm -rf "$UNPACK" "$PREVIOUS"
if ! mkdir -p "$UNPACK"; then
    echo "cannot create staging directory next to $APP_BUNDLE"
    restart
    exit 1
fi

if command -v ditto >/dev/null 2>&1; then
    EXTRACT_OK=0
    ditto -x -k "$ARCHIVE" "$UNPACK" && EXTRACT_OK=1
else
    EXTRACT_OK=0
    unzip -q "$ARCHIVE" -d "$UNPACK" && EXTRACT_OK=1
fi

if [ "$EXTRACT_OK" -ne 1 ]; then
    echo "cannot unpack $ARCHIVE"
    rm -rf "$UNPACK"
    restart
    exit 1
fi

NEW_APP="$(find "$UNPACK" -maxdepth 2 -type d -name '*.app' | head -n 1)"
if [ -z "$NEW_APP" ] || [ ! -x "$NEW_APP/Contents/MacOS/Melo" ]; then
    echo "no valid Melo.app bundle inside $ARCHIVE"
    rm -rf "$UNPACK"
    restart
    exit 1
fi

if mv "$APP_BUNDLE" "$PREVIOUS" && mv "$NEW_APP" "$APP_BUNDLE"; then
    rm -rf "$PREVIOUS" "$UNPACK" "$ARCHIVE"
    xattr -dr com.apple.quarantine "$APP_BUNDLE" 2>/dev/null || true
else
    echo "cannot replace $APP_BUNDLE"
    [ -e "$APP_BUNDLE" ] || mv "$PREVIOUS" "$APP_BUNDLE"
    rm -rf "$UNPACK"
    restart
    exit 1
fi

restart
