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
import cy.ac.ucy.cs.anyplace.smas.ui.chat.tmp_models.Messages
import cy.ac.ucy.cs.anyplace.smas.ui.chat.tmp_models.ReplyToMessage
import cy.ac.ucy.cs.anyplace.smas.ui.chat.utils.DateTimeHelper
import cy.ac.ucy.cs.anyplace.smas.ui.chat.utils.ImageBase64
import cy.ac.ucy.cs.anyplace.smas.ui.chat.utils.VoiceRecognition
import cy.ac.ucy.cs.anyplace.smas.utils.network.RetrofitHolderChat
import cy.ac.ucy.cs.anyplace.smas.utils.network.SmasAssetReader
import cy.ac.ucy.cs.anyplace.smas.viewmodel.util.MsgsGetUtil
import cy.ac.ucy.cs.anyplace.smas.viewmodel.util.MsgsSendUtil
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
        private val miscDS: MiscDataStore,
) : AndroidViewModel(app) {

  private val utlMsgsGet by lazy { MsgsGetUtil(app as SmasApp, this, RFH, repoChat) }
  private val utlMsgsSend by lazy { MsgsSendUtil(app as SmasApp, this, RFH, repoChat) }

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
      uid = smas.chatUserDS.readUser.first().uid
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

  fun fetchMessages() {
    LOG.E()
    viewModelScope.launch {
      utlMsgsGet.safeCall()
    }
  }

  /**
   * React to flow that is populated by [utlMsgsGet] safeCall
   */
  fun collectMessages() {
    viewModelScope.launch {
      utlMsgsGet.collect(app)
    }
  }

  fun sendMessage(newMsg: String?, mtype: Int) { //val lastCoordinates = UserCoordinates(spaceH.obj.id,... from SmasMain...
    viewModelScope.launch {
      //TODO:PM:ATH user coordinates & mdelivery
      val userCoord = UserCoordinates("1234", 1, 5.0, 5.0)
      val mdelivery = 1; //TODO:ATH
      var mexten: String? = null
      if (imageUri != null) {
        mexten = imageHelper.getMimeType(imageUri!!, app)
      }
      utlMsgsSend.safeCall(userCoord, 1, mtype, newMsg, mexten)
    }
    clearReply()
    clearTheReplyToMessage()
  }

  /**
   * React to flow that is populated by [utlMsgsSend] safeCall
   */
  fun collectMsgsSend() { //when is this called?
    viewModelScope.launch {
      utlMsgsSend.collect(app)
    }
  }

}