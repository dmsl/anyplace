package cy.ac.ucy.cs.anyplace.smas.logger.ui

import android.content.Intent
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.maps.OnMapReadyCallback
import cy.ac.ucy.cs.anyplace.lib.android.appSmas
import cy.ac.ucy.cs.anyplace.lib.android.data.anyplace.helpers.FloorHelper
import cy.ac.ucy.cs.anyplace.lib.android.extensions.METHOD
import cy.ac.ucy.cs.anyplace.lib.android.extensions.TAG
import cy.ac.ucy.cs.anyplace.lib.android.extensions.TAG_METHOD
import cy.ac.ucy.cs.anyplace.lib.android.extensions.dsCvLog
import cy.ac.ucy.cs.anyplace.lib.android.ui.components.StatusUpdater
import cy.ac.ucy.cs.anyplace.lib.android.ui.cv.CvMapActivity
import cy.ac.ucy.cs.anyplace.lib.android.ui.cv.yolo.tflite.Classifier
import cy.ac.ucy.cs.anyplace.lib.android.utils.LOG
import cy.ac.ucy.cs.anyplace.lib.android.viewmodels.anyplace.DetectorViewModel
import cy.ac.ucy.cs.anyplace.lib.android.viewmodels.anyplace.Localization
import cy.ac.ucy.cs.anyplace.lib.anyplace.core.LocalizationResult
import cy.ac.ucy.cs.anyplace.smas.R
import cy.ac.ucy.cs.anyplace.smas.logger.viewmodel.CvLoggerViewModel
import cy.ac.ucy.cs.anyplace.smas.logger.viewmodel.Logging
import cy.ac.ucy.cs.anyplace.smas.ui.SmasLoginActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * LEFTHERE:
 *
 * 1. settings: switch between SMAS / Logging
 *
 * 2. Bind w/ CV
 *
 * 3. Send fingerprints to remote
 *
 */
@AndroidEntryPoint
class CvLoggerActivity: CvMapActivity(), OnMapReadyCallback {
  // PROVIDE TO BASE CLASS [CameraActivity]:
  override val layout_activity: Int get() = R.layout.activity_cv_logger
  override val id_bottomsheet: Int get() = R.id.bottom_sheet_layout
  override val id_gesture_layout: Int get() = R.id.gesture_layout
  override val id_gmap: Int get() = R.id.mapView

  // private lateinit var binding: ActivityCvLoggerBinding
  // private lateinit var VM: CvLoggerViewModel
  // MERGE: all UI elements to abstract CVMapActivity
  // private lateinit var UI: UiActivityCvLogger

  @Suppress("UNCHECKED_CAST")
  override val view_model_class: Class<DetectorViewModel> =
          CvLoggerViewModel::class.java as Class<DetectorViewModel>

  // VIEW MODELS
  /** extends [CvMapViewModel] */
  private lateinit var VM: CvLoggerViewModel

  private val uiLog: CvLoggerUI by lazy {
    CvLoggerUI(this@CvLoggerActivity, lifecycleScope, VM,  id_bottomsheet, wMap)
  }


  override fun postCreate() {
    super.postCreate()
    VM = _vm as CvLoggerViewModel

    lifecycleScope.launch(Dispatchers.IO) {
      // CHECK: if this crashes (latinit not inited),
      // do something similar with the [readPrefsAndContinue] methods
      // or alternatively it could be put in [setupUi]
      VM.prefsCvLog = dsCvLog.read.first()
    }

    setupCollectors()
  }

  override fun onResume() {
    super.onResume()
    LOG.D(TAG, "$METHOD [CvLogger]")
    // MERGE
  }

  override fun setupUi() {
    super.setupUi()
    LOG.D2()
    setupUiReactions()


    // CLR: MERGE ..
    // uiLog.uiBottom.setup() // TODO why special method?
    // uiLog.setupBottomSheet() // TODO special method?

    // TODO
    // MERGE this was setupComputerVision()
    // TODO: CLR: collectors?
    // there is demo localization in Logger too,
    // to validate findings according to the latest CvMap
    collectLocalizationStatus()
    collectLocation()
  }

  private fun setupUiReactions() {
    lifecycleScope.launch(Dispatchers.Main) {
      uiReactObjectDetection()
      uiReactLogging()
    }
  }

