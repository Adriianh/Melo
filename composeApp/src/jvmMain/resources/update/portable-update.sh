#!/bin/sh
# Applies a portable (install.sh) update: replaces an app-image directory with the
# contents of a downloaded archive once the previous Melo instance is gone.
#
# Melo writes this script next to the downloaded archive, launches it detached and
# exits immediately. The script waits for the old instance, unpacks the archive,
# swaps the install directory and starts the new build. Any failure rolls the
# previous install back and relaunches it, so the user is never left without Melo.
#
# Arguments:
#   1  PID of the Melo instance being replaced
#   2  Downloaded .tar.gz archive
#   3  Install directory to replace (e.g. ~/.local/share/melo-gui)
#   4  Directory holding the launcher script (e.g. ~/.local/bin)
#   5  Launcher name inside that directory (e.g. melo-gui)
set -u

PARENT_PID="$1"
ARCHIVE="$2"
INSTALL_DIR="$3"
BIN_DIR="$4"
LAUNCHER="$5"

UNPACK="$INSTALL_DIR.unpack"
STAGED="$INSTALL_DIR.staged"
PREVIOUS="$INSTALL_DIR.previous"

restart() {
    if [ -x "$BIN_DIR/$LAUNCHER" ]; then
        "$BIN_DIR/$LAUNCHER" >/dev/null 2>&1 &
    elif [ -x "$INSTALL_DIR/bin/Melo" ]; then
        "$INSTALL_DIR/bin/Melo" >/dev/null 2>&1 &
    fi
}

# Detach from the dying parent and survive a closing terminal.
trap '' HUP INT TERM

while kill -0 "$PARENT_PID" 2>/dev/null; do
    sleep 0.5
done

rm -rf "$UNPACK" "$STAGED" "$PREVIOUS"
if ! mkdir -p "$UNPACK" "$STAGED"; then
    echo "cannot create staging directories next to $INSTALL_DIR"
    restart
    exit 1
fi

if ! tar -xzf "$ARCHIVE" -C "$UNPACK"; then
    echo "cannot unpack $ARCHIVE"
    rm -rf "$UNPACK" "$STAGED"
    restart
    exit 1
fi

# The archive holds a single "melo-<version>" directory with bin/ and lib/ inside.
PAYLOAD="$(find "$UNPACK" -maxdepth 4 -type f -name Melo -path '*/bin/Melo' | head -n 1)"
if [ -z "$PAYLOAD" ]; then
    echo "no bin/Melo inside $ARCHIVE"
    rm -rf "$UNPACK" "$STAGED"
    restart
    exit 1
fi
APP_DIR="$(dirname "$(dirname "$PAYLOAD")")"

if ! cp -rf "$APP_DIR"/. "$STAGED"/; then
    echo "cannot stage the new build"
    rm -rf "$UNPACK" "$STAGED"
    restart
    exit 1
fi

# Keep the marker the app uses to locate custom install locations.
if [ -f "$INSTALL_DIR/.melo-install" ]; then
    cp -f "$INSTALL_DIR/.melo-install" "$STAGED/" || true
fi

if mv "$INSTALL_DIR" "$PREVIOUS" && mv "$STAGED" "$INSTALL_DIR"; then
    rm -rf "$PREVIOUS" "$UNPACK" "$ARCHIVE"
else
    echo "cannot replace $INSTALL_DIR"
    [ -e "$INSTALL_DIR" ] || mv "$PREVIOUS" "$INSTALL_DIR"
    rm -rf "$UNPACK" "$STAGED"
    restart
    exit 1
fi

restart
