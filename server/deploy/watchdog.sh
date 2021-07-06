#!/bin/bash

DOMAIN=
PORT=
./sync.sh

watchmedo shell-command \
      --patterns="*.scala;*.js;*.css;*.vue;*.html;*.htm" \
      --recursive \
      --command="./sync.sh" \
      ..
