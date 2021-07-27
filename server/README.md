# Anyplace Server:
The codebase has been rewritten to the latest versions of:
- Play: `2.8.x`
- Scala: `2.13.x`
- sbt: `1.5.x`

The endpoints/database have been completely reimplemented to MongoDB.

***

The Version 4.0 is available as a prebuilt with docker (/develop branch).
Docker is not supported for 4.2+.

***

# 1. [DEPLOY Server](./DEPLOY.md)

# 2. [SETUP IntelliJ](./SETUP.md) 

# 3. Connect the Android Client
##### Connecting the Anyplace Android Client
+ Download the Android Client from the Play Store: https://play.google.com/store/apps/details?id=com.dmsl.anyplace&hl=en (https://play.google.com/store/apps/details?id=com.dmsl.anyplace&hl=en)
+ Under settings in the Android App, change the DNS of the Anyplace server to your own server IP/DNS.
+ (Optional) Download and recompile the Android client  and apply your default settings. (Note: Requires a seperate Android Developer Account.
+ IMPORTANT: You have to install an SSL certificate on your server to allow the Android Client to connect to your server.

[Read More..](../clients/)