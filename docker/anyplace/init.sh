#!/bin/bash
VERSION=$1
SUPPORTED_VERSIONS=$2

if [[ -z "$VERSION" ]]; then
	echo "######################################"
	echo "# ERROR: Failed to load anyplace.env #"
	echo "######################################"
	exit 1
fi

case "$VERSION" in
  "v3.4.1"|"v3.5-dev"|"v4.0")
    echo "Building anyplace $VERSION"
    ;;
  *)
    echo "Unsupported version: $VERSION"
    echo "Supported versions: $SUPPORTED_VERSIONS"
    exit 1
    ;;
esac
