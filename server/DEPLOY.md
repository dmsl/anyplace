# DEPLOY:

## 1. PRODUCTION:
<details>
<summary>
Deploy Production
</summary>

1. **Download Anyplace**:
   
    For the latest binaries visit our Github releases, and `unzip`, e.g, using:

    ```
    $ unzip anyplace_<version>.zip
    ```
    Or simply follow the standard `sbt` instructions after cloning the project.


2. **Generate application key**:
   
   This is now required for security purposes.  
   Generate one using the `sbt shell` (inside IntelliJ):
   ```
   playGenerateSecret
   ```

    [Read more](https://www.playframework.com/documentation/2.8.x/ApplicationSecret).


3. **Update [conf/application.conf](./conf/application.conf)**:
- `application.secret` - previous step
- `server.address` - The URL the server is running on.
- database settings (mongodb, etc)
- filesystem settings (optional):
  + `floorPlansRootDir`: directory of the floopr plans
  + `radioMapRawDir`: directory for the raw radiomap data
  + `radioMapFrozenDir`: directory for the frozen radiomaps
  + `tilerRootDir`: directory of the tiler

4. **Install [tiler dependencies](anyplace_tiler/README.md)**:

5. **Run anyplace service**:

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

</details>

## 2. DEVELOPMENT:
<details>
<summary>
Deploy Development
</summary>

Instead of pushing compiling and testing everything on a local machine,
these set of scripts send the code changes to a remote machine.
Those are then compile remotely

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


## 3. [Docker](../../docker/README.md) (outdated)
<details>
<summary>
Deploy Docker
</summary>
The backend's codebase has changed significantly.
All of it was rewritten to MongoDB, dependencies has changed,
and the docker image is now outdated.  
The backend now uses the latest version of `Play`, `Scala`,` sbt`, making its deployment easier.

Any contributions from the community on docker are welcome.

</details>

---
# Testing:
<details>
<summary>
Testing Instructions
</summary>

Just open a browser and test the following URLs:
```bash
$ http://localhost:9000/viewer
$ http://localhost:9000/architect
$ http://localhost:9000/developers
```

You can obviously setup the service on an IP/Domain name by configuring the underlying
Operating System with standard unix, mac or windows configurations.
</details>
