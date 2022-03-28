package cy.ac.ucy.cs.anyplace.smas.ui.tmp_models

import com.google.gson.annotations.SerializedName
import cy.ac.ucy.cs.anyplace.smas.data.models.ChatMsg

data class Messages(
    @SerializedName("messages")
    val messagesList: List<ChatMsg>
)