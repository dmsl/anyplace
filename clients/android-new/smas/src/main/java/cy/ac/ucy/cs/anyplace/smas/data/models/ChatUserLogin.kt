package cy.ac.ucy.cs.anyplace.smas.data.models

import com.google.gson.annotations.SerializedName

data class ChatUserLoginResp(
    @SerializedName("descr")
    val descr: String?,
    @SerializedName("sessionid")
    val sessionid: String,
    @SerializedName("status")
    val status: String,
    @SerializedName("uid")
    val uid: String
)

data class ChatUser(
        @SerializedName("uid")
        val uid: String,
        @SerializedName("sessionid")
        val sessionid: String,
        )

data class ChatUserLoginForm(
        val uid: String,
        val password: String)
