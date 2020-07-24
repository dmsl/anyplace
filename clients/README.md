# TODO CA: task 0
1. Make core/lib a pure JAVA (compatible both in Android and desktop JVMs).

2. core/cli: put all stuff that will be removed from core/lib here
    - core/cli/bin: put a jar file here

3. include core/lib in android-new/lib

4. `hello world` on android-new/demo
    - commit !

5. merging
    - remove stuff from android/ to:
        + core/lib, and
        + android-new/lib
    - Deprecate stuff (while moving) from android/
        + e.g. replace ActionBarSherlock with build in libs..
            * for exaple: SherlockActivity -> SupportActivity  (android support lib)
                - see tutorials on migrating/deprecating Sherlock



# TODO CA Put a bash script in linux/macos from 421 on linux and macos
from android:
    - most stuff will end up:
        + core/lib
        + android-new/lib


Anyplace Clients
================
Different clients for Anyplace.

## android
Android client with Navigator and Logger.
Developed by Timotheos.

## android-legacy
Legacy code from old Android client.

## bin
Precompiled binaries for the clients.

## core
### lib
Gradle-based Java core library that can be included in both Android and desktop JVMs.

### cli
Command line interface for fully-fledged desktops.
Is used to create CLI clients for Windows, Linux, macOS.

### library
Gradle-based Android library for anyplace.
Uses core and implements new features in android java.

## robotos
Robot OS client demo.

## Deprecated
Clients that won't be further developed.

### windows-phone
Legacy Windows client code.

### ios 
Legacy iOS client.

## macos
Demo bash script for macOS.

## linux
Demo bash script for Linux.

## windows
PowerShell script for Windows (placeholder).

CURRENTLY WORKING
---
* Christakis Achilleos

OLD TEAM:
* Marcos Antonios Charalambous
* Constandinos Demetriou
* Christakis Achilleos

TEAM
---
* [https://anyplace.cs.ucy.ac.cy/](https://anyplace.cs.ucy.ac.cy/)
