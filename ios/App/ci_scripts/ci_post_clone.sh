#!/usr/bin/env bash

# Exit on error and print commands
set -e -x

# Environment setup
export HOMEBREW_NO_INSTALL_CLEANUP=TRUE
export JAVA_HOME=/usr/local/opt/openjdk/libexec/openjdk.jdk/Contents/Home
export PATH="/usr/local/opt/openjdk/bin:$PATH"
export CPPFLAGS="-I/usr/local/opt/openjdk/include"

# Create symlink for Java wrappers (with error handling)
if [ ! -L "/Library/Java/JavaVirtualMachines/openjdk.jdk" ]; then
    sudo ln -sfn /usr/local/opt/openjdk/libexec/openjdk.jdk /Library/Java/JavaVirtualMachines/openjdk.jdk || {
        echo "Failed to create Java symlink. Please check permissions."
        exit 1
    }
fi

# Install required packages
brew install cocoapods openjdk node || {
    echo "Failed to install packages via Homebrew"
    exit 1
}

# Install dependencies
npm ci || {
    echo "npm ci failed"
    exit 1
}

# Build and sync
npm run build:web || {
    echo "build:web failed"
    exit 1
}

npm run sync:ios || {
    echo "sync:ios failed"
    exit 1
}
