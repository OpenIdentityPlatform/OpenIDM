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
 * Tests against connectionPoolPatchHelper.js.
 *
 */
exports.test = function() {
    var connectionPoolPatchHelper = require("connectionPoolPatchHelper");

    testUpgradeConnectionPool();

    function testUpgradeConnectionPool() {
        [
            [
                {
                    "type" : "bonecp",
                    "partitionCount" : 4,
                    "maxConnectionsPerPartition" : 25,
                    "minConnectionsPerPartition" : 5
                },
                {
                    "type" : "bonecp",
                    "partitionCount" : 4,
                    "maxConnectionsPerPartition" : 25,
                    "minConnectionsPerPartition" : 5
                },
                {
                    "type" : "bonecp",
                    "partitionCount" : 4,
                    "maxConnectionsPerPartition" : 25,
                    "minConnectionsPerPartition" : 5
                }
            ],
            [
                {
                    "type" : "bonecp"
                },
                {
                    "type" : "bonecp",
                    "partitionCount" : 4,
                    "maxConnectionsPerPartition" : 25,
                    "minConnectionsPerPartition" : 5
                },
                {
                    "type" : "bonecp",
                    "partitionCount" : 4,
                    "maxConnectionsPerPartition" : 25,
                    "minConnectionsPerPartition" : 5
                }
            ],
            [
                {
                    "type" : "bonecp",
                    "partitionCount" : 2
                },
                {
                    "type" : "bonecp",
                    "partitionCount" : 4,
                    "maxConnectionsPerPartition" : 25,
                    "minConnectionsPerPartition" : 5
                },
                {
                    "type" : "bonecp",
                    "partitionCount" : 2,
                    "maxConnectionsPerPartition" : 25,
                    "minConnectionsPerPartition" : 5
                }
            ]
        ].map(
            function (testcase) {
                (function (existingConnectionPool, updatedConnectionPool, expectedResult) {
                    var result = connectionPoolPatchHelper.upgradeConnectionPool(existingConnectionPool, updatedConnectionPool);
                    if (JSON.stringify(result) !== JSON.stringify(expectedResult)) {
                        throw {
                            "message": "upgradeConnectionPool failed: got <" + JSON.stringify(result) +
                            ">, expected <" + JSON.stringify(expectedResult) + ">"
                        };
                    }
                }).apply(null, testcase);
            });
    }
}