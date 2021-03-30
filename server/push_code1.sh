#!/bin/bash
__dir="$(dirname "$0")"
source $__dir/config.sh

RFOLDER='~/alpha/nneofy01-fresh'

# Pushes all web sources to remote machine

# DBG+=" --dry-run" # testing
FLAGS+=$DBG
FLAGS+=" -avr"
FLAGS+=" --delete"

# excludes+="--exclude=bootstrap/cache/ "
# excludes+="--exclude=storage/framework/cache/ "
# excludes+="--exclude=storage/logs/ "
# excludes+="--exclude=storage/framework/sessions/ "
# excludes+="--exclude=storage/framework/views/ "
# excludes+="--exclude=storage/app/ "
# excludes+="--exclude=Build "
# excludes+="--exclude=.composer.lock"
# excludes+="--exclude=_build "
# excludes+="--exclude=blib "
# excludes+="--exclude=public/anyplace_architect/libs/ "
# excludes+="--exclude=public/anyplace_viewer_campus/libs/ "
# excludes+="--exclude=public/anyplace_viewer/libs/ "

excludes+="--exclude=anyplace_views "
excludes+="--exclude=application.conf "
excludes+="--exclude=logs "
excludes+="--exclude=dist "
excludes+="--exclude=tmp "
excludes+="--exclude=public/anyplace_architect/build/ "
excludes+="--exclude=public/anyplace_viewer_campus/build/ "
excludes+="--exclude=public/anyplace_viewer/build/ "

excludes+="--exclude=.env "
excludes+="--exclude=.DS_Store "
excludes+="--exclude=\".git*\" "
excludes+="--exclude=.idea "
excludes+="--exclude=node_modules "
excludes+="--exclude=bower_components "

# Generated play files
excludes+="--exclude=target "
excludes+="--exclude=test " # scala testing

lfolders="$LFOLDER"
rsync $FLAGS $LFOLDER $REMOTE:$RFOLDER $excludes
# output=$(rsync $FLAGS $LFOLDER $REMOTE:$RFOLDER $excludes)

if [[ $DBG != "" ]]; then
  echo ""
  echo "DRY RUN:"
  echo rsync $FLAGS $LFOLDER $REMOTE:$RFOLDER $excludes
  echo "FULL OUTPUT:"
  echo -e $output
fi

output=$(echo "$output" | egrep -v "building file")
output=$(echo "$output" | egrep -v "sent")
output=$(echo "$output" | egrep -v "total")

if [[ $NOTIFIER != "" ]]; then
  $NOTIFIER -title "Synced" -message "$output"
fi
