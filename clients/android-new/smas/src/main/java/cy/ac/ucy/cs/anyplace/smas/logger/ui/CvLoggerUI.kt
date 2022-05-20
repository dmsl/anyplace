package cy.ac.ucy.cs.anyplace.smas.logger.ui

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.widget.AppCompatButton
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.Group
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.button.MaterialButton
import cy.ac.ucy.cs.anyplace.lib.android.extensions.*
import cy.ac.ucy.cs.anyplace.lib.android.ui.components.StatusUpdater
// import cy.ac.ucy.cs.anyplace.lib.android.ui.cv.gnk.logger.CvLoggerBottomSheetCallback
// import cy.ac.ucy.cs.anyplace.lib.android.ui.cv.gnk.logger.showBottomSheet
import cy.ac.ucy.cs.anyplace.lib.android.ui.cv.map.GmapWrapper
import cy.ac.ucy.cs.anyplace.lib.android.ui.settings.SettingsCvLoggerActivity
import cy.ac.ucy.cs.anyplace.lib.android.utils.LOG
import cy.ac.ucy.cs.anyplace.lib.android.utils.ui.utlButton
import cy.ac.ucy.cs.anyplace.lib.android.utils.utlTime
import cy.ac.ucy.cs.anyplace.lib.android.viewmodels.Localization
import cy.ac.ucy.cs.anyplace.smas.R
import cy.ac.ucy.cs.anyplace.smas.logger.viewmodel.CvLoggerViewModel
import cy.ac.ucy.cs.anyplace.smas.logger.viewmodel.Logging
import cy.ac.ucy.cs.anyplace.smas.logger.viewmodel.TimerAnimation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


class CvLoggerUI(private val activity: Activity,
                 private val scope: CoroutineScope,
                 private val VM: CvLoggerViewModel,
                 private val id_bottomsheet: Int,
                private val wMap: GmapWrapper
        // fragmentManager: FragmentManager, // CHECK (on CvMapUi?) or GmapWrapper?
        // floorSelector: FloorSelector, //  CHECK (on CvMapUi?) or GmapWrapper?
        // overlays: Overlays, //  CHECK (on CvMapUi?) or GmapWrapper?
                 )
