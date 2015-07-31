/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2013 ForgeRock AS. All rights reserved.
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

define("config/AppConfiguration", [
    "org/forgerock/openidm/ui/common/util/Constants",
    "org/forgerock/commons/ui/common/main/EventManager"
], function(constants, eventManager) {
    var obj = {
        moduleDefinition: [
            {
                moduleClass: "org/forgerock/commons/ui/common/main/SessionManager",
                configuration: {
                    loginHelperClass: "org/forgerock/openidm/ui/common/login/InternalLoginHelper"
                }
            },
            {
                moduleClass: "org/forgerock/commons/ui/common/SiteConfigurator",
                configuration: {
                    remoteConfig: true,
                    delegate: "org/forgerock/openidm/ui/util/delegates/SiteConfigurationDelegate"
                }
            },
            {
                moduleClass: "org/forgerock/commons/ui/common/main/ProcessConfiguration",
                configuration: {
                    processConfigurationFiles: [
                        "config/process/IDMConfig",
                        "config/process/CommonIDMConfig",
                        "config/process/UserConfig",
                        "config/process/CommonConfig"
                    ]
                }
            },
            {
                moduleClass: "org/forgerock/commons/ui/common/main/Router",
                configuration: {
                    routes: {
                    },
                    loader: [
                        {"routes":"config/routes/CommonRoutesConfig"},
                        {"routes":"config/routes/CommonIDMRoutesConfig"},
                        {"routes":"config/routes/SelfServiceRoutesConfig"},
                        {"routes":"config/routes/UserRoutesConfig"},
                        {"routes":"config/routes/IDMRoutesConfig"}
                    ]
                }
            },
            {
                moduleClass: "org/forgerock/commons/ui/common/main/ServiceInvoker",
                configuration: {
                    defaultHeaders: {
                    }
                }
            },
            {
                moduleClass: "org/forgerock/commons/ui/common/main/ErrorsHandler",
                configuration: {
                    defaultHandlers: {
                    },
                    loader: [
                        {"defaultHandlers":"config/errorhandlers/CommonErrorHandlers"}
                    ]
                }
            },
            {
                moduleClass: "org/forgerock/commons/ui/common/components/Navigation",
                configuration: {
                    username: {
                        "isLink": true,
                        "href" : "#profile/",
                        "secondaryLabel" : "config.AppConfiguration.Navigation.links.viewProfile"
                    },
                    userBar: [
                        {
                            "id": "security_link",
                            "href": "#profile/change_security_data/",
                            "i18nKey": "templates.user.UserProfileTemplate.changeSecurityData"
                        },
                        {
                            "id": "logout_link",
                            "href": "#logout/",
                            "i18nKey": "common.form.logout"
                        }
                    ],
                    links: {
                        "user" : {
                            "urls": {
                                "dashboard": {
                                    "url": "#dashboard/",
                                    "name": "config.AppConfiguration.Navigation.links.dashboard",
                                    "icon": "fa fa-dashboard",
                                    "inactive": false
                                },
                                "profile": {
                                    "url": "#profile/",
                                    "name": "common.user.profile",
                                    "icon": "fa fa-user",
                                    "inactive": false
                                }
                            }
                        }
                    }
                }
            },
            {
                moduleClass: "org/forgerock/openidm/ui/common/workflow/FormManager",
                configuration: {
                    forms: { // Workflow User Task to View mapping
                    }
                }
            },
            {
                moduleClass: "org/forgerock/commons/ui/common/util/UIUtils",
                configuration: {
                    templateUrls: [ ]
                }
            },
            {
                moduleClass: "org/forgerock/commons/ui/common/components/Messages",
                configuration: {
                    messages: {
                    },
                    loader: [
                        {"messages":"config/messages/CommonMessages"},
                        {"messages":"config/messages/SelfServiceMessages"},
                        {"messages":"config/messages/UserMessages"}
                    ]
                }
            },
            {
                moduleClass: "org/forgerock/commons/ui/common/main/ValidatorsManager",
                configuration: {
                    policyDelegate: "org/forgerock/openidm/ui/common/delegates/PolicyDelegate",
                    validators: { },
                    loader: [
                        {"validators":"config/validators/SelfServiceValidators"},
                        {"validators":"config/validators/UserValidators"},
                        {"validators":"config/validators/CommonValidators"}
                    ]
                }
            }
        ],
        loggerLevel: 'debug'
    };

    return obj;
});
