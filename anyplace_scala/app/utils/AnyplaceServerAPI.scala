package utils

import play.Play
import java.io.File
//remove if not needed
import scala.collection.JavaConversions._

object AnyplaceServerAPI {

    val SERVER_ADDRESS = Play.application().configuration().getString("server.address")

    val SERVER_PORT = "80"

    val SERVER_FULL_URL = SERVER_ADDRESS + ":" + SERVER_PORT

    val SERVER_API_ROOT = SERVER_FULL_URL + File.separatorChar + "anyplace" + File.separatorChar

    val ANDROID_API_ROOT = SERVER_FULL_URL + File.separatorChar + "android" + File.separatorChar
}
