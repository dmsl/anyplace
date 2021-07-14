# WEB APPLICATIONS:
Separate web application frontends written in Angular-JS.
They utilize Google Maps JS. In future work we aim to migrate to Leaflet/Angular.
They also share some common resources: [shared](./shared/)
```
shared
├── css
├── images
└── js/
    └── anyplace-core-js
```

1. [architect](./anyplace_architect)
2. [viewer](./anyplace_viewer)
3. [viewer_campus](./anyplace_viewer_campus)

##  Compilation:
To compile the
[architect](./anyplace_architect), [viewer](./anyplace_viewer), or  [viewer_campus](./anyplace_viewer_campus)
please follow the below instructions:

<details>
<summary>
Show Compilation Instructions
</summary>
1. `cd` to the relevant web app directory

2. Install [Bower](http://bower.io/) dependencies:
```
bower install
```

3. Install [Grunt](http://gruntjs.com/) tasks (requires [npm](https://www.npmjs.com/)):

```
# For Unix:
npm install
# For Windows:
npm install -g grunt-cli
```
4. Build the web app:

4.1 Development Version:
```
# grunt will keep 'watching' for resource updates (js/css/images)
grunt
```
4.1 Deployment version:
```
grunt deploy
```
For windows: use `grunt.cmd`


### The built files will be in the *build* folder with the following structure:
```
    <web-app>
    ├── build
    │   ├── css
    │   │   └── anyplace.min.css  # Concatenated and minified CSS
    │   ├── images
    │   │   └── ...               # Optimized images
    │   └── js
    │       └── anyplace.min.js   # Concatenated and minified JS files
    ├── bower_components
    │   └── ...                   # Bower dependencies
    └── index.html
```

#### Older notes on deploying the HTML:
Once the *build* folder is ready, we need to access the index.html file through the http protocol.
An easy way to do that is to start a Python [SimpleHTTPServer](https://docs.python.org/2/library/simplehttpserver.html) in the directory were the index.html is located.

```
python -m SimpleHTTPServer 9000
```

Then hit *http://localhost:9000/* in your browser to launch AnyplaceArchitect.

(The index.html file cannot be simply opened through the file system because the browser will throw security errors.)

**The port number is important**.
For security purposes, AnyplaceServer accepts Cross-Origin requests from *localhost* only on ports 3030, 8080 and 9000.

</details>

---

# DOCUMENTATION:
- [developers](./developers): Uses `Swagger-UI`

# Common resources:
- [images](./images): Common images shared between above web applications.
- [js](./js): Common JavaScript shared between above web applications.
- [style](./style): Common CSS shared between above web applications.
