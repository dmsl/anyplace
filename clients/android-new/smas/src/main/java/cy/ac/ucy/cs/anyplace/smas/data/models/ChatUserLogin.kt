package cy.ac.ucy.cs.anyplace.smas.data.models

import com.google.gson.annotations.SerializedName

data class ChatUser(
        val uid: String,
        val sessionkey: String,
        )

data class ChatLoginReq(
        val uid: String,
        val password: String)

data class ChatLoginResp(
        @SerializedName("descr")
        val descr: String?,
        // TODO:DZ: sessionid
        @SerializedName("sessionid")
        val sessionkey: String,
        @SerializedName("status")
        val status: String,
        @SerializedName("uid")
        val uid: String
)