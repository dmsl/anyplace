package cy.ac.ucy.cs.anyplace.logger

import cy.ac.ucy.cs.anyplace.lib.android.AnyplaceApp
import cy.ac.ucy.cs.anyplace.lib.android.LOG
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class LoggerApp : AnyplaceApp() {
  private val TAG = LoggerApp::class.java.simpleName

  override val navigator = false
  override val logger = true

  override fun onCreate() {
    super.onCreate()
    LOG.D2(TAG, "onCreate")
  }
}