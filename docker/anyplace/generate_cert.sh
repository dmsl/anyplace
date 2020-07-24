#!/bin/bash
############################################
# Generate private certificate for HTTPS/SSL
############################################

echo "SSL Encryption (HTTPS certificate):"

cache=/opt/cache/cert/
out=/opt/cert/

if [ -d  $cache ]; then
    mv $cache $out
    echo "SSL Encryption: will use cached certificate!" 
    exit 0
fi

name=$1
DOMAIN=$2
OrganizationalUnit=$3
Organization=$4
Locality=$5
StateOrProvinceName=$6
CountryName=$6
###
URL=$name.$DOMAIN

rm -f $out
mkdir -p $out

_CN=$name
_OU=$OrganizationalUnit
_O=$Organization
_L=$Locality
_S=$StateOrProvinceName
_C=$CountryName

echo "#################################"
echo "Generating HTTPS/SSL certificates"
echo "Hostname: $_CN"
echo "Domain: $DOMAIN"
echo "Organizational Unit: $_OU"
echo "Organization: $_O"
echo "Locality: $_L"
echo "State or Province: $_S"
echo "Country: $_C"
echo "URL : $URL"
echo "#################################"
echo -e "\n\n"

_PW=`pwgen -Bs 10 1`
echo $_PW > $out/password

export PW=$_PW

########
# Generating a server CA: this will sign domain specific certificate
########
echo "1. Generating: $out/$name.jks"
keytool -genkeypair -v \
  -alias $name"ca" \
  -dname "CN=$_CN, OU=$_OU, O=$_O, L=$_L, ST=$_S, C=$_C" \
  -keystore $out/$name.jks \
  -keypass:env PW \
  -storepass:env PW \
  -keyalg RSA \
  -keysize 4096 \
  -ext KeyUsage:critical="keyCertSign" \
  -ext BasicConstraints:critical="ca:true" \
  -validity 9999

# Export the exampleCA public certificate as exampleca.crt so that it can be used in trust stores.
echo "2. Generating: $out/$name.crt"
keytool -export -v \
  -alias $name"ca" \
  -file $out/$name.crt \
  -keypass:env PW \
  -storepass:env PW \
  -keystore $out/$name.jks \
  -rfc

########
# Create a server certificate, tied to URL
########
echo "3. Generating: $out/$URL.jks"
keytool -genkeypair -v \
  -alias $URL \
  -dname "CN=$URL, OU=$_OU, O=$_O, L=$_L, ST=$_S, C=$_C" \
  -keystore $out/$URL.jks \
  -keypass:env PW \
  -storepass:env PW \
  -keyalg RSA \
  -keysize 2048 \
  -validity 385

# Create a certificate signing request for URL
echo "4. Generating: $out/$URL.csr"
keytool -certreq -v \
  -alias $URL \
  -keypass:env PW \
  -storepass:env PW \
  -keystore $out/$URL.jks \
  -file $out/$URL.csr

# Tell exampleCA to sign the URL certificate. Note the extension is on the request, not the
# original certificate.
# Technically, keyUsage should be digitalSignature for DHE or ECDHE, keyEncipherment for RSA.
echo "5. Sign: $out/$URL.jks"
keytool -gencert -v \
  -alias $name"ca" \
  -keypass:env PW \
  -storepass:env PW \
  -keystore $out/$name".jks" \
  -infile $out/$URL.csr \
  -outfile $out/$URL.crt \
  -ext KeyUsage:critical="digitalSignature,keyEncipherment" \
  -ext EKU="serverAuth" \
  -ext SAN="DNS:$URL" \
  -rfc

# Tell example.com.jks it can trust exampleca as a signer.
echo "6. Trust : $out/$URL.crt"
keytool -import -v \
  -alias $name"ca" \
  -file $out/$name".crt" \
  -keystore $out/$URL.jks \
  -storetype JKS \
  -storepass:env PW << EOF
yes
EOF

# Import the signed certificate back into example.com.jks 
echo "7. Import signed certificate to: $out/$URL.jks"
keytool -import -v \
  -alias $URL \
  -file $out/$URL.crt \
  -keystore $out/$URL.jks \
  -storetype JKS \
  -storepass:env PW

# List out the contents of example.com.jks just to confirm it.  
# If you are using Play as a TLS termination point, this is the key store you should present as the server.
echo "8. Verifying: $out/$URL.jks"
keytool -list -v \
  -keystore $out/$URL.jks \
  -storepass:env PW
