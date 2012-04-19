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

/*global define, require*/

/**
 * @author yaromin
 */
define(["app/util/Constants", 
        "app/comp/common/eventmanager/EventManager", 
        "app/comp/main/Configuration",
        "app/comp/common/configuration/AbstractConfigurationAware"],
        function(constants, eventManager, configuration, AbstractConfigurationAware) {

    var obj = new AbstractConfigurationAware();
    obj.em = eventManager;

    eventManager.registerListener(constants.EVENT_CONFIGURATION_CHANGED, function(event) {
        obj.callService(event.moduleClass, "updateConfigurationCallback", [event.configuration]);
    });

    eventManager.registerListener(constants.EVENT_DEPENDECIES_LOADED, function(event) {
        obj.callService("app/comp/main/Configuration","sendConfigurationChangeInfo");
        obj.registerAllListenersFromConfig();
        eventManager.sendEvent(constants.EVENT_APP_INTIALIZED);
    });

    obj.callRegisterListenerFromConfig = function (config) {		
        eventManager.registerListener(config.startEvent, function(event) {
            var i, callParameters;
            if(config.description) {
                console.info("Event processing: ",config.description);
            }
            callParameters = [event];
            for(i = 0; i < config.dependencies.length; i++) {
                callParameters.push(require(config.dependencies[i]));
            }
            config.processDescription.apply(this, callParameters);
        });
    };

    obj.registerAllListenersFromConfig = function() {
        var j, oneProcessDefinitionObject, i;
        for(j=0; j < obj.configuration.processConfigurationFiles.length;j++) {
            oneProcessDefinitionObject = require(obj.configuration.processConfigurationFiles[j]);

            for(i = 0; i < oneProcessDefinitionObject.length; i++) {
                obj.callRegisterListenerFromConfig(oneProcessDefinitionObject[i]);
            }	
        }
    };

    obj.callService = function(serviceId, methodName,params) {
        try {
            var service = require(serviceId);
            if(service) {
                service[methodName].apply(service, params);
            }
        } catch(exception) {
            if(params) {
                params = JSON.stringify(params);
            }
            console.warn("Unable to invoke serviceId=" + serviceId + " method=" + methodName + " params=" + params + " exception=" + exception);
        }
    };

    return obj;
});