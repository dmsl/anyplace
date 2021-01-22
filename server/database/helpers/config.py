CDB_NAME = "localhost"
DIR_MIGRATION = "/couchdb/migration"
DOMAIN_NAME = "anyplace@ap-dev.cs.ucy.ac.cy"
PORT = "8091"
USERNAME = "anyplace"
PASSWORD = "ENTER PASSWORD"
BUCKET = "anyplace"


def getDocumentsPath():
	return DIR_MIGRATION + "/docs"


def getCollectionsPath():
	return getDocumentsPath() + "/collections"
	
	
def getFingerprintsPath():
	return getCollectionsPath() + "/buildings"