package cy.ac.ucy.cs.anyplace.logger

import cy.ac.ucy.cs.anyplace.lib.android.AnyplaceApp
import cy.ac.ucy.cs.anyplace.lib.android.LOG
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class LoggerApp : AnyplaceApp() {

  override fun onCreate() {
    super.onCreate()
    LOG.D2()
  }
}