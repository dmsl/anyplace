# Remote Deploy during develop:

Workflow:
1. Write code locally
2. Sync code to remote for compilation, and view the result from the browser

See [deploy](./deploy)

## Run on different port:
`sbt "run 9001"`

## Troubleshooting:
- `Can't connect to X11 window server using ':0' as the value of the DISPLAY variable.`

Try:
`unset DISPLAY`

## keystore
```
keytool -genkey -alias MyKey -keyalg RSA -keysize 2048 -keystore keystore.jks
play -Dhttps.port=9443 -Dhttps.keyStore=keystore.jks -Dhttps.keyStorePassword=password run
```
add dummy value to this
