package cy.ac.ucy.cs.anyplace.logger

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import cy.ac.ucy.cs.anyplace.lib.android.LOG.Companion.D

class SettingsActivity : AppCompatActivity() {
  private val TAG = SettingsActivity::class.java.simpleName
  override fun onCreate(savedInstanceState: Bundle?) {
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
    fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String) {
      D(TAG, "root key: $rootKey")
      //setPreferencesFromResource(R.xml.root_preferences, rootKey);
      setPreferencesFromResource(R.xml.preferences_anyplace, rootKey)
    }

    companion object {
      private val TAG = SettingsFragment::class.java.simpleName
    }
  }
}