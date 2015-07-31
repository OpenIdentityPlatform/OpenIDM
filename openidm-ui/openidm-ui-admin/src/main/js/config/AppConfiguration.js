/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2014 ForgeRock AS. All rights reserved.
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

/*global define */

define("config/AppConfiguration", [
    "org/forgerock/openidm/ui/common/util/Constants"
], function(constants) {
    var obj = {
        moduleDefinition: [
            {
                moduleClass: "org/forgerock/commons/ui/common/main/SessionManager",
                configuration: {
                    loginHelperClass: "org/forgerock/openidm/ui/common/login/InternalLoginHelper"
                }
            },
            {
                moduleClass: "org/forgerock/openidm/ui/admin/connector/ConnectorRegistry",
                configuration: {
                    "org.identityconnectors.ldap.LdapConnector_1.1" : "org/forgerock/openidm/ui/admin/connector/ldap/LDAPTypeView",
                    "org.identityconnectors.ldap.LdapConnector_1.4" : "org/forgerock/openidm/ui/admin/connector/ldap/LDAPTypeView",
                    "org.forgerock.openicf.connectors.googleapps.GoogleAppsConnector_1.4" : "org/forgerock/openidm/ui/admin/connector/oauth/GoogleTypeView",
                    "org.forgerock.openidm.salesforce.Salesforce_2.0" : "org/forgerock/openidm/ui/admin/connector/oauth/SalesforceTypeView"
                }
            },
            {
                moduleClass: "org/forgerock/openidm/ui/common/resource/ResourceEditViewRegistry",
                configuration: {
                    "resource-role" : "org/forgerock/openidm/ui/admin/role/EditRoleView"
                }
            },
            {
                moduleClass: "org/forgerock/commons/ui/common/components/Messages",
                configuration: {
                    messages: {
                    },
                    loader: [
                        {"messages":"config/messages/CommonMessages"},
                        {"messages":"config/messages/AdminMessages"}
                    ]
                }
            },
            {
                moduleClass: "org/forgerock/commons/ui/common/SiteConfigurator",
                configuration: {
                    remoteConfig: true,
                    delegate: "org/forgerock/openidm/ui/admin/delegates/SiteConfigurationDelegate"
                }
            },
            {
                moduleClass: "org/forgerock/commons/ui/common/main/ProcessConfiguration",
                configuration: {
                    processConfigurationFiles: [
                        "config/process/CommonConfig",
                        "config/process/CommonIDMConfig",
                        "config/process/AdminIDMConfig"
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
                        {"routes":"config/routes/AdminRoutesConfig"}
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
                    userBar: [
                        {
                            "id": "user_link",
                            "href": "",
                            "event" : constants.EVENT_SELF_SERVICE_CONTEXT,
                            "i18nKey": "common.form.userView"
                        },
                        {
                            "id": "logout_link",
                            "href": "#logout/",
                            "i18nKey": "common.form.logout"
                        }
                    ],
                    links: {
                        "admin" : {
                            "role": "ui-admin",
                            "urls": {
                                "configuration": {
                                    "name": "Configure",
                                    "icon": "fa fa-wrench",
                                    "dropdown": true,
                                    "urls" : [
                                        {
                                            "url": "#connectors/",
                                            "name": "config.AppConfiguration.Navigation.links.connectors",
                                            "icon": "fa fa-cubes",
                                            "inactive": false
                                        },
                                        {
                                            "url": "#managed/",
                                            "name": "config.AppConfiguration.Navigation.links.managedObjects",
                                            "icon": "fa fa-th",
                                            "inactive": false
                                        },
                                        {
                                            "url": "#mapping/",
                                            "name": "config.AppConfiguration.Navigation.links.mapping",
                                            "icon": "fa fa-arrows-h",
                                            "inactive": false
                                        },
                                        {
                                            "url": "#settings/",
                                            "name": "config.AppConfiguration.Navigation.links.systemPref",
                                            "icon": "fa fa-cog",
                                            "inactive": false
                                        }
                                    ]
                                },
                                "managed": {
                                    "name": "config.AppConfiguration.Navigation.links.manage",
                                    "icon": "fa fa-cogs",
                                    "dropdown": true,
                                    "urls" : []
                                }
                            }
                        }
                    }
                }
            },
            {
                moduleClass: "org/forgerock/commons/ui/common/util/UIUtils",
                configuration: {
                    templateUrls: [ //preloaded templates
                    ]
                }
            },

            {
                moduleClass: "org/forgerock/commons/ui/common/main/ValidatorsManager",
                configuration: {
                    policyDelegate: "org/forgerock/openidm/ui/common/delegates/PolicyDelegate",
                    validators: { },
                    loader: [
                        {"validators":"config/validators/CommonValidators"},
                        {"validators":"config/validators/AdminValidators"}
                    ]
                }
            }
        ],
        loggerLevel: 'debug'
    };

    return obj;
});
