import os, stat

def isWritable(dirname):
  uid = os.geteuid()
  gid = os.getegid()
  s = os.stat(dirname)
  mode = s[stat.ST_MODE]
  return (
     ((s[stat.ST_UID] == uid) and (mode & stat.S_IWUSR)) or
     ((s[stat.ST_GID] == gid) and (mode & stat.S_IWGRP)) or
     (mode & stat.S_IWOTH)
     )

def checkWritable(dirName):
    try:
        if not isWritable(dirName):
            die("Directory not writable: " + dirName)
    except FileNotFoundError as error:
            die("Error: " + str(error) +": "  + dirName)

def die(msg):
    print (msg)
    exit()
