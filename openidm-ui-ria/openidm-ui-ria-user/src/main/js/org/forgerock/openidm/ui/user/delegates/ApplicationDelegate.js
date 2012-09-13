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
define("org/forgerock/openidm/ui/user/delegates/ApplicationDelegate", [
    "org/forgerock/openidm/ui/common/util/Constants",
    "org/forgerock/openidm/ui/common/main/AbstractDelegate",
    "org/forgerock/openidm/ui/common/main/Configuration",
    "org/forgerock/openidm/ui/common/main/EventManager"
], function(constants, AbstractDelegate, configuration, eventManager) {

    var obj = new AbstractDelegate(constants.host + "/openidm/managed/application");

    obj.applicationsCallback = null;
    obj.applications = {};
    obj.numberOfApplications = 0;
    
    obj.getAllApplications = function(successCallback, errorCallback) {
        var i;
        console.info("Getting all applications");

        obj.applicationsCallback = successCallback;
        obj.numberOfApplications = 0;

        obj.serviceCall({url: "/?_query-id=query-all-applications", success: function(data) {
            if(successCallback) {
                for (i = 0; i < data.result.length; i++) {
                    obj.applications[data.result[i]._id] = data.result[i];
                }
                successCallback(data.result);
            }
        }, error: errorCallback} );
    };

    //TODO this is only test utility method should be moved to the special test package
    obj.removeAllApplications = function() {
        var serviceInvokerConfig, finished, serviceInvokerModuleName = "org/forgerock/openidm/ui/common/main/ServiceInvoker";
        serviceInvokerConfig = configuration.getModuleConfiguration(serviceInvokerModuleName);

        serviceInvokerConfig.defaultHeaders[constants.OPENIDM_HEADER_PARAM_PASSWORD] = "openidm-admin";
        serviceInvokerConfig.defaultHeaders[constants.OPENIDM_HEADER_PARAM_USERNAME] = "openidm-admin";
        serviceInvokerConfig.defaultHeaders[constants.OPENIDM_HEADER_PARAM_NO_SESION] = true;

        configuration.sendSingleModuleConfigurationChangeInfo(serviceInvokerModuleName);

        finished = 0;
        obj.getAllApplications(function() {
            var i, successCallback;
            
            successCallback = function(){
                finished++;
                if(finished === obj.applications.length) {
                    console.debug("deleting finished");
                    serviceInvokerConfig.defaultHeaders = [];
                    eventManager.sendEvent(constants.EVENT_AUTHENTICATION_DATA_CHANGED, { anonymousMode: true});
                }
            };
            
            for(i = 0; i < obj.users.length; i++ ) {
                obj.deleteEntity(obj.applications[i]._id, successCallback);
            }           
        });
    };


    obj.getListOfApplications = function(applicationIds, successCallback, errorCallback) {
        var par,i;
        for (i = 0; i < applicationIds.length; i++) {
            applicationIds[i] = "'"+applicationIds[i]+"'";
        }
        par = applicationIds.join(",");
        obj.serviceCall({
            url: "/?_query-id=application-for-ids&" + $.param({applicationIds: par}), 
            success: function (data) {
                if(successCallback) {
                    successCallback(data.result);
                }
            },
            error: errorCallback
        });
    };
    
    obj.prepareApplications = function(userApplicationLnks, successCallback, errorCallback) {
        var i, applicationIdsToGet, applicationIdsToPrepare;
        applicationIdsToPrepare = obj.converToIdList(userApplicationLnks);
        applicationIdsToGet = obj.getApplicationIdsNotInCache(applicationIdsToPrepare);
        if (applicationIdsToGet.length > 0) {
            obj.getListOfApplications(applicationIdsToGet, function(applications) {
                for (i = 0; i < applications.length; i++) {
                    obj.applications[applications[i]._id] = applications[i];
                }
                successCallback();
            });
        } else {
            console.log("All applications in cache");
            successCallback();
        }
    };
    
    obj.converToIdList = function(userApplicationLnks) {
        var i, appIds = [];
        for (i = 0; i < userApplicationLnks.length; i++) {
            appIds.push(userApplicationLnks[i].applicationId);
        }
        return appIds;
    };
    
    obj.getApplicationIdsNotInCache = function(applicationIds) {
        var i, notInCache = [];
        for (i = 0; i < applicationIds.length; i++) {
            if (!obj.applications[applicationIds[i]]) {
                notInCache.push(applicationIds[i]);
            }
        }
        return notInCache;
    };
    
    obj.getApplicationDetails = function(itemId) {
        return obj.applications[itemId];
    };
    
    return obj;
});



