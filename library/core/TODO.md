# CA Tasks
* TESTING:
    - Use the test/ directory for all junit/test related code
    - import tests there, with a README, and commands on how to run all of them
    - try to make the whole testing in gradle
        - so we can run something like gradle testEndpoints,
          and check everything

    - there should be tests that validate for real access points..
       * a few values should be gathered from ucy.. and there could be validation also
       e.g. one measurement in lab216 listened to X WiFI RSSis, and resolved to lat,long,
       and then lat, long is automatically verified

* Exception handling:
    - code does not compile in java 1.8 (and it's not a good practise not to handle exceptions)
    - I temporarily throw the exceptions at the method level,
      but they should be handled properly:
        - remove Throws in CLI/main and in Anyplace.java
        - handle them inside Anyplace.java:

    e.g. when a website is not accessible instead of emitting the exception, show a helpful message
    and if it's CLI then emit just ERROR: <error msg>.
    if it is a library then emit a JSON object!

    - TimeOut:
        + some endpoints don't have enough time it seems.
        + make this param: TIMEOUT
        + and because some endpoints might take even more time, make another one,
        + e.g. TIMEOUT_LONG, and use e.g. for fetching all buildings.. or a time consuming query..

* .anyplace settings:
    - I've written some code for the settings, in the case that the code is executed as a tool
    - in my code it uses .anyplace file.
    - change this to:
        + .anyplace/config for the settings,
        + .anyplace/cache/ for any offline caching
        + so you will need to create a .anyplace folder in home directory (and equivalent on Windows)
        + and then the config file and cache folder..
        + extend .anyplace/config to add more settings like TIMEOUT, and TIMEOUT_LONG
    - you can even do more changes, e.g. api key is huge, so it could be stored in its own file .anyplace/api_key
        + and prompt users to paste the key there..
    - if settings do not exist: automatically create them with some defaults
    
* Deprecated API warning: 
    - when building with the gradle, we get the following:
    
    Note: /Users/paschalis/src/anyplace/github/library/core/src/main/java/cy/ac/ucy/cs/anyplace/RestClient.java uses or overrides a deprecated API.
    Note: Recompile with -Xlint:deprecation for details.
    
    - Investigate this, and replace the API call!

* DEBUG mode:
    - when the argument `--debug/-d` is given it should print out more info
    like the parameters that are being used.. and be more verbose..
    to have a flexible CLI you might wanna use a project that already handles the CLIs..
    (there's an Apache one that works well..)

# OTHER FIXES:
* Jar file fixes/issues
The file size of the previous jar file is rather small.
So lib dependencies shouldn't have been included.
Also when running there are issues:
java -jar anyplace-lib.jar
OUTPUT:
no main manifest attribute, in anyplace-lib.jar

Now it is generated using

