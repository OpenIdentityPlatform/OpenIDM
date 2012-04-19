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
define("config/process/CommonConfig",["app/util/Constants", "app/comp/common/eventmanager/EventManager"], 
        function(constants, eventManager) {
    var obj = 
        [
         {
             startEvent: constants.EVENT_APP_INTIALIZED,
             description: "Starting basic components",
             dependencies: [
                            "app/comp/main/MainCtrl",
                            "app/comp/user/login/LoginCtrl",
                            "app/comp/common/breadcrumbs/BreadcrumbsCtrl",
                            "app/comp/common/navigation/NavigationCtrl",
                            "app/comp/common/dialog/DialogsCtrl",
                            "app/comp/common/popup/PopupCtrl",
                            "app/comp/common/urltools/URLActionDispatcher"
                            ],
                            processDescription: function(event, mainCtrl, loginCtrl, breadcrumbsCtrl, navigationCtrl, dialogsCtrl, popupCtrl,urlActionDispatcher) {
                                mainCtrl.clearContent();
                                loginCtrl.init();
                                breadcrumbsCtrl.init();
                                navigationCtrl.init();
                                dialogsCtrl.init();
                                popupCtrl.init();
                                eventManager.sendEvent(constants.EVENT_AUTHENTICATION_DATA_CHANGED, { anonymousMode: true});
                                urlActionDispatcher.decodeURLAndPerformAction();
                            }	
         },
         {
             startEvent: constants.EVENT_AUTHENTICATION_DATA_CHANGED,
             description: "",
             dependencies: [
                            "app/comp/common/breadcrumbs/BreadcrumbsCtrl",
                            "app/comp/main/Configuration"
                            ],
                            processDescription: function(event, breadCrumbsCtrl, configuration) {
                                var serviceInvokerModuleName, serviceInvokerConfig; 
                                serviceInvokerModuleName = "app/comp/common/delegate/ServiceInvoker";
                                serviceInvokerConfig = configuration.getModuleConfiguration(serviceInvokerModuleName);
                                if(!event.anonymousMode) {
                                    delete serviceInvokerConfig.defaultHeaders[constants.OPENIDM_HEADER_PARAM_PASSWORD];
                                    delete serviceInvokerConfig.defaultHeaders[constants.OPENIDM_HEADER_PARAM_USERNAME];
                                    delete serviceInvokerConfig.defaultHeaders[constants.OPENIDM_HEADER_PARAM_NO_SESION];
                                } else {
                                    serviceInvokerConfig.defaultHeaders[constants.OPENIDM_HEADER_PARAM_PASSWORD] = constants.OPENIDM_ANONYMOUS_PASSWORD;
                                    serviceInvokerConfig.defaultHeaders[constants.OPENIDM_HEADER_PARAM_USERNAME] = constants.OPENIDM_ANONYMOUS_USERNAME;
                                    serviceInvokerConfig.defaultHeaders[constants.OPENIDM_HEADER_PARAM_NO_SESION]= true; 
                                }
                                configuration.sendSingleModuleConfigurationChangeInfo(serviceInvokerModuleName);
                            }
         },
         {
             startEvent: constants.EVENT_GO_BACK_REQUEST,
             description: "",
             dependencies: [
                            "app/comp/common/breadcrumbs/BreadcrumbsCtrl"
                            ],
                            processDescription: function(event, breadCrumbsCtrl) {
                                breadCrumbsCtrl.goBack();
                            }
         },
         {
             startEvent: constants.EVENT_SWITCH_VIEW_REQUEST,
             description: "",
             dependencies: ["app/comp/main/ProcessConfiguration"],
             processDescription: function(event, pc) {
                 pc.callService(event.viewId, "init");
             }
         },
         {
             startEvent: constants.EVENT_NAVIGATION_HOME_REQUEST,
             description: "",
             dependencies: [
                            "app/comp/main/MainCtrl",
                            "app/comp/common/breadcrumbs/BreadcrumbsCtrl"
                            ],
                            processDescription: function(event, mainCtrl, breadCrumbsCtrl) {
                                mainCtrl.clearContent();
                                breadCrumbsCtrl.clearPath();
                                breadCrumbsCtrl.set('Home');
                            }
         },
         {
             startEvent: constants.EVENT_BREADCRUMBS_HOME_CLICKED,
             description: "",
             dependencies: [
                            "app/comp/main/MainCtrl"
                            ],
                            processDescription: function(event, mainCtrl) {
                                mainCtrl.clearContent();
                            }
         }
         ];
    return obj;
});