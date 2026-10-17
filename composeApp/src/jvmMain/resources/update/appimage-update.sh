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

# Detach from the dying parent and survive a closing terminal.
trap '' HUP INT TERM

while kill -0 "$PARENT_PID" 2>/dev/null; do
    sleep 0.5
done

if [ ! -f "$STAGED" ]; then
    echo "downloaded AppImage not found: $STAGED"
    exit 1
fi

# Copy next to the target first so the final rename stays atomic.
if ! cp -f "$STAGED" "$TARGET.new"; then
    echo "cannot write next to $TARGET"
    "$TARGET" >/dev/null 2>&1 &
    exit 1
fi

chmod +x "$TARGET.new" || true

if ! mv -f "$TARGET.new" "$TARGET"; then
    echo "cannot replace $TARGET"
    rm -f "$TARGET.new"
    "$TARGET" >/dev/null 2>&1 &
    exit 1
fi

rm -f "$STAGED"
"$TARGET" >/dev/null 2>&1 &
