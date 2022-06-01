package cy.ac.ucy.cs.anyplace.smas.utils

import android.content.Context
import com.example.search.modules.Route
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import cy.ac.ucy.cs.anyplace.lib.android.utils.LOG
import cy.ac.ucy.cs.anyplace.lib.android.extensions.TAG
import cy.ac.ucy.cs.anyplace.lib.android.utils.demo.AssetReader
import cy.ac.ucy.cs.anyplace.lib.models.Connections
import cy.ac.ucy.cs.anyplace.lib.models.POIs
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
        LOG.E(TAG, "Failed to parse: $str")
      }
    }
    return null
  }

  private fun getMessagesStr(): String? {
    return getJsonDataFromAsset(ctx, "dummy_messages.json")
  }

  fun getRoute(): Route? {
    val str = getRouteStr()
    str.let {
      try {
        return Gson().fromJson(str, Route::class.java)
      } catch (e: Exception) {
        LOG.E(TAG, "Failed to parse: $str")
      }
    }
    return null
  }

  fun getPois(): POIs?{
    val str = getPoisStr()
    str.let {
      try {
        return Gson().fromJson(str, POIs::class.java)
      } catch (e: Exception) {
        LOG.E(TAG,"Failed to parse: $str")
      }
    }
    return null
  }

  fun getConnections(): Connections?{
    val str = getConnectionsStr()
    str.let {
      try {
        return Gson().fromJson(str, Connections::class.java)
      } catch (e: Exception) {
        LOG.E(TAG,"Failed to parse: $str")
      }
    }
    return null
  }

  private fun getRouteStr(): String? {
    return getJsonDataFromAsset(ctx, "route_to_heelingtank_2.json")
  }

  private fun getPoisStr(): String?{
    return getJsonDataFromAsset(ctx, "pois_floor1&2.json")
  }

  private fun getConnectionsStr(): String?{
    return getJsonDataFromAsset(ctx, "connections.json")
  }

}

@Deprecated("")
data class TestMessages(
        @SerializedName("messages")
        val messagesList: List<ChatMsg>
)