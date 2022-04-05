package cy.ac.ucy.cs.anyplace.smas.viewmodel.util.nw

import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import androidx.preference.Preference
import cy.ac.ucy.cs.anyplace.lib.R
import cy.ac.ucy.cs.anyplace.lib.android.LOG
import cy.ac.ucy.cs.anyplace.lib.android.extensions.TAG
import cy.ac.ucy.cs.anyplace.lib.android.extensions.TAG_METHOD
import cy.ac.ucy.cs.anyplace.lib.android.utils.GenUtils
import cy.ac.ucy.cs.anyplace.lib.network.NetworkResult
import cy.ac.ucy.cs.anyplace.smas.SmasApp
import cy.ac.ucy.cs.anyplace.smas.consts.CHAT
import cy.ac.ucy.cs.anyplace.smas.data.RepoChat
import cy.ac.ucy.cs.anyplace.smas.data.models.ChatVersion
import cy.ac.ucy.cs.anyplace.smas.utils.network.RetrofitHolderChat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectIndexed
import kotlinx.coroutines.flow.collectLatest
import retrofit2.Response
import java.lang.Exception
import java.lang.NullPointerException
import java.net.UnknownServiceException

/**
 * Utility method to encapsulate:
 * - the SafeCall of the version endpoint and it's handling:
 */
class VersionNW(
        private val app: SmasApp,
        private val RH: RetrofitHolderChat,
        private val repoChat: RepoChat) {

  private val resp: MutableStateFlow<NetworkResult<ChatVersion>> = MutableStateFlow(NetworkResult.Unset())

  private val C by lazy { CHAT(app.applicationContext) }

  suspend fun safeCall(versionPref: Preference?) {
    LOG.D4(TAG_METHOD, "base url: ${RH.baseURL}")
    versionPref?.summary = "reaching server .."

    var msg = ""
    var exception : Exception? = null
    var versionColor : ForegroundColorSpan? = null

    if (app.hasInternet()) {
      try {
        val response = repoChat.remote.getVersion()
        resp.value = handleVersionResponse(response)
        val version = resp.value.data
        if (version != null) {  // SUCCESS
          msg = "${version.rows.version} (connected: ${GenUtils.prettyTime()})"
          versionPref?.icon = null

          // store it in the DS too
          app.dsChat.storeVersion(version.rows.version)
        } else {
          exception = Exception("Failed to get version.")
        }
      } catch(e: UnknownServiceException) {
        LOG.E(TAG, e)
        exception = e
        e.let {
          if (e.message?.contains(C.ERR_MSG_HTTP_FORBIDEN) == true) {
            exception = Exception(C.MSG_ERR_ONLY_SSL)
          }
          resp.value = NetworkResult.Error(e.message)
        }
      } catch(e: Exception) {
        LOG.E(TAG, "EXCEPTION: ${e.javaClass}")
        LOG.E(TAG, e)
        exception = when (e) {
          is NullPointerException -> Exception(C.MSG_ERR_NPE)
          else -> e
        }

        resp.value = NetworkResult.Error(exception?.message)
      }
    } else {
      exception = Exception("No internet connection.")
    }
    exception?.let { it ->
      msg = it.message.toString()
      versionPref?.setIcon(R.drawable.ic_sad)
      LOG.E(msg)
      LOG.E(it)
      versionColor = ForegroundColorSpan(app.getColor(R.color.redDark))
    } ?: run {
      versionPref?.setIcon(R.drawable.ic_happy)
    }

    val spannableMsg = SpannableString(msg)
    versionColor?.let { spannableMsg.setSpan(versionColor, 0, spannableMsg.length, 0) }
    versionPref?.summary = spannableMsg
  }

  private fun handleVersionResponse(response: Response<ChatVersion>): NetworkResult<ChatVersion> {
    return when {
      response.message().toString().contains("timeout") -> NetworkResult.Error("Timeout.")
      response.body()!!.rows.version.isEmpty() -> NetworkResult.Error("Version not found.")
      response.isSuccessful -> NetworkResult.Success(response.body()!!) // can be nullable
      else -> NetworkResult.Error("Cannot reach server.")
    }
  }
}