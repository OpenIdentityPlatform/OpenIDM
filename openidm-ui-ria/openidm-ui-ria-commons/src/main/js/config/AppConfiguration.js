/* @license 
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

/*global define*/

/**
 * @author yaromin
 */
define("config/AppConfiguration", [
    "org/forgerock/openidm/ui/common/util/Constants", 
    "org/forgerock/openidm/ui/common/main/EventManager"
], function(constants, eventManager) {
    var obj = {
    moduleDefinition: [   
        {
            moduleClass: "org/forgerock/openidm/ui/common/main/ProcessConfiguration",
            configuration: {
            processConfigurationFiles: [
                "config/process/CommonConfig"
            ]
            } 
        },
        {
            moduleClass: "org/forgerock/openidm/ui/common/main/ErrorsHandler",
            configuration: {
            defaultHandlers: {
                "unauthorized": {
                    status: "401",
                    event: constants.EVENT_UNAUTHORIZED
                },
                "serverError": {
                    status: "503",
                    event: constants.EVENT_SERVICE_UNAVAILABLE
                }
            }
            } 
        },
        {
            moduleClass: "org/forgerock/openidm/ui/common/components/Navigation",
            configuration: {
                links: {                    
                    "default" : {
                        "urls": {
                            //"role": "openidm-authorized"
                            "openidm": {
                                "url": "http://forgerock.com/openidm.html",
                                "name": "OpenIDM"
                            }
                        }    
                    }
                }                                       
            } 
        }
        ],
        loggerLevel: 'debug'
    };
    return obj;
});
