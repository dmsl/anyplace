package cy.ac.ucy.cs.anyplace.smas.utils

import android.content.Context
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import cy.ac.ucy.cs.anyplace.lib.android.utils.LOG
import cy.ac.ucy.cs.anyplace.lib.android.extensions.TAG
import cy.ac.ucy.cs.anyplace.lib.android.utils.demo.AssetReader
import cy.ac.ucy.cs.anyplace.smas.data.models.ChatMsg
import java.lang.Exception

/**
 * [AssetReader] with additional functionality to read:
 * - chat messages
 */
class SmasAssetReader(ctx: Context) : AssetReader(ctx) {

  fun getMessages(): TestMessages? {
    val str = getMessagesStr()
    str.let {
      try {
        return Gson().fromJson(str, TestMessages::class.java)
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

@Deprecated("")
data class TestMessages(
        @SerializedName("messages")
        val messagesList: List<ChatMsg>
)