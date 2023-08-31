#!/usr/bin/env bash

set -e -x

export HOMEBREW_NO_INSTALL_CLEANUP=TRUE
export JAVA_HOME=/usr/local/opt/openjdk/libexec/openjdk.jdk/Contents/Home 

# We need cocoapods for Capacitor and Java for cljs
brew install cocoapods openjdk

# Installing node through brew doesn't work for some reason
# https://stackoverflow.com/questions/73462672/xcode-cloud-suddenly-failing-to-link-node-and-install-dependencies
NODE_VER=16
VERSION=$(curl -s https://nodejs.org/dist/latest-v$NODE_VER.x/ | sed -nE 's|.*>node-(.*)\.pkg</a>.*|\1|p')
if [[ "$(arch)" == "arm64" ]]
then
  ARCH="arm64"
else
  ARCH="x64"
fi

curl "https://nodejs.org/dist/latest-v$NODE_VER.x/node-$VERSION-darwin-$ARCH.tar.gz" -o $HOME/Downloads/node.tar.gz
tar -xf "$HOME/Downloads/node.tar.gz"
NODE_PATH="$PWD/node-$VERSION-darwin-$ARCH/bin"
PATH+=":$NODE_PATH"
export PATH
node -v
npm -v

# Install dependencies
npm ci

# Build and sync
npm run build:web
npm run sync:ios
