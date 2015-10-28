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
 * Copyright 2014-2015 ForgeRock AS.
 */

/*global require, define, module, $, QUnit, window*/
define([
    "sinon",
    "org/forgerock/commons/ui/common/main/Configuration",
    "org/forgerock/openidm/ui/LoginView",
    "org/forgerock/openidm/ui/util/delegates/SiteIdentificationDelegate",
    "./mocks/siteIdentification",
    "./mocks/adminUserView",
    "./mocks/oldPassword"
], function (sinon, conf, LoginView, siteIdentificationDelegate, siteIdentificationMocks, adminUserViewMock, oldPasswordMock) {

    return {
        executeAll: function (server) {

            module("EndUser UI Functions");

            QUnit.asyncTest("Site Identification for login screen", function () {

                siteIdentificationMocks(server);
                conf.globalData.siteIdentification = true;

                LoginView.element = $("<div>")[0];
                delete LoginView.route;
                LoginView.render([], function () {

                    var siteIdStub = sinon.stub(siteIdentificationDelegate, "getSiteIdentificationForLogin", function (login, callback) {
                        siteIdentificationDelegate.getSiteIdentificationForLogin.restore();
                        siteIdentificationDelegate.getSiteIdentificationForLogin(login, function (data) {

                            callback(data);

                            QUnit.equal(LoginView.$el.find("#siteImage").css('display'), "block", "Site Image displayed");
                            QUnit.equal(LoginView.$el.find("#passPhrase").text(), "human", "Site ID Phrase displayed");

                            QUnit.start();

                        });

                    });

                    LoginView.$el.find("[name=login]").val("openidm-admin").trigger("change");

                });
            });

            QUnit.asyncTest("Change password dialog", function () {
                var oldPasswordDialog = require("org/forgerock/openidm/ui/profile/EnterOldPasswordDialog");
                oldPasswordMock(server);

                oldPasswordDialog.render([], function () {

                    var userDelegate = require("UserDelegate");
                    sinon.stub(userDelegate, "checkCredentials", function (value, successCallback, errorCallback) {
                        if (value === "testpassword") {
                            successCallback();
                        } else {
                            errorCallback("Incorrect");
                        }
                    });

                    oldPasswordDialog.$el.find("input[name=oldPassword]").val("test").trigger("change");
                    QUnit.equal(oldPasswordDialog.$el.find("input[name=oldPassword]").attr('data-validation-status'), 'error', 'Old password field should be disabled following incorrect password value');
                    QUnit.equal(oldPasswordDialog.$el.find("input[type=submit]").prop('disabled'), false, 'Submit button for form should still be enabled after validation failed');

                    oldPasswordDialog.$el.find("input[name=oldPassword]").val("testpassword").trigger("change");
                    QUnit.equal(oldPasswordDialog.$el.find("input[name=oldPassword]").attr('data-validation-status'), 'ok', 'Old password field should be enabled following correct password value');


                    QUnit.start();
                })
            });
        }
    };
});