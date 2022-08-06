# Anyplace Server:
The codebase has been rewritten to the latest versions of:
- Play: `2.8.x`
- Scala: `2.13.x`
- sbt: `1.5.x`
- MongoDB: `4.3.x`

All endpoints have been completely reimplemented to `MongoDB`.

***

The Version 4.0 is available as a prebuilt with docker (on the `/develop` branch).
Docker is not supported for 4.2+.

***

# 1. [Install MongoDB](./database/README.md)

# 2. [Deploy Server](./deploy/README.md)

# 3. [Setup IDE (IntelliJ)](./SETUP_IDE.md) 

# 4. Connect Android app
<details>
<summary>
Android Client Details
</summary>

###### Connecting the Anyplace Android Client
4.1. Download the Android Client from the Play Store: [com.dmsl.anyplace (old)](https://play.google.com/store/apps/details?id=com.dmsl.anyplace&hl=en)

4.2. Under settings in the Android App, change the DNS of the Anyplace server to your own server IP/DNS.

***
#### NOTES:

- Optional: Change more client settings:
  - You can download, modify, and recompile the Android client.
  - Requires an Android Developer Account.
+ An SSL certificate is **mandatory** for allocing the Android client to connect to your server.
+ Source code: [../clients](../clients/)

</details>