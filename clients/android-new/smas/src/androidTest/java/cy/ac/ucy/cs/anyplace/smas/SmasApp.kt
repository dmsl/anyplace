package cy.ac.ucy.cs.anyplace.smas

import cy.ac.ucy.cs.anyplace.lib.android.NavigationAppSelection
import cy.ac.ucy.cs.anyplace.lib.android.NavigatorAppBase
import cy.ac.ucy.cs.anyplace.lib.android.consts.CONST
import cy.ac.ucy.cs.anyplace.lib.android.utils.LOG
import dagger.hilt.android.HiltAndroidApp

/**
 * NOTE: this might be better if it moves to the smas folder/application module
 */
@HiltAndroidApp
class SmasApp : NavigatorAppBase() {
  private val TG ="app-smass"

  override val navigatorBaseApp = NavigationAppSelection.SMAS

  override fun onCreate() {
    super.onCreate()
    val MT = ::onCreate.name
    LOG.E(TG, MT)

    SET_SMAS_APP()
  }

  private fun SET_SMAS_APP() {
    dsCvMap.setMainActivity(CONST.START_ACT_SMAS)
  }
}

