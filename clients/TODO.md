## TODO CA: bash scripts:
This is a minor task (5-10mins).
Put some bash script code snippets for these (from 421):
linux/anyplace.sh
macos/anyplace.sh

With a few comments on what they do (e.g. fetch all buildings..)


# TODO CA: gradle structure: (must be working by Tuesday 28/07)
1. Make core/lib a pure JAVA (compatible both in Android and desktop JVMs).

2. core/cli: put all stuff that will be removed from core/lib here
    - core/cli/bin: put a jar file here

3. include core/lib in android-new/lib
It should be included.... (with relative path).
from core/lib you must remove stuff that dont work on android.

Worse case: 
core/lib exports a jar, and include that jar (maybe from relative path)

Ideal:
core/lib  and android-new/lib will be published as gradle libraries at gradle/maven repos..

4. `hello world` on android-new/demo
    - commit !


## TODO CA: Merging process: (must have aleast started/dug into by Tuesday 28/07)
Merge the legacy code from clients/android:
* most stuff will end up:
    - core/lib
    - android-new/lib
    - few stuff on android-new/demo

* Deprecate stuff (while moving) from android/
    - e.g. replace ActionBarSherlock with build in libs..
        + for exaple: SherlockActivity -> SupportActivity  (android support lib)
            - see tutorials on migrating/deprecating Sherlock
    - similarly with other stuff..

## On next meeting (with PM): see status of this [TODO.md](./core/lib/TODO.md)
