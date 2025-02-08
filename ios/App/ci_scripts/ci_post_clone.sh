#!/usr/bin/env bash
set -e -x

export HOMEBREW_NO_INSTALL_CLEANUP=TRUE
export JAVA_HOME=/usr/local/opt/openjdk/libexec/openjdk.jdk/Contents/Home 

# We need cocoapods for Capacitor and Java + Nodefor cljs
brew install cocoapods openjdk node

# Install dependencies
npm ci

# Build and sync
npm run build:web
npm run sync:ios
