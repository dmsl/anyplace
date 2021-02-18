#!/bin/bash

./sync.sh
watchmedo shell-command \
      --patterns="*.scala;*.css;*.js;*.vue;" \
      --recursive \
      --command="./sync.sh" \
      .
