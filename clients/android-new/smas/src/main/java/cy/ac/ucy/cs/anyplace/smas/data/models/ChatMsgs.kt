package cy.ac.ucy.cs.anyplace.smas.data.models

import com.google.gson.annotations.SerializedName

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
        /** TODO:DZ extension in case of image types */
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
data class ChatMsgsResp(
        @SerializedName("rows")
        val chatMsgs: List<ChatMsg>,
        @SerializedName("status")
        val status: String,
        @SerializedName("uid")
        val uid: String
)

/**
 * TODO [ChatMsg] Request model
 */
data class ChatMsgReq(
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
        @SerializedName("sessionkey")
        val sessionkey: String,
        @SerializedName("time")
        val time: String,
        @SerializedName("uid")
        val uid: String,
        @SerializedName("x")
        val x: Double,
        @SerializedName("y")
        val y: Double
)