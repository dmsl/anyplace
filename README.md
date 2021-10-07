# Anyplace

---
### A free and open Indoor Navigation Service with superb accuracy!
---

[![Join the chat at https://gitter.im/dmsl/anyplace](https://badges.gitter.im/dmsl/anyplace.svg)](https://gitter.im/dmsl/anyplace?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)


## 0. Public
Latest Version 4.2.5 (MongoDB):
- https://ap.cs.ucy.ac.cy/viewer/ 
- https://ap.cs.ucy.ac.cy/architect
- https://ap.cs.ucy.ac.cy/developers
- Android: Currently not compatible as the endpoint structure has changed. A new Android Client to be released as part of Anyplace 5.0 (Early 2022)

Deprecated Version 4.0.1 (Couchbase *** STALE DATA (not transferred to latest version ****)
- https://ap.cs.ucy.ac.cy:9443/viewer
- https://ap.cs.ucy.ac.cy:9443/architect
- https://ap.cs.ucy.ac.cy:9443/developers
- Android: Please insert the above IP/PORT in the settings to connect to the Deprecated service)

## 1. Clone
To include the submodules (`anyplace-lib`, and `anyplace-lib-android`) please clone using:
```
git clone git@github.com:dmsl/anyplace.git anyplace --recurse-submodules
```

Fetching those git submodules can be also done at a later stage.
These submodules are needed when developing on the libraries or the client apps.

## 2. Preface 

<details open>
<summary>
Preface
</summary>

Anyplace is a first-of-a-kind indoor information service offering GPS-less
localization, navigation and search inside buildings using ordinary smartphones. 
	 
- URL: [anyplace.cs.ucy.ac.cy](https://anyplace.cs.ucy.ac.cy)

It is recommended to watch the [video tutorials](https://anyplace.cs.ucy.ac.cy/#how-works) before proceeding with these instructions.

We hope that you will find our Anyplace Indoor Information Service useful for your research and innovation activities.  We would like to have feedback, comments, remarks, and, of course, any experience or test results from your own experimental setups. Currently, we can offer only limited support and assistance on the code, due to lack of resources, but we will try to get back to you as soon as possible. Questions and feedback may be sent to
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

# Components 

<details open>
<summary>
Server
</summary>

## 3. [Server](server):

#### 3.1 [Viewer](https://anyplace.cs.ucy.ac.cy/viewer/)

#### 3.2 [Architect](https://anyplace.cs.ucy.ac.cy/architect/)

#### 3.3 [API](https://anyplace.cs.ucy.ac.cy/developers/)
</details>

<details open>
<summary>
Clients
</summary>

## 4. [Clients](clients):
- [Android](clients/android/)
- [iOS](clients/deprecated/ios/)
- [Windows Phone](clients/deprecated/windows-phone/)
- [RobotOS](clients/robotos/)
- [Linux](clients/linux/)
- [macOS](clients/macos/)
- [Simulator](clients/simulator)

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
