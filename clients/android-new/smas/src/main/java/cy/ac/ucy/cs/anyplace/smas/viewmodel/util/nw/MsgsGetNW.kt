package cy.ac.ucy.cs.anyplace.smas.viewmodel.util.nw

import android.content.Context
import android.widget.Toast
import androidx.lifecycle.viewModelScope
import cy.ac.ucy.cs.anyplace.lib.android.consts.CONST
import cy.ac.ucy.cs.anyplace.lib.android.utils.LOG
import cy.ac.ucy.cs.anyplace.lib.android.extensions.TAG
import cy.ac.ucy.cs.anyplace.lib.android.extensions.TAG_METHOD
import cy.ac.ucy.cs.anyplace.lib.android.utils.utlTime
import cy.ac.ucy.cs.anyplace.lib.network.NetworkResult
import cy.ac.ucy.cs.anyplace.smas.SmasApp
import cy.ac.ucy.cs.anyplace.smas.consts.CHAT
import cy.ac.ucy.cs.anyplace.smas.data.RepoChat
import cy.ac.ucy.cs.anyplace.smas.data.db.entities.DatabaseConverters.Companion.entityToChatMessages
import cy.ac.ucy.cs.anyplace.smas.data.models.*
import cy.ac.ucy.cs.anyplace.smas.data.models.helpers.ChatMsgHelper
import cy.ac.ucy.cs.anyplace.smas.utils.network.RetrofitHolderChat
import cy.ac.ucy.cs.anyplace.smas.viewmodel.SmasChatViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import retrofit2.Response
import java.lang.Exception
import java.net.ConnectException

class MsgsGetNW(
        private val app: SmasApp,
        private val VM: SmasChatViewModel,
        private val RH: RetrofitHolderChat,
        private val repo: RepoChat) {

  /** Network Responses from API calls
   *
   * TODO: this is the last batch of messages.
   *  - how distinguish new from already received messages?
   *  - might need a separate flow
   * they have to be filtered & persisted (TODO:PM SQLite)
   */
  private val resp: MutableStateFlow<NetworkResult<ChatMsgsResp>> = MutableStateFlow(NetworkResult.Unset())

  private val C by lazy { CHAT(app.applicationContext) }
  private val err by lazy { SmasErrors(app, VM.viewModelScope) }
  private lateinit var chatUser: ChatUser

  /** Show warning only once */
  var shownNoInternetWarning = false

  /**
   * Get [ChatMsg] SafeCall
   *
   * TODO: get alert? or just get all messages? (I think the latter..)
   *
   * TODO:ATH: extend this method..
   */
  suspend fun safeCall(msgType: Int = 0) {
    LOG.D2(TAG_METHOD)
    resp.value = NetworkResult.Loading()
    chatUser = app.dsChatUser.readUser.first()

    if (app.hasInternet()) {
      try {

        // TODO:PM if it has msgs: get the latest only...

        val response = repo.remote.messagesGet(MsgGetReq(chatUser, msgType))
        LOG.D2(TAG, "ChatMessages: ${response.message()}")
        resp.value = handleResponse(response)

        // Persist msgs in local store
        val msgs = resp.value.data
        if (msgs != null) { cacheMessages(msgs) }

      } catch (ce: ConnectException) {
        val msg = "Connection failed:\n${RH.retrofit.baseUrl()}"
        handleException(msg, ce)
      } catch (e: Exception) {
        val msg = TAG
        handleException(msg, e)
      }
    } else {
      val msg="${C.ERR_MSG_NO_INTERNET} (message fetch)"
      LOG.D(TAG_METHOD, msg)
      if (!shownNoInternetWarning) {
        shownNoInternetWarning=true
        app.showToast(msg, Toast.LENGTH_LONG)
      }

      val localMsgs = repo.local.readMsgs().first()
      if (localMsgs.isNotEmpty()) {
        resp.value=NetworkResult.Success(entityToChatMessages(localMsgs))
      } else {
        resp.value = NetworkResult.Error(C.ERR_MSG_NO_INTERNET)
      }
    }
  }

  private fun handleResponse(response: Response<ChatMsgsResp>): NetworkResult<ChatMsgsResp> {
    LOG.D3()
    if (response.isSuccessful) {
      when {
        response.message().toString().contains("timeout") -> return NetworkResult.Error("Timeout.")
        // response.body()!!.chatMsgs.isNullOrEmpty() -> return NetworkResult.Error("Can't get messages.")
        response.isSuccessful -> {

          LOG.E(TAG, "MSGS-GET: Successful")

          // SMAS special handling (errors should not be 200/OK)
          val r = response.body()!!
          if (r.status == "err") {
            return NetworkResult.Error(r.descr)
          }

          // CHECK: this could be normal?
          if (response.body()!!.msgs.isEmpty()) {
            val errMsg = "No new messages received."
            LOG.E(TAG, errMsg)
            return NetworkResult.Error(errMsg)
          }

          return NetworkResult.Success(response.body()!!)
        } // can be nullable
        else -> return NetworkResult.Error(response.message())
      }
    }

    return NetworkResult.Error("$TAG: ${response.message()}")
  }

  private fun handleException(msg: String, e: Exception) {
    val details = "$msg:${e.message}"
    LOG.E(TAG_METHOD, details)
    resp.value = NetworkResult.Error(details)
  }

  suspend fun collect(ctx: Context) {
    resp.collect {
      when (it) {
        is NetworkResult.Success -> {
          val msgs = it.data!!.msgs
          LOG.E(TAG, "Got new messages: ${msgs.size}")
          process(ctx, VM, msgs)
        }
        else -> {
          //db error
          if (!err.handle(app, it.message, "msg-get")) {
            val msg = it.message ?: "unspecified error"
            VM.viewModelScope.launch {
              app.showToast(msg, Toast.LENGTH_SHORT)
              LOG.E(TAG, msg)
            }
          }
        }
      }
    }
  }

  // TODO:PM
  // 1. make refresh for whole chat: messages, locations, etc.
  // 2. messages should be fetched: in mid delay:
  //// e.g. after 2 secs: locations: after 1 sec msgs
  // Separate alerts from others?
  private fun process(ctx: Context, VM: SmasChatViewModel, msgs: List<ChatMsg>) {

    msgs.forEach { obj ->
      val msgH = ChatMsgHelper(ctx, repo, obj)
      val contents = when {
        msgH.isAlert() -> msgH.obj.msg
        msgH.isText() -> msgH.obj.msg
        msgH.isImage() -> "<base64>"
        else -> "unknown"
      }
      if (obj.mtype == 2)
        VM.chatCache.saveImg(obj)
      VM.listOfMessages.add(obj)
      val prettyTimestamp = utlTime.getPrettyEpoch(obj.time.toLong(), utlTime.TIMEZONE_CY)
      LOG.D4(TAG, "MSG |$prettyTimestamp| ${msgH.prettyTypeCapitalize.format(6)} | $contents  || [${obj.time}][${obj.timestr}]")
    }
  }

  // ROOM
  private fun cacheMessages(msgs: ChatMsgsResp) {
    LOG.D2(TAG_METHOD, "storing msgs: ${msgs.msgs.size}")
    VM.viewModelScope.launch(Dispatchers.IO) {
      // repo.local.dropMsgs() // TODO: don't drop msgs first..
      msgs.msgs.forEach { msg ->
        repo.local.insertMsg(msg)
      }
    }
  }

}
