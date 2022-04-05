package cy.ac.ucy.cs.anyplace.smas.ui

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.widget.Button
import android.widget.Toast
import androidx.compose.material.ExperimentalMaterialApi
import androidx.constraintlayout.widget.Group
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.android.gms.maps.OnMapReadyCallback
import cy.ac.ucy.cs.anyplace.lib.android.LOG
import cy.ac.ucy.cs.anyplace.lib.android.data.models.helpers.FloorHelper
import cy.ac.ucy.cs.anyplace.lib.android.extensions.*
import cy.ac.ucy.cs.anyplace.lib.android.ui.cv.CvMapActivity
import cy.ac.ucy.cs.anyplace.lib.android.ui.cv.map.GmapHandler
import cy.ac.ucy.cs.anyplace.lib.android.utils.ui.OutlineTextView
import cy.ac.ucy.cs.anyplace.lib.android.utils.ui.utlButton
import cy.ac.ucy.cs.anyplace.lib.android.viewmodels.CvMapViewModel
import cy.ac.ucy.cs.anyplace.lib.android.viewmodels.DetectorViewModel
import cy.ac.ucy.cs.anyplace.lib.android.viewmodels.Localization
import cy.ac.ucy.cs.anyplace.lib.core.LocalizationResult
import cy.ac.ucy.cs.anyplace.smas.R
import cy.ac.ucy.cs.anyplace.smas.extensions.appSmas
import cy.ac.ucy.cs.anyplace.smas.ui.settings.dialogs.MainSmasSettingsDialog
import cy.ac.ucy.cs.anyplace.smas.viewmodel.SmasChatViewModel
import cy.ac.ucy.cs.anyplace.smas.viewmodel.SmasMainViewModel
import cy.ac.ucy.cs.anyplace.smas.viewmodel.util.nw.LocationSendNW
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/*
  CONST: from string/xml
  - prefs:
    - xml
    - ChatDataStore
    - SettingsActivity (uses CDS)

  - RetrofitSetup: (with all the above)
    - RetrofitHolder: VERIFY dynamically adapt to domain change

    - API Setup:
      - remoteDataSources (talks with API interface)
      - connection verification with server (due to some issues)1
    - ENDPOINTS:
      - version: check I can talk
      - login: working on it (below)
    - models

    - loginProgrammatically
      - TODO build UI for this
      - TODO persist in a ChatUser DataStore

  - ViewModelChat: this will be used by Athina

  - working on:
    - ChatUserDataSource: to preserve the logged in user

    - TODO: persist user login: store ChatUser DS

   */
@AndroidEntryPoint
class SmasMainActivity : CvMapActivity(), OnMapReadyCallback {

  // PROVIDE TO BASE CLASS [CvMapActivity]:
  override val layout_activity: Int get() = R.layout.activity_smas
  override val id_bottomsheet: Int get() = R.id.bottom_sheet_cvmap
  override val id_gesture_layout: Int get() = R.id.gesture_layout
  override val id_gmap: Int get() = R.id.mapView

  @Suppress("UNCHECKED_CAST")
  override val view_model_class: Class<DetectorViewModel> =
          SmasMainViewModel::class.java as Class<DetectorViewModel>

  // VIEW MODELS
  /** extends [CvMapViewModel] */
  private lateinit var VM: SmasMainViewModel
  /** Async handling of SMAS Messages and Alerts */
  private lateinit var VMchat: SmasChatViewModel

  // UI COMPONENTS
  private lateinit var btnChat: Button
  private lateinit var btnFlir: Button
  private lateinit var btnSettings: Button
  private lateinit var btnAlert: Button
  private lateinit var btnLocalization: Button

  /**
   * Called by [CvMapActivity]
   */
  override fun setupButtonsAndUi() {
    super.setupButtonsAndUi() // TODO floor selector
    LOG.D2()

    setupButtonSettings()
    setupButtonLocalization()
    setupButtonChat()
    setupButtonFlir()
    setupButtonAlert()
  }

