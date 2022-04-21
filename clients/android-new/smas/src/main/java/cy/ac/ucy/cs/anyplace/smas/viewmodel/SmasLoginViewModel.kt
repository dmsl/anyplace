package cy.ac.ucy.cs.anyplace.smas.viewmodel

import android.app.Application
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import cy.ac.ucy.cs.anyplace.lib.R
import cy.ac.ucy.cs.anyplace.lib.android.utils.LOG
import cy.ac.ucy.cs.anyplace.lib.android.data.LoginFormState
import cy.ac.ucy.cs.anyplace.lib.android.extensions.TAG
import cy.ac.ucy.cs.anyplace.lib.android.extensions.TAG_METHOD
import cy.ac.ucy.cs.anyplace.lib.android.extensions.app
import cy.ac.ucy.cs.anyplace.lib.network.NetworkResult
import cy.ac.ucy.cs.anyplace.smas.consts.CHAT
import cy.ac.ucy.cs.anyplace.smas.data.RepoChat
import cy.ac.ucy.cs.anyplace.smas.data.models.ChatLoginReq
import cy.ac.ucy.cs.anyplace.smas.data.models.ChatLoginResp
import cy.ac.ucy.cs.anyplace.smas.data.source.RetrofitHolderChat
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import retrofit2.Response
import java.lang.Exception
import javax.inject.Inject

/**
 * Handling the [ChatUser] login (used by [SmasLoginActivity])
 */
@HiltViewModel
class SmasLoginViewModel @Inject constructor(
        application: Application,
        private val repo: RepoChat,
        private val retrofitHolder: RetrofitHolderChat) : AndroidViewModel(application) {

  private val C by lazy { CHAT(application.applicationContext) }

  private val _loginForm = MutableLiveData<LoginFormState>()
  val loginFormState: LiveData<LoginFormState> = _loginForm

  //// RETROFIT
  val resp: MutableStateFlow<NetworkResult<ChatLoginResp>> = MutableStateFlow(NetworkResult.Unset())

  fun login(req: ChatLoginReq) = viewModelScope.launch { safeCall(req) }

  private suspend fun safeCall(req: ChatLoginReq) {
    LOG.D(TAG_METHOD)
    resp.value = NetworkResult.Loading()
    var exception : Exception? = null
      if (app.hasInternet()) {
      try {
        val response = repo.remote.userLogin(req)
        resp.value=NetworkResult.Unset()
        resp.value = handleResponse(response)

        if (resp.value is NetworkResult.Error) {
         exception = Exception(resp.value.message)
        } else if (resp.value is NetworkResult.Success
                && resp.value.data?.status == "err") {
          // login failed
          exception = Exception(resp.value.data?.descr)
        }
      } catch(e: Exception) {
        exception = e
        val msg = "Login failed"
        handleException(msg, e)
        e.let {
          when {
            e.message?.contains(C.ERR_MSG_HTTP_FORBIDEN) == true -> {
              exception = Exception(C.MSG_ERR_ONLY_SSL)
            }
            e.message?.contains(C.ERR_MSG_ILLEGAL_STATE) == true -> {
              exception = Exception(C.MSG_ERR_ILLEGAL_STATE)
            }
            e.message?.contains(C.EXCEPTION_MSG_NPE) == true -> {
              exception = Exception(C.MSG_ERR_NPE)
            }
          }
        }
        LOG.E(TAG, "Exception: ${exception!!.message}")
      }
      } else {
        exception = Exception("No Internet Connection.")
      }

    exception?.let { it ->
      val msg = it.message.toString()
      LOG.E(TAG, msg)
      resp.value = NetworkResult.Error(exception?.message)
    }
  }

  private fun handleException(msg: String, e: Exception) {
    if (e is IllegalStateException) {
      resp.value = NetworkResult.Error(e.javaClass.name)
    } else {
      resp.value = NetworkResult.Error(msg)
    }
    LOG.E(msg)
    LOG.E(TAG, e)
  }

  private fun handleResponse(response: Response<ChatLoginResp>):
          NetworkResult<ChatLoginResp> {
    return when {
      response.message().toString().contains("timeout") -> NetworkResult.Error("Timeout.")
      !response.isSuccessful-> {
        val rawErrorResponse = response.errorBody()!!.string()
        val errorReponse = Gson().fromJson(rawErrorResponse, ChatLoginResp::class.java)
        NetworkResult.Error(errorReponse.descr)
      }
      response.isSuccessful -> {
        val user = response.body()!!
        LOG.D(TAG_METHOD, "LOGIN OK: $user")
        // TODO:DZ SMAS API return 401
        // SMAS API wrongly marks result as OK:
        if (user.status=="err") {
          NetworkResult.Error(user.descr)
        } else {
          NetworkResult.Success(user)
        }
      } // can be nullable
      else -> NetworkResult.Error(response.message())
    }
  }

  fun loginDataChanged(username: String, password: String) {
    if (!isUserNameValid(username)) {
      _loginForm.value = LoginFormState(usernameError = R.string.invalid_username)
    } else if (!isPasswordValid(password)) {
      _loginForm.value = LoginFormState(passwordError = R.string.invalid_password)
    } else {
      _loginForm.value = LoginFormState(isDataValid = true)
    }
  }

  private fun isUserNameValid(username: String): Boolean {
    return username.isNotBlank()
  }

  private fun isPasswordValid(password: String): Boolean {
    return password.length > 3
  }
}