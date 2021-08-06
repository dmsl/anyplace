#!/bin/bash
cwd="$(dirname "$0")"
source $cwd/helper.sh

backupDir=$BACKUP_DIR

host=$RESTORE_MDB_HOST
port=$RESTORE_MDB_PORT
user=$RESTORE_MDB_USER
pass=$RESTORE_MDB_PASS
restoreToDatabase=$RESTORE_MDB_DATABASE

backupLatest=$backupDir/backup.latest

if [ $# -eq 0 ]; then
  backup=$backupLatest
elif [ $# -eq 1 ]; then
  backup=$backupDir/$1
else
 echo "Usage: $0 [backupFilename]"
 echo "backupFilename: optional. If not given it uses the latest"
 exit 1
fi

backupTmp=$backupDir/tmp

checkBackupExists $backup

untarBackup $backup $backupTmp

renameRestoreDatabase $backupData  $restoreToDatabase

restoreBackup $backupData $backupTmp
