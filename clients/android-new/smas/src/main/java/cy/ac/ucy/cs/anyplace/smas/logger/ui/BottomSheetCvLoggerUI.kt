package cy.ac.ucy.cs.anyplace.smas.logger.ui

import android.annotation.SuppressLint
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.widget.AppCompatButton
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.Group
import com.google.android.material.button.MaterialButton
import cy.ac.ucy.cs.anyplace.lib.android.extensions.TAG
import cy.ac.ucy.cs.anyplace.lib.android.ui.cv.map.BottomSheetCvUI
import cy.ac.ucy.cs.anyplace.lib.android.ui.cv.yolo.tflite.DetectorActivityBase
import cy.ac.ucy.cs.anyplace.lib.android.utils.LOG
import cy.ac.ucy.cs.anyplace.smas.R
import cy.ac.ucy.cs.anyplace.smas.logger.viewmodel.CvLoggerViewModel

class BottomSheetCvLoggerUI(
        private val act: CvLoggerActivity,
        val VMlog: CvLoggerViewModel,
        val id_bottomsheet: Int)
  : BottomSheetCvUI(act as DetectorActivityBase, true) {

  // val llBottomSheet: ConstraintLayout by lazy {act.findViewById(id_bottomsheet) }

  // TODO replace  bu_TvTimeInfo by: tvTimeInfo
  // TODO replace bu_TvCropInfo: with tvCropInfo
  // TODO replace ivBottomSheetArrow with  ivArrowImg

  val tvWindowObjectsAll : TextView by lazy { act.findViewById(R.id.tv_windowObjectsAll) }
  val btnLogging : AppCompatButton by lazy { act.findViewById(R.id.button_logging) }
  val btnClearObj: MaterialButton by lazy { act.findViewById(R.id.button_clearObjects) }
  val btnTimer : MaterialButton by lazy { act.findViewById(R.id.button_cameraTimer) }
  val groupTutorial : Group by lazy { act.findViewById(R.id.group_tutorial) }

  val llBottomSheetInternal: LinearLayout  by lazy { act.findViewById(R.id.bottom_sheet_internal) }
  val ivBottomSheetArrow: ImageView by lazy { act.findViewById(R.id.bottom_sheet_arrow) }
  val llGestureLayout: ConstraintLayout by lazy {act.findViewById(R.id.gesture_layout) }
  val groupDevSettings: Group by lazy { act.findViewById(R.id.group_devSettings) }

  val tvElapsedTime: TextView by lazy { act.findViewById(R.id.tv_elapsedTime) }
  val tvObjUnique: TextView by lazy { act.findViewById(R.id.tv_windowObjectsUnique) }
  val tvCurWindow: TextView by lazy { act.findViewById(R.id.tv_currentWindow) }
  val tvObjTotal: TextView by lazy { act.findViewById(R.id.tv_totalObjects) }


  // TODO: in parent BottomSheetCvUI?
  // NAV COMMON shared between activities? (pre-merge)
  fun bindCvStats() {
    tvElapsedTime.text=VMlog.getElapsedSecondsStr()
    tvObjUnique.text=VMlog.objWindowUnique.toString()
    tvCurWindow.text=VMlog.objOnMAP.size.toString()
    tvObjTotal.text=VMlog.objTotal.toString()
  }

  // TODO: in parent BottomSheetCvUI? override?!
  override fun hideBottomSheet() {
    super.hideBottomSheet()

    llBottomSheetInternal.visibility = View.GONE // CHECK: binding.bottomUi.bottomSheetInternal
    ivBottomSheetArrow.visibility = View.GONE // CHECK: binding.bottomUi.bottomSheetArrow
  }

  override fun showBottomSheet() {
    super.showBottomSheet()

    llBottomSheetInternal.visibility = View.VISIBLE
    ivBottomSheetArrow.visibility = View.VISIBLE

    // OLD comments (except CLR)
    // hide developer options: TODO:PM once options are in place
    // if (viewModel.prefs.devMode) {
    groupDevSettings.visibility = View.VISIBLE  //  CLR:PM binding.bottomUi.groupDevSettings
    // }
  }

  // fun setup() {
  //   super.setup()
  //     LOG.E(TAG, "setup: BottomSheetCV")
  //     LOG.E(TAG, METHOD)
  //
  //     // CHECK:PM: might hit?
  //     // val sheetBehavior = BottomSheetBehavior.from(buBottomSheet as View)
  //     // val sheetBehavior = BottomSheetBehavior.from(buBottomSheet.parent as View)
  //     // val sheetBehavior = BottomSheetBehavior.from(binding.bottomUi.root)
  //
  //     // sheetBehavior.isHideable = false
  //     // if (!forceShow && !viewModel.prefs.devMode) { // OLD
  //     //   hideBottomSheet()
  //     //   return
  //     // }
  //
  //     // showBottomSheet()
  //
  //     // val callback = BottomSheetCallback(ivBottomSheetArrow)
  //     // sheetBehavior.addBottomSheetCallback(callback)
  //
  //     // val gestureLayout = binding.bottomUi.gestureLayout
  //
  // }

  override fun setupSpecialize() {

    // Peak height setup
    llGestureLayout.viewTreeObserver.addOnGlobalLayoutListener {
      act.sheetBehavior.peekHeight = ivBottomSheetArrow.bottom + 60
      LOG.V4(TAG, "peek height: ${act.sheetBehavior.peekHeight}")
    }

    // TODO:PM get detectionModel and setup sizes
    // val model = VMb.detector.getDetectionModel()
    @SuppressLint("SetTextI18n")
    tvCropInfo.text = "<NAN>x<NAN>"
    // binding.bottomUi.cropInfo.text = "${model.inputSize}x${model.inputSize}"
  }
}