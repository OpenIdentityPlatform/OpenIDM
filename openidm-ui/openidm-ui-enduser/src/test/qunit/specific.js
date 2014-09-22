/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2014 ForgeRock AS. All Rights Reserved
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
    "./mocks/adminUserView"
], function (sinon, conf, LoginView, LinkedView, usersView, adminUserProfileView, siteIdentificationDelegate, siteIdentificationMocks, adminUserViewMock) {

    return {
        executeAll: function (server) {

            module("EndUser UI Functions");

            QUnit.asyncTest("Site Identification for login screen", function () {

                siteIdentificationMocks(server);
                conf.globalData.siteIdentification = true;

                LoginView.element = $("<div>")[0];
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

                    QUnit.ok(LoginView.$el.find('#identificationMessage').length, "Site Identification Prompt displayed");

                    LoginView.$el.find("[name=login]").val("openidm-admin").trigger("change");

                });
            });


            QUnit.asyncTest("Admin user page", function () {
                adminUserViewMock(server);

                adminUserProfileView.render(["FakeTest"], function () {

                    QUnit.equal(adminUserProfileView.$el.find("#userName").val(), "FakeTest", "userName properly loaded into form");

                    QUnit.equal(adminUserProfileView.$el.find("#linkedViewSelect option").length, 1, "Linked Resource properly loaded");

                    QUnit.start();

                });

            });
        }
    };
});