package cy.ac.ucy.cs.anyplace.smas.data.source

import cy.ac.ucy.cs.anyplace.smas.ChatUserAuth
import cy.ac.ucy.cs.anyplace.smas.data.models.*
import cy.ac.ucy.cs.anyplace.smas.utils.network.RetrofitHolderChat
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
  suspend fun locationGet(r: ChatUserAuth) : Response<UserLocations> = RH.api.locationGet(r)
  suspend fun locationSend(r: LocationSendReq) : Response<LocationSendResp> = RH.api.locationSend(r)

  // CHAT
  suspend fun messagesGet(r: MsgGetReq) : Response<MsgGetResp> = RH.api.messagesGet(r)
  suspend fun messagesSend(r: MsgSendReq) : Response<MsgSendResp> = RH.api.messageSend(r)
}