# TODO: replace config.py with .env
CDB_NAME = "localhost"
DIR_MIGRATION = "/couchdb/migration"
CDB_DOMAIN_NAME = "anyplace@ap-dev.cs.ucy.ac.cy"
CDB_PORT = "8091"
CDB_USERNAME = "anyplace"
CDB_PASSWORD = "enter_password"
CDB_BUCKET = "anyplace"
MDB_USER = "anyplace"
MDB_PASSWORD = "new_db_password"
MDB_DATABASE = "anyplace"
MDB_ADMIN_PASSWORD = "admin_password"

def getDocumentsPath():
        return DIR_MIGRATION + "/docs"


def getCollectionsPath():
        return getDocumentsPath() + "/collections"


def getFingerprintsPath():
        return getCollectionsPath() + "/buildings"