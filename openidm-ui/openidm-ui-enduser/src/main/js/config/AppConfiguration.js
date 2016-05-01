/**
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
 * Header, with the fields enclosed by brackets [] replaced by your own identifying
 * information: "Portions copyright [year] [name of copyright owner]".
 *
 * Copyright 2011-2016 ForgeRock AS.
 */

define([
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
                        {"routes":"config/routes/UserRoutesConfig"}
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
                            "id": "change_password",
                            "href": "#profile/password",
                            "i18nKey": "common.user.changePassword"
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
                        {"messages":"config/messages/CommonIDMMessages"},
                        {"messages":"config/messages/SelfServiceMessages"},
                        {"messages":"config/messages/UserMessages"}
                    ]
                }
            },
            {
                moduleClass: "org/forgerock/commons/ui/common/main/ValidatorsManager",
                configuration: {
                    validators: { },
                    loader: [
                        {"validators":"config/validators/SelfServiceValidators"},
                        {"validators":"config/validators/CommonValidators"}
                    ]
                }
            }
        ],
        loggerLevel: 'debug'
    };

    return obj;
});
