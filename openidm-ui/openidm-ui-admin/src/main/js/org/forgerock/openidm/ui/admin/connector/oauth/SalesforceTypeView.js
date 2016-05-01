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
    "jquery",
    "underscore",
    "org/forgerock/openidm/ui/admin/connector/oauth/AbstractOAuthView",
    "org/forgerock/openidm/ui/admin/delegates/ExternalAccessDelegate",
    "org/forgerock/commons/ui/common/main/Router",
    "org/forgerock/openidm/ui/common/delegates/ConfigDelegate",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/openidm/ui/admin/delegates/ConnectorDelegate"
], function($, _, AbstractOAuthView, ExternalAccessDelegate, router, ConfigDelegate, eventManager, constants, ConnectorDelegate) {

    var SalesforceTypeView = AbstractOAuthView.extend({
        events: {
            "change .url-radio" : "changeUrl"
        },
        data: {
            "callbackURL" : window.location.protocol+"//"+window.location.host + "/admin/oauth.html",
            "urlTypes": [
                {
                    "name": "Production",
                    "value" : "https://login.salesforce.com/services/oauth2/token",
                    "id": "productionRadio",
                    "readonly": true,
                    "selected":false
                },
                {
                    "name": "Sandbox",
                    "value": "https://test.salesforce.com/services/oauth2/token",
                    "id" : "sandboxRadio",
                    "readonly": true,
                    "selected":false
                },
                {
                    "name": "Custom",
                    "value": "https://[custom domain name]/services/oauth2/token",
                    "id" : "customRadio",
                    "readonly": false,
                    "selected":false
                }
            ]
        },
        getScopes: function() {
            var salesforceScope = "id%20api%20refresh_token";

            return salesforceScope;
        },

        getAuthUrl : function() {
            var url = this.$el.find("#OAuthurl").val().replace("/token", "/authorize");

            return url;
        },

        getToken: function(mergedResult, oAuthCode) {
            return ExternalAccessDelegate.getToken(mergedResult.configurationProperties.clientId,
                oAuthCode,
                    window.location.protocol+"//"+window.location.host + "/admin/oauth.html",
                mergedResult.configurationProperties.loginUrl,
                mergedResult._id.replace("/", "_"));
        },

        setToken: function(refeshDetails, connectorDetails, connectorLocation, urlArgs) {
            if(refeshDetails.refresh_token !== undefined) {
                connectorDetails.configurationProperties.refreshToken = refeshDetails.refresh_token;
                connectorDetails.enabled = true;
            } else {
                connectorDetails.configurationProperties.refreshToken = null;
                connectorDetails.enabled = false;
            }

            connectorDetails.configurationProperties.instanceUrl = refeshDetails.instance_url;

            ConnectorDelegate.testConnector(connectorDetails).then(_.bind(function(testResult){
                connectorDetails.objectTypes = testResult.objectTypes;

                ConfigDelegate.updateEntity(connectorLocation, connectorDetails).then(_.bind(function () {
                    _.delay(function () {
                        eventManager.sendEvent(constants.EVENT_CHANGE_VIEW, {route: router.configuration.routes.connectorListView});
                    }, 1500);
                }, this));
            }, this));
        },

        cleanResult: function(mergedResult) {
            delete mergedResult.urlRadio;

            return mergedResult;
        },

        connectorSpecificChanges: function(connectorDetails) {
            if(connectorDetails.configurationProperties.loginUrl) {
                if(this.data.urlTypes[0].value === connectorDetails.configurationProperties.loginUrl) {
                    this.data.urlTypes[0].selected = true;
                } else if (this.data.urlTypes[1].value === connectorDetails.configurationProperties.loginUrl) {
                    this.data.urlTypes[1].selected = true;
                } else {
                    this.data.urlTypes[2].selected = true;
                }
            } else {
                this.data.urlTypes[0].selected = true;
            }
        },

        connectorSpecificValidation: function() {
            if(this.data.connectorDefaults.configurationProperties.loginUrl &&
                this.data.connectorDefaults.configurationProperties.loginUrl === this.$el.find("#OAuthurl").val()) {
                return false;
            } else {
                return true;
            }
        },

        changeUrl: function(event) {
            var radio = event.target,
                id = $(event.target).prop("id");

            if(id === "productionRadio" || id === "sandboxRadio") {
                $("#OAuthurl").prop('readonly', true);
            } else {
                $("#OAuthurl").prop('readonly', false);
            }

            $("#OAuthurl").val($(radio).val());
        }
    });

    return new SalesforceTypeView();
});
