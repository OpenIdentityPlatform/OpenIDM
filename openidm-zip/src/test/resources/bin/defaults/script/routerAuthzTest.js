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
 * Tests the list helpers of router-authz.js.
 *
 * router-authz.js is a script, not a module: it declares its helper functions at top level and
 * then evaluates the current request against the access configuration. The test evaluates the
 * real script inside a function with stubbed host globals; the function declarations are hoisted,
 * so they stay reachable even though the trailing access check throws for the stub request.
 */
exports.test = function () {
    var helpers = loadHelpers(readClasspathResource("bin/defaults/script/router-authz.js"));

    [
        // function, list, value, expected, description
        ["contains", ["a", "b"], "b", true, "present element is found"],
        ["contains", ["a", "b"], "c", false, "absent element is not found"],
        ["contains", ["a", "b"], undefined, false, "undefined must not match the slot past the end of the list"],
        ["contains", [], undefined, false, "undefined is not contained in an empty list"],
        ["containsIgnoreCase", ["Admin"], "admin", true, "match is case-insensitive"],
        ["containsIgnoreCase", ["admin"], "user", false, "absent element is not found"],
        ["containsIgnoreCase", [], undefined, false, "undefined is not contained in an empty list"],
        ["containsIgnoreCase", ["a"], undefined, false, "undefined must not match the slot past the end of the list"]
    ].forEach(function (testcase) {
        var fn = testcase[0], list = testcase[1], value = testcase[2], expected = testcase[3], scenario = testcase[4],
            actual = helpers[fn](list, value);
        if (actual !== expected) {
            throw { "message": fn + ": " + scenario + " - got <" + actual + ">, expected <" + expected + ">" };
        }
    });

    function loadHelpers(source) {
        var request = { method: "read", resourcePath: "info/ping" },
            context = { security: { authorization: { roles: [] } } },
            logger = { debug: function () {}, trace: function () {} },
            identityServer = { getProjectLocation: function () { return ""; } },
            load = function () {},
            openidm = {};

        try {
            eval(source);
        } catch (e) {
            // the trailing access check is expected to reject the stub request
        }
        return { contains: contains, containsIgnoreCase: containsIgnoreCase };
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
