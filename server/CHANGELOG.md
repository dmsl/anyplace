
# Version 4.2
<details open>
<summary>
Details..
</summary>

This release contain major code base updates.
Large portion of the code base was rewritten to the latest version of the below:
- Play: `2.8.8`(latest release, April'21)
- Scala: `2.13.x`
- sbt: `1.5.x`

Also most of the endpoints are now in MondoDB, and Couchbase is anticipated to
be completely removed in the coming weeks.

<details open>
<summary>
New features
</summary>

- Fingerprint cache-collections:
  Instead of recalculating and waste resources we are caching fingerprint groupping. There are in total 3 zoom levels and the possibility to select a date span to see fingerprints, thus there are in total 6 collections. Those are heatmaps1,2,3 and heatmapTimestamp1,2,3.
  - on heatmap/Wifi-Coverage request, depends on the zoom level the heatmap cache collections are created for the requested floor.
  - on heatmap-timestamp request all zoom levels are created for the requested floor.
  - on delete all cached objects that are related to that floor are deleted.
  - on update all cached objects that are related to that floor are deleted.
  - on fingerprint insertion cache collections are NOT affected.
  - on space/floor deletion, cache collections are also deleted.
- Fingerprints groupping:
  + level 1: Approximately 3 meters
  + level 2: 1.11 meters
  + level 3: No groupping
- New login/register feature:
  - Register
  In the past users could only use the system with Google Accounts. Now they can create Anyplace accounts and interact with the system through them.
  - Login
  User provides username and password in order to login.
- Changed how API key is generated and authenticates
  Since the former way of interacting with Anyplace was with Google we were using Google API-key.
  Now we generate our own API-keys and authenticate the endpoints with them.
- Added space type
  Buildings are renamed to Spaces. There are two types of spaces:
  + building
  + vessel
- Rebuilding /developers with swagger-ui
  New api interface using swagger.  Still on develop stage.

</details>

<details>
<summary>
Bug fixes
</summary>

- Crossfilter:
  The feature where a user could see fingerprints on a time span was not working.
  - Reset button was also fixed.
  The feature is now working properly and supports:
  - Wi-Fi coverage
  - Heatmaps
</details>

<details>
<summary>
Known Bugs
</summary>

- On space deletion directory path/radiomaps_frozen/building/floor remained
- Show access points feature shows wrong location
- Problematic features when logged with local account:
  + Update space
  + Add campus
  + Update campus
  + Search pois

</details>

<details>
<summary>
Endpoints renaming
</summary>

- /admin/accounts_all						-> 	/user/admin/accounts_all
- /anyplace/version 						->  /api/version
- /anyplace/mapping/building/add        	->	/anyplace/mapping/space/add 
- /anyplace/mapping/building/update     	->	/anyplace/mapping/space/update 
- /anyplace/mapping/building/delete     	->	/anyplace/mapping/space/delete 
- /anyplace/mapping/building/all        	->	/anyplace/mapping/space/all 
- /anyplace/mapping/building/all_owner  	->	/anyplace/mapping/space/all_owner
- /anyplace/mapping/building/coordinates	->	/anyplace/mapping/space/coordinates
- /anyplace/mapping/building/get 			->	/anyplace/mapping/space/get

</details>

</details>