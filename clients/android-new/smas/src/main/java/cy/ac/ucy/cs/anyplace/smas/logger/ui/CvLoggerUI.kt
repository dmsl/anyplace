package cy.ac.ucy.cs.anyplace.smas.logger.ui

import android.annotation.SuppressLint
import android.app.Activity
import android.provider.Contacts
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.widget.AppCompatButton
import androidx.constraintlayout.widget.Group
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.maps.MapView
import com.google.android.material.button.MaterialButton
import cy.ac.ucy.cs.anyplace.lib.R
import cy.ac.ucy.cs.anyplace.lib.android.extensions.TAG_METHOD
import cy.ac.ucy.cs.anyplace.lib.android.extensions.animateAlpha
import cy.ac.ucy.cs.anyplace.lib.android.extensions.fadeIn
import cy.ac.ucy.cs.anyplace.lib.android.extensions.fadeOut
import cy.ac.ucy.cs.anyplace.lib.android.maps.Overlays
import cy.ac.ucy.cs.anyplace.lib.android.ui.components.FloorSelector
import cy.ac.ucy.cs.anyplace.lib.android.ui.components.StatusUpdater
import cy.ac.ucy.cs.anyplace.lib.android.ui.cv.gnk.CvActivityBaseRM
import cy.ac.ucy.cs.anyplace.lib.android.ui.cv.map.GmapWrapper
import cy.ac.ucy.cs.anyplace.lib.android.utils.LOG
import cy.ac.ucy.cs.anyplace.lib.android.utils.ui.utlButton
import cy.ac.ucy.cs.anyplace.lib.android.utils.utlTime
import cy.ac.ucy.cs.anyplace.lib.android.viewmodels.Localization
import cy.ac.ucy.cs.anyplace.smas.logger.viewmodel.CvLoggerViewModel
import cy.ac.ucy.cs.anyplace.smas.logger.viewmodel.Logging
import cy.ac.ucy.cs.anyplace.smas.logger.viewmodel.TimerAnimation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class CvLoggerUI(private val activity: Activity,
                 private val scope: CoroutineScope,
                 private val VM: CvLoggerViewModel,
                private val wMap: GmapWrapper
        // fragmentManager: FragmentManager, // CHECK (on CvMapUi?)
        // floorSelector: FloorSelector, //  CHECK (on CvMapUi?)
        // overlays: Overlays, //  CHECK (on CvMapUi?)
                 )
// : UiActivityCvBase(activity, // MERGED? possible inheritance is OK
// fragmentManager,
// VM as CvViewModelBase,
// scope, statusUpdater, overlays,
// floorSelector)
{

  companion object {
    const val CAMERA_REQUEST_CODE: Int = 1
    const val CAMERA_ASPECT_RATIO: Int = AspectRatio.RATIO_4_3 // AspectRatio.RATIO_16_9
    const val OPACITY_MAP_LOGGING = 0f
    const val ANIMATION_DELAY : Long = 100
  }

  private val ctx = activity.applicationContext

  private val statusUpdater = StatusUpdater(
          activity,
          scope,
          activity.findViewById(R.id.tv_statusSticky),
          activity.findViewById(R.id.tv_msgTitle),
          activity.findViewById(R.id.tv_msgSubtitle),
          activity.findViewById(R.id.view_statusBackground),
          activity.findViewById(R.id.view_warning))

  // CHECK: this was in bottom sheet?
  val tvWindowObjectsAll : TextView = activity.findViewById(R.id.tv_windowObjectsAll)

  val btnLogging : AppCompatButton = activity.findViewById(R.id.button_logging)
  val btnDemoNav : MaterialButton = activity.findViewById(R.id.btn_demoNavigation)
  val btnTimer : MaterialButton = activity.findViewById(R.id.button_cameraTimer)
  val groupTutorial : Group = activity.findViewById(R.id.group_tutorial)

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

  /**
   * TODO:PM put in UiLogger??
   */
  @SuppressLint("SetTextI18n")
  private fun updateLoggingUi(status: Logging) {
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
        wMap.mapView.animateAlpha(OPACITY_MAP_LOGGING, CvActivityBaseRM.ANIMATION_DELAY)
      }
      Logging.stopped -> { // stopped after a pause or a store: can start logging again
        btnDemoNav.fadeIn()
        // clear btnTimer related components.. TODO make this a class..
        VM.circleTimerAnimation = TimerAnimation.reset
        btnTimer.fadeOut()
        binding.bottomUi.progressBarTimer.fadeOut()
        VM.circleTimerAnimation = TimerAnimation.paused
        if (VM.previouslyPaused) {
          btnLogging.text = "resume"
        } else {
          btnLogging.text = "scan"
          binding.bottomUi.groupTutorial.visibility = View.VISIBLE
        }
        utlButton.changeBackgroundButtonCompat(btnLogging, applicationContext, R.color.colorPrimary)
        binding.mapView.animateAlpha(1f, CvActivityBaseRM.ANIMATION_DELAY)
        utlButton.changeBackgroundButtonDONT_USE(btnTimer, applicationContext, R.color.darkGray)
      }
      Logging.stoppedNoDetections -> { // stopped after no detections: retry a scan
        btnDemoNav.visibility = View.GONE
        VM.circleTimerAnimation = TimerAnimation.reset
        lifecycleScope.launch {
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

        binding.mapView.animateAlpha(1f, CvActivityBaseRM.ANIMATION_DELAY)
        utlButton.changeBackgroundButtonDONT_USE(btnTimer, applicationContext, R.color.yellowDark)

        val storedDetections = VM.storedDetections.size
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

}