# CA Tasks
* TESTING:
    - Use the test/ directory for all junit/test related code
        + proper way
        + keep code tidy, clean, readable
    - import tests there, with a README, and commands on how to run all of them
      e.g. ./gradlew test
    - make the whole testing in gradle
        - so we can run something like gradle testEndpoints,
          and check everything
        - one can do it on any OS just by cloning the src (no IDEs.. just terminal)
    - and this testing can be integrated on github (future)

    -  Value Validation (FUTURE)
    there should be tests that validate for real access points..
       * a few values should be gathered from ucy.. and there could be validation also
       e.g. one measurement in lab216 listened to X WiFI RSSis, and resolved to lat,long,
       and then lat, long is automatically verified


* Exception handling:
    - code does not compile in java 1.8 (and it's not a good practice not to handle exceptions)
    - I temporarily throw the exceptions at the method level,
      but they should be handled properly:
        - remove Throws in CLI/main and in Anyplace.java
        - handle them inside Anyplace.java:

    e.g. when a website is not accessible instead of emitting the exception, show a helpful message
    and if it's CLI then emit just ERROR: <error msg>.
    if it is a library then emit a JSON object!

    - TimeOut: (FUTURE)
        + make this param: TIMEOUT

* .anyplace settings:
    - I've written some code for the settings, in the case that the code is executed as a tool
    - in my code it uses .anyplace file.
    - change this to:
        + api_key separate file since key is large
        + .anyplace/config for the settings,
        + .anyplace/cache/ for any offline caching
        + so you will need to create a .anyplace folder in home directory (and equivalent on Windows)
        + and then the config file and cache folder..
        + extend .anyplace/config to add more settings like TIMEOUT, and TIMEOUT_LONG
    - you can even do more changes, e.g. api key is huge, so it could be stored in its own file .anyplace/api_key
        + and prompt users to paste the key there..
    - if settings do not exist: automatically create them with some defaults

* RETURN OF VALUES:
    - --json by default
    - new methods that return custom java Objects (see example in code)
        + may be automatically parsed by GSON

        * server is down (from haproxy): OUTPUT:
<html><body><h1>503 Service Unavailable</h1>
No server is available to handle this request.
</body></html>

(until here..)
------------------


* offline localization issue (after the .anyplace/cache dir is fixed) ( ON MAC)
Estimate the location of the user offline. Needs the radiomap file:
Exception in thread "main" org.json.JSONException: JSONObject["rss"] is not a number.
        at org.json.JSONObject.getDouble(JSONObject.java:543)
        at org.json.JSONObject.getInt(JSONObject.java:560)
        at Anyplace.estimatePositionOffline(Anyplace.java:608)
        at CLI.main(CLI.java:424


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


* READMEs in github:
    - make them simpler
    - authors at the bottom
    - remove license, and licence the projects properly
    - (separate file in specific format, and then github can do it's thing)


* ENDPOINT: which backend is used
    - for testing


API KEY: safety to
