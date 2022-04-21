package cy.ac.ucy.cs.anyplace.smas.data.source

import cy.ac.ucy.cs.anyplace.smas.ChatUserAuth
import cy.ac.ucy.cs.anyplace.smas.data.models.*
import cy.ac.ucy.cs.anyplace.smas.data.models.helpers.ChatMsgHelper
import retrofit2.Response
import javax.inject.Inject

/**
 * Chat DataSource
 */
class ChatRemoteDataSource @Inject constructor(private val RH: RetrofitHolderChat) {

  // MISC
  suspend fun getVersion(): Response<ChatVersion>  = RH.api.version()

  // USER
  suspend fun userLogin(r: ChatLoginReq) : Response<ChatLoginResp> = RH.api.login(r)

  // LOCATION
  /** Get locations of all other users */
  suspend fun locationGet(r: ChatUserAuth) : Response<UserLocations> = RH.api.locationGet(r)
  /** Send location of current user */
  suspend fun locationSend(r: LocationSendReq) : Response<LocationSendResp> = RH.api.locationSend(r)

  // CHAT
  /** Get all Chat Messages */
  suspend fun messagesGet(r: MsgGetReq) : Response<ChatMsgsResp> = RH.api.messagesGet(r)

  /** Get all Chat Messages that have arrived after the timestamp */
  suspend fun messagesGetFrom(r: MsgGetReq, timestamp: Int) : Response<ChatMsgsResp> {
    val from=(timestamp+1).toString()  // +1 to make timestamp exclusive
    val req = MsgGetReq(r.uid, r.sessionkey, ChatMsgHelper.TP_GET_FROM, from)
    return RH.api.messagesGet(req)
  }
  suspend fun messagesSend(r: MsgSendReq) : Response<MsgSendResp> = RH.api.messageSend(r)
}