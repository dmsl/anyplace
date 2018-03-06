# Anyplace v3.2 Installation Notes
This is a latest version of the Anyplace backend, which has been ported to tha scala language and that also supports all the latest versions of its underlying software layers (i.e., it has been tested with couchbase 4.5 and play framework 2.5).

# Anyplace v3.2 for administrators

## Setup/Configuration

  1. **Install & Configure Couchbase** Download the latest Couchbase Server Community Edition from [https://www.couchbase.com/downloads](https://www.couchbase.com/downloads). Anyplace v3.2 has been tested with Couchbase 4.5, but compatibility with later versions is expected.
  
  2. **Download Anyplace v3.2:**
 
    $ wget https://anyplace.cs.ucy.ac.cy/downloads/anyplace_v3.zip  
    #if you don't have wget, just download the file with a browser)
    
    $ unzip anyplace_v3.zip
    #if you don't have unzip, just use any unzip tool (winzip, etc.)

  3. **Link Couchbase to Anyplace**
    Now you have to change the default configurations. Please follow the below instructions before running Anyplace
  
    + Fill in the paremeters in `conf/application.conf` according to the development or production environment.
        * `application.secret` - This is a Play Framework parameter. You can see its purpose and how to generate one in Play Framework's [documentation](https://www.playframework.com/documentation/2.5.x/ApplicationSecret).
        * `server.address` - The URL the server is running on. (e.g. `http://anyplace.cs.ucy.ac.cy`)
        * `server.port` - The port the server is running on. (e.g. `80 (htpp), 443 (https) `)     
        * `couchbase.hostname` - The URL where the Couchbase instance is running. (e.g. `http://db.<<domain>>.com`)
        * `couchbase.port` - Couchbase's port. The default is `8091`.
        * `couchbase.bucket` - The name of the Couchbase bucket where the Anyplace documents reside.
        * `couchbase.password` - The password to access the DB instance.
    + Make sure a Couchbase instance is running, with the [Production Views](https://developer.couchbase.com/documentation/server/4.6/introduction/whats-new.html) the server invokes.
    You can use the automated script (`create-views.sh`) in order to create the views under the [`anyplace_views`](anyplace_views) directory.
    You need to set the username and the password for your couchbase instance.  
        * `USERNAME=""` - This is the administrator's username for the couchbase instance.
        * `PASSWORD=""` - This is the administrator's password for the couchbase instance.
        * `BUCKET=""` - This is the bucket for the couchbase instance.
    + Important: As with all passwords, this should be kept a secret. 
    + Note: If you are getting Authentication Failure errors, then create a user with the bucket name. This is a new change from Couchbase 5.0 (V5.0 - NEW Role-Based Authentication - Bucket Passwords, etc)(https://forums.couchbase.com/t/v5-0-new-role-based-authentication-bucket-passwords-etc/14637)
# Running

    You can now launch the Anyplace service:
    # LINUX / MACOSX 
    $ cd anyplace_v3/bin
    $ chmod +x anyplace_v3
    $ ./anyplace_v3  (alternatively use: $ nohup ./anyplace_v3 > anyplace.log 2>&1 )
    # To stop press Ctrl-C or kill the respective process

    # WINDOWS
    $ Go to the folder you unzipped in the prior step, then go to "bin" 
    $ Double click  anyplace_v3.bat
    # To stop press Ctrl-C or kill the respective process through the task manager
    
## Testing
    Just open a browser and test the following URLs.

    $ http://localhost:9000/viewer
    $ http://localhost:9000/architect
    $ http://localhost:9000/developers

    You can obviously setup the service on an IP/Domain name by configuring the underlying Operating System with standard unix, mac or windows configurations.
    
# Anyplace v3 for developers

## How to setup Anyplace v3 in you IDE?
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
