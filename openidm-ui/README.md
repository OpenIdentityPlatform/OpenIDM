# Testing with NightwatchJS


### Install npm dependencies:
    > npm install

### download selenium-server-standalone-{version}.jar from http://selenium-release.storage.googleapis.com/index.html
### copy selenium-server-standalone-{version}.jar to ./selenium/selenium-server-standalone.jar

### to start all tests:
    > grunt test

### to start admin tests:
    > grunt nightwatch:admin

### to start enduser:
    > grunt nightwatch:enduser

### to run a specific test use:
    > grunt nightwatch:enduser --test openidm-ui-enduser/src/test/nightwatchjs/tests/profile/changePassword.js

### to start tests in Chrome you will need to download the chromedriver from http://chromedriver.storage.googleapis.com/index.html
### once downloaded:
    > unzip ~/Downloads/chromedriver_mac32.zip
    > mv ~/Downloads/chromedriver /usr/local/bin/

### to run tests in Chrome run:
    > grunt test --env chrome

### to run tests in both Firefox and Chrome run:
    > grunt test -e default,chrome

### for api documentation check out http://nightwatchjs.org/api

### if for some reason you get the error saying there is something already running on port 4445
    > lsof -i -n -P | grep 4445
    then
    > kill -9 {the process number}
