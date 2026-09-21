#!/usr/bin/env bash
# ==============================================================================
# Melo TUI & CLI One-Line Installer (Linux & macOS)
#
# Usage:
#   curl -fsSL https://raw.githubusercontent.com/Adriianh/Melo/master/scripts/install.sh | bash
#
# Options:
#   MELO_VERSION="2.1.0"      Install a specific version (default: latest)
#   MELO_INSTALL_DIR="..."    Custom install directory (default: ~/.local/share/melo-tui)
#   MELO_BIN_DIR="..."        Custom bin directory (default: ~/.local/bin)
# ==============================================================================
set -euo pipefail

REPO="Adriianh/Melo"
INSTALL_DIR="${MELO_INSTALL_DIR:-$HOME/.local/share/melo-tui}"
BIN_DIR="${MELO_BIN_DIR:-$HOME/.local/bin}"
CONFIG_DIR="${MELO_CONFIG_DIR:-$HOME/.config/melo}"

# Check color support
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
log_warn()    { printf "  ${YELLOW}⚠${RESET} %b\n" "$1"; }
log_error()   { printf "  ${RED}✖${RESET} %b\n" "$1" >&2; }

print_banner() {
    printf "\n"
    printf "${PURPLE}${BOLD}"
    cat << 'EOF'
    __  __      _       
   |  \/  | ___| | ___  
   | |\/| |/ _ \ |/ _ \ 
   | |  | |  __/ | (_) |
   |_|  |_|\___|_|\___/ 
EOF
    printf "${RESET}"
    printf "   ${GRAY}Terminal Audio Player & CLI • https://github.com/%s${RESET}\n\n" "$REPO"
}

VERSION="${MELO_VERSION:-}"
while [[ $# -gt 0 ]]; do
    case "$1" in
        -v|--version)
            VERSION="$2"
            shift 2
            ;;
        --uninstall)
            SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
            if [ -f "$SCRIPT_DIR/uninstall.sh" ]; then
                exec bash "$SCRIPT_DIR/uninstall.sh" "$@"
            else
                exec curl -fsSL "https://raw.githubusercontent.com/$REPO/master/scripts/uninstall.sh" | bash
            fi
            exit 0
            ;;
        -h|--help)
            echo "Usage: install.sh [OPTIONS]"
            echo ""
            echo "Options:"
            echo "  -v, --version <VERSION>   Install specific version (e.g. 2.1.0)"
            echo "      --uninstall           Uninstall Melo TUI"
            echo "  -h, --help                Show this help message"
            exit 0
            ;;
        *)
            log_error "Unknown option: $1"
            exit 1
            ;;
    esac
done

detect_os() {
    local os_raw
    os_raw="$(uname -s)"
    case "$os_raw" in
        Linux*)  echo "linux" ;;
        Darwin*) echo "macos" ;;
        *)
            log_error "Unsupported operating system: $os_raw"
            log_error "Melo TUI installer currently supports Linux and macOS."
            exit 1
            ;;
    esac
}

detect_arch() {
    local arch_raw
    arch_raw="$(uname -m)"
    case "$arch_raw" in
        x86_64|amd64) echo "x86_64" ;;
        arm64|aarch64) echo "arm64" ;;
        *)            echo "$arch_raw" ;;
    esac
}

