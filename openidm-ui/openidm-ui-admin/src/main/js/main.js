/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2014-2015 ForgeRock AS. All rights reserved.
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

/*global require, define, window, JSONEditor */

require.config({
    paths: {
        less: "libs/less-1.5.1-min",
        i18next: "libs/i18next-1.7.3-min",
        i18nGrid: "libs/i18n/grid.locale-en",
        backbone: "libs/backbone-1.1.2-min",
        "backbone.paginator": "libs/backbone.paginator.min-2.0.2-min",
        "backbone-relational": "libs/backbone-relational-0.9.0-min",
        "backgrid": "libs/backgrid.min-0.3.5-min",
        "backgrid-filter": "libs/backgrid-filter.min-0.3.5-min",
        "backgrid-paginator": "libs/backgrid-paginator.min-0.3.5-min",
        underscore: "libs/lodash-2.4.1-min",
        js2form: "libs/js2form-2.0",
        form2js: "libs/form2js-2.0",
        spin: "libs/spin-2.0.1-min",
        jquery: "libs/jquery-2.1.1-min",
        jqueryui: "libs/jquery-ui-1.11.1-min",
        jqgrid: "libs/jquery.jqGrid-4.5.4-min",
        gentleSelect: "libs/jquery-gentleSelect-0.1.3.1-min",
        cron: "libs/jquery-cron-r2427",
        xdate: "libs/xdate-0.8-min",
        doTimeout: "libs/jquery.ba-dotimeout-1.0-min",
        handlebars: "libs/handlebars-1.3.0-min",
        "bootstrap-tabdrop": "libs/bootstrap-tabdrop-1.0",
        bootstrap: "libs/bootstrap-3.3.4-custom",
        "bootstrap-dialog": "libs/bootstrap-dialog-1.34.4-min",
        placeholder: "libs/jquery.placeholder-2.0.8",
        selectize : "libs/selectize-0.12.1-min",
        d3 : "libs/d3-3.5.5-min",
        moment: "libs/moment-2.8.1-min",
        AuthnDelegate: "org/forgerock/openidm/ui/common/delegates/AuthnDelegate",
        jsonEditor: "libs/jsoneditor-0.7.9-min",
        UserDelegate: "org/forgerock/openidm/ui/common/util/UserDelegate",
        ThemeManager: "org/forgerock/openidm/ui/common/util/ThemeManager",
        "ldapjs-filter": "libs/ldapjs-filter-2253-min",
        faiconpicker: "libs/fontawesome-iconpicker-1.0.0-min"
    },

    shim: {
        underscore: {
            exports: "_"
        },
        backbone: {
            deps: ["underscore"],
            exports: "Backbone"
        },
        "backbone.paginator": {
            deps: ["backbone"]
        },
        "backgrid": {
            deps: ["jquery", "underscore", "backbone"],
            exports: "Backgrid"
        },
        "backgrid-filter": {
            deps: ["backgrid"]
        },
        "backgrid-paginator": {
            deps: ["backgrid", "backbone.paginator"]
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
            exports: "JSONEditor"
        },
        cron: {
            deps: ["jquery"]
        },
        gentleSelect: {
            deps: ["jquery"]
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
        },
        selectize: {
            deps: ["jquery"]
        },
        d3: {
            exports: "d3"
        },
        bootstrap: {
            deps: ["jquery"]
        },
        placeholder: {
            deps: ["jquery"]
        },
        'bootstrap-dialog': {
            deps: ["jquery", "underscore","backbone", "bootstrap"]
        },
        'bootstrap-tabdrop': {
            deps: ["jquery", "bootstrap"]
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
    "placeholder",
    "i18next",
    "jsonEditor",
    "gentleSelect",
    "cron",
    "selectize",
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
    placeholder,
    i18next,
    jsonEditor,
    gentleSelect,
    cron,
    selectize,
    i18n,
    constants,
    eventManager) {

    // Helpers for the code that hasn't been properly migrated to require these as explicit dependencies:
    window.$ = $;
    window._ = _;
    window.Backbone = Backbone;

    eventManager.sendEvent(constants.EVENT_DEPENDECIES_LOADED);

    JSONEditor.defaults.options.theme = 'bootstrap3';
    JSONEditor.defaults.options.iconlib = "fontawesome4";
});
