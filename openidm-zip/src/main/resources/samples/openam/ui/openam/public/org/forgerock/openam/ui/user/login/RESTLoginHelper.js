/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2014 ForgeRock AS. All rights reserved.
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

/*global define, _, window, $ */

define("org/forgerock/openam/ui/user/login/RESTLoginHelper", [
    "./AuthNDelegate",
    "org/forgerock/commons/ui/common/main/ViewManager",
    "org/forgerock/commons/ui/common/main/AbstractConfigurationAware",
    "org/forgerock/commons/ui/common/main/Router",
    "org/forgerock/commons/ui/common/main/Configuration",
    "org/forgerock/commons/ui/common/util/UIUtils",
    "org/forgerock/commons/ui/common/util/Constants"
], function (authNDelegate, viewManager, AbstractConfigurationAware, router, conf, uiUtils, constants) {
    var obj = new AbstractConfigurationAware();

    obj.login = function(params, successCallback, errorCallback) {
        var _this = this;
        authNDelegate.getRequirements().done(function (requirements) {
        
            // populate the current set of requirements with the values we have from params
            var populatedRequirements = _.clone(requirements);
            
            // used in auto login from self registration
            if (params.userName &&  params.password && requirements.stage === "DataStore1"){
                populatedRequirements.callbacks[0].input[0].value = params.userName;
                populatedRequirements.callbacks[1].input[0].value = params.password;
            } else {
                _.each(requirements.callbacks, function (obj, i) {
                    if (params.hasOwnProperty("callback_" + i)) {
                        populatedRequirements.callbacks[i].input[0].value = params["callback_" + i];
                    }
                });
            }

            authNDelegate
                .submitRequirements(populatedRequirements)
                .then(function (result) {
                        if (result.hasOwnProperty("tokenId")) {
                            obj.getLoggedUser(function(user){
                                conf.setProperty('loggedUser', user);
                                successCallback(user);
                                authNDelegate.resetProcess();
                            }, errorCallback, {});
                        } else if (result.hasOwnProperty("authId")) {
                            // re-render login form for next set of required inputs
                            if (viewManager.currentView === 'LoginView') {
                                viewManager.refresh();
                            } else {
                                // TODO: If using a module chain with autologin the user is currently routed to the first login screen.
                                location.href = '#login' + conf.globalData.auth.realm;
                            }
                        }
                    },
                    function (failedStage) {
                        if (failedStage > 1) {
                            // re-render login form, sending back to the start of the process.
                            viewManager.refresh();
                        }
                        errorCallback();
                    });
        
        });
    };

    obj.logout = function(successCallback, errorCallback) {
        var _this = this;
        return authNDelegate.logout().then(successCallback, errorCallback);
    };
    
    obj.getLoggedUser = function(successCallback, errorCallback) {
        try{
            authNDelegate.getProfile({"serverError": {status: "503"}, "unauthorized": {status: "401"}}, {}).then(successCallback, errorCallback);
        } catch(e) {
            console.log(e);
            errorCallback();
        }
    };
    
    obj.getLoginUrlParams = function() {
        var url = conf.globalData.auth.fullLoginURL;
        return uiUtils.convertQueryParametersToJSON(url.substring(url.indexOf('?') + 1));
       
    };
        
    obj.filterUrlParams = function(params){
        var paramsToSave = ['arg','authIndexType','authIndexValue','goto','gotoOnFail','ForceAuth','locale'],
            filteredParams = {};
        
        _.each(paramsToSave, function(p){
            if(params[p]){
                filteredParams[p] = params[p];
            }
        });
        
        return (!$.isEmptyObject(filteredParams)) ? '&' + $.param(filteredParams) : '';
    };
    
    return obj;
});
