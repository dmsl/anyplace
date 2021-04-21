package cy.ac.ucy.cs.anyplace.logger

import android.app.Application
import cy.ac.ucy.cs.anyplace.lib.Anyplace

class AnyplaceApp : Application() {
  lateinit var client: Anyplace

  override fun onCreate() {
    super.onCreate()
    // TODO where is the client used?
    // TODO: initialize with shared preferences
    client = Anyplace("ap-dev.cs.ucy.ac.cy", "443",
            applicationContext.cacheDir.absolutePath)
  }


  // fileCache.initDirs(); // TODO this in App class

}