package cy.ac.ucy.cs.anyplace.navigator

import cy.ac.ucy.cs.anyplace.lib.android.AnyplaceApp
import cy.ac.ucy.cs.anyplace.lib.android.LOG
import cy.ac.ucy.cs.anyplace.lib.android.extensions.TAG
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class NavigatorApp : AnyplaceApp() {

  override fun onCreate() {
    super.onCreate()
    LOG.D2(TAG, "onCreate")
  }
}