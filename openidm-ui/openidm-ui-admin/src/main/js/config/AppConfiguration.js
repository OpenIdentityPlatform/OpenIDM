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
 * Copyright 2014-2016 ForgeRock AS.
 */

define([
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
                    "resource-assignment" : "org/forgerock/openidm/ui/admin/assignment/AssignmentView",
                    "resource-user" : "org/forgerock/openidm/ui/admin/user/EditUserView",
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
                        {"messages":"config/messages/CommonIDMMessages"},
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
                                "dashboard": {
                                    "name": "config.AppConfiguration.Navigation.links.dashboard",
                                    "icon": "fa fa-dashboard",
                                    "dropdown": true,
                                    "urls": []
                                },
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
                                        },
                                        {
                                            divider: true
                                        },
                                        {
                                            "header": true,
                                            "headerTitle": "config.AppConfiguration.Navigation.links.userSelfService"
                                        },
                                        {
                                            "url": "#selfservice/userregistration/",
                                            "name": "config.AppConfiguration.Navigation.links.userRegistration",
                                            "icon": "fa fa-user",
                                            "inactive": false
                                        },
                                        {
                                            "url": "#selfservice/passwordreset/",
                                            "name": "config.AppConfiguration.Navigation.links.passwordReset",
                                            "icon": "fa fa-key",
                                            "inactive": false
                                        },
                                        {
                                            "url": "#selfservice/forgotUsername/",
                                            "name": "config.AppConfiguration.Navigation.links.forgotUsername",
                                            "icon": "fa fa-question",
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
