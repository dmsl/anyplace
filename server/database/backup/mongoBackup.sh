#!/bin/bash
MAX_BACKUPS=5
BACKUPDIR="./mongodb/backups/"
timestamp=$(date +'%H:%M_%d.%m.%Y')
backupFilename=${BACKUPDIR}backup.${timestamp}
backupLatest=${BACKUPDIR}backup.latest

function createBackup() {
  backupFile=$1
  mongodump --host localhost --port 27017 --db anyplace  --authenticationDatabase admin --username anyplace --password t2Jqwd6r5k88DGpD --out ${backupFile}
  touch $backupFile # for testing…
}


mkdir -p $BACKUPDIR/

if [ -L $backupLatest ]; then
  rm $backupLatest
fi

createBackup $backupFilename
ln -s $backupFilename $backupLatest

totalFiles=$(ls ${BACKUPDIR} -l | grep -v ^l | grep -v "total"| wc -l)
if [ $totalFiles -gt $((MAX_BACKUPS)) ]; then
  toDelete=$(ls -tp ${BACKUPDIR} | grep -v ^l | tail -n +7)
  echo $toDelete
fi