  /**
   * Setup the BottomSheet by always making it visible.
   * This is because the logging UI is part of the BottomSheet.
   */
  override fun lazyInitBottomSheet() {
    uiLog.bottom = BottomSheetCvLoggerUI(this@CvLoggerActivity, VM, id_bottomsheet)
    uiBottom = uiLog.bottom

    setupLoggerBottomSheet()
  }

  // TODO:PM put this method in bottom (CvLoggerBottom)
  // and init it on creation.
  // and remove all code from [uiLog]
  private fun setupLoggerBottomSheet() {
    uiLog.setupTimerButtonClick()
    uiLog.setupClickClearObjectsPopup()
    uiLog.setupButtonSettings()
    uiLog.setupClickDemoNavigation()
  }


  /**
   * Observes [VM.objectDetectionsAll] changes and updates
   * [binding.bottomUi.buttonCameraTimer] accordingly.
   */
  private fun uiReactObjectDetection() {
    VM.objWindowALL.observeForever { detections ->
      // CHECK:PM binding.bottomUi.tvWindowObjectsAll
      uiLog.bottom.tvWindowObjectsAll.text = detections.toString()
    }
  }

  private fun uiReactLogging() {
    VM.logging.observeForever { status ->
      LOG.D(TAG_METHOD, "logging: $status")
      uiLog.refresh(status)
    }
  }

  private fun collectLocation() {
    lifecycleScope.launch{
      VM.location.collect { result ->
        when (result) {
          is LocalizationResult.Unset -> { }
          is LocalizationResult.Error -> {
            val msg = result.message.toString()
            val details = result.details
            if (details != null) {
              uiLog.statusUpdater.showErrorAutohide(msg, details, 4000L)
            } else {
              uiLog.statusUpdater.showErrorAutohide(msg, 4000L)
            }
          }
          is LocalizationResult.Success -> {
            result.coord?.let { VM.setUserLocation(it) }
            uiLog.statusUpdater.showInfoAutohide("Found loc","XY: ${result.details}.", 3000L)
          }
        }
      }
    }
  }

  private fun collectLocalizationStatus() {
    lifecycleScope.launch{
      VM.localization.collect { status ->
        LOG.W(TAG_METHOD, "status: $status")
        when(status) {
          Localization.stopped -> {
            uiLog.endLocalization()
            VM.logging.postValue(Logging.stopped)
          }
          else ->  {}
        }
      }
    }
  }

  /**
   * CHECK:PM
   */
  var collectorsSet=false
  private fun setupCollectors() {
    if (collectorsSet) return
    LOG.D(TAG_METHOD)
    collectLoadedFloors()
    collectLoggedInChatUser()
    VM.nwCvFingerprintSend.collect()
    collectorsSet=true
  }


  /*
  * Reacts to updates on [ChatUser]'s login status:
  * Only authenticated users are allowed to use this activity
  */
  private fun collectLoggedInChatUser() {
    // only logged in users are allowed on this activity:
    lifecycleScope.launch(Dispatchers.IO) {
      appSmas.dsChatUser.readUser.collect { user ->
        if (user.sessionkey.isBlank()) {
          finish()
          startActivity(Intent(this@CvLoggerActivity, SmasLoginActivity::class.java))
        } else {
          lifecycleScope.launch(Dispatchers.Main) {
            Toast.makeText(applicationContext, "Welcome ${user.uid}!", Toast.LENGTH_LONG).show()
          }
        }
      }
    }
  }


  /**
   * Observes when the initial floor will be loaded, and runs a method
   */
  private fun collectLoadedFloors() {
    lifecycleScope.launch(Dispatchers.IO) {
      VM.floor.collect { floor ->
        if (floor == null) return@collect

        VM.floorH = FloorHelper(floor, VM.spaceH)
        LOG.W(TAG,"FLOOR NOW IS: ${VM.floorH!!.prettyFloorName()}")
        // MERGE
        // CLR:PM ..
      }
    }
  }

  override fun onMapReadyCallback() {
    uiLog.setupClickedLoggingButton()
    uiLog.setupOnMapLongClick()
  }

  override fun onInferenceRan(detections: MutableList<Classifier.Recognition>) {
    if (detections.isNotEmpty()) {
      LOG.D2(TAG, "$METHOD: detections: ${detections.size} (LOGGER OVERRIDE)")
    }
    VM.processDetections(detections)
  }

}