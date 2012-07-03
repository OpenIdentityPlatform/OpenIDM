/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
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

/*global define*/

define("app/comp/user/login/InternalLoginHelper",
        ["app/comp/user/delegates/UserDelegate",
         "app/comp/common/eventmanager/EventManager",
         "app/util/Constants",
         "app/comp/common/configuration/AbstractConfigurationAware",
         "app/comp/common/delegate/ServiceInvoker"],
         function (userDelegate, eventManager, constants, AbstractConfigurationAware, serviceInvoker) {
    var obj = new AbstractConfigurationAware();

    obj.loginRequest = function(userName, password, successCallback, errorCallback) {
        userDelegate.logAndGetUserDataByCredentials(userName, password, function(user) {
            eventManager.sendEvent(constants.EVENT_SUCCESFULY_LOGGGED_IN, user.userName);
        }, function() {
            eventManager.sendEvent(constants.EVENT_LOGIN_FAILED);
        });
    };

    obj.logoutRequest = function() {
        userDelegate.logout();
    };

    return obj;
});
