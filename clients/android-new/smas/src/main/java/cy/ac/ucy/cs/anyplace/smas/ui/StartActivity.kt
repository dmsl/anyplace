/**
* Anyplace: A free and open Indoor Navigation Service with superb accuracy!
*
* Anyplace is a first-of-a-kind indoor information service offering GPS-less
* localization, navigation and search inside buildings using ordinary smartphones.
*
* Author(s): Paschalis Mpeis
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

package cy.ac.ucy.cs.anyplace.smas.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.compose.material.ExperimentalMaterialApi
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import cy.ac.ucy.cs.anyplace.lib.android.utils.LOG
import cy.ac.ucy.cs.anyplace.lib.android.extensions.TAG
import cy.ac.ucy.cs.anyplace.lib.android.extensions.TAG_METHOD
import cy.ac.ucy.cs.anyplace.smas.BuildConfig
import cy.ac.ucy.cs.anyplace.smas.R
import cy.ac.ucy.cs.anyplace.smas.extensions.appSmas
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class StartActivity : Activity() {
  private val SPLASH_TIME_OUT = 500L

  lateinit var tvVersion : TextView
  // private val appInfo by lazy { AppInfo(applicationContext) }

  public override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_start)
    tvVersion = findViewById<View>(R.id.tvVersion) as TextView

    setupVersion()
  }

  private fun setupVersion() {
    CoroutineScope(Main).launch {
      var versionStr = "ver: ${BuildConfig.VERSION_NAME}"
      tvVersion.text = versionStr
      // TODO:PMX
      // val prefsChat = appSmas.dsChat.read.first()
      // if (prefsChat.version != null) versionStr += " (${prefsChat.version})"
    }
  }

  override fun onResume() {
    super.onResume()
    CoroutineScope(Main).launch {
      delay(SPLASH_TIME_OUT)
      openInitialActivity()
    }
  }

  @OptIn(ExperimentalMaterialApi::class, ExperimentalPermissionsApi::class)  // compose
  private fun openInitialActivity() {
    LOG.D2(TAG_METHOD)
    CoroutineScope(Main).launch {
      // startActivity(Intent(this@StartActivity, DetectorActivity::class.java))
      // startActivity(Intent(this@StartActivity, CvMapActivity::class.java))
      // startActivity(Intent(this@StartActivity, SmasMainActivity::class.java))

      // authenticated users go straight to the Main Smas activity
      val chatUser = appSmas.dsChatUser.readUser.first()
      if (chatUser.sessionkey.isNotBlank()) {
        LOG.D2(TAG, "Opening activity: SmasMain")
        LOG.D2(TAG_METHOD, "USER: SESSION: $chatUser")
        startActivity(Intent(this@StartActivity, SmasMainActivity::class.java))
        // CHECK:ATH: example on how to start chat activity from here..
        // startActivity(Intent(this@StartActivity, SmasChatActivity::class.java))
      } else {
        LOG.D2(TAG, "Opening activity: Login")
        startActivity(Intent(this@StartActivity, SmasLoginActivity::class.java))
      }

      finish()
    }
  }
}