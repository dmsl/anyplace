#!/bin/bash

./sync.sh

watchmedo shell-command \
      --patterns="*.scala;*.js;*.css;*.vue;*.html;*.htm;*.json;*.routes" \
      --recursive \
      --command="./sync.sh" \
      ..
