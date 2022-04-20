package cy.ac.ucy.cs.anyplace.smas.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.*
import cy.ac.ucy.cs.anyplace.lib.android.utils.LOG
import cy.ac.ucy.cs.anyplace.lib.android.data.store.*
import cy.ac.ucy.cs.anyplace.lib.android.extensions.TAG_METHOD
import cy.ac.ucy.cs.anyplace.lib.models.UserCoordinates
import cy.ac.ucy.cs.anyplace.lib.network.NetworkResult
import cy.ac.ucy.cs.anyplace.smas.SmasApp
import cy.ac.ucy.cs.anyplace.smas.data.RepoChat
import cy.ac.ucy.cs.anyplace.smas.data.models.ChatMsg
import cy.ac.ucy.cs.anyplace.smas.data.models.MsgSendResp
import cy.ac.ucy.cs.anyplace.smas.data.models.UserLocations
import cy.ac.ucy.cs.anyplace.smas.data.store.ChatPrefsDataStore
import cy.ac.ucy.cs.anyplace.smas.ui.chat.theme.AnyplaceBlue
import cy.ac.ucy.cs.anyplace.smas.ui.chat.tmp_models.ReplyToMessage
import cy.ac.ucy.cs.anyplace.smas.ui.chat.utils.ChatCache
import cy.ac.ucy.cs.anyplace.smas.ui.chat.utils.DateTimeHelper
import cy.ac.ucy.cs.anyplace.smas.ui.chat.utils.ImageBase64
import cy.ac.ucy.cs.anyplace.smas.ui.dialogs.ImgDialog
import cy.ac.ucy.cs.anyplace.smas.ui.dialogs.MsgDeliveryDialog
import cy.ac.ucy.cs.anyplace.smas.utils.network.RetrofitHolderChat
import cy.ac.ucy.cs.anyplace.smas.viewmodel.util.nw.MsgsGetNW
import cy.ac.ucy.cs.anyplace.smas.viewmodel.util.nw.MsgsSendNW
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Handles everything related to the Chat.
 *
 * [UserLocations] are handled by [SmasMainViewModel]
 *
 */
@HiltViewModel
class SmasChatViewModel @Inject constructor(
        private val _application: Application,
        private val repoChat: RepoChat,
        private val RFH: RetrofitHolderChat,
        private val dsChat: ChatPrefsDataStore,
        private val dsMisc: MiscDataStore,
) : AndroidViewModel(_application) {

  private val app = _application as SmasApp

  private val nwMsgsGet by lazy { MsgsGetNW(app, this, RFH, repoChat) }
  private val nwMsgsSend by lazy { MsgsSendNW(app, this, RFH, repoChat) }

  val chatCache by lazy { ChatCache(app.applicationContext) }

  // Class objects
  val imageHelper = ImageBase64()
  val dateUtl = DateTimeHelper()

  //Variables observed by composable functions
  var reply: String by mutableStateOf("")
  var imageUri: Uri? by mutableStateOf(null)
  var showDialog: Boolean by mutableStateOf(false)
  var replyToMessage: ReplyToMessage? by mutableStateOf(null)
  var mdelivery: String by mutableStateOf("")
  var errColor: Color by mutableStateOf(AnyplaceBlue)
  var isLoading: Boolean by mutableStateOf(false)

  /** list of messages shown on screen by [LazyColumn] */
  var msgList = mutableStateListOf<ChatMsg>()
  val msgFlow: MutableStateFlow<List<ChatMsg>> = MutableStateFlow(emptyList())

  fun getLoggedInUser(): String {
    var uid = ""
    viewModelScope.launch {
      uid = app.dsChatUser.readUser.first().uid
    }
    return uid
  }

  fun setDeliveryMethod(){
    viewModelScope.launch {
      val chatPrefs = dsChat.read.first()
      mdelivery = chatPrefs.mdelivery
    }
  }

  fun openMsgDeliveryDialog(fragmentManager: FragmentManager){
    MsgDeliveryDialog.SHOW(fragmentManager, dsChat, app,this)
  }

  fun openImgDialog(fragmentManager: FragmentManager, img: Bitmap){
    ImgDialog.SHOW(fragmentManager, img)
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

  fun nwPullMessages(showToast: Boolean = false) {
    LOG.V()
    viewModelScope.launch(Dispatchers.IO) {
      nwMsgsGet.safeCall(showToast)
    }
  }

  /**
   * React to flow that is populated by [nwMsgsGet] safeCall
   */
  fun collectMessages() {
    viewModelScope.launch(Dispatchers.IO) {
      nwMsgsGet.collect(app)
    }
  }

  private fun getUserCoordinates(VM: SmasMainViewModel): UserCoordinates? {
    var userCoord : UserCoordinates? = null
    if (VM.location.value.coord != null) {
      userCoord = UserCoordinates(VM.spaceH.obj.id,
              VM.floorH?.obj!!.floorNumber.toInt(),
              VM.location.value.coord!!.lat,
              VM.location.value.coord!!.lon)
      return userCoord
    }

    return null
  }

  private fun getCenterOfFloor(VM: SmasMainViewModel): UserCoordinates {
    val latLng = VM.spaceH.latLng()
    return UserCoordinates(VM.spaceH.obj.id,
            VM.floorH?.obj!!.floorNumber.toInt(),
            latLng.latitude,
            latLng.longitude)
  }
  //

  fun sendMessage(VM: SmasMainViewModel, newMsg: String?, mtype: Int) {
    viewModelScope.launch {

      // var userCoordinates = getUserCoordinates(VM)
      // if (userCoordinates==null) {
      //   val msg = "Cannot attach location to msg"
      //   LOG.E(TAG_METHOD, msg)
      //   app.showToast(msg)
      //   userCoordinates = getCenterOfFloor(VM)
      // }

      val chatPrefs = dsChat.read.first()
      val mdelivery = chatPrefs.mdelivery
      var mexten: String? = null
      if (imageUri != null) {
        mexten = imageHelper.getMimeType(imageUri!!, app)
      }

      // TODO:PMX real coordinates
      val dummy = UserCoordinates("1234",1,5.0,5.0)
      nwMsgsSend.safeCall(dummy, mdelivery, mtype, newMsg, mexten)
      // if (userCoord != null)
      //   nwMsgsSend.safeCall(userCoord, mdelivery, mtype, newMsg, mexten)
      // else{
      //   Toast.makeText(app,"Localization problem. Message cannot be delivered.",Toast.LENGTH_SHORT)
      //   LOG.E("Localization problem. Message cannot be delivered.")
      // }
    }
    collectMsgsSend()
  }

  /**
   * React to flow that is populated by [nwMsgsSend] safeCall
   */
  fun collectMsgsSend() {
    viewModelScope.launch {
      nwMsgsSend.collect(app)
    }
  }

}