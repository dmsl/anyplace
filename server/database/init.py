from helpers.config import *
import os
import subprocess

cmd = "printf", "use admin\ndb.createUser({user: \"" + MDB_USER + "\",pwd: \"" + MDB_PASSWORD + "\",roles: [{ role: \"dbOwner\",db: \"" + MDB_DATABASE + "\" } ]})"
printable = subprocess.Popen((cmd), stdout=subprocess.PIPE) 
output = subprocess.check_output(('mongo', '-u', 'admin', '-p', MDB_ADMIN_PASSWORD , '--authenticationDatabase', 'admin'), stdin=printable.stdout)
printable.wait()

#cmd = "use admin\n"+"db.createUser({user: \"" + MDB_USER + "\",pwd: \"" + MDB_PASSWORD + "\",roles: [{ role: \"dbOwner\",db: \"" + MDB_DATABASE + "\" } ]})"
#query=subprocess.check_output(('mongo', '-u', 'admin', '-p', MDB_ADMIN_PASSWORD , '--authenticationDatabase', 'admin'), stdin=cmd)
#query.wait()
#if len(query.stderr) > 0: # not sure about the stderr
#    print("ERROR: " + query.stderr)
#print(query.stdout)
