#!/bin/bash

./sync.sh

watchmedo shell-command \
      --patterns="*.scala;*.js;*.css;*.vue;*.html;*.htm;*.json;*.routes;*.yml" \
      --recursive \
      --command="./sync.sh" \
      ..
