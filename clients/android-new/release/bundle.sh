#!/bin/bash

## PREREQUISITES:
### install somehow bundletool

DIR_KEYSTORE=~/src/dmsl/play-keystores
KS=$DIR_KEYSTORE/anyplace.jks
KS_PASS=$DIR_KEYSTORE/keystore.pwd
KEY_PASS=$KS_PASS

IN=logger-release.aab
APKS=logger.apks

bundletool build-apks \
  --bundle=$IN --output=$APKS \
  --ks=$KS --ks-pass=file:$KS_PASS \
  --ks-key-alias=logger --key-pass=file:$KEY_PASS


mkdir -p apks

bundletool extract-apks \
  --apks=$APKS \
  --output-dir=./apks/ \
  --device-spec=device.json
