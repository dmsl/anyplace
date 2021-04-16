#!/bin/bash
__dir="$(dirname "$0")"
source $__dir/config.sh

./$__dir/push_code.sh

# trigger immediate recompilation
curl $DOMAIN:$RPORT --max-time  1

if [[ $RPORT2 != "" ]]; then
  curl $DOMAIN:$RPORT2 --max-time  1
fi
