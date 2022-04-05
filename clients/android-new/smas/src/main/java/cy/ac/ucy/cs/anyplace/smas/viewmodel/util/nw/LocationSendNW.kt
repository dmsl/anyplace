package cy.ac.ucy.cs.anyplace.smas.viewmodel.util.nw

import android.widget.Toast
import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.model.LatLng
import cy.ac.ucy.cs.anyplace.lib.android.LOG
import cy.ac.ucy.cs.anyplace.lib.android.extensions.TAG
import cy.ac.ucy.cs.anyplace.lib.android.utils.utlTime
import cy.ac.ucy.cs.anyplace.lib.models.UserCoordinates
import cy.ac.ucy.cs.anyplace.lib.network.NetworkResult
import cy.ac.ucy.cs.anyplace.smas.SmasApp
import cy.ac.ucy.cs.anyplace.smas.consts.CHAT
import cy.ac.ucy.cs.anyplace.smas.data.RepoChat
import cy.ac.ucy.cs.anyplace.smas.data.models.ChatUser
import cy.ac.ucy.cs.anyplace.smas.data.models.LocationSendReq
import cy.ac.ucy.cs.anyplace.smas.data.models.LocationSendResp
import cy.ac.ucy.cs.anyplace.smas.data.models.SmasErrors
import cy.ac.ucy.cs.anyplace.smas.utils.network.RetrofitHolderChat
import cy.ac.ucy.cs.anyplace.smas.viewmodel.SmasMainViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import retrofit2.Response
import java.lang.Exception
import java.net.ConnectException

/**
 * Manages Location fetching of other users
 */
class LocationSendNW(
        private val app: SmasApp,
        private val VM: SmasMainViewModel,
        private val RH: RetrofitHolderChat,
        private val repo: RepoChat) {

  companion object {
    val TEST_COORDS = LatLng(57.69579631991111, 11.913666007922222)
  }

  enum class Mode {
    normal,
    alert,
  }

  private val err by lazy { SmasErrors(app, VM.viewModelScope) }

  /** Network Responses from API calls */
  private val resp: MutableStateFlow<NetworkResult<LocationSendResp>> = MutableStateFlow(NetworkResult.Unset())

  /** whether the user is issuing an alert or not */
  val mode: MutableStateFlow<Mode> = MutableStateFlow(Mode.normal)

  fun alerting() = mode.value == Mode.alert

  private fun getAlertFlag(): Int {
    return when (mode.value) {
      Mode.alert -> 1
      Mode.normal -> 0
    }
  }

  private val C by lazy { CHAT(app.applicationContext) }
  private lateinit var chatUser : ChatUser

  /** Send the [Chatuser]'s location (safecall) */
  suspend fun safeCall(userCoords: UserCoordinates) {
    chatUser = app.dsChatUser.readUser.first()

    LOG.D4(TAG, "Session: ${chatUser.uid} ${chatUser.sessionkey}")

    resp.value = NetworkResult.Unset()
    resp.value = NetworkResult.Loading()
    if (app.hasInternet()) {
      try {
        val req= LocationSendReq(chatUser, getAlertFlag(), userCoords, utlTime.epoch().toString())
        LOG.D3(TAG, "LocSend: ${req.time}: tp: ${mode.value} deck: ${req.deck}: x:${req.x} y:${req.y}")
        val response = repo.remote.locationSend(req)
        LOG.D4(TAG, "LocationSend: Resp: ${response.message()}" )
        resp.value = handleResponse(response)
      } catch(ce: ConnectException) {
        val msg = "Connection failed:\n${RH.retrofit.baseUrl()}"
        handleException(msg, ce)
      } catch(e: Exception) {
        val msg = "$TAG: Not Found." + "\nURL: ${RH.retrofit.baseUrl()}"
        handleException(msg, e)
      }
    } else {
      resp.value = NetworkResult.Error(C.ERR_MSG_NO_INTERNET)
    }
  }

  private fun handleResponse(resp: Response<LocationSendResp>): NetworkResult<LocationSendResp> {
    LOG.D3(TAG, "handleResponse")
    if(resp.isSuccessful) {
      when {
        resp.message().toString().contains("timeout") -> return NetworkResult.Error("Timeout.")
        resp.isSuccessful -> {
          // SMAS special handling (errors should not be 200/OK)
          val r = resp.body()!!
          if (r.status == "err")  {
            return NetworkResult.Error(r.descr)
          }

          return NetworkResult.Success(r)
        } // can be nullable
        else -> return NetworkResult.Error(resp.message())
      }
    }

    return NetworkResult.Error("$TAG: ${resp.message()}")
  }

  private fun handleException(msg: String, e: Exception) {
    resp.value = NetworkResult.Error(msg)
    LOG.E(TAG, msg)
    LOG.E(TAG, e)
  }

  // TODO: make icons gray.. if time delay......
  // TODO: show initials in icons....... (don't commit..)
  // TODO: model.. UCY
  suspend fun collect() {
    resp.collect {
      when (it)  {
        is NetworkResult.Success -> {
          LOG.D4(TAG, "LocationSend: ${it.data?.status}")
        }
        is NetworkResult.Error -> {
          if (!err.handle(app, it.message, "loc-send")) {
            val msg = it.message ?: "unspecified error"
            VM.viewModelScope.launch {
              app.showToast(msg, Toast.LENGTH_SHORT)
            }
          }
        }
        else -> {}
      }
    }
  }

}
