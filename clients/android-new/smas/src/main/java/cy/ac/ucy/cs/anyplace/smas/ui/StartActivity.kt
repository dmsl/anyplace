package cy.ac.ucy.cs.anyplace.smas.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.compose.material.ExperimentalMaterialApi
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import cy.ac.ucy.cs.anyplace.lib.android.appSmas
import cy.ac.ucy.cs.anyplace.lib.android.extensions.METHOD
import cy.ac.ucy.cs.anyplace.lib.android.utils.LOG
import cy.ac.ucy.cs.anyplace.lib.android.extensions.TAG
import cy.ac.ucy.cs.anyplace.lib.android.extensions.TAG_METHOD
import cy.ac.ucy.cs.anyplace.smas.BuildConfig
import cy.ac.ucy.cs.anyplace.smas.R
import cy.ac.ucy.cs.anyplace.lib.android.ui.cv.logger.CvLoggerActivity
import cy.ac.ucy.cs.anyplace.lib.android.ui.smas.SmasLoginActivity
import cy.ac.ucy.cs.anyplace.lib.android.ui.smas.SmasLoginActivity.Companion.OPEN_ACT
import cy.ac.ucy.cs.anyplace.lib.android.ui.smas.SmasLoginActivity.Companion.OPEN_ACT_LOGGER
import cy.ac.ucy.cs.anyplace.lib.android.ui.smas.SmasLoginActivity.Companion.OPEN_ACT_SMAS
import cy.ac.ucy.cs.anyplace.lib.android.ui.smas.SmasMainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class StartActivity : Activity() {
  private val SPLASH_TIME_OUT = 500L
  lateinit var tvVersion : TextView

  public override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_start)
    tvVersion = findViewById<View>(R.id.tvVersion) as TextView
    setupVersion()
  }

  private fun setupVersion() {
    val versionStr = "ver: ${BuildConfig.VERSION_NAME}"
    tvVersion.text = versionStr
  }

  override fun onResume() {
    super.onResume()
    CoroutineScope(Main).launch {
      delay(SPLASH_TIME_OUT)
      openInitialActivity()
    }
  }

  @OptIn(ExperimentalMaterialApi::class, ExperimentalPermissionsApi::class)  // compose
  private fun openInitialActivity() {
    LOG.D2(TAG_METHOD)
    CoroutineScope(Main).launch {
      /*
      startActivity(Intent(this@StartActivity, DetectorActivity::class.java))
      startActivity(Intent(this@StartActivity, CvMapActivity::class.java))
      startActivity(Intent(this@StartActivity, SmasChatActivity::class.java))
       */

      // authenticated users go straight to the Main Smas activity
      val chatUser = appSmas.dsChatUser.readUser.first()
      if (chatUser.sessionkey.isNotBlank()) {
        LOG.D2(TAG, "$METHOD: user: session: $chatUser")
        // startSmas()
        startLogger()
      } else {
        LOG.D2(TAG, "Opening activity: Login")

        val intent = Intent(this@StartActivity, SmasLoginActivity::class.java)
        intent.putExtra(OPEN_ACT, OPEN_ACT_SMAS)
        startActivity(intent)
      }
      finish()
    }
  }

  private fun startLogger() {
    LOG.W(TAG, "OPENING LOGGER ACTIVITY")
    startActivity(Intent(this@StartActivity, CvLoggerActivity::class.java))
  }

  private fun startSmas() {
    LOG.W(TAG, "OPENING SMAS ACTIVITY")
    startActivity(Intent(this@StartActivity, SmasMainActivity::class.java))
  }
}