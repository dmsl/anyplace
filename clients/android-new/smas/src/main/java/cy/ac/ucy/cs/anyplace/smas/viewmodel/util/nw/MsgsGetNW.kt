package cy.ac.ucy.cs.anyplace.smas.viewmodel.util.nw

import android.content.Context
import android.widget.Toast
import androidx.lifecycle.viewModelScope
import cy.ac.ucy.cs.anyplace.lib.android.extensions.METHOD
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
  suspend fun safeCall(showToast: Boolean = false) {
    LOG.D2(TAG_METHOD)
    LOG.D2(TAG, "msg-get: size: ${VM.msgList.size}")

    if (resp.value is NetworkResult.Loading) {
      LOG.W(TAG, "MsgsGet: already in progress (skipped)")
      return
    }


    resp.value = NetworkResult.Loading()
    LOG.D2(TAG, "msg-get: size: ${VM.msgList.size} after resetting..")
    chatUser = app.dsChatUser.readUser.first()

    if (app.hasInternet()) {
      try {
        val lastTimestamp = repo.local.getLastMsgTimestamp()
        var incrementalFetch = false
        val response : Response<ChatMsgsResp>
        if (lastTimestamp==null) {
          LOG.W(TAG, "Will fetch ALL messages")
          response = repo.remote.messagesGet(MsgGetReq(chatUser))
        } else {
          incrementalFetch=true
          response = repo.remote.messagesGetFrom(MsgGetReq(chatUser), lastTimestamp)
        }

        LOG.D2(TAG, "Messages: ${response.message()}")
        resp.value = handleResponse(response, incrementalFetch, showToast)

        // Persist msgs in local store
        val msgs = resp.value.data
        // no new msgs fetched. instead the prev msgs were were loaded from localDB
        val dbLoaded = resp.value.message.toString() == NetworkResult.DB_LOADED
        if (msgs != null && !dbLoaded) {
          LOG.W(TAG, "WILL PERSIST to DB")
          persistToDB(msgs)
        } else {
          // TODO only for new msgs..
          LOG.W(TAG, "WILL NOT PERSIST to DB")
        }

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
        VM.viewModelScope.launch(Dispatchers.Main) {
          app.showToast(msg, Toast.LENGTH_LONG)
        }
      }

      resp.value = getMsgsFromDB()
    }
  }

  /**
   * Reads messages from Room (SQLite)
   */
  private suspend fun getMsgsFromDB(): NetworkResult<ChatMsgsResp> {
    LOG.D2(TAG, "reading msgs from cache")
    val localMsgs = repo.local.readMsgs().first()

    return if (localMsgs.isNotEmpty()) {
      NetworkResult.Success(entityToChatMessages(localMsgs), NetworkResult.DB_LOADED)
    } else {
      NetworkResult.Error(C.ERR_MSG_NO_INTERNET)
    }
  }

  private suspend fun handleResponse(
          response: Response<ChatMsgsResp>,
          incrementalFetch: Boolean,
          showToast: Boolean): NetworkResult<ChatMsgsResp> {
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
            if (incrementalFetch) {
              if (VM.msgList.isEmpty()) {
                // initial load of app: no new messages, but haven't loaded yet msgs from local DB
                LOG.E(TAG,"Incremental fetch: 0 msgs & 0 in msgList. (reading from DB..)")
                return getMsgsFromDB()
              } else {
                LOG.E(TAG, "msgList prev msgs: ${VM.msgList.size}")
                return NetworkResult.Success(response.body()!!, NetworkResult.UP_TO_DATE)
              }
            }

            val msg = "No new messages."
            if (showToast) {
              VM.viewModelScope.launch(Dispatchers.Main) {
                LOG.D3(TAG, msg)
                // app.showToast(msg, Toast.LENGTH_LONG)
              }
            }

            // empty messages
            LOG.W(TAG, msg)
            return NetworkResult.Success(response.body()!!)
          }

          var resp  = response.body()!!
          var loadType: String? = null
          if (incrementalFetch)  {
            val oldMsgs=getMsgsFromDB().data!!.msgs // local messages
            val newMsgs = resp.msgs // remote messages

            persistToDB(resp) // persist after we use [getMsgsFromDB] (avoid dups)

            // localMsgs are descending: newest msgs is first
            // they are merged w/ the [newMsgs] from the remote (also descending)
            val mergedMsgs = newMsgs + oldMsgs
            val merged=ChatMsgsResp(resp.status, resp.descr, resp.uid, mergedMsgs)
            LOG.E(TAG, "TODO: merged messages: local: ${oldMsgs.size} remote: ${resp.msgs.size}")
            LOG.E(TAG, "Status: ${resp.status} desc: ${resp.descr}")

            // LOG.E(TAG, "all msgs")
            // mergedMsgs.forEach {
            //   val wMsg = ChatMsgHelper(app, repo, it)
            //   LOG.W(TAG, "MSG: ${wMsg.content()}")
            // }

            resp=merged
            loadType=NetworkResult.DB_LOADED
          }

          // This is actually hybrid:
          // - we load new msgs from remote. we persist those async in DB.
          // - we merge newMsgs w/ local msgs
          // - and return the result
          return NetworkResult.Success(resp, loadType)

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
          when (it.message) {
            NetworkResult.UP_TO_DATE -> {
              LOG.E(TAG, "Messages: up-to-date. MsgList size: ${VM.msgList.size} (skip processing)")
            }
            NetworkResult.DB_LOADED,
            null -> {
              val msgs = it.data!!.msgs
              LOG.E(TAG, "Messages: new: ${msgs.size}. Old MsgList size: ${VM.msgList.size}. (processing)")
              appendMessages(ctx, VM, msgs)
            }
          }

        }
        // TODO:PMX unspecified error
        is NetworkResult.Error ->  {
          //db error
          if (!err.handle(app, it.message, "msg-get")) {
            val msg = it.message ?: "unspecified error"
            VM.viewModelScope.launch(Dispatchers.Main) {
              app.showToast(msg, Toast.LENGTH_SHORT)
              LOG.E(TAG, msg)
              // LOG.E(TAG, "$msg: from MsgGetNW Collect. class: ${it::class.simpleName}")
            }
          }
        }
        else -> {}
      }
    }
  }

  // TODO:PM
  // 1. make refresh for whole chat: messages, locations, etc.
  // 2. messages should be fetched: in mid delay:
  //// e.g. after 2 secs: locations: after 1 sec msgs
  // Separate alerts from others?
  private fun appendMessages(ctx: Context, VM: SmasChatViewModel, msgs: List<ChatMsg>) {
    LOG.W(TAG,"PROCESS-MSGS: new list: ${msgs.size}")
    LOG.W(TAG,"PROCESS-MSGS: msgList: ${VM.msgList.size}")
    // val msgList = mutableStateListOf<ChatMsg>()
    msgs.forEach { obj ->
      val msgH = ChatMsgHelper(ctx, repo, obj)
      val contents = msgH.content()

      // cache images
      if (obj.mtype == 2) VM.chatCache.saveImg(obj)

      VM.msgList.add(obj)
      val prettyTimestamp = utlTime.getPrettyEpoch(obj.time, utlTime.TIMEZONE_CY)
      LOG.D4(TAG, "MSG |$prettyTimestamp| ${msgH.prettyTypeCapitalize.format(6)} | $contents  || [${obj.time}][${obj.timestr}]")
    }

    // TODO: CLR this..
    // VM.msgFlow.value = msgList
    // VM.msgList = msgList
  }

  // ROOM
  /**
   * Persists [ChatMsg]s to SQLite (through ROOM).
   * It does not store images (base64) to DB.
   * Those will be stored in [SmasCache] (file cache)
   */
  private fun persistToDB(msgs: ChatMsgsResp) {
    LOG.D2(TAG, "$METHOD: storing msgs: ${msgs.msgs.size}")
    VM.viewModelScope.launch(Dispatchers.IO) {
      // repo.local.dropMsgs() // TODO: don't drop msgs first..
      msgs.msgs.forEach { msg ->
        repo.local.insertMsg(msg)
      }
    }
  }

}

