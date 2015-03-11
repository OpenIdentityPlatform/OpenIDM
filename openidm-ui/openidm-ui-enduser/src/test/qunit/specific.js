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

/*global require, define, module, $, QUnit, window*/
define([
    "sinon",
    "org/forgerock/commons/ui/common/main/Configuration",
    "org/forgerock/openidm/ui/user/LoginView",
    "org/forgerock/openidm/ui/admin/linkedView/LinkedView",
    "org/forgerock/openidm/ui/admin/users/UsersView",
    "org/forgerock/openidm/ui/admin/users/AdminUserProfileView",
    "org/forgerock/openidm/ui/user/delegates/SiteIdentificationDelegate",
    "./mocks/siteIdentification",
    "./mocks/adminUserView",
    "./mocks/oldPassword"
], function (sinon, conf, LoginView, LinkedView, usersView, adminUserProfileView, siteIdentificationDelegate, siteIdentificationMocks, adminUserViewMock, oldPasswordMock) {

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


            QUnit.asyncTest("Admin user page", function () {
                adminUserViewMock(server);

                adminUserProfileView.render(["FakeTest"], function () {

                    QUnit.equal(adminUserProfileView.$el.find("#userName").val(), "FakeTest", "userName properly loaded into form");

                    QUnit.equal(adminUserProfileView.$el.find("#linkedViewSelect option").length, 1, "Linked Resource properly loaded");

                    adminUserProfileView.reloadData();

                    QUnit.equal($("[name=roles]:checked").val(), "openidm-authorized", "Role remains checked after form reloaded");

                    QUnit.start();

                });

            });

            QUnit.asyncTest("Change password dialog", function () {
                var oldPasswordDialog = require("org/forgerock/openidm/ui/user/profile/EnterOldPasswordDialog");
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