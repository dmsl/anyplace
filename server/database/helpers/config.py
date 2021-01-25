CDB_NAME = "localhost"
DIR_MIGRATION = "/couchdb/migration"
CDB_DOMAIN_NAME = "anyplace@ap-dev.cs.ucy.ac.cy"
CDB_PORT = "8091"
CDB_USERNAME = "anyplace"
CDB_PASSWORD = "ENTER PASSWORD"
CDB_BUCKET = "anyplace"


def getDocumentsPath():
	return DIR_MIGRATION + "/docs"


def getCollectionsPath():
	return getDocumentsPath() + "/collections"
	
	
def getFingerprintsPath():
	return getCollectionsPath() + "/buildings"