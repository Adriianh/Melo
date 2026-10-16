#!/usr/bin/env bash
# ==============================================================================
# Melo TUI & CLI Uninstaller (Linux & macOS)
#
# Usage:
#   curl -fsSL https://raw.githubusercontent.com/Adriianh/Melo/master/scripts/uninstall.sh | bash
#   bash scripts/uninstall.sh [-y|--yes] [--user|--system|--all]
#
# Scope:
#   --user       Remove the per-user install (default)
#   --system     Remove the system-wide install (requires root)
#   --scope=...  user | system | all
#   --all        Remove every known install (user + system)
#
# Running through sudo removes the system install *and* the invoking user's
# install, so a single `sudo uninstall.sh` cleans both.
# ==============================================================================
set -euo pipefail

is_root() { [ "$(id -u 2>/dev/null || echo 1)" -eq 0 ]; }

# Home of the user that invoked the script (NOT root's home under sudo).
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

if [ -t 1 ] && [ -n "$(tput colors 2>/dev/null || true)" ] && [ "$(tput colors 2>/dev/null || true)" -ge 8 ]; then
    BOLD="\033[1m"
    DIM="\033[2m"
    PURPLE="\033[38;5;141m"
    CYAN="\033[38;5;81m"
    GREEN="\033[38;5;84m"
    YELLOW="\033[38;5;221m"
    RED="\033[38;5;203m"
    RESET="\033[0m"
else
    BOLD=""
    DIM=""
    PURPLE=""
    CYAN=""
    GREEN=""
    YELLOW=""
    RED=""
    RESET=""
fi

log_step()    { printf "  ${PURPLE}●${RESET} %b\n" "$1"; }
log_success() { printf "  ${GREEN}✔${RESET} %b\n" "$1"; }
log_warn()    { printf "  ${YELLOW}⚠${RESET} %b\n" "$1"; }
log_info()    { printf "  ${CYAN}ℹ${RESET} %b\n" "$1"; }

AUTO_CONFIRM=false
SCOPE="${MELO_SCOPE:-auto}"
while [[ $# -gt 0 ]]; do
    case "$1" in
        -y|--yes)
            AUTO_CONFIRM=true
            shift
            ;;
        --user)
            SCOPE=user
            shift
            ;;
        --system)
            SCOPE=system
            shift
            ;;
        --all)
            SCOPE=all
            shift
            ;;
        --scope=*)
            SCOPE="${1#*=}"
            shift
            ;;
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
        *)
            shift
            ;;
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
        log_warn "Invalid scope '${SCOPE}' (expected user|system|all)."
        exit 1
        ;;
esac

USER_HOME="$(effective_home)"
NEED_SUDO=false
TOUCHED_BINS=""

remove_path() {
    local p="$1"
    [ -e "$p" ] || [ -L "$p" ] || return 0
    if rm -rf "$p" 2>/dev/null; then
        log_success "Removed $p"
    else
        log_warn "Could not remove $p (permission denied) — re-run with sudo."
        NEED_SUDO=true
    fi
}

# Remove only the launchers we generate, never an unrelated `melo`.
remove_launchers() {
    local bindir="$1"
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
    local scope="$1" dir bindir recorded_bin
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

    if [ -f "$dir/.melo-install" ]; then
        recorded_bin="$(sed -n 's/^MELO_BIN_DIR=//p' "$dir/.melo-install" | tail -n 1)"
        if [ -n "$recorded_bin" ]; then bindir="$recorded_bin"; fi
    fi

    log_step "${scope} scope: ${dir}"
    remove_path "$dir"
    remove_launchers "$bindir"
    case " $TOUCHED_BINS " in
        *" $bindir "*) : ;;
        *) TOUCHED_BINS="$TOUCHED_BINS $bindir" ;;
    esac
}

printf "\n  ${BOLD}${RED}Uninstalling Melo TUI...${RESET}\n"

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
        log_info "Melo GUI is still installed; preserving ${bindir}/melo."
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
        printf "\n  ${YELLOW}?${RESET} Remove user configuration & cache at ${BOLD}%s${RESET}? [y/N]: " "$CONFIG_DIR"
        read -r answer 2>/dev/null < /dev/tty || read -r answer || answer="n"
        case "$answer" in
            [yY]*) remove_config=true ;;
            *)     remove_config=false ;;
        esac
    fi

    if [ "$remove_config" = true ]; then
        remove_path "$CONFIG_DIR"
    else
        log_info "Configuration preserved at $CONFIG_DIR"
    fi
fi

printf "\n"
if [ "$NEED_SUDO" = true ]; then
    log_warn "Some files could not be removed. Re-run with sudo:"
    printf "      sudo bash %s --all\n\n" "$0"
fi

printf "  ${GREEN}┌────────────────────────────────────────────────────────┐${RESET}\n"
printf "  ${GREEN}│${RESET}  ${BOLD}${GREEN}✔ Melo TUI was successfully uninstalled.${RESET}             ${GREEN}│${RESET}\n"
printf "  ${GREEN}│${RESET}  Thank you for using Melo!                             ${GREEN}│${RESET}\n"
printf "  ${GREEN}└────────────────────────────────────────────────────────┘${RESET}\n\n"
