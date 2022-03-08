package cy.ac.ucy.cs.anyplace.smas.ui.settings

import android.graphics.Color
import android.os.Bundle
import android.widget.Toast
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.asLiveData
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewModelScope
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import cy.ac.ucy.cs.anyplace.lib.android.LOG
import cy.ac.ucy.cs.anyplace.lib.android.extensions.TAG
import cy.ac.ucy.cs.anyplace.lib.android.extensions.setBackButton
import cy.ac.ucy.cs.anyplace.lib.android.extensions.setTextColor
import cy.ac.ucy.cs.anyplace.lib.android.ui.settings.BaseSettingsActivity
import cy.ac.ucy.cs.anyplace.smas.R
import cy.ac.ucy.cs.anyplace.smas.data.RepoChat
import cy.ac.ucy.cs.anyplace.smas.data.store.ChatPrefsDataStore
import cy.ac.ucy.cs.anyplace.smas.extensions.appSmas
import cy.ac.ucy.cs.anyplace.smas.utils.network.RetrofitHolderChat
import cy.ac.ucy.cs.anyplace.smas.viewmodel.ChatViewModel
import cy.ac.ucy.cs.anyplace.smas.viewmodel.SmasMainViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class SettingsChatActivity: BaseSettingsActivity() {
  private lateinit var settingsFragment: SettingsChatFragment
  private lateinit var VM: SmasMainViewModel
  private lateinit var chatVM: ChatViewModel
  @Inject  lateinit var repo: RepoChat
  @Inject  lateinit var retrofitHolder: RetrofitHolderChat

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    VM = ViewModelProvider(this)[SmasMainViewModel::class.java]
    chatVM = ViewModelProvider(this)[ChatViewModel::class.java]

    settingsFragment = SettingsChatFragment(chatVM, retrofitHolder, this.appSmas.chatPrefsDS)
    setupFragment(settingsFragment, savedInstanceState)

    supportActionBar?.setDisplayHomeAsUpEnabled(true)
    supportActionBar?.setTextColor(Color.WHITE)
    supportActionBar?.setBackButton(applicationContext, Color.WHITE)
  }

  class SettingsChatFragment(
          private val VM: SmasMainViewModel,
          private val chatVM: ChatViewModel,
          private val retrofitH: RetrofitHolderChat,
          private val chatPrefsDS: ChatPrefsDataStore) : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
      preferenceManager.preferenceDataStore = chatPrefsDS
      setPreferencesFromResource(R.xml.preferences_chat, rootKey)

      val versionPrefRow : Preference? = findPreference(getString(R.string.pref_chat_server_version))
      versionPrefRow?.setOnPreferenceClickListener {
        versionPrefRow.icon = null
        versionPrefRow.summary = "refreshing.."
        lifecycleScope.launch {  // artificial delay
          delay(250)
          VM.displayBackendVersion(versionPrefRow)
        }
        true // click is handled
      }
      observeChatPrefs(versionPrefRow)
    }

    private fun observeChatPrefs(versionPreferences: Preference?) {
      VM.prefsChat.asLiveData().observe(this) { prefs ->
        retrofitH.set(prefs)
        LOG.D3(TAG, "Chat Base URL: ${retrofitH.retrofit.baseUrl()}")
        VM.displayBackendVersion(versionPreferences)
      }
    }
  }
}