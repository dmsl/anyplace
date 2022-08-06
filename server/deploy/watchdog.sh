#!/bin/bash

DOMAIN=
PORT=
./sync.sh

watchmedo shell-command \
      --patterns="*.scala;*.js;*.css;*.vue;*.html;*.htm;*.json;*.routes;*.yml;*.png;*.svg;*.jpg" \
      --recursive \
      --command="./sync.sh" \
      ..
