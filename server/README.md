# Anyplace Server v3.0a (Alpha release)
This is a latest version of the Anyplace backend, which has been ported to tha scala language and that also supports all the latest versions of its underlying software layers (i.e., couchbase 4.5 and play framework 2.5).

# Anyplace v3.0a for testers

You can run Anyplace v3.0a in two modes: a) Cloud Mode, where you host the backend but not the datastore and b) Hosted Mode, where you run the backend and the datastore.
 
## Cloud Mode (Testing only) 
 In this mode you host the backend but not the datastore.
 1. **Download Anyplace v3.0a:** To proceed just download the following zip file [https://github.com/dmsl/anyplace/archive/v3.zip](https://github.com/dmsl/anyplace/archive/v3.zip).   
 2. **Run Anyplace v3.0a:** Follow the instructions below ("How to run Anyplace v3.0a?").
   
 **Note:** Please be aware that in this mode you will connect a replica READ-ONLY datastore of Anyplace on http://194.42.17.165. This datastore has been setup mainly for testing (i.e., proof-of-concept validation). Its not fully operational as it doesn't allow additions. 
 
 If you want to observe the couchbase 4.5 administrative interface for this mode use the following details. 
  
 * `url: http://194.42.17.165:8091/ui/index.html#/overview`
 * `username: open`
 * `password: openopen`

## Hosted Mode (Fully operational)
  In this mode you run the backend and the datastore.
  1. **Install Couchbase v4.5:** Download the latest Couchbase Server Community Edition from [https://www.couchbase.com/downloads](https://www.couchbase.com/downloads)  
  2. **Download Anyplace v3.0a:** Obtain the zip file as this is described in cloud mode.
  3. **Configure Anyplace v3.0a:**  

+ Fill in the paremeters in `conf/application.conf` according to the development or production environment.
    * `application.secret` - This is a Play Framework parameter. You can see its purpose and how to generate one in Play Framework's [documentation](https://www.playframework.com/documentation/2.5.x/ApplicationSecret).
    * `server.address` - The URL the server is running on. (e.g. `http://anyplace.cs.ucy.ac.cy`)
    * `couchbase.hostname` - The URL where the Couchbase instance is running. (e.g. `http://db.<<domain>>.com`)
    * `couchbase.port` - Couchbase's port. The default is `8092`.
    * `couchbase.bucket` - The name of the Couchbase bucket where the Anyplace documents reside.
    * `couchbase.password` - The password to access the DB instance.
+ Make sure a Couchbase instance is running, with the [Production Views](https://developer.couchbase.com/documentation/server/4.6/introduction/whats-new.html) the server invokes.
You can use the automated script (`create-views.sh`) in order to create the views under the [`anyplace_views`](anyplace_views) directory.
You need to set the username and the password for your couchbase instance.  
    * `USERNAME=""` - This is the administrator's username for the couchbase instance.
    * `PASSWORD=""` - This is the administrator's password for the couchbase instance.
    * `BUCKET=""` - This is the bucket for the couchbase instance.

**Important**: As with all passwords, this should be kept a secret. Do not push it to a Version Control System.
 
## How to run Anyplace v3.0a?
1. You can run the distribution package under the  [`target/universal`](target/universal) directory.
    * Unzip the *.zip file
    * Go to the `bin` directory
    * `chmod +x` ...
    * Run the script (Linux based systems) or the batch file (Windows)

# Anyplace v3.0a for developers

## How to setup Anyplace v3.0a in you IDE?
1. You can run the project locally on your personal computer using the [IntelliJ IDEA](https://www.jetbrains.com/idea/download/)
**Important**: In order to run the Play project you need download and install the Ultimate edition.


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
