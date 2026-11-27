#!/bin/sh
# Replaces the AppImage Melo is running from.
#
# Melo writes this script next to a downloaded AppImage, launches it detached and
# exits immediately. The script waits for the old instance to disappear, swaps the
# image atomically and starts the new one.
#
# Arguments:
#   1  PID of the Melo instance being replaced
#   2  AppImage path being replaced (the one currently mounted)
#   3  Downloaded AppImage to move into place
set -u

PARENT_PID="$1"
TARGET="$2"
STAGED="$3"

restart() {
    if [ -x "$TARGET" ]; then
        echo "relaunching AppImage: $TARGET"
        cd "${HOME:-/}" 2>/dev/null || true
        unset APPIMAGE APPDIR ARGV0 OWD
        if command -v setsid >/dev/null 2>&1; then
            setsid "$TARGET" </dev/null >/dev/null 2>&1 &
        else
            nohup "$TARGET" </dev/null >/dev/null 2>&1 &
        fi
    else
        echo "cannot relaunch AppImage (not executable): $TARGET"
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

if [ ! -f "$STAGED" ]; then
    echo "downloaded AppImage not found: $STAGED"
    restart
    exit 1
fi

# Copy next to the target first so the final rename stays atomic.
if ! cp -f "$STAGED" "$TARGET.new"; then
    echo "cannot write next to $TARGET"
    restart
    exit 1
fi

chmod +x "$TARGET.new" || true

if ! mv -f "$TARGET.new" "$TARGET"; then
    echo "cannot replace $TARGET"
    rm -f "$TARGET.new"
    restart
    exit 1
fi

rm -f "$STAGED"
restart
