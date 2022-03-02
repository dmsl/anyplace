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
import android.widget.TextView
import cy.ac.ucy.cs.anyplace.lib.android.LOG
import android.content.Intent
import android.view.View
import cy.ac.ucy.cs.anyplace.lib.android.extensions.TAG
import cy.ac.ucy.cs.anyplace.lib.android.ui.cv.gnk.logger.CvLoggerActivity
import cy.ac.ucy.cs.anyplace.lib.android.utils.AppInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class StartActivity : Activity() {
  private val SPLASH_TIME_OUT = 500L // TODO 100L ?

  lateinit var tvVersion : TextView
  private val appInfo by lazy { AppInfo(applicationContext) }

  public override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.start_screen_layout)
    tvVersion = findViewById<View>(R.id.tvVersion) as TextView
    appInfo.version?.let { tvVersion.text = it }
  }

  override fun onResume() {
    super.onResume()
    CoroutineScope(Main).launch {
      delay(SPLASH_TIME_OUT)
      openInitialActivity()
    }
  }

  private fun openInitialActivity() {
    LOG.D2(TAG, "openInitialActivity")
    CoroutineScope(Main).launch {

      // startActivity(Intent(this@StartActivity, DetectorActivity::class.java))
      startActivity(Intent(this@StartActivity, CvLoggerActivity::class.java))

      // SAMPLE CODE:
      // val user = app.dataStoreUser.readUser.first()
      // if (user.accessToken.isNotBlank()) {
      //   // TODO if space is selected, then open map directly
      //   LOG.D2(TAG, "Opening SelectSpace activity")
      //   startActivity(Intent(this@StartActivity, SelectSpaceActivity::class.java))
      //   // startActivity(Intent(this@StartFragmentActivity, SelectSpaceFragmentActivity::class.java))
      // } else {
      //   LOG.D2(TAG, "Opening Login activity")
      //   // Start login activity
      //   startActivity(Intent(this@StartActivity, LoginActivity::class.java))
      //   // startActivity(Intent(this@StartFragmentActivity, LoginFragmentActivity::class.java))
      // }
      finish()
    }
  }
}