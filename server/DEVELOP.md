# Deploy a development instance

Idea:
1. code for the backend locally
2. sync sources to remote, compile them, and view from browser.

# Run on different port:
sbt "run 9001"


# Troubleshooting: 
- `Can't connect to X11 window server using ':0' as the value of the DISPLAY variable.`

 Try:
`unset DISPLAY`


# keystore

```
keytool -genkey -alias MyKey -keyalg RSA -keysize 2048 -keystore keystore.jks
play -Dhttps.port=9443 -Dhttps.keyStore=keystore.jks -Dhttps.keyStorePassword=password run
```

add dummy value to this
