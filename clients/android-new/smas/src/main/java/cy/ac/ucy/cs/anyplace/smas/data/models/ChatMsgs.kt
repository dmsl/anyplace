package cy.ac.ucy.cs.anyplace.smas.data.models

import com.google.gson.annotations.SerializedName

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
        val time: Int,
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
data class MsgGetResp(
        @SerializedName("status")
        val status: String,
        @SerializedName("descr")
        val descr: String?,
        @SerializedName("uid")
        val uid: String,

        /** List of messages received*/
        @SerializedName("rows")
        val chatMsgs: List<ChatMsg>,
)

data class MsgGetReq(
        val uid: String,
        val sessionkey: String,
        /** 0 is always used */
        val mgettype: Int) {
  constructor(user: ChatUser, mgettype: Int) : this(user.uid, user.sessionkey, mgettype)
}

/**
 * TODO [ChatMsg] Request model
 *
 * TODO:ATH MsgSendReq ?
 */
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
        val mdelivery: Int,
        @SerializedName("msg")
        val msg: String,
        @SerializedName("mtype")
        val mtype: Int,

        @SerializedName("time")
        val time: String,

        @SerializedName("x")
        val x: Double,
        @SerializedName("y")
        val y: Double
)