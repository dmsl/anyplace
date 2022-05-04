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

---
# SMAS

## SM1. MSGS:
- going to chat, sending msg, going back to MainActivity, then back to chat
  - shows only 1 msg
  





-------


# OPT:

## 0. "Too much work on main thread.."