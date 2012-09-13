/*
 * @license DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright © 2011-2012 ForgeRock AS. All rights reserved.
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


/**
 * @author yaromin
 */

require.config({
    paths: {
        mustache: "libs/mustache",
        backbone: "libs/backbone-0.9.2-min",
        underscore: "libs/underscore-1.3.3-min",
        js2form: "libs/js2form",
        form2js: "libs/form2js",
        contentflow: "libs/contentflow",
        spin: "libs/spin-1.2.5-min",
        jqueryui: "libs/jquery-ui-1.8.23.custom.min",
        xdate: "libs/xdate-0.7-min"
    },

    shim: {
        mustache: {
            exports: "Mustache"
        },
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
        contentflow: {
            exports: "contentflow"
        },
        spin: {
            exports: "spin"
        },
        jqueryui: {
            exports: "jqueryui"
        },
        xdate: {
            exports: "xdate"
        }
    }
});

/**
 * Loads all application on start, so each module will be available to
 * required synchronously
 */
require([
    "mustache",
    "underscore",
    "backbone",
    "form2js",
    "js2form",
    "contentflow",
    "spin",
    "jqueryui",
    "xdate",
    "org/forgerock/openidm/ui/common/util/Constants", 
    "org/forgerock/openidm/ui/common/main/EventManager",
    "config/main",
    "org/forgerock/openidm/ui/user/main", 
    "org/forgerock/openidm/ui/common/main"
], function(a, b, c, d, e, f, g, h, i, constants, eventManager) { 
    eventManager.sendEvent(constants.EVENT_DEPENDECIES_LOADED);
});



