# Anyplace v4

[![Join the chat at https://gitter.im/dmsl/anyplace](https://badges.gitter.im/dmsl/anyplace.svg)](https://gitter.im/dmsl/anyplace?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)

A free and open Indoor Navigation Service with superb accuracy!

## Preface 
Anyplace is a first-of-a-kind indoor information service offering GPS-less
localization, navigation and search inside buildings using ordinary smartphones. 
	 
- URL: [anyplace.cs.ucy.ac.cy](https://anyplace.cs.ucy.ac.cy)

It is recommended to watch the video tutorials of the Anyplace system on
[anyplace.cs.ucy.ac.cy](https://anyplace.cs.ucy.ac.cy), before proceeding with these instructions.

We hope that you find our Anyplace Indoor Information Service useful for your research and innovation activities.  We would like to have feedback, comments, remarks, and, of course, any experience or test results from your own experimental setups. Currently, we can offer only limited support and assistance on the code, due to lack of resources, but we will try to get back to you as soon as possible. Questions and feedback may be sent to
anyplace@cs.ucy.ac.cy

#### In case you have any publications resulting from the Anyplace platform, please cite the following paper(s):

- [The Anatomy of the Anyplace Indoor Navigation Service**](http://www.sigspatial.org/sigspatial-special-issues/sigspatial-special-volume-9-number-2-july-2017/04-Paper01_Anatomy.pdf)  
  **Demetrios Zeinalipour-Yazti and Christos Laoudias**  
  _ACM SIGSPATIAL Special (SIGSPATIAL '17), ACM Press, Vol. 9, pp. 3-10, 201_

- [Internet-Based Indoor Navigation Services](http://www.cs.ucy.ac.cy/~dzeina/papers/ic16-iin.pdf)  
  **Demetrios Zeinalipour-Yazti, Christos Laoudias, Kyriakos Georgiou, Georgios Chatzimilioudis**  
  _IEEE Internet Computing, vol. 21, no. , pp. 54-63, July 2017, doi:10.1109/MIC.2017.2911420_

- [Anyplace: A Crowdsourced Indoor Information Service](http://www.cs.ucy.ac.cy/~dzeina/papers/mdm15-anyplace-demo.pdf)  
  **Kyriakos Georgiou, Timotheos Constambeys, Christos Laoudias, Lambros Petrou, Georgios Chatzimilioudis and Demetrios Zeinalipour-Yazti**  
  _IEEE Mobile Data Management (MDM ’15), IEEE Press, Volume 2, Pages: 291-294, 2015_


## Contributors: 
- University of Cyprus (Cyprus)
- Alstom (France)
- Infosys (India)
- University of Pittsburgh (USA)
- University of Mannheim (Germany)

# Components 

## A. Server:

The server entails all components to run the anyplace service on your own server. 

- Executable (Binary): Visit our Github releases page or visit the link:
    https://anyplace.cs.ucy.ac.cy/downloads/anyplace_v4.zip
- Source Code (including IntelliJ IDE SBT file) : [master.zip](https://github.com/dmsl/anyplace/archive/master.zip)
- Instructions: [server](server)
- Current Leaders: Constantinos Costa, Paschalis Mpeis, Kyriakos Georgiou

#### Server: Viewer
The source code of Anyplace HTML5/CSS3 Viewer Client (IP localization). 
- Try: [Viewer](https://anyplace.cs.ucy.ac.cy/viewer/)
- Documentation/Source: [server/public/anyplace_viewer](server/public/anyplace_viewer)

#### Server: Architect:
The source code of Anyplace HTML5/CSS3 Architect Tool. 
- Try: [Architect](https://anyplace.cs.ucy.ac.cy/architect/)
- Documentation/Source: [server/public/anyplace_architect](server/public/anyplace_architect)

#### Server: API:
The source code of Anyplace JSON API.  
- Try: [API](https://anyplace.cs.ucy.ac.cy/developers/)
- Documentation/Source: [server/public/anyplace_developers](server/public/anyplace_developers)

#### Server: Data Store:
To setup Anyplace with Couchbase follow the general server instructions here:
- Instructions: [server](server)
- Couchbase: [Views](server/anyplace_views)

## B. Clients
Various anyplace clients:
- [Android](clients/android/)
- [ios](clients/deprecated/ios/)
- [windows-phone](clients/deprecated/windows-phone/)
- [RobotOS]clients/robotos/)
- [Linux]clients/linux/)
- [macOS]clients/macos/)

## C. Simulator

## LATEST DEVELOPMENT VERSION
To test the latest development version you can fork the [DEVELOP branch](https://github.com/dmsl/anyplace/tree/develop)

# Links
## [Team](https://anyplace.cs.ucy.ac.cy/#about)

---
## [License](LICENSE.txt)
