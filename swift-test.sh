#!/usr/bin/bash
# swift-test.sh — build the Swift Export package and run swift test locally.
#
# Usage:
#   ./swift-test.sh           # build + test
#   ./swift-test.sh --build   # build only (generates SPM package + static archive)
#   ./swift-test.sh --test    # test only (assumes build already ran)
#
# After running --build, you can open swift-test-harness/Package.swift in Xcode
# and use the full IDE experience (test navigator, breakpoints, etc.).
set -euo pipefail
REPO_ROOT="$(cd "$(dirname "$0")" && pwd)"
ACTION="${1:-all}"

export BUILT_PRODUCTS_DIR="$REPO_ROOT/build/swift-test"
export TARGET_BUILD_DIR="$REPO_ROOT/build/swift-test"
export SDK_NAME=macosx
export CONFIGURATION=Debug
export ARCHS=arm64
export FRAMEWORKS_FOLDER_PATH=Frameworks
export MACOSX_DEPLOYMENT_TARGET=14.0
export DEPLOYMENT_TARGET_SETTING_NAME=MACOSX_DEPLOYMENT_TARGET

if [ "$ACTION" = "--test" ]; then
    echo "Running swift test (assuming build artifacts exist)..."
    cd "$REPO_ROOT/swift-test-harness"
    exec swift test
fi

echo "Building Swift Export package for macOS arm64..."
cd "$REPO_ROOT"
./gradlew embedSwiftExportForXcode --no-configuration-cache

if [ "$ACTION" = "--build" ]; then
    echo "Build complete. SPM package at: build/SPMPackage/macosArm64/Debug/"
    echo "Static archive at: build/swift-test/libSerde.a"
    echo ""
    echo "To open in Xcode:"
    echo "  open swift-test-harness/Package.swift"
    exit 0
fi

echo "Running swift test..."
cd "$REPO_ROOT/swift-test-harness"
exec swift test
