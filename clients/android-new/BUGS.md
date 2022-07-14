## 1.
ERROR:class kotlin.UninitializedPropertyAccessException: null: lateinit property trackingOverlay has
not been initialized

## 2. Init crash

java.lang.ExceptionInInitializerError
at cy.ac.ucy.cs.anyplace.lib.android.ui.cv.yolo.tflite.DetectorActivityBase.postCreate(DetectorActivityBase.kt:97)
  at cy.ac.ucy.cs.anyplace.lib.android.ui.cv.CvMapActivity.postCreate(CvMapActivity.kt:64)
  at cy.ac.ucy.cs.anyplace.smas.ui.SmasMainActivity.postCreate(SmasMainActivity.kt:125)
  at cy.ac.ucy.cs.anyplace.lib.android.ui.cv.yolo.tflite.CameraActivity.onCreate(CameraActivity.kt:116)


Caused by: java.lang.IllegalArgumentException: Invalid primitive conversion from boolean to long

## 3. Init BUG: smas opens twice?
- toast msgs appear twice..

---

# Any App (Smas, Logger):

## 1. BUG:F84F: Failed to fetch <prettyType>
Failed to fetch vessel:

---

# SMAS

## LONG-PRESS on SMAS to force location
- after some panning (zoom in/out), it also works w/ normal click

## SM1. MSGS:
- going to chat, sending msg, going back to MainActivity, then back to chat
  - shows only 1 msg
  

## SM2. WiFi Disconnected
- When we have wifi but then it goes out of range: the app crashes.


## BUG: Loosing foreground: state changes
- might crash..
- logging state is wrong...
  - proper reset must be done...


## BUG: Scanning from different models:
- scanned some objects from ucy co
- then change model
 - then it crashes

## Heatmaps:
- cleanup..

-------


# OPT:

## 0. "Too much work on main thread.."

# No objects detected, while objects ARE detected
- most likely happens ONLY on the first run
- as the remote classes have NOT been downloaded yet (and populated revelant structures in the app)
- on 2nd run it should be OK