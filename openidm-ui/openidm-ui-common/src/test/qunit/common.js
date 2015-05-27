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

/*global require, define, QUnit, $ */

define([
    "sinon",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/commons/ui/common/main/Router",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/openidm/ui/common/MandatoryPasswordChangeDialog",
    "org/forgerock/commons/ui/common/LoginView",
    "./mocks/encryptedPW",
    "./mocks/cleartextPW"
], function (sinon, constants, router, eventManager, mandatoryPasswordChangeDialog, loginView, encryptedPW, cleartextPW) {

    return {
        executeAll: function (server) {
            module('Common IDM functionality');

            QUnit.asyncTest("Login Form", function () {
                loginView.render([], function () {
                    QUnit.ok(loginView.$el.find("#login").length && loginView.$el.find("#password").length, "Username and Password displayed")

                    QUnit.start();
                });
            });

            QUnit.asyncTest("Initial Login Process", function () {

                var dialogRenderStub = sinon.stub(mandatoryPasswordChangeDialog, "render", function (args, callback) {

                    mandatoryPasswordChangeDialog.render.restore();
                    mandatoryPasswordChangeDialog.render(args, function () {

                        QUnit.ok(true, "Mandatory password change dialog displayed when cleartext password used");

                        QUnit.equal(mandatoryPasswordChangeDialog.$el.find(".validationRules > .field-rule").length, 5, "Five validation rules for password displayed");

                        if (callback) {
                            callback();
                        }

                        QUnit.start();
                    });
                });

                cleartextPW(server);

                eventManager.sendEvent(constants.EVENT_LOGIN_REQUEST, { userName: "openidm-admin", password: "openidm-admin" });
            });


            QUnit.asyncTest("Subsequent Login Process", function () {
                var landingPageView = require(router.configuration.routes.landingPage.view),
                    landingPageRenderStub = sinon.stub(landingPageView, "render", function (args, callback) {

                    landingPageView.render.restore();
                    landingPageView.render(args, function () {
                        var viewManager = require("org/forgerock/commons/ui/common/main/ViewManager");

                        QUnit.ok(viewManager.currentView === router.configuration.routes.landingPage.view && viewManager.currentDialog === null, "Landing page shown after successful login with encrypted password");

                        if (callback) {
                            callback();
                        }

                        QUnit.start();
                    });
                });

                encryptedPW(server);

                eventManager.sendEvent(constants.EVENT_LOGIN_REQUEST, { userName: "openidm-admin", password: "Passw0rd" });
            });

        }
    };

});
