CDB_NAME = "localhost"
DIR_MIGRATION = "/data/db/migration/" # temporary location for the migration

# COUCHBASE CONF:
CDB_DOMAIN_NAME = "USERNAME@HOST"
CDB_PORT = "8091"
CDB_USERNAME = "anyplace"
CDB_PASSWORD = "CDB_PASSWORD"
CDB_BUCKET = "anyplace"

CBEXPORT = "/opt/couchbase/bin/cbexport"

# MONGODB CONF:
#MDB_ADMIN_PASSWORD = "admin_password" required only for initUser.py
# (which has some sample code and is not in use)
MDB_DATABASE = "anyplace"
MDB_USER = "anyplace"
MDB_PASSWORD = "MDB_PASSWORD"

from helpers.helper import *
backupDirNotFoundMsg="Please create the backup directory and give relevant permissions."
checkWritable(DIR_MIGRATION, backupDirNotFoundMsg)
checkTools(CBEXPORT)

def getDocumentsPath():
    return DIR_MIGRATION + "/docs"

def getCollectionsPath():
    return getDocumentsPath() + "/collections"

def getFingerprintsPath():
    return getCollectionsPath() + "/buildings"