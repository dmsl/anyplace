| Old name | New name |  Verify                                                                                                  |
| --- | --- | --- |
| BUILDINGS | |                                                                                                            |
| /anyplace/mapping/building/all                                        | /api/mapping/space/all   |              V         |
| /anyplace/mapping/building/get                                        | /api/mapping/space/get  |                        |
| /anyplace/mapping/building/add                                        | /api/auth/mapping/space/add |           V         |
| /anyplace/mapping/building/update                                     | /api/auth/mapping/space/update |        V         |
| /anyplace/mapping/building/delete                                     | /api/auth/mapping/space/delete   |       v        |
| /anyplace/mapping/building/all_owner                                  | /api/auth/mapping/space/user   |         V        |
| /anyplace/mapping/building/coordinates                                | /api/auth/mapping/space/coordinates |     V       |
| FLOORS | |                                                                                                               |
| /anyplace/mapping/floor/add                                           |  /api/auth/mapping/floor/add |                        |
| /anyplace/mapping/floor/delete                                        | /api/auth/mapping/floor/delete |                      |
| /anyplace/mapping/floor/uploadWithZoom                                |  /api/mapping/floor/uploadWithZoom|         |
| /anyplace/mapping/floor/all                                           | /api/mapping/floor/all |                    |
| CAMPUS | |                                                                                                               |
| /anyplace/mapping/campus/all_owner                                    | /api/auth/mapping/campus/user |                  |
| /anyplace/mapping/campus/add                                          | /api/auth/mapping/campus/add |                 |
| /anyplace/mapping/campus/update                                       | /api/auth/mapping/campus/update |                |
| /anyplace/mapping/campus/delete                                       | /api/auth/mapping/campus/delete |                |
| /anyplace/mapping/campus/all_cucode                                   | /api/mapping/campus/all_cucode |                 |
| POIS | |                                                                                                                 |
| /anyplace/mapping/pois/all_floor                                      | /api/mapping/pois/all_floor   |                  |
| /anyplace/mapping/pois/all_building                                   | /api/mapping/pois/all_building  |                |
| /anyplace/mapping/pois/all_pois                                       | /api/mapping/pois/search |                       |
| /anyplace/mapping/pois/add                                            | /api/auth/mapping/pois/add   |                   |
| /anyplace/mapping/pois/delete                                         | /api/auth/mapping/pois/delete |                  |
| /anyplace/mapping/pois/update                                         | /api/auth/mapping/pois/update |                  |
| CONNECTION | |                                                                                                           |
| /anyplace/mapping/connection/all_floor                                | /api/mapping/connection/all_floor  |             |
| /anyplace/mapping/connection/all_floors                               | /api/mapping/connection/all_floors  |            |
| /anyplace/mapping/connection/add                                      | /api/auth/mapping/connection/add |               |
| /anyplace/mapping/connection/delete                                   | /api/auth/mapping/connection/delete |            |
| NAVIGATION | |                                                                                                           |
| /anyplace/navigation/route_xy                                         |  |                                               |
| /anyplace/navigation/route                                            |  |                                               |
| /anyplace/navigation/building/id                                      |  |                                               |
| /anyplace/navigation/pois/id                                          |  |                                               |
| HEATMAPS | |                                                                                                             |
| /anyplace/position/predictFloorAlgo1                                  |  |                                               |
| /anyplace/position/estimate_position                                  |  |                                               |
| /anyplace/position/radio/delete                                       |  |                                               |
| /anyplace/position/radio_by_building_floor_all                        |  |                                               |
| /anyplace/position/radio_by_floor_bbox                                |  |                                               |
| /anyplace/position/radio_upload                                       |  |                                               |
| /anyplace/position/radio_download_floor                               |  |                                               |
| /anyplace/position/radio_by_building_floor                            |  |                                               |
| /anyplace/position/radio/APs_building_floor                           |  |                                               |
| /anyplace/position/radio/aps_ids                                      |  |                                               |
| /anyplace/position/radio/heatmap_building_floor_average_1             |  |                                               |
| /anyplace/position/radio/heatmap_building_floor_average_2             |  |                                               |
| /anyplace/position/radio/heatmap_building_floor_average_3             |  |                                               |
| /anyplace/position/radio/heatmap_building_floor_average_3_tiles       |  |                                               |
| /anyplace/position/radio/time                                         |  |                                               |
| /anyplace/position/radio/delete/time                                  |  |                                               |
| /anyplace/position/radio/heatmap_building_floor_timestamp_average_1   |  |                                               |
| /anyplace/position/radio/heatmap_building_floor_timestamp_average_2   |  |                                               |
| /anyplace/position/radio/heatmap_building_floor_timestamp_average_3   |  |                                               |
| /anyplace/position/radio/heatmap_building_floor_timestamp_tiles       |  |                                               |
| OTHERS | |                                                                                                               |
| /anyplace/debug/accounts_all                                     | /api/user/auth/admin/accounts_all |                   |
| /anyplace/version                                                     | /api/version   |                                 |
																														   