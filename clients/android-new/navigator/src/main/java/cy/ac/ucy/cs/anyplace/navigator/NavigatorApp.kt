package cy.ac.ucy.cs.anyplace.navigator

import cy.ac.ucy.cs.anyplace.lib.android.AnyplaceApp
import cy.ac.ucy.cs.anyplace.lib.android.NavigationAppSelection
import cy.ac.ucy.cs.anyplace.lib.android.NavigatorAppBase
import cy.ac.ucy.cs.anyplace.lib.android.consts.CONST
import cy.ac.ucy.cs.anyplace.lib.android.utils.LOG
import dagger.hilt.android.HiltAndroidApp

/**
 * An application that specializes for Anyplace Navigation.
 * Note: [AnyplaceApp] already defaults for: NavigationAppSelection.Navigator
 * see: [AnyplaceApp.navigatorBaseApp]
 */
@HiltAndroidApp
class NavigatorApp : NavigatorAppBase() {
  private val TG ="app-nav"
  override val navigatorBaseApp = NavigationAppSelection.Navigator

  override fun onCreate() {
    super.onCreate()
    val MT = ::onCreate.name
    LOG.E(TG, MT)
  }
}