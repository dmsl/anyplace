package cy.ac.ucy.cs.anyplace.smas.viewmodel

import android.app.Application
import android.widget.Toast
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import androidx.preference.Preference
import cy.ac.ucy.cs.anyplace.lib.android.LOG
import cy.ac.ucy.cs.anyplace.lib.android.data.RepoAP
import cy.ac.ucy.cs.anyplace.lib.android.data.store.CvNavDataStore
import cy.ac.ucy.cs.anyplace.lib.android.data.store.MiscDataStore
import cy.ac.ucy.cs.anyplace.lib.android.extensions.TAG_METHOD
import cy.ac.ucy.cs.anyplace.lib.android.extensions.app
import cy.ac.ucy.cs.anyplace.lib.android.ui.cv.map.GmapHandler
import cy.ac.ucy.cs.anyplace.lib.android.utils.network.RetrofitHolderAP
import cy.ac.ucy.cs.anyplace.lib.android.viewmodels.CvMapViewModel
import cy.ac.ucy.cs.anyplace.lib.models.UserCoordinates
import cy.ac.ucy.cs.anyplace.lib.models.UserLocation
import cy.ac.ucy.cs.anyplace.smas.SmasApp
import cy.ac.ucy.cs.anyplace.smas.consts.CHAT
import cy.ac.ucy.cs.anyplace.smas.data.RepoChat
import cy.ac.ucy.cs.anyplace.smas.data.store.ChatPrefsDataStore
import cy.ac.ucy.cs.anyplace.smas.utils.network.RetrofitHolderChat
import cy.ac.ucy.cs.anyplace.smas.viewmodel.util.LocationGetUtil
import cy.ac.ucy.cs.anyplace.smas.viewmodel.util.LocationSendUtil
import cy.ac.ucy.cs.anyplace.smas.viewmodel.util.VersionUtil
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Extends [CvMapViewModel]:
 * - TODO here merge chat / messages
 */
@HiltViewModel
class SmasMainViewModel @Inject constructor(
        application: Application,
        repoAP: RepoAP,
        private val repoChat: RepoChat,
        chatPrefsDS: ChatPrefsDataStore,
        navDS: CvNavDataStore,
        private val miscDS: MiscDataStore,
        private val chatRFH: RetrofitHolderChat,
        apRFH: RetrofitHolderAP):
        CvMapViewModel(application, navDS, repoAP, apRFH) {

  private val C by lazy { CHAT(app.applicationContext) }

  // PREFERENCES
  val prefsChat = chatPrefsDS.read

  override fun prefWindowLocalizationMillis(): Int {
    // TODO:PM modify properly for Smas?
    return C.DEFAULT_PREF_CVLOG_WINDOW_LOCALIZATION_SECONDS.toInt()
  }

  //// RETROFIT UTILS:
  private val utlVersion by lazy { VersionUtil(app as SmasApp, chatRFH, repoChat) }
  private val utlLocationGet by lazy { LocationGetUtil(app as SmasApp, this, chatRFH, repoChat) }
  private val utlLocationSend by lazy { LocationSendUtil(app as SmasApp, this, chatRFH, repoChat) }

  val alertingUser : MutableStateFlow<UserLocation?>
    get() = utlLocationGet.alertingUser

  /**
   * [p]: the Chat [Preference] row that will be replaced with the result of the version call
   */
  fun displayVersion(p: Preference?) = viewModelScope.launch { utlVersion.safeCall(p) }

  /** In a loop:
   * - send own location
   * - get other users locations
   *
   *     TODO:PM get from anyplace location
   *     TODO:PM get a list of those locations: how? parse json?
   */
  fun updateLocationsLoop()  {
    viewModelScope.launch {
      while (true) {
        if (location.value.coord != null) {
          val lastCoordinates = UserCoordinates(spaceH.obj.id,
                  floorH?.obj!!.floorNumber.toInt(),
                  location.value.coord!!.lat,
                  location.value.coord!!.lon)

          utlLocationSend.safeCall(lastCoordinates)
        }

        utlLocationGet.safeCall()

        delay(navDS.first().locationRefresh.toLong()*1000)
      }
    }
  }

  /**
   * React to user location updates:
   * - for current user [utlLocationSend]
   * - for other users [utlLocationGet]
   */
  fun collectLocations(mapH: GmapHandler) {
    if (floor.value == null) {  // floor not ready yet
      LOG.W(TAG_METHOD, "Floor not loaded yet")
      return
    }

    viewModelScope.launch(Dispatchers.IO) {
      utlLocationSend.collect()
    }

    viewModelScope.launch(Dispatchers.IO) {
      utlLocationGet.collect(this@SmasMainViewModel, mapH)
    }

  }

  fun toggleAlert() : LocationSendUtil.Mode {
    val newMode = if (utlLocationSend.alerting()) {
      LocationSendUtil.Mode.normal
    } else {
      LocationSendUtil.Mode.alert
    }
    utlLocationSend.mode.value = newMode

    return newMode
  }

  ///////////////////////////////////////
  ///////////////////////////////////////
  ///////////////////////////////////////
  // TODO: network manager?
  // TODO these in the MainViewModel (SmassMainVM or a centrally main VM).
  var networkStatus = false
  /** normal var, filled by the observer (SelectSpaceActivity) */
  var backOnline = false

  // TODO:PM: bind this when connectivity status changes
  var readBackOnline = miscDS.readBackOnline.asLiveData()
  // TODO? for chat?
  // var readUserLoggedIn = dataStoreUser.readUser.asLiveData()
  var backFromSettings= false // INFO filled by the observer (collected from the fragment)
  var readBackFromSettings= miscDS.readBackFromSettings.asLiveData()
  fun showNetworkStatus() {
    if (!networkStatus) {
      Toast.makeText(getApplication(), "No internet connection!", Toast.LENGTH_SHORT).show()
      saveBackOnline(true)
    } else if(networkStatus && backOnline)  {
      Toast.makeText(getApplication(), "Back online!", Toast.LENGTH_SHORT).show()
      saveBackOnline(false)
    }
  }
  private fun saveBackOnline(value: Boolean) =
          viewModelScope.launch(Dispatchers.IO) {
            miscDS.saveBackOnline(value)
          }
  fun setBackFromSettings() = saveBackFromSettings(true)
  fun unsetBackFromSettings() = saveBackFromSettings(false)
  private fun saveBackFromSettings(value: Boolean) =
          viewModelScope.launch(Dispatchers.IO) {  miscDS.saveBackFromSettings(value) }

  ///////////////////////////////////////
  ///////////////////////////////////////
  ///////////////////////////////////////

}