#!/bin/bash
USERNAME=""
PASSWORD=""
curl -X PUT -H "Content-Type: application/json" http://localhost:8092/anyplace/_design/dev_accounts -d @accounts.json -u $USERNAME:$PASSWORD

curl -X PUT -H "Content-Type: application/json" http://localhost:8092/anyplace/_design/dev_admin -d @admin.json -u $USERNAME:$PASSWORD

curl -X PUT -H "Content-Type: application/json" http://localhost:8092/anyplace/_design/dev_campus -d @accounts.json -u $USERNAME:$PASSWORD

curl -X PUT -H "Content-Type: application/json" http://localhost:8092/anyplace/_design/dev_floor -d @floor.json -u $USERNAME:$PASSWORD

curl -X PUT -H "Content-Type: application/json" http://localhost:8092/anyplace/_design/dev_magnetic -d @magnetic.json -u $USERNAME:$PASSWORD

curl -X PUT -H "Content-Type: application/json" http://localhost:8092/anyplace/_design/dev_nav -d @nav.json -u $USERNAME:$PASSWORD

curl -X PUT -H "Content-Type: application/json" http://localhost:8092/anyplace/_design/dev_radio -d @radio.json -u $USERNAME:$PASSWORD

curl -X PUT -H "Content-Type: application/json" http://localhost:8092/anyplace/_design/dev_radio_spatial -d @spatial_radio.json -u $USERNAME:$PASSWORD

curl -X PUT -H "Content-Type: application/json" http://localhost:8092/anyplace/_design/dev_nav_spatial -d @spatial_nav.json -u $USERNAME:$PASSWORD

##########
# PUBLISH
##########

curl -X PUT -H "Content-Type: application/json" http://localhost:8092/anyplace/_design/accounts -d @accounts.json -u $USERNAME:$PASSWORD

curl -X PUT -H "Content-Type: application/json" http://localhost:8092/anyplace/_design/admin -d @admin.json -u $USERNAME:$PASSWORD

curl -X PUT -H "Content-Type: application/json" http://localhost:8092/anyplace/_design/campus -d @accounts.json -u $USERNAME:$PASSWORD

curl -X PUT -H "Content-Type: application/json" http://localhost:8092/anyplace/_design/floor -d @floor.json -u $USERNAME:$PASSWORD

curl -X PUT -H "Content-Type: application/json" http://localhost:8092/anyplace/_design/magnetic -d @magnetic.json -u $USERNAME:$PASSWORD

curl -X PUT -H "Content-Type: application/json" http://localhost:8092/anyplace/_design/nav -d @nav.json -u $USERNAME:$PASSWORD

curl -X PUT -H "Content-Type: application/json" http://localhost:8092/anyplace/_design/radio -d @radio.json -u $USERNAME:$PASSWORD

curl -X PUT -H "Content-Type: application/json" http://localhost:8092/anyplace/_design/radio_spatial -d @spatial_radio.json -u $USERNAME:$PASSWORD

curl -X PUT -H "Content-Type: application/json" http://localhost:8092/anyplace/_design/nav_spatial -d @spatial_nav.json -u $USERNAME:$PASSWORD
