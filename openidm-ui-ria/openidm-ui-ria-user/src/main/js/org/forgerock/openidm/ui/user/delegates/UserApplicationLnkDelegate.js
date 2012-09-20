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

/*global $, define */

/**
 * @author jdabrowski
 */
define("org/forgerock/openidm/ui/user/delegates/UserApplicationLnkDelegate", [
    "org/forgerock/openidm/ui/common/util/Constants",
    "org/forgerock/openidm/ui/common/main/AbstractDelegate",
    "org/forgerock/openidm/ui/common/main/Configuration",
    "org/forgerock/openidm/ui/common/main/EventManager",
    "org/forgerock/openidm/ui/common/util/DateUtil"
], function(constants, AbstractDelegate, configuration, eventManager, dateUtil) {

    var obj = new AbstractDelegate(constants.host + "/openidm/managed/user_application_lnk");

    obj.userApplicationLnksCallback = null;
    obj.userApplicationLnks = null;
    obj.numberOfUserApplicationLnks = 0;
    
    obj.normalizeDateField = function(item, fieldName) {
        if (item[fieldName]) {
            item[fieldName] = dateUtil.parseDateString(item[fieldName]);
        }
    };
    
    obj.rebuildLnk = function(link) {
        obj.normalizeDateField(link, 'lastTimeUsed');
    };
    
    obj.rebuildLnks = function(links) {
        var i;
        for(i = 0; i < links.length; i++ ) {
            obj.rebuildLnk(links[i]);
        } 
    };
    
    obj.getAllUserApplicationLnks = function(successCallback, errorCallback) {
        console.info("getting all user application links");

        obj.userApplicationLnksCallback = successCallback;
        obj.numberOfUserApplicationLnks = 0;

        obj.serviceCall({url: "/?_query-id=query-all-user_application_lnk", success: function(data) {
            if(successCallback) {
                obj.userApplicationLnks = obj.rebuildLnks(data.result);
                successCallback(data.result);
            }
        }, error: errorCallback} );
    };

    //TODO this is only test utility method should be moved to the special test package
    obj.removeAllApplicationLnks = function() {
        var serviceInvokerConfig, finished, serviceInvokerModuleName = "org/forgerock/openidm/ui/common/main/ServiceInvoker";
        serviceInvokerConfig = configuration.getModuleConfiguration(serviceInvokerModuleName);

        serviceInvokerConfig.defaultHeaders[constants.OPENIDM_HEADER_PARAM_PASSWORD] = "openidm-admin";
        serviceInvokerConfig.defaultHeaders[constants.OPENIDM_HEADER_PARAM_USERNAME] = "openidm-admin";
        serviceInvokerConfig.defaultHeaders[constants.OPENIDM_HEADER_PARAM_NO_SESION] = true;

        configuration.sendSingleModuleConfigurationChangeInfo(serviceInvokerModuleName);

        finished = 0;
        obj.getAllApplicationLnks(function() {
            var i, successCallback;
            
            successCallback = function(){
                finished++;
                if(finished === obj.notifications.length) {
                    console.debug("deleting finished");
                    serviceInvokerConfig.defaultHeaders = [];
                    eventManager.sendEvent(constants.EVENT_AUTHENTICATION_DATA_CHANGED, { anonymousMode: true});
                }
            };
            
            for(i = 0; i < obj.users.length; i++ ) {
                obj.deleteEntity(obj.userApplicationLnks[i]._id, successCallback);
            }           
        });
    };


    obj.getUserApplicationLnksForUserName = function(uid, successCallback, errorCallback) {
        var result;
        obj.serviceCall({
            url: "/?_query-id=user_application_lnk-for-userName&" + $.param({uid: uid.toLowerCase()}), 
            success: function (data) {
                if(successCallback) {
                    result = data.result;
                    obj.rebuildLnks(result);
                    successCallback(result);
                }
            },
            error: errorCallback
        });
    };

    return obj;
});



