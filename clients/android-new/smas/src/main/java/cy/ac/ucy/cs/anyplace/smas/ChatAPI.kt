package cy.ac.ucy.cs.anyplace.smas

import cy.ac.ucy.cs.anyplace.smas.data.models.*
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

/** SMAS Chat API */
interface ChatAPI {

  @POST("/smas/version.php")
  suspend fun version(@Body body: Any = Object()): Response<ChatVersion>

  @POST("/smas/login.php")
  suspend fun userLogin(@Body req: ChatLoginReq): Response<ChatLoginResp>

  @POST("/smas/location-get.php")
  suspend fun userLocations(@Body user: ChatUserAuth): Response<UserLocations>

  @POST("/smas/msg-get.php")
  suspend fun userMessages(@Body user: ChatUserAuthMsgs): Response<ChatMsgsResp>
}

/** Authenticated ChatUser */
data class ChatUserAuth(
        val uid: String,
        val sessionkey: String) {
  constructor(user: ChatUser) : this(user.uid, user.sessionid)
}

data class ChatUserAuthMsgs(
        val uid: String,
        val sessionkey: String,
        val mgettype: Int) {
  constructor(user: ChatUser, mgettype: Int) : this(user.uid, user.sessionid, mgettype)
}