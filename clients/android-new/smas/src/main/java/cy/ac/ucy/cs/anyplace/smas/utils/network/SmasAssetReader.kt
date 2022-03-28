package cy.ac.ucy.cs.anyplace.smas.utils.network

import android.content.Context
import com.google.gson.Gson
import cy.ac.ucy.cs.anyplace.lib.android.LOG
import cy.ac.ucy.cs.anyplace.lib.android.extensions.TAG
import cy.ac.ucy.cs.anyplace.lib.android.utils.demo.AssetReader
import cy.ac.ucy.cs.anyplace.smas.ui.chat.tmp_models.Messages
import java.lang.Exception

/**
 * [AssetReader] with additional functionality to read:
 * - chat messages
 */
class SmasAssetReader(ctx: Context) : AssetReader(ctx) {

  fun getMessages(): Messages? {
    val str = getMessagesStr()
    str.let {
      try {
        return Gson().fromJson(str, Messages::class.java)
      } catch (e: Exception) {
        LOG.E(TAG,"Failed to parse: $str")
      }
    }
    return null
  }

  private fun getMessagesStr(): String?{
    return getJsonDataFromAsset(ctx, "dummy_messages.json")
  }
}