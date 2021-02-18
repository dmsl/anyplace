#!/bin/bash

# ONCE UPDATED IGNORE THIS FILE USING:
# git update-index --assume-unchanged config.sh
USER=anyplace
DOMAIN=ap-dev.cs.ucy.ac.cy
REMOTE=$USER@$DOMAIN

# L: Local
# R: Remote
LFOLDER=.
RFOLDER='~/alpha/nneofy01'
RPORT=9002

## COMPILATION NOTIFICATIONS:
# macOS: terminal-notifier
# Ubuntu/Windows: ?
NOTIFIER=
