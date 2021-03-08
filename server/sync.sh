#!/bin/bash
__dir="$(dirname "$0")"
source $__dir/config.sh

./$__dir/push_code.sh

# trigger immediate recompilation
curl $DOMAIN:$RPORT --max-time  1
