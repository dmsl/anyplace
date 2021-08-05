#!/usr/bin/env python3

from helpers.config import *
from helpers.defineCollections import *
import subprocess
from pathlib import Path

# Used only for testing. It creates just the collection folders.
# It used a cached copy of the pulled documents directly.
# you must edit the imports of defineCollections for this to run

filename=DIR_MIGRATION+"/docs/all_docs.jsonrows"
fAllDocs = open(filename, encoding="utf8")
defineCollections(fAllDocs)
