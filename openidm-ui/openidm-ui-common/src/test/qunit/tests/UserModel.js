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
 * Copyright 2016 ForgeRock AS.
 */

define([
    "jquery",
    "sinon",
    "org/forgerock/commons/ui/common/main/Configuration",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/commons/ui/common/main/ServiceInvoker",
    "org/forgerock/openidm/ui/common/UserModel"
], function ($, sinon, Configuration, Constants, ServiceInvoker, UserModel) {
    QUnit.module('UserModel Functions');

    QUnit.test("User Model reflects appropriate policy after subsequent login (OPENIDM-5154)", function () {
        var headers = {};

        Configuration.globalData = {roles: {}};
        // stub the rest calls invoked by the UserModel to use these simple responses
        sinon.stub(ServiceInvoker, "restCall", function (options) {
            options.headers = options.headers || {};
            if (options.url.indexOf("/" + Constants.context + "/policy/") !== -1 ) {
                return $.Deferred().resolve({
                    "url": options.url
                });
            } else {
                return $.Deferred().resolve({
                    authenticationId: Constants.HEADER_PARAM_USERNAME,
                    authorization: {
                        id: options.headers[Constants.HEADER_PARAM_USERNAME],
                        component: "managed/user",
                        roles: ["openidm-authorized"]
                    }
                });
            }
        });

        QUnit.ok(UserModel.policy === undefined, "Policy initially undefined for UserModel");
        headers[Constants.HEADER_PARAM_USERNAME] = "openidm-admin";
        UserModel.getProfile(headers).then(function () {
            QUnit.ok(UserModel.policy.url.indexOf('openidm-admin') !== -1, "Policy loaded for openidm-admin");
        }).then(function () {
            headers[Constants.HEADER_PARAM_USERNAME] = "bjensen";
            return UserModel.getProfile(headers);
        }).then(function () {
            QUnit.ok(UserModel.policy.url.indexOf('bjensen') !== -1, "Policy loaded for bjensen");
        }).then(function () {
            ServiceInvoker.restCall.restore();
        });

    });
});
