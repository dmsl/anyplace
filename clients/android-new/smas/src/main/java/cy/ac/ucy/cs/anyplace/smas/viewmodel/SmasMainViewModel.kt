package cy.ac.ucy.cs.anyplace.smas.viewmodel

import android.app.Application
import android.widget.Toast
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import androidx.preference.Preference
import cy.ac.ucy.cs.anyplace.lib.android.data.RepoAP
import cy.ac.ucy.cs.anyplace.lib.android.data.store.CvNavDataStore
import cy.ac.ucy.cs.anyplace.lib.android.data.store.MiscDataStore
import cy.ac.ucy.cs.anyplace.lib.android.extensions.app
import cy.ac.ucy.cs.anyplace.lib.android.utils.network.RetrofitHolderAP
import cy.ac.ucy.cs.anyplace.lib.android.viewmodels.CvMapViewModel
import cy.ac.ucy.cs.anyplace.smas.SmasApp
import cy.ac.ucy.cs.anyplace.smas.consts.CHAT
import cy.ac.ucy.cs.anyplace.smas.data.RepoChat
import cy.ac.ucy.cs.anyplace.smas.data.store.ChatPrefsDataStore
import cy.ac.ucy.cs.anyplace.smas.utils.network.RetrofitHolderChat
import cy.ac.ucy.cs.anyplace.smas.viewmodel.util.SmasUserLocations
import cy.ac.ucy.cs.anyplace.smas.viewmodel.util.SmasVersion
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class SmasMode {
  alert,
  normal,
}

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
        private val retrofitHolderChat: RetrofitHolderChat,
        retrofitHolderAP: RetrofitHolderAP):
        CvMapViewModel(application, navDS, repoAP, retrofitHolderAP) {

  private val C by lazy { CHAT(app.applicationContext) }

  // PREFERENCES
  val prefsChat = chatPrefsDS.read

  override fun prefWindowLocalizationMillis(): Int {
    // TODO:PM modify properly for Smas?
    return C.DEFAULT_PREF_CVLOG_WINDOW_LOCALIZATION_SECONDS.toInt()
  }

  //// RETROFIT
  private val version by lazy { SmasVersion(app as SmasApp, retrofitHolderChat, repoChat) }
  val userLocations by lazy { SmasUserLocations(app as SmasApp, retrofitHolderChat, repoChat) }

  /**
   * [p]: the Chat [Preference] row that will be replaced with the result of the version call
   */
  fun displayVersion(p: Preference?) = viewModelScope.launch { version.safeCall(p) }

  /** In a loop: keep getting user locations */
  fun pullUserLocationsLOOP()  {
    viewModelScope.launch {
      while (true) {
        delay(navDS.first().locationRefresh.toLong()*1000)
        userLocations.getSC()
      }
    }
  }

  ///////////////////////////////////////
  ///////////////////////////////////////
  ///////////////////////////////////////
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