  /**
   * Called by [CvMapActivity] ? (some parent method)
   */
  override fun postCreate() {
    super.postCreate()
    LOG.D2()

    VM = _vm as SmasMainViewModel
    VMchat = ViewModelProvider(this)[SmasChatViewModel::class.java]

    setupBackendCommunication()
    setupCollectors()
  }

  /**
   * Runs only once, when any of the floors is loaded for the first time.
   */
  private fun onFloorLoaded() {
    LOG.D2(TAG_METHOD, "Floor: ${VM.floor.value}")

    // Send own location, and receive other users locations
    VM.nwPullLocationsLoop()
    collectOwnLocation()
    VM.collectLocations(mapH)

    setupFakeUserLocation(mapH) // TODO:PMX
    // collect alert
  }

  ////////////////////////////////////////////////

  /**
   * Pulling data (maybe periodically) from the SMAS backend
   * Also reporting user locations
   */
  private fun setupBackendCommunication() {
    // TODO:ATH
    VMchat.nwPullMessages()
  }

  /**
   * Async Collection of remotely fetched data
   * TODO local cache (SQLITE)
   */
  private fun setupCollectors() {
    LOG.D()

    collectLoggedInUser()
    collectLoadedFloors()

    VMchat.collectMessages() // TODO:ATH

    // NOTE: [collectOtherUsersLocations] is done on floorLoaded
    // collectUserLocalizationStatus(): localizing or not localizing
  }

  @Deprecated("TODO replace with Anyplace Location")
  private fun setupFakeUserLocation(mapH: GmapHandler) {
    val loc = VM.spaceH.latLng().toCoord()
    VM.location.value = LocalizationResult.Success(loc)

    mapH.obj.setOnMapLongClickListener {
      LOG.W(TAG, "Setting fake location: $it")
      VM.location.value = LocalizationResult.Success(it.toCoord())
    }
  }

  var firstFloorLoaded = false
  /**
   * Observes when the initial floor will be loaded, and runs a method
   */
  private fun collectLoadedFloors() {
    lifecycleScope.launch {
      VM.floor.collect { floor ->
        if (floor == null) return@collect

        LOG.D4(TAG, "collectLoadedFloors: is spaceH filled? ${VM.spaceH.obj.name}")
        // Update FH
        VM.floorH = FloorHelper(floor, VM.spaceH)

        if (firstFloorLoaded) {
          cancel()
        } else {
          onFloorLoaded()
          firstFloorLoaded = true
        }
      }
    }
  }

  /**
   * Reacts to updates on [ChatUser]'s login status:
   * Only authenticated users are allowed to use this activity
   */
  private fun collectLoggedInUser() {
    // only logged in users are allowed on this activity:
    lifecycleScope.launch {
      appSmas.dsChatUser.readUser.collect { user ->
        if (user.sessionkey.isBlank()) {
          finish()
          startActivity(Intent(this@SmasMainActivity, SmasLoginActivity::class.java))
        } else {
          Toast.makeText(applicationContext, "Welcome ${user.uid}!", Toast.LENGTH_LONG).show()
        }
      }
    }
  }

  /**
   * React when a user is in alert mode
   */
  @SuppressLint("SetTextI18n")
  private fun collectAlertingUser() { // TODO:PMX
    lifecycleScope.launch {
      val group : Group = findViewById(R.id.group_userAlert)
      val tvUserAlert : OutlineTextView = findViewById(R.id.tv_alertUser)
      val tvAlertTitle: OutlineTextView = findViewById(R.id.tv_alertTitle)
      VM.alertingUser.collect {
        if (it == null) { // no user alerting
          // btnAlert.visibility = View.VISIBLE
          group.fadeOut()
          delay(100)
          btnAlert.fadeIn()
          tvAlertTitle.clearAnimation()
          // group.visibility = View.INVISIBLE
        } else { // user alerting
          tvUserAlert.text = "${it.name} ${it.surname}"
          btnAlert.fadeOut()
          delay(100)
          group.fadeIn()
          delay(100)
          tvAlertTitle.flashingLoop()
          // btnAlert.visibility = View.INVISIBLE
          // group.visibility = View.VISIBLE
        }
      }
    }
  }

