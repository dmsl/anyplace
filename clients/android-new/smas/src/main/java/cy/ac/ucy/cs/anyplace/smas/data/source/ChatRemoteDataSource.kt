package cy.ac.ucy.cs.anyplace.smas.data.source

import cy.ac.ucy.cs.anyplace.smas.BuildConfig
import cy.ac.ucy.cs.anyplace.smas.ChatUserAuth
import cy.ac.ucy.cs.anyplace.smas.data.models.ChatUserLoginForm
import cy.ac.ucy.cs.anyplace.smas.data.models.ChatUserLoginResp
import cy.ac.ucy.cs.anyplace.smas.data.models.ChatVersion
import cy.ac.ucy.cs.anyplace.smas.utils.network.RetrofitHolderChat
import retrofit2.Response
import javax.inject.Inject

/**
 * Chat DataSource
 */
class ChatRemoteDataSource @Inject constructor(private val retrofitH: RetrofitHolderChat) {

  @Deprecated("TODO REMOVE THIS")
  private val demoUser = ChatUserAuth(BuildConfig.LASH_DEMO_UID, BuildConfig.LASH_DEMO_TOKEN)

  // MISC
  suspend fun getVersion(): Response<ChatVersion>  = retrofitH.api.getVersion(demoUser)

  // USER
  suspend fun userLogin(obj: ChatUserLoginForm) : Response<ChatUserLoginResp>
      = retrofitH.api.userLogin(obj)

  // TODO USER LOCATION
}