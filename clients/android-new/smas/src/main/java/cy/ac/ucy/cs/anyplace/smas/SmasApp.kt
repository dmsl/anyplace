package cy.ac.ucy.cs.anyplace.smas

import cy.ac.ucy.cs.anyplace.lib.android.NavigationAppSelection
import cy.ac.ucy.cs.anyplace.lib.android.NavigatorAppBase
import cy.ac.ucy.cs.anyplace.lib.android.consts.CONST
import cy.ac.ucy.cs.anyplace.lib.android.utils.LOG
import dagger.hilt.android.HiltAndroidApp

/**
 * SMAS Application.
 * by overriding [navigatorBaseApp] the UI is modified to be specialized for smass
 * - follow [AnyplaceApp.navigatorBaseApp] to figure things out
 */
@HiltAndroidApp
class SmasApp : NavigatorAppBase() {
  private val TG ="app-smass"

  override val navigatorBaseApp = NavigationAppSelection.SMAS

  override fun onCreate() {
    super.onCreate()
    val MT = ::onCreate.name
    LOG.E(TG, MT)
  }
}

