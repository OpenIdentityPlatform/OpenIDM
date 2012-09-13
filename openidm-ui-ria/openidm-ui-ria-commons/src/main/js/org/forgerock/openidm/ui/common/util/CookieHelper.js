/*
 * @license DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
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

define("org/forgerock/openidm/ui/common/util/CookieHelper", [
], function () {
    var obj = {};
    
    /**
     * Create a cookie in the browser with given parameters. Only name parameter is mandatory. 
     */
    obj.createCookie = function(cookieName, cookieValue, expirationDate, cookiePath, cookieDomain, secureCookie) {
        var expirationDatePart, nameValuePart, pathPart, domainPart, securePart; 
        expirationDatePart = (expirationDate) ? ";expires=" + expirationDate.toGMTString() : "";
        nameValuePart = cookieName + "=" + cookieValue;
        pathPart = (cookiePath) ? ";path=" + cookiePath : "";
        domainPart = (cookieDomain) ? ";domain=" + cookieDomain : "";
        securePart = (secureCookie) ? ";secure" : "";
    
        return nameValuePart + expirationDatePart + pathPart + domainPart + securePart;
    };
    
    obj.setCookie = function(cookieName, cookieValue, expirationDate, cookiePath, cookieDomain, secureCookie) {
        document.cookie = obj.createCookie(cookieName, cookieValue, expirationDate, cookiePath, cookieDomain, secureCookie);
    };
    
    return obj;
});