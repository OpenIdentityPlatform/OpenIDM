/*
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
 * Copyright 2012-2016 ForgeRock AS.
 */

/*globals request */

/**
 * Used to enable automatic policy evaluation during router processing of any
 * given resource.
 * @module policyFilter
 * @see policy
 */

(function () {

    /**
     * Translates different request details into a single path which can have
     * policy checked for it.
     * @param {string} method Either create or update
     * @param {string} basePath Path to resource (e.g. managed/user or managed/user/1)
     * @param {string} unencodedId The identifier for the new resource, for creates
     * @returns {string} The full path to use for policy evaluation
     */
    exports.getFullResourcePath = function (method, basePath, unencodedId) {
        var fullResourcePath;
        var id = org.forgerock.http.util.Uris.urlEncodePathElement(unencodedId);

        if (method === "create") {
            if (basePath === "") {
                fullResourcePath = id !== null ? id : "*";
            } else {
                fullResourcePath = basePath + "/" + (id !== null ? id : "*");
            }
        } else {
            fullResourcePath = basePath;
        }
        return fullResourcePath;
    };

    /**
     * @param {string} path The full path to use for policy evaluation
     * @param {Object} content Content of the resource to be evaluated
     * @returns {Array} List of all failing policies, with details
     */
    exports.evaluatePolicy = function(path, content) {
        return openidm.action("policy/" + path, "validateObject", content, { "external" : "true" });
    };

    /**
     * Method intended to be called from router filter context, for implicit evaluation of a resource.
     * Throws an error when policy fails, with the failure details included.
     */
    exports.runFilter = function () {
        var enforce = identityServer.getProperty("openidm.policy.enforcement.enabled", "true", true),
            fullResourcePath = this.getFullResourcePath(request.method, request.resourcePath, request.newResourceId);

        if (fullResourcePath.indexOf("policy/") !== 0 && enforce !== "false") {

            result = this.evaluatePolicy(fullResourcePath, request.content);

            if (!result.result) {
                throw {
                    "code" : 403,
                    "message" : "Policy validation failed",
                    "detail" : result
                };
            }
        }

    };

}());