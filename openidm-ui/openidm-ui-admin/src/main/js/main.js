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
        backbone: "libs/backbone-1.1.0-min",
        underscore: "libs/lodash-2.4.1-min",
        js2form: "libs/js2form-2.0",
        form2js: "libs/form2js-2.0",
        spin: "libs/spin-2.0.1-min",
        jquery: "libs/jquery-1.11.1-min",
        jqueryui: "libs/jquery-ui-1.11.1-min",
        jqgrid: "libs/jquery.jqGrid-4.5.4-min",
        cron: "libs/jquery-cron-r2427",
        gentleSelect: "libs/jquery-gentleSelect-0.1.3.1-min",
        xdate: "libs/xdate-0.8-min",
        doTimeout: "libs/jquery.ba-dotimeout-1.0-min",
        handlebars: "libs/handlebars-1.3.0-min",
        moment: "libs/moment-2.8.1-min",
        AuthnDelegate: "org/forgerock/openidm/ui/common/delegates/AuthnDelegate",
        jsonEditor: "libs/jsoneditor-0.7.9-min",
        ThemeManager: "org/forgerock/openidm/ui/common/util/ThemeManager",
        "ldapjs-filter": "libs/ldapjs-filter-2253-min"
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
        spin: {
            exports: "spin"
        },
        jsonEditor: {
            exports: "jsonEditor"
        },
        cron: {
            exports: "cron"
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
            deps: ["handlebars"],
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
    "less",
    "underscore",
    "backbone",
    "form2js",
    "js2form",
    "spin",
    "jqgrid",
    "jqueryui",
    "cron",
    "gentleSelect",
    "xdate",
    "moment",
    "doTimeout",
    "handlebars",
    "i18next",
    "jsonEditor",
    "org/forgerock/commons/ui/common/main/i18nManager",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/openidm/ui/common/main",
    "org/forgerock/openidm/ui/admin/main",
    "org/forgerock/commons/ui/common/main",
    "AuthnDelegate",
    "ThemeManager",
    "config/main"
], function(
    less,
    underscore,
    backbone,
    form2js,
    js2form,
    spin,
    jqgrid,
    jqueryui,
    cron,
    gentleSelect,
    xdate,
    moment,
    doTimeout,
    handlebars,
    i18next,
    jsonEditor,
    i18n,
    constants,
    eventManager) {
        eventManager.sendEvent(constants.EVENT_DEPENDECIES_LOADED);
});



