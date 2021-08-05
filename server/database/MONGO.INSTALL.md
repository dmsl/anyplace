# Install MongoDB Tutorial

### Environment:
This tutorial uses:
- Ubuntu Server: `18.04`
- MongoDB Community Edition Version: `5.0`

## 1. Preparation:
### 1.1 Update dependencies
```bash
sudo apt-get update -y && sudo apt-get upgrade -y
```
### 2.1 Optional: Create a new partition
We recommend storing all data on a new partition
([sample guide](https://askubuntu.com/a/932362/34777)).

When required, the rest of the tutorial will assume the below structure:
- `/data`:
    + `/data/db/mongodb/`: install MongoDB
    + `/data/db/mongodb.bac/`: backups of MongoDB
    + `/data/apfs/`
    + `/data/apfs.bac/`: backup of the raw data

Sample commands:

```bash
sudo mkdir -p /data/db/mongodb/
sudo mkdir -p /data/db/mongodb.bac/
sudo mkdir -p /data/apfs/
sudo mkdir -p /data/apfs.bac/
```

## 2. Install MongoDB
For the latest instructions see: [mongodb.com](https://docs.mongodb.com/manual/tutorial/install-mongodb-on-ubuntu/)
This tutorial will install `MongoDB 5.0`.

### 2.1. Download MongoDB:
```bash
wget -qO - https://www.mongodb.org/static/pgp/server-5.0.asc | sudo apt-key add -
```

#### 2.2. Install GNUpg
Make sure that `gnupg` is installed. For more see the
[official guide](https://docs.mongodb.com/manual/tutorial/install-mongodb-on-ubuntu/).
```bash
sudo apt-get install gnupg
```

### 2.3. Create list file
```bash
echo "deb [ arch=amd64,arm64 ] https://repo.mongodb.org/apt/ubuntu bionic/mongodb-org/5.0 multiverse" | sudo tee /etc/apt/sources.list.d/mongodb-org-5.0.list
```

### 2.4. Reload packages, and install the mongodb ones
```bash
sudo apt-get update
sudo apt-get install -y mongodb-org
```

##### 2.4.1. Prevent unintented upgrades using:
```bash
echo "mongodb-org hold" | sudo dpkg --set-selections
echo "mongodb-org-database hold" | sudo dpkg --set-selections
echo "mongodb-org-server hold" | sudo dpkg --set-selections
echo "mongodb-org-shell hold" | sudo dpkg --set-selections
echo "mongodb-org-mongos hold" | sudo dpkg --set-selections
echo "mongodb-org-tools hold" | sudo dpkg --set-selections
```

### 2.5 Optional:  Increase `ulimit`:
Sample thread: [askubuntu.com](https://askubuntu.com/a/1193851/34777)

### 2.6 Give relevant permissions:
```bash
sudo chown -R mongodb:mongodb /data/db
sudo chown -R anyplace:anyplace /data/apfs
sudo chown -R anyplace:anyplace /data/apfs.bac

# this may also be required in some cases.
# replace 27017 with your port (if you modified the default)
sudo chown mongodb:mongodb /tmp/mongodb-27017.sock
```

NOTE: `anyplace` is your local user.

### 2.7 Change paths in configuration:
Change `dbPath` and `symstemLog/path`:

```bash
# database path
storage:
  # ...
  dbPath: /data/db/mongodb/
# logs path
systemLog:
  # ...
  path: /data/db/mongodb/mongod.log
```

### 2.8 start and verify MongoDB deamon (`mongod`):
```bash
sudo service mongod start
sudo service mongod status
```

### 2.9 Verify that the port is open:
```bash
# if no output the open the port.
sudo lsof -i:27017
```

If `ufw` is used, the port can open using:
```
sudo ufw allow 27017
```

If other software interferes (e.g. a Load Balancer like [HAProxy](http://www.haproxy.org/)), 
then follow any relevant guides of the software to enable port forwarding.


## 3. Configure MongoDB:

### 3.1 Create accounts and database:
This step will create the `admin` user, `anyplace` database, and `anyplace` user.

Admin will have access to everything.
The `anyplace` user will only have access to the anyplace database.  
Never share any of these credentials.
Use the `anyplace` account and DB for your Play deployment.

```bash
mongosh authenticate
 >
 use admin
  >
  db.createUser({user: "admin", pwd: "ADMIN_PASSWORD_HERE",roles: [ { role: "userAdminAnyDatabase", db: "admin" }, "readWriteAnyDatabase" ]})
  # create anyplace user to anyplace-database:
  db.createUser({user: "anyplace", pwd: "DB_PASSWORD_HERE",roles: [ { role: "dbOwner", db: "anyplace" } ]})
```

### 3.2 Expose the IP and secure the database
To publicly access this database, append the public IP to the configuration.  

**IMPORTANT:**
> Authorization must also be enabled at this step.  
> Otherwise, the db could be hijacked.

```bash
vim /etc/mongod.conf
```
```bash
## sample configuration:
net:
  port: 27017
  bindIp: 127.0.0.1,YOUR_PUBLIC_IP
  
security:
   authorization: enabled
``` 

#### 3.3 Access MongoDB
######  3.3.1 Access using MongoDB Compass:
```bash
mongodb://$USERNAME:$PASS@$HOST:$PORT/?authSource=admin&readPreference=primary&appname=MongoDB%20Compass&directConnection=true&ssl=false
```

**NOTE:** Default MongoDB port is: `27017`

######  3.3.2 Access using MongoDB Shell (`mongo`):
On your remote machine type:
```bash
mongo --host $HOSTNAME --port $PORT --username $USERNAME --password $PASSWORD
```

***

## 4. Uninstalling:
In case any errors were introduced (see official documentation).

**Sample commands:**
```bash
sudo service mongod stop
sudo apt-get purge mongodb-org*
sudo rm -r /var/log/mongodb
sudo rm -r /var/lib/mongodb
```

## 5. Scripts:

#### initUser.py:
Not in use. Sample code for automatically creating the `anyplace` user.
This is already done by this guide.




