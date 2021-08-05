from helpers.config import *
import os
import subprocess

# NOT IN USE.
# Sample script for automatically creating anyplace user
cmd = "printf", "use admin\ndb.createUser({user: \"" + MDB_USER + "\",pwd: \"" + MDB_PASSWORD + "\",roles: [{ role: \"dbOwner\",db: \"" + MDB_DATABASE + "\" } ]})"
printable = subprocess.Popen((cmd), stdout=subprocess.PIPE) 
output = subprocess.check_output(('mongo', '-u', 'admin', '-p', MDB_ADMIN_PASSWORD , '--authenticationDatabase', 'admin'), stdin=printable.stdout)
printable.wait()
