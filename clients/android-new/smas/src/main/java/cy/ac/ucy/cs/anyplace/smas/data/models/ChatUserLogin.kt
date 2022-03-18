package cy.ac.ucy.cs.anyplace.smas.data.models

import com.google.gson.annotations.SerializedName

data class ChatUser(
        @SerializedName("uid")
        val uid: String,
        @SerializedName("sessionid")
        val sessionid: String,
        )

data class ChatLoginReq(
        val uid: String,
        val password: String)

data class ChatLoginResp(
        @SerializedName("descr")
        val descr: String?,
        @SerializedName("sessionid")
        val sessionid: String,
        @SerializedName("status")
        val status: String,
        @SerializedName("uid")
        val uid: String
)