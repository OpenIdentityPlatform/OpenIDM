/**
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
 * Header, with the fields enclosed by brackets [] replaced by your own identifying
 * information: "Portions copyright [year] [name of copyright owner]".
 *
 * Copyright 2014-2016 ForgeRock AS.
 */

module.exports = function(grunt) {
    grunt.initConfig({
        nightwatch: {
            /**
             * Run the integration tests using Nightwatch and Firefox or Chrome.
             */
            admin: {
                config_path: 'openidm-ui-admin/src/test/nightwatchjs/config.json'
            },
            enduser: {
                config_path: 'openidm-ui-enduser/src/test/nightwatchjs/config.json'
            },
            options: {
                selenium: {
                    start_process: true,
                    server_path: "./selenium/selenium-server-standalone.jar",
                    log_path: "reports",
                    host: "127.0.0.1",
                    port: 4445,
                    cli_args: {
                        "webdriver.chrome.driver": "/usr/local/bin/chromedriver",
                        "webdriver.ie.driver": ""
                    }
                },
                test_settings: {
                    "default": {
                        launch_url: "http://localhost",
                        selenium_port: 4445,
                        selenium_host: "localhost",
                        silent: true,
                        screenshots: {
                            enabled: false,
                            path: ""
                        },
                        desiredCapabilities: {
                            browserName: "firefox",
                            javascriptEnabled: true,
                            acceptSslCerts: true
                        }
                    }
                }
            }
        },
        parallel: {
            /**
             * Run the dev tasks for the sub-projects in parallel.
             */
            dev: {
                tasks: [{
                    grunt: true,
                    args: ['dev'],
                    opts: {
                        cwd: 'openidm-ui-admin'
                    }
                }, {
                    grunt: true,
                    args: ['dev'],
                    opts: {
                        cwd: 'openidm-ui-enduser'
                    }
                }],
                options: {
                    stream: true
                }
            }
        }
    });

    grunt.loadNpmTasks("grunt-nightwatch");
    grunt.loadNpmTasks('grunt-parallel');

    grunt.registerTask('test', ['nightwatch:admin', 'nightwatch:enduser']);
    grunt.registerTask('dev', ['parallel']);
    grunt.registerTask('default', 'dev');
};
