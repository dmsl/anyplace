#!/bin/bash
OUT=./build/outputs/apk/debug/
APK=$OUT/smas-debug.apk

rm $APK
gradle assembleDebug
