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
 * Copyright 2016 ForgeRock AS.
 */

define([
    "jquery",
    "underscore",
    "handlebars",
    "org/forgerock/commons/ui/common/main/Router",
    "org/forgerock/commons/ui/common/util/OAuth"
], function ($, _,
             Handlebars,
             Router,
             OAuth) {

    var obj = {};

    /**
     *
     * @param options - Object containing parameters used to generate the session in window
     *      path - URL to be used for the new window
     *      windowName - Name of the new window
     *      windowOptions - height and width of the window
     *
     *      Generates a new window
     */
    obj.oauthPopup = function (options) {
        let width = "";
        let height = "";

        width = screen.width * (2/3);
        height = screen.height * (2/3);

        options.windowName = options.windowName ||  'ConnectWithOAuth';
        options.windowOptions = options.windowOptions || 'location=0,status=0,width=' + width + ',height=' + height;
        options.callback = options.callback || function(){ window.location.reload(); };

        window.open(options.path, options.windowName, options.windowOptions);
    };

    /**
     *
     * @param provider - IDM provider object
     * @param state - The URL state used for the oAuth return
     * @returns - Built out url for oAuth use
     */
    obj.getUrl = function(provider, state) {
        let scopes;

        if (_.isArray(provider.scope)) {
            scopes = provider.scope.join(" ");
        } else {
            scopes = provider.scope;
        }

        return OAuth.getRequestURL(provider.authorization_endpoint, provider.client_id, scopes, state);
    };

    /**
     * Iterates over
     * @param name
     */
    obj.setDisplayIcons = function(providers) {
        _.each(providers, function (provider) {
            switch (provider.name) {
                case "google":
                    provider.displayIcon = "google";
                    break;
                case "facebook":
                    provider.displayIcon = "facebook";
                    break;
                case "linkedIn":
                    provider.displayIcon = "linkedin";
                    break;
                default:
                    provider.displayIcon = "cloud";
                    break;
            }
        });

        return providers;
    };

    return obj;
});