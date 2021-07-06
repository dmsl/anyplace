from defineCollections import *
fAllDocs = open("/couchdb/migration/docs/all_docs.jsonrows", encoding="utf8")
defineCollections(fAllDocs)
