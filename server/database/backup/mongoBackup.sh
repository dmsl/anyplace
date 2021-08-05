#!/bin/bash
cwd="$(dirname "$0")"
source $cwd/config.sh

#timestamp=$(date +'%Y.%m.%d_%H:%M:%S')
timestamp=$(date +'%H:%M_%d.%m.%Y')
backupFilename=$BACKUPDIR/backup.$timestamp
backupLatest=$BACKUPDIR/backup.latest

if [ -d $backupFilename ]; then
  echo "Backup was already taken at this timestamp: $backupFilename"
  exit 0
fi

function createBackup() {
  backupFile=$1

  mongodump --host localhost --port 27017 --db $MDB_DATABASE --authenticationDatabase admin --username $MDB_USER --password $MDB_PASS --out $backupFile
  # fake backup
  # mkdir $backupFile # for testing…
}
mkdir -p $BACKUPDIR/

if [ -L $backupLatest ]; then
  rm $backupLatest
fi

echo "Backing up: $backupFilename"
createBackup $backupFilename
ln -s $backupFilename $backupLatest

totalFiles=$(ls $BACKUPDIR -l | grep -v ^l | grep -v "total"| wc -l)
if [ $totalFiles -gt $((MAX_BACKUPS)) ]; then
  numBackups=$(($totalFiles + 0))
  maxBackups=$(($MAX_BACKUPS + 0))
  ls -tp $BACKUPDIR | grep -v ^l
  deleteNum=$(expr $numBackups - $maxBackups)
  ls -tp $BACKUPDIR | grep -v ^l  | tail -n $deleteNum
  toDelete=$(ls -tp $BACKUPDIR | grep -v ^l | tail -n $deleteNum)
  echo $toDelete
  for f in $toDelete;
  do
    folder=$BACKUPDIR/$f
    if [ ! -z $f ] && [ -d $folder ]; then
      echo "Deleting: $folder"
      rm -rf $folder
    fi
  done
fi