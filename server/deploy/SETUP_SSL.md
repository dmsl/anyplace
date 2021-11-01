# 1. SETUP SSL CERTIFICATE:
Guide for setting up a certificate from an external authority
These are sample commands for Ubuntu18 to get your started.
It might be better to follow online official guides as these might not work or become outdated.

A certificate must be generated first with the relevant authority.

## 1.1 Example
```bash
openssl req -nodes -newkey rsa:4096 -keyout private.key -out req.csr
# Country Name (2 letter code) [GB]:CY
# State or Province Name (full name) [Berkshire]:Nicosia
# Locality Name (eg, city) [Newbury]:Aglantzia
# Organization Name (eg, company) [My Company Ltd]:University of Cyprus
# Organizational Unit Name (eg, section) []: Department of Computer Science
# Common Name (eg, your name or your server's hostname) []: <DOMAIN.cs.ucy.ac.cy>
# Email Address []: <EMAIL>
# Please enter the following 'extra' attributes
# to be sent with your certificate request
# A challenge password []:
# An optional company name []:
```

Then the `private.key` has to be securely saved and the `req.csr` to be
forwarded to the relevant providers to get your certificate.

The next steps assume that a certificate was issued, and verified:

## 1.2 Example Verify
```bash
openssl x509 -noout -modulus -in <FILE>.cer | openssl md5
openssl rsa -noout -modulus -in private.key | openssl md5
```

## 2.1 Play Framework


#### 2.1.1 Generate JSK
Use the `create_jks.sh`

This creates a .p12 (which is a key + pem), and then uses this one, plus the chain pem
(from the certificates) to generate and copy the JKS file.

Then use generated the JKS to launch the `Anyplace Backend` service.


## 3.1 HaProxy

### Copy certificate to haproxy:
It requires the below files must be combined into a .pem:
- `CERT.crt`
- `CERT.pem`
- `private.key`


```bash
cat CERT.crt CERT.pem private.key > tmp.pem
sudo mv tmp.pem /etc/haproxy/certs/ap.pem
chmod 600 /etc/haproxy/certs/ap.pem
```

Then use `ap.pem` in HaProxy configuration.
Make sure to verify that it can be correctly read using:
```bash
haproxy -f /etc/haproxy/haproxy.cfg -c

```
## 4.1 Uninstall certbot (Optional/Cleanup):
In case you are moving out of certbot, you might want to cleanup your environment (especially if apache plugins were installed).

```bash
sudo certbot delete
sudo apt purge python-certbot-apache
```

