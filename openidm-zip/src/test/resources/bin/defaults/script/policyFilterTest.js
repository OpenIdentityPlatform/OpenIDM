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
 * Copyright 2016 ForgeRock AS.
 */

/**
 * Tests against policyFilter.js.
 *
 * Currently, only getFullResourcePath is tested.
 */
exports.test = function() {
    var policyFilter = require("policyFilter");

    testGetFullResourcePath();

    function testGetFullResourcePath() {
        // test cases for getFullResourcePath
        [
            ["create", "managed/user", "user%with%encoded%percentage", "managed/user/user%25with%25encoded%25percentage"],
            ["create", "managed/user", "user//with/encoded/%percentage", "managed/user/user%2F%2Fwith%2Fencoded%2F%25percentage"],
            ["create", "managed/user", "user+%2B/", "managed/user/user+%252B%2F"],
            ["create", "managed/user", "$user  /+", "managed/user/$user%20%20%2F+"],
            ["create", "managed/user", null, "managed/user/*"],
            ["create", "managed/user", "null", "managed/user/null"],
            ["create", "", "user%with%encoded%percentage", "user%25with%25encoded%25percentage"],
            ["create", "", "*", "*"],
            ["create", "", "null", "null"],
            ["create", "", null, "*"],
            ["create", "", "", ""],
            ["read", "", "user%with%encoded%percentage", ""],
            ["read", "", null, ""],
            ["read", "system/hrdb", "user%with%encoded%percentage", "system/hrdb"],
            ["CReaTe", "", "user%with%encoded%percentage", ""]
        ].map(
            function (testcase) {
                (function (method, resourcePath, resourceId, expectedFullResourcePath) {
                    // get full resource path, forcing to javascript string
                    var fullResourcePath = policyFilter.getFullResourcePath(method, resourcePath, resourceId) + "";
                    if (fullResourcePath !== expectedFullResourcePath) {
                        throw {
                            "message": "for " + method + " on '" + resourcePath + "' with resourceId '" + resourceId
                            + "', got <" + fullResourcePath + ">, expected <" + expectedFullResourcePath + ">"
                        };
                    }
                }).apply(null, testcase);
            });
    }
}
