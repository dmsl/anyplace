#!/bin/bash

#watchmedo shell-command --patterns="*.scala" --recursive --command="./sync.sh"


#--patterns="*.scala;*.css;*.js;*.vue;" \

./sync.sh
watchmedo shell-command \
      --patterns="*.scala;*.js;*.css;*.vue;*.html;*.htm" \
      --recursive \
      --command="./sync.sh"
