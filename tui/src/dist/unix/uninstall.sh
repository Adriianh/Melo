#!/usr/bin/env sh
# =====================================================================
# Melo TUI & CLI Uninstaller for Unix (Linux & macOS)
#
# Scope:
#   --user       remove the per-user install (default)
#   --system     remove the system-wide install (requires root)
#   --scope=user|system|all
#   --all        remove every known install (user + system)
#
# Running through sudo removes the system install *and* the invoking
# user's install, so a single `sudo uninstall.sh` cleans both. Anything
# that cannot be removed is reported with the exact command to run.
# =====================================================================
set -e

is_root() { [ "$(id -u 2>/dev/null || echo 1)" -eq 0 ]; }

# Home of the user that actually invoked the script (NOT root under sudo).
effective_home() {
    if is_root && [ -n "${SUDO_USER:-}" ] && [ "${SUDO_USER}" != "root" ]; then
        h="$(eval echo "~${SUDO_USER}" 2>/dev/null || true)"
        case "$h" in
            ""|"~"*|"~${SUDO_USER}") : ;;
            *) printf '%s\n' "$h"; return 0 ;;
        esac
    fi
    printf '%s\n' "${HOME:-/root}"
}

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
            echo "  -y, --yes    Remove configuration without prompt"
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
        printf "Error: invalid scope '%s' (expected user|system|all).\n" "$SCOPE" >&2
        exit 1
        ;;
esac

if [ -t 1 ]; then
    BOLD='\033[1m'
    GREEN='\033[38;5;84m'
    YELLOW='\033[38;5;221m'
    RED='\033[38;5;203m'
    CYAN='\033[38;5;81m'
    NC='\033[0m'
else
    BOLD=''
    GREEN=''
    YELLOW=''
    RED=''
    CYAN=''
    NC=''
fi

USER_HOME="$(effective_home)"
NEED_SUDO=false
TOUCHED_BINS=""

remove_path() {
    p="$1"
    [ -e "$p" ] || [ -L "$p" ] || return 0
    if rm -rf "$p" 2>/dev/null; then
        printf "  ${GREEN}✔${NC} Removed %s\n" "$p"
    else
        printf "  ${YELLOW}⚠${NC} Could not remove %s (permission denied) — re-run with sudo.\n" "$p"
        NEED_SUDO=true
    fi
}

# Remove only the launchers we generate, never an unrelated `melo`.
remove_launchers() {
    bindir="$1"
    remove_path "$bindir/melo-tui"
    remove_path "$bindir/melo-cli"
    if [ -L "$bindir/melo" ]; then
        case "$(readlink "$bindir/melo" 2>/dev/null || true)" in
            *melo*) remove_path "$bindir/melo" ;;
        esac
    elif [ -f "$bindir/melo" ] && grep -q "Melo Unified Smart Launcher" "$bindir/melo" 2>/dev/null; then
        remove_path "$bindir/melo"
    fi
}

process_scope() {
    scope="$1"
    case "$scope" in
        user)
            dir="${MELO_INSTALL_DIR:-$USER_HOME/.local/share/melo-tui}"
            bindir="${MELO_BIN_DIR:-$USER_HOME/.local/bin}"
            ;;
        system)
            dir="${MELO_INSTALL_DIR:-/usr/local/lib/melo-tui}"
            bindir="${MELO_BIN_DIR:-/usr/local/bin}"
            ;;
    esac

    # A marker can record a custom bin dir chosen at install time.
    if [ -f "$dir/.melo-install" ]; then
        recorded_bin="$(sed -n 's/^MELO_BIN_DIR=//p' "$dir/.melo-install" | tail -n 1)"
        if [ -n "$recorded_bin" ]; then bindir="$recorded_bin"; fi
    fi

    printf "\n  ${BOLD}${scope} scope${NC}\n"
    remove_path "$dir"
    remove_launchers "$bindir"
    case " $TOUCHED_BINS " in
        *" $bindir "*) : ;;
        *) TOUCHED_BINS="$TOUCHED_BINS $bindir" ;;
    esac
}

printf "\n  ${BOLD}${RED}Uninstalling Melo TUI...${NC}\n"

for scope in $SCOPES; do
    process_scope "$scope"
done

# Remove the optional desktop menu entry from every known location.
for desktop in \
    "${XDG_DATA_HOME:-$USER_HOME/.local/share}/applications/melo-tui.desktop" \
    "/usr/share/applications/melo-tui.desktop"
do
    if [ -f "$desktop" ]; then
        remove_path "$desktop"
        if command -v update-desktop-database >/dev/null 2>&1; then
            update-desktop-database "$(dirname "$desktop")" >/dev/null 2>&1 || true
        fi
    fi
done

# Preserve the unified launcher if a Melo GUI is still installed.
for bindir in $TOUCHED_BINS; do
    if [ -x "$bindir/melo-gui" ] || [ -d "$USER_HOME/.local/share/melo-gui" ] || [ -d "/opt/melo-gui" ]; then
        printf "  ${CYAN}ℹ${NC} Melo GUI is still installed; preserving %s/melo.\n" "$bindir"
        if [ -x "$bindir/melo-gui" ]; then
            ln -sf "melo-gui" "$bindir/melo" 2>/dev/null || true
        fi
    fi
done

# Configuration directory prompt (always the invoking user's).
CONFIG_DIR="${MELO_CONFIG_DIR:-$USER_HOME/.config/melo}"
if [ -d "$CONFIG_DIR" ]; then
    remove_config=false
    if [ "$AUTO_CONFIRM" = true ]; then
        remove_config=true
    else
        printf "\n  ${YELLOW}?${NC} Remove user configuration & cache at ${BOLD}%s${NC}? [y/N]: " "$CONFIG_DIR"
        read -r answer 2>/dev/null < /dev/tty || read -r answer || answer="n"
        case "$answer" in
            [yY]*) remove_config=true ;;
            *)     remove_config=false ;;
        esac
    fi

    if [ "$remove_config" = true ]; then
        remove_path "$CONFIG_DIR"
    else
        printf "  ${CYAN}ℹ${NC} Configuration preserved at %s\n" "$CONFIG_DIR"
    fi
fi

if [ "$NEED_SUDO" = true ]; then
    printf "\n  ${YELLOW}⚠${NC} Some files could not be removed. Re-run with sudo:\n"
    printf "      sudo sh %s --all\n" "$0"
fi

printf "\n  ${GREEN}✔${NC} Melo TUI uninstalled successfully.\n\n"