check_dependencies() {
    local missing=()
    for cmd in curl tar; do
        if ! command -v "$cmd" >/dev/null 2>&1; then
            missing+=("$cmd")
        fi
    done
    if [ ${#missing[@]} -gt 0 ]; then
        log_error "Missing required utilities: ${missing[*]}"
        log_error "Please install them via your package manager and try again."
        exit 1
    fi
}

resolve_latest_version() {
    local latest_tag=""
    # 1. GitHub API
    latest_tag="$(curl -fsSL -H "Accept: application/vnd.github.v3+json" "https://api.github.com/repos/$REPO/releases/latest" 2>/dev/null | grep '"tag_name":' | sed -E 's/.*"tag_name": *"([^"]+)".*/\1/' || true)"
    
    # 2. Redirect header fallback
    if [ -z "$latest_tag" ]; then
        latest_tag="$(curl -fsSI "https://github.com/$REPO/releases/latest" 2>/dev/null | grep -i '^location:' | sed -E 's/.*tag\/(.*)/\1/' | tr -d '\r\n' || true)"
    fi

    # 3. Default fallback
    if [ -z "$latest_tag" ]; then
        latest_tag="v2.1.3"
    fi

    echo "${latest_tag#v}"
}

main() {
    print_banner
    check_dependencies

    local os arch
    os="$(detect_os)"
    arch="$(detect_arch)"

    log_step "Resolving target release..."
    if [ -z "$VERSION" ]; then
        VERSION="$(resolve_latest_version)"
    fi
    VERSION="${VERSION#v}"

    printf "  ${GRAY}┌────────────────────────────────────────────────────────┐${RESET}\n"
    printf "  ${GRAY}│${RESET}  ${BOLD}Platform:${RESET}  %-44s${GRAY}│${RESET}\n" "$os ($arch)"
    printf "  ${GRAY}│${RESET}  ${BOLD}Version:${RESET}   %-44s${GRAY}│${RESET}\n" "v$VERSION"
    printf "  ${GRAY}│${RESET}  ${BOLD}Target:${RESET}    %-44s${GRAY}│${RESET}\n" "$INSTALL_DIR"
    printf "  ${GRAY}│${RESET}  ${BOLD}Binaries:${RESET}  %-44s${GRAY}│${RESET}\n" "$BIN_DIR"
    printf "  ${GRAY}└────────────────────────────────────────────────────────┘${RESET}\n\n"

    local asset_name="melo-${VERSION}-${os}.tar.gz"
    local download_url="https://github.com/$REPO/releases/download/v${VERSION}/${asset_name}"

    local tmp_dir
    tmp_dir="$(mktemp -d)"
    trap 'rm -rf "$tmp_dir"' EXIT

    log_step "Downloading ${CYAN}${asset_name}${RESET}..."
    if ! curl -fSL --progress-bar "$download_url" -o "$tmp_dir/$asset_name"; then
        printf "\n"
        log_error "Failed to download asset: ${download_url}"
        log_error "Verify release v${VERSION} exists at https://github.com/$REPO/releases"
        exit 1
    fi
    log_success "Package downloaded successfully."

    log_step "Extracting files..."
    tar -xzf "$tmp_dir/$asset_name" -C "$tmp_dir"

    local extract_root
    if [ -d "$tmp_dir/melo-$VERSION" ]; then
        extract_root="$tmp_dir/melo-$VERSION"
    else
        extract_root="$(find "$tmp_dir" -mindepth 1 -maxdepth 1 -type d | head -n 1)"
    fi

    if [ -z "$extract_root" ] || [ ! -f "$extract_root/melo" ]; then
        log_error "Could not find 'melo' executable in archive."
        exit 1
    fi

    log_step "Installing native binaries & libraries..."
    mkdir -p "$INSTALL_DIR" "$BIN_DIR" "$CONFIG_DIR"

    cp -f "$extract_root/melo" "$INSTALL_DIR/melo"
    chmod +x "$INSTALL_DIR/melo"
    find "$extract_root" -maxdepth 1 \( -name "*.so" -o -name "*.dylib" \) -exec cp -f {} "$INSTALL_DIR/" \; 2>/dev/null || true

    # Create melo-tui launcher
    cat << 'EOF' > "$BIN_DIR/melo-tui"
#!/usr/bin/env sh
INSTALL_DIR="%INSTALL_DIR%"
if [ "$(uname)" = "Darwin" ]; then
  export DYLD_LIBRARY_PATH="$INSTALL_DIR:$DYLD_LIBRARY_PATH"
else
  export LD_LIBRARY_PATH="$INSTALL_DIR:$LD_LIBRARY_PATH"
fi
exec "$INSTALL_DIR/melo" "$@"
EOF
    sed -i "s|%INSTALL_DIR%|$INSTALL_DIR|g" "$BIN_DIR/melo-tui" 2>/dev/null || sed -i '' "s|%INSTALL_DIR%|$INSTALL_DIR|g" "$BIN_DIR/melo-tui"
    chmod +x "$BIN_DIR/melo-tui"
    ln -sf "melo-tui" "$BIN_DIR/melo-cli"

    # Create unified launcher
    if [ -f "$extract_root/install.sh" ]; then
        MELO_INSTALL_DIR="$INSTALL_DIR" MELO_BIN_DIR="$BIN_DIR" MELO_CONFIG_DIR="$CONFIG_DIR" sh "$extract_root/install.sh" >/dev/null 2>&1 || true
    else
        ln -sf "melo-tui" "$BIN_DIR/melo"
    fi

    log_success "Melo TUI v${VERSION} installed."

    # Verify PATH
    local in_path=false
    case ":$PATH:" in
        *":$BIN_DIR:"*) in_path=true ;;
    esac

    printf "\n"
    if [ "$in_path" = false ]; then
        printf "  ${YELLOW}┌────────────────────────────────────────────────────────┐${RESET}\n"
        printf "  ${YELLOW}│${RESET}  ${BOLD}Notice:${RESET} $BIN_DIR is not in your \$PATH            ${YELLOW}│${RESET}\n"
        printf "  ${YELLOW}│${RESET}  Add it to your shell configuration to run 'melo':    ${YELLOW}│${RESET}\n"
        printf "  ${YELLOW}│${RESET}                                                         ${YELLOW}│${RESET}\n"
        if [ -n "${ZSH_VERSION:-}" ] || [ "${SHELL##*/}" = "zsh" ]; then
            printf "  ${YELLOW}│${RESET}  ${CYAN}echo 'export PATH=\"%s:\$PATH\"' >> ~/.zshrc${RESET}    ${YELLOW}│${RESET}\n" "$BIN_DIR"
            printf "  ${YELLOW}│${RESET}  ${CYAN}source ~/.zshrc${RESET}                                      ${YELLOW}│${RESET}\n"
        elif [ -n "${FISH_VERSION:-}" ] || [ "${SHELL##*/}" = "fish" ]; then
            printf "  ${YELLOW}│${RESET}  ${CYAN}fish_add_path %s${RESET}                          ${YELLOW}│${RESET}\n" "$BIN_DIR"
        else
            printf "  ${YELLOW}│${RESET}  ${CYAN}echo 'export PATH=\"%s:\$PATH\"' >> ~/.bashrc${RESET}   ${YELLOW}│${RESET}\n" "$BIN_DIR"
            printf "  ${YELLOW}│${RESET}  ${CYAN}source ~/.bashrc${RESET}                                     ${YELLOW}│${RESET}\n"
        fi
        printf "  ${YELLOW}└────────────────────────────────────────────────────────┘${RESET}\n\n"
    fi

    printf "  ${GREEN}┌────────────────────────────────────────────────────────┐${RESET}\n"
    printf "  ${GREEN}│${RESET}  ${BOLD}${GREEN}✦ Installation complete!${RESET}                              ${GREEN}│${RESET}\n"
    printf "  ${GREEN}│${RESET}                                                         ${GREEN}│${RESET}\n"
    printf "  ${GREEN}│${RESET}  Run ${BOLD}${CYAN}melo${RESET}             Start the interactive TUI player    ${GREEN}│${RESET}\n"
    printf "  ${GREEN}│${RESET}  Run ${BOLD}${CYAN}melo play <song>${RESET} Instant playback from terminal      ${GREEN}│${RESET}\n"
    printf "  ${GREEN}│${RESET}  Run ${BOLD}${CYAN}melo --help${RESET}      Explore full CLI commands           ${GREEN}│${RESET}\n"
    printf "  ${GREEN}└────────────────────────────────────────────────────────┘${RESET}\n\n"
}

main "$@"
