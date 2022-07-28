#!/bin/bash

## ASSUMING AN APACHE DEPLOYMENT.
## WHATEVER EXISTS ALREADY IN $DEST, WILL NOT BE REGENERATED.

## MODIFY THESE:
DEST=~/web
SRC=~/git.web/clients/web


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
  cp -R $SRC/anyplace_$app $DEST/$app
  echo "# Installed $app at: $DEST/$app"
}

deploy_anyplace_app "viewer"
deploy_anyplace_app "viewer_campus"
deploy_anyplace_app "architect"

deploy_developers
