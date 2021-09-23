# DEPLOY TUTORIAL

## 1. PRODUCTION:
<details>
<summary>
Deploy Production
</summary>

### 1. **Download Anyplace**:
   
    For the latest binaries visit our Github releases, and `unzip`, e.g, using:

    ```
    $ unzip anyplace_<version>.zip
    ```
    Or simply follow the standard `sbt` instructions after cloning the project.


### 2. **Generate application key**:
   
   This is now required for security purposes.  
   Generate one using the `sbt shell` (inside IntelliJ):
   ```
   playGenerateSecret
   ```

    [Read more](https://www.playframework.com/documentation/2.8.x/ApplicationSecret).


### 3. **Update [configuration](./conf/)**:
Configuration is split amongst these files:

#### 3.1 [conf/.app.private.conf](./conf/app.private.example.conf)
Private configuration. Never share online.
Create this file by duplicating [app.private.example.conf](./conf/app.private.example.conf) 
and adapting as necessary.  

- `application.secret` - Generated from Step 2.
- `server.address` - The URL the server is running on.

- `password.salt` - used for password encryption
- `password.pepper` - used for password encryption

- filesystem settings (optional):
  + `floorPlansRootDir`: directory of the floorplans
  + `radioMapRawDir`: directory for the raw radiomap data
  + `radioMapFrozenDir`: directory for the frozen radiomaps
  + `tilerRootDir`: directory of the tiler

#### 3.2 [conf/app.base.conf](./conf/app.base.conf)
- `app.base.conf`: put the base configuration. Don't reference any variables as it is used by [build.sbt](build.sbt).

#### 3.3 [conf/app.play.conf](./conf/app.play.conf)
Contains the remaining of the Play configuration.

### 4. **Install [tiler dependencies](anyplace_tiler/README.md)**:

### 5. **Run anyplace service**:

   **Unix/Linux**:
    ```bash
    # LINUX / MACOSX
    $ cd anyplace_v3/bin
    $ chmod +x anyplace
    $ ./anyplace  (alternatively use: $ nohup ./anyplace > anyplace.log 2>&1 )
    # To stop press Ctrl-C or kill the respective process
    ```

    **Windows**:
    ```bash
    $ Go to the folder you unzipped in the prior step, then go to "bin"
    $ Double click  anyplace_v3.bat
    # To stop press Ctrl-C or kill the respective process through the task manager
    ``` 

### 6. **SSL and Cluster Configuration**:
+ Install a free certificate from
  [letsencrypt.org](https://letsencrypt.org/) on your Anyplace Server 
  to obtain a secure https connection. SSL is only optional for 
  web functionality. For Android, SSL is a prerequisite!


+ (Optional) Install a free load balancer from 
  [HAProxy](http://www.haproxy.org/) to scale your installation 
  to multiple Anyplace servers. 
  In case of Anyplace cluster configuration, 
  please install the certificate on the Load Balancer.

***



</details>

## 2. DEVELOPMENT:
<details>
<summary>
Deploy Development
</summary>

Instead of pushing compiling and testing everything on a local machine,
these set of scripts send the code changes to a remote machine.
Those are then compile remotely

The below scripts run locally and sync the local files to a remote.  
There needs to be some remote scripts as well:
- one that does `sbt "run PORT"`
- three that compile the web apps and watch for changes:
  + `grunt` (for viewer, viewerCampus, and architect)
  + see [./public](public/README.md)


## 1. Setup
Create `deploy/config.sh` from [deploy/config.example.sh](./deploy/config.example.sh)

## 2. Push private configuration and install dependencies:

#### 1.1 [deploy/push_private.conf.sh](./deploy/push_private.conf.sh):
Pushes the app.private.remote.conf, which contains the passwords, etc.

#### 1.2 REMOTELY: Compile all dependencies
See  [./public](public/README.md) for more.  
Must run remotely.

## 3. Sync any new changes
`./deploy/watchdog.sh` can do this automatically.

---

# Used scripts:

#### [deploy/watchdog.sh](./deploy/watchdog.sh):
Watches for file changes and automatically calls sync.sh

#### [deploy/sync.sh](./deploy/sync.sh):
Wrapper over push_code.sh
Also makes a curl request to trigger an automatic recompilation of the sources (sbt).

#### [deploy/push_code.sh](./deploy/push_code.sh):
Pushes any relevant Scala or JS code.

### TROUBLESHOOTING 
#### `\r` Windows issue:
```
sed -i 's/\r$//' *.sh
# ignore new changes in commits 
git update-index --assume-unchanged config.sh push_code.sh watchdog.sh sync.sh
```

</details>

## 3. LOCAL:
<details>
<summary>
Local Deployment
</summary>

Just open a browser and test the following URLs:
```bash
$ http://localhost:9000/viewer
$ http://localhost:9000/architect
$ http://localhost:9000/developers
```

You can obviously setup the service on an IP/Domain name by configuring the underlying
Operating System with standard unix, mac or windows configurations.

For the compilation of the web apps ([architect](public/anyplace_architect),
[viewer](public/anyplace_viewer), [viewerCampus](public/anyplace_viewer_campus)), please see this instructions:
- [./public](public/README.md)

</details>


## 4. [~~DOCKER~~](../docker/README.md) (deprecated)
<details>
<summary>
Docker Deployment (outdated)
</summary>
The backend's codebase has changed significantly.
All of it was rewritten to MongoDB, dependencies has changed,
and the docker image is now outdated.  

The backend now uses the latest version of 
`Play`, `Scala`,` sbt`, making its deployment easier.
Compilations are faster as incremental builds can now be used by the more recent `sbt` version.

Any contributions from the community on `docker` are welcome.

</details>

---

## 5. Testing: with [POSTMAN](/database/postman/README.md)
