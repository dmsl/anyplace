package cy.ac.ucy.cs.anyplace.navigator

// import android.app.Application
// import cy.ac.ucy.cs.anyplace.lib.Anyplace
// import cy.ac.ucy.cs.anyplace.lib.Preferences
import cy.ac.ucy.cs.anyplace.lib.android.AnyplaceApp
import cy.ac.ucy.cs.anyplace.lib.android.LOG

class NavigatorApp : AnyplaceApp() {
  private val TAG = NavigatorApp::class.java.simpleName

  override val navigator = true
  override val logger = false

  override fun onCreate() {
    super.onCreate()
    LOG.D2(TAG, "onCreate")
  }
}