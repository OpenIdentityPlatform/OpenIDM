/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2014 ForgeRock AS. All rights reserved.
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

/*global require, define*/

require.config({
    paths: {
        less: "libs/less-1.5.1-min",
        i18next: "libs/i18next-1.7.3-min",
        i18nGrid: "libs/i18n/grid.locale-en",
        backbone: "libs/backbone-1.1.2-min",
        underscore: "libs/lodash-2.4.1-min",
        js2form: "libs/js2form-2.0",
        form2js: "libs/form2js-2.0",
        spin: "libs/spin-2.0.1-min",
        jquery: "libs/jquery-1.11.1-min",
        jqueryui: "libs/jquery-ui-1.11.1-min",
        jqgrid: "libs/jquery.jqGrid-4.5.4-min",
        xdate: "libs/xdate-0.8-min",
        doTimeout: "libs/jquery.ba-dotimeout-1.0-min",
        handlebars: "libs/handlebars-1.3.0-min",
        moment: "libs/moment-2.8.1-min",
        contentflow: "libs/contentflow",
        AuthnDelegate: "org/forgerock/openidm/ui/common/delegates/AuthnDelegate",
        UserDelegate: "org/forgerock/openidm/ui/user/delegates/UserDelegate",
        ThemeManager: "org/forgerock/openidm/ui/common/util/ThemeManager",
        jsonEditor: "libs/jsoneditor-0.7.9-min"
    },

    shim: {
        underscore: {
            exports: "_"
        },
        backbone: {
            deps: ["underscore"],
            exports: "Backbone"
        },
        js2form: {
            exports: "js2form"
        },
        form2js: {
            exports: "form2js"
        },
        jsonEditor: {
            exports: "jsonEditor"
        },
        contentflow: {
            exports: "contentflow"
        },
        spin: {
            exports: "spin"
        },
        jqueryui: {
            deps: ["jquery"],
            exports: "jqueryui"
        },
        i18nGrid: {
            deps: ["jquery"]
        },
        jqgrid: {
            deps: ["jqueryui", "i18nGrid"]
        }, 
        xdate: {
            exports: "xdate"
        },
        doTimeout: {
            deps: ["jquery"],
            exports: "doTimeout"
        },
        handlebars: {
            exports: "handlebars"
        },
        i18next: {
            deps: ["jquery", "handlebars"],
            exports: "i18next"
        },
        moment: {
            exports: "moment"
        }
    }
});

/**
 * Loads all application on start, so each module will be available to
 * required synchronously
 */
require([
    "jquery",
    "underscore",
    "backbone",
    "less",
    "form2js",
    "js2form",
    "spin",
    "jqgrid",
    "jqueryui",
    "xdate",
    "moment",
    "doTimeout",
    "handlebars",
    "i18next",
    "jsonEditor",
    "contentflow",
    "org/forgerock/commons/ui/common/main/i18nManager",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/openidm/ui/common/main",
    "org/forgerock/openidm/ui/user/main",
    "org/forgerock/openidm/ui/admin/main",
    "org/forgerock/commons/ui/user/main",
    "org/forgerock/commons/ui/common/main",
    "AuthnDelegate",
    "UserDelegate",
    "ThemeManager",
    "config/main",
    "org/forgerock/openam/ui/common/main",
    "org/forgerock/openam/ui/user/main"
], function(
    $,
    _,
    Backbone,
    less,
    form2js,
    js2form,
    spin,
    jqgrid,
    jqueryui,
    xdate,
    moment,
    doTimeout,
    handlebars,
    i18next,
    jsonEditor,
    contentflow,
    i18n,
    constants,
    eventManager) {

    // Helpers for the code that hasn't been properly migrated to require these as explicit dependencies:
    window.$ = $;
    window._ = _;
    window.Backbone = Backbone;

    eventManager.sendEvent(constants.EVENT_DEPENDECIES_LOADED);

});