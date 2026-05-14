"use strict";

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
 * Copyright 2011-2016 ForgeRock AS.
 */

define(["jquery", "underscore", "org/forgerock/commons/ui/common/util/CookieHelper", "org/forgerock/commons/ui/common/main/AbstractConfigurationAware", "org/forgerock/commons/ui/common/util/ModuleLoader"], function ($, _, cookieHelper, AbstractConfigurationAware, ModuleLoader) {
    var obj = new AbstractConfigurationAware();

    obj.login = function (params, successCallback, errorCallback) {
        // resets the session cookie to discard old session that may still exist
        cookieHelper.deleteCookie("session-jwt", "/", "");
        return ModuleLoader.load(obj.configuration.loginHelperClass).then(function (helper) {
            return ModuleLoader.promiseWrapper(_.bind(_.curry(helper.login)(params), helper), {
                success: successCallback,
                error: errorCallback
            });
        });
    };

    obj.logout = function (successCallback, errorCallback) {
        return ModuleLoader.load(obj.configuration.loginHelperClass).then(function (helper) {
            return ModuleLoader.promiseWrapper(_.bind(helper.logout, helper), {
                success: successCallback,
                error: errorCallback
            });
        });
    };

    obj.getLoggedUser = function (successCallback, errorCallback) {
        return ModuleLoader.load(obj.configuration.loginHelperClass).then(function (helper) {
            return ModuleLoader.promiseWrapper(_.bind(helper.getLoggedUser, helper), {
                success: successCallback,
                error: errorCallback
            });
        });
    };

    return obj;
});
