### install nightwatch (if you have not already) the following command will install nightwatch globally on your system:
    > npm install nightwatch -g
    
### download selenium-server-standalone-{version}.jar from http://selenium-release.storage.googleapis.com/index.html 
### copy selenium-server-standalone-{version}.jar to test/nightwatchjs/selenium or whereever you want to put it
### adjust the "server_path" setting in the nightwatch.js file to reflect your selenium-server-standalone-{version}.jar location

### to start all tests in Firefox cd to your nightwatchjs test directory and run:
    > nightwatch -c ./nightwatch.js --env default
    
### to run a specific test use:
    > nightwatch -c ./nightwatch.js --env default --test tests/login/test1_login.js
    
### to start tests in Chrome you will need to download the chromedriver from http://chromedriver.storage.googleapis.com/index.html
### once downloaded:
    > unzip ~/Downloads/chromedriver_mac32.zip
    > mv ~/Downloads/chromedriver /user/local/bin/
    
### to run tests in Chrome run:
    > nightwatch -c ./nightwatch.js --env chrome


### for api documentation check out http://nightwatchjs.org/api

### if for some reason you get the error saying there is something already running on port 4445
    > lsof -i -n -P | grep 4445
    then
    > kill -9 {the process number}
