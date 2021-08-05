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
    if not isWritable(dirName):
        print ("Directory not writable: " + dirName)
        exit()