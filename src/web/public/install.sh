#!/bin/bash
set -e

# Dropper CLI Installation Script
# Usage:
#   curl -fsSL https://raw.githubusercontent.com/sebastianstupak/dropper/main/scripts/install.sh | sh
#   ./install.sh [version]
#   ./install.sh v1.0.0  # Install specific version
#   ./install.sh draft   # Install latest draft release

VERSION="${1:-latest}"
REPO="sebastianstupak/dropper"
INSTALL_DIR="${DROPPER_INSTALL_DIR:-$HOME/.dropper}"
BIN_DIR="${DROPPER_BIN_DIR:-$HOME/.local/bin}"

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

error() {
    echo -e "${RED}[ERROR]${NC} $1"
    exit 1
}

# Detect platform
detect_platform() {
    OS=$(uname -s | tr '[:upper:]' '[:lower:]')
    ARCH=$(uname -m)

    case "$OS" in
        linux*)
            PLATFORM="linux"
            ;;
        darwin*)
            PLATFORM="macos"
            ;;
        msys*|mingw*|cygwin*)
            PLATFORM="windows"
            ;;
        *)
            error "Unsupported OS: $OS"
            ;;
    esac

    case "$ARCH" in
        x86_64|amd64)
            ARCH="amd64"
            ;;
        aarch64|arm64)
            ARCH="arm64"
            ;;
        *)
            error "Unsupported architecture: $ARCH"
            ;;
    esac

    BINARY_NAME="dropper-$PLATFORM-$ARCH"
    if [ "$PLATFORM" = "windows" ]; then
        BINARY_NAME="$BINARY_NAME.exe"
    fi
}

# Get release version
get_release() {
    info "Fetching release information..."

    if [ "$VERSION" = "latest" ]; then
        RELEASE_URL="https://api.github.com/repos/$REPO/releases/latest"
    elif [ "$VERSION" = "draft" ]; then
        # Get latest draft release
        RELEASE_URL="https://api.github.com/repos/$REPO/releases"
        RELEASE_DATA=$(curl -sL "$RELEASE_URL" | grep -A 10 '"draft": true' | head -20)
        RELEASE_TAG=$(echo "$RELEASE_DATA" | grep '"tag_name"' | head -1 | sed -E 's/.*"([^"]+)".*/\1/')
        if [ -z "$RELEASE_TAG" ]; then
            error "No draft releases found"
        fi
        VERSION="$RELEASE_TAG"
    else
        RELEASE_TAG="$VERSION"
    fi

    if [ "$VERSION" != "draft" ] && [ "$VERSION" != "$RELEASE_TAG" ]; then
        RELEASE_DATA=$(curl -sL "https://api.github.com/repos/$REPO/releases/tags/$VERSION")
        RELEASE_TAG=$(echo "$RELEASE_DATA" | grep '"tag_name"' | sed -E 's/.*"([^"]+)".*/\1/')
    fi

    if [ -z "$RELEASE_TAG" ]; then
        RELEASE_DATA=$(curl -sL "$RELEASE_URL")
        RELEASE_TAG=$(echo "$RELEASE_DATA" | grep '"tag_name"' | sed -E 's/.*"([^"]+)".*/\1/')
    fi

    if [ -z "$RELEASE_TAG" ]; then
        error "Could not determine release version"
    fi

    info "Installing Dropper $RELEASE_TAG"
}

# Download binary
download_binary() {
    DOWNLOAD_URL="https://github.com/$REPO/releases/download/$RELEASE_TAG/$BINARY_NAME"

    info "Downloading from $DOWNLOAD_URL..."

    mkdir -p "$INSTALL_DIR"

    if command -v curl &> /dev/null; then
        curl -fsSL "$DOWNLOAD_URL" -o "$INSTALL_DIR/dropper" || error "Download failed"
    elif command -v wget &> /dev/null; then
        wget -q "$DOWNLOAD_URL" -O "$INSTALL_DIR/dropper" || error "Download failed"
    else
        error "Neither curl nor wget found. Please install one of them."
    fi

    chmod +x "$INSTALL_DIR/dropper"
}

# Install to PATH
install_to_path() {
    mkdir -p "$BIN_DIR"

    # Create or update symlink
    ln -sf "$INSTALL_DIR/dropper" "$BIN_DIR/dropper"

    info "Dropper installed to $BIN_DIR/dropper"
}

# Update shell configuration
update_shell_config() {
    # Detect shell
    SHELL_RC=""
    if [ -n "$ZSH_VERSION" ]; then
        SHELL_RC="$HOME/.zshrc"
    elif [ -n "$BASH_VERSION" ]; then
        SHELL_RC="$HOME/.bashrc"
    fi

    if [ -n "$SHELL_RC" ]; then
        if ! echo "$PATH" | grep -q "$BIN_DIR"; then
            echo "" >> "$SHELL_RC"
            echo "# Dropper CLI" >> "$SHELL_RC"
            echo "export PATH=\"$BIN_DIR:\$PATH\"" >> "$SHELL_RC"
            warn "Added $BIN_DIR to PATH in $SHELL_RC"
            warn "Run: source $SHELL_RC"
        fi
    fi
}

# Verify installation
verify_installation() {
    if [ -x "$BIN_DIR/dropper" ]; then
        VERSION_OUTPUT=$("$BIN_DIR/dropper" --version 2>&1 || echo "")
        info "✓ Installation successful!"
        echo ""
        echo "Dropper $RELEASE_TAG installed"
        echo ""
        echo "Get started:"
        echo "  dropper init my-mod"
        echo ""
        echo "For help:"
        echo "  dropper --help"
    else
        error "Installation verification failed"
    fi
}

# Main installation flow
main() {
    info "Installing Dropper CLI for $PLATFORM-$ARCH..."

    detect_platform
    get_release
    download_binary
    install_to_path
    update_shell_config
    verify_installation
}

main
