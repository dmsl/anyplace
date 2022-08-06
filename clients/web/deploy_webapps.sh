#!/bin/bash

## ASSUMING AN APACHE DEPLOYMENT.
## WHATEVER EXISTS ALREADY IN $DEST, WILL NOT BE REGENERATED.
## Make sure you put the correct Anyplace Backend URL in the API_FILE

## MODIFY THESE:
DEST=~/web
SRC=.
API_FILE=$SRC/shared/js/anyplace-core-js/api.js

# this is the default url that must be modified (in api.js)
defaultStr="protocol://server:port/path"

function check_api_url() {
  if [ ! -f $API_FILE ]; then
    echo "Not found:"
    echo "$API_FILE"
    exit
  fi


  if grep -q $defaultStr $API_FILE
  then
    echo "Must update API.url in:"
    echo "$API_FILE"
    exit
  fi
}

function show_skip_warning() {
    local app=$1
    local path=$2
    echo "################"
    echo "################"
    echo "## SKIPPED $app: folder $path already exists.."
    echo "################"
    echo "################"
}


function deploy_developers() {
  local app="developers"

  if [ -d $DEST/$app ]; then
    show_skip_warning $app $DEST/$app
    return
  fi

  # no compilation is needed. just copy this..
  cp -R $SRC/$app $DEST/$app
  echo "# Installed $app at: $DEST/$app"
}

function deploy_anyplace_app() {
  app=$1

  if [ -d $DEST/$app ]; then
    show_skip_warning $app $DEST/$app
    return
  fi

  echo "# Compiling $app.."

  cd $SRC/anyplace_$app
  bower install && npm install && grunt deploy
  cd ..
  cp -R $SRC/anyplace_$app $DEST/$app
  echo "# Installed $app at: $DEST/$app"
}



#############


check_api_url
deploy_anyplace_app "viewer"
deploy_anyplace_app "viewer_campus"
deploy_anyplace_app "architect"

deploy_developers
