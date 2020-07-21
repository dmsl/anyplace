# anyplace-couchbase
It's essentially the Couchbase 6.0 Community Edition that is compatible with anyplace.
It does not extend that image.

Couchbase initialization is done on a separate manual step using: `../post_install.sh`

If Couchbase has not been initialized the `anyplace` docker image will prompt the user
and wait until initialization is done. This should happen only during the first deployment.

# anyplace_views (from github/anyplace):
Couchbase views initialization specific to anyplace
