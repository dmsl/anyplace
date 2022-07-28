var MIN_ZOOM_FOR_HEATMAPS = 19;
var MAX_ZOOM_FOR_HEATMAPS = 21;
var _MAX_ZOOM_LEVEL = 22;
// var DEFAULT_MAP_TILES = "OSM";
var DEFAULT_MAP_TILES = "CartoLight";

// MESSAGES
//// Error messages
ERR_FETCH_BUILDINGS="Error while fetching buildings";
ERR_FETCH_ALL_FLOORS="Error while fetching floors";
ERR_FETCH_ALL_FLOORPLANS="Error while fetching floorplans";
ERR_FETCH_ALL_RADIOMAPS="Error while fetching radiomaps";
ERR_USER_AUTH="Could not authorize user. Please refresh";
ERR_FETCH_FINGERPRINTS="Error while fetching fingerprints";
ERR_GEOLOC_DEVICE_SETTINGS="Please enable location access";
ERR_GEOLOC_NET_OR_SATELLITES="Position unavailable. The network is down or the positioning satellites couldn't be contacted.";
ERR_GEOLOC_TIMEOUT="Timeout on the geolocation request.";
ERR_GEOLOC_UNKNOWN="Error while geolocating. Please try again.";
ERR_GEOLOC_NOT_SUPPORTED="Geolocation not supported by the browser.";

WARN_NO_FINGERPRINTS="This floor seems not to be FingerPrint mapped. Download the Anyplace app from the Google Play store to map the floor.";
WARN_ACCES_REMOVED="ACCES map removed.";