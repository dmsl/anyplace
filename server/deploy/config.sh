#!/bin/bash

# ONCE UPDATED IGNORE THIS FILE USING:
# git update-index --assume-unchanged config.sh
USER=
DOMAIN=
REMOTE=$USER@$DOMAIN

# L: Local
# R: Remote
LFOLDER=..

RFOLDER='~/alpha-deploy/'
RPORT=9001

# optionally send to a second folder
RFOLDER2=''
RPORT2=

## COMPILATION NOTIFICATIONS:
# macOS: terminal-notifier
# Ubuntu/Windows: ?
NOTIFIER=
