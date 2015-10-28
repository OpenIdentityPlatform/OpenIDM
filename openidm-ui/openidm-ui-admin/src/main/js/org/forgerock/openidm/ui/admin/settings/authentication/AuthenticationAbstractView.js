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
 * Copyright 2015 ForgeRock AS.
 */

/*global define*/

define("org/forgerock/openidm/ui/admin/settings/authentication/AuthenticationAbstractView", [
    "jquery",
    "underscore",
    "org/forgerock/openidm/ui/admin/util/AdminAbstractView",
    "org/forgerock/openidm/ui/common/delegates/ConfigDelegate",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/commons/ui/common/util/Constants"

], function($, _,
            AdminAbstractView,
            ConfigDelegate,
            EventManager,
            Constants) {

    var authenticationDataChanges = {},
        authenticationData = {},
        AuthenticationAbstractView = AdminAbstractView.extend({

            retrieveAuthenticationData: function (callback) {
                ConfigDelegate.readEntity("authentication").then(_.bind(function (data) {
                    authenticationDataChanges = _.clone(data, true);
                    authenticationData = _.clone(data, true);

                    if (callback) {
                        callback();
                    }
                }, this));
            },

            getAuthenticationData: function () {
                return _.clone(authenticationData.serverAuthContext, true);
            },

            getAuthenticationDataChanges: function () {
                return _.clone(authenticationDataChanges.serverAuthContext, true);
            },

            /**
             * Keeps a clean, ready to save copy of the authentication changes.
             * This should be called by implementing views right before calling checkChanges to ensure data is always uptodate.
             *
             * @param properties {array} - an array of strings representing the properties in the passed in object.
             * @param object {object} - the object containing changes
             */
            setProperties: function(properties, object) {
                _.each(properties, function(prop) {
                    if (_.isEmpty(object[prop]) &&
                        !_.isNumber(object[prop]) &&
                        !_.isBoolean(object[prop])) {
                        delete authenticationDataChanges[prop];
                    } else {
                        authenticationDataChanges.serverAuthContext[prop] = object[prop];
                    }
                }, this);
            },

            saveAuthentication: function() {
                return ConfigDelegate.updateEntity("authentication", authenticationDataChanges).then(function() {
                    EventManager.sendEvent(Constants.EVENT_DISPLAY_MESSAGE_REQUEST, "authSaveSuccess");
                    authenticationData = _.clone(authenticationDataChanges, true);
                });
            }

        });

    return AuthenticationAbstractView;
});
