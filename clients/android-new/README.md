# lib
Android Gradle library.
Most of the android legacy code will go here.
(some will go to ../core/lib pure Java library)

# lib-core
The core java library used as a dependency.
Use https://jitpack.io/#dmsl/anyplace-lib-core to get the artifact.

# Development
To pull submodules after the initial `git clone`:
```bash
git submodule update --init --recursive
```
This is required for including locally the subprojects `anyplace-lib-core`, `anyplace-lib-android`.

### Google API KEY:
`string.xml` uses a `maps_api_key`, which is a secure property from `build.gradle`.

This is stored to a file on home directory named:
`MAPS_API_KEY`

### Server Google OAuth Client ID:
Similarly it's:
`SERVER_GOOGLE_OAUTH_CLIENT_ID`
