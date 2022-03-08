package cy.ac.ucy.cs.anyplace.smas

import cy.ac.ucy.cs.anyplace.smas.data.models.ChatUserLoginForm
import cy.ac.ucy.cs.anyplace.smas.data.models.ChatUserLoginResp
import cy.ac.ucy.cs.anyplace.smas.data.models.ChatVersion
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

/** SMAS Chat API */
interface ChatAPI {

  // @POST("/smas/pm-test.php")
  @POST("/smas/version.php")
  suspend fun getVersion(@Body chatUserAuth: ChatUserAuth): Response<ChatVersion>

  @POST("/smas/login.php")
  suspend fun userLogin(@Body userLoginLocal: ChatUserLoginForm): Response<ChatUserLoginResp>

  // TODO GET LOCATIONS
}

data class ChatUserAuth(
        val uid: String,
        val sessionkey: String)