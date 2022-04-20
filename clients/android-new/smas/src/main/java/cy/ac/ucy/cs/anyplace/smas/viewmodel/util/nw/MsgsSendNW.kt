package cy.ac.ucy.cs.anyplace.smas.viewmodel.util.nw

import android.content.Context
import android.widget.Toast
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.viewModelScope
import cy.ac.ucy.cs.anyplace.lib.android.utils.LOG
import cy.ac.ucy.cs.anyplace.lib.android.extensions.TAG
import cy.ac.ucy.cs.anyplace.lib.android.extensions.TAG_METHOD
import cy.ac.ucy.cs.anyplace.lib.android.utils.utlTime
import cy.ac.ucy.cs.anyplace.lib.models.UserCoordinates
import cy.ac.ucy.cs.anyplace.lib.network.NetworkResult
import cy.ac.ucy.cs.anyplace.smas.SmasApp
import cy.ac.ucy.cs.anyplace.smas.consts.CHAT
import cy.ac.ucy.cs.anyplace.smas.data.RepoChat
import cy.ac.ucy.cs.anyplace.smas.data.models.*
import cy.ac.ucy.cs.anyplace.smas.data.models.helpers.ChatMsgHelper
import cy.ac.ucy.cs.anyplace.smas.ui.chat.theme.WineRed
import cy.ac.ucy.cs.anyplace.smas.utils.network.RetrofitHolderChat
import cy.ac.ucy.cs.anyplace.smas.viewmodel.SmasChatViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import retrofit2.Response
import java.lang.Exception
import java.net.ConnectException

class MsgsSendNW(private val app: SmasApp,
                 private val VM: SmasChatViewModel,
                 private val RH: RetrofitHolderChat,
                 private val repo: RepoChat) {

  private val resp: MutableStateFlow<NetworkResult<MsgSendResp>> = MutableStateFlow(NetworkResult.Unset())

  private val C by lazy { CHAT(app.applicationContext) }
  private lateinit var chatUser: ChatUser
  private val err by lazy { SmasErrors(app, VM.viewModelScope) }

  suspend fun safeCall(userCoords: UserCoordinates, mdelivery: String, mtype: Int, msg: String?, mexten: String?) {
    LOG.D2(TAG_METHOD)
    resp.value = NetworkResult.Loading()
    chatUser = app.dsChatUser.readUser.first()

    if (app.hasInternet()) {
      try {
        val req = MsgSendReq(chatUser, userCoords, mdelivery, msg, mtype, mexten, utlTime.epoch().toString())
        val content = if (ChatMsgHelper.isImage(mtype)) "<base64>" else msg
        LOG.D2(TAG, "MSG-SEND: Send: ${req.time}: mtype: ${mtype} msg: ${content} x,y: ${userCoords.lat},${userCoords.lon} deck: ${userCoords.level} ")
        val response = repo.remote.messagesSend(req)
        LOG.D2(TAG, "MSG-SEND: Resp: ${response.message()}")
        resp.value = handleResponse(response)
      } catch (ce: ConnectException) {
        val msg = "Connection failed:\n${RH.retrofit.baseUrl()}"
        handleException(msg, ce)
      } catch (e: Exception) {
        val msg = "$TAG: Not Found.\nURL: ${RH.retrofit.baseUrl()}"
        handleException(msg, e)
      }
    } else {
      resp.value = NetworkResult.Error(C.ERR_MSG_NO_INTERNET)
    }
  }

  private fun handleResponse(response: Response<MsgSendResp>): NetworkResult<MsgSendResp> {
    LOG.V3(TAG, "HandleResponse:: MsgSend")
    if (response.isSuccessful) {
      when {
        response.message().toString().contains("timeout") -> return NetworkResult.Error("Timeout.")
        // response.body()!!.chatMsgs.isNullOrEmpty() -> return NetworkResult.Error("Can't get messages.")
        response.isSuccessful -> {

          // SMAS special handling (errors should not be 200/OK)
          val r = response.body()!!
          if (r.status == "err") {
            return NetworkResult.Error(r.descr)
          }

          LOG.D2(TAG, "MSGS-SEND: Successful")
          LOG.D2(TAG, "MSGS-SEND: Pulling msgs right after send")
          // TODO:PMX LEFTHERE..
          VM.nwPullMessages()

          return NetworkResult.Success(r)
        } // can be nullable
        else -> return NetworkResult.Error(response.message())
      }
    }

    return NetworkResult.Error("$TAG: ${response.message()}")
  }

  private fun handleException(msg: String, e: Exception) {
    LOG.E(TAG_METHOD, msg)
    LOG.E(TAG_METHOD, e)
    resp.value = NetworkResult.Error(msg)
  }

  suspend fun collect(ctx: Context) {
    resp.collect {
      when (it) {
        is NetworkResult.Loading -> {
          VM.isLoading = true
        }
        is NetworkResult.Success -> {
          LOG.D1(TAG, "MessageSend: ${it.data?.status}")
          VM.isLoading = false
          VM.clearReply()
          VM.clearTheReplyToMessage()
        }
        is NetworkResult.Error -> {
          LOG.D1(TAG, "MessageSend Error: ${it.message}")
          VM.isLoading = false
          VM.errColor = WineRed
          VM.viewModelScope.launch {
            app.showToast("Message failed to send", Toast.LENGTH_SHORT)
          }
        }
        else -> {
          //db error
          if (!err.handle(app, it.message, "msg-send")) {
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
}