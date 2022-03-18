package cy.ac.ucy.cs.anyplace.smas.data.models

import com.google.gson.annotations.SerializedName
import cy.ac.ucy.cs.anyplace.lib.models.UserLocation

data class UserLocations(
        @SerializedName("rows")
    val rows: List<UserLocation>,
        @SerializedName("status")
    val status: String,
        @SerializedName("uid")
    val uid: String
)
