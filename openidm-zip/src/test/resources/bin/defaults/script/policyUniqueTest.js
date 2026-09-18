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
 * Copyright 2026 3A Systems, LLC.
 */

/*global require, exports, java */

/**
 * Tests the "unique" policy of policy.js.
 *
 * The value under validation is user-supplied (it arrives with the create/update request, including
 * self-registration) and is embedded in a CREST query filter string literal. It must be escaped the
 * same way auth/queryFilter.escapeStringValue does, so that neither a backslash nor a double quote
 * can break the literal or inject further predicates.
 *
 * policy.js is not a module: it is a script that expects the OpenIDM host globals (request,
 * resources, resourceName, openidm) and ends by running the request. The test therefore reads
 * the real script from the classpath and evaluates it inside a function that provides those
 * globals as stubs, capturing the _queryFilter handed to openidm.query.
 */
exports.test = function () {
    var queryFilter = require("auth/queryFilter"),
        source = readClasspathResource("bin/defaults/script/policy.js");

    [
        ["plain", "plain value"],
        ['a"b', "double quote"],
        ["a\\", "trailing backslash must not swallow the closing quote"],
        ['x\\" or /userName pr or /x eq "', "backslash-quote sequence must not break out of the literal"]
    ].forEach(function (testcase) {
        var value = testcase[0],
            scenario = testcase[1],
            captured = runUniquePolicy(source, value),
            expected = 'userName eq "' + queryFilter.escapeStringValue(value) + '"';

        if (captured !== expected) {
            throw {
                "message": "unique policy, " + scenario + ": got <" + captured + ">, expected <" + expected + ">"
            };
        }
    });

    function runUniquePolicy(source, value) {
        var captured = null,
            // ---- host globals policy.js expects; locals here so eval() resolves them ----
            request = {
                method: "action",
                action: "validateProperty",
                resourcePath: "managed/user/abc",
                content: { userName: value }
            },
            resources = [{
                resource: "managed/user/*",
                properties: [{ name: "userName", policies: [{ policyId: "unique" }] }]
            }],
            resourceName = {
                leaf: function () { return "abc"; },
                parent: function () { return { toString: function () { return "managed/user"; } }; }
            },
            openidm = {
                read: function () { return { objects: [] }; },
                isEncrypted: function () { return false; },
                query: function (resource, params) {
                    captured = String(params._queryFilter);
                    return { result: [] };
                }
            };

        eval(source);
        return captured;
    }

    function readClasspathResource(path) {
        var url = java.lang.Thread.currentThread().getContextClassLoader().getResource(path),
            scanner,
            text;
        if (url === null) {
            throw { "message": "classpath resource not found: " + path };
        }
        scanner = new java.util.Scanner(url.openStream(), "UTF-8").useDelimiter("\\A");
        text = scanner.hasNext() ? scanner.next() : "";
        scanner.close();
        return String(text);
    }
};
