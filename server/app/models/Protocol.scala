package models

import play.api.libs.json.{Json, OWrites}

object Protocol {

  implicit val versionImplicitWrites: OWrites[Protocol.Version] = Json.writes[models.Protocol.Version]

  case class Version(version: String,
                     variant: String,
                     port: String,
                     address: String)

  case class LocalRegister(name: String,
                           email: String,
                           username: String,
                           password: String)

  case class LocalRegisterResponse(name: String,
                                   email: String,
                                   username: String,
                                   external: String,
                                   owner_id: String)

  case class LocalLogin(username: String, password: String)

  case class LocalLoginResponse(name: String,
                                email: String,
                                username: String,
                                external: String,
                                owner_id: String)

  case class SpaceAdd(name: String,
                      description: String,
                      url: String,
                      address: String,
                      coordinates_lat: String,
                      coordinates_lon: String,
                      space_type: String,
                      is_published: String)

  case class SpaceUpdate(buid: String,
                         name: String,
                         description: String,
                         url: String,
                         address: String,
                         coordinates_lat: String,
                         coordinates_lon: String,
                         space_type: String,
                         is_published: String)

  case class SpaceDelete(buid: String)

  case class SpaceCoords(coordinates_lat: String,
                         coordinates_lon: String)

  case class PoisAdd(name: String,
                     buid: String,
                     floor_name: String,
                     floor_number: String,
                     is_building_entrance: String,
                     is_door: String,
                     description: String,
                     coordinates_lat: String,
                     coordinates_lon: String,
                     pois_type: String,
                     is_published: String)

  case class PoisUpdate(buid: String,
                        puid: String,
                        name: String,
                        is_building_entrance: String,
                        is_door: String,
                        description: String,
                        coordinates_lat: String,
                        coordinates_lon: String,
                        pois_type: String,
                        is_published: String)

  case class PoisDelete(buid: String,
                        puid: String)

  case class FloorAll(buid: String,
                      floor_number: String)

  case class Buid(buid: String)

  case class PoisSearch(cuid: String,
                        letters: String,
                        buid: String,
                        greeklish: String)

  case class ConnectionAdd(is_published: String,
                           buid_a: String,
                           floor_a: String,
                           pois_a: String,
                           buid_b: String,
                           floor_b: String,
                           pois_b: String,
                           buid: String,
                           edge_type: String)

  case class ConnectionDelete(buid_a: String,
                              pois_a: String,
                              buid_b: String,
                              pois_b: String)

}