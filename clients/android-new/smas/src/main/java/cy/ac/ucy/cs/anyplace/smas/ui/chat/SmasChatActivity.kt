package cy.ac.ucy.cs.anyplace.smas.ui.chat

import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material.*
import androidx.lifecycle.ViewModelProvider
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import cy.ac.ucy.cs.anyplace.smas.consts.CHAT
import cy.ac.ucy.cs.anyplace.smas.data.RepoChat
import cy.ac.ucy.cs.anyplace.smas.ui.chat.components.Conversation
import cy.ac.ucy.cs.anyplace.smas.ui.chat.components.TopMessagesBar
import cy.ac.ucy.cs.anyplace.smas.ui.chat.theme.*
import cy.ac.ucy.cs.anyplace.smas.viewmodel.SmasChatViewModel
import cy.ac.ucy.cs.anyplace.smas.viewmodel.SmasMainViewModel
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@RequiresApi(Build.VERSION_CODES.O)
@ExperimentalMaterialApi
@ExperimentalPermissionsApi
@AndroidEntryPoint
class SmasChatActivity : AppCompatActivity() {

  private lateinit var VMchat: SmasChatViewModel
  private lateinit var VM: SmasMainViewModel
  @Inject lateinit var repo: RepoChat

  private fun pullData() {
    VMchat.netPullMessagesONCE() // TODO:PMX LOOP
    VMchat.collectMessages()
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    VMchat = ViewModelProvider(this)[SmasChatViewModel::class.java]
    VM = ViewModelProvider(this)[SmasMainViewModel::class.java]

    pullData()

    setContent {
      Scaffold(
              topBar = { TopMessagesBar(::onBackClick) },
              content = { Conversation(VM, VMchat, supportFragmentManager, repo, ::returnLoc) },
              backgroundColor = WhiteGray
      )
    }
  }

  //Called when the back btn on the top bar is clicked
  private fun onBackClick() {
    intent.data = null
    finish()
  }

  //Called when the location button in a message is clicked
  private fun returnLoc(latitude: Double, longitude: Double) {
    // TODO:PMX put deck here also..
    setResult(Activity.RESULT_OK, Intent().putExtra("latitude", latitude).putExtra("longitude", longitude))
    finish()
  }
}




