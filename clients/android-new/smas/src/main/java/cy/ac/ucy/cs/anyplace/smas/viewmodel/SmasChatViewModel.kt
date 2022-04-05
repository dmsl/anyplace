package cy.ac.ucy.cs.anyplace.smas.viewmodel

import android.app.Application
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.*
import cy.ac.ucy.cs.anyplace.lib.android.LOG
import cy.ac.ucy.cs.anyplace.lib.android.data.store.*
import cy.ac.ucy.cs.anyplace.lib.models.UserCoordinates
import cy.ac.ucy.cs.anyplace.smas.SmasApp
import cy.ac.ucy.cs.anyplace.smas.data.RepoChat
import cy.ac.ucy.cs.anyplace.smas.data.models.ChatMsg
import cy.ac.ucy.cs.anyplace.smas.data.models.UserLocations
import cy.ac.ucy.cs.anyplace.smas.data.store.ChatPrefsDataStore
import cy.ac.ucy.cs.anyplace.smas.ui.chat.tmp_models.Messages
import cy.ac.ucy.cs.anyplace.smas.ui.chat.tmp_models.ReplyToMessage
import cy.ac.ucy.cs.anyplace.smas.ui.chat.utils.DateTimeHelper
import cy.ac.ucy.cs.anyplace.smas.ui.chat.utils.ImageBase64
import cy.ac.ucy.cs.anyplace.smas.ui.chat.utils.VoiceRecognition
import cy.ac.ucy.cs.anyplace.smas.utils.network.RetrofitHolderChat
import cy.ac.ucy.cs.anyplace.smas.utils.network.SmasAssetReader
import cy.ac.ucy.cs.anyplace.smas.viewmodel.util.nw.MsgsGetNW
import cy.ac.ucy.cs.anyplace.smas.viewmodel.util.nw.MsgsSendNW
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Handles everything related to the Chat.
 *
 * [UserLocations] are handled by [SmasMainViewModel]
 *
 * TODO:AH will put stuff here.
 */
@HiltViewModel
class SmasChatViewModel @Inject constructor(
        private val app: Application,
        private val repoChat: RepoChat,
        private val RFH: RetrofitHolderChat,
        private val dsChat: ChatPrefsDataStore,
        private val dsMisc: MiscDataStore,
) : AndroidViewModel(app) {

  private val nwMsgsGet by lazy { MsgsGetNW(app as SmasApp, this, RFH, repoChat) }
  private val nwMsgsSend by lazy { MsgsSendNW(app as SmasApp, this, RFH, repoChat) }

  //Json data
  private val assetReader by lazy { SmasAssetReader(app) }
  var messages: Messages? = null

  //Class objects
  val voiceRecognizer = VoiceRecognition()
  val imageHelper = ImageBase64()
  val dateTimeHelper = DateTimeHelper()

  //Variables observed by composable functions
  var reply: String by mutableStateOf("")
  var wantsToRecord: Boolean by mutableStateOf(false)
  var imageUri: Uri? by mutableStateOf(null)
  var showDialog: Boolean by mutableStateOf(false)
  var replyToMessage: ReplyToMessage? by mutableStateOf(null)

  //The list of messages shown on screen
  val listOfMessages = mutableStateListOf<ChatMsg>()

  fun readData() {
    messages = assetReader.getMessages()
  }

  fun getLoggedInUser(): String {
    val smas = app as SmasApp
    var uid: String = ""
    viewModelScope.launch {
      uid = smas.dsChatUser.readUser.first().uid
    }
    return uid
  }

  fun clearReply() {
    reply = ""
  }

  fun clearImgUri() {
    imageUri = null
  }

  fun clearTheReplyToMessage() {
    replyToMessage = null
  }

  fun nwPullMessages() {
    LOG.E()
    viewModelScope.launch {
      nwMsgsGet.safeCall()
    }
  }

  /**
   * React to flow that is populated by [nwMsgsGet] safeCall
   */
  fun collectMessages() {
    viewModelScope.launch {
      nwMsgsGet.collect(app)
    }
  }

  fun sendMessage(newMsg: String?, mtype: Int) { // TODO:ATH: val lastCoordinates = UserCoordinates(spaceH.obj.id,... from SmasMain...
    viewModelScope.launch {
      //TODO:PM:ATH user coordinates & mdelivery
      val userCoord = UserCoordinates("1234", 1, 5.0, 5.0)

      val chatPrefs = dsChat.read.first()
      // TODO:ATH: now you are using this.
      // Update your UI, w/ TextView to update this accordingly.
      // I'll also update it from settings... on chat settings..
      // TODO:ATH: bind settings button to open [SettingsChatActivity]
      val mdelivery = chatPrefs.mdelivery
      var mexten: String? = null
      if (imageUri != null) {
        mexten = imageHelper.getMimeType(imageUri!!, app)
      }
      nwMsgsSend.safeCall(userCoord, 1, mtype, newMsg, mexten)
    }
    clearReply()
    clearTheReplyToMessage()
  }

  /**
   * React to flow that is populated by [nwMsgsSend] safeCall
   */
  fun collectMsgsSend() { //when is this called?
    viewModelScope.launch {
      nwMsgsSend.collect(app)
    }
  }

}