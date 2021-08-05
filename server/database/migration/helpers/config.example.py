CDB_NAME = "localhost"
DIR_MIGRATION = "/data/db/migration-couchbase/" # temporary location for the migration

USERNAME="anyplace"
HOST="YOUR_HOSTNAME"

# COUCHBASE CONF:
CDB_PORT = "8091"
CDB_USERNAME = "anyplace"
CDB_PASSWORD = "enter_password"
CDB_BUCKET = "anyplace"
CDB_DOMAIN_NAME = "$USERNAME@$HOST"

# MONGODB CONF:
#MDB_ADMIN_PASSWORD = "admin_password" required only for initUser.py
# (which has some sample code and is not in use)
MDB_DATABASE = "anyplace"
MDB_USER = "anyplace"
MDB_PASSWORD = "new_db_password"

def getDocumentsPath():
    return DIR_MIGRATION + "/docs"

def getCollectionsPath():
    return getDocumentsPath() + "/collections"

def getFingerprintsPath():
    return getCollectionsPath() + "/buildings"