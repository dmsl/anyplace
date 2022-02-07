package cy.ac.ucy.cs.anyplace.smass

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.PersistableBundle
import android.view.ViewTreeObserver
import android.widget.ImageView
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetBehavior
import cy.ac.ucy.cs.anyplace.lib.android.LOG
import cy.ac.ucy.cs.anyplace.lib.android.ui.cv.yolov4tflite.DetectorActivityBase
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 */
@AndroidEntryPoint
class SmassActivity : DetectorActivityBase() {
  // PROVIDE TO BASE CLASS [CameraActivity]:
  override val layout_activity: Int get() = R.layout.activity_cvmap
  override val id_bottomsheet: Int get() = R.id.bottom_sheet_cvmap
  override val id_gesture_layout: Int get() = R.id.gesture_layout

  // BottomSheet specific details (default ones)
  lateinit var frameValueTextView: TextView
  lateinit var cropValueTextView: TextView
  lateinit var inferenceTimeTextView: TextView
  lateinit var bottomSheetArrowImageView: ImageView

  override fun onCreate(savedInstanceState: Bundle?, persistentState: PersistableBundle?) {
    LOG.D()
    super.onCreate(savedInstanceState, persistentState)
  }

  override fun setupSpecializedUi() {
    setupBottomSheet()
  }

  private fun setupBottomSheet() {
    bottomSheetArrowImageView = findViewById(R.id.bottom_sheet_arrow)
    sheetBehavior = BottomSheetBehavior.from(bottomSheetLayout)
    val vto = gestureLayout.viewTreeObserver

    vto.addOnGlobalLayoutListener(
            object : ViewTreeObserver.OnGlobalLayoutListener {
              override fun onGlobalLayout() {
                gestureLayout.viewTreeObserver.removeOnGlobalLayoutListener(this)
                val height = gestureLayout.measuredHeight
                sheetBehavior.peekHeight = height/3
              }
            })

    sheetBehavior.isHideable = false
    setupBottomStageChange(bottomSheetArrowImageView,
            R.drawable.ic_icon_down, R.drawable.ic_icon_up)

    // frameValueTextView = findViewById(R.id.frame_info)
    // cropValueTextView = findViewById(R.id.crop_info)
    // inferenceTimeTextView = findViewById(R.id.inference_info)
  }

  override fun onProcessImageFinished() {
    LOG.D()
    lifecycleScope.launch(Dispatchers.Main) {
      updateUiBottomSheet()
    }
  }

  @SuppressLint("SetTextI18n")
  private fun updateUiBottomSheet() {
    // frameValueTextView.text = "${previewWidth}x${previewHeight}"
    // val w = cropCopyBitmap.width
    // val h = cropCopyBitmap.height
    // cropValueTextView.text = "${w}x${h}"
    // inferenceTimeTextView.text =  "${lastProcessingTimeMs}ms"
  }
}