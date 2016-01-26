module.exports = {
  "src_folders" : [
    "tests/login",
    "tests/connector",
    "tests/managed"
  ],
  "output_folder" : "reports",
  "custom_commands_path" : "",
  "custom_assertions_path" : "",
  "page_objects_path" : "",
  "globals_path" : "",

  "selenium" : {
    "start_process" : true,
    "server_path" : "selenium/selenium-server-standalone-2.48.2.jar",
    "log_path" : "reports",
    "host" : "127.0.0.1",
    "port" : 4445,
    "cli_args" : {
      "webdriver.chrome.driver" : "/usr/local/bin/chromedriver",
      "webdriver.ie.driver" : ""
    }
  },

  "test_settings" : {
    "default" : {
      "launch_url" : "http://localhost",
      "selenium_port"  : 4445,
      "selenium_host"  : "localhost",
      "silent": true,
      "screenshots" : {
        "enabled" : false,
        "path" : ""
      },
      "desiredCapabilities": {
        "browserName": "firefox",
        "javascriptEnabled": true,
        "acceptSslCerts": true
      },
      globals: require('./data/devGlobals')
    },

    "chrome" : {
      "desiredCapabilities": {
        "browserName": "chrome",
        "javascriptEnabled": true,
        "acceptSslCerts": true
      }
    }
  }
};
