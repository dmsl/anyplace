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

## 3. Open project in Android studio
- open the folder: `android-new` as a project
- this will have the following components: `lib-android`, `lib-core`, `smas`,  `navigator` and `logger`
- these are detailed below in the guide (in Structure)

</details>

---

## Asset files [Optional]:
<details><summary></summary>

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

</details>


---

# IDE Options:


## Logcat:
<details><summary></summary>

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

---

# Code Structure:
<details><summary></summary>

## lib-core:
- contains the retrofit code and all interactions with the relevant APIs
  - for Anyplace and for SMAS
- it has **nothing** that is bounded to Android
- It is a pure java/kotlin library module, that is included by android-lib


## android-lib:
- almost all of the code is here
  - the remaining modules are mostly "clean"
- originally it was supposed to have only Anyplace-related code
- due to the addition of CV localization methods in SMAS, it also contains all of the SMAS logic

# smas:
- smart alert system

# navigator:
- it's the SMAS code base with the following functionality removed:
  - chat and alerts

# logger:
- not up to date..

</details>

---

# WHAT TO STUDY:
<details><summary></summary>

## 0. Android Studio IDE
- Learn the shortcuts, see tutorials how to make the most of the IDE.

## 1. Kotlin
See tutorials and understand the basics of kotlin
- including:
  - by lazy init, lateinit, and initialization in general
  - Flows
  - Extension Functions
  - Coroutines

## 2. Android
- Application, Activity, Fragments
- logcat, how to filter logcat, how to debug
  - gradle, ConstraintLayouts
- MVVM Pattern
  - View Models
  - DataStores
- how to use AppInspection to ispect DBs
- how to inspect files of app:
  - eg Cache stores in: /data/data/<PACKAGE>/
- Dependency Injection (Hilt/Dagger)
  - and in general: what it is, what it does, why it's needed
- UI:
  -data binding, view binding
  - jetpack compose (used in SmasChatActivity)
  - BindingAdapter
  - RecyclerView
  - NavController: SpaceListFragment (SpaceSelectorActivity)
- DataStore, and how it is connected to activities and XML files
  - e.g. [SettingsCvActivity] is connected to [CvMapDataStore]
  - study helper methods in there, e.g., setPercentageInput
- Retrofit: for Http requests
- Room: for an SQLite interface (also learn how to use properly the db-related features of the IDE)

### General tips about code:
- SomethingWrapper: (e.g. GmapWrapper, or LevelWrapper)
  - an object (probably a data class) is wrapped to provide additional functionality
  - this leaves the original object clean (a data class), so it can be better in many scenarios
    - serialized/deserialized, used by Retrofit, etc


---

## Some Important classes:
- CvMapActivity
  - child: CVLog, CvSMAS
-  
  
## AnyplaceApp.loadSpace:
- loads a Space and all Floors json objects, that are fetched from anyplace
- These must be loaded before anything else can happen..  

</details>


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


## What is the flow from app open until the main activity (SMAS/Navigator or logger) opens?
- Assuming there was a successful SMAS & anyplace login, and no building was elected yet
- [SpaceAdaptor] will select a building (or SMAS pick one hardcoded)
- Then [AnyplaceApp.loadSpace] will load some necessary json files
- when from SMAS/logger: GmapWrapper setup -> loadSpace will do this
- otherwise: the last building will be used (stored in DataStore), and the last activity will open
  (also stored in DataStore)
  
## Where are the SQLite queries of SMAS?
- View are here: cy/ac/ucy/cs/anyplace/lib/android/data/smas/db/Views.kt
- Queries here:  cy/ac/ucy/cs/anyplace/lib/android/data/smas/db/smasQueries.kt
