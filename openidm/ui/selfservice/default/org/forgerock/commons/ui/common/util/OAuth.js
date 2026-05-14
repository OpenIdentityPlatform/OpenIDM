"use strict";

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

define(["lodash", "./URIUtils"], function (_, URIUtils) {
    /**
     * Provides generic methods for interacting with oAuth endpoints
     * @exports org/forgerock/commons/ui/common/util/OAuth
     */
    var obj = {};

    /**
        Provides the redirect_url value relative to the browser's current location
        @param {string} returnFileName - a file relative to the current application
        that will be used to accept the incoming oauth response; defaults to "oauthReturn.html"
     */
    obj.getRedirectURI = function (returnFileName) {
        return URIUtils.getCurrentOrigin() + URIUtils.getCurrentPathName().replace(/(\/index\.html)|(\/$)/, "/" + (returnFileName || "oauthReturn.html"));
    };

    /**
      Generates a URL to take the user to an oAuth IDP in order to obtain an authorization code,
      to be subsequently presented to the token endpoint (likely, by a server process).
      @param {string} authorization_endpoint - the base URL to the IDP endpoint used for obtaining access codes
      @param {string} client_id - client (RP) identifier registered with the IDP
      @param {string} scopes - space-separated list of scopes requested by this client to obtain for this user
      @param {string} state - whatever details are useful to get back from the IDP upon return, so
                                    the local processing logic can resume
    */
    obj.getRequestURL = function (authorization_endpoint, client_id, scopes, state) {
        return authorization_endpoint + '?response_type=code&scope=' + encodeURIComponent(scopes) + '&redirect_uri=' + this.getRedirectURI() + '&state=' + encodeURIComponent(state) + '&nonce=' + this.generateNonce(client_id) + '&client_id=' + client_id;
    };

    obj.generateNonce = function () {
        // Math.random().toString(36) converts a random number into a string with letters and numbers
        // ex: Math.random() produces 0.12; 0.12.toString(36) => "0.4bipx4bipx5cxg5veqmfmkj4i"
        // "0.4bipx4bipx5cxg5veqmfmkj4i".substr(2,12) => 4bipx4bipx5c
        // this is sufficiently random to be used as an unguessable nonce
        var nonce = Math.random().toString(36).substr(2, 12);
        sessionStorage.setItem("OAuthNonce", nonce);
        return nonce;
    };

    obj.getCurrentNonce = function () {
        var nonce = sessionStorage.getItem("OAuthNonce");
        sessionStorage.removeItem("OAuthNonce");
        return nonce;
    };

    return obj;
});
