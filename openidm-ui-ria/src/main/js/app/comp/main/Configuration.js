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

/*global define*/

/**
 * @author yaromin
 */
define(["app/comp/common/eventmanager/EventManager",
        "app/util/Constants",
        "config/AppConfiguration"],
        function(eventManager, constants, appConfiguration) {
    var obj = {};
    obj.appConfiguration = appConfiguration; 

    obj.mode = 'admin';

    obj.setProperty = function(propertyName, propertyValue) {
        obj[propertyName] = propertyValue;
    };

    obj.removeModuleConfigurationProperty = function(moduleClassName, propertyName, propertyValue) {
        var moduleConf = obj.getModuleConfiguration(moduleClassName);
        moduleConf[propertyName] = propertyValue; 
    };

    //NOT in use. NOT tested
    obj.appendToModuleConfigurationPropertyArray = function(moduleClassName, propertyName, propertyValue) {
        var moduleConf = obj.getModuleConfiguration(moduleClassName);
        if(!moduleConf[propertyName]) {
            moduleConf[propertyName] = [propertyValue];
        } else {
            moduleConf[propertyName].push(propertyValue);
        }
    };


    if(obj.appConfiguration.loggerLevel !== 'debug') {
        console.log = function(message) {};
        console.debug = function(message) {};
        console.info = function(message) {};
        console.error = function(message) {};
        console.warn = function(message) {};
    }

    obj.sendConfigurationChangeInfo = function() {
        var i;
        for(i = 0; i < obj.appConfiguration.moduleDefinition.length; i++) {
            eventManager.sendEvent(constants.EVENT_CONFIGURATION_CHANGED, obj.appConfiguration.moduleDefinition[i]);
        }
    };

    obj.sendSingleModuleConfigurationChangeInfo = function(moduleClassName) {
        var i;
        for(i = 0; i < obj.appConfiguration.moduleDefinition.length; i++) {
            if(moduleClassName === obj.appConfiguration.moduleDefinition[i].moduleClass) {
                eventManager.sendEvent(constants.EVENT_CONFIGURATION_CHANGED, obj.appConfiguration.moduleDefinition[i]);
                return;
            }
        }			
        console.warn("No module name " + moduleClassName + " found to send configuration to");
    };

    obj.getModuleConfiguration = function(moduleClass) {
        var i;
        for(i = 0; i < obj.appConfiguration.moduleDefinition.length; i++) {
            if(obj.appConfiguration.moduleDefinition[i].moduleClass === moduleClass) {
                return obj.appConfiguration.moduleDefinition[i].configuration;
            }
        }
    };


    return obj;
});