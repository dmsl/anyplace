package cy.ac.ucy.cs.anyplace.logger.legacy

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceFragmentCompat
import cy.ac.ucy.cs.anyplace.lib.android.LOG
import cy.ac.ucy.cs.anyplace.logger.R

class OLDLoggerSettingsActivity : AppCompatActivity() {
  companion object {
    private val TAG = SettingsFragment::class.java.simpleName
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    LOG.D("$TAG: onCreate")
    super.onCreate(savedInstanceState)
    setContentView(R.layout.settings_activity)
    supportFragmentManager
            .beginTransaction()
            .replace(R.id.settings, SettingsFragment())
            .commit()
    val actionBar = supportActionBar
    actionBar?.setDisplayHomeAsUpEnabled(true)
  }

  class SettingsFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
      LOG.E(TAG, "root key: $rootKey")
      //setPreferencesFromResource(R.xml.root_preferences, rootKey); // ANYPLACE
      setPreferencesFromResource(R.xml.preferences_anyplace, rootKey) // LOGGER
    }
  }
}