#!/usr/bin/env sh
# =====================================================================
# Melo TUI & CLI Uninstaller for Unix (Linux & macOS)
# =====================================================================
set -e

INSTALL_DIR="${MELO_INSTALL_DIR:-$HOME/.local/share/melo-tui}"
BIN_DIR="${MELO_BIN_DIR:-$HOME/.local/bin}"
CONFIG_DIR="${MELO_CONFIG_DIR:-$HOME/.config/melo}"

AUTO_CONFIRM=false
for arg in "$@"; do
    case "$arg" in
        -y|--yes) AUTO_CONFIRM=true ;;
        -h|--help)
            echo "Usage: uninstall.sh [OPTIONS]"
            echo ""
            echo "Options:"
            echo "  -y, --yes   Remove configuration without prompt"
            echo "  -h, --help  Show this help message"
            exit 0
            ;;
        *) ;;
    esac
done

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

printf "\n  ${BOLD}${RED}Uninstalling Melo TUI...${NC}\n\n"

if [ -d "$INSTALL_DIR" ]; then
    echo "  Removing binaries from $INSTALL_DIR..."
    rm -rf "$INSTALL_DIR"
    printf "  ${GREEN}✔${NC} Binaries removed.\n"
fi

rm -f "$BIN_DIR/melo-tui" "$BIN_DIR/melo-cli"

# Remove the optional TUI desktop menu entry (if present)
DESKTOP_FILE="${XDG_DATA_HOME:-$HOME/.local/share}/applications/melo-tui.desktop"
if [ -f "$DESKTOP_FILE" ]; then
    rm -f "$DESKTOP_FILE"
    printf "  ${GREEN}✔${NC} Removed desktop menu entry.\n"
    if command -v update-desktop-database >/dev/null 2>&1; then
        update-desktop-database "$(dirname "$DESKTOP_FILE")" >/dev/null 2>&1 || true
    fi
fi

# Check if GUI is still present
if [ -x "$BIN_DIR/melo-gui" ] || [ -d "$HOME/.local/share/melo-gui" ] || [ -d "/opt/melo-gui" ]; then
    printf "  ${CYAN}ℹ${NC} Melo GUI is still installed; preserving unified launcher at %s/melo.\n" "$BIN_DIR"
    if [ -x "$BIN_DIR/melo-gui" ]; then
        ln -sf "melo-gui" "$BIN_DIR/melo"
    fi
else
    rm -f "$BIN_DIR/melo"
    printf "  ${GREEN}✔${NC} Launchers removed.\n"
fi

# Configuration directory prompt
if [ -d "$CONFIG_DIR" ]; then
    remove_config=false
    if [ "$AUTO_CONFIRM" = true ]; then
        remove_config=true
    else
        printf "\n  ${YELLOW}?${NC} Remove user configuration & cache at ${BOLD}%s${NC}? [y/N]: " "$CONFIG_DIR"
        read -r answer < /dev/tty 2>/dev/null || read -r answer || answer="n"
        case "$answer" in
            [yY]*) remove_config=true ;;
            *)     remove_config=false ;;
        esac
    fi

    if [ "$remove_config" = true ]; then
        rm -rf "$CONFIG_DIR"
        printf "  ${GREEN}✔${NC} Configuration directory removed.\n"
    else
        printf "  ${CYAN}ℹ${NC} Configuration preserved at %s\n" "$CONFIG_DIR"
    fi
fi

printf "\n  ${GREEN}✔${NC} Melo TUI uninstalled successfully.\n\n"
