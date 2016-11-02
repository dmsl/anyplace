# Anyplace Server

## Requirements
1. [Play Framework](https://www.playframework.com/download#older-versions) 2.2.x
2. [Couchbase](http://www.couchbase.com/)

## Build
1. Fill in the paremeters in `conf/application.conf` according to the development or production environment.
  * `application.secret` - This is a Play Framework parameter. You can see its purpose and how to generate one in Play Framework's [documentation](https://www.playframework.com/documentation/2.3.x/ApplicationSecret).
  * `server.address` - The URL the server is running on. (e.g. `http://anyplace.cs.ucy.ac.cy`)
  * `couchbase.hostname` - The URL where the Couchbase instance is running. (e.g. `http://db.<<domain>>.com`)
  * `couchbase.port` - Couchbase's port. The default is `8092`.
  * `couchbase.bucket` - The name of the Couchbase bucket where the Anyplace documents reside.
  * `couchbase.password` - The password to acces the DB instance.  
  **Important**: As with all passwords, this should be kept a secret. Do not push it to a Version Control System.  
2. Make sure a Couchbase instance is running, with the [Production Views](http://docs.couchbase.com/admin/admin/Views/views-production.html) the server invokes. _(Anyplace's Couchbase Views will be published soon.)_  
3. _WIP_

## Run
_WIP_

## Project Structure & Description  
```
.
├── anyplace_tiler
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
├── public
│   ├── anyplace_architect
│   └── anyplace_viewer
└── web_apps
    └── anyplace_developers
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
* **Anyplace Developers** - This is the page for Anyplace API's interactive documentation, built with [Dart](https://www.dartlang.org/). It resides in the [`web_apps`](web_apps) directory and served through the [`AnyplaceWebApps`](app/controllers/AnyplaceWebApps.java) controller.
