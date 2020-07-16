# Anyplace Docker image:
Automates the deployment of the Anyplace service.

# Build Anyplace image only
Use docker-compose on parent directory:
```
docker-compose build anyplace
```

# cache:
It contains caches of `cert`, or anyplace-server zip files.

## cert:
This is a cache of the HTTPS/SSL certificates.
It may be used to supply pre-existing certificates on a production
environment, or to cache a previously generated SSL certificate
that was generated when the anyplace image was built.

## anyplace-server zip: (docker development)
It is the compiled sources of anyplace.
It is used mostly to speedup the docker image development.

# tiler:
Sources of the `anyplace_tiler`.

TODO: if possible re-use the existing script from github/anyplace.
