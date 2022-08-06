#!/bin/bash
OUT=./build/outputs/apk/debug/
APK=$OUT/navigator-debug.apk
DIR_LPROP=../local.properties

if [ ! -f $DIR_LPROP ]; then
  echo "no ../local.properties file found"
  exit
fi

rm $APK
gradle assembleDebug
