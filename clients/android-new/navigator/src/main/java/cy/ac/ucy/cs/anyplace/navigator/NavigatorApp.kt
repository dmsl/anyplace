package cy.ac.ucy.cs.anyplace.navigator

import cy.ac.ucy.cs.anyplace.lib.android.AnyplaceApp
import cy.ac.ucy.cs.anyplace.lib.android.LOG
import cy.ac.ucy.cs.anyplace.lib.android.extensions.TAG
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class NavigatorApp : AnyplaceApp() {

  override val navigator = false
  override val logger = true

  override fun onCreate() {
    super.onCreate()
    LOG.D2(TAG, "onCreate")
  }
}