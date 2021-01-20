#!/bin/bash

# User must provide 6 arguments
if [ "$#" != 6 ]
then 
	echo "Please provide domain name, port, username, password, bucket, path."
exit 1
fi

sudo /opt/couchbase/bin/cbexport json --c $1:$2 --u $3 --p $4 -b $5 -f lines -o $6
