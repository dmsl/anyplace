# lib
Android Gradle library.
Most of the android legacy code will go here.
(some will go to ../core/lib pure Java library)

# lib-core
The core java library used as a dependency.

# Development
To pull submodules after the initial `git clone`:
```bash
git submodule update --init --recursive
```
This is required for including locally the subprojects `anyplace-lib-core`, `anyplace-lib-android`.