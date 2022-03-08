package cy.ac.ucy.cs.anyplace.smas.viewmodel

import android.app.Application
import android.widget.Toast
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import androidx.preference.Preference
import cy.ac.ucy.cs.anyplace.lib.android.consts.CONST
import cy.ac.ucy.cs.anyplace.lib.android.data.RepoAP
import cy.ac.ucy.cs.anyplace.lib.android.data.store.MiscDataStore
import cy.ac.ucy.cs.anyplace.lib.android.extensions.app
import cy.ac.ucy.cs.anyplace.lib.android.utils.network.RetrofitHolderAP
import cy.ac.ucy.cs.anyplace.lib.android.viewmodels.CvMapViewModel
import cy.ac.ucy.cs.anyplace.lib.network.NetworkResult
import cy.ac.ucy.cs.anyplace.smas.SmasApp
import cy.ac.ucy.cs.anyplace.smas.consts.CHAT
import cy.ac.ucy.cs.anyplace.smas.data.RepoChat
import cy.ac.ucy.cs.anyplace.smas.data.models.ChatVersion
import cy.ac.ucy.cs.anyplace.smas.data.models.UserLocations
import cy.ac.ucy.cs.anyplace.smas.data.store.ChatPrefsDataStore
import cy.ac.ucy.cs.anyplace.smas.utils.network.RetrofitHolderChat
import cy.ac.ucy.cs.anyplace.smas.viewmodel.util.SmasVersion
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
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
        private val miscDS: MiscDataStore,
        private val retrofitHolderChat: RetrofitHolderChat,
        retrofitHolderAP: RetrofitHolderAP):
        CvMapViewModel(application, repoAP, retrofitHolderAP) {

  private val C by lazy { CHAT(app.applicationContext) }

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

  // PREFERENCES
  val prefsChat = chatPrefsDS.read

  override fun prefWindowLocalizationMillis(): Int {
    // TODO:PM modify properly for Smas?
    return C.DEFAULT_PREF_CVLOG_WINDOW_LOCALIZATION_SECONDS.toInt()
  }

  private val utilVersion by lazy { SmasVersion(retrofitHolderChat, app as SmasApp, repoChat, versionResp) }

  /**
   * [prefRow]: the Chat [Preference] row that will be replaced with the result of the version call
   */
  fun displayBackendVersion(prefRow: Preference?) =
          viewModelScope.launch { utilVersion.displaySafeCall(prefRow) }

  //// RETROFIT
  ////// Flows
  private val versionResp: MutableStateFlow<NetworkResult<ChatVersion>> = MutableStateFlow(NetworkResult.Unset())
  private val userLocations: MutableStateFlow<NetworkResult<UserLocations>> = MutableStateFlow(NetworkResult.Unset())

}