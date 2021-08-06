#!/bin/bash
cwd="$(dirname "$0")"
source $cwd/helper.sh

backupDir=$BACKUP_DIR
host=$MDB_HOST
port=$MDB_PORT
user=$MDB_USER
pass=$MDB_PASS
database=$MDB_DATABASE

#timestamp=$(date +'%Y.%m.%d-%H.%M.%S')
timestamp=$(date +'%Y.%m.%d-%H.%M')
backup=$backupDir/backup.$timestamp
backupTar=$backup.tar.gz
backupLatest=$backupDir/backup.latest

checkBackupTaken $backup

backupPrepare $backupDir $backupLatest

createBackup $backup

deleteOldBackups $backupDir
