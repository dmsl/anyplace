package cy.ac.ucy.cs.anyplace.smas.ui.settings

import android.graphics.Color
import android.os.Bundle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.asLiveData
import androidx.lifecycle.lifecycleScope
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import cy.ac.ucy.cs.anyplace.lib.android.utils.LOG
import cy.ac.ucy.cs.anyplace.lib.android.extensions.TAG
import cy.ac.ucy.cs.anyplace.lib.android.extensions.setBackButton
import cy.ac.ucy.cs.anyplace.lib.android.extensions.setTextColor
import cy.ac.ucy.cs.anyplace.lib.android.ui.dialogs.ConfirmActionDialog
import cy.ac.ucy.cs.anyplace.lib.android.ui.settings.BaseSettingsActivity
import cy.ac.ucy.cs.anyplace.smas.R
import cy.ac.ucy.cs.anyplace.smas.data.RepoChat
import cy.ac.ucy.cs.anyplace.smas.data.store.ChatPrefsDataStore
import cy.ac.ucy.cs.anyplace.smas.extensions.appSmas
import cy.ac.ucy.cs.anyplace.smas.data.source.RetrofitHolderChat
import cy.ac.ucy.cs.anyplace.smas.viewmodel.SmasChatViewModel
import cy.ac.ucy.cs.anyplace.smas.viewmodel.SmasMainViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class SettingsChatActivity: BaseSettingsActivity() {
  private lateinit var settingsFragment: SettingsChatFragment
  private lateinit var VM: SmasMainViewModel
  private lateinit var smasChatVM: SmasChatViewModel
  @Inject lateinit var repo: RepoChat
  @Inject lateinit var retrofitHolder: RetrofitHolderChat

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    VM = ViewModelProvider(this)[SmasMainViewModel::class.java]
    smasChatVM = ViewModelProvider(this)[SmasChatViewModel::class.java]

    settingsFragment = SettingsChatFragment(VM, retrofitHolder, this.appSmas.dsChat)
    setupFragment(settingsFragment, savedInstanceState)

    supportActionBar?.setDisplayHomeAsUpEnabled(true)
    supportActionBar?.setTextColor(Color.WHITE)
    supportActionBar?.setBackButton(applicationContext, Color.WHITE)
  }

  class SettingsChatFragment(
    private val VM: SmasMainViewModel,
    private val retrofitH: RetrofitHolderChat,
    private val chatPrefsDS: ChatPrefsDataStore) : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
      preferenceManager.preferenceDataStore = chatPrefsDS
      setPreferencesFromResource(R.xml.preferences_chat, rootKey)

      setupVersionButton()
      setupClearMessagesButton()
    }

    private fun setupClearMessagesButton() {
      val title= "Clearing all messages?"
      val subtitle = "Those will be fetched again, given internet connectivity."
      val mgr = requireActivity().supportFragmentManager

      val prefBtn : Preference? = findPreference(getString(R.string.pref_chat_delete_local_msgs))

      prefBtn?.setOnPreferenceClickListener {
        ConfirmActionDialog.SHOW(mgr, title, subtitle)
        true
      }
    }

    private fun setupVersionButton() {
      val prefBtn: Preference? = findPreference(getString(R.string.pref_chat_server_version))
      prefBtn?.setOnPreferenceClickListener {
        prefBtn.icon = null
        prefBtn.summary = "refreshing.."
        lifecycleScope.launch {  // artificial delay
          delay(250)
          VM.displayVersion(prefBtn)
        }
        true // click is handled
      }
      observeChatPrefs(prefBtn)
    }

    /**
     * When Chat Preferences change:
     * - update Retrofit Holder (wrapper to work well w/ DI)
     * - re-initiate contact with the Chat Server
     */
    private fun observeChatPrefs(versionPreferences: Preference?) {
      VM.prefsChat.asLiveData().observe(this) { prefs ->
        retrofitH.set(prefs)
        LOG.D3(TAG, "Chat Base URL: ${retrofitH.retrofit.baseUrl()}")
        VM.displayVersion(versionPreferences)
      }
    }
  }
}