  private fun setupButtonAlert() {
    btnAlert = findViewById(R.id.btnAlert)
    btnAlert.setOnClickListener {
     Toast.makeText(applicationContext, "Use long-press", Toast.LENGTH_SHORT).show()
    }

    btnAlert.setOnLongClickListener {
      when (VM.toggleAlert()) {
        LocationSendNW.Mode.alert -> {
          btnAlert.flashingLoop()
          btnAlert.text = "ALERTING"
          utlButton.changeBackgroundButton(btnAlert, this, R.color.redDark)
          btnAlert.setTextColor(Color.WHITE)
        }
        LocationSendNW.Mode.normal -> {
          btnAlert.clearAnimation()
          btnAlert.text = "SEND ALERT"
          btnAlert.setTextColor(Color.BLACK)
          utlButton.changeBackgroundButton(btnAlert, this, R.color.yellowDark)
        }
      }
      true
    }
  }

  /**
   * TODO this could toggle the heavyweight DNN engine
   */
  private fun setupButtonLocalization() {
    btnFlir = findViewById(R.id.button_flir)
    btnFlir.setOnClickListener {
      lifecycleScope.launch {
      }
    }
  }

  private fun setupButtonSettings() {
    btnSettings = findViewById(R.id.button_settings)
    btnSettings.setOnClickListener {
      MainSmasSettingsDialog.SHOW(supportFragmentManager, MainSmasSettingsDialog.FROM_MAIN)
    }
  }

  // TODO CHECK:PM
  private suspend fun collectLocalizationStatus() {
    VM.localization.collect {  status ->
      when (status) {
        Localization.running -> {
          utlButton.changeBackgroundButtonCompat(btnLocalization, applicationContext,
                  cy.ac.ucy.cs.anyplace.lib.R.color.colorPrimary)
        }
        else -> {
          utlButton.changeBackgroundButtonCompat(btnLocalization, applicationContext,
                  cy.ac.ucy.cs.anyplace.lib.R.color.gray)
        }
      }
    }
  }

  // CLR:PM
  // // TODO ADAPT this to get all user locations..
  /**
   * Collect own user's location that is calculated via the
   * Anyplace CV-based localization engine
   */
  private fun collectOwnLocation() {
    LOG.E()
    lifecycleScope.launch {
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
  // CLR:PM
  // private suspend fun setupFakeLocation() {
  //   LOG.E(TAG_METHOD)
  //   delay(2000)
  //   LOG.E(TAG_METHOD, " Continuing..")
  //   val coord = Coord(57.69531517496923, 11.913105745699344)
  //   VM.location.value = LocalizationResult.Unset()
  //   VM.location.value = LocalizationResult.Success(coord)
  //
  //   delay(5000)
  //   VM.location.value = LocalizationResult.Unset()
  // }

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

  @OptIn(ExperimentalMaterialApi::class, ExperimentalPermissionsApi::class) // compose
  private fun setupButtonChat() {
    LOG.D()
    btnChat = findViewById(R.id.button_chat)
    btnChat.setOnClickListener {
      lifecycleScope.launch {
        // TODO:ATH
        // val intent = Intent(applicationContext, SmasChatActivity::class.java) // addon
        // startActivity(intent)
      }
    }
  }

  // TODO search: removed for now
  // private fun setupButtonFind() {
  //   LOG.D()
  //   btnFind= findViewById(R.id.button_find)
  //   btnFind.setOnClickListener {
  //     FindDialog.SHOW(supportFragmentManager, VM.repository)
  //   }
  // }
}