package cy.ac.ucy.cs.anyplace.smas.viewmodel.util

import cy.ac.ucy.cs.anyplace.lib.android.DBG
import cy.ac.ucy.cs.anyplace.lib.android.LOG
import cy.ac.ucy.cs.anyplace.lib.android.data.helpers.FloorHelper
import cy.ac.ucy.cs.anyplace.lib.android.extensions.TAG
import cy.ac.ucy.cs.anyplace.lib.android.extensions.TAG_METHOD
import cy.ac.ucy.cs.anyplace.lib.android.ui.cv.map.GmapHandler
import cy.ac.ucy.cs.anyplace.lib.network.NetworkResult
import cy.ac.ucy.cs.anyplace.smas.ChatUserAuth
import cy.ac.ucy.cs.anyplace.smas.SmasApp
import cy.ac.ucy.cs.anyplace.smas.consts.CHAT
import cy.ac.ucy.cs.anyplace.smas.data.RepoChat
import cy.ac.ucy.cs.anyplace.smas.data.models.ChatUser
import cy.ac.ucy.cs.anyplace.smas.data.models.UserLocations
import cy.ac.ucy.cs.anyplace.smas.utils.network.RetrofitHolderChat
import cy.ac.ucy.cs.anyplace.smas.viewmodel.SmasMainViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import retrofit2.Response
import java.lang.Exception
import java.net.ConnectException

class SmasUserLocations(
        private val app: SmasApp,
        private val RH: RetrofitHolderChat,
        private val repo: RepoChat) {

  /** Network Responses from API calls */
  private val resp: MutableStateFlow<NetworkResult<UserLocations>> = MutableStateFlow(NetworkResult.Unset())

  private val C by lazy { CHAT(app.applicationContext) }
  private lateinit var chatUser : ChatUser

  /** Get [UserLocations] SafeCall */
  suspend fun getSC() {
    LOG.D3(TAG, "get")
    chatUser = app.chatUserDS.readUser.first()

    resp.value = NetworkResult.Unset()
    if (app.hasInternet()) {
      try {
        val response = repo.remote.userLocations(ChatUserAuth(chatUser))
        LOG.D4(TAG, "UserLocations: ${response.message()}" )
        resp.value = handleResponse(response)

        // TODO:PM Persist: put in cache & list (main mem).
        val userLocations = resp.value.data
        // if (userLocations != null) { cache(useLocations, UserOwnership.PUBLIC) }
      } catch(ce: ConnectException) {
        val msg = "Connection failed:\n${RH.retrofit.baseUrl()}"
        handleError(msg, ce)
      } catch(e: Exception) {
        val msg = "$TAG: Not Found." + "\nURL: ${RH.retrofit.baseUrl()}"
        handleError(msg, e)
      }
    } else {
      resp.value = NetworkResult.Error(C.ERR_MSG_NO_INTERNET)
    }
  }

  private fun handleResponse(response: Response<UserLocations>): NetworkResult<UserLocations> {
    LOG.D4(TAG_METHOD)
    if(response.isSuccessful) {
      return when {
        response.message().toString().contains("timeout") -> NetworkResult.Error("Timeout.")
        response.body()!!.rows.isNullOrEmpty() -> NetworkResult.Error("User locations not found.")
        response.isSuccessful -> NetworkResult.Success(response.body()!!) // can be nullable
        else -> NetworkResult.Error(response.message())
      }
    }

    return NetworkResult.Error("$TAG: ${response.message()}")
  }

  private fun handleError(msg:String, e: Exception) {
    resp.value = NetworkResult.Error(msg)
    LOG.E(TAG, msg)
    LOG.E(TAG, e)
  }

  suspend fun collect(VM: SmasMainViewModel, gmap: GmapHandler) {
    resp.collect {
      when (it)  {
        is NetworkResult.Success -> {
          val locations = it.data
          LOG.D4(TAG, "Got user locations: ${locations?.rows?.size}")
          process(VM, locations, gmap)
        }
        else -> {}
      }
    }
  }

  private fun process(VM: SmasMainViewModel, locations: UserLocations?, gmap: GmapHandler) {
    LOG.D3(TAG)
    if (locations == null) return

    val FH = FloorHelper(VM.floor.value!!, VM.spaceH)
    val sameFloorUsers = locations.rows.filter { userLocation ->
      userLocation.buid == FH.spaceH.space.id &&  // same space
              userLocation.deck == FH.floor.floorNumber.toInt() && // same deck
              userLocation.uid != chatUser.uid // not current user
    }

    // TODO:PM right after
    // val dataset = MutableList<>();

    LOG.D4(TAG, "UserLocations: current floor: ${FH.prettyFloorName()}")
    gmap.renderUserLocations(sameFloorUsers)

    if (DBG.D2) {
      sameFloorUsers.forEach {
        LOG.D4(TAG, "User: ${it.uid} on floor: ${it.deck}")
      }
    }
  }

}
