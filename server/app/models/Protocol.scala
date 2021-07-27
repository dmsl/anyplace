package models

import play.api.libs.json.{Json, OWrites}

object Protocol {

  implicit val versionImplicitWrites: OWrites[Protocol.Version] = Json.writes[models.Protocol.Version]
  case class Version(version: String,
                     variant: String,
                     port: String,
                     address: String)
}