# AnyPlace

[![Join the chat at https://gitter.im/dmsl/anyplace](https://badges.gitter.im/dmsl/anyplace.svg)](https://gitter.im/dmsl/anyplace?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)

A free and open Indoor Navigation Service with superb accuracy!

## Preface 
Anyplace is a first-of-a-kind indoor information service offering GPS-less
localization, navigation and search inside buildings using ordinary smartphones.
	 
- URL: http://anyplace.cs.ucy.ac.cy

It is recommended to watch the video tutorials of the Anyplace system on http://anyplace.cs.ucy.ac.cy/, before proceeding with these instructions.

We hope that you find our Anyplace Indoor Information Service useful for your research and innovation activities.  We would like to have feedback, comments, remarks, and, of course, any experience or test results from your own experimental setups. Currently, we can offer only limited support and assistance on the code, due to lack of resources, but we will try to get back to you as soon as possible. Questions and feedback may be sent to anyplace@cs.ucy.ac.cy

In case you have any publications resulting from the Anyplace platform, please cite the following paper(s):

- "Internet-based Indoor Navigation Services", Demetrios Zeinalipour-Yazti, Christos Laoudias, Kyriakos Georgiou and Georgios Chatzimiloudis IEEE Internet Computing (IC '16), IEEE Press, Volume xx, Pages: xx-xx, 2016. Download: http://www.cs.ucy.ac.cy/~dzeina/papers/ic16-iin.pdf

- “Anyplace: A Crowdsourced Indoor Information Service”, Kyriakos Georgiou, Timotheos Constambeys, Christos Laoudias, Lambros Petrou, Georgios Chatzimilioudis and Demetrios Zeinalipour-Yazti, Proceedings of the 16th IEEE International Conference on Mobile Data Management (MDM ’15), IEEE Press, Volume 2, Pages: 291-294, 2015. Download: http://www.cs.ucy.ac.cy/~dzeina/papers/mdm15-anyplace-demo.pdf

Enjoy Anyplace!

The Anyplace Team 
	 
Copyright (c) 2015, Data Management Systems Lab (DMSL), Department of Computer Science
University of Cyprus.

All rights reserved.

## MIT Open Source Licence

Permission is hereby granted, free of charge, to any person obtaining a copy of
this software and associated documentation files (the “Software”), to deal in the
Software without restriction, including without limitation the rights to use, copy,
modify, merge, publish, distribute, sublicense, and/or sell copies of the Software,
and to permit persons to whom the Software is furnished to do so, subject to the
following conditions:
	 
The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.
	 
THE SOFTWARE IS PROVIDED “AS IS”, WITHOUT WARRANTY OF ANY KIND, EXPRESS
OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
DEALINGS IN THE SOFTWARE.
	
## Components 

Short description of the contents included in this release.

### Android
The Source code for the Anyplace Android Logger and Navigator (WiFi/IMU/IP Localization). Current Leader: Timotheos Constambeys. 
- Try: https://play.google.com/store/apps/details?id=com.dmsl.anyplace
- Documentation/Source: https://github.com/dmsl/anyplace/tree/master/android
- Current Leader: Timotheos Constambeys

### Windows
The Source code of the Anyplace Windows Phone Client (IP localization). 
- Try: https://www.microsoft.com/en-us/store/apps/anyplace/9nblgggzldsk
- Documentation/Source: https://github.com/dmsl/anyplace/tree/master/windows
- Current Leader: Pangiotis Irakleous

### iOS
The Source code of the Anyplace iOS Phone Client (Magnetic/IMU/IP localization). 
- Try: coming soon
- Current Leader: Nikitin Artem.

### Viewer
The source code of Anyplace HTML5/CSS3 Viewer Client (IP localization). 
- Try: https://anyplace.cs.ucy.ac.cy/viewer/
- Documentation/Source: https://github.com/dmsl/anyplace/tree/master/viewer
- Current Leaders: Kyriakos Georgiou, Dimitris Valeris

### API
The source code of Anyplace JSON API.  
- Try: https://anyplace.cs.ucy.ac.cy/developers/
- Current Leaders: Kyriakos Georgiou, Dimitris Valeris

### Architect
The source code of Anyplace HTML5/CSS3 Architect Tool. 
- Try: https://anyplace.cs.ucy.ac.cy/architect/
- Documentation/Source: https://github.com/dmsl/anyplace/tree/master/architect
- Current Leaders: Kyriakos Georgiou, Dimitris Valeris

### Data Store
Anyplace uses a scalable document store for its backend (named Couchbase). The couchbase views are available through the following JSON file:
- Couch Views: https://github.com/dmsl/anyplace/blob/develop/ddocs.json
- Couchbase 2.2.1 Installation: https://www.digitalocean.com/community/tutorials/how-to-install-couchbase-from-source-with-git-and-make-on-a-debian-7-vps
- Anyplace Couchbase 3.0 Version: available in May 2017.
- Current Leaders: Constantinos Costa

## LATEST DEVELOPMENT VERSION
To test the latest development version you can use a replica installation of the backend architecture through the following URL (the backend database is not updated, as such you might not see the latest data here): 
- DEV Viewer: https://goo.gl/eSzl8n
- DEV Architect: https://goo.gl/5S0oN3
- DEV API: https://goo.gl/p5YUdb
- DEV Code: https://github.com/dmsl/anyplace/tree/develop

## TEAM
- https://anyplace.cs.ucy.ac.cy/#about

