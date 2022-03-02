#!/bin/bash
OUT=./build/outputs/apk/debug/
APK=$OUT/smas-debug.apk
DIR_PUBLISH=~/Dropbox/DMSL/anyplace-cv-imu/smas/

rm $APK
gradle assembleDebug
