package cy.ac.ucy.cs.anyplace.smas.data.models

import com.google.gson.annotations.SerializedName

data class UserLocations(
    @SerializedName("rows")
    val rows: List<UserLocation>,
    @SerializedName("status")
    val status: String,
    @SerializedName("uid")
    val uid: String
) {
  data class UserLocation(
          @SerializedName("alert")
          val alert: Int,
          @SerializedName("buid")
          val buid: String,
          @SerializedName("deck")
          val deck: Int,
          @SerializedName("servertime")
          val servertime: String,
          @SerializedName("time")
          val time: Int,
          @SerializedName("timestr")
          val timestr: String,
          @SerializedName("uid")
          val uid: String,
          @SerializedName("x")
          val x: Double,
          @SerializedName("y")
          val y: Double
  )
}