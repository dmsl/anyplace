# CA Tasks
* TESTING:
    - Use the test/ directory for all junit/test related code
    - import tests there, with a README, and commands on how to run all of them
    - try to make the whole testing in gradle
        - so we can run something like gradle testEndpoints,
          and check everything
          
* Exception handling:          
    - code does not compile in java 1.8 (and it's not a good practise not to handle exceptions)
    - I temporarily throw the exceptions at the method level,
      but they should be handled properly:
        - remove Throws in CLI/main and in Anyplace.java
        - handle them inside Anyplace.java:
          
* .anyplace settings:
    - I've written some code for the settings, in the case that the code is executed as a tool
    - extend it, for all of our settings
    - if it does not exist, automatically create it with some defaults 
    
