package cy.ac.ucy.cs.anyplace.smas.logger.viewmodel

import android.app.Application
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.model.LatLng
import cy.ac.ucy.cs.anyplace.lib.android.data.anyplace.RepoAP
import cy.ac.ucy.cs.anyplace.lib.android.data.anyplace.helpers.CvMapHelper
import cy.ac.ucy.cs.anyplace.lib.android.data.anyplace.helpers.FloorHelper
import cy.ac.ucy.cs.anyplace.lib.android.data.anyplace.store.*
import cy.ac.ucy.cs.anyplace.lib.android.extensions.METHOD
import cy.ac.ucy.cs.anyplace.lib.android.extensions.TAG
import cy.ac.ucy.cs.anyplace.lib.android.extensions.app
import cy.ac.ucy.cs.anyplace.lib.android.ui.cv.yolo.tflite.Classifier
import cy.ac.ucy.cs.anyplace.lib.android.ui.cv.yolo.tflite.YoloV4Classifier
import cy.ac.ucy.cs.anyplace.lib.android.utils.LOG
import cy.ac.ucy.cs.anyplace.lib.android.utils.net.RetrofitHolderAP
import cy.ac.ucy.cs.anyplace.lib.android.utils.utlTime
import cy.ac.ucy.cs.anyplace.lib.android.viewmodels.anyplace.CvMapViewModel
import cy.ac.ucy.cs.anyplace.lib.android.consts.smas.CHAT
import cy.ac.ucy.cs.anyplace.lib.android.data.anyplace.DetectionModel
import cy.ac.ucy.cs.anyplace.lib.android.data.smas.RepoSmas
import cy.ac.ucy.cs.anyplace.lib.android.data.smas.source.RetrofitHolderSmas
import cy.ac.ucy.cs.anyplace.lib.anyplace.models.CvDetectionREQ
import cy.ac.ucy.cs.anyplace.lib.anyplace.models.UserCoordinates
import cy.ac.ucy.cs.anyplace.lib.anyplace.network.NetworkResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.forEach
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class Logging {
  running,
  stopped,
  stoppedMustStore,
  stoppedNoDetections,
  demoNavigation, // DemoLocalization
}

enum class TimerAnimation { running,  paused,  reset  }

/**
 * Extends [CvMapViewModel]:
 */
