/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2014-2015 ForgeRock AS. All Rights Reserved
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://forgerock.org/license/CDDLv1.0.html
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at http://forgerock.org/license/CDDLv1.0.html
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 */

module.exports = function(grunt) {

    grunt.initConfig({
        forgerockui: process.env.FORGEROCK_UI_SRC,
        watch: {
            sync_and_test: {
                files: [

                    '<%= forgerockui %>/forgerock-ui-commons/src/main/js/**',
                    '<%= forgerockui %>/forgerock-ui-commons/src/main/resources/**',
                    '<%= forgerockui %>/forgerock-ui-user/src/main/js/**',
                    '<%= forgerockui %>/forgerock-ui-user/src/main/resources/**',

                    'openidm-ui-common/src/test/resources/**',
                    'openidm-ui-common/src/test/js/**',
                    'openidm-ui-common/src/test/qunit/**',

                    'openidm-ui-common/src/main/js/**',
                    'openidm-ui-common/src/main/resources/**',

                    'openidm-ui-admin/src/main/js/**',
                    'openidm-ui-admin/src/main/resources/**',
                    'openidm-ui-admin/src/test/qunit/**',

                    'openidm-ui-enduser/src/main/js/**',
                    'openidm-ui-enduser/src/main/resources/**',
                    'openidm-ui-enduser/src/test/qunit/**',
                ],
                tasks: [ 'sync:target', 'sync:zip' ]
            }
        },
        sync: {
            target: {
                files: [

                    {
                        cwd     : '<%= forgerockui %>/forgerock-ui-commons/src/main/js',
                        src     : ['**/*'],
                        dest    : 'openidm-ui-admin/target/www'
                    },
                    {
                        cwd     : '<%= forgerockui %>/forgerock-ui-commons/src/main/resources',
                        src     : ['**/*'],
                        dest    : 'openidm-ui-admin/target/www'
                    },

                    {
                        cwd     : '<%= forgerockui %>/forgerock-ui-commons/src/main/js',
                        src     : ['**/*'],
                        dest    : 'openidm-ui-enduser/target/www'
                    },
                    {
                        cwd     : '<%= forgerockui %>/forgerock-ui-commons/src/main/resources',
                        src     : ['**/*'],
                        dest    : 'openidm-ui-enduser/target/www'
                    },

                    {
                        cwd     : '<%= forgerockui %>/forgerock-ui-user/src/main/js',
                        src     : ['**/*'],
                        dest    : 'openidm-ui-enduser/target/www'
                    },
                    {
                        cwd     : '<%= forgerockui %>/forgerock-ui-user/src/main/resources',
                        src     : ['**/*'],
                        dest    : 'openidm-ui-enduser/target/www'
                    },

                    // common test libs
                    {
                        cwd     : 'openidm-ui-common/target/test/libs',
                        src     : ['**'],
                        dest    : 'openidm-ui-admin/target/test/libs'
                    },
                    {
                        cwd     : 'openidm-ui-common/target/test/libs',
                        src     : ['**'],
                        dest    : 'openidm-ui-enduser/target/test/libs'
                    },

                    // openidm-ui-common main
                    {
                        cwd     : 'openidm-ui-common/src/main/resources',
                        src     : ['**'],
                        dest    : 'openidm-ui-admin/target/www'
                    },
                    {
                        cwd     : 'openidm-ui-common/main/js',
                        src     : ['**'],
                        dest    : 'openidm-ui-admin/target/www'
                    },
                    {
                        cwd     : 'openidm-ui-common/main/resources',
                        src     : ['**'],
                        dest    : 'openidm-ui-enduser/target/www'
                    },
                    {
                        cwd     : 'openidm-ui-common/main/js',
                        src     : ['**'],
                        dest    : 'openidm-ui-enduser/target/www'
                    },

                    // openidm-ui-admin main
                    {
                        cwd     : 'openidm-ui-admin/src/main/js',
                        src     : ['**'],
                        dest    : 'openidm-ui-admin/target/www'
                    },
                    {
                        cwd     : 'openidm-ui-admin/src/main/resources',
                        src     : ['**'],
                        dest    : 'openidm-ui-admin/target/www'
                    },

                    // openidm-ui-enduser main
                    {
                        cwd     : 'openidm-ui-enduser/src/main/js',
                        src     : ['**'],
                        dest    : 'openidm-ui-enduser/target/www'
                    },
                    {
                        cwd     : 'openidm-ui-enduser/src/main/resources',
                        src     : ['**'],
                        dest    : 'openidm-ui-enduser/target/www'
                    },

                    // openidm-ui-common test
                    {
                        cwd     : 'openidm-ui-common/src/test/resources',
                        src     : ['css/**', 'qunit.html'],
                        dest    : 'openidm-ui-admin/target/test'
                    },
                    {
                        cwd     : 'openidm-ui-common/src/test/qunit',
                        src     : ['**'],
                        dest    : 'openidm-ui-admin/target/test/tests'
                    },
                    {
                        cwd     : 'openidm-ui-common/src/test/js',
                        src     : ['**'],
                        dest    : 'openidm-ui-admin/target/test'
                    },
                    {
                        cwd     : 'openidm-ui-common/src/test/resources',
                        src     : ['css/**', 'qunit.html'],
                        dest    : 'openidm-ui-enduser/target/test'
                    },
                    {
                        cwd     : 'openidm-ui-common/src/test/qunit',
                        src     : ['**'],
                        dest    : 'openidm-ui-enduser/target/test/tests'
                    },
                    {
                        cwd     : 'openidm-ui-common/src/test/js',
                        src     : ['**'],
                        dest    : 'openidm-ui-enduser/target/test'
                    },


                    // openidm-ui-admin test
                    {
                        cwd     : 'openidm-ui-admin/src/test/qunit',
                        src     : ['**'],
                        dest    : 'openidm-ui-admin/target/test/tests'
                    },

                    // openidm-ui-enduser test
                    {
                        cwd     : 'openidm-ui-enduser/src/test/qunit',
                        src     : ['**'],
                        dest    : 'openidm-ui-enduser/target/test/tests'
                    }


                ]
            },
            zip: {
                files: [
                    {
                        cwd     : 'openidm-ui-admin/target/www',
                        src     : ['**'],
                        dest    : '../openidm-zip/target/openidm/ui/default/admin/public'
                    },

                    {
                        cwd     : 'openidm-ui-enduser/target/www',
                        src     : ['**'],
                        dest    : '../openidm-zip/target/openidm/ui/default/enduser/public'
                    }
                ]
            }
        },
        qunit: {
            admin: 'openidm-ui-admin/target/test/qunit.html',
            enduser: 'openidm-ui-enduser/target/test/qunit.html'
        },
        notify_hooks: {
            options: {
                enabled: true,
                title: "QUnit Tests"
            }
        }
    });

    grunt.loadNpmTasks('grunt-contrib-qunit');

    grunt.loadNpmTasks('grunt-contrib-watch');
    
    grunt.loadNpmTasks('grunt-notify');

    grunt.loadNpmTasks('grunt-sync');

    grunt.task.run('notify_hooks');
    grunt.registerTask('default', ['sync:target', 'sync:zip', 'qunit:admin', 'qunit:enduser', 'watch']);

};
