#!/bin/bash
__dir="$(dirname "$0")"

./$__dir/push_code.sh

# trigger immediate recompilation
curl $DOMAIN:$PORT --max-time  1
