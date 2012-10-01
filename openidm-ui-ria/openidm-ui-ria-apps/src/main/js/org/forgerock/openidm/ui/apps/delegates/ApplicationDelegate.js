/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2012 ForgeRock AS. All rights reserved.
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

/*global $, define, _ */

/**
 * @author jdabrowski
 */
define("org/forgerock/openidm/ui/apps/delegates/ApplicationDelegate", [
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/commons/ui/common/main/AbstractDelegate",
    "org/forgerock/commons/ui/common/main/Configuration",
    "org/forgerock/commons/ui/common/main/EventManager"
], function(constants, AbstractDelegate, configuration, eventManager) {

    var obj = new AbstractDelegate(constants.host + "/openidm/config/ui/applications");

    obj.applications = {};
    obj.statesById = {};
    obj.statesByName = {};
           
    obj.getAllApplications = function(successCallback, errorCallback) {
        var i;
        console.info("Getting all applications");
        
        obj.serviceCall({url: "", success: function(data) {
            if(successCallback) {
                for (i = 0; i < data.availableApps.length; i++) {
                    obj.applications[data.availableApps[i]._id] = data.availableApps[i];
                }
           
                successCallback(data.availableApps);
            }
        }, error: errorCallback} );
    };
    
    obj.getAllStates = function(successCallback, errorCallback) {
        var i;
        console.info("Getting all states");
        
        obj.serviceCall({url: "", success: function(data) {
            if(successCallback) {                    
                for (i = 0; i < data.states.length; i++) {
                    obj.statesById[data.states[i]._id] = data.states[i];
                    obj.statesByName[data.states[i].name] = data.states[i];
                }
                
                successCallback(data.states);
            }
        }, error: errorCallback} );
    };
    
    obj.getUserAppStateByName = function(name, callback) {
        if(_.isEmpty(obj.statesByName)) {
            obj.getAllStates(function() {
                callback(obj.statesByName[name]);
            });
        } else {
            callback(obj.statesByName[name]);
        }
    };
    
    obj.getUserAppStateById = function(name, callback) {
        if(_.isEmpty(obj.statesById)) {
            obj.getAllStates(function() {
                callback(obj.statesById[name]);
            });
        } else {
            callback(obj.statesById[name]);
        }
    };
    
    obj.prepareApplications = function(links, callback) {
        obj.getAllApplications(callback, function(){});
    };
    
    obj.getApplicationDetails = function(itemId, callback) {        
        if(_.isEmpty(obj.applications)) {
            obj.getAllApplications(function() {
                callback(obj.applications[itemId]);
            });
        } else {
            if(callback) {
                callback(obj.applications[itemId]);
            }
            
            return obj.applications[itemId];
        }  
    };
    
    return obj;
});



