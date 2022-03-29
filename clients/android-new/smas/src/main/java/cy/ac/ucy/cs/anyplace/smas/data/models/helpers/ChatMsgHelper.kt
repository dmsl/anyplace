package cy.ac.ucy.cs.anyplace.smas.data.models.helpers

import android.content.Context
import com.google.android.gms.maps.model.LatLng
import com.google.gson.Gson
import cy.ac.ucy.cs.anyplace.lib.android.cache.Cache
import cy.ac.ucy.cs.anyplace.smas.data.RepoChat
import cy.ac.ucy.cs.anyplace.smas.data.models.ChatMsg

/**
 * Extra functionality on top of the [ChatMsg] data class.
 * TODO: rename to data the encapsualting class of all helpers
 */
class ChatMsgHelper(val ctx: Context,
                    val repo: RepoChat,
                    val obj: ChatMsg) {

  override fun toString(): String = Gson().toJson(obj, ChatMsg::class.java)

  companion object {
    const val TP_TXT = 1
    const val TP_IMG= 2
    const val TP_LOCATION= 3
    const val TP_4= 4 // TODO:DZ was alert. now unused.

    const val STP_TXT = "txt"
    const val STP_IMG= "img"
    const val STP_LOCATION= "loc" // TODO Alert is better as a flag.
    const val STP_TP4= "tp4"

    fun parse(str: String): ChatMsg = Gson().fromJson(str, ChatMsg::class.java)
  }

  private val cache by lazy { Cache(ctx) }

  val prettyType: String
    get() {
      return when (obj.mtype) {
        TP_TXT -> STP_TXT
        TP_IMG -> STP_IMG
        TP_LOCATION ->  STP_LOCATION
        TP_4 -> STP_TP4
        else -> "UnknownType"
      }
    }

  val prettyTypeCapitalize: String
    get() { return prettyType.replaceFirstChar(Char::uppercase) }

  fun isText() : Boolean = obj.mtype == TP_TXT
  fun isAlert() : Boolean = obj.mtype == TP_LOCATION
  fun isImage() : Boolean = obj.mtype == TP_IMG

  fun latLng() : LatLng {
    val lat = obj.x
    val lon = obj.y
    return LatLng(lat, lon)
  }

}