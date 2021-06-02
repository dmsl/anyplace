package cy.ac.ucy.cs.anyplace.logger

import android.app.Application
import cy.ac.ucy.cs.anyplace.lib.Anyplace
import cy.ac.ucy.cs.anyplace.lib.Preferences
import cy.ac.ucy.cs.anyplace.lib.android.AnyplaceApp
import cy.ac.ucy.cs.anyplace.lib.android.LOG

class LoggerApp : AnyplaceApp() {
  private val TAG = LoggerApp::class.java.simpleName

  override val navigator = false
  override val logger = true

  override fun onCreate() {
    super.onCreate()
    LOG.D2(TAG, "onCreate")
  }
}