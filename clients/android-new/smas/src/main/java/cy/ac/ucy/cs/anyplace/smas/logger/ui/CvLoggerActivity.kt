package cy.ac.ucy.cs.anyplace.smas.logger.ui

import androidx.lifecycle.lifecycleScope
import com.google.android.gms.maps.OnMapReadyCallback
import cy.ac.ucy.cs.anyplace.lib.android.data.models.helpers.FloorHelper
import cy.ac.ucy.cs.anyplace.lib.android.extensions.TAG
import cy.ac.ucy.cs.anyplace.lib.android.extensions.TAG_METHOD
import cy.ac.ucy.cs.anyplace.lib.android.ui.components.StatusUpdater
import cy.ac.ucy.cs.anyplace.lib.android.ui.cv.CvMapActivity
import cy.ac.ucy.cs.anyplace.lib.android.utils.LOG
import cy.ac.ucy.cs.anyplace.lib.android.viewmodels.DetectorViewModel
import cy.ac.ucy.cs.anyplace.lib.core.LocalizationResult
import cy.ac.ucy.cs.anyplace.smas.R
import cy.ac.ucy.cs.anyplace.smas.logger.viewmodel.CvLoggerViewModel
import cy.ac.ucy.cs.anyplace.smas.logger.viewmodel.Logging
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@AndroidEntryPoint
class CvLoggerActivity: CvMapActivity(), OnMapReadyCallback {
  // PROVIDE TO BASE CLASS [CameraActivity]:
  // PROVIDE TO BASE CLASS [CameraActivity]:
  override val layout_activity: Int get() = R.layout.activity_cv_logger
  override val id_bottomsheet: Int get() = R.id.bottom_ui
  override val id_gesture_layout: Int get() = R.id.gesture_layout
  override val id_gmap: Int get() = R.id.mapView

  // private lateinit var binding: ActivityCvLoggerBinding
  // private lateinit var VM: CvLoggerViewModel
  private lateinit var statusUpdater: StatusUpdater
  // MERGE: all UI elements to abstract CVMapActivity
  // private lateinit var UI: UiActivityCvLogger

  @Suppress("UNCHECKED_CAST")
  override val view_model_class: Class<DetectorViewModel> =
          CvLoggerViewModel::class.java as Class<DetectorViewModel>

  // VIEW MODELS
  /** extends [CvMapViewModel] */
  private lateinit var VM: CvLoggerViewModel

  protected lateinit var uiLog: CvLoggerUI

  override fun postCreate() {
    super.postCreate()
    VM = _vm as CvLoggerViewModel

    setupCollectors()
  }

  override fun onResume() {
    super.onResume()
    LOG.D(TAG, "onResume")
    // MERGE
  }

  override fun setupButtonsAndUi() {
    super.setupButtonsAndUi()
    LOG.D2()

    uiLog = CvLoggerUI(this@CvLoggerActivity, lifecycleScope, VM, wMap)
    // LEFTHERE : MERGE
    // LEFTHERE : MERGE

    setupComputerVision() // TODO: pull this in here..
  }

  /**
   * TODO:PM expand this method in UI setup?
   * MERGE: put this in the collectors
   */
  private fun setupComputerVision() {
    // super.setupComputerVision(binding.tovCamera, binding.pvCamera) CLR:PM no need

    observeObjectDetections()
    observeLoggingStatus()

    // there is demo localization in Logger too,
    // to validate findings according to the latest CvMap
    collectLocalizationStatus()
    collectLocation()
  }

  /**
   * Observes [VM.objectDetectionsAll] changes and updates
   * [binding.bottomUi.buttonCameraTimer] accordingly.
   */
  private fun observeObjectDetections() {
    VM.objWindowALL.observeForever { detections ->

      // CHECK:PM binding.bottomUi.tvWindowObjectsAll
      uiLog.tvWindowObjectsAll.text = detections.toString()
    }
  }

  private fun observeLoggingStatus() {
    VM.logging.observeForever { status ->
      LOG.D(TAG_METHOD, "logging: $status")
      updateLoggingUi(status)
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
              statusUpdater.showErrorAutohide(msg, details, 4000L)
            } else {
              statusUpdater.showErrorAutohide(msg, 4000L)
            }
          }
          is LocalizationResult.Success -> {
            result.coord?.let { VM.setUserLocation(it) }
            statusUpdater.showInfoAutohide("Found loc","XY: ${result.details}.", 3000L)
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
            UI.endLocalization(binding.mapView)
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
  private fun setupCollectors() {
    LOG.D(TAG_METHOD)
    collectLoadedFloors()
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
    // MERGE
  }

}