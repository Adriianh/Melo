#!/usr/bin/env bash
# =====================================================================
# Melo GUI Desktop Uninstaller for Linux
#
# Scope:
#   --user       remove the per-user install (default)
#   --system     remove the system-wide install (requires root)
#   --scope=user|system|all
#   --all        remove every known install (user + system)
#
# Running through sudo removes the system install *and* the invoking
# user's install, so a single `sudo uninstall.sh` cleans both.
# =====================================================================

set -e

GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

is_root() { [ "$(id -u 2>/dev/null || echo 1)" -eq 0 ]; }

# Home of the user that actually invoked the script (NOT root under sudo).
effective_home() {
    if is_root && [ -n "${SUDO_USER:-}" ] && [ "${SUDO_USER}" != "root" ]; then
        local h
        h="$(eval echo "~${SUDO_USER}" 2>/dev/null || true)"
        case "$h" in
            ""|"~"*|"~${SUDO_USER}") : ;;
            *) printf '%s\n' "$h"; return 0 ;;
        esac
    fi
    printf '%s\n' "${HOME:-/root}"
}

echo -e "${BLUE}=== Melo Music Player (GUI) — Linux Uninstaller ===${NC}"

AUTO_CONFIRM=false
SCOPE="${MELO_SCOPE:-auto}"
for arg in "$@"; do
    case "$arg" in
        -y|--yes) AUTO_CONFIRM=true ;;
        --user) SCOPE=user ;;
        --system) SCOPE=system ;;
        --all) SCOPE=all ;;
        --scope=*) SCOPE="${arg#*=}" ;;
        -h|--help)
            echo "Usage: uninstall.sh [OPTIONS]"
            echo ""
            echo "Options:"
            echo "  --user       Remove the per-user install (default)"
            echo "  --system     Remove the system-wide install (requires root)"
            echo "  --scope=...  user | system | all"
            echo "  --all        Remove per-user and system installs"
            echo "  -y, --yes    Also purge database/cache without prompting"
            echo "  -h, --help   Show this help message"
            exit 0
            ;;
        *) ;;
    esac
done

case "$SCOPE" in
    auto)
        SCOPES="user"
        if is_root; then SCOPES="user system"; fi
        ;;
    user)   SCOPES="user" ;;
    system) SCOPES="system" ;;
    all)    SCOPES="user system" ;;
    *)
        echo -e "${RED}Error: invalid scope '${SCOPE}' (expected user|system|all).${NC}"
        exit 1
        ;;
esac

USER_HOME="$(effective_home)"
NEED_SUDO=false

remove_path() {
    local p="$1"
    [ -e "$p" ] || [ -L "$p" ] || return 0
    if rm -rf "$p" 2>/dev/null; then
        echo -e "  ${GREEN}✔${NC} Removed $p"
    else
        echo -e "  ${YELLOW}⚠${NC} Could not remove $p (permission denied) — re-run with sudo."
        NEED_SUDO=true
    fi
}

process_scope() {
    local scope="$1" dir bindir desktop icon recorded

    case "$scope" in
        user)
            dir="${MELO_INSTALL_DIR:-$USER_HOME/.local/share/melo-gui}"
            bindir="${MELO_BIN_DIR:-$USER_HOME/.local/bin}"
            desktop="${XDG_DATA_HOME:-$USER_HOME/.local/share}/applications/melo.desktop"
            icon="${XDG_DATA_HOME:-$USER_HOME/.local/share}/icons/hicolor/512x512/apps/melo.png"
            ;;
        system)
            dir="${MELO_INSTALL_DIR:-/opt/melo-gui}"
            bindir="${MELO_BIN_DIR:-/usr/local/bin}"
            desktop="/usr/share/applications/melo.desktop"
            icon="/usr/share/icons/hicolor/512x512/apps/melo.png"
            ;;
    esac

    # A marker can record custom dirs chosen at install time.
    if [ -f "$dir/.melo-install" ]; then
        recorded="$(sed -n 's/^MELO_BIN_DIR=//p' "$dir/.melo-install" | tail -n 1)"
        if [ -n "$recorded" ]; then bindir="$recorded"; fi
        recorded="$(sed -n 's/^MELO_DESKTOP_DIR=//p' "$dir/.melo-install" | tail -n 1)"
        if [ -n "$recorded" ]; then desktop="$recorded/melo.desktop"; fi
        recorded="$(sed -n 's/^MELO_ICON_DIR=//p' "$dir/.melo-install" | tail -n 1)"
        if [ -n "$recorded" ]; then icon="$recorded/melo.png"; fi
    fi

    echo ""
    echo -e "  ${BLUE}${scope} scope${NC}"
    remove_path "$dir"
    # Legacy layout that stored the app directly under <data>/melo.
    remove_path "$(dirname "$dir")/melo"
    remove_path "$bindir/melo-gui"
    remove_path "$desktop"
    remove_path "$icon"

    # Unified launcher: keep it while a TUI launcher still lives here.
    if [ -x "$bindir/melo-tui" ]; then
        echo -e "  ${BLUE}ℹ${NC} Melo TUI is still installed; keeping unified launcher $bindir/melo."
    elif [ -f "$bindir/melo" ] && grep -q "Melo Unified Smart Launcher" "$bindir/melo" 2>/dev/null; then
        remove_path "$bindir/melo"
    fi

    if command -v update-desktop-database >/dev/null 2>&1; then
        update-desktop-database "$(dirname "$desktop")" 2>/dev/null || true
    fi
    if command -v gtk-update-icon-cache >/dev/null 2>&1; then
        gtk-update-icon-cache -f -t "$(dirname "$(dirname "$(dirname "$icon")")")" 2>/dev/null || true
    fi
}

printf "\n"
for scope in $SCOPES; do
    process_scope "$scope"
done

# Optional purge of user data (always the invoking user's).
purge=false
if [ "$AUTO_CONFIRM" = true ]; then
    purge=true
else
    printf "\nDo you also want to remove Melo database, playback history, and cache? [y/N]: "
    read -r answer 2>/dev/null < /dev/tty || read -r answer || answer="n"
    case "$answer" in
        [yY]*) purge=true ;;
    esac
fi

if [ "$purge" = true ]; then
    remove_path "$USER_HOME/.melo"
    remove_path "${XDG_CONFIG_HOME:-$USER_HOME/.config}/Melo"
    remove_path "${XDG_CONFIG_HOME:-$USER_HOME/.config}/melo"
    remove_path "${XDG_CACHE_HOME:-$USER_HOME/.cache}/Melo"
    remove_path "${XDG_CACHE_HOME:-$USER_HOME/.cache}/melo"
    remove_path "${XDG_DATA_HOME:-$USER_HOME/.local/share}/Melo"
    echo -e "${GREEN}Melo database and cache removed.${NC}"
fi

if [ "$NEED_SUDO" = true ]; then
    echo -e "${YELLOW}⚠ Some files could not be removed. Re-run with sudo:${NC}"
    echo "      sudo bash $0 --all"
fi

echo -e "${GREEN}Melo GUI uninstalled successfully.${NC}"
