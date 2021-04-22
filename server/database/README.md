# Migration guide from CouchBase to mongoDB


The migration_0.py script is trying to fetch from database the fingerprintWifi according to 'buid'. If not found then its starting to adding fingerprints of that buid. The reason is in case the script dies while adding fingerprints the next time you try to add them its going to continue where it left. Therefore even all fingerprints are in database the script will fetch them to make sure that everything is okay.
TODO pull

# Testing
	
This file `anyplace.postman_collection.json` can be imported to postman. 
Not all endpoints are included
