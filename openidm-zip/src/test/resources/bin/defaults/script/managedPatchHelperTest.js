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
 * Tests against managedPatchHelper.js.
 *
 */
exports.test = function() {
    var managedPatchHelper = require("managedPatchHelper");

    testRemove();

    function testRemove() {
        // test cases for remove
        [
            [
                [
                    {
                        "name" : "user",
                        "b" : {
                            "b1" : "text/javascript",
                            "b2" : "roles"
                        }
                    }
                ],
                "user",
                "b.b2",
                [
                    {
                        "name" : "user",
                        "b" : {
                            "b1" : "text/javascript"
                        }
                    }
                ]
            ],
            [
                [
                    {
                        "name" : "user",
                        "b" : {
                            "b1" : "text/javascript",
                            "b2" : "roles"
                        }
                    }
                ],
                "role",
                "b.b2",
                [
                    {
                        "name" : "user",
                        "b" : {
                            "b1" : "text/javascript",
                            "b2" : "roles"
                        }
                    }
                ]
            ]
        ].map(
            function (testcase) {
                (function (content, name, path, expectedResult) {
                    var result = managedPatchHelper.remove(content, name, path);
                    if (JSON.stringify(result) !== JSON.stringify(expectedResult)) {
                        throw {
                            "message": "Remove failed for content " + JSON.stringify(content) +", got <"
                            + JSON.stringify(result) + ">, expected <" + JSON.stringify(expectedResult) + ">"
                        };
                    }
                }).apply(null, testcase);
            });
    }

    testAdd();

    function testAdd() {
        // test cases for add
        [
            [
                [
                    {
                        "name" : "user",
                        "b" : {
                            "b1" : "text/javascript",
                            "b2" : "roles"
                        }
                    }
                ],
                "user",
                "b.bb2",
                "added",
                [
                    {
                        "name" : "user",
                        "b" : {
                            "b1" : "text/javascript",
                            "b2" : "roles",
                            "bb2" : "added"
                        }
                    }
                ]
            ]
        ].map(
            function (testcase) {
                (function (content, name, path, value, expectedResult) {
                    var result = managedPatchHelper.add(content, name, path, value);
                    if (JSON.stringify(result) !== JSON.stringify(expectedResult)) {
                        throw {
                            "message": "Add failed for content " + JSON.stringify(content) +", got <"
                            + JSON.stringify(result) + ">, expected <" + JSON.stringify(expectedResult) + ">"
                        };
                    }
                }).apply(null, testcase);
            });
    }
}