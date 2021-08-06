#!/bin/bash
__dir="$(dirname "$0")"
source $__dir/config.sh

remoteConf=$__dir"/../conf/app.private.remote.conf"

if [ ! -f $remoteConf ]; then
  echo "ERROR: Create the remote configuration file first:"
  echo "cd ../conf/"
  echo "cp app.private.example.conf app.private.remote.conf"
  echo "Then modify it with the remote values."
  exit 1
fi

scp $remoteConf $REMOTE:$RFOLDER/conf/app.private.conf

echo "Make sure you install grunt and npm dependencies on each web app."