// CLR:PM once this is merged
// : UiActivityCvBase(activity, // MERGED? possible inheritance is OK
// fragmentManager,
// VM as CvViewModelBase,
// scope, statusUpdater, overlays,
// floorSelector)
{

  /**
   * MERGE:PM once image is analyzed
   * Used to be inside analyzeImage I think
   */
  fun onAnalyzedImageTODO() {
    LOG.E(TAG, "onAnalyzedImage")
    // bindCvStatsImgDimensions(image) // MERGE (and do this once. not on each analyze)
    bu_TvTimeInfo.text =  "<TODO>ms" // "${detectionTime}ms" // TODO:PM timer?
    updateCameraTimerButton()
    bindCvStatsText()
  }


  companion object {
    const val OPACITY_MAP_LOGGING = 0f
    const val ANIMATION_DELAY : Long = 100

    // CLR:PM GNK CODE?
    // const val CAMERA_REQUEST_CODE: Int = 1
    // const val CAMERA_ASPECT_RATIO: Int = AspectRatio.RATIO_4_3 // AspectRatio.RATIO_16_9
  }

  private val ctx = activity.applicationContext

  // UI COMPONENTS:
  // CHECK: this was in bottom sheet?
  val tvWindowObjectsAll : TextView = activity.findViewById(R.id.tv_windowObjectsAll)
  val btnLogging : AppCompatButton = activity.findViewById(R.id.button_logging)
  val btnClearObj: MaterialButton = activity.findViewById(R.id.button_clearObjects)
  val btnDemoNav : MaterialButton = activity.findViewById(R.id.btn_demoNavigation)
  val btnTimer : MaterialButton = activity.findViewById(R.id.button_cameraTimer)
  val groupTutorial : Group = activity.findViewById(R.id.group_tutorial)
  val progressBarTimer: ProgressBar = activity.findViewById(R.id.progressBar_timer)
  val btnSettings: MaterialButton = activity.findViewById(R.id.button_settings)

  private val statusUpdater = StatusUpdater(
          activity,
          scope,
          activity.findViewById(R.id.tv_statusSticky),
          activity.findViewById(R.id.tv_msgTitle),
          activity.findViewById(R.id.tv_msgSubtitle),
          activity.findViewById(R.id.view_statusBackground),
          activity.findViewById(R.id.view_warning))


  /**
   * TODO:PM: NAV COMMON shared between activities? (pre-merge)
   * - move in CvMap?
   */
  fun bindCvStatsText() {
    bu_TvElapsedTime.text=VM.getElapsedSecondsStr()
    bu_TvObjUnique.text=VM.objWindowUnique.toString()
    bu_TvWindowCur.text=VM.objOnMAP.size.toString()
    bu_TvObjTotal.text=VM.objTotal.toString()
  }

  // MERGE:PM bind this once (when we have CV img dimensions)
  // fun bindCvStatsImgDimensions(image: ImageProxy) { // TODO:PM: NAV COMMON shared between activities?
  //   binding.bottomUi.frameInfo.text = "${image.width}x${image.height}"
  // }

  // val btnLogging : Button = binding.bottomUi.buttonLogging
  // val btnDemoNav= binding.btnDemoNavigation
  // val btnTimer = binding.bottomUi.buttonCameraTimer


  /**
   * Observes [VM.windowDetections] changes and updates
   * [binding.bottomUi.buttonCameraWindow] accordingly.
   */
  fun updateCameraTimerButton() {
    val elapsed = VM.getElapsedSeconds()
    val remaining = (VM.prefs.windowLoggingSeconds.toInt()) - elapsed

    // TODO MERGE: must go through binding.bottomUi.buttonCameraTimer
    val btn = activity.findViewById<MaterialButton>(R.id.button_cameraTimer)
    // TODO MERGE: binding.bottomUi.progressBarTimer
    val progressBar = activity.findViewById<ProgressBar>(R.id.progressBar_timer)

    if (remaining>0) {
      val windowSecs = VM.prefs.windowLoggingSeconds.toInt()
      setupProgressBarTimerAnimation(btn, progressBar, windowSecs)
      btn.text = utlTime.getSecondsRounded(remaining, windowSecs)
    } else {
      progressBar.visibility = View.INVISIBLE
      btn.text = ""
      progressBar.progress = 100

      if (!VM.objWindowLOG.value.isNullOrEmpty()) {
        utlButton.changeMaterialButtonIcon(btn, ctx, R.drawable.ic_objects)
      } else {   // no results, hide the timer
        utlButton.removeMaterialButtonIcon(btn)
        btn.fadeOut()
      }
    }
  }

  /**
   * Initiate a circular progress bar animation, inside a coroutine for
   * smooth (and independent from other threads) updates.
   * It progresses according to the window time
   */
  private fun setupProgressBarTimerAnimation(
          btnTimer: MaterialButton,
          progressBar: ProgressBar,
          windowSecs: Int) {
    // showing timer button but not yet the progress bar
    if (btnTimer.visibility == View.VISIBLE &&
            progressBar.visibility != View.VISIBLE) {
      val delayMs = (windowSecs*1000/100).toLong()
      scope.launch {
        var progress = 0
        progressBar.progress=progress
        progressBar.visibility = View.VISIBLE
        while(progress < 100) {
          when (VM.circleTimerAnimation) {
            TimerAnimation.reset -> { resetCircleAnimation(progressBar); break }
            TimerAnimation.running -> { progressBar.setProgress(++progress, true) }
            TimerAnimation.paused -> {  }
          }
          delay(delayMs)
        }
      }
    }
  }

  fun endLocalization() {
    LOG.D2()
    // val btnDemoNav = bt//binding.btnDemoNavigation CLR:PM
    statusUpdater.clearStatus()
    utlButton.changeBackgroundButtonDONT_USE(btnDemoNav, ctx, R.color.darkGray)
    btnDemoNav.isEnabled = true
    wMap.mapView.alpha = 1f
    VM.localization.tryEmit(Localization.stopped)
  }

  /**
   * Stores some measurements on the given GPS locations
   */
  fun setupOnMapLongClick() {
    wMap.obj.setOnMapLongClickListener { location ->
      if (VM.canStoreDetections()) {
        LOG.V3(TAG, "clicked at: $location")

        // re-center map
        wMap.obj.animateCamera(
                CameraUpdateFactory.newCameraPosition(
                        CameraPosition(
                                location, wMap.obj.cameraPosition.zoom,
                                // don't alter tilt/bearing
                                wMap.obj.cameraPosition.tilt,
                                wMap.obj.cameraPosition.bearing)
                )
        )

        val windowDetections = VM.objWindowLOG.value.orEmpty().size
        VM.addDetections(location)

        // add marker
        val curPoint = VM.objOnMAP.size.toString()
        val msg = "Point: $curPoint\n\nObjects: $windowDetections\n"

        VM.markers.addCvMarker(location, msg)
        // VM.addMarker(location, msg) // CHECK:PM

        // pause a bit, then restart logging
        scope.launch {
          restartLogging()
        }
        // binding.bottomUi.buttonCameraTimer.fadeOut() CHECK:PM old comment

      } else {
        val msg ="Not in scanning mode"
        statusUpdater.showWarningAutohide(msg, 2000)
        LOG.V2("onMapLongClick: $msg")
      }
    }
  }

  /**
   * TODO:PM put in UiLogger??
   *
   * MERGE: was updateLoggingUi
   */
  @SuppressLint("SetTextI18n")
  fun refresh(status: Logging) {
    LOG.D4(TAG_METHOD, "status: $status")
    // CLR:PM comments
    // val btnLogging = binding.bottomUi.buttonLogging
    // val btnDemoNav= binding.btnDemoNavigation
    // val btnTimer = binding.bottomUi.buttonCameraTimer
    groupTutorial.visibility = View.GONE
    // binding.bottomUi.groupTutorial.visibility = View.GONE
    btnLogging.visibility = View.VISIBLE // hidden only by demo-nav

    when (status) {
      Logging.demoNavigation -> {
        btnLogging.visibility = View.INVISIBLE
        VM.circleTimerAnimation = TimerAnimation.reset
        startLocalization(wMap.mapView)
      }
      // CLR:PM these were OLD comments (before merging)
      // Logging.finished -> { // finished a scanning
      //   btnDemoNav.visibility = View.GONE
      //   btnTimer.fadeOut()
      //   btnLogging.text = "Stored"
      //   changeBackgroundButtonCompat(btnLogging, applicationContext, R.color.green)
      //   // TODO:TRIAL new logic?
      //   // TODO:TRIAL onMapLongClick store on long-click
      //   // VM.circleTimerAnimation = TimerAnimation.reset
      //   // DetectionMapHelper.generate(storedDetections)
      //   // sleep a while.....
      //   // TODO show the below menu..
      //   // TODO store..
      //   // TODO show upload section..(on the below menu, with UPLOAD button)..
      //   // TODO set to stopped again
      // }
      Logging.running -> { // just started scanning
        btnDemoNav.fadeOut()
        VM.circleTimerAnimation = TimerAnimation.running
        btnLogging.text = "pause"
        utlButton.removeMaterialButtonIcon(btnTimer)
        utlButton.changeBackgroundButtonCompat(btnLogging, ctx, R.color.darkGray)
        utlButton.changeBackgroundButtonDONT_USE(btnTimer, ctx, R.color.redDark)
        btnTimer.fadeIn()
        wMap.mapView.animateAlpha(OPACITY_MAP_LOGGING, ANIMATION_DELAY)
      }
      Logging.stopped -> { // stopped after a pause or a store: can start logging again
        btnDemoNav.fadeIn()
        // clear btnTimer related components.. TODO make this a class..
        VM.circleTimerAnimation = TimerAnimation.reset
        btnTimer.fadeOut()
        progressBarTimer.fadeOut()
        VM.circleTimerAnimation = TimerAnimation.paused
        if (VM.previouslyPaused) {
          btnLogging.text = "resume"
        } else {
          btnLogging.text = "scan"
          groupTutorial.visibility = View.VISIBLE
        }
        utlButton.changeBackgroundButtonCompat(btnLogging, ctx, R.color.colorPrimary)
        wMap.mapView.animateAlpha(1f, ANIMATION_DELAY)
        utlButton.changeBackgroundButtonDONT_USE(btnTimer, ctx, R.color.darkGray)
      }
      Logging.stoppedNoDetections -> { // stopped after no detections: retry a scan
        btnDemoNav.visibility = View.GONE
        VM.circleTimerAnimation = TimerAnimation.reset
        scope.launch {
          val ms = 1500L
          statusUpdater.showWarningAutohide("No detections.", "trying again..", ms)
          delay(ms) // wait before restarting..
          restartLogging()
        }
      }
      Logging.stoppedMustStore -> {
        btnDemoNav.visibility = View.GONE
        VM.circleTimerAnimation = TimerAnimation.reset
        btnTimer.visibility= View.VISIBLE
        LOG.D(TAG_METHOD, "stopped must store: visible")

        wMap.mapView.animateAlpha(1f, ANIMATION_DELAY)
        utlButton.changeBackgroundButtonDONT_USE(btnTimer, ctx, R.color.yellowDark)

        val storedDetections = VM.objOnMAP.size
        val noDetections = storedDetections == 0
        val title="long-click on map"
        val subtitle = if (noDetections) "nothing new attached on map yet" else "mapped locations: $storedDetections"
        val delay = if(noDetections) 7000L else 5000L
        statusUpdater.showNormalAutohide(title, subtitle, delay)

        btnLogging.text = "END"
        // val loggingBtnColor = if (noDetections) R.color.darkGray else R.color.yellowDark
        // changeBackgroundButtonCompat(btnLogging, applicationContext, loggingBtnColor)
        utlButton.changeBackgroundButtonCompat(btnLogging, ctx, R.color.darkGray)
      }
    }
  }

  fun startLocalization(mapView: MapView) {
    // val btnDemoNav = binding.btnDemoNavigation CLR
    btnDemoNav.isEnabled = false
    VM.currentTime = System.currentTimeMillis()
    VM.windowStart = VM.currentTime
    VM.localization.value = Localization.running
    statusUpdater.setStatus("scanning..")
    btnDemoNav.visibility = View.VISIBLE
    utlButton.changeBackgroundButtonDONT_USE(btnDemoNav, ctx, R.color.colorPrimary)
    mapView.alpha = 0.90f // TODO:PM no alpha? (use alpha from settings?
  }

  private fun resetCircleAnimation(progressBar: ProgressBar) {
    progressBar.visibility = View.INVISIBLE
    progressBar.progress=0
  }

  /**
   * Pauses a bit, then it restarts logging
   * Used when:
   * - detections were stored on the map
   * - no detections found
   *
   * Actions taken:
   * - hides the top status
   * - removes the camera timer (will be added again when reseted?) and makes it gray,
   * - removes the logging button
   * - shows the map
   * - stars a new window
   */
  private suspend fun restartLogging(delayMs: Long = 500) {
    LOG.D()
    delay(delayMs)
    statusUpdater.hideStatus()
    utlButton.removeMaterialButtonIcon(btnTimer)
    // CHECK:PM: replaced changeBackgroundButtonDONT_USE
    utlButton.changeBackgroundButtonCompat(btnTimer, ctx, R.color.darkGray)
    utlButton.changeBackgroundButtonCompat(btnLogging, ctx, R.color.colorPrimary)
    wMap.mapView.animateAlpha(1f, ANIMATION_DELAY)
    VM.startNewWindow()
  }


  private var clearConfirm=false
  private var clickedScannedObjects=false
  /**
   * Allowing to clear the window that was just scanned
   *
   * MERGED w/ setupClickCameraTimerCircleButton
   */
  fun setupTimerButtonClick() {
    btnTimer.setOnClickListener {
      if (VM.objWindowUnique > 0 &&!clickedScannedObjects) {
        clickedScannedObjects=true
        btnClearObj.fadeIn()
        scope.launch {
          delay(5000)
          clickedScannedObjects=false
          if(clearConfirm) {
            clickedScannedObjects=true
            delay(5000) // an extra delay
            clearConfirm=false
            clickedScannedObjects=false
          }
          hideClearObjectsButton()
        }
      }
    }
  }

  fun setupClickClearObjectsPopup() {
    btnClearObj.setOnClickListener {
      if (!clearConfirm) {
        clearConfirm = true
        btnClearObj.text = "Sure ?"
        btnClearObj.alpha = 1f
      } else {
        hideClearObjectsButton()
        btnTimer.fadeOut()
        VM.resetLoggingWindow()
        statusUpdater.hideStatus()
      }
    }
  }

  fun hideClearObjectsButton() {
    clearConfirm=false
    btnClearObj.fadeOut()
    scope.launch {
      delay(100)
      btnClearObj.alpha = 0.5f
      btnClearObj.text = "Clear"
    }
  }

  fun setupClickSettingsMenuButton() {
    LOG.D2()

    // Setups a regular button to act as a menu button
    btnSettings.setOnClickListener { // TODO:PM
      val intent = Intent(activity, SettingsCvLoggerActivity::class.java)
      intent.putExtra(SettingsCvLoggerActivity.ARG_SPACE, VM.spaceH.toString())
      intent.putExtra(SettingsCvLoggerActivity.ARG_FLOORS, VM.floorsH.toString())
      intent.putExtra(SettingsCvLoggerActivity.ARG_FLOOR, VM.floorH.toString())
      activity.startActivity(intent)
    }

    // // TODO:PM Settings
    // // Setups a regular button to act as a menu button
    //   binding.buttonSettings.setOnClickListener {
    //     SettingsDialog.SHOW(supportFragmentManager, SettingsDialog.FROM_CVLOGGER)
    //   }
    val versionName = "VER.x.todo"  // TODO:PM {BuildConfig.VERSION_NAME}
    btnSettings.setOnLongClickListener {
      scope.launch {
        statusUpdater.showInfoAutohide("App Version: $versionName", 1000L)
      }
      true
    }
  }


  private var longClickClearCvMap=false

  fun setupClickDemoNavigation() {
    btnDemoNav.setOnClickListener {
      when (VM.logging.value) {
        Logging.stopped,
        Logging.stoppedMustStore -> {  // enter demo-nav mode
          VM.logging.postValue(Logging.demoNavigation)
        }
        // Logging.demoNavigation-> { // exit demo-nav mode:
        //   // stopLocalization(mapView)
        //   // VM.logging.postValue(Logging.stopped)
        // }
        else -> { // ignore click
          LOG.D(TAG_METHOD, "Ignoring Demo-Navigation. status: ${VM.logging}")
        }
      }
    }

    // CHECK remove this functionality?
    btnDemoNav.setOnLongClickListener {
      if (!longClickClearCvMap) {
        scope.launch {
          statusUpdater.showWarningAutohide("Delete CvMap?", "long-click again", 2000L)
        }
        longClickClearCvMap = true
      } else {
        scope.launch {
          statusUpdater.showInfoAutohide("Deleted CvMap", 2000L)
        }
        VM.cvMapH?.clearCache()
      }

      true
    }
  }

  /**
   * When logging button is clicked and we must store, else: toggle logging
   */
  fun setupClickedLoggingButton() {
    btnLogging.setOnClickListener {
      LOG.D(TAG, "buttonStartLogging: ${VM.logging}")
      when (VM.logging.value) {
        Logging.stoppedMustStore -> {
          if (VM.objOnMAP.isEmpty())  handleStoreNoDetections()
           else handleStoreDetections(wMap.obj)
        }
        else -> VM.toggleLogging()
      }
    }

    // CLR:PM all below comments are OLD (pre-merge)
    // CLR:PM SIMPLIFY
    // logging button long clicked: forcing store?!
    btnLogging.setOnLongClickListener {
      // val btnTimer = binding.bottomUi.buttonCameraTimer
      // VM.longClickFinished = true // CLR:PM remove this variable
      // TODO hide any stuff here...
      VM.circleTimerAnimation = TimerAnimation.reset
      wMap.mapView.animateAlpha(1f, ANIMATION_DELAY)
      // buttonUtils.changeBackgroundButton(btnTimer, ctx, R.color.yellowDark)

      // this needs testing?
      statusUpdater.showInfoAutohide("stored ${VM.objOnMAP.size} locations", 3000)
      handleStoreDetections(wMap.obj)
      true
    }
  }

  fun handleStoreNoDetections() {
    statusUpdater.showWarningAutohide("Nothing stored.", "no objects attached on map", 5000L)
    VM.resetLoggingWindow()
    VM.logging.value = Logging.stopped
  }

  /**
   * It hides any active markers from the map, and if the detections are not empty:
   * - it merges detections with the local cache
   * - it updates the weighted heatmap
   */
  fun handleStoreDetections(gmap: GoogleMap) {
    storeDetectionsAndUpdateUI(gmap)
    VM.logging.value = Logging.stopped
  }

  /**
   * It stores detections using [VM], and updates the UI:
   * - shows warning when no detections captured
   * otherwise:
   * - clears the [gmap] markers
   * - updates the heatmap
   */
  fun storeDetectionsAndUpdateUI(gmap: GoogleMap) {
    // TODO show/enable an upload button
    VM.markers.hideCvObjMarkers() // CLR:PM VM.hideActiveMarkers()

    // an extra check in case of a forced storing (long click while running or paused mode)
    if (VM.objOnMAP.isEmpty()) {
      val msg = "Nothing stored."
      LOG.W(TAG, msg)
      statusUpdater.showWarningAutohide(msg, 3000)
      return
    }
    val detectionsToStored = VM.objOnMAP.size
    VM.storeDetections(VM.floorH)
    VM.cvMapH?.let { wMap.overlays.refreshHeatmap(gmap, it.getWeightedLocationList()) }
    statusUpdater.showWarningAutohide("stored $detectionsToStored locations", 3000)
  }


  // TODO:PM make BottomSheet a separate UiClass?
  val bu_TvElapsedTime: TextView = activity.findViewById(R.id.tv_elapsedTime)
  val bu_TvObjUnique: TextView = activity.findViewById(R.id.tv_windowObjectsUnique)
  val bu_TvWindowCur: TextView = activity.findViewById(R.id.tv_currentWindow)
  val bu_TvObjTotal: TextView = activity.findViewById(R.id.tv_totalObjects)
  val bu_TvTimeInfo: TextView = activity.findViewById(R.id.time_info)
  val bu_TvCropInfo: TextView = activity.findViewById(R.id.crop_info)

  val llBottomSheetInternal: LinearLayout = activity.findViewById(R.id.bottom_sheet_internal)
  val ivBottomSheetArrow: ImageView = activity.findViewById(R.id.bottom_sheet_arrow)

  val llGestureLayout: ImageView = activity.findViewById(R.id.gesture_layout)

  // CHECK:PM
  val buBottomSheet: ConstraintLayout = activity.findViewById(id_bottomsheet)
  val groupDevSettings: Group = activity.findViewById(R.id.group_devSettings)

  fun setUpBottomSheet() {
    LOG.E(TAG, "setUpBottomSheet")

    // CHECK:PM: might hit?
    val sheetBehavior = BottomSheetBehavior.from(buBottomSheet.parent as View)
    // val sheetBehavior = BottomSheetBehavior.from(binding.bottomUi.root)


    sheetBehavior.isHideable = false
    // if (!forceShow && !viewModel.prefs.devMode) { // OLD
    //   hideBottomSheet()
    //   return
    // }

    showBottomSheet()

    val callback = CvLoggerBottomSheetCallback(ivBottomSheetArrow)
    sheetBehavior.addBottomSheetCallback(callback)

    // val gestureLayout = binding.bottomUi.gestureLayout
    llGestureLayout.viewTreeObserver.addOnGlobalLayoutListener {
      sheetBehavior.peekHeight = ivBottomSheetArrow.bottom + 60
      LOG.V4(TAG, "peek height: ${sheetBehavior.peekHeight}")
    }

    // TODO:PM get detectionModel and setup sizes
    // val model = VMb.detector.getDetectionModel()
    @SuppressLint("SetTextI18n")
    bu_TvCropInfo.text = "<NAN>x<NAN>"
    // binding.bottomUi.cropInfo.text = "${model.inputSize}x${model.inputSize}"
  }

  // TODO:PM move to CvMap
  private fun hideBottomSheet() {
    llBottomSheetInternal.visibility = View.GONE // CHECK: binding.bottomUi.bottomSheetInternal
    ivBottomSheetArrow.visibility = View.GONE // CHECK: binding.bottomUi.bottomSheetArrow
  }

  private fun showBottomSheet() {
    llBottomSheetInternal.visibility = View.VISIBLE
    ivBottomSheetArrow.visibility = View.VISIBLE

    // OLD comments (except CLR)
    // hide developer options: TODO:PM once options are in place
    // if (viewModel.prefs.devMode) {
    groupDevSettings.visibility = View.VISIBLE  //  CLR:PM binding.bottomUi.groupDevSettings
    // }
  }

  class CvLoggerBottomSheetCallback(
          private val ivArrow: ImageView) : BottomSheetBehavior.BottomSheetCallback() {
    override fun onStateChanged(bottomSheet: View, newState: Int) {
      when (newState) {
        BottomSheetBehavior.STATE_HIDDEN -> { }
        BottomSheetBehavior.STATE_EXPANDED -> { ivArrow.setImageResource(cy.ac.ucy.cs.anyplace.lib.R.drawable.ic_icon_down) }
        BottomSheetBehavior.STATE_COLLAPSED -> { ivArrow.setImageResource(cy.ac.ucy.cs.anyplace.lib.R.drawable.ic_icon_up) }
        BottomSheetBehavior.STATE_DRAGGING -> { }
        BottomSheetBehavior.STATE_SETTLING -> { ivArrow.setImageResource(cy.ac.ucy.cs.anyplace.lib.R.drawable.ic_icon_up) }
        BottomSheetBehavior.STATE_HALF_EXPANDED -> { }
      }
    }
    override fun onSlide(bottomSheet: View, slideOffset: Float) {}
  }

}