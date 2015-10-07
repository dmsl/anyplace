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


Once the *build* folder is ready, open index.html to launch Anyplace Viewer.