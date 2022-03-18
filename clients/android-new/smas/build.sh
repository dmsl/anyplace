#!/bin/bash
OUT=./build/outputs/apk/debug/
APK=$OUT/smas-debug.apk
DIR_PUBLISH=~/Dropbox/DMSL/anyplace-cv-imu/

rm $APK
gradle assembleDebug
