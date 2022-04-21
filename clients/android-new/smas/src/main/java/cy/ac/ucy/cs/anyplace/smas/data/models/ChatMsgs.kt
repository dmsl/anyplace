package cy.ac.ucy.cs.anyplace.smas.data.models

import com.google.gson.annotations.SerializedName
import cy.ac.ucy.cs.anyplace.lib.models.UserCoordinates
import cy.ac.ucy.cs.anyplace.smas.data.models.helpers.ChatMsgHelper

/** A Single [ChatMsg] */
data class ChatMsg(
        @SerializedName("mid")
        val mid: String,

        /** From [ChatUser] */
        @SerializedName("uid")
        val uid: String,

        /** Delivery outreach:
         * - 1: delivered to all
         * - 2: on same deck
         * - 3: KNN
         * - 4: bounding box on location
         */
        @SerializedName("mdelivery")
        val mdelivery: Int,

        @SerializedName("mtype")
        val mtype: Int,
        @SerializedName("msg")
        val msg: String?,

        /** Extension in case of image types */
        @SerializedName("mexten")
        val mexten: String,

        /** Server timestamp */
        @SerializedName("time")
        val time: Long,
        /** Server pretty time */
        @SerializedName("timestr")
        val timestr: String,

        // Location
        /** Latitude */
        @SerializedName("x")
        val x: Double,
        /** Longitude */
        @SerializedName("y")
        val y: Double,
        //// Space
        @SerializedName("buid")
        val buid: String,
        @SerializedName("deck")
        val deck: Int,
)

/**
 * [ChatMsg] Response
 */
data class ChatMsgsResp(
        @SerializedName("status")
        val status: String?,
        @SerializedName("descr")
        val descr: String?,
        @SerializedName("uid")
        val uid: String?,

        /** List of messages received*/
        @SerializedName("rows")
        val msgs: List<ChatMsg>,
)

data class MsgGetReq(
        val uid: String,
        val sessionkey: String,
        val mgettype: Int = ChatMsgHelper.TP_GET_ALL,
        val from: String? ="") {
  constructor(user: ChatUser, mgettype: Int = ChatMsgHelper.TP_GET_ALL, from: String?=null)
          : this(user.uid, user.sessionkey, mgettype, from)
}

data class MsgSendReq(
        // [ChatUser]
        @SerializedName("uid")
        val uid: String,
        @SerializedName("sessionkey")
        val sessionkey: String,

        @SerializedName("buid")
        val buid: String,
        @SerializedName("deck")
        val deck: Int,
        @SerializedName("mdelivery")
        val mdelivery: String,
        @SerializedName("msg")
        val msg: String?,
        @SerializedName("mtype")
        val mtype: Int,
        @SerializedName("mexten")
        val mexten: String?,
        @SerializedName("time")
        val time: String,

        @SerializedName("x")
        val x: Double,
        @SerializedName("y")
        val y: Double
){
  constructor(user: ChatUser, userCoords: UserCoordinates, mdelivery: String, msg: String?, mtype: Int, mexten: String?, time: String) :
          this(user.uid, user.sessionkey, userCoords.buid, userCoords.level, mdelivery, msg, mtype, mexten, time, userCoords.lat, userCoords.lon)
}

data class MsgSendResp(
        @SerializedName("status")
        val status: String,
        @SerializedName("descr")
        val descr: String?,
        @SerializedName("uid")
        val uid: String,
        @SerializedName("rows")
        val rows: Int?
)

/**
 * Used for ReplyTo's (which are not fully implemented)
 */
data class ReplyToMessage(
        val sender : String,
        val message : String?,
        val attachment : String?
)
