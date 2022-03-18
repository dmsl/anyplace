package cy.ac.ucy.cs.anyplace.smas.data.source

import cy.ac.ucy.cs.anyplace.smas.ChatUserAuth
import cy.ac.ucy.cs.anyplace.smas.ChatUserAuthMsgs
import cy.ac.ucy.cs.anyplace.smas.data.models.*
import cy.ac.ucy.cs.anyplace.smas.utils.network.RetrofitHolderChat
import retrofit2.Response
import javax.inject.Inject

/**
 * Chat DataSource
 */
class ChatRemoteDataSource @Inject constructor(private val retrofitH: RetrofitHolderChat) {

  // MISC
  suspend fun getVersion(): Response<ChatVersion>  = retrofitH.api.version()

  // USER
  suspend fun userLogin(obj: ChatLoginReq) : Response<ChatLoginResp>
      = retrofitH.api.userLogin(obj)

  suspend fun userLocations(obj: ChatUserAuth) : Response<UserLocations>
          = retrofitH.api.userLocations(obj)

  suspend fun userMessages(obj: ChatUserAuthMsgs) : Response<ChatMsgsResp>
          = retrofitH.api.userMessages(obj)
}