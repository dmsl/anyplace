# SETUP INTELLIJ IDE:

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
│   ├── models.oauth
│   ├── modules.floor_module
│   ├── modules.radiomapserver
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
* [`MappingController`](app/controllers/MappingController.java) - Handles all of the interactions coming from Anyplace Architect, like the [CRUD](https://en.wikipedia.org/wiki/Create,_read,_update_and_delete) operations on buildings/floors/POIs.
* [`PositioningController`](app/controllers/PositioningController.java) - Handles indoor positioning items like RSS logs and radiomaps. (_Also contains some experimental positioning methods using magnetic readings, which is work in progress._)
* [`NavigationController`](app/controllers/NavigationController.java) - Provides indoor navigation paths between two POIs. Uses [Dijkstra's algorithm](https://en.wikipedia.org/wiki/Dijkstra%27s_algorithm) to find shortest paths.

##### [Radiomap Server](app/modules/radiomapserver)
Provides two classes that gather RSS logs from the database and compiles
the Radiomap that is served to the Anyplace Navigator for indoors positioning.

##### [Anyplace Tiler](anyplace_tiler)
A set of scripts that cut the floor plans into tiles that can fit on Google Maps, for different zoom levels.

##### [Floor Module](app/modules/floor_module)
This class provides floor detection functionality for detecting when a user has changed floors, using the Navigator Android app.

##### Web Apps
* **Anyplace Viewer & Architect** - We serve Anyplace Viewer & Architect through the [`public/`](public) folder of Play, where the [static assets](https://www.playframework.com/documentation/2.2.x/Assets) of the project reside. Play Framework provides out of the box perfomance features like compression (gzip) and caching for static assets.
* **Anyplace Developers** - This is the page for Anyplace API's interactive documentation, built with [Dart](https://www.dartlang.org/). It resides in the [`public`](public) directory and served through the [`AnyplaceWebApps`](app/controllers/AnyplaceWebApps.java) controller.


# SBT INTERFACE:
As of 4.2 sbt interface is available from IntelliJ (and terminal)

