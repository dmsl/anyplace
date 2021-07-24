package models

object Protocol {
  case class Version(version: String,
                     variant: String,
                     port: String,
                     address: String)
}