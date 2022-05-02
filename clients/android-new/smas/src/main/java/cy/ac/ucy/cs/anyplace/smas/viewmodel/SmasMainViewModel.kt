package cy.ac.ucy.cs.anyplace.smas.viewmodel

import android.app.Application
import android.widget.Toast
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import androidx.preference.Preference
import cy.ac.ucy.cs.anyplace.lib.android.utils.LOG
import cy.ac.ucy.cs.anyplace.lib.android.data.RepoAP
import cy.ac.ucy.cs.anyplace.lib.android.data.store.CvDataStore
import cy.ac.ucy.cs.anyplace.lib.android.data.store.CvNavDataStore
import cy.ac.ucy.cs.anyplace.lib.android.data.store.MiscDataStore
import cy.ac.ucy.cs.anyplace.lib.android.extensions.TAG_METHOD
import cy.ac.ucy.cs.anyplace.lib.android.extensions.app
import cy.ac.ucy.cs.anyplace.lib.android.ui.cv.map.GmapWrapper
import cy.ac.ucy.cs.anyplace.lib.android.utils.net.RetrofitHolderAP
import cy.ac.ucy.cs.anyplace.lib.android.viewmodels.CvMapViewModel
import cy.ac.ucy.cs.anyplace.lib.models.UserCoordinates
import cy.ac.ucy.cs.anyplace.lib.models.UserLocation
import cy.ac.ucy.cs.anyplace.smas.SmasApp
import cy.ac.ucy.cs.anyplace.smas.consts.CHAT
import cy.ac.ucy.cs.anyplace.smas.data.RepoChat
import cy.ac.ucy.cs.anyplace.smas.data.store.ChatPrefsDataStore
import cy.ac.ucy.cs.anyplace.smas.data.source.RetrofitHolderChat
import cy.ac.ucy.cs.anyplace.smas.viewmodel.util.nw.LocationGetNW
import cy.ac.ucy.cs.anyplace.smas.viewmodel.util.nw.LocationSendNW
import cy.ac.ucy.cs.anyplace.smas.viewmodel.util.nw.VersionNW
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
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
        val dsChat: ChatPrefsDataStore,
        dsCv: CvDataStore,
        dsCvNav: CvNavDataStore,
        private val dsMisc: MiscDataStore,
        private val RHchat: RetrofitHolderChat,
        RHap: RetrofitHolderAP):
        CvMapViewModel(application, dsCv, dsCvNav, repoAP, RHap) {

  private val C by lazy { CHAT(app.applicationContext) }

  // PREFERENCES
  val prefsChat = dsChat.read

  /** How often to refresh UI components from backend (in ms) */
  private var refreshMs : Long = C.DEFAULT_PREF_SMAS_LOCATION_REFRESH.toLong()*1000L

  override fun prefWindowLocalizationMillis(): Int {
    // modify properly for Smas?
    return C.DEFAULT_PREF_CVLOG_WINDOW_LOCALIZATION_SECONDS.toInt()
  }

  //// RETROFIT UTILS:
  private val nwVersion by lazy { VersionNW(app as SmasApp, RHchat, repoChat) }
  private val nwLocationGet by lazy { LocationGetNW(app as SmasApp, this, RHchat, repoChat) }
  private val nwLocationSend by lazy { LocationSendNW(app as SmasApp, this, RHchat, repoChat) }

  val alertingUser : MutableStateFlow<UserLocation?>
    get() = nwLocationGet.alertingUser

  /**
   * [p]: the Chat [Preference] row that will be replaced with the result of the version call
   */
  fun displayVersion(p: Preference?) = viewModelScope.launch { nwVersion.safeCall(p) }

  private fun collectRefreshMs() {
    viewModelScope.launch(Dispatchers.IO) {
      prefsCvNav.collectLatest{ refreshMs = it.locationRefresh.toLong()*1000L }
    }
  }

  /** In a loop:
   * - send own location
   * - get other users locations
   *
   *  - TODO:PM get from anyplace location
   *  - TODO:PM get a list of those locations: how? parse json?
   */
  fun netPullLocationsLOOP()  {
    viewModelScope.launch(Dispatchers.IO) {
      collectRefreshMs()
      while (true) {
        if (location.value.coord != null) {
          val lastCoordinates = UserCoordinates(spaceH.obj.id,
                  floorH?.obj!!.floorNumber.toInt(),
                  location.value.coord!!.lat,
                  location.value.coord!!.lon)

          nwLocationSend.safeCall(lastCoordinates)
        }

        nwLocationGet.safeCall()
        delay(refreshMs)
      }
    }
  }

  /**
   * React to user location updates:
   * - for current user [nwLocationSend]
   * - for other users [nwLocationGet]
   */
  fun collectLocations(mapH: GmapWrapper) {
    if (floor.value == null) {  // floor not ready yet
      LOG.W(TAG_METHOD, "Floor not loaded yet")
      return
    }

    viewModelScope.launch(Dispatchers.IO) {
      nwLocationSend.collect()
    }

    viewModelScope.launch(Dispatchers.IO) {
      nwLocationGet.collect(this@SmasMainViewModel, mapH)
    }

  }

  fun toggleAlert() : LocationSendNW.Mode {
    val newMode = if (nwLocationSend.alerting()) {
      LocationSendNW.Mode.normal
    } else {
      LocationSendNW.Mode.alert
    }

    nwLocationSend.mode.value = newMode
    return newMode
  }

  fun saveNewMsgs(value: Boolean) {
    viewModelScope.launch(Dispatchers.IO) {
      dsChat.saveNewMsgs(value)
    }
  }

  ///////////////////////////////////////
  ///////////////////////////////////////
  ///////////////////////////////////////
  // TODO: network manager?
  // TODO these in the MainViewModel (SmassMainVM or a centrally main VM).
  var networkStatus = false
  /** normal var, filled by the observer (SelectSpaceActivity) */
  var backOnline = false

  /** Set when a user has new messages */
  var readHasNewMessages = dsChat.readHasNewMessages.asLiveData()

  // TODO:PM: bind this when connectivity status changes
  var readBackOnline = dsMisc.readBackOnline.asLiveData()
  // TODO? for chat?
  // var readUserLoggedIn = dataStoreUser.readUser.asLiveData()
  var backFromSettings= false // INFO filled by the observer (collected from the fragment)
  var readBackFromSettings= dsMisc.readBackFromSettings.asLiveData()
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
            dsMisc.saveBackOnline(value)
          }
  fun setBackFromSettings() = saveBackFromSettings(true)
  fun unsetBackFromSettings() = saveBackFromSettings(false)
  private fun saveBackFromSettings(value: Boolean) =
          viewModelScope.launch(Dispatchers.IO) {  dsMisc.saveBackFromSettings(value) }



  ///////////////////////////////////////
  ///////////////////////////////////////
  ///////////////////////////////////////

}