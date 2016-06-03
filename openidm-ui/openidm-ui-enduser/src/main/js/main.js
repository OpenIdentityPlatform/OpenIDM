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
 * Copyright 2011-2015 ForgeRock AS.
 */

/*global require, define, window */

require.config({
    map: {
        "*" : {
            "Footer": "org/forgerock/openidm/ui/common/components/Footer",
            "ThemeManager": "org/forgerock/openidm/ui/common/util/ThemeManager",
            "LoginView": "org/forgerock/openidm/ui/common/login/LoginView",
            "LoginDialog": "org/forgerock/commons/ui/common/LoginDialog",
            "RegisterView": "org/forgerock/openidm/ui/user/anonymousProcess/SelfRegistrationView",
            "ForgotUsernameView": "org/forgerock/commons/ui/user/anonymousProcess/ForgotUsernameView",
            "PasswordResetView": "org/forgerock/openidm/ui/user/anonymousProcess/PasswordResetView",
            "KBADelegate": "org/forgerock/commons/ui/user/delegates/KBADelegate",
            "NavigationFilter" : "org/forgerock/commons/ui/common/components/navigation/filters/RoleFilter",
            // TODO: Remove this when there are no longer any references to the "underscore" dependency
            "underscore": "lodash"
        }
    },
    paths: {
        i18next: "libs/i18next-1.7.3-min",
        backbone: "libs/backbone-1.1.2-min",
        lodash: "libs/lodash-3.10.1-min",
        js2form: "libs/js2form-2.0",
        form2js: "libs/form2js-2.0",
        spin: "libs/spin-2.0.1-min",
        jquery: "libs/jquery-2.1.1-min",
        xdate: "libs/xdate-0.8-min",
        doTimeout: "libs/jquery.ba-dotimeout-1.0-min",
        handlebars: "libs/handlebars-4.0.5",
        bootstrap: "libs/bootstrap-3.3.5-custom",
        "bootstrap-dialog": "libs/bootstrap-dialog-1.34.4-min",
        placeholder: "libs/jquery.placeholder-2.0.8",
        moment: "libs/moment-2.8.1-min",
        contentflow: "libs/contentflow",
        selectize : "libs/selectize-0.12.1-min",
        "backgrid": "libs/backgrid.min-0.3.5-min",
        "backgrid-filter": "libs/backgrid-filter.min-0.3.5-min",
        "backgrid-paginator": "libs/backgrid-paginator.min-0.3.5-min",
        faiconpicker: "libs/fontawesome-iconpicker-1.0.0-min",
        d3 : "libs/d3-3.5.5-min",
        dimple : "libs/dimple-2.1.2-min",
        jsonEditor: "libs/jsoneditor-0.7.9-min",
        dragula : "libs/dragula-3.6.7-min"
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
            exports: "JSONEditor"
        },
        contentflow: {
            exports: "contentflow"
        },
        spin: {
            exports: "spin"
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
        dimple: {
            exports: "dimple",
            deps: ["d3"]
        },
        d3: {
            exports: "d3"
        },
        selectize: {
            deps: ["jquery"]
        },
        bootstrap: {
            deps: ["jquery"]
        },
        'bootstrap-dialog': {
            deps: ["jquery", "underscore","backbone", "bootstrap"]
        },
        placeholder: {
            deps: ["jquery"]
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
        }
    }
});

require([
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/commons/ui/common/main/EventManager",

    "org/forgerock/commons/ui/common/main",
    "org/forgerock/openidm/ui/common/main",
    "config/main",

    "jquery",
    "underscore",
    "backbone",
    "handlebars",
    "i18next",
    "spin",
    "placeholder"
], function (Constants, EventManager) {
    EventManager.sendEvent(Constants.EVENT_DEPENDENCIES_LOADED);
});
