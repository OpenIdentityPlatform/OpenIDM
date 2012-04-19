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
define("app/comp/common/urltools/URLActionDispatcher",
        ["app/comp/common/eventmanager/EventManager", 
         "app/util/Constants",
         "app/util/UIUtils",
         "app/comp/common/configuration/AbstractConfigurationAware"], 
         function(eventManager, constants, uiUtils, AbstractConfigurationAware) {
    var obj = new AbstractConfigurationAware();

    obj.decodeURLAndPerformAction = function() {
        var eventName, decodedUrl = uiUtils.convertCurrentUrlToJSON();
        console.debug("url=" + decodedUrl);

        if(decodedUrl.pathName) {
            eventName = obj.configuration.pathNameEventMappings[decodedUrl.pathName];
            if(eventName) {
                eventManager.sendEvent(eventName, decodedUrl.params);
            }
        }

        if(decodedUrl.params && decodedUrl.params.action) {
            eventName = obj.configuration.queryParamEventMappings[decodedUrl.params.action];
            if(eventName) {
                eventManager.sendEvent(eventName, decodedUrl.params);
            }
        } 

    };

    return obj;

});	
