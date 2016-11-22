#!/usr/bin/env bash

if [ -d "deb" ]; then
    # If running on GoCD then use deb file created in earlier stage

    echo "Using Debian package file from GoCD."
    DEB=$(find deb -name '*.deb' -print -quit)
else
    # Not in GoCD, so package the deb file ourselves
    # First construct deb file for installing job
    export PATH="./src/it/docker/package-commands:${PATH}"

    ./package/package.sh 1

    DEB=$(find target -name '*.deb' -print -quit)
fi

if [ -z "$DEB" ]; then
    echo "Could not find debian package file."
    exit 1
fi

# The Docker ITs require the deb file to be in src/it/docker/target
mkdir -p src/it/docker/target/
rm src/it/docker/target/*.deb
ln $DEB src/it/docker/target/

# Build container and run tests.
trap 'src/it/docker/post_it.sh; exit $EC' INT TERM EXIT

EC=1

set -e
./src/it/docker/pre_it.sh

./src/it/docker/run_it.sh

EC=0
