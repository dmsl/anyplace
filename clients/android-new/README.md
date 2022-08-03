# SETUP:
<details><summary></summary>

## 1. Setup private keys:
You need to set some of the below secret keys in [local.properties](./local.properties)
- SERVER_GOOGLE_OAUTH_CLIENT_ID
- MAPS_API_KEY
- SMAS_API_KEY: bearer token for accessing the SMAS backend API

## 2. Pulling submodules:
This repo uses separate git repositories (called submodules) for `lib-core`,
and `lib` (android-lib).
If you haven't pulled those with the`git clone`, you can do now using:
```bash
git submodule update --init --recursive
```
This is required for including Activitylocally the subprojects `anyplace-lib-core`, `anyplace-lib-android`.

## NOTE: Checking out different branches?
If you checkout different branches, make sure to checkout the relevant commit for:
- `lib-core`, and `lib`

---

## Asset files [Optional]:
If the `DBG.USE_ASSETS` option is set, then you need to provide some assets for the TFlite models
There might be optionally some further `json` files that are used in assets also
(you may find traces of those in code().
- `AssetReader` has some examples of how the `json` files are used
- `DetectionModel` enum class contains the paths for the model files
  - NOTE: `CvModelFilesGetNW` downloads the models remotely, so 

### Step 1: create the assets folder:
1. at `lib-android` module:
   - right click -> new directory -> `src/main/assets`
2. Place in there any assets:
   - models
   - demo.spaces (optionally, some `json` files)
     - you can use `AssetReader` to load from these
     
```aidl
.
├── demo
│   └── spaces
│       ├── building
│       └── vessel
│           └── flavia
│               ├── floors.json
│               └── space.json
└── models
    ├── coco
    │   ├── model.tflite
    │   └── obj.names
    └── ucyco
        ├── model.tflite
        └── obj.names
```

---

# IDE Options:


## Logcat:
When you run the app, it is suggested to filter the logs (select `debuggable process`).
You may additionally import this filtering in [.idea/workspace.xml](.idea/workspace.xml),
as a direct child of the project tag:

```xml
<component name="AndroidConfiguredLogFilters">
    <filters>
      <filter>
        <option name="logLevel" value="verbose" />
        <option name="logMessagePattern" value="" />
        <option name="logTagPattern" value="^(anyplace|anyplace/.*|ap_*)$" />
        <option name="name" value="log-anyplace" />
        <option name="packageNamePattern" value="cy.ac.ucy.cs.anyplace.*" />
      </filter>
    </filters>
  </component>
```

</details>



# Code Structure:

<details><summary></summary>

# lib-core (git submodule)
The core java library used as a dependency.
Use https://jitpack.io/#dmsl/anyplace-lib-core to get the artifact.

# lib (git submodule)
Android Gradle library.
Most of the android legacy code will go here.
(some will go to ../core/lib pure Java library)

# logger

# smas

</details>


# STUDY:

## 0. Android Studio IDE
Learn the shortcuts, see tutorials how to make the most of the IDE.

## 1. Kotlin
See tutorials and understand the basics of kotlin
- including:
  - by lazy init, lateinit, and initialization in general
  - Flows
  - Extension Functions
  - Coroutines

  
---


## Important classes:
- CvMapActivity
  - child: CVLog, CvSMAS..
  
## AnyplaceApp.loadSpace:
- loads a Space and all Floors json objects, that are fetched from anyplace
- These must be loaded before anything else can happen..  


---

# FAQ:

## How to issue notifications:
- [app.bar] is a [Snackbar]. Try to use that, if you can bind it to activity (using [setMainView])
- If it cannot be binded to activity, use [app.showToast]

## Where is [GoogleMap] object located:
- There is a [GmapWraapper] on top of [GoogleMap]
  - the wrapper is in CvViewModel object: VM.ui.map
  - the object: VM.ui.map.obj


## Google login:
- See tutorials and official guide
- Must create a key at https://console.cloud.google.com/apis/