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
 * Copyright 2016 ForgeRock AS.
 */

module.exports = function(grunt, options) {
    var buildCompositionDirs = [
            "target/dependencies",
            "target/dependency",
            "target/classes"
        ],
        watchCompositionDirs = [
            "src/main/js",
            "src/main/resources",
            "../openidm-ui-common/src/main/js",
            "../openidm-ui-common/src/main/resources"
        ].concat(options.watchCompositionDirs),
        testCompositionDirs = {
            "../openidm-ui-common/src/test/qunit": "/",
            "src/test/qunit": "/tests"
        },
        mavenSrcPath = "src/main/js",
        compositionDirectory = "target/compose",
        compiledDirectory = "target/www",
        testDirectory = "target/qunit",
        deployDirectory = "../../openidm-zip/target/openidm/ui/" + options.deployDirectory,
        lessFiles = [{
            src: compositionDirectory + "/css/structure.less",
            dest: compiledDirectory + "/css/structure.css"
        }, {
            src: compositionDirectory + "/css/theme.less",
            dest: compiledDirectory + "/css/theme.css"
        }];

    grunt.initConfig({
        copy: {
            /**
             * Copy all the sources and resources from this project and all dependencies into the composition directory.
             *
             * TODO: This copying shouldn't really be necessary, but is required because the dependencies are all over
             * the place. If we move to using npm for our dependencies, this can be greatly simplified.
             */
            compose: {
                files: buildCompositionDirs.map(function (dir) {
                    return {
                        expand: true,
                        cwd: dir,
                        src: ["**"],
                        dest: compositionDirectory
                    }
                })
            },
            /**
             * Copy files that do not need to be compiled into the compiled directory.
             */
            compiled: {
                files: [{
                    expand: true,
                    cwd: compositionDirectory,
                    src: [
                        "**",
                        "!main.js", // Output by r.js
                        "!**/*.less" // Compiled to CSS files
                    ],
                    dest: compiledDirectory
                }]
            },
            /**
             * Copy the main file. This is used when skipping the requirejs step.
             */
            compiledMain: {
                files: [{
                    src: compositionDirectory + "/main.js",
                    dest: compiledDirectory + "/main.js"
                }],
                compareUsing: "md5"
            },
            /**
             * Copy the test source files into the test target directory.
             */
            test: {
                files: Object.keys(testCompositionDirs).map(function (dir) {
                    return {
                        expand: true,
                        cwd: dir,
                        src: ["**"],
                        dest: testDirectory + testCompositionDirs[dir]
                    }
                })
            }
        },
        eslint: {
            /**
             * Check the JavaScript source code for common mistakes and style issues.
             */
            lint: {
                src: [
                    mavenSrcPath + "/**/*.js"
                ],
                options: {
                    format: options.eslintFormatter
                }
            }
        },
        less: {
            /**
             * Compile LESS source code into minified CSS files.
             */
            dev: {
                files: lessFiles
            },
            prod: {
                files: lessFiles,
                options: {
                    compress: true,
                    plugins: options.lessPlugins
                }
            },
            options: {
                relativeUrls: true
            }
        },
        notify_hooks: {
            options: {
                enabled: true,
                title: "QUnit Tests"
            }
        },
        qunit: {
            /**
             * Run the unit tests using PhantonJS.
             */
            test: testDirectory + '/index.html'
        },
        requirejs: {
            /**
             * Concatenate and uglify the JavaScript.
             */
            compile: {
                options: {
                    baseUrl: compositionDirectory,
                    mainConfigFile: compositionDirectory + "/main.js",
                    out: compiledDirectory + "/main.js",
                    include: ["main"],
                    preserveLicenseComments: false,
                    generateSourceMaps: true,
                    optimize: "uglify2",
                    // This file is excluded from optimization so that the UI can be customized without having to
                    // repackage it.
                    excludeShallow: [
                        "config/AppConfiguration"
                    ]
                }
            }
        },
        sync: {
            /**
             * Copy all the sources and resources from this project and all dependencies into the composition directory.
             */
            compose: {
                files: watchCompositionDirs.map(function (dir) {
                    return {
                        cwd: dir,
                        src: ["**"],
                        dest: compositionDirectory
                    }
                }),
                compareUsing: "md5"
            },
            /**
             * Copy files that do not need to be compiled into the compiled directory.
             *
             * Note that this also copies main.js because the requirejs step is not being performed when watching (it
             * is too slow).
             */
            compiled: {
                files: [{
                    cwd: compositionDirectory,
                    src: [
                        "**",
                        "!**/*.less" // Compiled to CSS files
                    ],
                    dest: compiledDirectory
                }],
                compareUsing: "md5"
            },
            /**
             * Copy the test source files into the test target directory.
             */
            test: {
                files: Object.keys(testCompositionDirs).map(function (inputDirectory) {
                    return {
                        cwd: inputDirectory,
                        src: ["**"],
                        dest: testDirectory + testCompositionDirs[inputDirectory]
                    };
                }),
                verbose: true,
                compareUsing: "md5" // Avoids spurious syncs of touched, but otherwise unchanged, files (e.g. CSS)
            },
            /**
             * Copy the compiled files to the deploy directory.
             */
            deploy: {
                files: [{
                    cwd: compiledDirectory,
                    src: ["**"],
                    dest: deployDirectory
                }],
                verbose: true,
                compareUsing: "md5" // Avoids spurious syncs of touched, but otherwise unchanged, files (e.g. CSS)
            }
        },
        watch: {
            /**
             * Redeploy whenever any source files change.
             */
            source: {
                files: watchCompositionDirs.concat(Object.keys(testCompositionDirs)).map(function (dir) {
                    return dir + "/**";
                }),
                tasks: ["deploy"]
            }
        }
    });

    grunt.loadNpmTasks("grunt-contrib-copy");
    grunt.loadNpmTasks("grunt-contrib-less");
    grunt.loadNpmTasks('grunt-contrib-qunit');
    grunt.loadNpmTasks("grunt-contrib-requirejs");
    grunt.loadNpmTasks("grunt-contrib-watch");
    grunt.loadNpmTasks("grunt-eslint");
    grunt.loadNpmTasks('grunt-notify');
    grunt.loadNpmTasks("grunt-sync");

    grunt.registerTask('build:dev', [
        'copy:compose',
        'eslint',
        'less:dev',
        'copy:compiled',
        'copy:compiledMain',
        'copy:test',
        'qunit'
    ]);

    grunt.registerTask('build:prod', [
        'copy:compose',
        'eslint',
        'requirejs',
        'less:prod',
        'copy:compiled',
        'copy:test',
        'qunit'
    ]);

    grunt.registerTask("deploy", [
        "sync:compose",
        "less:dev",
        "sync:compiled",
        "sync:test",
        "sync:deploy",
        'qunit'
    ]);

    grunt.registerTask("dev", ["copy:compose", "deploy", "watch"]);
    grunt.registerTask("default", "dev");

    grunt.task.run('notify_hooks');
};