package models

import play.api.libs.json.{Json, OWrites}

/**
 * Can be used with swagger (iheartradio) definitions.
 * Currently it is not used as we use the conf/swagger.yml to keep code clean.
 */
object Protocol {

  implicit val versionImplicitWrites: OWrites[Protocol.Version] = Json.writes[models.Protocol.Version]

  case class Version(version: String,
                     variant: String,
                     port: String,
                     address: String)
}