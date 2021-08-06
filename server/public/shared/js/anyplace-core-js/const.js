var MIN_ZOOM_FOR_HEATMAPS = 19;
var MAX_ZOOM_FOR_HEATMAPS = 21;
var _MAX_ZOOM_LEVEL = 22;
// var DEFAULT_MAP_TILES = "OSM";
var DEFAULT_MAP_TILES = "CartoLight";

// MESSAGES
//// Error messages
ERR_FETCH_BUILDINGS="Something went wrong while fetching buildings.";
ERR_FETCH_ALL_FLOORS="Something went wrong while fetching all floors.";
ERR_USER_AUTH="Could not authorize user. Please refresh.";
ERR_FETCH_FINGERPRINTS="Something went wrong while fetching fingerprints.";
ERR_GEOLOC_DEVICE_SETTINGS="Please enable location access.";
ERR_GEOLOC_NET_OR_SATELLITES="Position unavailable. The network is down or the positioning satellites couldn't be contacted.";
ERR_GEOLOC_TIMEOUT="Timeout. The request for retrieving your Geolocation was timed out.";
ERR_GEOLOC_UNKNOWN="There was an error while retrieving your Geolocation. Please try again.";
ERR_GEOLOC_NOT_SUPPORTED="The Geolocation feature is not supported by this browser.";

WARN_NO_FINGERPRINTS="This floor seems not to be FingerPrint mapped. Download the Anyplace app from the Google Play store to map the floor.";
WARN_ACCES_REMOVED="ACCES map removed.";