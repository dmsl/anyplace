package cy.ac.ucy.cs.anyplace.smas.data.models

import com.google.gson.annotations.SerializedName
import cy.ac.ucy.cs.anyplace.lib.models.UserCoordinates

/** Request for sending [ChatUser]'s location */
data class LocationSendReq(
        // Authentication:
        @SerializedName("uid")
        val uid: String,
        @SerializedName("sessionkey")
        val sessionkey: String,

        /** User is in alert-mode */
        @SerializedName("alert")
        val alert: Int,

        // [UserCoordinates]
        /** Space ID*/
        @SerializedName("buid")
        val buid: String,
        @SerializedName("deck")
        val deck: Int,
        /** Latitude */
        @SerializedName("x")
        val x: Double,
        /** Longitude */
        @SerializedName("y")
        val y: Double,

        @SerializedName("time")
        val time: String,
) {
  // TODO:DZ: why location-send needs time? (and why string?)
  constructor(user: ChatUser, alert: Int, uc: UserCoordinates, time: String):
          this(user.uid, user.sessionkey, alert,
          uc.buid, uc.level, uc.lat, uc.lon, time)
}

/** Response for sending [ChatUser]'s location */
data class LocationSendResp(
        @SerializedName("rows")
        val rows: Int,
        @SerializedName("status")
        val status: String,
        @SerializedName("uid")
        val uid: String,
        @SerializedName("descr")
        val descr: String?
)