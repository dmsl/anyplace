# Administration of MongoDB:

### Backup MongoDB:

For the administration scripts copy `config.example.sh` to `config.sh` and adapt as necessary.

##### mongo.sh:
Just a wrapper based on`config.sh`.

##### backup.sh:
Backups all collections using `mongodump`.
Once a backup is taken, it keeps the last `MAX_BACKUPS`.
The latest one has a symlink to `backup.latest`.

##### restore.sh:
Restores all collections using `mongorestore`.
You may optionally modify the script to use the `--drop` flag,
for clearing everything in the database before restoring.

You can supply different restore mongodb server and database than the one used during backup.

The backup name to restore is optional.
If not given it uses the `backup.latest`.
