#!/bin/bash
__dir="$(dirname "$0")"
source $__dir/config.sh

# Pushes all web sources to remote machine

# DBG+=" --dry-run" # testing
FLAGS+=$DBG
FLAGS+=" -avr"
FLAGS+=" --delete"

excludes+="--exclude=anyplace_views "
excludes+="--exclude=app.private*.conf "
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

function pushCode() {
  rfolder=$1
  echo "Syncing code to: '$rfolder':"
  output=$(rsync $FLAGS $LFOLDER $REMOTE:$rfolder $excludes)
  
  if [[ $DBG != "" ]]; then
    echo ""
    echo "DRY RUN:"
    echo rsync $FLAGS $LFOLDER $REMOTE:$rfolder $excludes
    echo "FULL OUTPUT:"
    echo -e $output
  else 
    echo -e $output
  fi
  
  output=$(echo "$output" | egrep -v "building file")
  output=$(echo "$output" | egrep -v "sent")
  output=$(echo "$output" | egrep -v "total")
  
  if [[ $NOTIFIER != "" ]]; then
    $NOTIFIER -title "Synced" -message "$output"
  fi
}

pushCode $RFOLDER

if [[ $RFOLDER2 != "" ]]; then
  pushCode $RFOLDER2
fi
