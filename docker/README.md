# Anyplace Docker: version 4.0
<p align="center">
<img
src="https://gist.githubusercontent.com/Paschalis/09af7e45f069582a3e97a7021be2a729/raw/560245f7d9021f32da64ee7d7c195ba9b8a60c49/anyplace-docker.png"
width="250">
</p>

[![Watch the video]()](https://www.youtube.com/embed/dRDnF2wCoUo)

<a href="https://www.cs.ucy.ac.cy/~dzeina/papers/mdm20-a4iot.pdf" class="publications-title" target="_blank">The Anyplace 4.0 IoT Localization Architecture</a>"</strong>,  Paschalis Mpeis, Thierry Roussel, Manish Kumar, Constantinos Costa, Christos Laoudias, Denis Capot-Ray  Demetrios Zeinalipour-Yazti <b>"Proceedings of the 21st IEEE International Conference on Mobile Data Management"</b> (<strong>MDM '20</strong>), IEEE Computer Society, ISBN:,  Pages: 8, June 30 - July 3, 2020, Versailles, France, <strong>2020</strong>.&nbsp;

This repository is part of [Anyplace Open Source project](https://github.com/dmsl/anyplace):
```
A free and open Indoor Navigation Service with superb accuracy!
```

It helps setting up and running Anyplace 4.0 software stack within minutes.

[![Join the chat at https://gitter.im/dmsl/anyplace](https://badges.gitter.im/dmsl/anyplace.svg)](https://gitter.im/dmsl/anyplace?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)

# Requirements
## 1. Docker:
Follow instructions here:
https://docs.docker.com/get-docker/

Convenience script:
```
curl -fsSL https://get.docker.com -o get-docker.sh
sudo sh get-docker.sh
```

## Docker without sudo
```
sudo usermod -aG docker $USER
```

## 2. docker-compose
Follow instructions here:
https://docs.docker.com/compose/install/

## 3. Docker `anyplace-base` image¹
```
docker build anyplace-base/ -t anyplace-base
```

It contains all the necessary software stack for the backend.
We expect this image to be updated infrequently.

¹ will be published in [Docker Hub](https://hub.docker.com/).

---

# Deploy anyplace:

## 1. Initialize local folders:
### Run `pre_install.sh`:
This creates the directory structure shown below that will preserve the
data of the deployment to the host OS.

```
~.anyplace:
└── server
    ├── db
    │   ├── couchbase:          Data & logs
    │   └── influx              Data & logs
    ├── fs:                     Filesystem: raw files
    │   ├── acces
    │   ├── floorplans
    │   ├── radiomaps_frozen
    │   ├── radiomaps_raw
    │   └── tiler
    └── play
        ├── cert                LetsEncrypt SSL Certificates
        └── log                 Anyplace logs
```

### Configure installation (optional):
You may modify the above folders by creating symbolic links to
existing external sources. For example you can point `fs` to an existing
distributed filesystem.

## 2. Setup variables:
Edit `.env` with your default values.


## 3. Deploy Single Node:
Create docker images:
```
docker-compose build
```

Start service:
```
docker-compose up
```

NOTE: use `-d` to start the container in detached mode.

### On first run:
```
./post_install.sh
```

User will be prompted  if couchbase was not initialized.
Post install will take care of initializing the database with what
the anyplace service expects to find. The initialization is done
according to the `.env` file.

---

# Advanced modifications:

## External SSL certificate (optional):
Anyplace Docker will automatically create a certificate authority,
and securely sign for the domain that it is requested.
This ensures that even in private, isolated scenarios there is
encryption between the server and the clients.

However in a public scenario you might want to use an externally provided certificate, that is trusted by Operating Systems and Browsers.
To do so, you should place all externally generated certificates at:
`.anyplace/server/play/cert/`

These files are: `.crt`, `.crs`, `.jks`, and `password`.
You should name them according to the values you have set in the `.env` file.

For example:
- `<CERT_HOSTNAME>.<CERT_DOMAIN>.crt`
- `<CERT_HOSTNAME>.<CERT_DOMAIN>.crs`
- `<CERT_HOSTNAME>.<CERT_DOMAIN>.jks`
- `password`

Then execute:
`./cache_cert.sh`

## External Filesystem (optional):
The `fs` directory can be `symlinked` to another directory,
so you can link for example a distributed filesystem.

###### Steps:
a. Delete the `fs` directory and its subfolders.
b. Symlink it like the following example:
```
ln -s ~/.anyplace/server/fs /media/DFS/
```
c. run `pre_install.sh` again to recreate the fs directories (if DFS was empty)

##### External Couchbase (optional):
a. Setup the relevant fields in .env, so it can connect to the provided couchbase server/cluster.
Then build & start only the anyplace container:
```
docker-compose build anyplace
docker-compose up anyplace
```

In case you want to provide already generated internal structures of couchbase, you can do so by creating a `symlink` from the following directory:
~/.anyplace/server/db/couchbase/data:

By default couchbase places internal structures at:
`/opt/couchbase/var/lib/couchbase/data`

NOTE: this is an experimental feature.

## Deploy Cluster:
This assumes that there is an external Couchbase service to connect to,
and optionally an external DFS `symlink`'ed to: ~/anyplace/server/fs

### Create and run anyplace
```
docker-compose build anyplace
docker-compose up anyplace
```

---

# Architectural Overview
<p align="center">
<img src="https://gist.githubusercontent.com/Paschalis/09af7e45f069582a3e97a7021be2a729/raw/468c2e6caaf4afc4c95ec940181b21416ee0ad4a/architecture-new-ppt-1.jpg"
width="800">
</p>

# Links
## [Contributing](CONTRIBUTING.md)

---
## [License](LICENSE.txt)

