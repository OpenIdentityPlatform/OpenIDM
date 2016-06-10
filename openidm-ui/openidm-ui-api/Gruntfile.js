/*
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
module.exports = function (grunt) {
    grunt.initConfig({
        copy: {
            swagger: {
                files: [{
                    expand: true,
                    cwd: 'node_modules/swagger-ui/dist/',
                    src: ['swagger-ui.js', 'css/*', 'fonts/*', 'images/*', 'lang/*', 'lib/*'],
                    dest: 'target/www/'
                }],
                options: {
                    noProcess: ['**/*.{png,gif,jpg,ico,svg,ttf,eot,woff}'],
                    process: function (content, srcpath) {
                        // https://github.com/swagger-api/swagger-js/pull/780
                        if (srcpath.indexOf('swagger-ui.js') !== -1) {
                            return content.replace(
                                /var requestUrl = this\.path;/,
                                "var requestUrl = this.path.replace(/#.*/, '');");
                        }
                        return content;
                    }
                }
            },
            swaggerThemes: {
                files: [{
                    expand: true,
                    cwd: 'node_modules/swagger-ui-themes/themes/',
                    src: ['theme-flattop.css'],
                    dest: 'target/www/'
                }]
            },
            resources: {
                files: [{
                    expand: true,
                    cwd: 'src/main/resources/',
                    src: ['**'],
                    dest: 'target/www/'
                }]
            }
        }
    });

    grunt.loadNpmTasks('grunt-contrib-copy');

    grunt.registerTask('build:dev', ['copy:swagger', 'copy:swaggerThemes', 'copy:resources']);
    grunt.registerTask('build:prod', ['copy:swagger', 'copy:swaggerThemes', 'copy:resources']);
};