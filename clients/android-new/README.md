# SETUP:
<details><summary></summary>

## 1. Setup private keys:
You need to set some of the below secret keys in [local.properties](./local.properties)
- SERVER_GOOGLE_OAUTH_CLIENT_ID
- MAPS_API_KEY
- LASH_CHAT_API_KEY (for smas)

## 2. Pulling submodules:
This repo uses separate git repositories (called submodules) for `lib-core`,
and `lib` (android-lib).
If you haven't pulled those with the`git clone`, you can do now using:
```bash
git submodule update --init --recursive
```
This is required for including Activitylocally the subprojects `anyplace-lib-core`, `anyplace-lib-android`.

## 3. Checkout it different branches
If you checkout different branches, make sure to checkout the relevant commit for:
- `lib-core`, and `lib`


---

## CV Specific setup
You may need to provide assets for Deep-Learning models.
There might be optionally some further `json` files that are used in assets also
(you may find traces of those in code().
These were used for testing and also for code that was not yet fully communicating with the backend.

### Step 1: create the assets folder:
1. at `lib-android` module:
   - right click -> new directory -> `src/main/assets`
2. Place in there any assets:
   - models
   - demo.spaces (optionally)
(and optionally some `json` fiels )
demo
     
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