@HiltViewModel
class CvLoggerViewModel @Inject constructor(
        application: Application,
        dsCv: CvDataStore,
        dsCvNav: CvNavDataStore,
        dsMisc: MiscDataStore,
        dsCvLog: CvLoggerDataStore,
        repoAP: RepoAP,
        RHap: RetrofitHolderAP,
        repoSmas: RepoSmas,
        RHsmas: RetrofitHolderSmas):
        CvMapViewModel(application, dsCv, dsMisc, dsCvNav, repoAP, RHap, repoSmas, RHsmas) {

  private val C by lazy { CHAT(app.applicationContext) }

  // var longClickFinished: Boolean = false
  var circleTimerAnimation: TimerAnimation = TimerAnimation.paused
  // var prefs: CvLoggerPrefs = dsCvLog.d
  lateinit var prefsCvLog : CvLoggerPrefs  // by lazy { dsCvLog.read.first() }

  // TODO:PM: statusLOG ?
  val logging: MutableLiveData<Logging> = MutableLiveData(Logging.stopped)

  /** Detections of the current logger scan-window (MERGE: detectionsLogging) */
  val objWindowLOG: MutableLiveData<List<Classifier.Recognition>> = MutableLiveData()
  /** Detections assigned to map locations (MERGE:  with storedDetections) */
  var objOnMAP: MutableMap<LatLng, List<Classifier.Recognition>> = mutableMapOf()
  /** CHECK:PM a counter over all detections? */
  val objWindowALL: MutableLiveData<Int> = MutableLiveData(0)
  /** for stats, and for enabling scanned objects clear (on current window) (MERGE: objectsWindowUnique) */
  var objWindowUnique = 0
  var objTotal = 0
  /** whether there was a pause in the current scanning window */
  var previouslyPaused = false
  /** no logging operations performed before */
  var initialStart = true

  /** stores the elapsed time on stops/pauses */
  var windowElapsedPause : Long = 0
  var firstDetection = false

  // PREFERENCES (CHECK:PM these were SMAS).
  // val prefsChat = dsChat.read
  /** How often to refresh UI components from backend (in ms) */
  // var refreshMs : Long = C.DEFAULT_PREF_SMAS_LOCATION_REFRESH.toLong()*1000L

  override fun prefWindowLocalizationMillis(): Int {
    // modify properly for Smas?
    return C.DEFAULT_PREF_CVLOG_WINDOW_LOCALIZATION_SECONDS.toInt()
  }

  fun canStoreDetections() : Boolean {
    return (logging.value == Logging.running) || (logging.value == Logging.stoppedMustStore)
  }

  /**
   * CHECK:PM this was part of a VM that had the detections
   * (deep in the CV engine).
   *
   * Now this is at a higher level
   *
   * TODO:PM convert to a post call?
   */
  fun processDetections(recognitions: List<Classifier.Recognition>) {
    when (logging.value) {
      Logging.running -> {
        // val detectionTime: Long = detectionProcessor.processImage(bitmap)
        // val processingTime = conversionTime + detectionTime
        // val detections = detectionProcessor.frameDetections
        // LOG.V4(TAG, "Conversion time : $conversionTime ms")
        // LOG.V4(TAG, "Detection time : $detectionTime ms")
        // LOG.V3(TAG, "Analysis time : $processingTime ms")
        // LOG.V3(TAG, "Detected: ${detections.size}")
        // updateDetectionsLogging(detection)
        // return detectionTime

        updateLoggingRecognitions(recognitions)
      }
      Logging.demoNavigation -> {
        // val detectionTime: Long = detectionProcessor.processImage(bitmap)
        // val detections = detectionProcessor.frameDetections
        // LOG.V4(TAG, "Detection time : $detectionTime ms")

        updateDetectionsLocalization(recognitions)
        // return detectionTime
      }
      else -> {  // Clear objects
        // MERGE:PM:
        LOG.V2(TAG, "$METHOD: neither logging or localizing (ignoring objects)")
        // detectionProcessor.clearObjects()
      }
    }
  }

  /**
   * Update detections that concern only the logging phase.
   * MERGE:PM accept NEW detections..
   */
  private fun updateLoggingRecognitions(recognitions: List<Classifier.Recognition>) {
    currentTime = System.currentTimeMillis()
    LOG.D2(TAG, "$METHOD: ${logging.value}")

    val appendedDetections = objWindowLOG.value.orEmpty() + recognitions
    objWindowALL.postValue(appendedDetections.size)
    when {
      firstDetection -> {
        LOG.D3(TAG, "$METHOD: initing window: $currentTime")
        windowStart = currentTime
        firstDetection=false
        this.objWindowLOG.postValue(appendedDetections)
      }
      logging.value == Logging.stoppedMustStore -> {
        windowStart = currentTime
        LOG.D("updateDetectionsLogging: new window: $currentTime")
      }

      // WINDOW FINISHED:
      currentTime-windowStart > prefWindowLoggingMillis() -> {
        windowElapsedPause = 0 // resetting any pause time
        previouslyPaused=false
        if (appendedDetections.isEmpty()) {
          logging.postValue(Logging.stoppedNoDetections)
        } else {
          logging.postValue(Logging.stoppedMustStore)
          LOG.D3("updateDetectionsLogging: status: $logging objects: ${appendedDetections.size}")
          val detectionsDedup =
                  YoloV4Classifier.NMS(appendedDetections, detector.labels)

          objWindowLOG.postValue(detectionsDedup)
          LOG.D3("updateDetectionsLogging: status: $logging objects: ${detectionsDedup.size} (dedup)")
          objWindowUnique=detectionsDedup.size
          objTotal+=objWindowUnique
        }
      }
      else -> { // Within a window
        this.objWindowLOG.postValue(appendedDetections)
      }
    }
  }

  /**
   * Upload Unique detections
   */
  private fun uploadUniqueDetections(userCoords: UserCoordinates, detections: List<Classifier.Recognition>) {
    LOG.E(TAG,"$METHOD: CvModel: detections: ${detections.size}")

    viewModelScope.launch(Dispatchers.IO) {
      // TODO:PM store on database (still will be a resp success..)

      // to proceed: Must have CvModels on DB first
      if (!repoSmas.local.hasCvModelClassesDownloaded()) {
        val msg = "Cannot upload detections (must download CvModels first)"
        LOG.E(TAG, "$METHOD: $msg")
        app.showToast(viewModelScope, msg)
        return@launch
      }

      val cvModelClasses = repoSmas.local.readCvModelClasses(model.idSmas)
      val hmap : HashMap<Int, Int> = HashMap ()

      cvModelClasses.forEach { cvClass ->
        LOG.D5(TAG, "$METHOD: SmasModelClasses: ${cvClass.modelid} ${cvClass.name}")
        hmap[cvClass.cid] = cvClass.oid
      }

      // build detections request
      val detectionsReq = mutableListOf<CvDetectionREQ>()
      detections.forEach { detection ->
        val detectionStr = "${detection.detectedClass}:${detection.title}"
        val modelStr = "${model.idSmas}:${model.modelName}"

        LOG.D(TAG, "$METHOD: CvModel: UPLOAD: $detectionStr: $modelStr")

        val oid = hmap[detection.detectedClass]
        if (oid == null) {
          LOG.E(TAG, "$METHOD: No class for: ${detection.detectedClass}:${detection.title}")
        } else {
          val detReq = CvDetectionREQ(oid,
                  detection.location.width().toDouble(),
                  detection.location.height().toDouble())
          LOG.D(TAG, "$METHOD: CvModel: READY: ${detReq.oid}: w:${detReq.width} h:${detReq.height}")
          detectionsReq.add(detReq)
        }
      }

      // 1. if classes not in sqlite
      // download them and put them in DB. else do noth

      // when collecting FingerPrint:
      // 1. on click (not FileCaching/Batch)
      // 1.a read from DB (according to model)
      // 1.b convert to smass id's from CV (on classes from SQLITE)
      // 1.c send them.
      // TODO test: different foor
      // TODO test: different model


      // LEFTHERE:
      /* TODO: BATCH STORING
      - file cache: append a line: in this method
      - NW call: storeDetectionsAndUpdateUI
        - or w/ an upload button
       */

      // 0. make NW call (DONE)
      // 1. read from DB and to oid from DB query DONE
      // 2. keep recording elements
      // 3. store then in a file, line by line:
      // // 3.a it will be 2 places: file line by line
      // // 3.b and the CVMap json
      // CHECK:PM
      // Can we upload at the end the local radiomap?
      // UI TODO
      // 1. BIND THE TIMER BUTTON

      LOG.E(TAG, "$METHOD: TODO NW call here")
      nwCvFingerprintSend.safeCall(userCoords, detectionsReq, model)
    }
    // TODO test from different model too..
  }

fun prefWindowLoggingMillis(): Int { return prefsCvLog.windowLoggingSeconds.toInt()*1000 }
// MERGE:CHECK:PM
// override fun prefWindowLocalizationMillis(): Int { return prefs.windowLocalizationSeconds.toInt()*1000 }

/** Toggle [logging] between stopped (or notStarted), and started.
 *  There will be no effect when in stoppedMustStore mode.
 *
 *  In that case it will wait for the user to store the logging data.
 */
fun toggleLogging() {
  initialStart = false
  when (logging.value) {
    // Logging.finished-> {}
    Logging.stoppedNoDetections,
    Logging.stopped -> {
      logging.value = Logging.running
      val now = System.currentTimeMillis()
      windowStart=now-windowElapsedPause
    }
    Logging.running -> {
      previouslyPaused = true
      logging.value = Logging.stopped
      LOG.D(TAG, "$METHOD: paused")

      // pause timer:
      val now = System.currentTimeMillis()
      windowElapsedPause = now-windowStart
    }
    else ->  {
      LOG.W(TAG, "$METHOD: Ignoring: ${logging.value}")
    }
  }
}

fun getElapsedSeconds(): Float { return (currentTime - windowStart)/1000f }
fun getElapsedSecondsStr(): String { return utlTime.getSecondsPretty(getElapsedSeconds()) }

fun resetLoggingWindow() {
  objWindowUnique=0
  objWindowLOG.value = emptyList()
  logging.value= Logging.stopped// CHECK:PM this was stopped. starting directly
  // status.value= Logging.started // CHECK:PM this was stopped. starting directly
}

fun startNewWindow() {
  objWindowUnique=0
  objWindowLOG.value = emptyList()
  logging.value= Logging.stopped
  toggleLogging()
}

/**
 * Stores the detections on the [objOnMAP],
 * a Hash Map of locations and object fingerprints
 */

fun addDetections(FH: FloorHelper?, mdddd: DetectionModel, latLong: LatLng) {

  objTotal+=objWindowUnique
  // TODO:PM do them in batch later on..
  val detections = objWindowLOG.value.orEmpty()
  objOnMAP[latLong] = detections

  // floorH.spaceH.obj.id
  val userCoord = UserCoordinates(floorH?.spaceH?.obj?.id!!,
          floorH?.obj!!.floorNumber.toInt(),
          latLong.latitude, latLong.longitude)

  uploadUniqueDetections(userCoord, detections)
}


/**
 * Generates a [cvMap] from the stored detections.
 * Then it reads any local [CvMap] and merges with it.
 * Finally the merged [CvMap] is written to cache (overriding previous one),
 * and stored in [CvViewModelBase].
 */
fun storeDetections(FH: FloorHelper?) {
  if (FH == null) {
    LOG.E(TAG, "$METHOD: floorHelper is null.")
    return
  }

  // MERGE:PM:TODO
  // TODO: UPDATE radiomap (this was a trial todo?)
  val curMap = CvMapHelper.generate(model, FH, objOnMAP)
  val curMapH = CvMapHelper(curMap, detector.labels, FH)
  LOG.D(TAG, "$METHOD: has cache: ${curMapH.hasCache()}") // CLR:PM
  val merged = curMapH.readLocalAndMerge()
  val mergedH = CvMapHelper(merged, detector.labels, FH)
  mergedH.storeToCache()

  LOG.D(TAG, "$METHOD: has cache: ${cvMapH?.hasCache()}") // CLR:PM
  mergedH.generateCvMapFast()
  cvMapH = mergedH
  objOnMAP.clear()
}
}