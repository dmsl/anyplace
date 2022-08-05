# Anyplace

---
### A free and open Indoor Navigation Service with superb accuracy!
---

[![Join the chat at https://gitter.im/dmsl/anyplace](https://badges.gitter.im/dmsl/anyplace.svg)](https://gitter.im/dmsl/anyplace?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)


# 0. CLONE THE CODE:
<details><summary></summary>

### Cloning without the submodules:

```
git clone git@github.com:dmsl/anyplace.git anyplace
```

### Cloning with the submodules:
- Submodules are separate `git` repositories within this one
- You cal also fetch those at a later stage (with relevant git command)
- Needed when developing libraries or the android client apps.
- `core-lib`: [clients/core/lib]( clients/core/lib):
  - core library, written in kotlin
  - communicates to an Anyplace Backend service using `Retrofit2`
  - can be used to create more generic libraries
  - it is used by the `android-lib` to build the Android clients
- `android-lib`: [clients/android-new/lib-android](clients/android-new/lib-android):
  - most of the kotlin code is here
  - some thin clients are created out of this (SMAS, Navigator)

```
git clone git@github.com:dmsl/anyplace.git anyplace --recurse-submodules
```


</details>


# 1. Server: [PLAY Framework]
<details><summary></summary>

- This is the Anyplace Backend
- Latest Version 4.3.1 (MongoDB): See [ap.cs.ucy.ac.cy:44/api/version](https://ap.cs.ucy.ac.cy:44/api/version)
  - released as part of Anyplace 5.0 (Early 2022)
- For usage see: **Developers Front-end App** [ap.cs.ucy.ac.cy/developers](https://ap.cs.ucy.ac.cy/developers)
- Path: [./server](server)
- Branch: `develop-server`

</details>

# 2. Frontend apps:
<details><summary></summary>

##
- Path: 
- [./clients](clients)
- Branch: `develop-clients`

### Viewer: [ap.cs.ucy.ac.cy/viewer](https://ap.cs.ucy.ac.cy/viewer): Viewer
### Architect: [ap.cs.ucy.ac.cy/architect](https://ap.cs.ucy.ac.cy/architect): Architect
### Developers: [ap.cs.ucy.ac.cy/developers](https://ap.cs.ucy.ac.cy/developers): Developers (API through Swagger)

</details>

# 3. Preface 
<details open><summary>Preface</summary>

Anyplace is a first-of-a-kind indoor information service offering GPS-less
localization, navigation and search inside buildings using ordinary smartphones. 
	 
- URL: [anyplace.cs.ucy.ac.cy](https://anyplace.cs.ucy.ac.cy)

It is recommended to watch the [video tutorials](https://anyplace.cs.ucy.ac.cy/#how-works) before proceeding with these instructions.

We hope that you find our Anyplace Indoor Information Service useful for your research and innovation activities.  We would like to have feedback, comments, remarks, and, of course, any experience or test results from your own experimental setups. Currently, we can offer only limited support and assistance on the code, due to lack of resources, but we will try to get back to you as soon as possible. Questions and feedback may be sent to
anyplace@cs.ucy.ac.cy

If you install Anyplace on your own servers, please record your URL
[here](https://docs.google.com/spreadsheets/d/1GQySk4omlEcTPWoAt_Vt3WUmVbqFko4xoFKQ2N222RI/edit?usp=sharing).

#### In case you have any publications resulting from the Anyplace platform, please cite the following paper(s):

- [**The Anyplace 4.0 IoT Localization Architecture**](https://www.cs.ucy.ac.cy/~dzeina/papers/mdm20-a4iot.pdf)  
  **Paschalis Mpeis, Thierry Roussel, Manish Kumar, Constantinos Costa, Christos Laoudias, Denis Capot-Ray Demetrios Zeinalipour-Yazti**  
  _Proceedings of the 21st IEEE International Conference on Mobile Data Management (MDM '20), IEEE Computer Society, ISBN:, pp. 8, June 30 - July 3, 2020, Versailles, France, 2020_

- [**The Anatomy of the Anyplace Indoor Navigation Service**](http://www.sigspatial.org/sigspatial-special-issues/sigspatial-special-volume-9-number-2-july-2017/04-Paper01_Anatomy.pdf)  
  **Demetrios Zeinalipour-Yazti and Christos Laoudias**  
  _ACM SIGSPATIAL Special (SIGSPATIAL '17), ACM Press, Vol. 9, pp. 3-10, 2017_

- [**Internet-Based Indoor Navigation Services**](http://www.cs.ucy.ac.cy/~dzeina/papers/ic16-iin.pdf)  
  **Demetrios Zeinalipour-Yazti, Christos Laoudias, Kyriakos Georgiou, Georgios Chatzimilioudis**  
  _IEEE Internet Computing, vol. 21, no. , pp. 54-63, July 2017, doi:10.1109/MIC.2017.2911420_

- [**Anyplace: A Crowdsourced Indoor Information Service**](http://www.cs.ucy.ac.cy/~dzeina/papers/mdm15-anyplace-demo.pdf)  
  **Kyriakos Georgiou, Timotheos Constambeys, Christos Laoudias, Lambros Petrou, Georgios Chatzimilioudis and Demetrios Zeinalipour-Yazti**  
  _IEEE Mobile Data Management (MDM ’15), IEEE Press, Volume 2, Pages: 291-294, 2015_

</details>

# 4. Source Code Components 
<details open><summary></summary>

## 4.1 [Server](server):
- Play Framework server
- Written on scala
- Branch: `develop-server`

## 4.2 [Clients](clients):
- Branch: `develop-clients` (merging point of android and web apps)
- submodule: [core-lib](clients/core/lib)
- [Android](clients/android-new/)  Branch: `develop-clients-android`
  - submodule: [lib-android](clients/android-new/lib-android)
- [Web apps](clients/web): Branch: `develop-clients-web`
  - [Architect](clients/web/anyplace_architect)
  - [Viewer](clients/web/anyplace_viewer)
  - [Viewer Campus](clients/web/anyplace_viewer_campus)
- [Simulator](clients/simulator)
- Other:
  - [iOS](clients/deprecated/ios/)
  - [Windows Phone](clients/deprecated/windows-phone/)
  - [RobotOS](clients/robotos/)
  - [Linux](clients/linux/)
  - [macOS](clients/macos/)

</details>

## 5. LATEST DEVELOPMENT VERSION
To test the latest development version you can fork the [develop branch](https://github.com/dmsl/anyplace/tree/develop).

DEV Testing: [ap-dev.cs.ucy.ac.cy](https://ap-dev.cs.ucy.ac.cy)

---

# Contributors: 
- University of Cyprus (Cyprus)
- University of Pittsburgh (USA)
- University of Mannheim (Germany)
- Alstom (France)
- Infosys (India)

---

# Links

## [Contributing](CONTRIBUTING.md)

## [Team](https://anyplace.cs.ucy.ac.cy/#about)

## [License](LICENSE.txt)
