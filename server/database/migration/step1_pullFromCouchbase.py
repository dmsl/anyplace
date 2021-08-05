#!/usr/bin/env python3

from helpers.config import *
from helpers.defineCollections import *
import subprocess
from pathlib import Path

###
# Pulls all CouchbaseDB documents  in a file that has a json object on each row.
# Then it calls defineCollections, which breaks down those lines into different folders (for the collections).
###

#  main
path = getDocumentsPath()
allDocs = path + "/all_docs.jsonrows"
Path(path).mkdir(parents=True, exist_ok=True)

checkTools(CBEXPORT)

URL = CDB_DOMAIN_NAME + ":" + CDB_PORT
print("Exporting documents:")
print("  - Couchbase: " + URL)
print("  - Export dir: " + DIR_MIGRATION)

subprocess.run(["/opt/couchbase/bin/cbexport", "json", "-c", URL, "-u", CDB_USERNAME, "-p", CDB_PASSWORD, "-b", CDB_BUCKET, "-f", "lines", "-o", allDocs])
print("Exporting documents from couchbase: DONE\n\n")

print("Splitting documents..")
fAllDocs = open(allDocs, encoding="utf8")
defineCollections(fAllDocs)  # printing unique json keys with extra fields representing similar json keys
subprocess.run(["/opt/couchbase/bin/cbexport", "json", "-c", URL, "-u", CDB_USERNAME, "-p", CDB_PASSWORD, "-b", CDB_BUCKET, "-f", "lines", "-o", allDocs])
print("Exporting documents from couchbase: DONE\n\n")

print("Splitting documents..")
fAllDocs = open(allDocs, encoding="utf8")
defineCollections(fAllDocs)  # printing unique json keys with extra fields representing similar json keys
fAllDocs.close()
print("Splitting documents: Done")
