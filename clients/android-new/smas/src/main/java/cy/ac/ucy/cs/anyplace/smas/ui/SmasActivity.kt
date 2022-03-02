package cy.ac.ucy.cs.anyplace.smas.ui

import android.content.Intent
import android.net.Uri
import android.widget.Button
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.maps.OnMapReadyCallback
import cy.ac.ucy.cs.anyplace.lib.android.LOG
import cy.ac.ucy.cs.anyplace.lib.android.extensions.TAG
import cy.ac.ucy.cs.anyplace.lib.android.extensions.TAG_METHOD
import cy.ac.ucy.cs.anyplace.lib.android.ui.cv.CvMapActivity
import cy.ac.ucy.cs.anyplace.lib.android.utils.ui.buttonUtils
import cy.ac.ucy.cs.anyplace.lib.android.viewmodels.CvMapViewModel
import cy.ac.ucy.cs.anyplace.lib.android.viewmodels.DetectorViewModel
import cy.ac.ucy.cs.anyplace.lib.android.viewmodels.Localization
import cy.ac.ucy.cs.anyplace.lib.core.LocalizationResult
import cy.ac.ucy.cs.anyplace.lib.models.Coord
import cy.ac.ucy.cs.anyplace.smas.R
import cy.ac.ucy.cs.anyplace.smas.ui.dialogs.FindDialog
import cy.ac.ucy.cs.anyplace.smas.ui.dialogs.MainSmassSettingsDialog
import cy.ac.ucy.cs.anyplace.smas.viewmodel.SmasViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SmasActivity : CvMapActivity(), OnMapReadyCallback {

  // PROVIDE TO BASE CLASS [CameraActivity]:
  override val layout_activity: Int get() = R.layout.smass_activity
  override val id_bottomsheet: Int get() = R.id.bottom_sheet_cvmap
  override val id_gesture_layout: Int get() = R.id.gesture_layout

  @Suppress("UNCHECKED_CAST")
  override val view_model_class: Class<DetectorViewModel> =
          SmasViewModel::class.java as Class<DetectorViewModel>

  // VIEW MODELS
  /** extends [CvMapViewModel] */
  private lateinit var VM: SmasViewModel

  private lateinit var btnChat: Button
  private lateinit var btnFind: Button
  private lateinit var btnFlir: Button
  private lateinit var btnSettings: Button
  private lateinit var btnAlert: Button
  private lateinit var btnLocalization: Button

  override fun postCreate() {
    super.postCreate()

    VM = _vm as SmasViewModel
    LOG.D2(TAG_METHOD, "")

    setupButtonsAndUi()
    setupCollectors()
  }

   override fun setupButtonsAndUi() {
     super.setupButtonsAndUi() // TODO floor selector

     setupButtonSettings()
     setupButtonLocalization()
     setupButtonChat()
     setupButtonFind()
     setupButtonFlir()
     LOG.D(TAG_METHOD)
  }

  private fun setupCollectors() {
    LOG.E()
    collectLocation()
    // TODO collectChatMessages()
    // TODO collectLocalizationStatus(): localizing or not localizing
  }

  private fun setupButtonAlert() {
    btnAlert = findViewById(R.id.btnAlert)
    btnAlert.setOnClickListener {
     Toast.makeText(applicationContext, "SENDING ALERT", Toast.LENGTH_SHORT).show()
      FindDialog.SHOW(supportFragmentManager, VM.repository)
    }
  }

  private fun setupButtonLocalization() {
    btnFlir = findViewById(R.id.button_flir)
    btnFlir.setOnClickListener {
      LOG.E(TAG_METHOD, "on click")
      lifecycleScope.launch {
      }
    }
  }

  private fun setupButtonSettings() {
    btnSettings = findViewById(R.id.button_settings)
    btnSettings.setOnClickListener {
      MainSmassSettingsDialog.SHOW(supportFragmentManager, MainSmassSettingsDialog.FROM_MAIN)
    }
  }




  // TODO
  private suspend fun collectLocalizationStatus() {
    VM.localization.collect {  status ->
      when (status) {
        Localization.running -> {
          buttonUtils.changeBackgroundButtonCompat(btnLocalization, applicationContext,
                  cy.ac.ucy.cs.anyplace.lib.R.color.colorPrimary)
        }
        else -> {
          buttonUtils.changeBackgroundButtonCompat(btnLocalization, applicationContext,
                  cy.ac.ucy.cs.anyplace.lib.R.color.gray)
        }
      }
    }
  }

  private fun collectLocation() {
    LOG.E()
    lifecycleScope.launch{

      setupFakeLocation()
      VM.location.collect { result ->
        when (result) {
          is LocalizationResult.Unset -> { }
          is LocalizationResult.Error -> {
            // TODO HANDLE
          }
          is LocalizationResult.Success -> {
            result.coord?.let { VM.setUserLocation(it) }
          }
        }
      }
    }
  }

  private suspend fun setupFakeLocation() {
    LOG.E(TAG_METHOD)
    delay(2000)
    LOG.E(TAG_METHOD, " Continuing..")
    val coord = Coord(57.69531517496923, 11.913105745699344)
    VM.location.value = LocalizationResult.Unset()
    VM.location.value = LocalizationResult.Success(coord)

    delay(5000)
    VM.location.value = LocalizationResult.Unset()
  }

  private fun setupButtonFlir() {
    btnFlir = findViewById(R.id.button_flir)
    val FLIR_PKG = "com.flir.myflir.s62"

    btnFlir.setOnClickListener {
      LOG.E(TAG_METHOD, "on click")
      lifecycleScope.launch {
        var intent = packageManager.getLaunchIntentForPackage(FLIR_PKG)
        if (intent == null) {
          intent = try {
            LOG.E(TAG_METHOD, "intent is null")
            Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$FLIR_PKG"))
          } catch (e: Exception) {
            LOG.E(TAG_METHOD, "" + e.message)
            Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$FLIR_PKG"))
          }
        }
        LOG.E(TAG_METHOD, "launching activity")
        startActivity(intent)
      }
    }
  }

  // private fun setupButtonAlert() {
  //   LOG.D4()
  //   // btnChat = findViewById(R.id.button_chat)
  //   btnChat.setOnClickListener {
  //     // ChatDialog.SHOW(supportFragmentManager, VM.repository)
  //   }
  // }

  private fun setupButtonChat() {
    LOG.D()
    btnChat = findViewById(R.id.button_chat)
    btnChat.setOnClickListener {
      // TODO
    }
  }

  private fun setupButtonFind() {
    LOG.D()
    btnFind= findViewById(R.id.button_find)
    btnFind.setOnClickListener {
      FindDialog.SHOW(supportFragmentManager, VM.repository)
    }
  }

  override fun onResume() {
    super.onResume()
    LOG.E(TAG, "onResume")
    // readPrefsAndContinueSetup() // CHECK: specific for this?
  }

}