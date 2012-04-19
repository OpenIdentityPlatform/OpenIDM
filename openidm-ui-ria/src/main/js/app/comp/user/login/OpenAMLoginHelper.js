/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright © 2011 ForgeRock AS. All rights reserved.
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

/*global $, define*/

define("app/comp/user/login/OpenAMLoginHelper",[
                                                "app/comp/user/delegates/UserDelegate",
                                                "app/comp/common/eventmanager/EventManager",
                                                "app/util/Constants",
                                                "app/comp/common/configuration/AbstractConfigurationAware",
                                                "app/comp/common/delegate/ServiceInvoker",
                                                "app/util/UIUtils"],
                                                function (userDelegate, eventManager, constants, AbstractConfigurationAware, serviceInvoker, uiUtils) {
    var obj = new AbstractConfigurationAware();

    obj.loginRequest = function(login, password) {
        var completeUrl, queryParameters = {}, url = obj.configuration.loginURL;
         
        if(login && login.length) {
            queryParameters[obj.configuration.userNameParameterName] = login;
        }

        if(password && password.length) {
            queryParameters[obj.configuration.passwordParameterName] = password;
        }

        completeUrl = url + "?" + $.param(queryParameters) + "&goto=" + uiUtils.getCurrentUrlBasePart() + "?action=authenticated";
        console.debug("redirecting to external identity manager url=" + completeUrl);
        if(!obj.configuration.loginTestOnly) {
            uiUtils.setUrl(completeUrl);
        } else {
            console.debug("Test mode. Not redirecting");
        }
    };

    obj.logoutRequest = function() {
        if(!obj.configuration.logoutTestOnly) {
            if(obj.configuration.ajaxLogout) {
                serviceInvoker.restCall({dataType: 'jsonp', url: obj.configuration.logoutURL, type: "GET"});
            } else {
                uiUtils.setUrl(obj.configuration.logoutURL);
            }

        } else {
            console.debug("Test mode. Not redirecting");
        }
    };

    return obj;
});
