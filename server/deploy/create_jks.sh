#!/bin/bash

passpath=PASSWORD.jks
pempath=PATH_TO_CERTIFICATES
domain=DOMAIN_ADDRESS # e.g.: ap.cs.ucy.ac.cy
# JKS key will be needed to start play framework
jksname=PATH_TO_STORE_JKS_KEY


###
keyalias="play"
machine=`hostname`
pass=`cat $passpath`

certPem="$cert_name.pem"
pkcsname="$cert_name.p12"
name="$machine.$domain"
chainPem="$cert_name.cer.chain.pem" # PEM + CHAIN

if [ -f $pempath/$pkcsname ]; then
    rm -f $pempath/$pkcsname
fi

if [ -f $pempath/$jksname ]; then
    rm -f $pempath/$jksname
fi


# Create .p12 keystore (conversion into pkcs12 format)
openssl pkcs12 -export -passout pass:$pass -in $pempath/$certPem -inkey $pempath/private.key -out $pempath/$pkcsname -name $keyalias

# Create JKS
keytool -importkeystore -deststorepass $pass -destkeypass $pass \
	-destkeystore  $pempath/$jksname \
	-srckeystore $pempath/$pkcsname \
	-srcstoretype PKCS12 -srcstorepass $pass \
	-alias $keyalias

# Make the JSK certificate trusted
keytool -import -noprompt -trustcacerts \
	-deststorepass $pass -destkeypass $pass \
	-alias $name -file $pempath/$chainPem \
	-keystore $pempath/$jksname

chown USER:USER $pempath/$jksname
echo "Created: $pempath/$jksname"
