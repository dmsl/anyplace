package cy.ac.ucy.cs.anyplace.smas

import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.asLiveData
import cy.ac.ucy.cs.anyplace.lib.android.AnyplaceApp
import cy.ac.ucy.cs.anyplace.lib.android.extensions.METHOD
import cy.ac.ucy.cs.anyplace.lib.android.utils.LOG
import cy.ac.ucy.cs.anyplace.lib.android.extensions.TAG
import cy.ac.ucy.cs.anyplace.smas.data.models.ChatMsg
import cy.ac.ucy.cs.anyplace.smas.data.store.ChatPrefsDataStore
import cy.ac.ucy.cs.anyplace.smas.data.store.ChatUserDataStore
import cy.ac.ucy.cs.anyplace.smas.data.source.RetrofitHolderChat
import cy.ac.ucy.cs.anyplace.smas.viewmodel.SmasChatViewModel
import cy.ac.ucy.cs.anyplace.smas.viewmodel.SmasMainViewModel
import dagger.hilt.android.HiltAndroidApp
import dagger.hilt.android.components.ViewModelComponent
import javax.inject.Inject

@HiltAndroidApp
class SmasApp : AnyplaceApp() {

  /** SMAS Chat Server preferences */
  @Inject lateinit var dsChat: ChatPrefsDataStore
  /** Logged-in SMAS user */
  @Inject lateinit var dsChatUser: ChatUserDataStore
  @Inject lateinit var rfhChat: RetrofitHolderChat

  /** list of messages shown on screen by [LazyColumn] */
  var msgList = mutableStateListOf<ChatMsg>()

  /** The VM set by [SmasMainActivity] */
  private var VMchat : SmasChatViewModel? = null

  override fun onCreate() {
    super.onCreate()
    LOG.D2()

    observeChatPrefs()
  }

  /**
   * Set from the [SmasMainActivity],
   * to allow triggering the [SmasMainViewModel] from the [SmasChatActivity]
   */
  fun setChatVM(VMchat: SmasChatViewModel) {
    this.VMchat=VMchat
  }

  fun pullMessagesONCE() {
    LOG.V2(TAG, "$METHOD: using VMchat of Main Activity?")
    VMchat?.netPullMessagesONCE(false)
  }

  /** Manually create a new instance of the RetrofitHolder on pref changes */
  private fun observeChatPrefs() {
    val prefsChat = dsChat.read
    prefsChat.asLiveData().observeForever { prefs ->
      rfhChat.set(prefs)
      LOG.V3(TAG, "Updated Chat backend URL: ${rfhChat.baseURL}")
    }
  }
}