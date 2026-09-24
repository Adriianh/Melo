#!/usr/bin/env bash
# ==============================================================================
# Melo TUI & CLI Uninstaller (Linux & macOS)
#
# Usage:
#   curl -fsSL https://raw.githubusercontent.com/Adriianh/Melo/master/scripts/uninstall.sh | bash
#   bash scripts/uninstall.sh [-y|--yes]
# ==============================================================================
set -euo pipefail

INSTALL_DIR="${MELO_INSTALL_DIR:-$HOME/.local/share/melo-tui}"
BIN_DIR="${MELO_BIN_DIR:-$HOME/.local/bin}"
CONFIG_DIR="${MELO_CONFIG_DIR:-$HOME/.config/melo}"

if [ -t 1 ] && [ -n "$(tput colors 2>/dev/null || true)" ] && [ "$(tput colors 2>/dev/null || true)" -ge 8 ]; then
    BOLD="\033[1m"
    DIM="\033[2m"
    PURPLE="\033[38;5;141m"
    CYAN="\033[38;5;81m"
    GREEN="\033[38;5;84m"
    YELLOW="\033[38;5;221m"
    RED="\033[38;5;203m"
    GRAY="\033[38;5;245m"
    RESET="\033[0m"
else
    BOLD=""
    DIM=""
    PURPLE=""
    CYAN=""
    GREEN=""
    YELLOW=""
    RED=""
    GRAY=""
    RESET=""
fi

log_step()    { printf "  ${PURPLE}●${RESET} %b\n" "$1"; }
log_success() { printf "  ${GREEN}✔${RESET} %b\n" "$1"; }
log_info()    { printf "  ${CYAN}ℹ${RESET} %b\n" "$1"; }

AUTO_CONFIRM=false
while [[ $# -gt 0 ]]; do
    case "$1" in
        -y|--yes)
            AUTO_CONFIRM=true
            shift
            ;;
        -h|--help)
            echo "Usage: uninstall.sh [OPTIONS]"
            echo ""
            echo "Options:"
            echo "  -y, --yes   Remove configuration without prompt"
            echo "  -h, --help  Show this help message"
            exit 0
            ;;
        *)
            shift
            ;;
    esac
done

printf "\n  ${BOLD}${RED}Uninstalling Melo TUI...${RESET}\n\n"

if [ -d "$INSTALL_DIR" ]; then
    log_step "Removing binaries from $INSTALL_DIR..."
    rm -rf "$INSTALL_DIR"
    log_success "Binaries removed."
fi

# Remove launchers
rm -f "$BIN_DIR/melo-tui" "$BIN_DIR/melo-cli"

# Remove the optional TUI desktop menu entry (if present)
DESKTOP_FILE="${XDG_DATA_HOME:-$HOME/.local/share}/applications/melo-tui.desktop"
if [ -f "$DESKTOP_FILE" ]; then
    rm -f "$DESKTOP_FILE"
    log_success "Removed desktop menu entry."
    if command -v update-desktop-database >/dev/null 2>&1; then
        update-desktop-database "$(dirname "$DESKTOP_FILE")" >/dev/null 2>&1 || true
    fi
fi

# Check if GUI is still present
if [ -x "$BIN_DIR/melo-gui" ] || [ -d "$HOME/.local/share/melo-gui" ] || [ -d "/opt/melo-gui" ]; then
    log_info "Melo GUI is still installed; preserving unified launcher at $BIN_DIR/melo."
    if [ -x "$BIN_DIR/melo-gui" ]; then
        ln -sf "melo-gui" "$BIN_DIR/melo"
    fi
else
    rm -f "$BIN_DIR/melo"
    log_success "Launchers removed."
fi

# Configuration directory prompt
if [ -d "$CONFIG_DIR" ]; then
    remove_config=false
    if [ "$AUTO_CONFIRM" = true ]; then
        remove_config=true
    else
        printf "\n  ${YELLOW}?${RESET} Remove user configuration & cache at ${BOLD}%s${RESET}? [y/N]: " "$CONFIG_DIR"
        read -r answer < /dev/tty || answer="n"
        case "$answer" in
            [yY]*) remove_config=true ;;
            *)     remove_config=false ;;
        esac
    fi

    if [ "$remove_config" = true ]; then
        rm -rf "$CONFIG_DIR"
        log_success "Configuration directory removed."
    else
        log_info "Configuration preserved at $CONFIG_DIR"
    fi
fi

printf "\n"
printf "  ${GREEN}┌────────────────────────────────────────────────────────┐${RESET}\n"
printf "  ${GREEN}│${RESET}  ${BOLD}${GREEN}✔ Melo TUI was successfully uninstalled.${RESET}             ${GREEN}│${RESET}\n"
printf "  ${GREEN}│${RESET}  Thank you for using Melo!                             ${GREEN}│${RESET}\n"
printf "  ${GREEN}└────────────────────────────────────────────────────────┘${RESET}\n\n"
