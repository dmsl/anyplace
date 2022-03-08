package cy.ac.ucy.cs.anyplace.smas.data.models

import com.google.gson.annotations.SerializedName

data class ChatVersion(
    @SerializedName("rows")
    val rows: Rows,
    @SerializedName("status")
    val status: String) {
  data class Rows(
          @SerializedName("time_zone")
          val timeZone: String,
          @SerializedName("url")
          val url: String,
          @SerializedName("version")
          val version: String
  )
}

