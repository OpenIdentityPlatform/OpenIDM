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

/*global define*/

define("config/AppConfiguration", [
    "org/forgerock/commons/ui/common/util/Constants"
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
                    moduleClass: "org/forgerock/commons/ui/common/main/GenericRouteInterfaceMap",
                    configuration: {
                        LoginView : "org/forgerock/openidm/ui/admin/login/LoginView",
                        LoginDialog: "org/forgerock/commons/ui/common/LoginDialog"
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
                        delegate: "org/forgerock/openidm/ui/common/delegates/SiteConfigurationDelegate"
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
                                "id": "settings",
                                "href": "#settings/",
                                "i18nKey": "common.form.settings"
                            },
                            {
                                "id": "user_link",
                                "href": "../openidmui",
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
                                    "connectors": {
                                        "url": "#resources/",
                                        "name": "config.AppConfiguration.Navigation.links.resources",
                                        "icon": "fa fa-cogs",
                                        "inactive": false
                                    },
                                    "mapping": {
                                        "url": "#mapping/",
                                        "name": "config.AppConfiguration.Navigation.links.mapping",
                                        "icon": "fa fa-map-marker",
                                        "inactive": false,
                                        "urls": {
                                            "properties": {
                                                "url": "#properties/",
                                                "name": "templates.mapping.properties",
                                                "icon": "fa fa-list",
                                                "inactive": false
                                            },
                                            "correlation": {
                                                "url": "#correlation/",
                                                "name": "templates.correlation.correlation",
                                                "icon": "fa fa-random",
                                                "inactive": false
                                            },
                                            "sync": {
                                                "url": "#sync/",
                                                "name": "templates.sync.sync",
                                                "icon": "fa fa-clock-o",
                                                "inactive": false
                                            },
                                            "schedule": {
                                                "url": "#schedule/",
                                                "name": "templates.schedule.schedule",
                                                "icon": "fa fa-calendar",
                                                "inactive": false
                                            }
                                        }
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
