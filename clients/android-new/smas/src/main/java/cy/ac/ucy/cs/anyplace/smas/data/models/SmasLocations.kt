package cy.ac.ucy.cs.anyplace.smas.data.models

import com.google.gson.annotations.SerializedName
import cy.ac.ucy.cs.anyplace.lib.models.UserLocation

data class UserLocations(
        @SerializedName("uid")
        val uid: String,
        @SerializedName("status")
        val status: String,
        @SerializedName("descr")
        val descr: String?,
        @SerializedName("rows")
        val rows: List<UserLocation>,
)