# Anyplace Server:
Anyplace Server code base has been updated to the latest versions:
- Play: `2.8.x`
- Scala: `2.13.x`
- sbt: `1.3.x`

The endpoints/database have been completely rewritten to MongoDB.
**Couchbase will soon be completely removed!**

The previous version (4.0) is available as a prebuilt with docker (/develop branch).

***

# DEPLOY:

<details>
<summary>
Deploy Instructions
</summary>
1. **Install & Configure Couchbase**:   (WILL BE REMOVED SOON)
Download the latest Couchbase Server Community Edition from
     [https://www.couchbase.com/downloads](https://www.couchbase.com/downloads).
Anyplace v4.x has been tested with Couchbase 4.5 and 6.0.0. Version 6.5 breaks compatibility.
  
2. **Download Anyplace:**
For the latest binaries visit our Github releases, and `unzip`, e.g, using:

```
$ unzip anyplace_<version>.zip
```
3. **Generate application key**:
This is now required for security purposes.
Generate one using the `sbt shell` (inside IntelliJ):
`playGenerateSecret`

[Read more](https://www.playframework.com/documentation/2.8.x/ApplicationSecret).

4. **Update `[conf/application.conf](./conf/application.conf)`**
- `application.secret` - See step 3.
- `server.address` - The URL the server is running on. (e.g. `http://anyplace.cs.ucy.ac.cy`)
- database settings (mongodb, etc)
- filesystem settings (optional):
        + `floorPlansRootDir`: directory of the floopr plans
        + `radioMapRawDir`: directory for the raw radiomap data
        + `radioMapFrozenDir`: directory for the frozen radiomaps
        + `tilerRootDir`: directory of the tiler
        + `crlbsDir`: directory for the ACCES map data, generated using the  Cramer-Rao lower bound (CRLBS) algorithm.

5. Install [tiler dependencies] (anyplace_tiler/README.md)
</details>

## Run Anyplace  service:
```bash
# LINUX / MACOSX
$ cd anyplace_v3/bin
$ chmod +x anyplace
$ ./anyplace  (alternatively use: $ nohup ./anyplace > anyplace.log 2>&1 )
# To stop press Ctrl-C or kill the respective process
```

# WINDOWS
```bash
$ Go to the folder you unzipped in the prior step, then go to "bin"
$ Double click  anyplace_v3.bat
# To stop press Ctrl-C or kill the respective process through the task manager
```

## Testing
Just open a browser and test the following URLs:
```bash
$ http://localhost:9000/viewer
$ http://localhost:9000/architect
$ http://localhost:9000/developers
```

You can obviously setup the service on an IP/Domain name by configuring the underlying
Operating System with standard unix, mac or windows configurations.

## Connecting the Anyplace Android Client
+ Download the Android Client from the Play Store: https://play.google.com/store/apps/details?id=com.dmsl.anyplace&hl=en (https://play.google.com/store/apps/details?id=com.dmsl.anyplace&hl=en)
+ Under settings in the Android App, change the DNS of the Anyplace server to your own server IP/DNS.
+ (Optional) Download and recompile the Android client  and apply your default settings. (Note: Requires a seperate Android Developer Account.
+ IMPORTANT: You have to install an SSL certificate on your server to allow the Android Client to connect to your server.

## SSL and Cluster Configurations for Anyplace Server 
+ Install a free certificate from https://letsencrypt.org/ on your Anyplace Server to obtain a secure https connection. SSL is only optional for web functionality. For Android, SSL is a prerequisite!

+ (Optional) Install a free load balancer from [HAProxy](http://www.haproxy.org/) to scale your installation to multiple Anplace servers. In case of Anyplace cluster configuration, please install the certificate on the load balancer.

***

# FOR DEVELOPERS:

## SETUP INTELLIJ IDE:
1. You can run the project locally on your personal computer using the [IntelliJ IDEA](https://www.jetbrains.com/idea/download/)
+ Download the  [IntelliJ IDEA](https://www.jetbrains.com/idea/download/) (The Community Edition is free but if you are a student we recommend taking advantage of the Ultimate Edition which is free for 1 year - this has built-in integration for Play Framework debugging)
+ Install the Scala plugin during the installation process
+ Download the project from [GitHub](https://github.com/dmsl/anyplace/archive/master.zip)
+ Extract the `develop.zip` file
+ Click `Open` under the `File` menu on the IntelliJ IDEA
+ Choose the `anyplace-develop/server` directory of the extracted file (The directory icon should be marked)
+ Check the `SBT sources` checkbox
+ Setup the JAVA `jdk` if necessary
+ Click `OK` (It should start downloading the required libraries ~10-15 mins)  
+ If you are using the Community Edition you need to do the following three additional steps
    * Click the dropdown menu on the right side of the IDE to `Edit configuration`
    * Add a new configuration click the `+` symbol
    * Choose `SBT Task` and then write "run" in the `tasks` fields
+ (Tentatively) Refresh Viewer Packages: https://github.com/dmsl/anyplace/tree/master/server/public/anyplace_viewer
+ (Tentatively) Refresh Architect Packages:  https://github.com/dmsl/anyplace/tree/master/server/public/anyplace_architect
+ Ignore the directories which are generated with grunt. These essentially put all JavaScript into a single file,
  along with other artifacts like CSS and images. They should never be edited, and this excludes them from IDE's search.
    - Exclude using:
        -> `right click` on the sidebar folder
        -> `Mark Directory as`
        -> `Excluded`
    - Directories to exclude:
        + public/anyplace_architect/build
        + public/anyplace_viewer/build
        + public/anyplace_viewer_campus/build
+ Done!

**Important**: In order to fully support the Play project you need download and install the Ultimate edition.
**Important**: You need to have installed the JAVA enviroment.

## Build
1. You can build your own distribution package as described on [Play Framework Documentation](https://www.playframework.com/documentation/2.5.x/Deploying).
    * Open the SBT console
    * Run `dist` to create the distribution package under the [`target/universal`](target/universal) directory.
    
## Project Structure & Description  
```
.
├── anyplace_tiler
├── anyplace_views
├── app
│   ├── controllers
│   ├── datasources
│   ├── db_models
│   ├── floor_module
│   ├── oauth
│   ├── radiomapserver
│   └── utils
├── conf
├── lib
├── project
└── public
    ├── anyplace_architect
    ├── anyplace_viewer
    └── anyplace_developers
└── target
    ├── resolution-cache
    ├── scala-2.11
    ├── streams
    ├── universal
    └── web
```

##### API Controllers
* [`AnyplaceMapping`](app/controllers/AnyplaceMapping.java) - Handles all of the interactions coming from Anyplace Architect, like the [CRUD](https://en.wikipedia.org/wiki/Create,_read,_update_and_delete) operations on buildings/floors/POIs. 
* [`AnyplacePosition`](app/controllers/AnyplacePosition.java) - Handles indoor positioning items like RSS logs and radiomaps. (_Also contains some experimental positioning methods using magnetic readings, which is work in progress._) 
* [`AnyplaceNavigation`](app/controllers/AnyplaceNavigation.java) - Provides indoor navigation paths between two POIs. Uses [Dijkstra's algorithm](https://en.wikipedia.org/wiki/Dijkstra%27s_algorithm) to find shortest paths.

##### [Radiomap Server](app/radiomapserver)  
Provides two classes that gather RSS logs from Couchbase and compiles the Radiomap that is served to the Anyplace Navigator for indoors positioning.

##### [Anyplace Tiler](anyplace_tiler)  
A set of scripts that cut the floor plans into tiles that can fit on Google Maps, for different zoom levels.

##### [Floor Module](app/floor_module)
This class provides floor detection functionality for detecting when a user has changed floors, using the Navigator Android app.

##### Web Apps
* **Anyplace Viewer & Architect** - We serve Anyplace Viewer & Architect through the [`public/`](public) folder of Play, where the [static assets](https://www.playframework.com/documentation/2.2.x/Assets) of the project reside. Play Framework provides out of the box perfomance features like compression (gzip) and caching for static assets.
* **Anyplace Developers** - This is the page for Anyplace API's interactive documentation, built with [Dart](https://www.dartlang.org/). It resides in the [`public`](public) directory and served through the [`AnyplaceWebApps`](app/controllers/AnyplaceWebApps.java) controller.


## Geolocation using `InfluxDB`:
<details open>
<summary>
Influx Documentation (will be removed)
</summary>
TODO:PM remove influx
Anyplace provides endpoints dedicated to storing and retrieving geo-points:

|path|kind|description|
|:-|:-:|-:|
/anyplace/geolocation/insert| POST | insert a point in the database
/anyplace/geolocation/range_lookup| POST | retrieve all points in a timespan in a square of two points
/anyplace/geolocation/distance_lookup | POST | retrieve all points in a timespan within some distance in KM from the point

### Storing Geopoints

__path__: /anyplace/geolocation/insert  
__input kind__: json  
__parameters__:   
```
{
    point: {
        latitude: string,
        longitude: string,
    },
    deviceID: string,
    timestamp: long
}
```
__Example__:
```bash
$ curl --header "Content-Type: application/json" \
  --request POST \
  --data '{"deviceID":"myDevicePoint", "point":{"latitude":"22.2931","longitude":"17.12911"}, "timestamp":900000333}'  http://10.16.30.47:9000/anyplace/influxdb/insert
```

### Retrieving Geopoints

#### Using two points
__path__: /anyplace/geolocation/range_lookup  
__input kind__: json  
__parameters__:   
```
{
    point1: {
        latitude: string,
        longitude: string,
    },
    point2: {
        latitude: string,
        longitude: string,
    },
    deviceID: string,
    beginTime: long,
    endTime: long
}
```
__Example__:
```bash
curl --header "Content-Type: application/json" \
  --request POST \
  --data '{"deviceID":"5","point1":{"latitude":"37.350101","longitude":"37.3501"},"point2":{"latitude":"37.35012","longitude":"37.3502"}, "beginTime":1, "endTime":10}' \
  http://10.16.30.47:9000/anyplace/influxdb/range_lookup
```
#### Using distance
__path__: /anyplace/geolocation/distance_lookup  
__input kind__: json  
__parameters__:   
```
{
    point: {
        latitude: string,
        longitude: string,
    },
    distance: double,
    deviceID: string,
    beginTime: long,
    endTime: long
}
```
__Example__:
```bash
curl --header "Content-Type: application/json" \
  --request POST \
  --data '{"deviceID":"5","point":{"latitude":"37.350101","longitude":"37.3501"},"distance":10, "beginTime":1, "endTime":10}' \
  http://10.16.30.47:9000/anyplace/influxdb/distance_lookup
```
</details>
