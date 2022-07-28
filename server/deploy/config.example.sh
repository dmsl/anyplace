#!/bin/bash

# INFO: copy to config.sh and use.

# ONCE UPDATED IGNORE THIS FILE USING:
# git update-index --assume-unchanged config.sh
USER=
DOMAIN=

# remote folder
RFOLDER='~/alpha-deploy/'
RPORT=9001

# optionally send to a second folder
RFOLDER2=''
RPORT2=

## COMPILATION NOTIFICATIONS:
# macOS: terminal-notifier
# Ubuntu/Windows: ?
NOTIFIER=


###############
REMOTE=$USER@$DOMAIN

# Local folder
LFOLDER=..