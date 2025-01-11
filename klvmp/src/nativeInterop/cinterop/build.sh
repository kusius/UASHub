#!/bin/bash
set -e

[[ -z "$TARGET" ]] && echo "Please set the PLATFORM variable" && exit 1

mkdir -p build/$TARGET
cd build/$TARGET

cmake ../../
cmake --build .