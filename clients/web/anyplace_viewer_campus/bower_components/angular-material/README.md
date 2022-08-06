This repository publishes the AngularJS Material v1.x library and localized installs
using `npm`. You can find the component source-code for this library in the
[AngularJS Material repository](https://github.com/angular/material).

> Please file issues and pull requests against that `angular/material` repository only. Do not file
issues here on the publishing repository.

## Layouts and SCSS

Included in this repository are the:

* **[SCSS files](https://github.com/angular/bower-material/tree/master/modules/scss)** which are
used to build the *.css files
* **[Layout files](https://github.com/angular/bower-material/tree/master/modules/layouts)** which
are used with the AngularJS Material (Flexbox) Layout API. 

> Note these are already included in the `angular-material.css` files. These copies are for direct
developer access and contain IE flexbox fixes; as needed.

## Installing AngularJS Material

You can install this package locally with `npm`. 

**Please note**: AngularJS Material requires **AngularJS 1.7.2** to **AngularJS 1.8.x**.

```shell
# To install latest formal release 
npm install angular-material

# To install latest release and update package.json
npm install angular-material --save

# To install from HEAD of master
npm install http://github.com/angular/bower-material/tarball/master

# or use alternate syntax to install HEAD from master
npm install http://github.com/angular/bower-material#master --save
# note: ^^ creates the following package.json dependency
#      "angular-material": "git+ssh://git@github.com/angular/bower-material.git#master"


# To install the v1.2.1 version 
npm install http://github.com/angular/bower-material/tarball/v1.2.1 --save

# To view all installed package 
npm list
```

## Using the AngularJS Material Library

You have installed the AngularJS library, next include the scripts and 
stylesheet in your main HTML file, in the order shown in the example below. Note that NPM 
will install the files under `/node_modules/angular-material/`.

```html
<!DOCTYPE html>
<html>
<head>
  <meta name="viewport" content="initial-scale=1, maximum-scale=1, user-scalable=no" />
  <link rel="stylesheet" href="/node_modules/angular-material/angular-material.css">
</head>
  <body ng-app="YourApp">
  <div ng-controller="YourController">

  </div>

  <script src="/node_modules/angular/angular.js"></script>
  <script src="/node_modules/angular-aria/angular-aria.js"></script>
  <script src="/node_modules/angular-animate/angular-animate.js"></script>
  <script src="/node_modules/angular-messages/angular-messages.js"></script>
  <script src="/node_modules/angular-material/angular-material.js"></script>
  <script>
    // Include app dependency on ngMaterial
    angular.module('YourApp', ['ngMaterial', 'ngMessages'])
      .controller("YourController", YourController);
  </script>
</body>
</html>
```

## Using the CDN

With the Google CDN, you will not need to download local copies of the distribution files.
Instead, reference the CDN URLs to use those remote library files. 
This is especially useful when using online tools such as CodePen, Plunker, or jsFiddle.

```html
<head>
    <!-- Angular Material CSS now available via Google CDN; version 1.2.1 used here -->
    <link rel="stylesheet" href="https://ajax.googleapis.com/ajax/libs/angular_material/1.2.1/angular-material.min.css">
</head>
<body>

    <!-- Angular Material Dependencies -->
    <script src="https://ajax.googleapis.com/ajax/libs/angularjs/1.8.2/angular.min.js"></script>
    <script src="https://ajax.googleapis.com/ajax/libs/angularjs/1.8.2/angular-animate.min.js"></script>
    <script src="https://ajax.googleapis.com/ajax/libs/angularjs/1.8.2/angular-aria.min.js"></script>
    <script src="https://ajax.googleapis.com/ajax/libs/angularjs/1.8.2/angular-messages.min.js"></script>
    
    <!-- Angular Material Javascript now available via Google CDN; version 1.2.1 used here -->
    <script src="https://ajax.googleapis.com/ajax/libs/angular_material/1.2.1/angular-material.min.js"></script>
</body>
```

> Note that the above sample references the 1.2.1 CDN release. Your version will change 
based on the latest stable release version.

## Unit Testing with Angular Material

<br/>
If you are using AngularJS Material and will be using Jasmine to test your custom application
code, you will need to also load two (2) AngularJS mock files:

*  AngularJS mocks
    * **angular-mocks.js** from `/node_modules/angular-mocks/angular-mocks.js`
*  AngularJS Material mocks
    * **angular-material-mocks.js** from `/node_modules/angular-material/angular-material-mocks.js`

<br/>

Shown below is a karma-configuration file (`karma.conf.js`) sample that may be a useful template for
your testing purposes:<br/><br/>

```js
module.exports = function(config) {

  var SRC = [
    'src/myApp/**/*.js',
    'test/myApp/**/*.spec.js'
  ];

  var LIBS = [
    'node_modules/angular/angular.js',
    'node_modules/angular-animate/angular-animate.js',
    'node_modules/angular-aria/angular-aria.js',
    'node_modules/angular-messages/angular-messages.js',
    'node_modules/angular-material/angular-material.js',
    
    'node_modules/angular-mocks/angular-mocks.js',
    'node_modules/angular-material/angular-material-mocks.js'
  ];

  config.set({
    basePath: __dirname + '/..',
    frameworks: ['jasmine'],
    
    files: LIBS.concat(SRC),

    port: 9876,
    reporters: ['progress'],
    colors: true,

    autoWatch: false,
    singleRun: true,
    browsers: ['Chrome']
  });
};
```
