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
 * Copyright 2014-2015 ForgeRock AS.
 */

/*global define, window */

define("org/forgerock/openidm/ui/admin/connector/oauth/GoogleTypeView", [
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

    var GoogleTypeView = AbstractOAuthView.extend({
        getScopes: function() {
            var googleScope = "https://www.googleapis.com/auth/admin.directory.group%20"
                +"https://www.googleapis.com/auth/admin.directory.orgunit%20"
                +"https://www.googleapis.com/auth/admin.directory.user%20"
                +"https://www.googleapis.com/auth/apps.licensing";

            return googleScope;
        },
        data: {
            "callbackURL": window.location.protocol + "//" + window.location.host + "/admin/oauth.html"
        },
        getAuthUrl : function() {
            return $("#OAuthurl").val();
        },

        getToken: function(mergedResult, oAuthCode) {

            return ExternalAccessDelegate.getToken(mergedResult.configurationProperties.clientId,
                oAuthCode,
                window.location.protocol+"//"+window.location.host + "/admin/oauth.html",
                "https://accounts.google.com/o/oauth2/token",
                mergedResult._id.replace("/", "_"));
        },

        setToken: function(refeshDetails, connectorDetails, connectorLocation, urlArgs) {
            connectorDetails.configurationProperties.refreshToken = refeshDetails.refresh_token;

            ConnectorDelegate.testConnector(connectorDetails).then(_.bind(function(testResult){
                connectorDetails.objectTypes = testResult.objectTypes;
                connectorDetails.enabled = true;

                ConfigDelegate.updateEntity(connectorLocation, connectorDetails).then(_.bind(function () {
                    _.delay(function () {
                        eventManager.sendEvent(constants.EVENT_CHANGE_VIEW, {route: router.configuration.routes.connectorListView});
                    }, 1500);
                }, this));
            }, this));
        }
    });

    return new GoogleTypeView();
});
