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

/*global $, require, QUnit */


define([
    "jquery",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/commons/ui/common/main/EventManager",
    "sinon",
    "../test/tests/common",
    "../test/tests/specific"
], function ($, constants, eventManager, sinon, commonTests, specificTests) {

    $.doTimeout = function (name, time, func) {
        func(); // run the function immediately rather than delayed.
    };

    return function (server) {
        eventManager.registerListener(constants.EVENT_APP_INTIALIZED, function () {
            QUnit.testStart(function () {
                var vm = require("org/forgerock/commons/ui/common/main/ViewManager");

                vm.currentView = null;
                vm.currentDialog = null;
                vm.currentViewArgs = null;
                vm.currentDialogArgs = null;

                require("org/forgerock/commons/ui/common/main/Configuration").baseTemplate = null;
            });
            QUnit.start();

            commonTests.executeAll(server);
            specificTests.executeAll(server);

            QUnit.done(function () {
                localStorage.clear();
                Backbone.history.stop();
                window.location.hash = "";
            });

        });
    };

});
