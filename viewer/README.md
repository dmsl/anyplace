## Anyplace Viewer

### Installing dependencies

Install [Bower](http://bower.io/) dependencies:

```
$ bower install 
```

Install [Grunt](http://gruntjs.com/) tasks (requires [npm](https://www.npmjs.com/)):

```
$ npm install
```

### Building the app

Run Grunt

```
$ grunt
```

The built files will be in the *build* folder with the following structure:

    .
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


Once the *build* folder is ready, we need to access the index.html file through the http protocol. An easy way to do that is to start a Python [SimpleHTTPServer](https://docs.python.org/2/library/simplehttpserver.html) in the directory were the index.html is located.

```
$ python -m SimpleHTTPServer 9000
```

Then hit *http://localhost:9000/* in your browser to launch AnyplaceViewer. 

(The index.html file cannot be simply opened through the file system because the browser will throw security errors.)

**The port number is important**. For security purposes, AnyplaceServer accepts Cross-Origin requests from *localhost* only on ports 3030, 8080 and 9000.
