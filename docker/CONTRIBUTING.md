# Contributing to anyplace/docker

Thanks for your interest in contributing to [Anyplace](http://anyplace.cs.ucy.ac.cy/)!

Please use Github Issues to report a problem or create Github Pull Request to be reviewed.

---

# Components:

### `cache_cert.sh`: Certificate caching (for docker deployment):
The process of providing external certificates, or reusing the same self-signed certificate is done through this caching mechanism.
The latter is used for the docker deployment  in order to speed up the image generation when purging the container instance and the container iamge.

Anyplace Docker expects to find a certificate at: ./anyplace/cache/
If it does not exist, then it will go ahead and generate a new one and subsequently use it with the process explained above.

## .env:
    - files that are used during build as ARGS (when building the image)
    - and ENV stuff when running the container
## conf:
It is the play framework configuration. It should not be modified directly, as it is initialized with the `.env` file.
Depending on the Version installed (dev or stable) it uses the relevant `application.conf` file. It is located at:
conf/<environment>/application.conf

Current Stable versions:
    - v4.0
