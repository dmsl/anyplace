from helpers.config import *
from helpers.defineCollections import *
import subprocess
from pathlib import Path

#  main
path = getDocumentsPath()
allDocs = path + "/all_docs.jsonrows"
Path(path).mkdir(parents=True, exist_ok=True)
URL = DOMAIN_NAME + ":" + PORT
print("Exporting documents from couchbase..")
subprocess.run(["/opt/couchbase/bin/cbexport", "json", "-c", URL, "-u", USERNAME, "-p", PASSWORD, "-b", BUCKET, "-f", "lines", "-o", allDocs])
print("Exporting documents from couchbase: Done")
print("Splitting documents..")
f = open(allDocs, encoding="utf8")
defineCollections(f)  # printing unique json keys with extra fields representing similar json keys
f.close()
print("Splitting documents: Done")
