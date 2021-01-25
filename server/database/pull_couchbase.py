from helpers.config import *
from helpers.defineCollections import *
import subprocess
from pathlib import Path

#  main
path = getDocumentsPath()
allDocs = path + "/all_docs.jsonrows"
Path(path).mkdir(parents=True, exist_ok=True)
URL = CDB_DOMAIN_NAME + ":" + CDB_PORT
print("Exporting documents from couchbase..")
subprocess.run(["/opt/couchbase/bin/cbexport", "json", "-c", URL, "-u", CDB_USERNAME, "-p", CDB_PASSWORD, "-b", CDB_BUCKET, "-f", "lines", "-o", allDocs])
print("Exporting documents from couchbase: Done")
print("Splitting documents..")
f = open(allDocs, encoding="utf8")
defineCollections(f)  # printing unique json keys with extra fields representing similar json keys
f.close()
print("Splitting documents: Done")
