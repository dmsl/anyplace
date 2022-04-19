package cy.ac.ucy.cs.anyplace.smas.data.db.entities

import androidx.room.TypeConverter
import com.google.android.gms.maps.model.LatLng
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import cy.ac.ucy.cs.anyplace.smas.data.models.ChatMsg
import cy.ac.ucy.cs.anyplace.smas.data.models.ChatMsgsResp
import cy.ac.ucy.cs.anyplace.smas.data.models.helpers.ChatMsgHelper

class DatabaseConverters {

  companion object {
    fun chatMsgtoEntity(msg: ChatMsg): ChatMsgEntity {
      // skip saving base64 on SQLite.
      // Those will be stored in [SmasCache] (file cache)
      val content = if (msg.mtype == ChatMsgHelper.TP_IMG) " " else msg.msg
      return ChatMsgEntity(
              msg.mid,
              msg.uid,
              msg.mdelivery, msg.mtype,
              content,
              msg.mexten,
              msg.time, msg.timestr,
              msg.x, msg.y, msg.buid, msg.deck)
    }


    fun entityToChatMsg(tuple: ChatMsgEntity): ChatMsg {
      return ChatMsg(
              tuple.mid,
              tuple.uid,
              tuple.mdelivery, tuple.mtype,
              tuple.msg,
              tuple.mexten,
              tuple.time, tuple.timestr,
              tuple.x, tuple.y, tuple.buid, tuple.deck)
    }


    /**
     * Converts chat tuples to a list of messages
     */
    fun entityToChatMessages(tuples: List<ChatMsgEntity>): ChatMsgsResp {
      val spaces = mutableListOf<ChatMsg>()
      tuples.forEach { tuple ->
        spaces.add(entityToChatMsg(tuple))
      }
      return ChatMsgsResp(null, "msgs read locally", null, spaces)
    }
  }

  val gson = Gson() // Kotlin Serialization?
  @TypeConverter
  fun ltnLngToString(ltnLng: LatLng) : String {
    return gson.toJson(ltnLng)
  }

  @TypeConverter
  fun stringToLtnLng(data: String)  : LatLng {
    val listType = object : TypeToken<LatLng>() {}.type
    return gson.fromJson(data, listType)
  }

}
