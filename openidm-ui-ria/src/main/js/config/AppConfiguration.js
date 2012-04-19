/* @license 
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
define("config/AppConfiguration",["app/util/Constants", "app/comp/common/eventmanager/EventManager"], 
        function(constants, eventManager) {
    var obj = {
            moduleDefinition: [

                               {
                                   moduleClass: "app/comp/user/login/LoginCtrl",
                                   configuration: {
                                       loginHelperClass: "app/comp/user/login/InternalLoginHelper",
                                       showCredentialFields: true,
                                       loginButtonDisabledByDefault: true
//                                     showCredentialFields: false,
//                                     loginButtonDisabledByDefault: false,
//                                     loginHelperClass: "app/comp/user/login/OpenAMLoginHelper"
                                   } 
                               },
                               {
                                   moduleClass: "app/comp/user/login/OpenAMLoginHelper",
                                   configuration: {
                                       loginURL: "http://openaminstallationdomain.com:8090/openam/UI/Login",
                                       logoutURL: "http://openaminstallationdomain.com:8090/openam/UI/Logout",
                                       passwordParameterName: "IDToken2",
                                       userNameParameterName: "IDToken1",
                                       logoutTestOnly: false,
                                       loginTestOnly: false,
                                       ajaxLogout: false
                                   } 
                               },
                               {
                                   moduleClass: "app/comp/common/urltools/URLActionDispatcher",
                                   configuration: {
                                       queryParamEventMappings: 
                                       {
                                           authenticated: constants.EVENT_AUTHENTICATED
                                       },
                                       pathNameEventMappings:
                                       {
                                           "/profile/": constants.EVENT_AUTHENTICATED,
                                           "/userAdmin/": constants.EVENT_ADMIN_AUTHENTICATED,
                                           "/register/": constants.EVENT_SELF_REGISTRATION_REQUEST
                                       }
                                   } 
                               },
                               {
                                   moduleClass: "app/comp/main/ProcessConfiguration",
                                   configuration: {
                                       processConfigurationFiles: [
                                                                   "config/process/AdminConfig",
                                                                   "config/process/UserConfig",
                                                                   "config/process/CommonConfig"
                                                                   ]

                                   } 
                               },
                               {
                                   moduleClass: "app/comp/common/delegate/ServiceInvoker",
                                   configuration: {
                                       defaultHeaders: {
                                       }										 
                                   } 
                               }
                               ],
                               loggerLevel: 'debug'
    };
    return obj;
});