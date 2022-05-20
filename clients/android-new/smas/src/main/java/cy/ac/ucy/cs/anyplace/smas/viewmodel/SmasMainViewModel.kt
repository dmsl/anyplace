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
import cy.ac.ucy.cs.anyplace.lib.android.extensions.METHOD
import cy.ac.ucy.cs.anyplace.lib.android.extensions.TAG
import cy.ac.ucy.cs.anyplace.lib.android.extensions.TAG_METHOD
import cy.ac.ucy.cs.anyplace.lib.android.extensions.app
import cy.ac.ucy.cs.anyplace.lib.android.ui.cv.map.GmapWrapper
import cy.ac.ucy.cs.anyplace.lib.android.utils.net.RetrofitHolderAP
import cy.ac.ucy.cs.anyplace.lib.android.viewmodels.CvMapViewModel
import cy.ac.ucy.cs.anyplace.lib.models.UserLocation
import cy.ac.ucy.cs.anyplace.smas.SmasApp
import cy.ac.ucy.cs.anyplace.smas.consts.CHAT
import cy.ac.ucy.cs.anyplace.smas.data.RepoSmas
import cy.ac.ucy.cs.anyplace.smas.data.store.ChatPrefsDataStore
import cy.ac.ucy.cs.anyplace.smas.data.source.RetrofitHolderSmas
import cy.ac.ucy.cs.anyplace.smas.extensions.appSmas
import cy.ac.ucy.cs.anyplace.smas.viewmodel.util.nw.LocationGetNW
import cy.ac.ucy.cs.anyplace.smas.viewmodel.util.nw.LocationSendNW
import cy.ac.ucy.cs.anyplace.smas.viewmodel.util.nw.VersionNW
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
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
        private val repoSmas: RepoSmas,
        val dsChat: ChatPrefsDataStore,
        dsCv: CvDataStore,
        dsCvNav: CvNavDataStore,
        private val dsMisc: MiscDataStore,
        private val RHchat: RetrofitHolderSmas,
        RHap: RetrofitHolderAP):
        CvMapViewModel(application, dsCv, dsMisc, dsCvNav, repoAP, RHap) {

  private val C by lazy { CHAT(app.applicationContext) }

  // PREFERENCES
  val prefsChat = dsChat.read

  /** How often to refresh UI components from backend (in ms) */
  var refreshMs : Long = C.DEFAULT_PREF_SMAS_LOCATION_REFRESH.toLong()*1000L

  override fun prefWindowLocalizationMillis(): Int {
    // modify properly for Smas?
    return C.DEFAULT_PREF_CVLOG_WINDOW_LOCALIZATION_SECONDS.toInt()
  }

  //// RETROFIT UTILS:
  val nwVersion by lazy { VersionNW(app as SmasApp, RHchat, repoSmas) }
  val nwLocationGet by lazy { LocationGetNW(app as SmasApp, this, RHchat, repoSmas) }
  val nwLocationSend by lazy { LocationSendNW(app as SmasApp, this, RHchat, repoSmas) }

  val alertingUser : MutableStateFlow<UserLocation?>
    get() = nwLocationGet.alertingUser

  /**
   * [p]: the Chat [Preference] row that will be replaced with the result of the version call
   */
  fun displayVersion(p: Preference?) = viewModelScope.launch { nwVersion.safeCallAndUpdateUi(p) }

  fun collectRefreshMs() {
    viewModelScope.launch(Dispatchers.IO) {
      prefsCvNav.collectLatest{ refreshMs = it.locationRefresh.toLong()*1000L }
    }
  }

  /**
   * React to user location updates:
   * - for current user [nwLocationSend]
   * - for other users [nwLocationGet]
   */
  fun collectLocations(VMchat: SmasChatViewModel,mapH: GmapWrapper) {
    if (floor.value == null) {  // floor not ready yet
      LOG.W(TAG_METHOD, "Floor not loaded yet")
      return
    }

    viewModelScope.launch(Dispatchers.IO) {
      nwLocationSend.collect()
    }

    viewModelScope.launch(Dispatchers.IO) {
      nwLocationGet.collect(VMchat, mapH)
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

  fun hasNewMsgs(localTs: Long?, remoteTs: Long?) : Boolean {
    LOG.V2(TAG, "$METHOD: local: $localTs remote: $remoteTs")

    return when {
      // there is no remote timestamp
      remoteTs == null || remoteTs == 0L -> false

      // remote timestamp exists, but a local does not
      remoteTs != 0L && localTs == null -> true

      // both timestamps exist, and the remote one is more up-to-date
      // (this might end up loading local+remote new data)
      (remoteTs > localTs!!) -> true

      // there is a local timestamp (localTs != null; checked above)
      // meaning the DB has data, but the msgList is empty:
      // we have not loaded the local msgs yet
      appSmas.msgList.isEmpty() -> true

      // both timestamps exist
      else -> false
    }
  }

  /** Set when a user has new messages */
  var readHasNewMessages = dsChat.readHasNewMessages.asLiveData()
}