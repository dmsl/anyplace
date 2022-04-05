package cy.ac.ucy.cs.anyplace.smas.data.models

import android.widget.Toast
import cy.ac.ucy.cs.anyplace.lib.android.LOG
import cy.ac.ucy.cs.anyplace.lib.android.extensions.TAG
import cy.ac.ucy.cs.anyplace.smas.SmasApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Handling SMAS errors.
 * - Logging-out the user if required
 */
class SmasErrors(private val app: SmasApp,
                 private val scope: CoroutineScope) {
  private val SESSION_KEY = "invalid sessionkey"
  private val DB_ERROR = "db error"
  private val SESSION_KEY_PRETTY = "Logged session expired"
  private val DB_ERROR_PRETTY = "Server Database error"

  /**
   * Returns [true] when an error is handled
   */
  fun handle(app: SmasApp, cause: String?, extra: String?) : Boolean {
    return when (cause) {
      SESSION_KEY -> {
        scope.launch {
          app.showToast("$SESSION_KEY_PRETTY ($extra)", Toast.LENGTH_SHORT)
          LOG.W(TAG, "Logging out user (from: $extra)")
          logoutUser()
        }
        true
      }
      DB_ERROR -> {
        val msg = "$DB_ERROR_PRETTY ($extra)"
        app.showToast(msg, Toast.LENGTH_SHORT)
        LOG.W(TAG, msg)
        true
      }
      else -> false
    }
  }

  private suspend fun logoutUser() {
    app.dsChatUser.deleteUser()
  }

}