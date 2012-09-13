/*
 * @license DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright © 2012 ForgeRock AS. All rights reserved.
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

/*global require, define, _, $ */

/**
 * @author mbilski
 */
define("org/forgerock/openidm/ui/common/main/ErrorsHandler", [
    "org/forgerock/openidm/ui/common/main/AbstractConfigurationAware",
    "org/forgerock/openidm/ui/common/main/EventManager"
], function(AbstractConfigurationAware, eventManager) {
    var obj = new AbstractConfigurationAware();
    
    obj.handleError = function(error, handlers) {
        var handler;
        
        if(handlers) {
            //find match in handlers
            handler = obj.matchError(error, handlers);
        }
        
        if(!handler) {
            //find match in default handlers
            handler = obj.matchError(error, obj.configuration.defaultHandlers);
        }
        
        if(handler) {
            if(handler.event) {
                eventManager.sendEvent(handler.event, {handler: handler, error: error});
            }
            
            //TODO add support for error message
        }
    };
    
    obj.matchError = function(error, handlers) {
        var handler, handlerName;
                
        for(handlerName in handlers) {
            handler = handlers[handlerName];

            if(handler.status) {
                if(parseInt(error.status, 0) === parseInt(handler.status, 0)) {
                    return handler;
                }
            }
            
            //TODO add support for openidm errors
        }
    };

    return obj;
});    

