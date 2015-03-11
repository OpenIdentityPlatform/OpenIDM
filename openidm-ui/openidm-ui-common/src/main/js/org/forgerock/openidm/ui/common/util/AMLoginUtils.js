/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2015 ForgeRock AS. All rights reserved.
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

/*global define , _, $*/

/**
 * @author huck.elliott
 */
define("org/forgerock/openidm/ui/common/util/AMLoginUtils", [
    "org/forgerock/commons/ui/common/main/Configuration",
    "org/forgerock/openidm/ui/common/delegates/OpenAMProxyDelegate",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/commons/ui/common/util/Constants"
], function (conf, openamProxy, eventManager, constants) {
    var obj = {};
    
    obj.init = function(parent,adminContext) {
        var callback;
        
        if(adminContext){
            conf.globalData = _.omit(conf.globalData,"selfRegistration","securityQuestions","siteIdentification");
        }
        
        if(conf.globalData.openamAuthEnabled && conf.globalData.openamLoginUrl && conf.globalData.openamLoginUrl.length){
            if(conf.globalData.openamUseExclusively){
                obj.openamLogin();
                return false;
            } else {
                callback = function(){
                    var buttonText = conf.globalData.openamLoginLinkText || $.t("templates.user.LoginTemplate.loginWithOpenAM"),
                        openamLoginLink = $('<div class="group-field-block float-left" style="padding-top:25px;"><a id="openamLoginLink" href="#">' + buttonText + '</a></div>');
                    
                    openamLoginLink.click(obj.openamLogin);
                    
                    parent.$el.find(":submit[name=loginButton]").parent().after(openamLoginLink);
                };
            }
        }
        
        return callback;
    };
    
    obj.openamLogin = function(e){
        var gotoRoute = conf.gotoURL || "",
            gotoURL = encodeURIComponent(location.origin + location.pathname + gotoRoute),
            amURL = conf.globalData.openamLoginUrl + "&goto=" + gotoURL;
        
        if(e){
            e.preventDefault();
        }
        
        if(conf.globalData.authenticationUnavailable){
            eventManager.sendEvent(constants.ROUTE_REQUEST, {routeName: "authenticationUnavailable", args: []});
            if(!e){
                location.reload();
            }
        } else {
            location.href = amURL;
        }
        
    };
    
    obj.openamLogout = function(successCallback){
        openamProxy.logout().then(successCallback, function(){
            conf.globalData.authenticationUnavailable = true;
            if(conf.globalData.openamUseExclusively){
                eventManager.sendEvent(constants.ROUTE_REQUEST, {routeName: "authenticationUnavailable", args: []});
            } else {
                successCallback();
            }
        });
    };
    
    return obj;
});
