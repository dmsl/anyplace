| Old name | New name |  Verify                                                                                                  |
| --- | --- | --- |
| USER | |                                                                                                               |
| /anyplace/debug/accounts_all                                     | /api/auth/admin/user/all |                   |
| BUILDINGS | |                                                                                                            |
| /anyplace/mapping/building/all                                        | /api/mapping/space/all   |              V         |
| /anyplace/mapping/building/get                                        | /api/mapping/space/get  |                        |
| /anyplace/mapping/building/add                                        | /api/auth/mapping/space/add |           V         |
| /anyplace/mapping/building/update                                     | /api/auth/mapping/space/update |        V         |
| /anyplace/mapping/building/delete                                     | /api/auth/mapping/space/delete   |       v        |
| /anyplace/mapping/building/all_owner                                  | /api/auth/mapping/space/user   |         V        |
| /anyplace/mapping/building/coordinates                                | /api/auth/mapping/space/coordinates |     V       |
| FLOORS | |                                                                                                               |
| /anyplace/mapping/floor/add                                           |  /api/auth/mapping/floor/add |          V              |
| /anyplace/mapping/floor/delete                                        | /api/auth/mapping/floor/delete |        V              |
| /anyplace/mapping/floor/uploadWithZoom                                |  /api/mapping/floor/floorplan/upload|    V     |
| /anyplace/mapping/floor/all                                           | /api/mapping/floor/all |           V         |
| FLOORPLAN | | |
| /api/floorplans64all/:buid/:floor_number | /api/floorplans64/all/:buid/:floor_number | |
| CAMPUS | |                                                                                                               |
| /anyplace/mapping/campus/all_owner                                    | /api/auth/mapping/campus/user |        V          |
| /anyplace/mapping/campus/add                                          | /api/auth/mapping/campus/add |          V       |
| /anyplace/mapping/campus/update                                       | /api/auth/mapping/campus/update |        V        |
| /anyplace/mapping/campus/delete                                       | /api/auth/mapping/campus/delete |      V          |
| /anyplace/mapping/campus/all_cucode                                   | /api/mapping/campus/get |        V         |
| POIS | |                                                                                                                 |
| /anyplace/mapping/pois/all_floor                                      | /api/mapping/pois/floor/all   |                  |
| /anyplace/mapping/pois/all_building                                   | /api/mapping/pois/space/all  |                |
| /anyplace/mapping/pois/all_pois                                       | /api/mapping/pois/search |           V           |
| /anyplace/mapping/pois/add                                            | /api/auth/mapping/pois/add   |                   |
| /anyplace/mapping/pois/delete                                         | /api/auth/mapping/pois/delete |                  |
| /anyplace/mapping/pois/update                                         | /api/auth/mapping/pois/update |                  |
| CONNECTION | |                                                                                                           |
| /anyplace/mapping/connection/all_floor                                | /api/mapping/connection/all_floor  |             |
| /anyplace/mapping/connection/all_floors                               | /api/mapping/connection/all_floors  |            |
| /anyplace/mapping/connection/add                                      | /api/auth/mapping/connection/add |               |
| /anyplace/mapping/connection/delete                                   | /api/auth/mapping/connection/delete |            |
| NAVIGATION | |                                                                                                           |
| /anyplace/navigation/route_xy                                         | /api/navigation/route/coordinates  |      V         |
| /anyplace/navigation/route                                            | /api/navigation/route |         V       |
| /anyplace/navigation/building/id                                      | /api/navigation/space/id |       V          |
| /anyplace/navigation/pois/id                                          | /api/navigation/pois/id |      V           |
| POSITION | |                                                                                                             |
| /anyplace/position/predictFloorAlgo1                                  |  |                  |
| /anyplace/position/estimate_position                                  |  |          |
| /anyplace/position/radio/delete                                       | /api/radiomap/delete |        |
| /anyplace/position/radio_by_building_floor_all                        | /api/radiomap/floor/all |      |
| /anyplace/position/radio_by_floor_bbox                                | /api/radiomap/floor/bbox |    |
| /anyplace/position/radio_upload                                       | /api/radiomap/upload |       |
| /anyplace/position/radio_download_floor                               | /api/radiomap/floor |       |
| /anyplace/position/radio_by_building_floor                            | /api/radiomap/space |         |
| /anyplace/position/radio/time                                         | /api/radiomap/time |                       |
| /anyplace/position/radio/delete/time                                  | /api/radiomap/delete/time |                 |
| ACCESS POINTS |||
| /anyplace/position/radio/APs_building_floor                           | /api/wifi/access_points/floor |        |
| /anyplace/position/radio/aps_ids                                      | /api/wifi/access_points/ids |        |
| HEATMAPS |||
| /anyplace/position/radio/heatmap_building_floor_average_1             | /api/heatmap/floor/average/1 |       V      |
| /anyplace/position/radio/heatmap_building_floor_average_2             | /api/heatmap/floor/average/2 |        V            |
| /anyplace/position/radio/heatmap_building_floor_average_3             | /api/heatmap/floor/average/3 |            V               |
| /anyplace/position/radio/heatmap_building_floor_average_3_tiles       | /api/heatmap/floor/average/3/tiles |        V                |
| /anyplace/position/radio/heatmap_building_floor_timestamp_average_1   | /api/heatmap/floor/average/timestamp/1 |         V          |
| /anyplace/position/radio/heatmap_building_floor_timestamp_average_2   | /api/heatmap/floor/average/timestamp/2 |      V          |
| /anyplace/position/radio/heatmap_building_floor_timestamp_average_3   | /api/heatmap/floor/average/timestamp/3 |     V     |
| /anyplace/position/radio/heatmap_building_floor_timestamp_tiles       | /api/heatmap/floor/average/timestamp/tiles |      V      |
| OTHERS | |                                                                                                               |
| /anyplace/version                                                     | /api/version   |                                 |

																														   



