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

/*global $, define*/

define("org/forgerock/openidm/ui/user/login/OpenAMLoginHelper", [
	"org/forgerock/openidm/ui/user/delegates/UserDelegate",
	"org/forgerock/openidm/ui/common/main/EventManager",
	"org/forgerock/openidm/ui/common/util/Constants",
	"org/forgerock/openidm/ui/common/main/AbstractConfigurationAware",
	"org/forgerock/openidm/ui/common/main/ServiceInvoker",
	"org/forgerock/openidm/ui/common/util/UIUtils",
	"org/forgerock/openidm/ui/common/util/CookieHelper"
], function (userDelegate, eventManager, constants, AbstractConfigurationAware, serviceInvoker, uiUtils, cookieHelper) {
    var obj = new AbstractConfigurationAware();

    /**
     * Pass credentials provided on OpenIDM site to OpenAM site. 
     */
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

    /**
     * Logout from OpenAM 
     */
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
    
    /**
     * Redirect to OpenAM post registration site 
     */
    obj.redirectToPostPostRegistrationSite = function(login) {
        uiUtils.setUrl(obj.configuration.loginURL + "?" + obj.configuration.postSelfRegistrationQueryParameters + "?uid=" + login);
    };
    
    /**
     * Set OpenAM cookie obtained 
     */
    obj.setAuthenticationCookie = function(tokenId) {
        cookieHelper.setCookie(obj.configuration.authenticationTokenCookieName,tokenId, null,"/",obj.configuration.cookieDomain);
    };

    return obj;
});
