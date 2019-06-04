/*
 *
 *
 */
package db_models

import java.io.IOException
import java.util
import java.util.HashMap
import utils.LPUtils

import com.couchbase.client.java.document.json.JsonObject

class AccessPoint(hm: HashMap[String, String]) extends AbstractModel {

    private var json: JsonObject = _

    this.fields = hm

    def this() {
        this(new util.HashMap[String, String]())
        fields.put("ssid", "")
        fields.put("mac", "")
        fields.put("buid", "")
        fields.put("floor", "")
        fields.put("apid", "")
        fields.put("whitelisted", "false")
        fields.put("frequency", "")
        fields.put("capabilities", "")
        fields.put("channelWidth", "")
    }

     def this(json: JsonObject) {
        this()
        fields.put("ssid", json.getString("ssid"))
        fields.put("mac", json.getString("mac"))
        fields.put("buid", json.getString("buid"))
        fields.put("floor", json.getString("floor"))
        fields.put("whitelisted", json.getString("whitelisted"))
        fields.put("apid", json.getString("apid"))
        fields.put("frequency", json.getString("frequency"))
        fields.put("capabilities", json.getString("capabilities"))
        fields.put("channelWidth", json.getString("channelWidth"))
    }

    def this(ssid: String , mac: String = "", buid: String, floor: String, whitelisted: Boolean = false) {
        this()
        fields.put("ssid", ssid)
        fields.put("mac", mac)
        fields.put("buid", buid)
        fields.put("floor", floor)
        fields.put("whitelisted", whitelisted.toString)
        fields.put("frequency", "0")
        fields.put("capabilities", "NA")
        fields.put("channelWidth", "NA")
        this.json = json
    }

    def getId(): String = {
        var apid: String = fields.get("apid")
        if (apid == null || apid.isEmpty || apid == "") {
          val finalId = LPUtils.getRandomUUID + "_" + System.currentTimeMillis()
          fields.put("apid", "apid_" + finalId)
          apid = fields.get("apid")
        }
        apid
    }

    def updateWhitelisted(whitelisted: Boolean): String = {
        println("Updating whitelisted with val " + whitelisted)
        val sb = new StringBuilder()
        var json = toValidCouchJson()
        try {
            this.fields.put("whitelisted", whitelisted.toString)
            json.put("whitelisted", whitelisted.toString)
        } catch {
            case e: IOException => e.printStackTrace()
        }
        sb.append(json.toString)
        sb.toString
    }


    def updateAdditionalAttributes(frequency: String, channelWidth: String, capabilities: String) = {
        this.fields.put("frequency", frequency.toString)
        this.fields.put("capabilities", capabilities)
        this.fields.put("channelWidth", channelWidth)
        var json = toValidCouchJson()
        json.put("frequency", frequency.toString)
        json.put("capabilities", capabilities)
        json.put("channelWidth", channelWidth)
    }


    def toValidCouchJson(): JsonObject = {
        JsonObject.from(this.getFields())
    }

    def toCouchGeoJSON(): String = {
     ""
    }

    override def toString(): String = toValidCouchJson().toString
}
