/*
* Anyplace: A free and open Indoor Navigation Service with superb accuracy!
*
* Anyplace is a first-of-a-kind indoor information service offering GPS-less
* localization, navigation and search inside buildings using ordinary smartphones.
*
* Author(s): Lambros Petrou
* 
* Supervisor: Demetrios Zeinalipour-Yazti
*
* URL: http://anyplace.cs.ucy.ac.cy
* Contact: anyplace@cs.ucy.ac.cy
*
* Copyright (c) 2015, Data Management Systems Lab (DMSL), University of Cyprus.
* All rights reserved.
*
* Permission is hereby granted, free of charge, to any person obtaining a copy of
* this software and associated documentation files (the "Software"), to deal in the
* Software without restriction, including without limitation the rights to use, copy,
* modify, merge, publish, distribute, sublicense, and/or sell copies of the Software,
* and to permit persons to whom the Software is furnished to do so, subject to the
* following conditions:
*
* The above copyright notice and this permission notice shall be included in all
* copies or substantial portions of the Software.
*
* THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
* OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
* FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
* AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
* LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
* FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
* DEALINGS IN THE SOFTWARE.
*
*/
package cy.ac.ucy.cs.anyplace.logger

import android.app.Activity
import android.os.Bundle
import android.content.pm.PackageInfo
import android.widget.TextView
import android.content.pm.PackageManager
import cy.ac.ucy.cs.anyplace.lib.android.LOG
import android.content.Intent
import android.view.View
import cy.ac.ucy.cs.anyplace.lib.android.extensions.app
import cy.ac.ucy.cs.anyplace.lib.android.ui.cv.logger.CvLoggerActivity
import cy.ac.ucy.cs.anyplace.lib.android.ui.login.LoginActivity
import cy.ac.ucy.cs.anyplace.lib.android.ui.selector.space.SelectSpaceActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class StartActivity : Activity() {
  private val TAG = StartActivity::class.java.simpleName
  private val SPLASH_TIME_OUT = 0L // TODO 100L ?

  public override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.start_screen_layout)
    val pinfo: PackageInfo
    try {
      pinfo = packageManager.getPackageInfo(packageName, 0)
      val versionName = pinfo.versionName
      (findViewById<View>(R.id.tvVersion) as TextView).text = versionName
    } catch (e: PackageManager.NameNotFoundException) {
      LOG.E("Cannot get version name.")
    }
  }

  override fun onResume() {
    super.onResume()
    CoroutineScope(Main).launch {
      delay(SPLASH_TIME_OUT)

      // startActivity(Intent(this@StartActivity, MapsActivity::class.java))
      openInitialActivity()
    }
  }

  private fun openInitialActivity() {
    LOG.D2(TAG, "openInitialActivity")
    CoroutineScope(Main).launch {
      startActivity(Intent(this@StartActivity, CvLoggerActivity::class.java))

      // SAMPLE CODE:
      // val user = app.dataStoreUser.readUser.first()
      // if (user.accessToken.isNotBlank()) {
      //   // TODO if space is selected, then open map directly
      //   LOG.D2(TAG, "Opening SelectSpace activity")
      //   startActivity(Intent(this@StartFragmentActivity, SelectSpaceFragmentActivity::class.java))
      // } else {
      //   LOG.D2(TAG, "Opening Login activity")
      //   // Start login activity
      //   startActivity(Intent(this@StartFragmentActivity, LoginFragmentActivity::class.java))
      // }
      finish()
    }
  }
}