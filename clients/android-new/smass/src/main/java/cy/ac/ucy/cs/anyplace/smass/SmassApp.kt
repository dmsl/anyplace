package cy.ac.ucy.cs.anyplace.smass

import cy.ac.ucy.cs.anyplace.lib.android.AnyplaceApp
import cy.ac.ucy.cs.anyplace.lib.android.LOG
import cy.ac.ucy.cs.anyplace.lib.android.extensions.TAG
import cy.ac.ucy.cs.anyplace.lib.android.extensions.TAG_METHOD
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class SmassApp : AnyplaceApp() {
  override val navigator = false
  override val logger = true

  override fun onCreate() {
    super.onCreate()
    LOG.D2(TAG_METHOD)
  }
}