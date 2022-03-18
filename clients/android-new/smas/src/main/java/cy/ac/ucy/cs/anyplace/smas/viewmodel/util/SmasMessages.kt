package cy.ac.ucy.cs.anyplace.smas.viewmodel.util

import android.content.Context
import cy.ac.ucy.cs.anyplace.lib.android.LOG
import cy.ac.ucy.cs.anyplace.lib.android.extensions.TAG
import cy.ac.ucy.cs.anyplace.lib.android.extensions.TAG_METHOD
import cy.ac.ucy.cs.anyplace.lib.android.ui.cv.map.GmapHandler
import cy.ac.ucy.cs.anyplace.lib.android.utils.utlTime
import cy.ac.ucy.cs.anyplace.lib.network.NetworkResult
import cy.ac.ucy.cs.anyplace.smas.ChatUserAuthMsgs
import cy.ac.ucy.cs.anyplace.smas.SmasApp
import cy.ac.ucy.cs.anyplace.smas.consts.CHAT
import cy.ac.ucy.cs.anyplace.smas.data.RepoChat
import cy.ac.ucy.cs.anyplace.smas.data.models.ChatMsg
import cy.ac.ucy.cs.anyplace.smas.data.models.ChatMsgsResp
import cy.ac.ucy.cs.anyplace.smas.data.models.ChatUser
import cy.ac.ucy.cs.anyplace.smas.data.models.helpers.ChatMsgHelper
import cy.ac.ucy.cs.anyplace.smas.utils.network.RetrofitHolderChat
import cy.ac.ucy.cs.anyplace.smas.viewmodel.SmasChatViewModel
import cy.ac.ucy.cs.anyplace.smas.viewmodel.SmasMainViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import retrofit2.Response
import java.lang.Exception
import java.net.ConnectException

/**
 * TODO distinguish between alerts
 */
class SmasMessages(
        private val app: SmasApp,
        private val RH: RetrofitHolderChat,
        private val repo: RepoChat) {


  // might need a separate flow
  /** Network Responses from API calls
   * TODO: this is the last batch of messages. they have to be filterd & persisted
   */
  // TODO alerts?
  // TODO: how distinquish new from already received messages?
  private val resp: MutableStateFlow<NetworkResult<ChatMsgsResp>> = MutableStateFlow(NetworkResult.Unset())

  private val C by lazy { CHAT(app.applicationContext) }
  private lateinit var chatUser : ChatUser

  /**
   * Get [ChatMsg] SafeCall
   *
   * TODO: get alert? or just get all messages? (I think the latter..)
   */
  suspend fun getSC() {
    LOG.D2(TAG_METHOD)
    chatUser = app.chatUserDS.readUser.first()

    resp.value = NetworkResult.Unset()
    if (app.hasInternet()) {
      try {
        val msgType = 0
        val response = repo.remote.userMessages(ChatUserAuthMsgs(chatUser, msgType))
        LOG.D2(TAG, "ChatMessages: ${response.message()}" )
        resp.value = handleResponse(response)

        // TODO: PERSISTS: in cache (SQLITE)
        // TODO Persist: put in cache & list (main mem).
        // TODO: if has local messages: pull latest only
        val userMessages = resp.value.data
      } catch(ce: ConnectException) {
        val msg = "Connection failed:\n${RH.retrofit.baseUrl()}"
        handleError(msg, ce)
      } catch(e: Exception) {
        val msg = "$TAG: Not Found.\nURL: ${RH.retrofit.baseUrl()}"
        handleError(msg, e)
      }
    } else {
      resp.value = NetworkResult.Error(C.ERR_MSG_NO_INTERNET)
    }
  }

  private fun handleResponse(response: Response<ChatMsgsResp>): NetworkResult<ChatMsgsResp> {
    LOG.D2()
    if(response.isSuccessful) {
      return when {
        response.message().toString().contains("timeout") -> NetworkResult.Error("Timeout.")
        response.body()!!.chatMsgs.isNullOrEmpty() -> NetworkResult.Error("Can't get messages.")
        response.isSuccessful -> {

          // CHECK: this could be normal?
          if (response.body()!!.chatMsgs.isEmpty()) {
            return NetworkResult.Error("No new messages received.")
          }

          NetworkResult.Success(response.body()!!)
        } // can be nullable
        else -> NetworkResult.Error(response.message())
      }
    }

    return NetworkResult.Error("$TAG: ${response.message()}")
  }

  private fun handleError(msg:String, e: Exception) {
    resp.value = NetworkResult.Error(msg)
    LOG.E(TAG_METHOD, msg)
    LOG.E(TAG_METHOD, e)
  }

  suspend fun collect(ctx: Context, VM: SmasChatViewModel) {
    resp.collect {
      when (it)  {
        is NetworkResult.Success -> {
          val msgs = it.data!!.chatMsgs
          LOG.E(TAG, "Got new messages: ${msgs.size}")
          process(ctx, VM, msgs)
        }
        else -> {
          LOG.E(TAG, "Error getting messages: ${it.message}")
        }
      }
    }
  }

  // TODO LEFTHERE:
  // 1. make refresh for whole chat: messages, locations, etc.
  // 2. messages should be fetched: in mid delay:
  //// e.g. after 2 secs: locations: after 1 sec msgs
  // Separate alerts from others?
  private fun process(ctx: Context, VM: SmasChatViewModel, msgs: List<ChatMsg>) {

    // TODO:DZ messages unsorted?

    msgs.forEach { msg ->
      val msgH = ChatMsgHelper(ctx, repo, msg)
      val contents = when {
        msgH.isAlert() -> msgH.data.msg
        msgH.isText() -> msgH.data.msg
        msgH.isImage() -> "<base64>"
        else -> "unknown"
      }

      val prettyTimestamp = utlTime.getPrettyEpoch(msg.time.toLong(), utlTime.TIMEZONE_CY)
      LOG.E(TAG, "MSG |$prettyTimestamp| ${msgH.prettyTypeCapitalize.format(6)} | $contents  || [${msg.time}][${msg.timestr}]")
    }
  }

}
