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
import cy.ac.ucy.cs.anyplace.lib.android.extensions.TAG
import cy.ac.ucy.cs.anyplace.lib.models.UserCoordinates
import cy.ac.ucy.cs.anyplace.smas.SmasApp
import cy.ac.ucy.cs.anyplace.smas.consts.CHAT
import cy.ac.ucy.cs.anyplace.smas.data.RepoChat
import cy.ac.ucy.cs.anyplace.smas.data.models.ChatMsg
import cy.ac.ucy.cs.anyplace.smas.data.models.ReplyToMessage
import cy.ac.ucy.cs.anyplace.smas.data.models.UserLocations
import cy.ac.ucy.cs.anyplace.smas.data.store.ChatPrefsDataStore
import cy.ac.ucy.cs.anyplace.smas.ui.chat.theme.AnyplaceBlue
import cy.ac.ucy.cs.anyplace.smas.data.files.SmasCache
import cy.ac.ucy.cs.anyplace.lib.android.utils.utlImg
import cy.ac.ucy.cs.anyplace.smas.ui.dialogs.ImgDialog
import cy.ac.ucy.cs.anyplace.smas.ui.dialogs.MsgDeliveryDialog
import cy.ac.ucy.cs.anyplace.smas.data.source.RetrofitHolderChat
import cy.ac.ucy.cs.anyplace.smas.viewmodel.util.nw.MsgGetNW
import cy.ac.ucy.cs.anyplace.smas.viewmodel.util.nw.MsgSendNW
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
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
        dsCvNav: CvNavDataStore,
        private val dsMisc: MiscDataStore,
) : AndroidViewModel(_application) {

  private val app = _application as SmasApp
  private val C by lazy { CHAT(app.applicationContext) }

  val nwMsgGet by lazy { MsgGetNW(app, this, RFH, repoChat) }
  private val nwMsgSend by lazy { MsgSendNW(app, this, RFH, repoChat) }

  val chatCache by lazy { SmasCache(app.applicationContext) }

  // Preferences
  val prefsCvNav = dsCvNav.read

  /** How often to refresh UI components from backend (in ms) */
  private var refreshMs : Long = C.DEFAULT_PREF_SMAS_LOCATION_REFRESH.toLong()*1000L

  //Variables observed by composable functions
  var reply: String by mutableStateOf("")
  var imageUri: Uri? by mutableStateOf(null)
  var showDialog: Boolean by mutableStateOf(false)
  var replyToMessage: ReplyToMessage? by mutableStateOf(null)
  var mdelivery: String by mutableStateOf("")
  var errColor: Color by mutableStateOf(AnyplaceBlue)
  var isLoading: Boolean by mutableStateOf(false)

  var newMessages = MutableStateFlow(false)

  private fun collectRefreshMs() {
    viewModelScope.launch(Dispatchers.IO) {
      prefsCvNav.collectLatest{ refreshMs = it.locationRefresh.toLong()*1000L }
    }
  }

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

  fun netPullMessagesONCE(showToast: Boolean = false) {
    LOG.D2(TAG, "PULL-MSGS")
    viewModelScope.launch(Dispatchers.IO) {
      nwMsgGet.safeCall(showToast)
    }
  }

  fun netPullMessagesLOOP()  {
    viewModelScope.launch(Dispatchers.IO) {
      collectRefreshMs()
      while (true) {
        LOG.D2(TAG, "loop: pull-msgs")
        nwMsgGet.safeCall()
        delay(refreshMs)
      }
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
        mexten = utlImg.getMimeType(imageUri!!, app)
      }

      // TODO:PMX real coordinates
      val dummy = UserCoordinates("1234",1,5.0,5.0)
      nwMsgSend.safeCall(dummy, mdelivery, mtype, newMsg, mexten)
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
   * React to flow that is populated by [nwMsgSend] safeCall
   */
  fun collectMsgsSend() {
    viewModelScope.launch {
      nwMsgSend.collect(app)
    }
  }

  fun savedNewMsgs(value: Boolean) {
    viewModelScope.launch(Dispatchers.IO) {
      dsChat.saveNewMsgs(value)
    }
  }

}